package com.xirc.mealmastery.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigrationTest {

    @Test
    void aTunedJsonFileIsCarriedIntoToml(@TempDir Path dir) throws Exception {
        // A real file from before the format changed, with values that differ
        // from the defaults so a silent reset would be visible.
        Files.writeString(dir.resolve("mealmastery-server.json"), """
                {
                  "mastery": {
                    "thresholds": [1, 10, 30, 75, 150],
                    "bonuses": {
                      "cookingSpeedAtMaxRank": 2.5,
                      "accelerateSmokers": false,
                      "effects": ["minecraft:regeneration", "minecraft:speed"]
                    }
                  },
                  "compatibility": { "trackVanillaRecipes": false }
                }
                """);

        ConfigManager.initialize(dir);
        ServerConfig config = ConfigManager.server();

        assertEquals(2.5, config.mastery.bonuses.cookingSpeedAtMaxRank);
        assertFalse(config.mastery.bonuses.accelerateSmokers);
        assertFalse(config.compatibility.trackVanillaRecipes);
        assertEquals(List.of(1, 10, 30, 75, 150), config.mastery.thresholds);
        assertEquals(List.of("minecraft:regeneration", "minecraft:speed"),
                config.mastery.bonuses.effects);
        // Untouched sections still arrive at their defaults.
        assertEquals(75, config.progression.discoveryBonusXp);

        assertTrue(Files.exists(dir.resolve("mealmastery-server.toml")), "TOML was written");
        assertFalse(Files.exists(dir.resolve("mealmastery-server.json")), "JSON was moved aside");
        assertTrue(Files.exists(dir.resolve("mealmastery-server.json.bak")), "original was kept");

        // And the file it just wrote reads back as the same configuration.
        ConfigManager.reload();
        assertEquals(2.5, ConfigManager.server().mastery.bonuses.cookingSpeedAtMaxRank);
        assertFalse(ConfigManager.server().compatibility.trackVanillaRecipes);
    }

    @Test
    void anUnreadableFileIsQuarantinedRatherThanFatal(@TempDir Path dir) throws Exception {
        Files.writeString(dir.resolve("mealmastery-server.toml"), "[mastery]\nnot a valid line\n");
        ConfigManager.initialize(dir);

        assertEquals(new ServerConfig().mastery.bonuses.cookingSpeedAtMaxRank,
                ConfigManager.server().mastery.bonuses.cookingSpeedAtMaxRank);
        assertTrue(Files.exists(dir.resolve("mealmastery-server.toml.invalid")),
                "the unreadable file is kept for the admin to fix");
    }
}
