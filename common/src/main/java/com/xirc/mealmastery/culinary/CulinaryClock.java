package com.xirc.mealmastery.culinary;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

/**
 * The single definition of "today" used by streaks, records and anti-farming.
 *
 * <p>Derived from the overworld's <em>game time</em> rather than its day time.
 * Game time only ever moves forward, so {@code /time set day} cannot be used to
 * reset the anti-farming period or inflate a streak, and every dimension agrees
 * on the date.</p>
 */
public final class CulinaryClock {
    private static final long TICKS_PER_DAY = 24_000L;

    private CulinaryClock() {
    }

    public static long day(Level level) {
        return level == null ? 0L : level.getGameTime() / TICKS_PER_DAY;
    }

    /**
     * The authoritative day for a server, taken from the overworld so players
     * in the Nether and the End share one calendar.
     */
    public static long day(MinecraftServer server) {
        if (server == null) {
            return 0L;
        }
        ServerLevel overworld = server.overworld();
        return overworld == null ? 0L : day(overworld);
    }

    /**
     * The day number players recognise from the F3 screen, used only for
     * display ("First Prepared: Day 47").
     */
    public static long displayDay(Level level) {
        return level == null ? 0L : level.getDayTime() / TICKS_PER_DAY;
    }
}
