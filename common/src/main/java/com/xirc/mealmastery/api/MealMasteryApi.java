package com.xirc.mealmastery.api;

import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.culinary.CulinaryProfile;
import com.xirc.mealmastery.culinary.MealRecord;
import com.xirc.mealmastery.culinary.ProfileManager;
import com.xirc.mealmastery.event.CulinaryEvents;
import com.xirc.mealmastery.mastery.MasteryRank;
import com.xirc.mealmastery.recipe.CookingMethod;
import com.xirc.mealmastery.recipe.CulinaryRegistries;
import com.xirc.mealmastery.recipe.MealEntry;
import com.xirc.mealmastery.tracking.CulinaryCredit;
import com.xirc.mealmastery.tracking.WorkstationRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;
import java.util.function.Consumer;

/**
 * The public surface other mods may use.
 *
 * <p>Kept deliberately small. Meal Mastery is not a kitchen framework and must
 * never become a mandatory dependency: an addon that does nothing at all is
 * already fully supported, because dishes are detected from recipes. This
 * exists for the two cases generic detection cannot reach — a workstation with
 * genuinely unusual mechanics, and a mod that wants to react to culinary
 * progress.</p>
 *
 * <p>Everything here is server-side and must be called on the server thread.</p>
 */
public final class MealMasteryApi {

    /** Bumped when a method here changes shape. Additions do not bump it. */
    public static final int API_VERSION = 1;

    private MealMasteryApi() {
    }

    // ------------------------------------------------------------ registration

    /**
     * Registers a block as a cooking workstation, so items a player obtains
     * just after using it are credited to them.
     *
     * <p>Only needed for a workstation that does not go through a menu and does
     * not drop its output — Farmer's Delight's own blocks and every addon that
     * reuses them already work.</p>
     */
    public static void registerWorkstation(ResourceLocation blockId, ResourceLocation methodId) {
        WorkstationRegistry.register(blockId, CookingMethod.of(methodId));
    }

    /**
     * Credits a player for producing a dish your mod made in a way Meal Mastery
     * cannot observe.
     *
     * <p>Only call this when a specific player genuinely caused it. Crediting a
     * nearby player, or the owner of an automated machine, is exactly the
     * guessing this mod refuses to do.</p>
     *
     * @param dish   the produced item's id; ignored unless it is a tracked dish
     * @param amount how many were produced
     */
    public static void awardCooking(ServerPlayer player, ResourceLocation dish, int amount,
                                    ResourceLocation methodId) {
        if (player == null || dish == null || amount <= 0) {
            return;
        }
        com.xirc.mealmastery.progression.ProgressionService.credit(new CulinaryCredit(
                player, dish, amount,
                methodId == null ? CookingMethod.UNKNOWN : CookingMethod.of(methodId),
                CulinaryCredit.Source.API, Set.of(), false));
    }

    // ------------------------------------------------------------------ queries

    /** @return {@code true} when this server has journal entries for the item */
    public static boolean isTrackedDish(ResourceLocation dish) {
        return CulinaryRegistries.current().isTracked(dish);
    }

    /** Every dish this server can make. Immutable and rebuilt on datapack reload. */
    public static Set<ResourceLocation> trackedDishes() {
        return CulinaryRegistries.current().targets();
    }

    /** @return the classified entry, or {@code null} when the dish is not tracked */
    public static MealEntry dish(ResourceLocation dish) {
        return CulinaryRegistries.current().entry(dish);
    }

    public static boolean hasDiscovered(ServerPlayer player, ResourceLocation dish) {
        CulinaryProfile profile = profileOf(player);
        return profile != null && profile.isDiscovered(dish);
    }

    public static MasteryRank masteryOf(ServerPlayer player, ResourceLocation dish) {
        CulinaryProfile profile = profileOf(player);
        return profile == null ? MasteryRank.UNFAMILIAR
                : profile.rankOf(dish, ConfigManager.server().mastery.toCurve());
    }

    public static long timesPrepared(ServerPlayer player, ResourceLocation dish) {
        CulinaryProfile profile = profileOf(player);
        if (profile == null) {
            return 0L;
        }
        MealRecord record = profile.peekMeal(dish);
        return record == null ? 0L : record.prepared();
    }

    public static int cookingLevel(ServerPlayer player) {
        CulinaryProfile profile = profileOf(player);
        return profile == null ? 1
                : profile.cookingLevel(ConfigManager.server().progression.toCurve());
    }

    // ------------------------------------------------------------------- events

    public static void onMealPrepared(Consumer<CulinaryEvents.MealPrepared> listener) {
        CulinaryEvents.MEAL_PREPARED.register(listener);
    }

    public static void onRecipeDiscovered(Consumer<CulinaryEvents.RecipeDiscovered> listener) {
        CulinaryEvents.RECIPE_DISCOVERED.register(listener);
    }

    public static void onMasteryChanged(Consumer<CulinaryEvents.MasteryRankChanged> listener) {
        CulinaryEvents.MASTERY_RANK_CHANGED.register(listener);
    }

    public static void onCookingLevelChanged(Consumer<CulinaryEvents.CookingLevelChanged> listener) {
        CulinaryEvents.COOKING_LEVEL_CHANGED.register(listener);
    }

    public static void onChallengeCompleted(Consumer<CulinaryEvents.ChallengeCompleted> listener) {
        CulinaryEvents.CHALLENGE_COMPLETED.register(listener);
    }

    private static CulinaryProfile profileOf(ServerPlayer player) {
        ProfileManager manager = ProfileManager.get();
        return manager == null || player == null ? null : manager.of(player);
    }
}
