package com.xirc.mealmastery.progression;

import com.xirc.mealmastery.config.ServerConfig;

import java.util.List;

/**
 * Turns a preparation into Cooking XP.
 *
 * <pre>
 *   (base + discovery + method + ingredient + experimentation)
 *     x repetition multiplier
 *     x mastery bonus
 *     x server multiplier
 * </pre>
 *
 * <p>Pure arithmetic with no game state, so the balance rules the design
 * cares about most — that cooking one cheap dish ten thousand times is a bad
 * strategy — are directly testable.</p>
 */
public final class XpEngine {

    private XpEngine() {
    }

    /**
     * @param priorPreparationsThisPeriod how many times this dish has already
     *                                    been prepared inside the current
     *                                    anti-farming period
     */
    public record Inputs(int amount,
                         int priorPreparationsThisPeriod,
                         boolean firstEverPreparation,
                         boolean firstEverMethod,
                         int newIngredientsDiscovered,
                         int newVariantsUsed,
                         int experimentationXpAlreadyEarnedToday,
                         int masteredDishCount) {
    }

    public record Result(long total, long base, long bonuses, double repetitionMultiplier,
                         double masteryMultiplier, int experimentationXpAwarded) {
    }

    public static Result compute(Inputs inputs, ServerConfig config) {
        ServerConfig.Progression progression = config.progression;
        if (!progression.enabled) {
            return new Result(0L, 0L, 0L, 0.0, 1.0, 0);
        }

        long base = (long) progression.baseXpPerPreparation * Math.max(1, inputs.amount());

        long bonuses = 0L;
        if (inputs.firstEverPreparation()) {
            bonuses += progression.discoveryBonusXp;
        }
        if (inputs.firstEverMethod()) {
            bonuses += progression.methodDiscoveryXp;
        }
        bonuses += (long) progression.ingredientDiscoveryXp * Math.max(0, inputs.newIngredientsDiscovered());

        // Experimentation is capped per day so ingredient variants cannot be
        // cycled endlessly for XP.
        int experimentation = 0;
        if (inputs.newVariantsUsed() > 0 && progression.experimentationXp > 0) {
            int remaining = Math.max(0, progression.experimentationDailyCap
                    - Math.max(0, inputs.experimentationXpAlreadyEarnedToday()));
            experimentation = Math.min(remaining,
                    progression.experimentationXp * inputs.newVariantsUsed());
            bonuses += experimentation;
        }

        // Discovery bonuses are exempt from diminishing returns: a first
        // preparation should always feel like one.
        double repetition = repetitionMultiplier(inputs.priorPreparationsThisPeriod(), config);
        double mastery = masteryMultiplier(inputs.masteredDishCount(), config);

        long scaledBase = Math.round(base * repetition);
        long total = Math.round((scaledBase + bonuses) * mastery * progression.xpMultiplier);

        return new Result(Math.max(0L, total), base, bonuses, repetition, mastery, experimentation);
    }

    /**
     * The diminishing-returns curve.
     *
     * <p>The first {@code fullXpPreparations} of a dish in a period pay in
     * full; after that each configured tier applies until the floor. The floor
     * is never zero by default — repetition should be worth less, not
     * worthless.</p>
     */
    public static double repetitionMultiplier(int priorPreparationsThisPeriod, ServerConfig config) {
        ServerConfig.AntiFarming antiFarming = config.antiFarming;
        if (!antiFarming.enabled) {
            return 1.0;
        }
        int completed = Math.max(0, priorPreparationsThisPeriod);
        if (completed < antiFarming.fullXpPreparations) {
            return 1.0;
        }
        int index = completed + 1;
        List<ServerConfig.AntiFarming.Tier> tiers = antiFarming.tiers;
        if (tiers != null) {
            for (ServerConfig.AntiFarming.Tier tier : tiers) {
                if (index <= tier.upToPreparations) {
                    return Math.max(antiFarming.minimumMultiplier, tier.multiplier);
                }
            }
        }
        return antiFarming.minimumMultiplier;
    }

    /**
     * Mastering dishes improves progression rather than the food itself, which
     * is the safest possible reward shape.
     */
    public static double masteryMultiplier(int masteredDishCount, ServerConfig config) {
        double bonus = Math.min(config.mastery.xpBonusCap,
                config.mastery.xpBonusPerMasteredDish * Math.max(0, masteredDishCount));
        return 1.0 + bonus;
    }

    /** XP for personally eating a dish; deliberately a fraction of preparing it. */
    public static long eatingXp(ServerConfig config) {
        if (!config.progression.enabled) {
            return 0L;
        }
        return Math.round(config.progression.baseXpPerMeal * config.progression.xpMultiplier);
    }
}
