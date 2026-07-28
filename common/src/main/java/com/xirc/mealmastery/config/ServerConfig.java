package com.xirc.mealmastery.config;

import com.xirc.mealmastery.config.ConfigEnums.AutomationCredit;
import com.xirc.mealmastery.config.ConfigEnums.DiscoveryMode;
import com.xirc.mealmastery.config.ConfigEnums.MasteryRewards;
import com.xirc.mealmastery.config.ConfigEnums.MasteryTarget;
import com.xirc.mealmastery.config.ConfigEnums.SharedDiscovery;
import com.xirc.mealmastery.config.ConfigEnums.StreakClock;
import com.xirc.mealmastery.culinary.LevelCurve;
import com.xirc.mealmastery.mastery.MasteryCurve;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-authoritative settings.
 *
 * <p>Plain mutable fields because this is a Gson document; {@link #validate()}
 * is what makes it safe, clamping nonsense rather than refusing to load. A
 * config file a player hand-edited into an impossible state must still boot the
 * server.</p>
 *
 * <p>Every default here follows installing Meal Mastery changes no food,
 * no recipe and no Farmer's Delight progression.</p>
 */
public final class ServerConfig {

    public Progression progression = new Progression();
    public AntiFarming antiFarming = new AntiFarming();
    public Mastery mastery = new Mastery();
    public Discovery discovery = new Discovery();
    public Streaks streaks = new Streaks();
    public Challenges challenges = new Challenges();
    public Multiplayer multiplayer = new Multiplayer();
    public Automation automation = new Automation();
    public Compatibility compatibility = new Compatibility();
    public Advanced advanced = new Advanced();

    public static final class Progression {
        public boolean enabled = true;
        public double xpMultiplier = 1.0;

        /** Base Cooking XP for preparing a dish that has been made before. */
        public int baseXpPerPreparation = 6;
        /** Extra XP the first time a dish is ever prepared. */
        public int discoveryBonusXp = 75;
        /** Extra XP the first time an ingredient is ever used. */
        public int ingredientDiscoveryXp = 10;
        /** Extra XP the first time a cooking method is ever used. */
        public int methodDiscoveryXp = 25;
        /** Extra XP for using an ingredient variant never used in that dish before. */
        public int experimentationXp = 8;
        /** Cap on experimentation XP per Minecraft day, so variants cannot be farmed. */
        public int experimentationDailyCap = 60;
        /** XP for personally eating a dish, kept far below preparing it. */
        public int baseXpPerMeal = 1;

        public int levelCurveBase = LevelCurve.DEFAULT_BASE;
        public int levelCurveLinear = LevelCurve.DEFAULT_LINEAR;
        public int levelCurveQuadratic = LevelCurve.DEFAULT_QUADRATIC;
        public int maxLevel = LevelCurve.DEFAULT_MAX_LEVEL;

        public LevelCurve toCurve() {
            return new LevelCurve(levelCurveBase, levelCurveLinear, levelCurveQuadratic, maxLevel);
        }
    }

    /**
     * Diminishing returns.
     *
     * <p>Cooking one cheap dish ten thousand times has to be a bad strategy, but
     * it must never become a punishment either — the floor multiplier keeps
     * repetition worth something.</p>
     */
    public static final class AntiFarming {
        public boolean enabled = true;
        /** Tracked per dish rather than globally, so variety is what resets it. */
        public boolean perRecipe = true;
        /** How many Minecraft days one tracking period covers. */
        public int trackingPeriodDays = 1;
        /** Preparations per period that still earn full XP. */
        public int fullXpPreparations = 1;
        /** Multiplier tiers applied after the full-XP preparations are used up. */
        public List<Tier> tiers = defaultTiers();
        /** Never drop below this, no matter how many times a dish is repeated. */
        public double minimumMultiplier = 0.2;

        public static final class Tier {
            /** Applies while the period count is at most this many. */
            public int upToPreparations;
            public double multiplier;

            public Tier() {
            }

            public Tier(int upToPreparations, double multiplier) {
                this.upToPreparations = upToPreparations;
                this.multiplier = multiplier;
            }
        }

        private static List<Tier> defaultTiers() {
            List<Tier> tiers = new ArrayList<>();
            tiers.add(new Tier(5, 0.75));
            tiers.add(new Tier(15, 0.50));
            return tiers;
        }
    }

    public static final class Mastery {
        public boolean enabled = true;
        public MasteryTarget target = MasteryTarget.ITEM;
        public MasteryRewards rewards = MasteryRewards.COSMETIC_ONLY;
        public List<Integer> thresholds = new ArrayList<>(MasteryCurve.DEFAULT_THRESHOLDS);

        /** Mastery points granted per preparation. */
        public int pointsPerPreparation = 1;
        /** Points for personally eating the dish; off by default. */
        public int pointsPerMeal = 0;
        /** Points for serving a portion to someone else. */
        public int pointsPerServing = 1;
        /** Extra points when the dish is made with its primary method. */
        public int intendedMethodBonus = 0;

        /**
         * Cooking XP bonus per mastered dish, as a fraction. The safest reward
         * shape: mastery improves progression rather than the food itself
         *.
         */
        public double xpBonusPerMasteredDish = 0.005;
        public double xpBonusCap = 0.25;

        /** The gameplay effects mastery has. On by default; amounts are tunable. */
        public Bonuses bonuses = new Bonuses();

        public MasteryCurve toCurve() {
            return new MasteryCurve(thresholds);
        }
    }

    /**
     * What mastering a dish actually does in play.
     *
     * <p>Unlike the XP shaping above, these are real mechanical effects, and
     * they are on by default. Every magnitude is a plain number here so a pack
     * can dial them from "barely noticeable" to "absurd" without touching
     * anything else.</p>
     */
    public static final class Bonuses {
        public boolean enabled = true;

        /**
         * Extra workstation ticks per second at full mastery, as a fraction.
         * 1.0 means a mastered dish cooks twice as fast; the value scales
         * linearly from zero at Unfamiliar.
         */
        public double cookingSpeedAtMaxRank = 1.0;

        /**
         * Whether the speed bonus reaches the vanilla smoker.
         *
         * <p>Separate from {@link #cookingSpeedAtMaxRank} because it is the one
         * accelerated workstation that is not Farmer's Delight's, and a pack
         * balanced around vanilla furnace timings may not want it touched.</p>
         */
        public boolean accelerateSmokers = true;

        /** Stars shown on food tooltips, one per mastery rank. */
        public boolean starsEnabled = true;

        /** Chance of a bonus effect when eating, at full mastery. */
        public double effectChanceAtMaxRank = 0.25;
        public int effectDurationSeconds = 20;
        /**
         * Effects rolled from. Vanilla ids only: no custom effect is registered
         * anywhere in this mod.
         *
         * <p>{@code minecraft:saturation} is deliberately absent. It is not a
         * buff — it refills food and saturation every tick it runs, so even a
         * short one turns any dish into a full meal several times over. A pack
         * that wants that can add it back, but it should be a choice.</p>
         */
        public List<String> effects = new ArrayList<>(List.of(
                "minecraft:regeneration",
                "minecraft:speed",
                "minecraft:dig_speed"));
        /** Amplifier at full mastery; scales down with rank. Level I by default. */
        public int effectAmplifierAtMaxRank = 0;

        /** Extra saturation points per rank when eating a dish you have mastered. */
        public float saturationPerRank = 0.2F;

        /** Chance at full mastery that a preparation comes out perfect. */
        public double perfectChanceAtMaxRank = 0.08;
        /** How much stronger a perfect dish is than its star rating alone. */
        public double perfectEffectMultiplier = 2.0;

        /** Chance at full mastery of an extra portion from the same ingredients. */
        public double extraPortionChanceAtMaxRank = 0.12;

        /** Chance at full method familiarity of refunding a point of tool durability. */
        public double toolRefundChanceAtMaxTier = 0.25;

        /** Mastery points the cook earns when someone else eats their cooking. */
        public int masteryPerMealServedToOthers = 2;
    }

    public static final class Discovery {
        public DiscoveryMode mode = DiscoveryMode.COOK_RECIPE;
        /** Dishes the player already has in a recipe book are still undiscovered here. */
        public boolean countObtainingAsDiscovery = false;
        public SharedDiscovery shared = SharedDiscovery.PERSONAL;
    }

    public static final class Streaks {
        public StreakClock clock = StreakClock.MINECRAFT_DAYS;
        /** Idle days tolerated before a cooking streak resets. */
        public int graceDays = 1;
        public boolean varietyStreakEnabled = true;
    }

    public static final class Challenges {
        public boolean enabled = true;
        public boolean dailyEnabled = true;
        public boolean weeklyEnabled = false;
        /** Real-time scheduling defaults off. */
        public boolean useRealTimeSchedule = false;
        public int dailyChallengeCount = 3;
        public int weeklyChallengeCount = 3;
        public boolean recipeOfTheDayEnabled = true;
        public int recipeOfTheDayBonusXp = 50;
        public double challengeXpMultiplier = 1.0;
        /** Data-driven rewards may run commands; off unless a pack opts in. */
        public boolean allowCommandRewards = false;
    }

    public static final class Multiplayer {
        /** Leaderboards are opt-in; the mod is not competitive by default. */
        public boolean leaderboardsEnabled = false;
        public boolean leaderboardsOptInOnly = true;
        public int leaderboardSize = 10;
        public boolean teamStatisticsEnabled = false;
        /** Inspecting another player's profile requires this permission level. */
        public int inspectPermissionLevel = 2;
        public int adminPermissionLevel = 2;
    }

    public static final class Automation {
        public AutomationCredit credit = AutomationCredit.NO_CREDIT;
        public double reducedCreditMultiplier = 0.25;
        /**
         * How long after a player interacts with a workstation their culinary
         * gains are still attributed to them, in ticks. Short on purpose:
         * anything longer starts crediting coincidences.
         */
        public int attributionWindowTicks = 20;
        /** Radius, in blocks, for crediting items dropped by a workstation. */
        public double attributionRadius = 3.0;
    }

    public static final class Compatibility {
        /** Extra block ids treated as cooking workstations, for addons with their own. */
        public List<String> extraWorkstationBlocks = new ArrayList<>();
        /** Menu classes never treated as culinary, on top of the built-in storage list. */
        public List<String> excludedMenuClasses = new ArrayList<>();
        public List<String> excludedRecipeTypes = new ArrayList<>();
        public List<String> excludedItems = new ArrayList<>();
        public List<String> excludedMods = new ArrayList<>();
        public List<String> includedOnlyMods = new ArrayList<>();
        public List<String> requiredItemTags = new ArrayList<>();
        public boolean requireEdibleOutput = true;
        /**
         * Whether vanilla recipes earn mastery.
         *
         * <p>On by default: bread and a baked potato are cooking too, and a
         * pack that wants the journal to be about its food addons only can
         * turn the whole {@code minecraft} namespace off here rather than
         * listing every vanilla dish by hand.</p>
         */
        public boolean trackVanillaRecipes = true;
    }

    public static final class Advanced {
        public boolean debugLogging = false;
        /** Milliseconds between profile autosaves. */
        public int autosaveIntervalMillis = 60_000;
        /** Recent-activity entries retained per profile. */
        public int activityHistorySize = 32;
        /** Maximum dishes a player may pin at once. */
        public int maxPinnedRecipes = 3;
        /** Journal entries sent per network page, keeping packets well under the limit. */
        public int journalPageSize = 64;
    }

    /**
     * Clamps every value into a workable range.
     *
     * @return human-readable notes about anything that had to be corrected;
     *         empty when the file was already sane
     */
    public List<String> validate() {
        List<String> issues = new ArrayList<>();

        progression.xpMultiplier = clamp(progression.xpMultiplier, 0.0, 100.0,
                "progression.xpMultiplier", issues);
        progression.baseXpPerPreparation = clamp(progression.baseXpPerPreparation, 0, 10_000,
                "progression.baseXpPerPreparation", issues);
        progression.discoveryBonusXp = clamp(progression.discoveryBonusXp, 0, 100_000,
                "progression.discoveryBonusXp", issues);
        progression.ingredientDiscoveryXp = clamp(progression.ingredientDiscoveryXp, 0, 100_000,
                "progression.ingredientDiscoveryXp", issues);
        progression.methodDiscoveryXp = clamp(progression.methodDiscoveryXp, 0, 100_000,
                "progression.methodDiscoveryXp", issues);
        progression.experimentationXp = clamp(progression.experimentationXp, 0, 100_000,
                "progression.experimentationXp", issues);
        progression.experimentationDailyCap = clamp(progression.experimentationDailyCap, 0, 1_000_000,
                "progression.experimentationDailyCap", issues);
        progression.baseXpPerMeal = clamp(progression.baseXpPerMeal, 0, 10_000,
                "progression.baseXpPerMeal", issues);
        progression.levelCurveBase = clamp(progression.levelCurveBase, 1, 1_000_000,
                "progression.levelCurveBase", issues);
        progression.levelCurveLinear = clamp(progression.levelCurveLinear, 0, 1_000_000,
                "progression.levelCurveLinear", issues);
        progression.levelCurveQuadratic = clamp(progression.levelCurveQuadratic, 0, 1_000_000,
                "progression.levelCurveQuadratic", issues);
        progression.maxLevel = clamp(progression.maxLevel, 1, 10_000,
                "progression.maxLevel", issues);

        antiFarming.trackingPeriodDays = clamp(antiFarming.trackingPeriodDays, 1, 365,
                "antiFarming.trackingPeriodDays", issues);
        antiFarming.fullXpPreparations = clamp(antiFarming.fullXpPreparations, 0, 10_000,
                "antiFarming.fullXpPreparations", issues);
        antiFarming.minimumMultiplier = clamp(antiFarming.minimumMultiplier, 0.0, 1.0,
                "antiFarming.minimumMultiplier", issues);
        if (antiFarming.tiers == null) {
            antiFarming.tiers = new ArrayList<>();
        }
        antiFarming.tiers.removeIf(tier -> tier == null);
        for (ServerConfig.AntiFarming.Tier tier : antiFarming.tiers) {
            tier.upToPreparations = clamp(tier.upToPreparations, 1, 1_000_000,
                    "antiFarming.tiers.upToPreparations", issues);
            tier.multiplier = clamp(tier.multiplier, 0.0, 10.0,
                    "antiFarming.tiers.multiplier", issues);
        }
        antiFarming.tiers.sort((left, right) ->
                Integer.compare(left.upToPreparations, right.upToPreparations));

        if (mastery.thresholds == null || mastery.thresholds.isEmpty()) {
            mastery.thresholds = new ArrayList<>(MasteryCurve.DEFAULT_THRESHOLDS);
            issues.add("mastery.thresholds was empty; restored the defaults");
        }
        mastery.pointsPerPreparation = clamp(mastery.pointsPerPreparation, 0, 10_000,
                "mastery.pointsPerPreparation", issues);
        mastery.pointsPerMeal = clamp(mastery.pointsPerMeal, 0, 10_000,
                "mastery.pointsPerMeal", issues);
        mastery.pointsPerServing = clamp(mastery.pointsPerServing, 0, 10_000,
                "mastery.pointsPerServing", issues);
        mastery.intendedMethodBonus = clamp(mastery.intendedMethodBonus, 0, 10_000,
                "mastery.intendedMethodBonus", issues);
        mastery.xpBonusPerMasteredDish = clamp(mastery.xpBonusPerMasteredDish, 0.0, 1.0,
                "mastery.xpBonusPerMasteredDish", issues);
        mastery.xpBonusCap = clamp(mastery.xpBonusCap, 0.0, 10.0,
                "mastery.xpBonusCap", issues);

        if (mastery.bonuses == null) {
            mastery.bonuses = new Bonuses();
            issues.add("mastery.bonuses was missing; restored the defaults");
        }
        mastery.bonuses.cookingSpeedAtMaxRank = clamp(mastery.bonuses.cookingSpeedAtMaxRank,
                0.0, 20.0, "mastery.bonuses.cookingSpeedAtMaxRank", issues);
        mastery.bonuses.effectChanceAtMaxRank = clamp(mastery.bonuses.effectChanceAtMaxRank,
                0.0, 1.0, "mastery.bonuses.effectChanceAtMaxRank", issues);
        mastery.bonuses.effectDurationSeconds = clamp(mastery.bonuses.effectDurationSeconds,
                1, 3600, "mastery.bonuses.effectDurationSeconds", issues);
        mastery.bonuses.effectAmplifierAtMaxRank = clamp(mastery.bonuses.effectAmplifierAtMaxRank,
                0, 9, "mastery.bonuses.effectAmplifierAtMaxRank", issues);
        mastery.bonuses.saturationPerRank = (float) clamp(mastery.bonuses.saturationPerRank,
                0.0, 20.0, "mastery.bonuses.saturationPerRank", issues);
        mastery.bonuses.perfectChanceAtMaxRank = clamp(mastery.bonuses.perfectChanceAtMaxRank,
                0.0, 1.0, "mastery.bonuses.perfectChanceAtMaxRank", issues);
        mastery.bonuses.perfectEffectMultiplier = clamp(mastery.bonuses.perfectEffectMultiplier,
                1.0, 10.0, "mastery.bonuses.perfectEffectMultiplier", issues);
        mastery.bonuses.extraPortionChanceAtMaxRank = clamp(
                mastery.bonuses.extraPortionChanceAtMaxRank, 0.0, 1.0,
                "mastery.bonuses.extraPortionChanceAtMaxRank", issues);
        mastery.bonuses.toolRefundChanceAtMaxTier = clamp(mastery.bonuses.toolRefundChanceAtMaxTier,
                0.0, 1.0, "mastery.bonuses.toolRefundChanceAtMaxTier", issues);
        mastery.bonuses.masteryPerMealServedToOthers = clamp(
                mastery.bonuses.masteryPerMealServedToOthers, 0, 100,
                "mastery.bonuses.masteryPerMealServedToOthers", issues);
        if (mastery.bonuses.effects == null || mastery.bonuses.effects.isEmpty()) {
            mastery.bonuses.effects = new ArrayList<>(List.of("minecraft:saturation"));
            issues.add("mastery.bonuses.effects was empty; restored a default");
        }

        streaks.graceDays = clamp(streaks.graceDays, 1, 365, "streaks.graceDays", issues);

        challenges.dailyChallengeCount = clamp(challenges.dailyChallengeCount, 0, 20,
                "challenges.dailyChallengeCount", issues);
        challenges.weeklyChallengeCount = clamp(challenges.weeklyChallengeCount, 0, 20,
                "challenges.weeklyChallengeCount", issues);
        challenges.recipeOfTheDayBonusXp = clamp(challenges.recipeOfTheDayBonusXp, 0, 100_000,
                "challenges.recipeOfTheDayBonusXp", issues);
        challenges.challengeXpMultiplier = clamp(challenges.challengeXpMultiplier, 0.0, 100.0,
                "challenges.challengeXpMultiplier", issues);

        multiplayer.leaderboardSize = clamp(multiplayer.leaderboardSize, 1, 100,
                "multiplayer.leaderboardSize", issues);
        multiplayer.inspectPermissionLevel = clamp(multiplayer.inspectPermissionLevel, 0, 4,
                "multiplayer.inspectPermissionLevel", issues);
        multiplayer.adminPermissionLevel = clamp(multiplayer.adminPermissionLevel, 0, 4,
                "multiplayer.adminPermissionLevel", issues);

        automation.reducedCreditMultiplier = clamp(automation.reducedCreditMultiplier, 0.0, 1.0,
                "automation.reducedCreditMultiplier", issues);
        automation.attributionWindowTicks = clamp(automation.attributionWindowTicks, 1, 200,
                "automation.attributionWindowTicks", issues);
        automation.attributionRadius = clamp(automation.attributionRadius, 0.5, 16.0,
                "automation.attributionRadius", issues);

        advanced.autosaveIntervalMillis = clamp(advanced.autosaveIntervalMillis, 5_000, 3_600_000,
                "advanced.autosaveIntervalMillis", issues);
        advanced.activityHistorySize = clamp(advanced.activityHistorySize, 1, 64,
                "advanced.activityHistorySize", issues);
        advanced.maxPinnedRecipes = clamp(advanced.maxPinnedRecipes, 1, 16,
                "advanced.maxPinnedRecipes", issues);
        advanced.journalPageSize = clamp(advanced.journalPageSize, 8, 256,
                "advanced.journalPageSize", issues);

        return issues;
    }

    private static int clamp(int value, int min, int max, String field, List<String> issues) {
        int clamped = Math.max(min, Math.min(max, value));
        if (clamped != value) {
            issues.add(field + " was " + value + "; clamped to " + clamped);
        }
        return clamped;
    }

    private static double clamp(double value, double min, double max, String field,
                                List<String> issues) {
        if (Double.isNaN(value)) {
            issues.add(field + " was not a number; reset to " + min);
            return min;
        }
        double clamped = Math.max(min, Math.min(max, value));
        if (clamped != value) {
            issues.add(field + " was " + value + "; clamped to " + clamped);
        }
        return clamped;
    }
}
