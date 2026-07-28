package com.xirc.mealmastery.challenge;

import com.xirc.mealmastery.MealMasteryLog;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Every challenge definition currently loaded.
 *
 * <p>Replaced wholesale on datapack reload. Definitions that fail validation
 * never make it in, so nothing downstream has to defend against a malformed
 * objective.</p>
 */
public final class ChallengeRegistry {

    public static final ChallengeRegistry EMPTY = new ChallengeRegistry(Map.of());

    private static volatile ChallengeRegistry current = EMPTY;

    private final Map<ResourceLocation, ChallengeDefinition> definitions;

    private ChallengeRegistry(Map<ResourceLocation, ChallengeDefinition> definitions) {
        this.definitions = Map.copyOf(definitions);
    }

    public static ChallengeRegistry current() {
        return current;
    }

    public static void replace(Collection<ChallengeDefinition> definitions) {
        Map<ResourceLocation, ChallengeDefinition> byId = new LinkedHashMap<>();
        for (ChallengeDefinition definition : definitions) {
            byId.put(definition.id(), definition);
        }

        // A chain whose prerequisite is missing would be permanently
        // unreachable, so it is dropped with an explanation rather than sitting
        // in the journal forever (never show an impossible objective).
        byId.values().removeIf(definition -> {
            if (definition.prerequisite() != null && !byId.containsKey(definition.prerequisite())) {
                MealMasteryLog.LOGGER.warn(
                        "Challenge {} requires {}, which is not loaded. It has been dropped.",
                        definition.id(), definition.prerequisite());
                return true;
            }
            return false;
        });

        current = new ChallengeRegistry(byId);
        MealMasteryLog.LOGGER.info("Loaded {} challenge definition(s)", byId.size());
    }

    public static void clear() {
        current = EMPTY;
    }

    public ChallengeDefinition get(ResourceLocation id) {
        return definitions.get(id);
    }

    public Collection<ChallengeDefinition> all() {
        return definitions.values();
    }

    public List<ChallengeDefinition> byScope(ChallengeDefinition.Scope scope) {
        List<ChallengeDefinition> result = new ArrayList<>();
        for (ChallengeDefinition definition : definitions.values()) {
            if (definition.scope() == scope) {
                result.add(definition);
            }
        }
        return result;
    }

    public int size() {
        return definitions.size();
    }
}
