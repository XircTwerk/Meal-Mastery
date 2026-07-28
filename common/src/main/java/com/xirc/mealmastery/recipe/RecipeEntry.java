package com.xirc.mealmastery.recipe;

import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * One classified recipe.
 *
 * <p>Several of these can point at the same {@link #masteryTarget()}: Farmer's
 * Delight alone ships three ways to get a beef patty (crafting, smoking,
 * campfire), and the design is explicit that those must not become three
 * journal pages. Method usage is still tracked per entry.</p>
 *
 * @param recipeId     the recipe's registry id
 * @param recipeTypeId the recipe type's registry id
 * @param method       the cooking method this recipe represents
 * @param masteryTarget the output item id that owns the mastery track
 * @param outputCount  how many items one completion produces
 * @param ingredients  one entry per ingredient slot
 * @param sourceModId  namespace of the output item, used for the journal's source label
 */
public record RecipeEntry(ResourceLocation recipeId,
                          ResourceLocation recipeTypeId,
                          CookingMethod method,
                          ResourceLocation masteryTarget,
                          int outputCount,
                          List<IngredientSlot> ingredients,
                          String sourceModId) {

    public RecipeEntry {
        ingredients = List.copyOf(ingredients);
    }

    /**
     * The items that satisfy one ingredient slot.
     *
     * <p>Vanilla does not expose whether an {@code Ingredient} was written as a
     * tag or as a literal item without reflection, so "is this a variant slot"
     * is answered by the only thing that actually matters to the player: does
     * the slot accept more than one item.</p>
     *
     * @param items every item accepted here, in registry order
     */
    public record IngredientSlot(List<ResourceLocation> items) {
        public IngredientSlot {
            items = List.copyOf(items);
        }

        public boolean accepts(ResourceLocation item) {
            return items.contains(item);
        }

        public boolean hasVariants() {
            return items.size() > 1;
        }
    }
}
