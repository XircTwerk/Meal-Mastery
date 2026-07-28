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
        assertEquals(MasteryRank.UNFAMILIAR, curve.rankFor(1));
        assertEquals(MasteryRank.NOVICE, curve.rankFor(2));
        assertEquals(MasteryRank.NOVICE, curve.rankFor(19));
        assertEquals(MasteryRank.FAMILIAR, curve.rankFor(20));
        assertEquals(MasteryRank.SKILLED, curve.rankFor(60));
        assertEquals(MasteryRank.EXPERT, curve.rankFor(150));
        assertEquals(MasteryRank.MASTERED, curve.rankFor(300));
    }

    @Test
    void masteryDoesNotRegressPastTheTopRank() {
        MasteryCurve curve = MasteryCurve.DEFAULT;
        assertEquals(MasteryRank.MASTERED, curve.rankFor(300));
        assertEquals(MasteryRank.MASTERED, curve.rankFor(10_000));
        // Mastery has to be worth the walk: 300 preparations, not 150.
        assertEquals(-1L, curve.pointsForNextRank(10_000));
    }

    @Test
    void progressIsMeasuredInsideTheCurrentRank() {
        MasteryCurve curve = MasteryCurve.DEFAULT;
        // 40 points: Familiar starts at 20, Skilled at 60.
        assertEquals(MasteryRank.FAMILIAR, curve.rankFor(40));
        assertEquals(20L, curve.progressIntoRank(40));
        assertEquals(40L, curve.pointsForNextRank(40));
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
        long earned = 80L;
        assertEquals(MasteryRank.SKILLED, MasteryCurve.DEFAULT.rankFor(earned));
        MasteryCurve stricter = new MasteryCurve(List.of(1, 10, 100, 200, 500, 1000));
        assertEquals(MasteryRank.FAMILIAR, stricter.rankFor(earned));
    }
}
