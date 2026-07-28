package com.xirc.mealmastery.mastery;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MasteryCurveTest {

    @Test
    void defaultThresholdsMatchTheDocumentedProgression() {
        MasteryCurve curve = MasteryCurve.DEFAULT;
        assertEquals(MasteryRank.UNFAMILIAR, curve.rankFor(0));
        assertEquals(MasteryRank.NOVICE, curve.rankFor(1));
        assertEquals(MasteryRank.NOVICE, curve.rankFor(9));
        assertEquals(MasteryRank.FAMILIAR, curve.rankFor(10));
        assertEquals(MasteryRank.SKILLED, curve.rankFor(30));
        assertEquals(MasteryRank.EXPERT, curve.rankFor(75));
        assertEquals(MasteryRank.MASTERED, curve.rankFor(150));
    }

    @Test
    void masteryDoesNotRegressPastTheTopRank() {
        MasteryCurve curve = MasteryCurve.DEFAULT;
        assertEquals(MasteryRank.MASTERED, curve.rankFor(150));
        assertEquals(MasteryRank.MASTERED, curve.rankFor(10_000));
        // Mastery has to be worth the walk: 150 preparations, not 75.
        assertEquals(-1L, curve.pointsForNextRank(10_000));
    }

    @Test
    void progressIsMeasuredInsideTheCurrentRank() {
        MasteryCurve curve = MasteryCurve.DEFAULT;
        // 20 points: Familiar starts at 10, Skilled at 30.
        assertEquals(MasteryRank.FAMILIAR, curve.rankFor(20));
        assertEquals(10L, curve.progressIntoRank(20));
        assertEquals(20L, curve.pointsForNextRank(20));
    }

    @Test
    void nonIncreasingThresholdsAreRepaired() {
        MasteryCurve curve = new MasteryCurve(List.of(5, 5, 3));
        assertEquals(List.of(5, 6, 7), curve.thresholds());
    }

    @Test
    void emptyCurveIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new MasteryCurve(List.of()));
    }

    @Test
    void reconfiguringThresholdsOnlyChangesTheDisplayedRank() {
        // Changing configuration must never destroy earned points.
        long earned = 40L;
        assertEquals(MasteryRank.SKILLED, MasteryCurve.DEFAULT.rankFor(earned));
        MasteryCurve stricter = new MasteryCurve(List.of(1, 10, 50, 200, 500, 1000));
        assertEquals(MasteryRank.FAMILIAR, stricter.rankFor(earned));
    }
}
