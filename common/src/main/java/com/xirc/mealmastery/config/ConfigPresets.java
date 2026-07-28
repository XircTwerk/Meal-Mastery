package com.xirc.mealmastery.config;

import com.xirc.mealmastery.config.ConfigEnums.DiscoveryMode;
import com.xirc.mealmastery.config.ConfigEnums.MasteryRewards;
import com.xirc.mealmastery.config.ConfigEnums.Preset;
import com.xirc.mealmastery.config.ConfigEnums.UnknownRecipeDisplay;

import java.util.List;

/**
 * Optional starting points.
 *
 * <p>Presets are never applied automatically — a fresh install always writes
 * the plain defaults. Applying one rewrites configuration only; it cannot and
 * does not touch a player profile.</p>
 */
public final class ConfigPresets {
    private ConfigPresets() {
    }

    public static ServerConfig applyServer(Preset preset) {
        ServerConfig config = new ServerConfig();
        switch (preset) {
            case VANILLA_PLUS -> {
                // Journal and statistics; progression exists but stays quiet.
                config.progression.xpMultiplier = 0.75;
                config.mastery.rewards = MasteryRewards.COSMETIC_ONLY;
                config.challenges.enabled = false;
                config.challenges.dailyEnabled = false;
                config.challenges.recipeOfTheDayEnabled = false;
            }
            case COZY -> {
                config.progression.xpMultiplier = 1.5;
                config.progression.discoveryBonusXp = 110;
                config.mastery.thresholds = List.of(1, 3, 10, 25, 50);
                config.antiFarming.fullXpPreparations = 3;
                config.antiFarming.minimumMultiplier = 0.35;
                config.streaks.graceDays = 3;
                config.discovery.mode = DiscoveryMode.COOK_RECIPE;
            }
            case COMPLETIONIST -> {
                config.progression.discoveryBonusXp = 120;
                config.progression.ingredientDiscoveryXp = 20;
                config.progression.methodDiscoveryXp = 40;
                config.progression.experimentationXp = 14;
                config.antiFarming.fullXpPreparations = 1;
                config.antiFarming.minimumMultiplier = 0.1;
                config.challenges.dailyEnabled = true;
                config.challenges.weeklyEnabled = true;
            }
            case SERVER -> {
                config.mastery.rewards = MasteryRewards.LIGHT;
                config.multiplayer.leaderboardsEnabled = true;
                config.multiplayer.leaderboardsOptInOnly = true;
                config.multiplayer.teamStatisticsEnabled = true;
                config.challenges.dailyEnabled = true;
                config.antiFarming.enabled = true;
            }
            case COSMETIC -> {
                config.mastery.rewards = MasteryRewards.COSMETIC_ONLY;
                config.mastery.xpBonusPerMasteredDish = 0.0;
                config.mastery.xpBonusCap = 0.0;
                config.challenges.allowCommandRewards = false;
            }
        }
        config.validate();
        return config;
    }

    public static ClientConfig applyClient(Preset preset) {
        ClientConfig config = new ClientConfig();
        switch (preset) {
            case VANILLA_PLUS -> config.hud.enabled = false;
            case COZY -> {
                config.journal.cozyMode = true;
                config.journal.unknownDisplay = UnknownRecipeDisplay.NAME_ONLY;
                config.notifications.xpPopups = false;
            }
            case COMPLETIONIST -> {
                config.journal.completionistMode = true;
                config.journal.unknownDisplay = UnknownRecipeDisplay.SILHOUETTE;
                config.hud.enabled = true;
                config.hud.onlyWhileActive = true;
            }
            case SERVER, COSMETIC -> {
                // Nothing client-side is implied by these; they are server shapes.
            }
        }
        config.validate();
        return config;
    }
}
