package com.xirc.mealmastery.recipe;

import com.xirc.mealmastery.MinecraftBootstrap;
import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.config.ServerConfig;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EligibilityRulesTest {
    private static final ResourceLocation RECIPE =
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "cooking/beef_stew");
    private static final ResourceLocation COOKING_TYPE =
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "cooking");
    private static final ResourceLocation CUTTING_TYPE =
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "cutting");
    private static final ResourceLocation BEEF_STEW =
            ResourceLocation.fromNamespaceAndPath("farmersdelight", "beef_stew");
    private static final ResourceLocation STRIPPED_LOG =
            ResourceLocation.fromNamespaceAndPath("minecraft", "stripped_acacia_log");
    private static final ResourceLocation ADDON_DISH =
            ResourceLocation.fromNamespaceAndPath("netherdelight", "hoglin_loin");
    private static final ResourceLocation BREAD =
            ResourceLocation.fromNamespaceAndPath("minecraft", "bread");
    private static final ResourceLocation BREAD_RECIPE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "bread");
    private static final ResourceLocation CRAFTING_TYPE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "crafting");

    private static final Predicate<TagKey<Item>> NO_TAGS = tag -> false;

    @BeforeAll
    static void bootstrap() {
        // TagKey.create touches the registry keys, which need the game bootstrapped.
        MinecraftBootstrap.ensure();
    }

    private static Predicate<TagKey<Item>> tagged(TagKey<Item>... tags) {
        Set<TagKey<Item>> set = Set.of(tags);
        return set::contains;
    }

    @Test
    void edibleOutputIsTheOnlyRuleNeededForAnUnknownAddon() {
        // The point of the whole design: nothing knows what "netherdelight" is.
        assertEquals(EligibilityRules.Verdict.ELIGIBLE,
                EligibilityRules.DEFAULT.evaluate(RECIPE, COOKING_TYPE, ADDON_DISH, true, NO_TAGS));
    }

    @Test
    void vanillaFoodIsTrackedByDefault() {
        assertEquals(EligibilityRules.Verdict.ELIGIBLE,
                EligibilityRules.DEFAULT.evaluate(BREAD_RECIPE, CRAFTING_TYPE, BREAD, true, NO_TAGS));
    }

    @Test
    void turningOffVanillaTrackingExcludesOnlyVanilla() {
        ServerConfig config = new ServerConfig();
        config.compatibility.trackVanillaRecipes = false;
        EligibilityRules rules = ConfigManager.toEligibilityRules(config);

        assertEquals(EligibilityRules.Verdict.EXCLUDED,
                rules.evaluate(BREAD_RECIPE, CRAFTING_TYPE, BREAD, true, NO_TAGS));
        // An addon dish is not collateral damage.
        assertEquals(EligibilityRules.Verdict.ELIGIBLE,
                rules.evaluate(RECIPE, COOKING_TYPE, ADDON_DISH, true, NO_TAGS));
        assertEquals(EligibilityRules.Verdict.ELIGIBLE,
                rules.evaluate(RECIPE, COOKING_TYPE, BEEF_STEW, true, NO_TAGS));
    }

    @Test
    void nonFoodOutputIsIgnoredRatherThanExcluded() {
        // Most farmersdelight:cutting recipes strip logs; they are not dishes,
        // but they are also not something an admin misconfigured.
        assertEquals(EligibilityRules.Verdict.NOT_FOOD,
                EligibilityRules.DEFAULT.evaluate(RECIPE, CUTTING_TYPE, STRIPPED_LOG, false, NO_TAGS));
    }

    @Test
    void missingOutputIsIgnored() {
        assertEquals(EligibilityRules.Verdict.NOT_FOOD,
                EligibilityRules.DEFAULT.evaluate(RECIPE, COOKING_TYPE, null, false, NO_TAGS));
    }

    @Test
    void denyListsWinOverEverything() {
        EligibilityRules rules = EligibilityRules.builder().denyRecipe(RECIPE).build();
        assertEquals(EligibilityRules.Verdict.EXCLUDED,
                rules.evaluate(RECIPE, COOKING_TYPE, BEEF_STEW, true, NO_TAGS));

        EligibilityRules byItem = EligibilityRules.builder().denyItem(BEEF_STEW).build();
        assertEquals(EligibilityRules.Verdict.EXCLUDED,
                byItem.evaluate(RECIPE, COOKING_TYPE, BEEF_STEW, true, NO_TAGS));

        EligibilityRules byMod = EligibilityRules.builder().denyMod("netherdelight").build();
        assertEquals(EligibilityRules.Verdict.EXCLUDED,
                byMod.evaluate(RECIPE, COOKING_TYPE, ADDON_DISH, true, NO_TAGS));

        EligibilityRules byType = EligibilityRules.builder().denyRecipeType(CUTTING_TYPE).build();
        assertEquals(EligibilityRules.Verdict.EXCLUDED,
                byType.evaluate(RECIPE, CUTTING_TYPE, BEEF_STEW, true, NO_TAGS));
    }

    @Test
    void explicitAllowOverridesTheEdibleRequirement() {
        // How a pack registers a dish that has no FoodProperties, e.g. a drink
        // implemented as a custom use action.
        EligibilityRules rules = EligibilityRules.builder().allowRecipe(RECIPE).build();
        assertEquals(EligibilityRules.Verdict.ELIGIBLE,
                rules.evaluate(RECIPE, COOKING_TYPE, STRIPPED_LOG, false, NO_TAGS));
    }

    @Test
    void allowedModsNarrowTheJournalToACuratedSet() {
        EligibilityRules rules = EligibilityRules.builder().allowMod("farmersdelight").build();
        assertEquals(EligibilityRules.Verdict.ELIGIBLE,
                rules.evaluate(RECIPE, COOKING_TYPE, BEEF_STEW, true, NO_TAGS));
        assertEquals(EligibilityRules.Verdict.EXCLUDED,
                rules.evaluate(RECIPE, COOKING_TYPE, ADDON_DISH, true, NO_TAGS));
    }

    @Test
    void requiredTagsGateTheJournalWithoutTouchingRecipes() {
        EligibilityRules rules = EligibilityRules.builder()
                .requireTag(CulinaryTags.MEALS)
                .build();
        assertEquals(EligibilityRules.Verdict.ELIGIBLE,
                rules.evaluate(RECIPE, COOKING_TYPE, BEEF_STEW, true, tagged(CulinaryTags.MEALS)));
        assertEquals(EligibilityRules.Verdict.EXCLUDED,
                rules.evaluate(RECIPE, COOKING_TYPE, ADDON_DISH, true, NO_TAGS));
    }

    @Test
    void deniedTagsExcludeEvenEdibleOutput() {
        EligibilityRules rules = EligibilityRules.builder()
                .denyTag(CulinaryTags.DRINKS)
                .build();
        assertEquals(EligibilityRules.Verdict.EXCLUDED,
                rules.evaluate(RECIPE, COOKING_TYPE, BEEF_STEW, true, tagged(CulinaryTags.DRINKS)));
    }

    @Test
    void allowedItemBypassesAModAllowList() {
        EligibilityRules rules = EligibilityRules.builder()
                .allowMod("farmersdelight")
                .allowItem(ADDON_DISH)
                .build();
        assertEquals(EligibilityRules.Verdict.ELIGIBLE,
                rules.evaluate(RECIPE, COOKING_TYPE, ADDON_DISH, true, NO_TAGS));
    }

    @Test
    void edibleRequirementCanBeRelaxedWholesale() {
        EligibilityRules rules = EligibilityRules.builder().requireEdibleOutput(false).build();
        assertEquals(EligibilityRules.Verdict.ELIGIBLE,
                rules.evaluate(RECIPE, CUTTING_TYPE, STRIPPED_LOG, false, NO_TAGS));
    }
}
