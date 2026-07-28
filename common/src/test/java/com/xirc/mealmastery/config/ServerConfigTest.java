package com.xirc.mealmastery.config;

import com.xirc.mealmastery.config.ConfigEnums.MasteryRewards;
import com.xirc.mealmastery.config.ConfigEnums.Preset;
import com.xirc.mealmastery.mastery.MasteryCurve;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerConfigTest {

    @Test
    void defaultsChangeNothingAboutFarmersDelight() {
        // Installing Meal Mastery must not rebalance anything.
        ServerConfig config = new ServerConfig();
        assertEquals(MasteryRewards.COSMETIC_ONLY, config.mastery.rewards);
        assertEquals(ConfigEnums.AutomationCredit.NO_CREDIT, config.automation.credit);
        assertEquals(ConfigEnums.SharedDiscovery.PERSONAL, config.discovery.shared);
        assertFalse(config.multiplayer.leaderboardsEnabled, "leaderboards are opt-in");
        assertFalse(config.challenges.useRealTimeSchedule, "real-time scheduling defaults off");
        assertFalse(config.challenges.allowCommandRewards);
        assertTrue(config.validate().isEmpty(), "defaults must already be valid");
    }

    @Test
    void vanillaRecipesAndSmokersAreOnByDefault() {
        // Both are opt-out: bread is cooking, and the smoker was in the speed
        // bonus before either became configurable.
        ServerConfig config = new ServerConfig();
        assertTrue(config.compatibility.trackVanillaRecipes);
        assertTrue(config.mastery.bonuses.accelerateSmokers);
    }

    @Test
    void nonsenseValuesAreClampedRatherThanRejected() {
        ServerConfig config = new ServerConfig();
        config.progression.xpMultiplier = -4.0;
        config.progression.maxLevel = 0;
        config.antiFarming.minimumMultiplier = 9.0;
        config.multiplayer.adminPermissionLevel = 99;
        config.advanced.journalPageSize = 1;

        List<String> issues = config.validate();

        assertEquals(5, issues.size(), issues.toString());
        assertEquals(0.0, config.progression.xpMultiplier);
        assertEquals(1, config.progression.maxLevel);
        assertEquals(1.0, config.antiFarming.minimumMultiplier);
        assertEquals(4, config.multiplayer.adminPermissionLevel);
        assertEquals(8, config.advanced.journalPageSize);
    }

    @Test
    void notANumberIsRecoveredRatherThanPropagated() {
        ServerConfig config = new ServerConfig();
        config.progression.xpMultiplier = Double.NaN;
        config.validate();
        assertEquals(0.0, config.progression.xpMultiplier);
    }

    @Test
    void emptyMasteryThresholdsAreRestored() {
        ServerConfig config = new ServerConfig();
        config.mastery.thresholds = new ArrayList<>();
        List<String> issues = config.validate();

        assertEquals(1, issues.size());
        assertEquals(MasteryCurve.DEFAULT_THRESHOLDS, config.mastery.thresholds);
    }

    @Test
    void antiFarmingTiersAreSortedAndCleaned() {
        ServerConfig config = new ServerConfig();
        config.antiFarming.tiers = new ArrayList<>(List.of(
                new ServerConfig.AntiFarming.Tier(30, 0.3),
                new ServerConfig.AntiFarming.Tier(5, 0.75)));
        config.antiFarming.tiers.add(null);

        config.validate();

        assertEquals(2, config.antiFarming.tiers.size());
        assertEquals(5, config.antiFarming.tiers.get(0).upToPreparations);
        assertEquals(30, config.antiFarming.tiers.get(1).upToPreparations);
    }

    @Test
    void curvesAreDerivedFromConfiguration() {
        ServerConfig config = new ServerConfig();
        config.progression.levelCurveBase = 200;
        config.progression.levelCurveLinear = 0;
        assertEquals(200L, config.progression.toCurve().xpForLevel(3));

        config.mastery.thresholds = List.of(2, 4, 6, 8, 10);
        assertEquals(List.of(2, 4, 6, 8, 10), config.mastery.toCurve().thresholds());
    }

    @Test
    void everyPresetProducesAValidConfiguration() {
        for (Preset preset : Preset.values()) {
            ServerConfig serverConfig = ConfigPresets.applyServer(preset);
            assertTrue(serverConfig.validate().isEmpty(), preset + " server config");

            ClientConfig clientConfig = ConfigPresets.applyClient(preset);
            assertTrue(clientConfig.validate().isEmpty(), preset + " client config");
        }
    }

    @Test
    void cosmeticPresetRemovesEveryGameplayBonus() {
        ServerConfig config = ConfigPresets.applyServer(Preset.COSMETIC);
        assertEquals(MasteryRewards.COSMETIC_ONLY, config.mastery.rewards);
        assertEquals(0.0, config.mastery.xpBonusPerMasteredDish);
        assertEquals(0.0, config.mastery.xpBonusCap);
    }

    @Test
    void reducedMotionOverridesAnimationSpeed() {
        ClientConfig config = new ClientConfig();
        config.accessibility.reducedMotion = true;
        config.accessibility.animationSpeed = 2.0F;
        config.validate();
        assertEquals(0.0F, config.accessibility.animationSpeed);
    }

    @Test
    void clientDefaultsDoNotClutterTheScreen() {
        ClientConfig config = new ClientConfig();
        assertFalse(config.hud.enabled, "the HUD is off by default");
        assertFalse(config.tooltips.showNutrition, "nutrition mods keep that tooltip section");
        assertTrue(config.tooltips.requireModifier);
    }
}
