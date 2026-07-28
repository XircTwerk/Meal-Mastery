package com.xirc.mealmastery.culinary;

import com.xirc.mealmastery.data.NbtUtil;
import com.xirc.mealmastery.recipe.CookingMethod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * One ingredient the player has actually cooked with.
 *
 * <p>Discovery is driven by preparation, never by browsing a recipe viewer
 *, so an entry only exists once the item has genuinely been consumed by
 * an eligible recipe.</p>
 */
public final class IngredientRecord {
    private static final long NO_DAY = Long.MIN_VALUE;

    private final ResourceLocation item;
    private long timesUsed;
    private long firstUsedDay = NO_DAY;
    private long lastUsedDay = NO_DAY;
    private final Set<ResourceLocation> usedInMeals = new LinkedHashSet<>();
    private final Set<CookingMethod> methods = new LinkedHashSet<>();

    public IngredientRecord(ResourceLocation item) {
        this.item = item;
    }

    public ResourceLocation item() {
        return item;
    }

    public long timesUsed() {
        return timesUsed;
    }

    public long firstUsedDay() {
        return firstUsedDay;
    }

    public long lastUsedDay() {
        return lastUsedDay;
    }

    public Set<ResourceLocation> usedInMeals() {
        return Collections.unmodifiableSet(usedInMeals);
    }

    public Set<CookingMethod> methods() {
        return Collections.unmodifiableSet(methods);
    }

    /** @return {@code true} the first time this ingredient is ever used. */
    public boolean recordUse(long day, int amount, ResourceLocation meal, CookingMethod method) {
        boolean first = timesUsed == 0L;
        timesUsed += Math.max(1, amount);
        if (firstUsedDay == NO_DAY) {
            firstUsedDay = day;
        }
        lastUsedDay = day;
        if (meal != null) {
            usedInMeals.add(meal);
        }
        if (method != null) {
            methods.add(method);
        }
        return first;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        NbtUtil.putIfNonZero(tag, "used", timesUsed);
        if (firstUsedDay != NO_DAY) {
            tag.putLong("firstUsedDay", firstUsedDay);
        }
        if (lastUsedDay != NO_DAY) {
            tag.putLong("lastUsedDay", lastUsedDay);
        }
        if (!usedInMeals.isEmpty()) {
            tag.put("meals", NbtUtil.writeIds(usedInMeals));
        }
        if (!methods.isEmpty()) {
            tag.put("methods", NbtUtil.writeIds(methods.stream().map(CookingMethod::id).toList()));
        }
        return tag;
    }

    public static IngredientRecord load(ResourceLocation item, CompoundTag tag) {
        IngredientRecord record = new IngredientRecord(item);
        record.timesUsed = tag.getLong("used");
        record.firstUsedDay = tag.contains("firstUsedDay") ? tag.getLong("firstUsedDay") : NO_DAY;
        record.lastUsedDay = tag.contains("lastUsedDay") ? tag.getLong("lastUsedDay") : NO_DAY;
        NbtUtil.forEachId(tag, "meals", record.usedInMeals::add);
        NbtUtil.forEachId(tag, "methods", id -> record.methods.add(CookingMethod.of(id)));
        return record;
    }
}
