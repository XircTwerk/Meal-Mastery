package com.xirc.mealmastery.recipe;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

import java.util.List;
import java.util.Set;

/**
 * One journal page: a dish, everything known about how to make it, and where
 * it came from.
 *
 * <p>Built once per recipe reload and cached. Nothing here is player-specific —
 * the player's history lives in {@code MealRecord} — so a single instance is
 * shared by every profile on the server.</p>
 *
 * @param target        the output item id; also the mastery key
 * @param item          the resolved item, for rendering its existing icon
 * @param sourceModId   namespace of {@code target}
 * @param recipes       every eligible recipe producing it
 * @param methods       the distinct methods those recipes represent
 * @param ingredients   every item that can appear in any of those recipes
 * @param category      derived from tags, or {@link FoodCategory#UNCATEGORISED}
 * @param nutrition     vanilla food nutrition, or 0 when the item is not edible
 * @param saturation    vanilla saturation modifier
 */
public record MealEntry(ResourceLocation target,
                        Item item,
                        String sourceModId,
                        List<RecipeEntry> recipes,
                        Set<CookingMethod> methods,
                        Set<ResourceLocation> ingredients,
                        FoodCategory category,
                        int nutrition,
                        float saturation) {

    public MealEntry {
        recipes = List.copyOf(recipes);
        methods = Set.copyOf(methods);
        ingredients = Set.copyOf(ingredients);
    }

    /**
     * The method shown as "the" way to make this dish.
     *
     * <p>Ties are broken by recipe order rather than by preferring a particular
     * mod, so nothing here depends on Farmer's Delight being installed.</p>
     */
    public CookingMethod primaryMethod() {
        return recipes.isEmpty() ? CookingMethod.UNKNOWN : recipes.get(0).method();
    }

    public boolean usesMethod(CookingMethod method) {
        return methods.contains(method);
    }

    public boolean usesIngredient(ResourceLocation ingredient) {
        return ingredients.contains(ingredient);
    }

    /** Ingredient slots with real alternatives, which is what variant tracking needs. */
    public List<RecipeEntry.IngredientSlot> variantSlots() {
        return recipes.stream()
                .flatMap(recipe -> recipe.ingredients().stream())
                .filter(RecipeEntry.IngredientSlot::hasVariants)
                .toList();
    }

    public String translationKey() {
        return item.getDescriptionId();
    }
}
