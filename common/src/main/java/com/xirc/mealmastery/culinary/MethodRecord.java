package com.xirc.mealmastery.culinary;

import com.xirc.mealmastery.data.NbtUtil;
import com.xirc.mealmastery.recipe.CookingMethod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Aggregate statistics for one cooking method.
 *
 * <p>Deliberately shallow: a couple of counters and the set of dishes made with
 * it. The design is explicit that this must not turn into a skill tree.</p>
 */
public final class MethodRecord {
    private static final long NO_DAY = Long.MIN_VALUE;

    private final CookingMethod method;
    private long preparations;
    private long firstUsedDay = NO_DAY;
    private final Set<ResourceLocation> uniqueMeals = new LinkedHashSet<>();

    public MethodRecord(CookingMethod method) {
        this.method = method;
    }

    public CookingMethod method() {
        return method;
    }

    public long preparations() {
        return preparations;
    }

    public int uniqueMealCount() {
        return uniqueMeals.size();
    }

    public Set<ResourceLocation> uniqueMeals() {
        return Collections.unmodifiableSet(uniqueMeals);
    }

    public long firstUsedDay() {
        return firstUsedDay;
    }

    /** @return {@code true} the first time this method is ever used. */
    public boolean recordPreparation(long day, int amount, ResourceLocation meal) {
        boolean first = preparations == 0L;
        preparations += Math.max(1, amount);
        if (firstUsedDay == NO_DAY) {
            firstUsedDay = day;
        }
        if (meal != null) {
            uniqueMeals.add(meal);
        }
        return first;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        NbtUtil.putIfNonZero(tag, "preparations", preparations);
        if (firstUsedDay != NO_DAY) {
            tag.putLong("firstUsedDay", firstUsedDay);
        }
        if (!uniqueMeals.isEmpty()) {
            tag.put("meals", NbtUtil.writeIds(uniqueMeals));
        }
        return tag;
    }

    public static MethodRecord load(CookingMethod method, CompoundTag tag) {
        MethodRecord record = new MethodRecord(method);
        record.preparations = tag.getLong("preparations");
        record.firstUsedDay = tag.contains("firstUsedDay") ? tag.getLong("firstUsedDay") : NO_DAY;
        NbtUtil.forEachId(tag, "meals", record.uniqueMeals::add);
        return record;
    }
}
