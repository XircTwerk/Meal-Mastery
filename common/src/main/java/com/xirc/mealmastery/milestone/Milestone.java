package com.xirc.mealmastery.milestone;

import com.google.gson.JsonObject;
import com.xirc.mealmastery.Constants;
import com.xirc.mealmastery.challenge.ChallengeParseException;
import com.xirc.mealmastery.culinary.CulinaryProfile;
import com.xirc.mealmastery.culinary.LevelCurve;
import com.xirc.mealmastery.mastery.MasteryCurve;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Locale;

/**
 * A one-off "you have come a long way" moment.
 *
 * <p>Data-driven and deliberately sparse. The design is explicit that cozy
 * cooking must not turn into constant achievement spam, so the built-in set is
 * short and the thresholds are far apart.</p>
 */
public record Milestone(ResourceLocation id, Type type, long threshold, ResourceLocation badge) {

    public enum Type {
        MEALS_PREPARED,
        UNIQUE_RECIPES_PREPARED,
        RECIPES_DISCOVERED,
        RECIPES_MASTERED,
        INGREDIENTS_DISCOVERED,
        COOKING_LEVEL,
        PORTIONS_SERVED;

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

    public String translationKey() {
        return "mealmastery.milestone." + id.getNamespace() + "." + id.getPath();
    }

    public long valueFor(CulinaryProfile profile, LevelCurve levelCurve, MasteryCurve masteryCurve) {
        return switch (type) {
            case MEALS_PREPARED -> profile.stats().mealsPrepared();
            case UNIQUE_RECIPES_PREPARED -> profile.uniquePreparedCount();
            case RECIPES_DISCOVERED -> profile.discoveredCount();
            case RECIPES_MASTERED -> profile.masteredCount(masteryCurve);
            case INGREDIENTS_DISCOVERED -> profile.ingredientsDiscoveredCount();
            case COOKING_LEVEL -> profile.cookingLevel(levelCurve);
            case PORTIONS_SERVED -> profile.stats().portionsServed();
        };
    }

    public static Milestone fromJson(ResourceLocation id, JsonObject json) {
        Type type = Type.byName(json.has("type") ? json.get("type").getAsString() : null);
        if (type == null) {
            throw new ChallengeParseException("milestone has no recognised \"type\"");
        }
        if (!json.has("threshold")) {
            throw new ChallengeParseException("milestone has no \"threshold\"");
        }
        long threshold = json.get("threshold").getAsLong();
        if (threshold <= 0L) {
            throw new ChallengeParseException("milestone \"threshold\" must be positive");
        }
        ResourceLocation badge = null;
        if (json.has("badge")) {
            badge = ResourceLocation.tryParse(json.get("badge").getAsString());
            if (badge == null) {
                throw new ChallengeParseException("milestone \"badge\" is not a valid id");
            }
        }
        return new Milestone(id, type, threshold, badge);
    }

    private static Milestone builtIn(String path, Type type, long threshold) {
        return new Milestone(new ResourceLocation(Constants.MOD_ID, path), type, threshold, null);
    }

    /** Shipped as data under {@code data/mealmastery/mealmastery/milestones}; mirrored here for tests. */
    public static List<Milestone> builtIns() {
        return List.of(
                builtIn("first_meal", Type.MEALS_PREPARED, 1),
                builtIn("ten_meals", Type.MEALS_PREPARED, 10),
                builtIn("hundred_meals", Type.MEALS_PREPARED, 100),
                builtIn("thousand_meals", Type.MEALS_PREPARED, 1_000),
                builtIn("twenty_five_recipes", Type.UNIQUE_RECIPES_PREPARED, 25),
                builtIn("fifty_recipes", Type.UNIQUE_RECIPES_PREPARED, 50),
                builtIn("ten_mastered", Type.RECIPES_MASTERED, 10),
                builtIn("fifty_ingredients", Type.INGREDIENTS_DISCOVERED, 50));
    }

    public String typeKey() {
        return "mealmastery.milestone.type." + type.name().toLowerCase(Locale.ROOT);
    }
}
