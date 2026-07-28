package com.xirc.mealmastery.culinary;

import com.xirc.mealmastery.data.NbtUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Daily and variety streaks.
 *
 * <p>Streaks are forgiving by design: a cooking streak only breaks when a full
 * grace window has passed with no cooking at all, and breaking one costs
 * nothing except the counter. The design is explicit that players must
 * never be punished harshly for missing a day.</p>
 */
public final class StreakState {
    private static final long NO_DAY = Long.MIN_VALUE;

    private int cookingStreak;
    private long lastCookedDay = NO_DAY;

    private int varietyStreak;
    /** The consecutive dishes making up the current variety run, oldest first. */
    private final Deque<ResourceLocation> varietyWindow = new ArrayDeque<>();

    private int discoveryStreak;
    private long lastDiscoveryDay = NO_DAY;

    /** Distinct dishes prepared on {@link #uniqueDay}; feeds the "most diverse day" record. */
    private long uniqueDay = NO_DAY;
    private final Set<ResourceLocation> uniqueMealsToday = new LinkedHashSet<>();
    private int mealsToday;

    public int cookingStreak() {
        return cookingStreak;
    }

    public int varietyStreak() {
        return varietyStreak;
    }

    public int discoveryStreak() {
        return discoveryStreak;
    }

    public int mealsToday() {
        return mealsToday;
    }

    public int uniqueMealsToday() {
        return uniqueMealsToday.size();
    }

    public long lastCookedDay() {
        return lastCookedDay;
    }

    /**
     * @param graceDays how many idle days are tolerated before the streak resets
     * @return the streak after this preparation
     */
    public int recordCookingDay(long day, int graceDays) {
        if (lastCookedDay == NO_DAY) {
            cookingStreak = 1;
        } else if (day == lastCookedDay) {
            return cookingStreak;
        } else if (day - lastCookedDay <= Math.max(1, graceDays)) {
            cookingStreak++;
        } else {
            cookingStreak = 1;
        }
        lastCookedDay = day;
        return cookingStreak;
    }

    /**
     * Advances the variety streak. Preparing a dish already inside the current
     * run restarts the run from that dish.
     *
     * @return the streak after this preparation
     */
    public int recordVariety(ResourceLocation meal) {
        if (meal == null) {
            return varietyStreak;
        }
        if (varietyWindow.contains(meal)) {
            varietyWindow.clear();
        }
        varietyWindow.addLast(meal);
        varietyStreak = varietyWindow.size();
        return varietyStreak;
    }

    public int recordDiscoveryDay(long day, int graceDays) {
        if (lastDiscoveryDay == NO_DAY || day - lastDiscoveryDay > Math.max(1, graceDays)) {
            discoveryStreak = 1;
        } else if (day != lastDiscoveryDay) {
            discoveryStreak++;
        }
        lastDiscoveryDay = day;
        return discoveryStreak;
    }

    /** @return the number of distinct dishes prepared today after this preparation */
    public int recordDailyMeal(long day, ResourceLocation meal) {
        if (uniqueDay != day) {
            uniqueDay = day;
            uniqueMealsToday.clear();
            mealsToday = 0;
        }
        mealsToday++;
        if (meal != null) {
            uniqueMealsToday.add(meal);
        }
        return uniqueMealsToday.size();
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        NbtUtil.putIfNonZero(tag, "cookingStreak", cookingStreak);
        if (lastCookedDay != NO_DAY) {
            tag.putLong("lastCookedDay", lastCookedDay);
        }
        NbtUtil.putIfNonZero(tag, "varietyStreak", varietyStreak);
        if (!varietyWindow.isEmpty()) {
            tag.put("varietyWindow", NbtUtil.writeIds(varietyWindow));
        }
        NbtUtil.putIfNonZero(tag, "discoveryStreak", discoveryStreak);
        if (lastDiscoveryDay != NO_DAY) {
            tag.putLong("lastDiscoveryDay", lastDiscoveryDay);
        }
        if (uniqueDay != NO_DAY) {
            tag.putLong("uniqueDay", uniqueDay);
            NbtUtil.putIfNonZero(tag, "mealsToday", mealsToday);
            if (!uniqueMealsToday.isEmpty()) {
                tag.put("uniqueMealsToday", NbtUtil.writeIds(uniqueMealsToday));
            }
        }
        return tag;
    }

    public static StreakState load(CompoundTag tag) {
        StreakState state = new StreakState();
        state.cookingStreak = tag.getInt("cookingStreak");
        state.lastCookedDay = tag.contains("lastCookedDay") ? tag.getLong("lastCookedDay") : NO_DAY;
        state.varietyStreak = tag.getInt("varietyStreak");
        NbtUtil.forEachId(tag, "varietyWindow", state.varietyWindow::addLast);
        state.discoveryStreak = tag.getInt("discoveryStreak");
        state.lastDiscoveryDay = tag.contains("lastDiscoveryDay")
                ? tag.getLong("lastDiscoveryDay") : NO_DAY;
        state.uniqueDay = tag.contains("uniqueDay") ? tag.getLong("uniqueDay") : NO_DAY;
        state.mealsToday = tag.getInt("mealsToday");
        NbtUtil.forEachId(tag, "uniqueMealsToday", state.uniqueMealsToday::add);
        return state;
    }
}
