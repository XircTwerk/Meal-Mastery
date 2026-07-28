package com.xirc.mealmastery.mastery;

/**
 * Named mastery tiers.
 *
 * <p>The design is explicit that mastery must not be capped at "five stars"
 * internally: the ordinal here is the display tier, while the underlying
 * mastery points keep accumulating past {@link #MASTERED} so preparation counts
 * stay meaningful without handing out endless levels.</p>
 */
public enum MasteryRank {
    UNFAMILIAR("unfamiliar"),
    NOVICE("novice"),
    FAMILIAR("familiar"),
    SKILLED("skilled"),
    EXPERT("expert"),
    MASTERED("mastered");

    private static final MasteryRank[] VALUES = values();

    private final String id;

    MasteryRank(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "mealmastery.mastery.rank." + id;
    }

    public boolean isMastered() {
        return this == MASTERED;
    }

    public static MasteryRank byIndex(int index) {
        if (index <= 0) {
            return UNFAMILIAR;
        }
        return index >= VALUES.length ? MASTERED : VALUES[index];
    }

    public static int highestIndex() {
        return VALUES.length - 1;
    }
}
