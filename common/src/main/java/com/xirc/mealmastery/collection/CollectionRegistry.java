package com.xirc.mealmastery.collection;

import com.xirc.mealmastery.MealMasteryLog;
import com.xirc.mealmastery.platform.Services;
import com.xirc.mealmastery.recipe.CulinaryRegistry;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Datapack collections, plus the per-mod collections generated automatically.
 *
 * <p>The automatic ones are the reason installing another food expansion makes
 * the journal grow on its own: every mod contributing at least one dish
 * gets a collection with no authoring at all.</p>
 */
public final class CollectionRegistry {

    public static final CollectionRegistry EMPTY = new CollectionRegistry(Map.of(), List.of());

    private static volatile CollectionRegistry current = EMPTY;

    private final Map<ResourceLocation, CollectionDefinition> defined;
    private final List<ModCollection> modCollections;

    /**
     * @param modId       the source namespace
     * @param displayName the loader's name for that mod, e.g. "Nether's Delight"
     */
    public record ModCollection(String modId, String displayName, List<ResourceLocation> members) {
        public ModCollection {
            members = List.copyOf(members);
        }
    }

    private CollectionRegistry(Map<ResourceLocation, CollectionDefinition> defined,
                               List<ModCollection> modCollections) {
        this.defined = Map.copyOf(defined);
        this.modCollections = List.copyOf(modCollections);
    }

    public static CollectionRegistry current() {
        return current;
    }

    public static void clear() {
        current = EMPTY;
    }

    public static void rebuild(Collection<CollectionDefinition> definitions,
                               CulinaryRegistry culinary) {
        Map<ResourceLocation, CollectionDefinition> byId = new LinkedHashMap<>();
        for (CollectionDefinition definition : definitions) {
            byId.put(definition.id(), definition);
        }

        List<ModCollection> mods = new ArrayList<>();
        for (String modId : culinary.sourceMods()) {
            List<ResourceLocation> members = culinary.byMod(modId).stream()
                    .map(entry -> entry.target())
                    .toList();
            if (members.isEmpty()) {
                continue;
            }
            mods.add(new ModCollection(modId, Services.PLATFORM.modDisplayName(modId), members));
        }
        mods.sort((left, right) -> left.displayName().compareToIgnoreCase(right.displayName()));

        current = new CollectionRegistry(byId, mods);
        MealMasteryLog.LOGGER.info("Loaded {} collection(s) and generated {} per-mod collection(s)",
                byId.size(), mods.size());
    }

    public CollectionDefinition get(ResourceLocation id) {
        return defined.get(id);
    }

    public Collection<CollectionDefinition> defined() {
        return defined.values();
    }

    public List<ModCollection> modCollections() {
        return modCollections;
    }
}
