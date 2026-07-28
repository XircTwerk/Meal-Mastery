package com.xirc.mealmastery.recipe;

import com.xirc.mealmastery.Constants;
import net.minecraft.resources.ResourceLocation;

/**
 * A way of preparing food, keyed by the recipe type that produced it.
 *
 * <p>Methods are <em>discovered</em>, not enumerated: whatever recipe
 * types the server ends up with become methods, so an addon's own workstation
 * gets its own statistics page without any code here knowing about it. The
 * constants below exist only so built-in defaults and tests have something to
 * name; none of them is required to be present.</p>
 */
public record CookingMethod(ResourceLocation id) {
    public static final CookingMethod CRAFTING = of("minecraft", "crafting");
    public static final CookingMethod SMELTING = of("minecraft", "smelting");
    public static final CookingMethod SMOKING = of("minecraft", "smoking");
    public static final CookingMethod CAMPFIRE = of("minecraft", "campfire_cooking");
    public static final CookingMethod COOKING_POT = of(Constants.FARMERS_DELIGHT_ID, "cooking");
    public static final CookingMethod CUTTING_BOARD = of(Constants.FARMERS_DELIGHT_ID, "cutting");
    public static final CookingMethod FEAST_SERVING = of(Constants.MOD_ID, "feast_serving");

    /** Assigned when food is credited to a player but the recipe cannot be identified. */
    public static final CookingMethod UNKNOWN = of(Constants.MOD_ID, "unknown");

    public CookingMethod {
        if (id == null) {
            throw new IllegalArgumentException("Cooking method needs an id");
        }
    }

    public static CookingMethod of(String namespace, String path) {
        return new CookingMethod(new ResourceLocation(namespace, path));
    }

    public static CookingMethod of(ResourceLocation id) {
        return new CookingMethod(id);
    }

    /**
     * Vanilla's three crafting recipe types are collapsed into one method:
     * players think of "crafting", not of "shaped versus shapeless".
     */
    public static CookingMethod fromRecipeType(ResourceLocation recipeTypeId) {
        if ("minecraft".equals(recipeTypeId.getNamespace())
                && recipeTypeId.getPath().startsWith("crafting")) {
            return CRAFTING;
        }
        return new CookingMethod(recipeTypeId);
    }

    public String translationKey() {
        return "mealmastery.method." + id.getNamespace() + "." + id.getPath();
    }

    public String sourceModId() {
        return id.getNamespace();
    }

    @Override
    public String toString() {
        return id.toString();
    }
}
