package com.xirc.mealmastery.culinary;

/**
 * Converts total Cooking XP into a Cooking Level.
 *
 * <p>{@code xpForLevel(n) = base + linear*(n-1) + quadratic*(n-1)^2}, which with
 * the defaults reproduces the design's worked example: level 27 costs
 * 1,738 XP. All three coefficients are server configuration so a pack can make
 * progression cozy or long without touching earned XP.</p>
 */
public final class LevelCurve {
    public static final int DEFAULT_BASE = 100;
    public static final int DEFAULT_LINEAR = 63;
    public static final int DEFAULT_QUADRATIC = 0;
    public static final int DEFAULT_MAX_LEVEL = 100;

    public static final LevelCurve DEFAULT =
            new LevelCurve(DEFAULT_BASE, DEFAULT_LINEAR, DEFAULT_QUADRATIC, DEFAULT_MAX_LEVEL);

    private final int base;
    private final int linear;
    private final int quadratic;
    private final int maxLevel;

    public LevelCurve(int base, int linear, int quadratic, int maxLevel) {
        this.base = Math.max(1, base);
        this.linear = Math.max(0, linear);
        this.quadratic = Math.max(0, quadratic);
        this.maxLevel = Math.max(1, maxLevel);
    }

    public int maxLevel() {
        return maxLevel;
    }

    /** XP required to go from {@code level} to {@code level + 1}. Level is 1-based. */
    public long xpForLevel(int level) {
        if (level < 1) {
            level = 1;
        }
        long steps = level - 1L;
        return base + linear * steps + quadratic * steps * steps;
    }

    /** Total XP required to have reached {@code level}. */
    public long totalXpForLevel(int level) {
        long total = 0L;
        for (int i = 1; i < Math.min(level, maxLevel); i++) {
            total += xpForLevel(i);
        }
        return total;
    }

    public int levelForXp(long totalXp) {
        if (totalXp <= 0L) {
            return 1;
        }
        int level = 1;
        long remaining = totalXp;
        while (level < maxLevel) {
            long needed = xpForLevel(level);
            if (remaining < needed) {
                break;
            }
            remaining -= needed;
            level++;
        }
        return level;
    }

    /** XP earned inside the current level. */
    public long xpIntoLevel(long totalXp) {
        int level = levelForXp(totalXp);
        return Math.max(0L, totalXp - totalXpForLevel(level));
    }

    /** XP needed to finish the current level, or {@code -1} once the cap is reached. */
    public long xpToNextLevel(long totalXp) {
        int level = levelForXp(totalXp);
        return level >= maxLevel ? -1L : xpForLevel(level);
    }
}
