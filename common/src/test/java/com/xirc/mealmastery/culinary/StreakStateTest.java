package com.xirc.mealmastery.culinary;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreakStateTest {
    private static final ResourceLocation SOUP = new ResourceLocation("farmersdelight", "onion_soup");
    private static final ResourceLocation STEW = new ResourceLocation("farmersdelight", "beef_stew");
    private static final ResourceLocation SALAD = new ResourceLocation("farmersdelight", "mixed_salad");

    @Test
    void cookingOnConsecutiveDaysExtendsTheStreak() {
        StreakState state = new StreakState();
        assertEquals(1, state.recordCookingDay(10L, 1));
        assertEquals(2, state.recordCookingDay(11L, 1));
        assertEquals(3, state.recordCookingDay(12L, 1));
    }

    @Test
    void cookingTwiceInOneDayDoesNotDoubleCount() {
        StreakState state = new StreakState();
        state.recordCookingDay(10L, 1);
        assertEquals(1, state.recordCookingDay(10L, 1));
    }

    @Test
    void graceWindowKeepsTheStreakAliveAcrossAnIdleDay() {
        StreakState state = new StreakState();
        state.recordCookingDay(10L, 2);
        assertEquals(2, state.recordCookingDay(12L, 2), "one idle day is forgiven");
        assertEquals(1, state.recordCookingDay(20L, 2), "a long gap starts over");
    }

    @Test
    void varietyStreakCountsDistinctConsecutiveDishes() {
        StreakState state = new StreakState();
        assertEquals(1, state.recordVariety(SOUP));
        assertEquals(2, state.recordVariety(STEW));
        assertEquals(3, state.recordVariety(SALAD));
    }

    @Test
    void repeatingADishRestartsTheVarietyRun() {
        StreakState state = new StreakState();
        state.recordVariety(SOUP);
        state.recordVariety(STEW);
        state.recordVariety(SALAD);
        assertEquals(1, state.recordVariety(STEW), "the repeat becomes the new run");
        assertEquals(2, state.recordVariety(SOUP));
    }

    @Test
    void dailyUniqueMealsResetWithTheDay() {
        StreakState state = new StreakState();
        state.recordDailyMeal(5L, SOUP);
        assertEquals(2, state.recordDailyMeal(5L, STEW));
        assertEquals(2, state.recordDailyMeal(5L, SOUP), "repeats do not add uniqueness");
        assertEquals(3, state.mealsToday());

        assertEquals(1, state.recordDailyMeal(6L, SOUP));
        assertEquals(1, state.mealsToday());
    }

    @Test
    void roundTripsThroughNbt() {
        StreakState state = new StreakState();
        state.recordCookingDay(10L, 1);
        state.recordCookingDay(11L, 1);
        state.recordVariety(SOUP);
        state.recordVariety(STEW);
        state.recordDiscoveryDay(11L, 1);
        state.recordDailyMeal(11L, SOUP);

        StreakState loaded = StreakState.load(state.save());
        assertEquals(2, loaded.cookingStreak());
        assertEquals(2, loaded.varietyStreak());
        assertEquals(1, loaded.discoveryStreak());
        assertEquals(1, loaded.uniqueMealsToday());
        // The restored variety window still knows what is already in the run.
        assertEquals(1, loaded.recordVariety(SOUP));
    }
}
