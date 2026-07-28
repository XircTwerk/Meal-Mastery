package com.xirc.mealmastery.culinary;

import com.xirc.mealmastery.mastery.MasteryCurve;
import com.xirc.mealmastery.mastery.MasteryRank;
import com.xirc.mealmastery.recipe.CookingMethod;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MealRecordTest {
    private static final ResourceLocation STEW = ResourceLocation.fromNamespaceAndPath("farmersdelight", "beef_stew");
    private static final ResourceLocation ONION = ResourceLocation.fromNamespaceAndPath("farmersdelight", "onion");

    @Test
    void perDayCountsResetWithTheDay() {
        // This is what the anti-farming period reads, so it has to roll cleanly.
        MealRecord record = new MealRecord(STEW);
        record.recordPreparation(5L, 3, CookingMethod.COOKING_POT);
        assertEquals(3, record.preparedOnDay(5L));
        assertEquals(0, record.preparedOnDay(6L));

        record.recordPreparation(6L, 1, CookingMethod.COOKING_POT);
        assertEquals(1, record.preparedOnDay(6L));
        assertEquals(4L, record.prepared(), "the lifetime total keeps accumulating");
    }

    @Test
    void bestDayRemembersThePeak() {
        MealRecord record = new MealRecord(STEW);
        record.recordPreparation(1L, 12, CookingMethod.COOKING_POT);
        record.recordPreparation(2L, 3, CookingMethod.COOKING_POT);
        assertEquals(12, record.bestPreparedInOneDay());
    }

    @Test
    void firstPreparedIsSetOnceAndNeverMovesForward() {
        MealRecord record = new MealRecord(STEW);
        record.recordPreparation(47L, 1, CookingMethod.COOKING_POT);
        record.recordPreparation(90L, 1, CookingMethod.COOKING_POT);
        assertEquals(47L, record.firstPreparedDay());
        assertEquals(90L, record.lastPreparedDay());
    }

    @Test
    void discoveryHappensExactlyOnce() {
        MealRecord record = new MealRecord(STEW);
        assertTrue(record.discover(10L));
        assertFalse(record.discover(20L), "a second discovery must not re-fire");
        assertEquals(10L, record.discoveredDay());
    }

    @Test
    void ingredientVariantsAreCountedOnlyTheFirstTime() {
        MealRecord record = new MealRecord(STEW);
        assertTrue(record.recordIngredientVariant(ONION));
        assertFalse(record.recordIngredientVariant(ONION));
        assertEquals(1, record.ingredientVariantsUsed().size());
    }

    @Test
    void rankAnnouncementsAreClaimedOnce() {
        // Stops a mastery notification firing on every preparation at the cap.
        MealRecord record = new MealRecord(STEW);
        assertTrue(record.claimRankAnnouncement(MasteryRank.SKILLED));
        assertFalse(record.claimRankAnnouncement(MasteryRank.SKILLED));
        assertFalse(record.claimRankAnnouncement(MasteryRank.NOVICE),
                "a lower rank must not re-announce");
        assertTrue(record.claimRankAnnouncement(MasteryRank.MASTERED));
    }

    @Test
    void administrativelyLoweringMasteryAlsoLowersTheAnnouncedRank() {
        MealRecord record = new MealRecord(STEW);
        record.addMasteryPoints(200L);
        record.claimRankAnnouncement(record.rank(MasteryCurve.DEFAULT));
        assertEquals(MasteryRank.MASTERED, record.announcedRank());

        record.setMasteryPoints(2L, MasteryCurve.DEFAULT);
        assertEquals(MasteryRank.NOVICE, record.rank(MasteryCurve.DEFAULT));
        assertEquals(MasteryRank.NOVICE, record.announcedRank());
        assertTrue(record.claimRankAnnouncement(MasteryRank.FAMILIAR),
                "earning the rank back announces it again");
    }

    @Test
    void methodCountsAreTrackedSeparatelyFromTheTotal() {
        MealRecord record = new MealRecord(STEW);
        record.recordPreparation(1L, 2, CookingMethod.COOKING_POT);
        record.recordPreparation(1L, 5, CookingMethod.CRAFTING);

        assertEquals(7L, record.prepared());
        assertEquals(2L, record.preparationsByMethod().get(CookingMethod.COOKING_POT));
        assertEquals(5L, record.preparationsByMethod().get(CookingMethod.CRAFTING));
    }

    @Test
    void anUntouchedRecordSerialisesToNothing() {
        assertTrue(new MealRecord(STEW).save().isEmpty());
        assertTrue(new MealRecord(STEW).isEmpty());
    }

    @Test
    void negativeAndZeroAmountsAreIgnored() {
        MealRecord record = new MealRecord(STEW);
        record.recordPreparation(1L, 0, CookingMethod.CRAFTING);
        record.recordPreparation(1L, -5, CookingMethod.CRAFTING);
        record.recordEaten(1L, -2);
        record.recordServed(-3L);
        record.addMasteryPoints(-10L);

        assertEquals(0L, record.prepared());
        assertEquals(0L, record.eaten());
        assertEquals(0L, record.served());
        assertEquals(0L, record.masteryPoints());
    }
}
