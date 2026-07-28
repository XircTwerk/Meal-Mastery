package com.xirc.mealmastery.data;

import com.xirc.mealmastery.Constants;
import com.xirc.mealmastery.MealMasteryLog;
import net.minecraft.nbt.CompoundTag;

/**
 * Schema upgrades for persisted profiles.
 *
 * <p>Three rules, all of them load-bearing:</p>
 * <ol>
 *   <li>Migration only ever <em>adds or rewrites</em>; it never deletes a
 *       section it does not understand.</li>
 *   <li>A failed migration logs loudly and returns the input unchanged, so the
 *       player loads with whatever could be read rather than with nothing.</li>
 *   <li>A profile from a <em>newer</em> schema is left completely alone;
 *       {@code CulinaryProfile} preserves unknown sections on round-trip.</li>
 * </ol>
 */
public final class ProfileMigrations {
    private ProfileMigrations() {
    }

    public static CompoundTag migrate(CompoundTag tag) {
        int from = tag.contains("schemaVersion") ? tag.getInt("schemaVersion") : 0;
        if (from == Constants.PROFILE_SCHEMA_VERSION) {
            return tag;
        }
        if (from > Constants.PROFILE_SCHEMA_VERSION) {
            MealMasteryLog.LOGGER.warn(
                    "Culinary profile was written by a newer build (schema {} > {}). "
                            + "Loading it read-mostly; unknown sections are preserved untouched.",
                    from, Constants.PROFILE_SCHEMA_VERSION);
            return tag;
        }

        CompoundTag working = tag.copy();
        try {
            for (int version = from; version < Constants.PROFILE_SCHEMA_VERSION; version++) {
                working = step(working, version);
            }
            working.putInt("schemaVersion", Constants.PROFILE_SCHEMA_VERSION);
            MealMasteryLog.LOGGER.info("Migrated culinary profile from schema {} to {}",
                    from, Constants.PROFILE_SCHEMA_VERSION);
            return working;
        } catch (RuntimeException failure) {
            MealMasteryLog.LOGGER.error(
                    "Culinary profile migration from schema {} failed. The profile has been left "
                            + "exactly as it was on disk; no progress was erased.", from, failure);
            return tag;
        }
    }

    private static CompoundTag step(CompoundTag tag, int fromVersion) {
        return switch (fromVersion) {
            // 0 -> 1: profiles that predate the schema marker. Nothing to
            // rewrite; stamping the version is enough.
            case 0 -> tag;
            default -> tag;
        };
    }
}
