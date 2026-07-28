package com.xirc.mealmastery.progression;

import com.xirc.mealmastery.config.ServerConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class XpEngineTest {

    private static XpEngine.Inputs repeat(int priorToday) {
        return new XpEngine.Inputs(1, priorToday, false, false, 0, 0, 0, 0);
    }

    @Test
    void firstPreparationOfTheDayPaysInFull() {
        ServerConfig config = new ServerConfig();
        assertEquals(1.0, XpEngine.repetitionMultiplier(0, config));
    }

    @Test
    void repeatedPreparationsFallThroughTheConfiguredTiers() {
        // 1st full, 2nd-5th at 75%, 6th-15th at 50%, 16th+ at the floor.
        ServerConfig config = new ServerConfig();
        assertEquals(1.0, XpEngine.repetitionMultiplier(0, config));
        assertEquals(0.75, XpEngine.repetitionMultiplier(1, config));
        assertEquals(0.75, XpEngine.repetitionMultiplier(4, config));
        assertEquals(0.50, XpEngine.repetitionMultiplier(5, config));
        assertEquals(0.50, XpEngine.repetitionMultiplier(14, config));
        assertEquals(0.20, XpEngine.repetitionMultiplier(15, config));
        assertEquals(0.20, XpEngine.repetitionMultiplier(9_999, config));
    }

    @Test
    void grindingOneDishIsAlwaysWorseThanCookingDifferentOnes() {
        // The balance rule the whole anti-farming system exists for.
        ServerConfig config = new ServerConfig();

        long grind = 0L;
        for (int i = 0; i < 20; i++) {
            grind += XpEngine.compute(repeat(i), config).total();
        }

        // Twenty dishes the player already knows: each is the first of its own
        // per-dish day count, so none of them is discounted.
        long variety = 20 * XpEngine.compute(repeat(0), config).total();

        // Twenty dishes the player has never made before. This is the path the
        // progression priority is actually about.
        long exploration = 20 * XpEngine.compute(
                new XpEngine.Inputs(1, 0, true, false, 0, 0, 0, 0), config).total();

        assertTrue(variety > grind, "variety beats repetition: " + variety + " vs " + grind);
        assertTrue(exploration > grind * 20,
                "discovery should dwarf grinding: " + exploration + " vs " + grind);
    }

    @Test
    void repetitionNeverDropsToZeroByDefault() {
        ServerConfig config = new ServerConfig();
        assertTrue(XpEngine.compute(repeat(1_000), config).total() > 0L,
                "repetition should be worth less, not worthless");
    }

    @Test
    void discoveryBonusIsExemptFromDiminishingReturns() {
        ServerConfig config = new ServerConfig();
        // A first-ever preparation on a heavily repeated day still pays the bonus.
        XpEngine.Result result = XpEngine.compute(
                new XpEngine.Inputs(1, 50, true, false, 0, 0, 0, 0), config);
        assertEquals(config.progression.discoveryBonusXp, result.bonuses());
        assertTrue(result.total() >= config.progression.discoveryBonusXp);
    }

    @Test
    void firstPreparationOutweighsARepeat() {
        // First preparation XP must clearly exceed a repeat.
        ServerConfig config = new ServerConfig();
        long first = XpEngine.compute(
                new XpEngine.Inputs(1, 0, true, false, 0, 0, 0, 0), config).total();
        long repeated = XpEngine.compute(repeat(0), config).total();
        assertTrue(first > repeated * 5, first + " vs " + repeated);
    }

    @Test
    void experimentationIsCappedPerDay() {
        // Ingredient variants must not be farmable.
        ServerConfig config = new ServerConfig();
        config.progression.experimentationDailyCap = 20;
        config.progression.experimentationXp = 8;

        XpEngine.Result fresh = XpEngine.compute(
                new XpEngine.Inputs(1, 0, false, false, 0, 2, 0, 0), config);
        assertEquals(16, fresh.experimentationXpAwarded());

        XpEngine.Result nearCap = XpEngine.compute(
                new XpEngine.Inputs(1, 0, false, false, 0, 2, 16, 0), config);
        assertEquals(4, nearCap.experimentationXpAwarded());

        XpEngine.Result atCap = XpEngine.compute(
                new XpEngine.Inputs(1, 0, false, false, 0, 2, 20, 0), config);
        assertEquals(0, atCap.experimentationXpAwarded());
    }

    @Test
    void masteryImprovesProgressionRatherThanFood() {
        // The safest mastery reward is more Cooking XP, and it is capped.
        ServerConfig config = new ServerConfig();
        assertEquals(1.0, XpEngine.masteryMultiplier(0, config));
        assertEquals(1.05, XpEngine.masteryMultiplier(10, config), 1.0e-9);
        assertEquals(1.0 + config.mastery.xpBonusCap, XpEngine.masteryMultiplier(10_000, config));
    }

    @Test
    void disablingProgressionYieldsNothing() {
        ServerConfig config = new ServerConfig();
        config.progression.enabled = false;
        assertEquals(0L, XpEngine.compute(
                new XpEngine.Inputs(1, 0, true, true, 5, 5, 0, 50), config).total());
        assertEquals(0L, XpEngine.eatingXp(config));
    }

    @Test
    void disablingAntiFarmingRemovesTheCurveEntirely() {
        ServerConfig config = new ServerConfig();
        config.antiFarming.enabled = false;
        assertEquals(1.0, XpEngine.repetitionMultiplier(10_000, config));
    }

    @Test
    void serverMultiplierScalesTheWholeResult() {
        ServerConfig config = new ServerConfig();
        long single = XpEngine.compute(repeat(0), config).total();
        config.progression.xpMultiplier = 3.0;
        assertEquals(single * 3, XpEngine.compute(repeat(0), config).total());
    }

    @Test
    void eatingIsWorthFarLessThanPreparing() {
        // Preparing meals for other players is the thing being rewarded.
        ServerConfig config = new ServerConfig();
        assertTrue(XpEngine.eatingXp(config) < XpEngine.compute(repeat(0), config).total());
    }
}
