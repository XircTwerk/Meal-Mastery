package com.xirc.mealmastery.culinary;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LevelCurveTest {

    @Test
    void defaultsReproduceTheWorkedExample() {
        // The design's overview mock-up shows level 27 costing ~1,750 XP.
        assertEquals(1738L, LevelCurve.DEFAULT.xpForLevel(27));
    }

    @Test
    void levelIsOneUntilTheFirstLevelIsPaidFor() {
        LevelCurve curve = LevelCurve.DEFAULT;
        assertEquals(1, curve.levelForXp(0L));
        assertEquals(1, curve.levelForXp(curve.xpForLevel(1) - 1));
        assertEquals(2, curve.levelForXp(curve.xpForLevel(1)));
    }

    @Test
    void totalXpAndLevelAreInverses() {
        LevelCurve curve = LevelCurve.DEFAULT;
        for (int level = 1; level <= 40; level++) {
            long total = curve.totalXpForLevel(level);
            assertEquals(level, curve.levelForXp(total), "level " + level);
            assertEquals(0L, curve.xpIntoLevel(total), "progress at level " + level);
        }
    }

    @Test
    void progressIntoLevelTracksTheRemainder() {
        LevelCurve curve = LevelCurve.DEFAULT;
        long total = curve.totalXpForLevel(10) + 42L;
        assertEquals(10, curve.levelForXp(total));
        assertEquals(42L, curve.xpIntoLevel(total));
        assertEquals(curve.xpForLevel(10), curve.xpToNextLevel(total));
    }

    @Test
    void capStopsLevellingWithoutLosingXp() {
        LevelCurve curve = new LevelCurve(100, 63, 0, 5);
        long beyond = curve.totalXpForLevel(5) + 1_000_000L;
        assertEquals(5, curve.levelForXp(beyond));
        assertEquals(-1L, curve.xpToNextLevel(beyond));
        assertTrue(curve.xpIntoLevel(beyond) > 0L, "overflow XP is still counted");
    }

    @Test
    void quadraticTermMakesLaterLevelsCostMore() {
        LevelCurve curve = new LevelCurve(100, 0, 10, 100);
        assertEquals(100L, curve.xpForLevel(1));
        assertEquals(110L, curve.xpForLevel(2));
        assertEquals(140L, curve.xpForLevel(3));
    }

    @Test
    void degenerateConfigurationIsClamped() {
        LevelCurve curve = new LevelCurve(0, -5, -5, 0);
        assertTrue(curve.xpForLevel(1) >= 1L);
        assertEquals(1, curve.maxLevel());
    }
}
