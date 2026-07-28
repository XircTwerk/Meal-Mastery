package com.xirc.mealmastery.recipe;

import com.xirc.mealmastery.Constants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A dish or ingredient category derived from tags rather than from a hardcoded
 * item list.
 *
 * <p>Definitions are ordered: the first matching definition wins, so a feast
 * is reported as a feast rather than as a meal even though Farmer's Delight
 * puts several items in both tags. Datapacks may prepend their own definitions
 * to override the built-ins entirely.</p>
 *
 * @param id       stable identifier used in configuration and datapacks
 * @param tags     any of these tags matching classifies an item into this category
 * @param priority lower sorts first; datapack definitions default below built-ins
 */
public record FoodCategory(ResourceLocation id, List<TagKey<Item>> tags, int priority) {

    public static final ResourceLocation UNCATEGORISED_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "uncategorised");

    /** Returned when no definition matches; never null, never guessed. */
    public static final FoodCategory UNCATEGORISED =
            new FoodCategory(UNCATEGORISED_ID, List.of(), Integer.MAX_VALUE);

    public static final ResourceLocation OTHER_FOOD_ID =
            ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "other_food");

    /**
     * Fallback for an edible dish no specific definition matched.
     *
     * <p>Most of what a real server detects is like this: vanilla foods and an
     * addon's raw produce carry none of Farmer's Delight's dish tags. Filing
     * them as "Other Food" rather than "Uncategorised" is honest — they really
     * are food — and it keeps the audit's uncategorised count meaningful as a
     * "these matched nothing specific" diagnostic.</p>
     */
    public static final FoodCategory OTHER_FOOD =
            new FoodCategory(OTHER_FOOD_ID, List.of(), Integer.MAX_VALUE - 1);

    public FoodCategory {
        tags = List.copyOf(tags);
    }

    public String translationKey() {
        return "mealmastery.category." + id.getNamespace() + "." + id.getPath();
    }

    public boolean matches(ItemStack stack) {
        for (TagKey<Item> tag : tags) {
            if (stack.is(tag)) {
                return true;
            }
        }
        return false;
    }

    public boolean matches(Item item) {
        return matches(new ItemStack(item));
    }

    @SafeVarargs
    private static FoodCategory builtIn(String path, int priority, TagKey<Item>... tags) {
        return new FoodCategory(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path),
                List.of(tags), priority);
    }

    /**
     * Built-in dish categories, most specific first.
     *
     * <p>Kept small on purpose: these mirror the tags Farmer's Delight already
     * maintains, so an addon that tags its food correctly is categorised with
     * no work at all.</p>
     */
    public static List<FoodCategory> builtInDishCategories() {
        List<FoodCategory> categories = new ArrayList<>();
        categories.add(builtIn("feast", 10, CulinaryTags.FEASTS));
        categories.add(builtIn("pie", 20, CulinaryTags.PIES));
        categories.add(builtIn("drink", 30, CulinaryTags.DRINKS));
        categories.add(builtIn("sweet", 40, CulinaryTags.SWEETS));
        categories.add(builtIn("snack", 50, CulinaryTags.SNACKS));
        categories.add(builtIn("meal", 60, CulinaryTags.MEALS));
        return categories;
    }

    /**
     * Built-in ingredient categories.
     *
     * <p>1.21 retired the {@code forge:} namespace entirely, and both loaders
     * now populate the same {@code c:} conventional tags. That is a real
     * simplification over the 1.20.1 branch, which had to list a Forge and a
     * Fabric spelling for every category.</p>
     */
    public static List<FoodCategory> builtInIngredientCategories() {
        List<FoodCategory> categories = new ArrayList<>();
        categories.add(builtIn("ingredient/fish", 10,
                CulinaryTags.item("c", "foods/raw_fish"),
                CulinaryTags.item("c", "foods/cooked_fish")));
        categories.add(builtIn("ingredient/meat", 20,
                CulinaryTags.item("c", "foods/raw_meat"),
                CulinaryTags.item("c", "foods/cooked_meat")));
        categories.add(builtIn("ingredient/dairy", 30,
                CulinaryTags.item("c", "drinks/milk"),
                CulinaryTags.item("c", "buckets/milk")));
        categories.add(builtIn("ingredient/egg", 40,
                CulinaryTags.item("c", "eggs"),
                CulinaryTags.item("c", "foods/cooked_egg")));
        categories.add(builtIn("ingredient/grain", 50,
                CulinaryTags.item("c", "crops/grain"),
                CulinaryTags.item("c", "foods/bread"),
                CulinaryTags.item("c", "foods/dough"),
                CulinaryTags.item("c", "foods/pasta")));
        categories.add(builtIn("ingredient/fruit", 60,
                CulinaryTags.item("c", "foods/fruit"),
                CulinaryTags.item("c", "foods/berry")));
        categories.add(builtIn("ingredient/vegetable", 70,
                CulinaryTags.item("c", "foods/vegetable")));
        categories.add(builtIn("ingredient/crop", 80,
                CulinaryTags.item("c", "crops"),
                CulinaryTags.item("c", "seeds")));
        return categories;
    }
}
