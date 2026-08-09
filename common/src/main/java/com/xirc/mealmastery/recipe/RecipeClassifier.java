package com.xirc.mealmastery.recipe;

import com.xirc.mealmastery.MealMasteryLog;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Turns an arbitrary {@link Recipe} into a {@link RecipeEntry}, or explains why
 * it is not a dish.
 *
 * <p>Reads nothing but the vanilla {@code Recipe} interface, which is the whole
 * reason an unknown addon's cooking recipe classifies identically to Farmer's
 * Delight's own.</p>
 *
 * <p>Every call into third-party recipe code is guarded: a modded recipe whose
 * {@code getResultItem} or {@code getIngredients} throws is reported as
 * unclassified and skipped, never allowed to abort the reload.</p>
 */
public final class RecipeClassifier {

    /** Beyond this many accepted items a slot is a wildcard, not a choice worth listing. */
    private static final int MAX_SLOT_ITEMS = 64;

    private final EligibilityRules rules;
    private final RegistryAccess registryAccess;

    public RecipeClassifier(EligibilityRules rules, RegistryAccess registryAccess) {
        this.rules = rules;
        this.registryAccess = registryAccess;
    }

    /**
     * @param verdict   why the recipe was or was not accepted
     * @param entry     present only when {@code verdict} is
     *                  {@link EligibilityRules.Verdict#ELIGIBLE}
     * @param failed    the recipe threw while being inspected
     */
    public record Classification(EligibilityRules.Verdict verdict, RecipeEntry entry, boolean failed) {

        static Classification rejected(EligibilityRules.Verdict verdict) {
            return new Classification(verdict, null, false);
        }

        static Classification failure() {
            return new Classification(EligibilityRules.Verdict.NOT_FOOD, null, true);
        }

        public boolean isEligible() {
            return entry != null;
        }
    }

    public Classification classify(Recipe<?> recipe) {
        ResourceLocation recipeId;
        ResourceLocation recipeTypeId;
        try {
            recipeId = recipe.getId();
            recipeTypeId = BuiltInRegistries.RECIPE_TYPE.getKey(recipe.getType());
        } catch (RuntimeException | LinkageError failure) {
            logFailure(null, failure);
            return Classification.failure();
        }
        if (recipeTypeId == null) {
            return Classification.rejected(EligibilityRules.Verdict.NOT_FOOD);
        }

        ItemStack output;
        try {
            output = recipe.getResultItem(registryAccess);
        } catch (RuntimeException | LinkageError failure) {
            logFailure(recipeId, failure);
            return Classification.failure();
        }

        EligibilityRules.Verdict verdict = rules.evaluate(recipeId, recipeTypeId, output);
        if (verdict != EligibilityRules.Verdict.ELIGIBLE) {
            return Classification.rejected(verdict);
        }

        ResourceLocation target = BuiltInRegistries.ITEM.getKey(output.getItem());
        if (target == null) {
            return Classification.rejected(EligibilityRules.Verdict.NOT_FOOD);
        }

        List<RecipeEntry.IngredientSlot> slots;
        try {
            slots = readIngredients(recipe);
        } catch (RuntimeException | LinkageError failure) {
            logFailure(recipeId, failure);
            // The dish is still real even if its inputs are unreadable, so it
            // keeps its journal page with an empty ingredient list rather than
            // vanishing from the registry.
            slots = List.of();
        }

        if (rules.isUnpacking(recipeTypeId, slots.size(), output.getCount())) {
            return Classification.rejected(EligibilityRules.Verdict.EXCLUDED);
        }

        return new Classification(EligibilityRules.Verdict.ELIGIBLE, new RecipeEntry(
                recipeId,
                recipeTypeId,
                CookingMethod.fromRecipeType(recipeTypeId),
                target,
                Math.max(1, output.getCount()),
                slots,
                target.getNamespace()), false);
    }

    private static List<RecipeEntry.IngredientSlot> readIngredients(Recipe<?> recipe) {
        List<RecipeEntry.IngredientSlot> slots = new ArrayList<>();
        for (Ingredient ingredient : recipe.getIngredients()) {
            if (ingredient == null || ingredient.isEmpty()) {
                continue;
            }
            Set<ResourceLocation> items = new LinkedHashSet<>();
            for (ItemStack stack : ingredient.getItems()) {
                if (stack.isEmpty()) {
                    continue;
                }
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (id != null) {
                    items.add(id);
                }
                if (items.size() >= MAX_SLOT_ITEMS) {
                    break;
                }
            }
            if (!items.isEmpty()) {
                slots.add(new RecipeEntry.IngredientSlot(List.copyOf(items)));
            }
        }
        return slots;
    }

    private static void logFailure(ResourceLocation recipeId, Throwable failure) {
        MealMasteryLog.LOGGER.debug(
                "Skipping recipe {} during culinary classification; it threw while being inspected. "
                        + "The recipe itself is unaffected.",
                recipeId == null ? "<unknown>" : recipeId, failure);
    }
}
