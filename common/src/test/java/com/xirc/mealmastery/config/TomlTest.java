package com.xirc.mealmastery.config;

import com.xirc.mealmastery.config.ConfigEnums.MasteryRewards;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TomlTest {

    private static ServerConfig roundTrip(ServerConfig config) {
        return Toml.read(Toml.write(config, List.of()), ServerConfig.class);
    }

    @Test
    void defaultsSurviveARoundTrip() {
        ServerConfig original = new ServerConfig();
        ServerConfig copy = roundTrip(original);

        assertEquals(original.progression.baseXpPerPreparation, copy.progression.baseXpPerPreparation);
        assertEquals(original.mastery.rewards, copy.mastery.rewards);
        assertEquals(original.mastery.thresholds, copy.mastery.thresholds);
        assertEquals(original.mastery.bonuses.cookingSpeedAtMaxRank,
                copy.mastery.bonuses.cookingSpeedAtMaxRank);
        assertEquals(original.mastery.bonuses.saturationPerRank,
                copy.mastery.bonuses.saturationPerRank);
        assertEquals(original.mastery.bonuses.effects, copy.mastery.bonuses.effects);
        assertEquals(original.compatibility.trackVanillaRecipes,
                copy.compatibility.trackVanillaRecipes);
        assertTrue(copy.validate().isEmpty(), "a round trip must not knock anything out of range");
    }

    @Test
    void arraysOfSectionsSurviveARoundTrip() {
        // antiFarming.tiers is the only array of tables in either document.
        ServerConfig copy = roundTrip(new ServerConfig());
        assertEquals(2, copy.antiFarming.tiers.size());
        assertEquals(5, copy.antiFarming.tiers.get(0).upToPreparations);
        assertEquals(0.75, copy.antiFarming.tiers.get(0).multiplier);
        assertEquals(15, copy.antiFarming.tiers.get(1).upToPreparations);
        assertEquals(0.50, copy.antiFarming.tiers.get(1).multiplier);
    }

    @Test
    void editedValuesAreReadBack() {
        ServerConfig config = new ServerConfig();
        config.mastery.bonuses.cookingSpeedAtMaxRank = 2.5;
        config.mastery.bonuses.accelerateSmokers = false;
        config.compatibility.trackVanillaRecipes = false;
        config.mastery.rewards = MasteryRewards.STANDARD;
        config.mastery.thresholds = List.of(2, 20, 60);
        config.compatibility.excludedMods = List.of("someaddon", "another");

        ServerConfig copy = roundTrip(config);
        assertEquals(2.5, copy.mastery.bonuses.cookingSpeedAtMaxRank);
        assertEquals(false, copy.mastery.bonuses.accelerateSmokers);
        assertEquals(false, copy.compatibility.trackVanillaRecipes);
        assertEquals(MasteryRewards.STANDARD, copy.mastery.rewards);
        assertEquals(List.of(2, 20, 60), copy.mastery.thresholds);
        assertEquals(List.of("someaddon", "another"), copy.compatibility.excludedMods);
    }

    @Test
    void scalarsAreWrittenBeforeNestedSections() {
        // TOML gives a bare key to whichever section precedes it, so a scalar
        // emitted after [mastery.bonuses] would silently move into it.
        String text = Toml.write(new ServerConfig(), List.of());
        int bonuses = text.indexOf("[mastery.bonuses]");
        int rewards = text.indexOf("rewards =");
        assertTrue(rewards > 0 && bonuses > rewards,
                "mastery's own keys must precede [mastery.bonuses]");

        int tiers = text.indexOf("[[antiFarming.tiers]]");
        int minimum = text.indexOf("minimumMultiplier =");
        assertTrue(minimum > 0 && tiers > minimum,
                "antiFarming's own keys must precede [[antiFarming.tiers]]");
    }

    @Test
    void commentsAreWrittenAboveTheirKey() {
        String text = Toml.write(new ServerConfig(), List.of("Header line"));
        assertTrue(text.startsWith("# Header line\n"));
        assertTrue(text.contains("# Extra XP the first time an ingredient is ever used.\n"
                + "ingredientDiscoveryXp = 10"), "a field comment sits directly above its key");
        assertTrue(text.contains("# Extra XP the first time a dish is ever prepared.\n"
                + "# Deliberately far larger than a repeat: exploring should beat grinding.\n"
                + "discoveryBonusXp = 75"), "a multi-line comment keeps its order");
    }

    @Test
    void aMissingKeyKeepsItsDefault() {
        // How a config written by an older build picks up a new setting.
        ServerConfig copy = Toml.read("[mastery]\nenabled = false\n", ServerConfig.class);
        assertEquals(false, copy.mastery.enabled);
        assertEquals(new ServerConfig().mastery.bonuses.cookingSpeedAtMaxRank,
                copy.mastery.bonuses.cookingSpeedAtMaxRank);
    }

    @Test
    void anUnknownKeyIsIgnoredRatherThanFatal() {
        // How downgrading survives: a key this build has never heard of.
        ServerConfig copy = Toml.read(
                "[mastery]\nenabled = false\nsomethingFromTheFuture = 7\n", ServerConfig.class);
        assertEquals(false, copy.mastery.enabled);
    }

    @Test
    void commentsAndBlankLinesAreIgnored() {
        ServerConfig copy = Toml.read("""
                # a leading comment

                [mastery]
                # explaining the next line
                enabled = false   # trailing comment
                """, ServerConfig.class);
        assertEquals(false, copy.mastery.enabled);
    }

    @Test
    void aHashInsideAStringIsNotAComment() {
        ServerConfig copy = Toml.read(
                "[compatibility]\nexcludedMods = [\"a#b\", \"c\"]\n", ServerConfig.class);
        assertEquals(List.of("a#b", "c"), copy.compatibility.excludedMods);
    }

    @Test
    void anUnreadableValueCostsOnlyItsOwnKey() {
        ServerConfig copy = Toml.read(
                "[mastery]\nrewards = \"NOT_A_REWARD\"\nenabled = false\n", ServerConfig.class);
        assertEquals(MasteryRewards.COSMETIC_ONLY, copy.mastery.rewards, "keeps its default");
        assertEquals(false, copy.mastery.enabled, "the rest of the section still loads");
    }

    @Test
    void aMalformedDocumentIsRejectedOutright() {
        assertThrows(Toml.TomlException.class,
                () -> Toml.read("[mastery]\nthis line has no equals sign\n", ServerConfig.class));
        assertThrows(Toml.TomlException.class,
                () -> Toml.read("[mastery]\nenabled = \"unterminated\n", ServerConfig.class));
    }

    @Test
    void clientConfigAlsoRoundTrips() {
        ClientConfig original = new ClientConfig();
        ClientConfig copy = Toml.read(Toml.write(original, List.of()), ClientConfig.class);
        assertEquals(original.journal.sidePanel, copy.journal.sidePanel);
        assertEquals(original.hud.enabled, copy.hud.enabled);
        assertEquals(original.tooltips.modifier, copy.tooltips.modifier);
        assertTrue(copy.validate().isEmpty());
    }
}
