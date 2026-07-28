package com.xirc.mealmastery.culinary;

import com.xirc.mealmastery.Constants;
import com.xirc.mealmastery.mastery.MasteryCurve;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Cosmetic titles a player unlocks by playing.
 *
 * <p>Purely journal decoration. Nothing here touches nameplates, chat or the
 * player list unless a server explicitly opts in, because a progression mod
 * silently rewriting how players appear to each other is not a reasonable
 * default.</p>
 *
 * @param requirement what must be reached to unlock it
 */
public record CulinaryTitle(ResourceLocation id, Requirement requirement, long amount) {

    public enum Requirement {
        COOKING_LEVEL,
        RECIPES_MASTERED,
        RECIPES_DISCOVERED,
        PORTIONS_SERVED,
        INGREDIENTS_DISCOVERED
    }

    public String translationKey() {
        return "mealmastery.title." + id.getNamespace() + "." + id.getPath();
    }

    public boolean isUnlocked(CulinaryProfile profile, LevelCurve levelCurve,
                              MasteryCurve masteryCurve) {
        long value = switch (requirement) {
            case COOKING_LEVEL -> profile.cookingLevel(levelCurve);
            case RECIPES_MASTERED -> profile.masteredCount(masteryCurve);
            case RECIPES_DISCOVERED -> profile.discoveredCount();
            case PORTIONS_SERVED -> profile.stats().portionsServed();
            case INGREDIENTS_DISCOVERED -> profile.ingredientsDiscoveredCount();
        };
        return value >= amount;
    }

    private static CulinaryTitle title(String path, Requirement requirement, long amount) {
        return new CulinaryTitle(ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path), requirement, amount);
    }

    public static List<CulinaryTitle> builtIns() {
        return List.of(
                title("home_cook", Requirement.COOKING_LEVEL, 1),
                title("sous_chef", Requirement.COOKING_LEVEL, 10),
                title("chef", Requirement.COOKING_LEVEL, 25),
                title("master_chef", Requirement.RECIPES_MASTERED, 25),
                title("culinary_explorer", Requirement.RECIPES_DISCOVERED, 50),
                title("feastmaster", Requirement.PORTIONS_SERVED, 100),
                title("ingredient_explorer", Requirement.INGREDIENTS_DISCOVERED, 50));
    }

    public static List<CulinaryTitle> unlockedFor(CulinaryProfile profile, LevelCurve levelCurve,
                                                  MasteryCurve masteryCurve) {
        List<CulinaryTitle> unlocked = new ArrayList<>();
        for (CulinaryTitle title : builtIns()) {
            if (title.isUnlocked(profile, levelCurve, masteryCurve)) {
                unlocked.add(title);
            }
        }
        return unlocked;
    }
}
