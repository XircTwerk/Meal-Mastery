package com.xirc.mealmastery.progression;

import com.xirc.mealmastery.MealMasteryLog;
import com.xirc.mealmastery.config.ConfigEnums;
import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.config.ServerConfig;
import com.xirc.mealmastery.culinary.ActivityEntry;
import com.xirc.mealmastery.culinary.CulinaryClock;
import com.xirc.mealmastery.culinary.CulinaryProfile;
import com.xirc.mealmastery.culinary.MealRecord;
import com.xirc.mealmastery.culinary.MethodRecord;
import com.xirc.mealmastery.culinary.ProfileManager;
import com.xirc.mealmastery.event.CulinaryEvents;
import com.xirc.mealmastery.mastery.MasteryCurve;
import com.xirc.mealmastery.mastery.MasteryRank;
import com.xirc.mealmastery.recipe.CookingMethod;
import com.xirc.mealmastery.recipe.CulinaryRegistries;
import com.xirc.mealmastery.recipe.CulinaryRegistry;
import com.xirc.mealmastery.recipe.MealEntry;
import com.xirc.mealmastery.recipe.RecipeEntry;
import com.xirc.mealmastery.tracking.CulinaryCredit;
import com.xirc.mealmastery.tracking.ServingTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Applies a {@link CulinaryCredit} to a profile.
 *
 * <p>Everything that happens because a player cooked something happens here, in
 * one place, in one order: record the preparation, resolve discovery, credit
 * ingredients and methods, award mastery, award XP, update statistics, records
 * and streaks, then announce. Downstream systems listen on the event bus rather
 * than being called from here.</p>
 */
public final class ProgressionService {

    private ProgressionService() {
    }

    public static void credit(CulinaryCredit credit) {
        try {
            apply(credit);
        } catch (RuntimeException | LinkageError failure) {
            // Progression is a bonus system. A failure here must not propagate
            // into whatever the player was doing.
            MealMasteryLog.LOGGER.error("Failed to apply culinary credit for {} x{}",
                    credit.meal(), credit.amount(), failure);
        }
    }

    private static void apply(CulinaryCredit credit) {
        ServerConfig config = ConfigManager.server();
        CulinaryRegistry registry = CulinaryRegistries.current();
        MealEntry entry = registry.entry(credit.meal());
        if (entry == null) {
            return;
        }

        double automationScale = automationScale(credit, config);
        if (automationScale <= 0.0) {
            return;
        }
        // Taking a portion out of a placed feast is serving, not cooking: the
        // dish was already prepared by whoever placed it.
        if (ServingTracker.recordIfServing(credit)) {
            return;
        }

        ProfileManager profiles = ProfileManager.get();
        if (profiles == null) {
            return;
        }
        ServerPlayer player = credit.player();
        CulinaryProfile profile = profiles.of(player);
        long day = CulinaryClock.day(player.server);
        MasteryCurve masteryCurve = config.mastery.toCurve();

        MealRecord record = profile.meal(credit.meal());
        boolean firstEver = !record.hasEverBeenPrepared();
        MasteryRank rankBefore = record.rank(masteryCurve);
        int levelBefore = profile.cookingLevel(config.progression.toCurve());

        record.recordPreparation(day, credit.amount(), credit.method());

        boolean discovered = resolveDiscovery(profile, record, credit.meal(), day, config, player);
        boolean firstMethodUse = creditMethod(profile, credit, day, player);
        Ingredients ingredients = creditIngredients(profile, entry, credit, day, player);

        awardMastery(config, record, credit, entry);
        MasteryRank rankAfter = record.rank(masteryCurve);
        if (rankAfter.ordinal() > rankBefore.ordinal()) {
            announceRank(player, profile, credit.meal(), rankBefore, rankAfter, record, day, masteryCurve);
        }

        long xp = awardXp(profile, config, credit, day, firstEver, firstMethodUse,
                ingredients, masteryCurve, automationScale, player);

        // Quality, perfect rolls, batch bonuses and tool perks all hang off the
        // preparation that just happened.
        CookingRewards.onPrepared(player, profile, credit.meal(), credit.amount(), credit.method());

        profile.stats().addMealsPrepared(credit.amount());
        if (entry.category().id().getPath().equals("feast")) {
            profile.stats().addFeastsPrepared(credit.amount());
        }
        updateStreaksAndRecords(profile, config, credit.meal(), day);

        profile.pushActivity(new ActivityEntry(
                ActivityEntry.Type.MEAL_PREPARED, credit.meal(), xp, day));
        profile.markDirty();

        CulinaryEvents.MEAL_PREPARED.fire(new CulinaryEvents.MealPrepared(
                player, profile, credit.meal(), credit.amount(), credit.method(), firstEver, day));
        if (discovered) {
            CulinaryEvents.RECIPE_DISCOVERED.fire(new CulinaryEvents.RecipeDiscovered(
                    player, profile, credit.meal(), day));
        }

        int levelAfter = profile.cookingLevel(config.progression.toCurve());
        if (levelAfter > levelBefore) {
            profile.pushActivity(new ActivityEntry(
                    ActivityEntry.Type.LEVEL_UP, null, levelAfter, day));
            CulinaryEvents.COOKING_LEVEL_CHANGED.fire(new CulinaryEvents.CookingLevelChanged(
                    player, profile, levelBefore, levelAfter));
        }
    }

    /**
     * Records that a player personally ate a tracked dish.
     *
     * @param stamp quality data the eaten stack carried, or {@code null}
     */
    public static void recordMeal(ServerPlayer player, ResourceLocation meal,
                                  com.xirc.mealmastery.culinary.MealStamp stamp) {
        try {
            ServerConfig config = ConfigManager.server();
            CulinaryRegistry registry = CulinaryRegistries.current();
            if (!registry.isTracked(meal)) {
                return;
            }
            ProfileManager profiles = ProfileManager.get();
            if (profiles == null) {
                return;
            }
            CulinaryProfile profile = profiles.of(player);
            long day = CulinaryClock.day(player.server);

            MealRecord record = profile.meal(meal);
            record.recordEaten(day, 1);
            profile.stats().addMealsEaten(1);

            if (config.discovery.mode == ConfigEnums.DiscoveryMode.EAT_OUTPUT
                    && record.discover(day)) {
                profile.pushActivity(new ActivityEntry(
                        ActivityEntry.Type.RECIPE_DISCOVERED, meal, 0L, day));
                CulinaryEvents.RECIPE_DISCOVERED.fire(new CulinaryEvents.RecipeDiscovered(
                        player, profile, meal, day));
            }
            if (config.mastery.enabled && config.mastery.pointsPerMeal > 0) {
                record.addMasteryPoints(config.mastery.pointsPerMeal);
            }
            long xp = XpEngine.eatingXp(config);
            if (xp > 0L) {
                profile.addCookingXp(xp, config.progression.toCurve());
            }
            profile.markDirty();

            CulinaryEvents.MEAL_EATEN.fire(
                    new CulinaryEvents.MealEaten(player, profile, meal, day, stamp));
        } catch (RuntimeException | LinkageError failure) {
            MealMasteryLog.LOGGER.error("Failed to record a meal eaten: {}", meal, failure);
        }
    }

    // -------------------------------------------------------------- internals

    private record Ingredients(int newlyDiscovered, int newVariants) {
    }

    private static double automationScale(CulinaryCredit credit, ServerConfig config) {
        if (!credit.automated()) {
            return 1.0;
        }
        return switch (config.automation.credit) {
            case NO_CREDIT -> 0.0;
            case REDUCED_CREDIT -> config.automation.reducedCreditMultiplier;
            case OWNER_CREDIT, FULL_CREDIT -> 1.0;
        };
    }

    private static boolean resolveDiscovery(CulinaryProfile profile, MealRecord record,
                                            ResourceLocation meal, long day, ServerConfig config,
                                            ServerPlayer player) {
        if (config.discovery.mode != ConfigEnums.DiscoveryMode.COOK_RECIPE
                && config.discovery.mode != ConfigEnums.DiscoveryMode.ALWAYS_VISIBLE) {
            return false;
        }
        if (!record.discover(day)) {
            return false;
        }
        profile.pushActivity(new ActivityEntry(
                ActivityEntry.Type.RECIPE_DISCOVERED, meal, 0L, day));
        profile.streaks().recordDiscoveryDay(day, Math.max(1, config.streaks.graceDays));
        profile.records().offerDiscoveryStreak(profile.streaks().discoveryStreak());
        return true;
    }

    private static boolean creditMethod(CulinaryProfile profile, CulinaryCredit credit, long day,
                                        ServerPlayer player) {
        CookingMethod method = credit.method();
        if (method == null || method.equals(CookingMethod.UNKNOWN)) {
            return false;
        }
        MethodRecord methodRecord = profile.method(method);
        boolean first = methodRecord.recordPreparation(day, credit.amount(), credit.meal());
        if (first) {
            profile.pushActivity(new ActivityEntry(
                    ActivityEntry.Type.METHOD_DISCOVERED, method.id(), 0L, day));
            CulinaryEvents.METHOD_DISCOVERED.fire(new CulinaryEvents.MethodDiscovered(
                    player, profile, method, day));
        }
        return first;
    }

    /**
     * Credits ingredients only where they were genuinely observed.
     *
     * <p>Two honest sources: slots that accept exactly one item (so there is
     * nothing to guess), and items seen leaving the player's inventory during
     * the same session. Tagged slots with no observed input are skipped rather
     * than credited to an arbitrary member.</p>
     */
    private static Ingredients creditIngredients(CulinaryProfile profile, MealEntry entry,
                                                 CulinaryCredit credit, long day,
                                                 ServerPlayer player) {
        Set<ResourceLocation> used = new LinkedHashSet<>();
        for (RecipeEntry recipe : entry.recipes()) {
            if (!recipe.method().equals(credit.method()) && entry.recipes().size() > 1) {
                continue;
            }
            for (RecipeEntry.IngredientSlot slot : recipe.ingredients()) {
                if (!slot.hasVariants()) {
                    used.add(slot.items().get(0));
                    continue;
                }
                for (ResourceLocation observed : credit.usedIngredients()) {
                    if (slot.accepts(observed)) {
                        used.add(observed);
                    }
                }
            }
        }

        int discovered = 0;
        int variants = 0;
        MealRecord record = profile.meal(credit.meal());
        for (ResourceLocation ingredient : used) {
            if (profile.ingredient(ingredient)
                    .recordUse(day, credit.amount(), credit.meal(), credit.method())) {
                discovered++;
                profile.pushActivity(new ActivityEntry(
                        ActivityEntry.Type.INGREDIENT_DISCOVERED, ingredient, 0L, day));
                CulinaryEvents.INGREDIENT_DISCOVERED.fire(new CulinaryEvents.IngredientDiscovered(
                        player, profile, ingredient, day));
            }
            if (record.recordIngredientVariant(ingredient)) {
                variants++;
            }
        }
        profile.stats().addIngredientUses(used.size());
        return new Ingredients(discovered, variants);
    }

    private static void awardMastery(ServerConfig config, MealRecord record,
                                     CulinaryCredit credit, MealEntry entry) {
        if (!config.mastery.enabled) {
            return;
        }
        long points = (long) config.mastery.pointsPerPreparation * credit.amount();
        if (config.mastery.intendedMethodBonus > 0
                && credit.method().equals(entry.primaryMethod())) {
            points += (long) config.mastery.intendedMethodBonus * credit.amount();
        }
        record.addMasteryPoints(points);
    }

    private static void announceRank(ServerPlayer player, CulinaryProfile profile,
                                     ResourceLocation meal, MasteryRank from, MasteryRank to,
                                     MealRecord record, long day, MasteryCurve curve) {
        if (!record.claimRankAnnouncement(to)) {
            return;
        }
        profile.pushActivity(new ActivityEntry(
                to.isMastered() ? ActivityEntry.Type.MEAL_MASTERED : ActivityEntry.Type.MASTERY_RANK,
                meal, to.ordinal(), day));
        if (to.isMastered()) {
            long first = record.firstPreparedDay();
            long daysTaken = first == Long.MIN_VALUE ? -1L : Math.max(0L, day - first);
            profile.records().offerMastery(meal, day, daysTaken);
        }
        CulinaryEvents.MASTERY_RANK_CHANGED.fire(new CulinaryEvents.MasteryRankChanged(
                player, profile, meal, from, to));
    }

    private static long awardXp(CulinaryProfile profile, ServerConfig config, CulinaryCredit credit,
                                long day, boolean firstEver, boolean firstMethodUse,
                                Ingredients ingredients, MasteryCurve masteryCurve,
                                double automationScale, ServerPlayer player) {
        MealRecord record = profile.meal(credit.meal());
        // recordPreparation already ran, so today's count includes this batch.
        int priorThisPeriod = Math.max(0, record.preparedOnDay(day) - credit.amount());

        XpEngine.Result result = XpEngine.compute(new XpEngine.Inputs(
                credit.amount(),
                priorThisPeriod,
                firstEver,
                firstMethodUse,
                ingredients.newlyDiscovered(),
                ingredients.newVariants(),
                0,
                profile.masteredCount(masteryCurve)), config);

        long xp = Math.round(result.total() * automationScale);
        if (xp <= 0L) {
            return 0L;
        }
        profile.addCookingXp(xp, config.progression.toCurve());
        CulinaryEvents.COOKING_XP_GAINED.fire(new CulinaryEvents.CookingXpGained(
                player, profile, xp, credit.meal()));
        return xp;
    }

    private static void updateStreaksAndRecords(CulinaryProfile profile, ServerConfig config,
                                                ResourceLocation meal, long day) {
        if (config.streaks.clock != ConfigEnums.StreakClock.DISABLED) {
            int streak = profile.streaks().recordCookingDay(day, config.streaks.graceDays);
            profile.records().offerCookingStreak(streak);
        }
        if (config.streaks.varietyStreakEnabled) {
            int variety = profile.streaks().recordVariety(meal);
            profile.records().offerVarietyStreak(variety);
        }
        int uniqueToday = profile.streaks().recordDailyMeal(day, meal);
        profile.records().offerUniqueMealsInOneDay(uniqueToday, day);
        profile.records().offerMealsInOneDay(profile.streaks().mealsToday(), day);
    }
}
