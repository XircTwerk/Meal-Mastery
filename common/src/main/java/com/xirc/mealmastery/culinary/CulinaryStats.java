package com.xirc.mealmastery.culinary;

import com.xirc.mealmastery.data.NbtUtil;
import net.minecraft.nbt.CompoundTag;

/**
 * Flat lifetime counters.
 *
 * <p>These are cheap running totals; anything that needs to be derived from the
 * per-meal records (unique recipes, mastered recipes, per-method breakdowns) is
 * computed on demand instead of being duplicated here, so the two can never
 * disagree.</p>
 */
public final class CulinaryStats {
    private long mealsPrepared;
    private long mealsEaten;
    private long portionsServed;
    private long feastsPrepared;
    private long nourishingMealsPrepared;
    private long ingredientUses;

    public long mealsPrepared() {
        return mealsPrepared;
    }

    public long mealsEaten() {
        return mealsEaten;
    }

    public long portionsServed() {
        return portionsServed;
    }

    public long feastsPrepared() {
        return feastsPrepared;
    }

    public long nourishingMealsPrepared() {
        return nourishingMealsPrepared;
    }

    public long ingredientUses() {
        return ingredientUses;
    }

    public void addMealsPrepared(long amount) {
        mealsPrepared += Math.max(0L, amount);
    }

    public void addMealsEaten(long amount) {
        mealsEaten += Math.max(0L, amount);
    }

    public void addPortionsServed(long amount) {
        portionsServed += Math.max(0L, amount);
    }

    public void addFeastsPrepared(long amount) {
        feastsPrepared += Math.max(0L, amount);
    }

    public void addNourishingMealsPrepared(long amount) {
        nourishingMealsPrepared += Math.max(0L, amount);
    }

    public void addIngredientUses(long amount) {
        ingredientUses += Math.max(0L, amount);
    }

    public void reset() {
        mealsPrepared = 0L;
        mealsEaten = 0L;
        portionsServed = 0L;
        feastsPrepared = 0L;
        nourishingMealsPrepared = 0L;
        ingredientUses = 0L;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        NbtUtil.putIfNonZero(tag, "mealsPrepared", mealsPrepared);
        NbtUtil.putIfNonZero(tag, "mealsEaten", mealsEaten);
        NbtUtil.putIfNonZero(tag, "portionsServed", portionsServed);
        NbtUtil.putIfNonZero(tag, "feastsPrepared", feastsPrepared);
        NbtUtil.putIfNonZero(tag, "nourishingMeals", nourishingMealsPrepared);
        NbtUtil.putIfNonZero(tag, "ingredientUses", ingredientUses);
        return tag;
    }

    public static CulinaryStats load(CompoundTag tag) {
        CulinaryStats stats = new CulinaryStats();
        stats.mealsPrepared = tag.getLong("mealsPrepared");
        stats.mealsEaten = tag.getLong("mealsEaten");
        stats.portionsServed = tag.getLong("portionsServed");
        stats.feastsPrepared = tag.getLong("feastsPrepared");
        stats.nourishingMealsPrepared = tag.getLong("nourishingMeals");
        stats.ingredientUses = tag.getLong("ingredientUses");
        return stats;
    }
}
