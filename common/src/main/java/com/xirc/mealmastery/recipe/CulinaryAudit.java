package com.xirc.mealmastery.recipe;

import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The report behind {@code /mealmastery audit}.
 *
 * <p>Its real job is making addon compatibility debuggable: if a pack's food is
 * missing from the journal, this says whether it was never seen, excluded by
 * configuration, or accepted but uncategorised.</p>
 *
 * @param detectedFoodItems  distinct dishes that earned a journal page
 * @param eligibleRecipes    recipes accepted into the registry
 * @param ignoredRecipes     recipes that produce nothing edible
 * @param excludedRecipes    recipes rejected by configuration
 * @param failedRecipes      recipes that threw while being inspected
 * @param recipesPerMethod   accepted recipe counts per cooking method
 * @param sourceMods         mods contributing at least one dish
 * @param uncategorised      dishes no category definition matched
 */
public record CulinaryAudit(int detectedFoodItems,
                            int eligibleRecipes,
                            int ignoredRecipes,
                            int excludedRecipes,
                            int failedRecipes,
                            Map<CookingMethod, Integer> recipesPerMethod,
                            Set<String> sourceMods,
                            List<ResourceLocation> uncategorised) {

    public static final CulinaryAudit EMPTY = new CulinaryAudit(
            0, 0, 0, 0, 0, Map.of(), Set.of(), List.of());

    public CulinaryAudit {
        recipesPerMethod = Map.copyOf(recipesPerMethod);
        sourceMods = Set.copyOf(sourceMods);
        uncategorised = List.copyOf(uncategorised);
    }

    public int totalRecipesInspected() {
        return eligibleRecipes + ignoredRecipes + excludedRecipes + failedRecipes;
    }
}
