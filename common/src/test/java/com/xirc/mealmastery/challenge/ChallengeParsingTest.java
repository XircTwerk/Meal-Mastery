package com.xirc.mealmastery.challenge;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChallengeParsingTest {
    private static final Gson GSON = new Gson();
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("mealmastery", "soup_season");

    private static JsonObject json(String raw) {
        return GSON.fromJson(raw, JsonObject.class);
    }

    @Test
    void readsAWellFormedChallenge() {
        ChallengeDefinition definition = ChallengeDefinition.fromJson(ID, json("""
                {
                  "scope": "PERMANENT",
                  "objectives": [
                    { "type": "COOK_CATEGORY", "value": "mealmastery:meal", "amount": 5 },
                    { "type": "COOK_RECIPE", "amount": 3, "unique": true }
                  ],
                  "rewards": { "cooking_xp": 120, "badge": "mealmastery:soup_specialist" }
                }
                """));

        assertEquals(ChallengeDefinition.Scope.PERMANENT, definition.scope());
        assertEquals(2, definition.objectives().size());
        assertEquals(ChallengeObjective.Type.COOK_CATEGORY, definition.objectives().get(0).type());
        assertEquals(5, definition.objectives().get(0).amount());
        assertTrue(definition.objectives().get(1).uniqueOnly());
        assertEquals(120, definition.rewards().cookingXp());
        assertNull(definition.prerequisite());
    }

    @Test
    void rejectsAChallengeWithNoObjectives() {
        assertThrows(ChallengeParseException.class,
                () -> ChallengeDefinition.fromJson(ID, json("{ \"objectives\": [] }")));
        assertThrows(ChallengeParseException.class,
                () -> ChallengeDefinition.fromJson(ID, json("{}")));
    }

    @Test
    void rejectsUnknownObjectiveTypes() {
        assertThrows(ChallengeParseException.class, () -> ChallengeObjective.fromJson(
                json("{ \"type\": \"BAKE_A_CAKE_SOMEHOW\" }")));
    }

    @Test
    void rejectsObjectivesMissingTheirValue() {
        // "cook 5 of a category" is meaningless without naming the category.
        assertThrows(ChallengeParseException.class, () -> ChallengeObjective.fromJson(
                json("{ \"type\": \"COOK_CATEGORY\", \"amount\": 5 }")));
        assertThrows(ChallengeParseException.class, () -> ChallengeObjective.fromJson(
                json("{ \"type\": \"USE_METHOD\", \"value\": \"not a valid id\" }")));
    }

    @Test
    void aBlankValueMeansAnything() {
        ChallengeObjective objective = ChallengeObjective.fromJson(
                json("{ \"type\": \"COOK_RECIPE\", \"amount\": 4 }"));
        assertEquals(false, objective.hasValue());
        assertEquals(4, objective.amount());
    }

    @Test
    void modObjectivesAcceptAPlainNamespace() {
        ChallengeObjective objective = ChallengeObjective.fromJson(
                json("{ \"type\": \"COOK_FROM_MOD\", \"value\": \"netherdelight\", \"amount\": 3 }"));
        assertEquals("netherdelight", objective.value());
    }

    @Test
    void commandRewardsAreReadButRemainGatedElsewhere() {
        ChallengeReward reward = ChallengeReward.fromJson(json("""
                { "commands": ["say hello"], "items": ["minecraft:bread 3"], "vanilla_xp": 20 }
                """));
        assertEquals(1, reward.commands().size());
        assertEquals(1, reward.items().size());
        assertEquals(20, reward.vanillaXp());
    }

    @Test
    void progressCountsDistinctSubjectsForUniqueObjectives() {
        ChallengeObjective unique = new ChallengeObjective(
                ChallengeObjective.Type.COOK_RECIPE, "", 3, true);
        ChallengeState state = new ChallengeState(1);

        ResourceLocation stew = ResourceLocation.fromNamespaceAndPath("farmersdelight", "beef_stew");
        assertTrue(state.advanceUnique(0, stew));
        assertEquals(false, state.advanceUnique(0, stew), "the same dish must not count twice");
        state.advanceUnique(0, ResourceLocation.fromNamespaceAndPath("farmersdelight", "onion_soup"));
        assertEquals(2, state.progress(0));
        assertEquals(false, state.satisfies(java.util.List.of(unique)));

        state.advanceUnique(0, ResourceLocation.fromNamespaceAndPath("farmersdelight", "mixed_salad"));
        assertTrue(state.satisfies(java.util.List.of(unique)));
    }

    @Test
    void raiseTracksABestRatherThanATotal() {
        ChallengeState state = new ChallengeState(1);
        assertTrue(state.raise(0, 5));
        assertEquals(false, state.raise(0, 3), "a worse result must not overwrite the best");
        assertEquals(5, state.progress(0));
    }

    @Test
    void completedChallengesStopAccumulating() {
        ChallengeState state = new ChallengeState(1);
        state.complete(12L);
        assertEquals(false, state.advance(0, 5));
        assertEquals(12L, state.completedDay());
    }

    @Test
    void stateRoundTripsThroughNbt() {
        ChallengeState state = new ChallengeState(2);
        state.advance(0, 4);
        state.advanceUnique(1, ResourceLocation.fromNamespaceAndPath("farmersdelight", "beef_stew"));

        ChallengeState loaded = ChallengeState.load(state.save(), 2);
        assertEquals(4, loaded.progress(0));
        assertEquals(1, loaded.progress(1));
        assertEquals(false, loaded.advanceUnique(1, ResourceLocation.fromNamespaceAndPath("farmersdelight", "beef_stew")),
                "the restored state still remembers which subjects were counted");
    }
}
