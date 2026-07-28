package com.xirc.mealmastery.mastery;

import java.util.Arrays;
import java.util.List;

/**
 * Maps accumulated mastery points onto a {@link MasteryRank}.
 *
 * <p>Thresholds are server configuration, not constants: changing them must
 * never destroy earned points, only re-derive the displayed rank. The
 * curve is deliberately non-linear but the defaults are reachable through
 * normal play rather than grinding.</p>
 */
public final class MasteryCurve {
    /**
     * Preparations required for Novice, Familiar, Skilled, Expert, Mastered.
     *
     * <p>One entry per rank above {@link MasteryRank#UNFAMILIAR}. The first
     * preparation of a dish is worth a rank on its own, which is what makes the
     * journal feel alive on a brand new world; the top rank is reachable
     * through ordinary play rather than grinding.</p>
     */
    public static final List<Integer> DEFAULT_THRESHOLDS = List.of(1, 10, 30, 75, 150);

    public static final MasteryCurve DEFAULT = new MasteryCurve(DEFAULT_THRESHOLDS);

    private final int[] thresholds;

    public MasteryCurve(List<Integer> thresholds) {
        if (thresholds == null || thresholds.isEmpty()) {
            throw new IllegalArgumentException("Mastery curve needs at least one threshold");
        }
        int[] values = new int[thresholds.size()];
        int previous = 0;
        for (int i = 0; i < thresholds.size(); i++) {
            Integer raw = thresholds.get(i);
            int value = raw == null ? previous + 1 : raw;
            // A non-increasing curve would make ranks unreachable or ambiguous.
            value = Math.max(value, previous + 1);
            values[i] = value;
            previous = value;
        }
        this.thresholds = values;
    }

    /**
     * Points needed to reach the given 1-based rank index, or {@code -1} past
     * the top rank.
     *
     * <p>A curve configured with more thresholds than there are ranks has its
     * surplus entries ignored rather than silently inventing a rank, which is
     * what {@link #maxRankIndex()} bounds this by.</p>
     */
    public int thresholdFor(int rankIndex) {
        if (rankIndex <= 0) {
            return 0;
        }
        if (rankIndex > maxRankIndex()) {
            return -1;
        }
        return thresholds[rankIndex - 1];
    }

    public int maxRankIndex() {
        return Math.min(thresholds.length, MasteryRank.highestIndex());
    }

    public MasteryRank rankFor(long points) {
        int rank = 0;
        for (int i = 0; i < thresholds.length && i < MasteryRank.highestIndex(); i++) {
            if (points >= thresholds[i]) {
                rank = i + 1;
            } else {
                break;
            }
        }
        return MasteryRank.byIndex(rank);
    }

    /** Points already earned toward the next rank; equals {@code pointsForNextRank} at the cap. */
    public long progressIntoRank(long points) {
        int rank = rankFor(points).ordinal();
        int floor = thresholdFor(rank);
        int ceiling = thresholdFor(rank + 1);
        if (ceiling < 0) {
            return Math.max(0L, points - floor);
        }
        return Math.max(0L, points - floor);
    }

    /** Points between the current and next rank, or {@code -1} when already at the cap. */
    public long pointsForNextRank(long points) {
        int rank = rankFor(points).ordinal();
        int floor = thresholdFor(rank);
        int ceiling = thresholdFor(rank + 1);
        return ceiling < 0 ? -1L : ceiling - floor;
    }

    public List<Integer> thresholds() {
        return Arrays.stream(thresholds).boxed().toList();
    }
}
