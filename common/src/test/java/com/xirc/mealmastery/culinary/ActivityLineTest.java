package com.xirc.mealmastery.culinary;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the activity feed's format strings against their arguments.
 *
 * <p>A translatable component whose format string wants more arguments than it
 * is given renders as the raw template — the journal showed a literal
 * "%s reached %s" for exactly this reason. Nothing about that fails to compile,
 * so the arity has to be asserted somewhere.</p>
 */
class ActivityLineTest {

    /** How many arguments OverviewPage#argumentsFor supplies for each type. */
    private static final Map<ActivityEntry.Type, Integer> EXPECTED_ARGUMENTS = Map.of(
            ActivityEntry.Type.RECIPE_DISCOVERED, 1,
            ActivityEntry.Type.MEAL_PREPARED, 1,
            ActivityEntry.Type.MASTERY_RANK, 2,
            ActivityEntry.Type.MEAL_MASTERED, 1,
            ActivityEntry.Type.LEVEL_UP, 1,
            ActivityEntry.Type.METHOD_DISCOVERED, 1,
            ActivityEntry.Type.INGREDIENT_DISCOVERED, 1,
            ActivityEntry.Type.CHALLENGE_COMPLETED, 1,
            ActivityEntry.Type.MILESTONE_REACHED, 1,
            ActivityEntry.Type.COLLECTION_COMPLETED, 1);

    private static JsonObject language() throws Exception {
        try (InputStream stream = ActivityLineTest.class
                .getResourceAsStream("/assets/mealmastery/lang/en_us.json")) {
            assertNotNull(stream, "en_us.json must be on the classpath");
            return JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject();
        }
    }

    private static int placeholders(String template) {
        int count = 0;
        for (int i = 0; i < template.length() - 1; i++) {
            if (template.charAt(i) == '%') {
                char next = template.charAt(i + 1);
                if (next == '%') {
                    i++;
                } else if (next == 's' || next == 'd') {
                    count++;
                }
            }
        }
        return count;
    }

    @Test
    void everyActivityTypeHasATranslation() throws Exception {
        JsonObject language = language();
        for (ActivityEntry.Type type : ActivityEntry.Type.values()) {
            assertTrue(language.has(type.translationKey()),
                    "missing translation for " + type.translationKey());
        }
    }

    @Test
    void everyActivityLineGetsAsManyArgumentsAsItAsksFor() throws Exception {
        JsonObject language = language();
        for (ActivityEntry.Type type : ActivityEntry.Type.values()) {
            String template = language.get(type.translationKey()).getAsString();
            assertEquals(EXPECTED_ARGUMENTS.get(type), placeholders(template),
                    type.translationKey() + " = \"" + template
                            + "\" does not match the arguments the journal supplies");
        }
    }
}
