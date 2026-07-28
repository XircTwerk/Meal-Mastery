package com.xirc.mealmastery.culinary;

import com.xirc.mealmastery.data.NbtUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * Bests worth remembering.
 *
 * <p>Only records that can be maintained honestly from observed events are
 * kept. Anything that would need guessing — "fastest mastery" measured in real
 * time on a server that was offline for a week, for instance — is measured in
 * Minecraft days or left out.</p>
 */
public final class PersonalRecords {
    private static final long NO_DAY = Long.MIN_VALUE;

    private int mostMealsInOneDay;
    private long mostMealsDay = NO_DAY;

    private int mostUniqueMealsInOneDay;
    private long mostUniqueMealsDay = NO_DAY;

    private int longestDiscoveryStreak;
    private int longestVarietyStreak;
    private int longestCookingStreak;

    private ResourceLocation firstMasteredMeal;
    private long firstMasteredDay = NO_DAY;

    /** Days between first preparing a dish and mastering it; the smallest wins. */
    private ResourceLocation fastestMasteredMeal;
    private long fastestMasteryDays = -1L;

    private int mostFeastPortionsServedInOneDay;

    public int mostMealsInOneDay() {
        return mostMealsInOneDay;
    }

    public long mostMealsDay() {
        return mostMealsDay;
    }

    public int mostUniqueMealsInOneDay() {
        return mostUniqueMealsInOneDay;
    }

    public long mostUniqueMealsDay() {
        return mostUniqueMealsDay;
    }

    public int longestDiscoveryStreak() {
        return longestDiscoveryStreak;
    }

    public int longestVarietyStreak() {
        return longestVarietyStreak;
    }

    public int longestCookingStreak() {
        return longestCookingStreak;
    }

    public ResourceLocation firstMasteredMeal() {
        return firstMasteredMeal;
    }

    public long firstMasteredDay() {
        return firstMasteredDay;
    }

    public ResourceLocation fastestMasteredMeal() {
        return fastestMasteredMeal;
    }

    public long fastestMasteryDays() {
        return fastestMasteryDays;
    }

    public int mostFeastPortionsServedInOneDay() {
        return mostFeastPortionsServedInOneDay;
    }

    public void offerMealsInOneDay(int count, long day) {
        if (count > mostMealsInOneDay) {
            mostMealsInOneDay = count;
            mostMealsDay = day;
        }
    }

    public void offerUniqueMealsInOneDay(int count, long day) {
        if (count > mostUniqueMealsInOneDay) {
            mostUniqueMealsInOneDay = count;
            mostUniqueMealsDay = day;
        }
    }

    public void offerDiscoveryStreak(int streak) {
        longestDiscoveryStreak = Math.max(longestDiscoveryStreak, streak);
    }

    public void offerVarietyStreak(int streak) {
        longestVarietyStreak = Math.max(longestVarietyStreak, streak);
    }

    public void offerCookingStreak(int streak) {
        longestCookingStreak = Math.max(longestCookingStreak, streak);
    }

    public void offerFeastPortionsInOneDay(int count) {
        mostFeastPortionsServedInOneDay = Math.max(mostFeastPortionsServedInOneDay, count);
    }

    public void offerMastery(ResourceLocation meal, long day, long daysTaken) {
        if (firstMasteredMeal == null) {
            firstMasteredMeal = meal;
            firstMasteredDay = day;
        }
        if (daysTaken >= 0L && (fastestMasteryDays < 0L || daysTaken < fastestMasteryDays)) {
            fastestMasteryDays = daysTaken;
            fastestMasteredMeal = meal;
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        NbtUtil.putIfNonZero(tag, "mostMealsInOneDay", mostMealsInOneDay);
        if (mostMealsDay != NO_DAY) {
            tag.putLong("mostMealsDay", mostMealsDay);
        }
        NbtUtil.putIfNonZero(tag, "mostUniqueMealsInOneDay", mostUniqueMealsInOneDay);
        if (mostUniqueMealsDay != NO_DAY) {
            tag.putLong("mostUniqueMealsDay", mostUniqueMealsDay);
        }
        NbtUtil.putIfNonZero(tag, "longestDiscoveryStreak", longestDiscoveryStreak);
        NbtUtil.putIfNonZero(tag, "longestVarietyStreak", longestVarietyStreak);
        NbtUtil.putIfNonZero(tag, "longestCookingStreak", longestCookingStreak);
        NbtUtil.putIfPresent(tag, "firstMasteredMeal", firstMasteredMeal);
        if (firstMasteredDay != NO_DAY) {
            tag.putLong("firstMasteredDay", firstMasteredDay);
        }
        NbtUtil.putIfPresent(tag, "fastestMasteredMeal", fastestMasteredMeal);
        if (fastestMasteryDays >= 0L) {
            tag.putLong("fastestMasteryDays", fastestMasteryDays);
        }
        NbtUtil.putIfNonZero(tag, "mostFeastPortionsInOneDay", mostFeastPortionsServedInOneDay);
        return tag;
    }

    public static PersonalRecords load(CompoundTag tag) {
        PersonalRecords records = new PersonalRecords();
        records.mostMealsInOneDay = tag.getInt("mostMealsInOneDay");
        records.mostMealsDay = tag.contains("mostMealsDay") ? tag.getLong("mostMealsDay") : NO_DAY;
        records.mostUniqueMealsInOneDay = tag.getInt("mostUniqueMealsInOneDay");
        records.mostUniqueMealsDay = tag.contains("mostUniqueMealsDay")
                ? tag.getLong("mostUniqueMealsDay") : NO_DAY;
        records.longestDiscoveryStreak = tag.getInt("longestDiscoveryStreak");
        records.longestVarietyStreak = tag.getInt("longestVarietyStreak");
        records.longestCookingStreak = tag.getInt("longestCookingStreak");
        records.firstMasteredMeal = NbtUtil.readId(tag, "firstMasteredMeal");
        records.firstMasteredDay = tag.contains("firstMasteredDay")
                ? tag.getLong("firstMasteredDay") : NO_DAY;
        records.fastestMasteredMeal = NbtUtil.readId(tag, "fastestMasteredMeal");
        records.fastestMasteryDays = tag.contains("fastestMasteryDays")
                ? tag.getLong("fastestMasteryDays") : -1L;
        records.mostFeastPortionsServedInOneDay = tag.getInt("mostFeastPortionsInOneDay");
        return records;
    }
}
