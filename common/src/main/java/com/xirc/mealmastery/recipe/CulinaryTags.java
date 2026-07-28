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
 * matches nothing, so the same constant list works on both loaders. On 1.21
 * that is easy: the {@code forge:} namespace is gone and NeoForge and Fabric
 * both populate the {@code c:} conventional tags, as the audit of the two
 * Farmer's Delight builds confirmed.</p>
 */
public final class CulinaryTags {
    private CulinaryTags() {
    }

    public static TagKey<Item> item(String namespace, String path) {
        return TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath(namespace, path));
    }

    public static TagKey<Block> block(String namespace, String path) {
        return TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath(namespace, path));
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
