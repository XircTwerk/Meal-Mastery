package com.xirc.mealmastery.config;

import com.xirc.mealmastery.config.ConfigEnums.UnknownRecipeDisplay;

import java.util.ArrayList;
import java.util.List;

/**
 * Client-only preferences.
 *
 * <p>Nothing here can influence progression. The client asks the server for
 * data and decides how to draw it; that is the whole extent of its authority
 *.</p>
 */
public final class ClientConfig {

    /** Where the compact stats panel beside a container screen appears. */
    public enum SidePanel {
        /** Beside every container screen. */
        ALWAYS,
        /** Only cooking surfaces, or screens actually holding a tracked dish. */
        CULINARY,
        OFF
    }


    public Journal journal = new Journal();
    public Hud hud = new Hud();
    public Notifications notifications = new Notifications();
    public Tooltips tooltips = new Tooltips();
    public Accessibility accessibility = new Accessibility();

    @Comment("The journal screen, opened with the keybind (J by default).")
    public static final class Journal {
        @Comment("0 follows the game's GUI scale. Range 0 - 6.")
        public int scale = 0;
        @Comment({
                "How an undiscovered dish is shown.",
                "HIDDEN, SILHOUETTE, NAME_ONLY, INGREDIENT_HINTS or FULL_RECIPE."})
        public UnknownRecipeDisplay unknownDisplay = UnknownRecipeDisplay.SILHOUETTE;
        @Comment("Hides percentages, XP numbers and leaderboards without changing the data.")
        public boolean cozyMode = false;
        @Comment("Surfaces missing dishes, per-mod completion and checklists.")
        public boolean completionistMode = false;
        @Comment("Which page the journal opens on.")
        public String defaultPage = "overview";
        public String favoriteSort = "alphabetical";
        @Comment("Lines of recent activity on the overview. Range 0 - 32.")
        public int recentActivityLines = 6;
        public boolean showNewIndicator = true;
        @Comment("Star rating under food names. The bonuses apply either way.")
        public boolean showStars = true;
        @Comment({
                "The compact stats panel docked beside a cooking screen.",
                "CULINARY shows it beside cooking stations only, ALWAYS beside every",
                "container, OFF never. It never appears beside the inventory or the",
                "creative tabs whichever is set."})
        public SidePanel sidePanel = SidePanel.CULINARY;
    }

    @Comment("The on-screen level bar. Off by default.")
    public static final class Hud {
        @Comment("Off by default: the HUD must not be permanently cluttered.")
        public boolean enabled = false;
        @Comment("Show the bar only briefly after XP is gained.")
        public boolean onlyWhileActive = true;
        @Comment("How long it stays up, in ticks. Range 20 - 1200.")
        public int visibleTicks = 60;
        @Comment("TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT or BOTTOM_RIGHT.")
        public Anchor anchor = Anchor.TOP_LEFT;
        @Comment("Pixels from that corner. Range -512 - 512.")
        public int offsetX = 4;
        public int offsetY = 4;
        @Comment("Range 0.5 - 3.0.")
        public float scale = 1.0F;
        public boolean showPinnedRecipes = true;

        public enum Anchor {
            TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
        }
    }

    @Comment("Toasts and popups. Each type is individually suppressible.")
    public static final class Notifications {
        public boolean discoveries = true;
        public boolean masteryRanks = true;
        public boolean levelUps = true;
        public boolean challenges = true;
        public boolean milestones = true;
        public boolean xpPopups = true;
        @Comment("Rapid gains are summed into one popup rather than spamming.")
        public int xpPopupAggregationTicks = 30;
        @Comment("Uses existing vanilla sounds; this mod adds none.")
        public boolean playSounds = true;
        @Comment("Toasts per second before further ones are dropped.")
        public int maxToastsPerSecond = 2;
    }

    @Comment("What Meal Mastery adds to a food item's tooltip.")
    public static final class Tooltips {
        public boolean enabled = true;
        @Comment("Show full detail only while the modifier below is held.")
        public boolean requireModifier = true;
        @Comment("SHIFT, CONTROL, ALT or NONE.")
        public Modifier modifier = Modifier.SHIFT;
        public boolean showMastery = true;
        public boolean showPreparedCount = true;
        @Comment({
                "Off by default so AppleSkin and the nutrition mods keep ownership",
                "of that tooltip section. Forced off while one is installed."})
        public boolean showNutrition = false;

        public enum Modifier {
            SHIFT, CONTROL, ALT, NONE
        }
    }

    @Comment("Accessibility.")
    public static final class Accessibility {
        @Comment("Disables every journal transition.")
        public boolean reducedMotion = false;
        @Comment("Range 0.1 - 3.0.")
        public float animationSpeed = 1.0F;
        @Comment("Draws rank as text as well as shapes, so rank is never colour-only.")
        public boolean alwaysShowRankText = true;
        public boolean highContrast = false;
    }

    public List<String> validate() {
        List<String> issues = new ArrayList<>();

        journal.scale = clamp(journal.scale, 0, 6, "journal.scale", issues);
        journal.recentActivityLines = clamp(journal.recentActivityLines, 0, 32,
                "journal.recentActivityLines", issues);

        hud.visibleTicks = clamp(hud.visibleTicks, 20, 1200, "hud.visibleTicks", issues);
        hud.offsetX = clamp(hud.offsetX, -512, 512, "hud.offsetX", issues);
        hud.offsetY = clamp(hud.offsetY, -512, 512, "hud.offsetY", issues);
        hud.scale = clamp(hud.scale, 0.5F, 3.0F, "hud.scale", issues);

        notifications.xpPopupAggregationTicks = clamp(notifications.xpPopupAggregationTicks, 1, 200,
                "notifications.xpPopupAggregationTicks", issues);
        notifications.maxToastsPerSecond = clamp(notifications.maxToastsPerSecond, 1, 20,
                "notifications.maxToastsPerSecond", issues);

        accessibility.animationSpeed = clamp(accessibility.animationSpeed, 0.1F, 4.0F,
                "accessibility.animationSpeed", issues);
        if (accessibility.reducedMotion) {
            accessibility.animationSpeed = 0.0F;
        }

        return issues;
    }

    private static int clamp(int value, int min, int max, String field, List<String> issues) {
        int clamped = Math.max(min, Math.min(max, value));
        if (clamped != value) {
            issues.add(field + " was " + value + "; clamped to " + clamped);
        }
        return clamped;
    }

    private static float clamp(float value, float min, float max, String field,
                               List<String> issues) {
        if (Float.isNaN(value)) {
            issues.add(field + " was not a number; reset to " + min);
            return min;
        }
        float clamped = Math.max(min, Math.min(max, value));
        if (clamped != value) {
            issues.add(field + " was " + value + "; clamped to " + clamped);
        }
        return clamped;
    }
}
