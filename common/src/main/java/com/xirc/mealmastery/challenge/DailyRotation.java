package com.xirc.mealmastery.challenge;

import com.xirc.mealmastery.MealMasteryLog;
import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.config.ServerConfig;
import com.xirc.mealmastery.culinary.CulinaryClock;
import com.xirc.mealmastery.culinary.CulinaryProfile;
import com.xirc.mealmastery.culinary.ProfileManager;
import com.xirc.mealmastery.recipe.CookingMethod;
import com.xirc.mealmastery.recipe.CulinaryRegistries;
import com.xirc.mealmastery.recipe.CulinaryRegistry;
import com.xirc.mealmastery.recipe.MealEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Recipe of the Day and generated daily/weekly challenges.
 *
 * <p>Everything is drawn from dishes the server can <em>actually</em> make, so
 * an objective is never impossible. The selection is seeded by the day number,
 * so every player sees the same dish without any extra state, and a server
 * restart does not reroll it.</p>
 *
 * <p>Real-time scheduling is deliberately unsupported: the clock is Minecraft
 * days, which keeps everything working on a world that is only played at
 * weekends.</p>
 */
public final class DailyRotation {

    private static volatile long currentDay = Long.MIN_VALUE;
    private static volatile ResourceLocation recipeOfTheDay;
    private static volatile List<ChallengeDefinition> dailyChallenges = List.of();

    private DailyRotation() {
    }

    public static ResourceLocation recipeOfTheDay() {
        return recipeOfTheDay;
    }

    public static List<ChallengeDefinition> dailyChallenges() {
        return dailyChallenges;
    }

    /** Called each server tick; only does work when the day actually changes. */
    public static void tick(MinecraftServer server) {
        ServerConfig config = ConfigManager.server();
        if (!config.challenges.enabled) {
            return;
        }
        long day = CulinaryClock.day(server);
        if (day == currentDay) {
            return;
        }
        currentDay = day;
        roll(day, config);

        ProfileManager profiles = ProfileManager.get();
        if (profiles == null) {
            return;
        }
        for (CulinaryProfile profile : profiles.onlineProfiles().values()) {
            ChallengeManager.rotate(profile, ChallengeDefinition.Scope.DAILY);
            if (config.challenges.weeklyEnabled && day % 7L == 0L) {
                ChallengeManager.rotate(profile, ChallengeDefinition.Scope.WEEKLY);
            }
        }
    }

    private static void roll(long day, ServerConfig config) {
        CulinaryRegistry registry = CulinaryRegistries.current();
        List<MealEntry> candidates = registry.entries();
        if (candidates.isEmpty()) {
            recipeOfTheDay = null;
            dailyChallenges = List.of();
            return;
        }
        // Seeded by the day so the choice is stable across restarts and equal
        // for every player, with no extra saved state.
        Random random = new Random(day * 0x9E3779B97F4A7C15L);

        if (config.challenges.recipeOfTheDayEnabled) {
            recipeOfTheDay = candidates.get(random.nextInt(candidates.size())).target();
        } else {
            recipeOfTheDay = null;
        }

        List<ChallengeDefinition> generated = new ArrayList<>();
        if (config.challenges.dailyEnabled) {
            for (int i = 0; i < config.challenges.dailyChallengeCount; i++) {
                ChallengeDefinition definition = generate(registry, random, i, day);
                if (definition != null) {
                    generated.add(definition);
                }
            }
        }
        dailyChallenges = List.copyOf(generated);
        MealMasteryLog.LOGGER.debug("Rolled day {}: recipe of the day {}, {} daily challenge(s)",
                day, recipeOfTheDay, generated.size());
    }

    /**
     * Builds one challenge out of what is installed.
     *
     * <p>Each shape is only offered when the server has enough content to make
     * it satisfiable — a "use this method" objective is never generated for a
     * method no installed recipe uses.</p>
     */
    private static ChallengeDefinition generate(CulinaryRegistry registry, Random random,
                                                int index, long day) {
        List<CookingMethod> methods = new ArrayList<>(registry.methods());
        List<ResourceLocation> categories = new ArrayList<>(registry.categories());

        ResourceLocation id = new ResourceLocation("mealmastery",
                "daily/" + day + "_" + index);
        ChallengeObjective objective;

        int shape = random.nextInt(4);
        if (shape == 0 && !methods.isEmpty()) {
            CookingMethod method = methods.get(random.nextInt(methods.size()));
            objective = new ChallengeObjective(ChallengeObjective.Type.USE_METHOD,
                    method.id().toString(), 3 + random.nextInt(5), false);
        } else if (shape == 1 && !categories.isEmpty()) {
            ResourceLocation category = categories.get(random.nextInt(categories.size()));
            int available = registry.byCategory(category).size();
            if (available == 0) {
                return null;
            }
            objective = new ChallengeObjective(ChallengeObjective.Type.COOK_CATEGORY,
                    category.toString(), Math.min(available, 2 + random.nextInt(3)), true);
        } else if (shape == 2) {
            objective = new ChallengeObjective(ChallengeObjective.Type.COOK_RECIPE, "",
                    Math.min(registry.size(), 3 + random.nextInt(4)), true);
        } else {
            objective = new ChallengeObjective(ChallengeObjective.Type.UNIQUE_RECIPES_IN_ONE_DAY,
                    "", Math.min(registry.size(), 3 + random.nextInt(3)), false);
        }

        return new ChallengeDefinition(id, ChallengeDefinition.Scope.DAILY,
                List.of(objective),
                new ChallengeReward(40, 0, 0, null, null, List.of(), List.of()),
                null, false);
    }

    public static void reset() {
        currentDay = Long.MIN_VALUE;
        recipeOfTheDay = null;
        dailyChallenges = List.of();
    }
}
