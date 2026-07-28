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

    public static final class Journal {
        /** 0 means "follow the game's GUI scale". */
        public int scale = 0;
        public UnknownRecipeDisplay unknownDisplay = UnknownRecipeDisplay.SILHOUETTE;
        /** Hides percentages, XP numbers and leaderboards without changing the data. */
        public boolean cozyMode = false;
        /** Surfaces missing dishes, per-mod completion and checklists. */
        public boolean completionistMode = false;
        public String defaultPage = "overview";
        public String favoriteSort = "alphabetical";
        public int recentActivityLines = 6;
        public boolean showNewIndicator = true;
        /** Star rating under food names. The bonuses apply either way. */
        public boolean showStars = true;
        /** The mini panel docked beside the Cooking Pot and other containers. */
        public SidePanel sidePanel = SidePanel.CULINARY;
    }

    public static final class Hud {
        /** Off by default: the HUD must not be permanently cluttered. */
        public boolean enabled = false;
        /** Show the level bar only briefly after XP is gained. */
        public boolean onlyWhileActive = true;
        public int visibleTicks = 60;
        public Anchor anchor = Anchor.TOP_LEFT;
        public int offsetX = 4;
        public int offsetY = 4;
        public float scale = 1.0F;
        public boolean showPinnedRecipes = true;

        public enum Anchor {
            TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
        }
    }

    /** Each toast type is individually suppressible. */
    public static final class Notifications {
        public boolean discoveries = true;
        public boolean masteryRanks = true;
        public boolean levelUps = true;
        public boolean challenges = true;
        public boolean milestones = true;
        public boolean xpPopups = true;
        /** Rapid gains are summed into one popup rather than spamming. */
        public int xpPopupAggregationTicks = 30;
        public boolean playSounds = true;
        /** Toasts per second before further ones are dropped. */
        public int maxToastsPerSecond = 2;
    }

    public static final class Tooltips {
        public boolean enabled = true;
        /** Full detail only while the modifier is held. */
        public boolean requireModifier = true;
        public Modifier modifier = Modifier.SHIFT;
        public boolean showMastery = true;
        public boolean showPreparedCount = true;
        /**
         * Off by default so AppleSkin and the nutrition mods keep ownership of
         * that tooltip section.
         */
        public boolean showNutrition = false;

        public enum Modifier {
            SHIFT, CONTROL, ALT, NONE
        }
    }

    public static final class Accessibility {
        /** Disables every journal transition. */
        public boolean reducedMotion = false;
        public float animationSpeed = 1.0F;
        /** Draws rank pips as text as well as shapes, so rank is never colour-only. */
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
