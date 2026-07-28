package com.xirc.mealmastery;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;

/**
 * Brings up just enough of Minecraft for tests that touch registry-backed types
 * such as {@code TagKey} or {@code ItemStack}.
 *
 * <p>Most of Meal Mastery's logic is deliberately written against plain values
 * so it needs none of this; the handful of tests that do call {@link #ensure()}
 * once.</p>
 */
public final class MinecraftBootstrap {
    private static boolean started;

    private MinecraftBootstrap() {
    }

    public static synchronized void ensure() {
        if (started) {
            return;
        }
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        started = true;
    }
}
