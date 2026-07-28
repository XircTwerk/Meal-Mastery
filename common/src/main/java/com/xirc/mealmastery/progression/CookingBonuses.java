package com.xirc.mealmastery.progression;

import com.xirc.mealmastery.MealMasteryLog;
import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.config.ServerConfig;
import com.xirc.mealmastery.culinary.CulinaryProfile;
import com.xirc.mealmastery.culinary.MealStamp;
import com.xirc.mealmastery.culinary.ProfileManager;
import com.xirc.mealmastery.event.CulinaryEvents;
import com.xirc.mealmastery.mastery.MasteryRank;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodData;

import java.util.ArrayList;
import java.util.List;

/**
 * What eating a dish you have mastered actually does.
 *
 * <p>Two effects, both scaled by your rank with that specific dish: a chance of
 * a beneficial potion effect, and extra saturation. A dish you have never made
 * behaves exactly as vanilla Farmer's Delight intends — the bonus is earned,
 * not universal.</p>
 *
 * <p>Only vanilla effect ids are used; nothing new is registered, and an id a
 * pack names that does not resolve is reported once and skipped rather than
 * crashing the meal.</p>
 */
public final class CookingBonuses {

    private static boolean registered;
    private static boolean warnedAboutEffects;

    private CookingBonuses() {
    }

    public static synchronized void install() {
        if (registered) {
            return;
        }
        registered = true;
        CulinaryEvents.MEAL_EATEN.register(CookingBonuses::onMealEaten);
    }

    private static void onMealEaten(CulinaryEvents.MealEaten event) {
        ServerConfig config = ConfigManager.server();
        ServerConfig.Bonuses bonuses = config.mastery.bonuses;
        if (!bonuses.enabled || !config.mastery.enabled) {
            return;
        }
        ServerPlayer player = event.player();
        CulinaryProfile profile = event.profile();

        // A stamped dish carries the quality of whoever cooked it, so a chef's
        // cooking is genuinely better for the person they hand it to. Unstamped
        // food falls back to the eater's own mastery.
        MealStamp stamp = event.stamp();
        int stars;
        double strength;
        if (stamp != null) {
            stars = stamp.effectiveStars();
            strength = stamp.perfect() ? bonuses.perfectEffectMultiplier : 1.0;
            creditCook(event, stamp, config);
        } else {
            stars = profile.rankOf(event.meal(), config.mastery.toCurve()).ordinal();
            strength = 1.0;
        }
        if (stars <= 0) {
            return;
        }
        float scale = (float) Math.min(1.0, strength * stars / MasteryRank.highestIndex());

        applySaturation(player, (float) (bonuses.saturationPerRank * stars * strength));
        rollEffect(player, bonuses, scale, stars);
    }

    /**
     * Pays the cook when somebody else eats their food.
     *
     * <p>This is what makes cooking for other people worth doing, and it needs
     * no "gave an item to a player" hook — the stamp already records who made
     * it, so the credit lands whenever and wherever it is eaten. Eating your
     * own cooking pays nothing extra; you were already credited for making it.</p>
     */
    private static void creditCook(CulinaryEvents.MealEaten event, MealStamp stamp,
                                   ServerConfig config) {
        java.util.UUID cookId = stamp.cookId();
        if (cookId == null || cookId.equals(event.player().getUUID())) {
            return;
        }
        int reward = config.mastery.bonuses.masteryPerMealServedToOthers;
        if (reward <= 0) {
            return;
        }
        ProfileManager profiles = ProfileManager.get();
        if (profiles == null) {
            return;
        }
        CulinaryProfile cook = profiles.of(cookId);
        cook.meal(event.meal()).recordServed(1L);
        cook.meal(event.meal()).addMasteryPoints(reward);
        cook.stats().addPortionsServed(1L);
        cook.markDirty();

        ServerPlayer online = event.player().server.getPlayerList().getPlayer(cookId);
        if (online != null) {
            online.displayClientMessage(Component.translatable("mealmastery.notify.served",
                    event.player().getDisplayName(),
                    Component.translatable(itemName(event.meal()))), true);
            CulinaryEvents.PORTIONS_SERVED.fire(new CulinaryEvents.PortionsServed(
                    online, cook, event.meal(), 1, event.day()));
        }
    }

    private static String itemName(net.minecraft.resources.ResourceLocation meal) {
        var item = BuiltInRegistries.ITEM.get(meal);
        return item == null ? meal.toString() : item.getDescriptionId();
    }

    /**
     * Extra saturation only. Hunger is left alone deliberately: topping up the
     * food bar would let a mastered dish out-feed what the recipe was balanced
     * to give, whereas saturation just makes it last longer.
     */
    private static void applySaturation(ServerPlayer player, float amount) {
        if (amount <= 0.0F) {
            return;
        }
        FoodData food = player.getFoodData();
        // Vanilla never lets saturation exceed the food level.
        float capped = Math.min(food.getSaturationLevel() + amount, food.getFoodLevel());
        food.setSaturation(capped);
    }

    private static void rollEffect(ServerPlayer player, ServerConfig.Bonuses bonuses, float scale,
                                   int stars) {
        double chance = bonuses.effectChanceAtMaxRank * scale;
        if (chance <= 0.0 || player.getRandom().nextDouble() >= chance) {
            return;
        }
        List<MobEffect> candidates = resolveEffects(bonuses.effects);
        if (candidates.isEmpty()) {
            return;
        }
        MobEffect effect = candidates.get(player.getRandom().nextInt(candidates.size()));

        int amplifier = Math.round(bonuses.effectAmplifierAtMaxRank * scale);
        int duration = Math.max(1, bonuses.effectDurationSeconds) * 20;
        player.addEffect(newEffect(effect, duration, amplifier));
    }

    private static List<MobEffect> resolveEffects(List<String> ids) {
        List<MobEffect> effects = new ArrayList<>(ids.size());
        for (String raw : ids) {
            ResourceLocation id = ResourceLocation.tryParse(raw);
            MobEffect effect = id == null ? null : BuiltInRegistries.MOB_EFFECT.get(id);
            if (effect == null) {
                if (!warnedAboutEffects) {
                    warnedAboutEffects = true;
                    MealMasteryLog.LOGGER.warn(
                            "mastery.bonuses.effects names an effect that does not exist: '{}'",
                            raw);
                }
                continue;
            }
            effects.add(effect);
        }
        return effects;
    }

    /**
     * Isolated because 1.21 changed this constructor to take a
     * {@code Holder<MobEffect>} rather than the effect itself, so the two
     * branches differ in exactly one place.
     */
    private static MobEffectInstance newEffect(MobEffect effect, int duration, int amplifier) {
        return new MobEffectInstance(effect, duration, amplifier, false, true, true);
    }

    /** Star rating for a dish, 0..5, used by tooltips and the journal. */
    public static int starsFor(CulinaryProfile profile, ResourceLocation dish) {
        return profile.rankOf(dish, ConfigManager.server().mastery.toCurve()).ordinal();
    }
}
