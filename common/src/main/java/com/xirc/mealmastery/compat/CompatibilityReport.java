package com.xirc.mealmastery.compat;

import com.xirc.mealmastery.Constants;
import com.xirc.mealmastery.platform.Services;
import com.xirc.mealmastery.recipe.CulinaryRegistries;
import com.xirc.mealmastery.recipe.CulinaryRegistry;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What Meal Mastery can honestly say about the other mods present.
 *
 * <p>The wording matters. "Detected" means the mod is installed and Meal
 * Mastery reads its content directly. "Generic Support" means dishes from it
 * were picked up by the ordinary recipe scan with no integration code — which
 * is the normal case for a food addon. "Compatible" means the two coexist
 * without interfering. <b>"Full Compatibility" is never reported</b>, because
 * that would be a claim about testing rather than about detection.</p>
 */
public final class CompatibilityReport {

    private CompatibilityReport() {
    }

    public enum Status {
        DETECTED("detected"),
        GENERIC("generic"),
        COMPATIBLE("compatible"),
        ABSENT("absent");

        private final String key;

        Status(String key) {
            this.key = key;
        }

        public String translationKey() {
            return "mealmastery.compat." + key;
        }
    }

    /**
     * @param dishCount how many journal entries this mod contributes; zero for
     *                  mods that are merely coexisting
     */
    public record Line(String modId, String displayName, Status status, int dishCount) {
    }

    /** Mods that overlap in behaviour and are checked for coexistence, not for content. */
    private static final List<String> COEXISTENCE = List.of(
            "jei", "roughlyenoughitems", "emi", "appleskin", "diet", "nutrition",
            "a_balanced_diet", "pmmo", "projectmmo", "levelz");

    public static List<Line> build() {
        CulinaryRegistry registry = CulinaryRegistries.current();
        List<Line> lines = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();

        // Farmer's Delight first: it is the dependency everything else hangs off.
        if (Services.PLATFORM.isModLoaded(Constants.FARMERS_DELIGHT_ID)) {
            lines.add(new Line(Constants.FARMERS_DELIGHT_ID,
                    Services.PLATFORM.modDisplayName(Constants.FARMERS_DELIGHT_ID),
                    Status.DETECTED, registry.byMod(Constants.FARMERS_DELIGHT_ID).size()));
            seen.add(Constants.FARMERS_DELIGHT_ID);
        }

        // Every other mod contributing dishes got there through the generic
        // scan, which is exactly what "Generic Support" means.
        for (String modId : registry.sourceMods()) {
            if (!seen.add(modId) || modId.equals(Constants.MOD_ID)) {
                continue;
            }
            lines.add(new Line(modId, Services.PLATFORM.modDisplayName(modId),
                    modId.equals("minecraft") ? Status.DETECTED : Status.GENERIC,
                    registry.byMod(modId).size()));
        }

        for (String modId : COEXISTENCE) {
            if (seen.contains(modId) || !Services.PLATFORM.isModLoaded(modId)) {
                continue;
            }
            seen.add(modId);
            lines.add(new Line(modId, Services.PLATFORM.modDisplayName(modId),
                    Status.COMPATIBLE, 0));
        }
        return lines;
    }
}
