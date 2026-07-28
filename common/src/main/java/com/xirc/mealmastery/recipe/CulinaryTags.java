package com.xirc.mealmastery.recipe;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Tag identifiers Meal Mastery consults, declared by id rather than by
 * referencing anyone's code.
 *
 * <p>Every tag here is optional. A tag that no installed mod populates simply
 * matches nothing, which is why the same constant list works whether Farmer's
 * Delight, its Forge {@code forge:} tags or the Fabric port's {@code c:} tags
 * are the ones present — the audit found the Fabric build ships both
 * namespaces, the Forge build only {@code forge:}.</p>
 */
public final class CulinaryTags {
    private CulinaryTags() {
    }

    public static TagKey<Item> item(String namespace, String path) {
        return TagKey.create(Registries.ITEM, new ResourceLocation(namespace, path));
    }

    public static TagKey<Block> block(String namespace, String path) {
        return TagKey.create(Registries.BLOCK, new ResourceLocation(namespace, path));
    }

    // ---- dish classification (Farmer's Delight ships all of these) --------
    public static final TagKey<Item> MEALS = item("farmersdelight", "meals");
    public static final TagKey<Item> FEASTS = item("farmersdelight", "feasts");
    public static final TagKey<Item> SNACKS = item("farmersdelight", "snacks");
    public static final TagKey<Item> SWEETS = item("farmersdelight", "sweets");
    public static final TagKey<Item> DRINKS = item("farmersdelight", "drinks");
    public static final TagKey<Item> PIES = item("farmersdelight", "pies");
    public static final TagKey<Item> SERVING_CONTAINERS = item("farmersdelight", "serving_containers");

    public static final TagKey<Block> FEAST_BLOCKS = block("farmersdelight", "feasts");
}
