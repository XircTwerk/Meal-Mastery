package com.xirc.mealmastery;

/**
 * Shared identifiers that remain safe to load inside plain unit tests.
 *
 * <p>Nothing here may touch Minecraft classes: the test source set runs without
 * a game runtime.</p>
 */
public final class Constants {
    public static final String MOD_ID = "mealmastery";
    public static final String MOD_NAME = "Farmer's Delight: Meal Mastery";
    public static final String MINECRAFT_VERSION = "1.20.1";

    /** Farmer's Delight is a hard runtime dependency, never a compile dependency. */
    public static final String FARMERS_DELIGHT_ID = "farmersdelight";

    public static final String COMMAND = "mealmastery";
    public static final String COMMAND_ALIAS_LONG = "mealmasters";
    public static final String COMMAND_ALIAS_SHORT = "mm";

    /** Bumped whenever the persisted profile layout changes. See {@code data/ProfileMigrations}. */
    public static final int PROFILE_SCHEMA_VERSION = 1;

    private Constants() {
    }
}
