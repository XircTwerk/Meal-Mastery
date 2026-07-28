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

    @Comment({
            "Cooking XP and the level curve.",
            "xpForLevel(n) = base + linear*(n-1) + quadratic*(n-1)^2"})
    public static final class Progression {
        @Comment("Set false to stop awarding Cooking XP entirely.")
        public boolean enabled = true;
        @Comment("Multiplies all Cooking XP. Range 0.0 - 100.0.")
        public double xpMultiplier = 1.0;

        @Comment("Cooking XP for preparing a dish that has been made before.")
        public int baseXpPerPreparation = 6;
        @Comment({
                "Extra XP the first time a dish is ever prepared.",
                "Deliberately far larger than a repeat: exploring should beat grinding."})
        public int discoveryBonusXp = 75;
        @Comment("Extra XP the first time an ingredient is ever used.")
        public int ingredientDiscoveryXp = 10;
        @Comment("Extra XP the first time a cooking method is ever used.")
        public int methodDiscoveryXp = 25;
        @Comment("Extra XP for an ingredient variant never used in that dish before.")
        public int experimentationXp = 8;
        @Comment("Cap on experimentation XP per Minecraft day, so variants cannot be farmed.")
        public int experimentationDailyCap = 60;
        @Comment("XP for personally eating a dish. Kept far below preparing it.")
        public int baseXpPerMeal = 1;

        @Comment("Level curve. With the defaults, level 27 costs 1,738 XP.")
        public int levelCurveBase = LevelCurve.DEFAULT_BASE;
        public int levelCurveLinear = LevelCurve.DEFAULT_LINEAR;
        public int levelCurveQuadratic = LevelCurve.DEFAULT_QUADRATIC;
        @Comment("Levelling stops here. Earned XP past it is kept, not discarded.")
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
    @Comment({
            "Diminishing returns for repeating the same dish.",
            "Repetition should be worth less, never worthless."})
    public static final class AntiFarming {
        @Comment("Set false to remove the curve entirely; every preparation pays full XP.")
        public boolean enabled = true;
        @Comment("Track counts per dish rather than globally, so variety is what resets them.")
        public boolean perRecipe = true;
        @Comment("How many Minecraft days one tracking period covers. Range 1 - 365.")
        public int trackingPeriodDays = 1;
        @Comment("Preparations per period that still earn full XP.")
        public int fullXpPreparations = 1;
        @Comment("Never drop below this multiplier, however often a dish is repeated.")
        public double minimumMultiplier = 0.2;

        @Comment({
                "Multiplier tiers applied once the full-XP preparations are used up.",
                "Each entry applies while the period count is at most upToPreparations.",
                "Add or remove [[antiFarming.tiers]] blocks to reshape the curve."})
        public List<Tier> tiers = defaultTiers();

        public static final class Tier {
            @Comment("Applies while the period count is at most this many.")
            public int upToPreparations;
            @Comment("XP multiplier while this tier applies.")
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

    @Comment("Per-dish mastery: the ranks, and what reaching them is worth.")
    public static final class Mastery {
        @Comment("Set false to stop tracking mastery at all.")
        public boolean enabled = true;
        @Comment("What mastery attaches to. ITEM or RECIPE.")
        public MasteryTarget target = MasteryTarget.ITEM;
        @Comment({
                "How mastery shapes Cooking XP. This is separate from mastery.bonuses,",
                "which is what mastery does to the food itself.",
                "COSMETIC_ONLY, LIGHT, STANDARD or CUSTOM."})
        public MasteryRewards rewards = MasteryRewards.COSMETIC_ONLY;
        @Comment({
                "Preparations needed for Novice, Familiar, Skilled, Expert, Mastered.",
                "One entry per rank above Unfamiliar.",
                "Changing these never destroys earned points; only the shown rank moves."})
        public List<Integer> thresholds = new ArrayList<>(MasteryCurve.DEFAULT_THRESHOLDS);

        @Comment("Mastery points granted per preparation.")
        public int pointsPerPreparation = 1;
        @Comment("Points for personally eating the dish. Off by default.")
        public int pointsPerMeal = 0;
        @Comment("Points for serving a portion to someone else.")
        public int pointsPerServing = 1;
        @Comment("Extra points when the dish is made with its primary method.")
        public int intendedMethodBonus = 0;

        @Comment({
                "Cooking XP bonus per mastered dish, as a fraction, and its ceiling.",
                "The safest reward shape: mastery improves progression, not the food."})
        public double xpBonusPerMasteredDish = 0.005;
        public double xpBonusCap = 0.25;

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
    @Comment({
            "What mastery does in play. These are real mechanical effects.",
            "Every chance scales linearly from zero at Unfamiliar to the value",
            "here at Mastered, so none of it applies to a first attempt.",
            "The real odds for a dish are printed on its tooltip."})
    public static final class Bonuses {
        @Comment("Set false for a statistics-only mod: no effect on food or cooking.")
        public boolean enabled = true;

        @Comment({
                "Extra workstation ticks at full mastery, as a fraction.",
                "1.0 means a mastered dish cooks twice as fast. Range 0.0 - 20.0.",
                "Applies to the cooking pot, skillet and stove, plus the smoker below."})
        public double cookingSpeedAtMaxRank = 1.0;

        @Comment({
                "Whether the speed bonus reaches the vanilla smoker.",
                "Separate because it is the one accelerated station that is not",
                "Farmer's Delight's; a pack balanced around furnace timings may",
                "want it left alone."})
        public boolean accelerateSmokers = true;

        @Comment("Stars shown under food names, one per mastery rank.")
        public boolean starsEnabled = true;

        @Comment("Chance of a bonus effect when eating a mastered dish. Range 0.0 - 1.0.")
        public double effectChanceAtMaxRank = 0.25;
        @Comment("How long that effect lasts. Range 1 - 3600.")
        public int effectDurationSeconds = 20;
        @Comment({
                "Effects rolled from. Vanilla ids only; this mod registers none.",
                "",
                "minecraft:saturation is deliberately absent. It is not a buff: it",
                "refills food and saturation every tick it runs, so even a short one",
                "turns any dish into several full meals. Add it back if you want",
                "that, but it should be a choice."})
        public List<String> effects = new ArrayList<>(List.of(
                "minecraft:regeneration",
                "minecraft:speed",
                "minecraft:dig_speed"));
        @Comment("Amplifier at full mastery, scaled down by rank. 0 is level I. Range 0 - 9.")
        public int effectAmplifierAtMaxRank = 0;

        @Comment({
                "Extra saturation per star when eating a dish you have mastered.",
                "Hunger itself is never touched."})
        public float saturationPerRank = 0.2F;

        @Comment("Chance a preparation comes out perfect. Range 0.0 - 1.0.")
        public double perfectChanceAtMaxRank = 0.08;
        @Comment("How much stronger a perfect dish is than its star rating alone.")
        public double perfectEffectMultiplier = 2.0;

        @Comment("Chance of a free extra portion from the same ingredients.")
        public double extraPortionChanceAtMaxRank = 0.12;

        @Comment("Chance a cutting-board cook refunds a point of knife durability.")
        public double toolRefundChanceAtMaxTier = 0.25;

        @Comment("Mastery points the cook earns when someone else eats their cooking.")
        public int masteryPerMealServedToOthers = 2;
    }

    @Comment("When a dish stops being a mystery in the journal.")
    public static final class Discovery {
        @Comment({
                "COOK_RECIPE, EAT_OUTPUT, OBTAIN_OUTPUT, VIEW_RECIPE or ALWAYS_VISIBLE."})
        public DiscoveryMode mode = DiscoveryMode.COOK_RECIPE;
        @Comment("Whether merely obtaining a dish counts as discovering it.")
        public boolean countObtainingAsDiscovery = false;
        @Comment("Who shares a discovery. PERSONAL, TEAM or SERVER.")
        public SharedDiscovery shared = SharedDiscovery.PERSONAL;
    }

    @Comment("Cooking streaks.")
    public static final class Streaks {
        @Comment("MINECRAFT_DAYS, REAL_DAYS or DISABLED.")
        public StreakClock clock = StreakClock.MINECRAFT_DAYS;
        @Comment("Idle days forgiven before a streak resets. Range 1 - 365.")
        public int graceDays = 1;
        @Comment("Track a separate streak for cooking something different each day.")
        public boolean varietyStreakEnabled = true;
    }

    @Comment({
            "Challenges. Objectives are generated from what is installed,",
            "so a challenge is never impossible to complete."})
    public static final class Challenges {
        public boolean enabled = true;
        public boolean dailyEnabled = true;
        public boolean weeklyEnabled = false;
        @Comment("Schedule against the real-world clock instead of Minecraft days.")
        public boolean useRealTimeSchedule = false;
        @Comment("How many are offered at once. Range 0 - 20.")
        public int dailyChallengeCount = 3;
        public int weeklyChallengeCount = 3;
        @Comment("Rotate a bonus dish each Minecraft day.")
        public boolean recipeOfTheDayEnabled = true;
        public int recipeOfTheDayBonusXp = 50;
        public double challengeXpMultiplier = 1.0;
        @Comment({
                "Datapack rewards may run commands. Off unless a pack is trusted:",
                "a reward command runs with server authority."})
        public boolean allowCommandRewards = false;
    }

    @Comment("Leaderboards and the permission levels for the admin commands.")
    public static final class Multiplayer {
        @Comment("Off by default: the mod is not competitive unless a server wants it.")
        public boolean leaderboardsEnabled = false;
        @Comment({
                "Players appear only after opting in, by carrying the scoreboard tag:",
                "  tag @s add mealmastery_leaderboard"})
        public boolean leaderboardsOptInOnly = true;
        @Comment("How many places a leaderboard shows. Range 1 - 100.")
        public int leaderboardSize = 10;
        public boolean teamStatisticsEnabled = false;
        @Comment("Permission level to inspect another player's profile. Range 0 - 4.")
        public int inspectPermissionLevel = 2;
        @Comment("Permission level for /mealmastery reload and the admin commands.")
        public int adminPermissionLevel = 2;
    }

    @Comment("Who gets the credit when a machine does the cooking.")
    public static final class Automation {
        @Comment("NO_CREDIT, OWNER_CREDIT, REDUCED_CREDIT or FULL_CREDIT.")
        public AutomationCredit credit = AutomationCredit.NO_CREDIT;
        @Comment("Multiplier used by REDUCED_CREDIT.")
        public double reducedCreditMultiplier = 0.25;
        @Comment({
                "How long after using a workstation a gain is still credited, in ticks.",
                "Short on purpose: longer windows start crediting coincidences.",
                "Range 1 - 200."})
        public int attributionWindowTicks = 20;
        @Comment("How close to the workstation a dropped item still counts, in blocks.")
        public double attributionRadius = 3.0;
    }

    @Comment({
            "Which recipes become journal entries.",
            "The default rule is structural - a recipe counts when it produces",
            "something edible, whichever mod defines it. These narrow or widen it."})
    public static final class Compatibility {
        @Comment({
                "Extra blocks treated as cooking workstations, for addons with their own.",
                "\"modid:block\" or \"modid:block=modid:method\"."})
        public List<String> extraWorkstationBlocks = new ArrayList<>();
        @Comment("Menu classes never treated as culinary, on top of the built-in list.")
        public List<String> excludedMenuClasses = new ArrayList<>();
        @Comment("Recipe type ids to ignore, e.g. \"farmersdelight:cutting\".")
        public List<String> excludedRecipeTypes = new ArrayList<>();
        @Comment("Dish item ids to ignore.")
        public List<String> excludedItems = new ArrayList<>();
        @Comment("Mod ids to ignore entirely.")
        public List<String> excludedMods = new ArrayList<>();
        @Comment("When not empty, only these mods contribute dishes.")
        public List<String> includedOnlyMods = new ArrayList<>();
        @Comment("A dish must carry one of these item tags to count.")
        public List<String> requiredItemTags = new ArrayList<>();
        @Comment("Set false to accept recipes whose output is not food.")
        public boolean requireEdibleOutput = true;
        @Comment({
                "Whether vanilla recipes earn mastery.",
                "On by default: bread and a baked potato are cooking too. Set false",
                "to turn the whole minecraft namespace off in one go, for a pack",
                "that wants the journal to be about its food addons."})
        public boolean trackVanillaRecipes = true;
    }

    @Comment("Knobs you should not need. Change them if a profile is misbehaving.")
    public static final class Advanced {
        @Comment("Verbose logging for tracking and attribution.")
        public boolean debugLogging = false;
        @Comment("Milliseconds between profile autosaves. Range 5000 - 3600000.")
        public int autosaveIntervalMillis = 60_000;
        @Comment("Recent-activity entries retained per profile. Range 1 - 64.")
        public int activityHistorySize = 32;
        @Comment("How many dishes a player may pin at once. Range 1 - 16.")
        public int maxPinnedRecipes = 3;
        @Comment({
                "Journal entries sent per network packet. Range 8 - 256.",
                "Lower this only if a very large modpack trips the packet size limit."})
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
