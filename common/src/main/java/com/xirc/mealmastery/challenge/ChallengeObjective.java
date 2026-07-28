package com.xirc.mealmastery.challenge;

import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

/**
 * One condition inside a challenge.
 *
 * <p>Objectives are datapack data, so they are deliberately flat: a type, a
 * single {@code value} whose meaning the type defines, and a target amount.
 * That keeps the JSON readable and the validator simple.</p>
 *
 * @param type        what is being counted
 * @param value       the thing being counted, interpreted per type; may be blank
 *                    to mean "anything"
 * @param amount      how many are needed
 * @param uniqueOnly  count distinct subjects rather than repeats
 */
public record ChallengeObjective(Type type, String value, int amount, boolean uniqueOnly) {

    public enum Type {
        /** {@code value} = dish item id, blank for any dish. */
        COOK_RECIPE,
        /** {@code value} = category id, e.g. {@code mealmastery:meal}. */
        COOK_CATEGORY,
        /** {@code value} = source mod id. */
        COOK_FROM_MOD,
        /** {@code value} = ingredient item id. */
        USE_INGREDIENT,
        /** {@code value} = item tag id. */
        USE_INGREDIENT_TAG,
        /** {@code value} = cooking method id. */
        USE_METHOD,
        /** Discover any dish. */
        DISCOVER_RECIPE,
        /** Reach the top mastery rank on any dish. */
        MASTER_RECIPE,
        /** {@code value} = dish item id, blank for any. */
        EAT_RECIPE,
        /** Serve portions from feasts and shared dishes. */
        SERVE_PORTIONS,
        /** {@code amount} distinct dishes in a row without repeating one. */
        PREPARE_VARIETY,
        /** {@code amount} distinct dishes inside one Minecraft day. */
        UNIQUE_RECIPES_IN_ONE_DAY,
        /** {@code amount} = the Cooking Level to reach. */
        REACH_LEVEL,
        /** {@code value} = another challenge id that must be completed first. */
        COMPLETE_CHALLENGE;

        public static Type byName(String raw) {
            if (raw == null) {
                return null;
            }
            for (Type type : values()) {
                if (type.name().equalsIgnoreCase(raw)) {
                    return type;
                }
            }
            return null;
        }
    }

    public ChallengeObjective {
        value = value == null ? "" : value;
        amount = Math.max(1, amount);
    }

    public boolean hasValue() {
        return !value.isBlank();
    }

    public ResourceLocation valueAsId() {
        return hasValue() ? ResourceLocation.tryParse(value) : null;
    }

    /**
     * @throws ChallengeParseException when the object is not a usable objective;
     *         the loader turns that into a skipped file plus a log line rather
     *         than a crash
     */
    public static ChallengeObjective fromJson(JsonObject json) {
        Type type = Type.byName(json.has("type") ? json.get("type").getAsString() : null);
        if (type == null) {
            throw new ChallengeParseException("objective has no recognised \"type\"");
        }
        String value = json.has("value") ? json.get("value").getAsString() : "";
        int amount = json.has("amount") ? json.get("amount").getAsInt() : 1;
        boolean unique = json.has("unique") && json.get("unique").getAsBoolean();

        if (requiresValue(type) && value.isBlank()) {
            throw new ChallengeParseException(type + " objectives need a \"value\"");
        }
        if (expectsId(type) && ResourceLocation.tryParse(value) == null) {
            throw new ChallengeParseException(
                    type + " objective has an unparseable id: \"" + value + "\"");
        }
        return new ChallengeObjective(type, value, amount, unique);
    }

    private static boolean requiresValue(Type type) {
        return switch (type) {
            case COOK_CATEGORY, COOK_FROM_MOD, USE_INGREDIENT, USE_INGREDIENT_TAG,
                 USE_METHOD, COMPLETE_CHALLENGE -> true;
            default -> false;
        };
    }

    private static boolean expectsId(Type type) {
        return switch (type) {
            case COOK_FROM_MOD -> false;
            default -> requiresValue(type);
        };
    }

    public String translationKey() {
        return "mealmastery.objective." + type.name().toLowerCase(java.util.Locale.ROOT);
    }
}
