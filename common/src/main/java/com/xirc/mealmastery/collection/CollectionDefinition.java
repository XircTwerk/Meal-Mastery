package com.xirc.mealmastery.collection;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.xirc.mealmastery.challenge.ChallengeParseException;
import com.xirc.mealmastery.culinary.CulinaryProfile;
import com.xirc.mealmastery.mastery.MasteryCurve;
import com.xirc.mealmastery.recipe.CulinaryRegistry;
import com.xirc.mealmastery.recipe.MealEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A themed group of dishes defined by a datapack.
 *
 * <p>Membership is expressed the same four ways a pack already thinks in —
 * items, tags, mod ids and categories — and resolved against whatever is
 * actually installed, so a collection that mentions a dish from an absent mod
 * is simply smaller rather than impossible.</p>
 *
 * @param icon an existing item used as the collection's icon; no custom art
 */
public record CollectionDefinition(ResourceLocation id,
                                   List<ResourceLocation> items,
                                   List<ResourceLocation> tags,
                                   List<String> mods,
                                   List<ResourceLocation> categories,
                                   ResourceLocation icon) {

    public CollectionDefinition {
        items = List.copyOf(items);
        tags = List.copyOf(tags);
        mods = List.copyOf(mods);
        categories = List.copyOf(categories);
    }

    public String titleKey() {
        return "mealmastery.collection." + id.getNamespace() + "." + id.getPath();
    }

    /** Dishes in this collection that the server actually has recipes for. */
    public Set<ResourceLocation> resolve(CulinaryRegistry registry) {
        Set<ResourceLocation> members = new LinkedHashSet<>();
        for (ResourceLocation item : items) {
            if (registry.isTracked(item)) {
                members.add(item);
            }
        }
        for (String mod : mods) {
            for (MealEntry entry : registry.byMod(mod)) {
                members.add(entry.target());
            }
        }
        for (ResourceLocation category : categories) {
            for (MealEntry entry : registry.byCategory(category)) {
                members.add(entry.target());
            }
        }
        for (ResourceLocation tagId : tags) {
            TagKey<Item> tag = TagKey.create(Registries.ITEM, tagId);
            for (MealEntry entry : registry.entries()) {
                if (new ItemStack(entry.item()).is(tag)) {
                    members.add(entry.target());
                }
            }
        }
        return members;
    }

    /**
     * @param total      dishes in the collection that this server can make
     * @param discovered how many the player has found
     * @param mastered   how many the player has taken to the top rank
     */
    public record Progress(int total, int discovered, int mastered) {
        public boolean isComplete() {
            return total > 0 && discovered == total;
        }

        public float discoveredFraction() {
            return total == 0 ? 0.0F : (float) discovered / total;
        }
    }

    public Progress progress(CulinaryRegistry registry, CulinaryProfile profile,
                             MasteryCurve curve) {
        Set<ResourceLocation> members = resolve(registry);
        int discovered = 0;
        int mastered = 0;
        for (ResourceLocation member : members) {
            if (profile.isDiscovered(member)) {
                discovered++;
            }
            if (profile.rankOf(member, curve).isMastered()) {
                mastered++;
            }
        }
        return new Progress(members.size(), discovered, mastered);
    }

    public static CollectionDefinition fromJson(ResourceLocation id, JsonObject json) {
        List<ResourceLocation> items = ids(json, "items");
        List<ResourceLocation> tags = ids(json, "tags");
        List<ResourceLocation> categories = ids(json, "categories");
        List<String> mods = strings(json, "mods");

        if (items.isEmpty() && tags.isEmpty() && categories.isEmpty() && mods.isEmpty()) {
            throw new ChallengeParseException(
                    "collection lists no items, tags, categories or mods");
        }

        ResourceLocation icon = null;
        if (json.has("icon")) {
            icon = ResourceLocation.tryParse(json.get("icon").getAsString());
            if (icon == null) {
                throw new ChallengeParseException("\"icon\" is not a valid item id");
            }
        }
        // Fall back to the first concrete member so the journal always has
        // something real to render.
        if (icon == null && !items.isEmpty()) {
            icon = items.get(0);
        }
        if (icon != null && !BuiltInRegistries.ITEM.containsKey(icon)) {
            icon = null;
        }
        return new CollectionDefinition(id, items, tags, mods, categories, icon);
    }

    private static List<ResourceLocation> ids(JsonObject json, String key) {
        List<ResourceLocation> result = new ArrayList<>();
        for (String raw : strings(json, key)) {
            ResourceLocation id = ResourceLocation.tryParse(raw);
            if (id == null) {
                throw new ChallengeParseException(
                        "\"" + key + "\" contains an unparseable id: \"" + raw + "\"");
            }
            result.add(id);
        }
        return result;
    }

    private static List<String> strings(JsonObject json, String key) {
        if (!json.has(key) || !json.get(key).isJsonArray()) {
            return List.of();
        }
        JsonArray array = json.getAsJsonArray(key);
        List<String> result = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            result.add(array.get(i).getAsString());
        }
        return result;
    }
}
