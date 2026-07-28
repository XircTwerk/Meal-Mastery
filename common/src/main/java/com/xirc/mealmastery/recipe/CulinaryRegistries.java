package com.xirc.mealmastery.recipe;

import com.xirc.mealmastery.MealMasteryLog;
import net.minecraft.core.RegistryAccess;
import net.minecraft.world.item.crafting.Recipe;

import java.util.Collection;
import java.util.List;

/**
 * Holds the server's current {@link CulinaryRegistry}.
 *
 * <p>Swapped atomically at the end of a recipe reload so a datapack reload
 * mid-session never exposes a half-built registry to a journal that is already
 * open. If a rebuild throws, the previous registry stays in place rather
 * than the server losing its journal entirely.</p>
 */
public final class CulinaryRegistries {
    private static volatile CulinaryRegistry current = CulinaryRegistry.EMPTY;

    private CulinaryRegistries() {
    }

    public static CulinaryRegistry current() {
        return current;
    }

    public static void rebuild(Collection<? extends Recipe<?>> recipes,
                               RegistryAccess registryAccess,
                               EligibilityRules rules,
                               List<FoodCategory> dishCategories) {
        try {
            current = CulinaryRegistry.build(recipes, registryAccess, rules, dishCategories);
        } catch (RuntimeException | LinkageError failure) {
            MealMasteryLog.LOGGER.error(
                    "Failed to rebuild the culinary registry. Keeping the previous one with {} "
                            + "dishes; cooking is unaffected.", current.size(), failure);
        }
    }

    /** Called on server shutdown so a singleplayer world does not leak into the next one. */
    public static void clear() {
        current = CulinaryRegistry.EMPTY;
    }
}
