package com.xirc.mealmastery.config;

/**
 * The mode switches a server or player can flip.
 *
 * <p>Grouped in one file because they are meaningless apart from each other and
 * every one of them is referenced from the config screen's category list.</p>
 */
public final class ConfigEnums {
    private ConfigEnums() {
    }

    /** How a dish becomes discovered. */
    public enum DiscoveryMode {
        /** Default: the player has to actually prepare it. */
        COOK_RECIPE,
        /** Eating the dish counts, however it was obtained. */
        EAT_OUTPUT,
        /** Simply having the item in inventory counts. */
        OBTAIN_OUTPUT,
        /** Opening its journal page counts. */
        VIEW_RECIPE,
        /** Nothing is hidden. */
        ALWAYS_VISIBLE
    }

    /** How undiscovered dishes are drawn in the journal. */
    public enum UnknownRecipeDisplay {
        /** Not listed at all. */
        HIDDEN,
        /** Listed, with the icon drawn as a flat silhouette and the name masked. */
        SILHOUETTE,
        /** Listed with its real name but no other detail. */
        NAME_ONLY,
        /** Listed with its name and a partial ingredient list. */
        INGREDIENT_HINTS,
        /** Fully visible. */
        FULL_RECIPE
    }

    /** What mastery is allowed to affect. */
    public enum MasteryRewards {
        /** Statistics, badges and journal labels only. Nothing touches gameplay. */
        COSMETIC_ONLY,
        /** Small progression-side bonuses: slightly more Cooking XP. */
        LIGHT,
        /** Noticeable but balanced; still progression-side by default. */
        STANDARD,
        /** Every individual bonus is read from configuration. */
        CUSTOM
    }

    /** Who gets credit when a machine cooks something. */
    public enum AutomationCredit {
        /** Default. No player is credited unless one is actually responsible. */
        NO_CREDIT,
        /** Credit the placing player where the game genuinely records one. */
        OWNER_CREDIT,
        /** As OWNER_CREDIT, at a reduced XP rate. */
        REDUCED_CREDIT,
        /** Treat automated output as if the owner had cooked it by hand. */
        FULL_CREDIT
    }

    /** Whose discoveries are shared. */
    public enum SharedDiscovery {
        /** Default. Discovery is personal. */
        PERSONAL,
        /** Anyone on your scoreboard team reveals dishes for the team. */
        TEAM,
        /** The first player to discover a dish reveals it for everyone. */
        SERVER
    }

    /** Which clock drives cooking streaks. */
    public enum StreakClock {
        /** Default. Streaks advance with in-game days. */
        MINECRAFT_DAYS,
        /** Streaks advance with real-world days. */
        REAL_DAYS,
        DISABLED
    }

    /** Whether mastery belongs to the dish or to each individual recipe. */
    public enum MasteryTarget {
        /** Default. Two ways to make tomato sauce share one page. */
        ITEM,
        /** Every recipe gets its own page. */
        RECIPE
    }

    /** Presets a server or player may apply deliberately. */
    public enum Preset {
        /** Journal and statistics, minimal progression pressure. */
        VANILLA_PLUS,
        /** Faster progression and discovery, softer anti-farming. */
        COZY,
        /** Emphasises discovery and completion. */
        COMPLETIONIST,
        /** Balanced multiplayer defaults. */
        SERVER,
        /** No gameplay bonuses whatsoever. */
        COSMETIC
    }
}
