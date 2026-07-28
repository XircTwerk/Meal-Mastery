package com.xirc.mealmastery.recipe;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MealSearchTest {

    private static final MealSearch.Indexed BEEF_STEW = new MealSearch.Indexed(
            "Beef Stew",
            "farmersdelight:beef_stew",
            "Farmer's Delight",
            "farmersdelight",
            List.of("Cooking Pot"),
            "Meal",
            List.of("Raw Beef", "Carrot", "Potato", "Onion"));

    private static final MealSearch.Indexed HOGLIN_LOIN = new MealSearch.Indexed(
            "Hoglin Loin",
            "netherdelight:hoglin_loin",
            "Nether's Delight",
            "netherdelight",
            List.of("Cutting Board"),
            "Snack",
            List.of("Raw Hoglin"));

    @Test
    void bareTermSearchesEveryField() {
        assertTrue(MealSearch.matches(BEEF_STEW, "stew"));
        assertTrue(MealSearch.matches(BEEF_STEW, "carrot"), "matches an ingredient");
        assertTrue(MealSearch.matches(BEEF_STEW, "cooking pot".split(" ")[0]));
        assertTrue(MealSearch.matches(BEEF_STEW, "delight"), "matches the mod name");
        assertFalse(MealSearch.matches(BEEF_STEW, "hoglin"));
    }

    @Test
    void fieldPrefixesNarrowTheSearch() {
        assertTrue(MealSearch.matches(BEEF_STEW, "ingredient:onion"));
        assertFalse(MealSearch.matches(BEEF_STEW, "name:onion"),
                "onion is an ingredient, not part of the dish name");
        assertTrue(MealSearch.matches(HOGLIN_LOIN, "mod:netherdelight"));
        assertTrue(MealSearch.matches(HOGLIN_LOIN, "method:cutting"));
        assertTrue(MealSearch.matches(BEEF_STEW, "category:meal"));
        assertTrue(MealSearch.matches(BEEF_STEW, "id:beef_stew"));
    }

    @Test
    void namespacedIdsAreNotMistakenForFieldSelectors() {
        List<MealSearch.Term> terms = MealSearch.parse("farmersdelight:beef_stew");
        assertEquals(1, terms.size());
        assertEquals(MealSearch.Field.ANY, terms.get(0).field());
        assertEquals("farmersdelight:beef_stew", terms.get(0).text());
        assertTrue(MealSearch.matches(BEEF_STEW, "farmersdelight:beef_stew"));
    }

    @Test
    void everyTermMustMatch() {
        assertTrue(MealSearch.matches(BEEF_STEW, "beef carrot"));
        assertFalse(MealSearch.matches(BEEF_STEW, "beef hoglin"));
    }

    @Test
    void emptyQueryMatchesEverything() {
        assertTrue(MealSearch.matches(BEEF_STEW, ""));
        assertTrue(MealSearch.matches(BEEF_STEW, "   "));
        assertTrue(MealSearch.matches(BEEF_STEW, (String) null));
        assertEquals(List.of(), MealSearch.parse(null));
    }

    @Test
    void searchIsCaseInsensitive() {
        assertTrue(MealSearch.matches(BEEF_STEW, "BEEF"));
        assertTrue(MealSearch.matches(BEEF_STEW, "Ingredient:CARROT"));
    }
}
