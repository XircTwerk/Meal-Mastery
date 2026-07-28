package com.xirc.mealmastery;

import com.xirc.mealmastery.platform.Services;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Loader-independent bootstrap.
 *
 * <p>Meal Mastery deliberately fails open (see the design rule in
 * {@code docs/architecture.md}): a missing or unexpected Farmer's Delight build
 * must never stop food from being cooked, so nothing here throws once the mod
 * has been accepted by the loader.</p>
 */
public final class MealMastery {
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private MealMastery() {
    }

    public static void initialize() {
        if (!INITIALIZED.compareAndSet(false, true)) {
            return;
        }

        MealMasteryLog.LOGGER.info(
                "{} {} starting on {} {} (Minecraft {})",
                Constants.MOD_NAME,
                Services.PLATFORM.isDevelopmentEnvironment() ? "[dev]" : "",
                Services.PLATFORM.loaderName(),
                Services.PLATFORM.loaderVersion(),
                Services.PLATFORM.minecraftVersion());

        if (!Services.PLATFORM.isModLoaded(Constants.FARMERS_DELIGHT_ID)) {
            MealMasteryLog.LOGGER.warn(
                    "Farmer's Delight was not detected. Meal Mastery will still load and "
                            + "track any compatible cooking recipes it can find, but the "
                            + "journal will be mostly empty.");
        }
    }
}
