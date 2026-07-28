package com.xirc.mealmastery.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Small helpers shared by every persisted record.
 *
 * <p>Two conventions are enforced here rather than repeated everywhere:
 * identifiers are always stored as their canonical string form, and absent
 * values are simply omitted so an untouched profile stays tiny.</p>
 */
public final class NbtUtil {
    private NbtUtil() {
    }

    public static void putIfNonZero(CompoundTag tag, String key, long value) {
        if (value != 0L) {
            tag.putLong(key, value);
        }
    }

    public static void putIfNonZero(CompoundTag tag, String key, int value) {
        if (value != 0) {
            tag.putInt(key, value);
        }
    }

    public static void putIfTrue(CompoundTag tag, String key, boolean value) {
        if (value) {
            tag.putBoolean(key, true);
        }
    }

    public static void putIfPresent(CompoundTag tag, String key, ResourceLocation value) {
        if (value != null) {
            tag.putString(key, value.toString());
        }
    }

    /**
     * Reads an identifier without ever throwing: malformed ids in a hand-edited
     * or corrupted profile are dropped rather than taking the whole file down.
     */
    public static ResourceLocation readId(CompoundTag tag, String key) {
        if (!tag.contains(key, Tag.TAG_STRING)) {
            return null;
        }
        return ResourceLocation.tryParse(tag.getString(key));
    }

    public static ListTag writeIds(Collection<ResourceLocation> ids) {
        ListTag list = new ListTag();
        for (ResourceLocation id : ids) {
            list.add(StringTag.valueOf(id.toString()));
        }
        return list;
    }

    public static Set<ResourceLocation> readIdSet(CompoundTag tag, String key) {
        Set<ResourceLocation> result = new LinkedHashSet<>();
        forEachId(tag, key, result::add);
        return result;
    }

    public static void forEachId(CompoundTag tag, String key, Consumer<ResourceLocation> consumer) {
        if (!tag.contains(key, Tag.TAG_LIST)) {
            return;
        }
        ListTag list = tag.getList(key, Tag.TAG_STRING);
        for (int i = 0; i < list.size(); i++) {
            ResourceLocation id = ResourceLocation.tryParse(list.getString(i));
            if (id != null) {
                consumer.accept(id);
            }
        }
    }

    /**
     * Writes a keyed map of sub-compounds, skipping entries that serialise to
     * nothing so empty records never reach disk.
     */
    public static <T> CompoundTag writeMap(java.util.Map<ResourceLocation, T> map,
                                           java.util.function.Function<T, CompoundTag> writer) {
        CompoundTag out = new CompoundTag();
        map.forEach((id, value) -> {
            CompoundTag entry = writer.apply(value);
            if (!entry.isEmpty()) {
                out.put(id.toString(), entry);
            }
        });
        return out;
    }

    public static <T> void readMap(CompoundTag tag, String key,
                                   java.util.Map<ResourceLocation, T> target,
                                   java.util.function.BiFunction<ResourceLocation, CompoundTag, T> reader) {
        if (!tag.contains(key, Tag.TAG_COMPOUND)) {
            return;
        }
        CompoundTag source = tag.getCompound(key);
        for (String rawId : source.getAllKeys()) {
            ResourceLocation id = ResourceLocation.tryParse(rawId);
            if (id == null) {
                continue;
            }
            target.put(id, reader.apply(id, source.getCompound(rawId)));
        }
    }
}
