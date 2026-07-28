package com.xirc.mealmastery.culinary;

import com.xirc.mealmastery.Constants;
import com.xirc.mealmastery.data.ProfileMigrations;
import com.xirc.mealmastery.mastery.MasteryCurve;
import com.xirc.mealmastery.mastery.MasteryRank;
import com.xirc.mealmastery.recipe.CookingMethod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CulinaryProfileTest {
    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-0000000000aa");
    private static final ResourceLocation ROAST_CHICKEN =
            new ResourceLocation("farmersdelight", "roast_chicken");
    private static final ResourceLocation BEEF_STEW =
            new ResourceLocation("farmersdelight", "beef_stew");
    private static final ResourceLocation ONION =
            new ResourceLocation("farmersdelight", "onion");

    private static CulinaryProfile populated() {
        CulinaryProfile profile = new CulinaryProfile(PLAYER);
        profile.addCookingXp(1_500L, LevelCurve.DEFAULT);

        MealRecord chicken = profile.meal(ROAST_CHICKEN);
        chicken.discover(47L);
        chicken.recordPreparation(47L, 3, CookingMethod.COOKING_POT);
        chicken.recordPreparation(48L, 80, CookingMethod.COOKING_POT);
        chicken.recordEaten(48L, 27);
        chicken.recordServed(14L);
        chicken.addMasteryPoints(83L);
        chicken.setFavorite(true);
        chicken.recordIngredientVariant(ONION);

        profile.ingredient(ONION).recordUse(47L, 2, ROAST_CHICKEN, CookingMethod.COOKING_POT);
        profile.method(CookingMethod.COOKING_POT).recordPreparation(47L, 83, ROAST_CHICKEN);
        profile.stats().addMealsPrepared(83L);
        profile.stats().addMealsEaten(27L);
        profile.records().offerMealsInOneDay(12, 48L);
        profile.streaks().recordCookingDay(48L, 1);
        profile.pushActivity(new ActivityEntry(
                ActivityEntry.Type.RECIPE_DISCOVERED, ROAST_CHICKEN, 75L, 47L));
        profile.togglePin(ROAST_CHICKEN, 3);
        return profile;
    }

    @Test
    void roundTripsEverySection() {
        CulinaryProfile saved = populated();
        CulinaryProfile loaded = CulinaryProfile.load(PLAYER, saved.save());

        assertEquals(1_500L, loaded.cookingXp());

        MealRecord chicken = loaded.peekMeal(ROAST_CHICKEN);
        assertTrue(chicken.isDiscovered());
        assertEquals(47L, chicken.discoveredDay());
        assertEquals(83L, chicken.prepared());
        assertEquals(27L, chicken.eaten());
        assertEquals(14L, chicken.served());
        assertEquals(83L, chicken.masteryPoints());
        assertTrue(chicken.isFavorite());
        assertEquals(47L, chicken.firstPreparedDay());
        assertEquals(48L, chicken.lastPreparedDay());
        assertEquals(80, chicken.bestPreparedInOneDay());
        assertEquals(Set.of(ONION), chicken.ingredientVariantsUsed());
        assertEquals(83L, chicken.preparationsByMethod().get(CookingMethod.COOKING_POT));

        assertEquals(2L, loaded.peekIngredient(ONION).timesUsed());
        assertEquals(83L, loaded.peekMethod(CookingMethod.COOKING_POT).preparations());
        assertEquals(83L, loaded.stats().mealsPrepared());
        assertEquals(12, loaded.records().mostMealsInOneDay());
        assertEquals(1, loaded.streaks().cookingStreak());
        assertEquals(List.of(ROAST_CHICKEN), loaded.pinned());
        assertEquals(1, loaded.activity().size());
        assertEquals(ActivityEntry.Type.RECIPE_DISCOVERED, loaded.activity().get(0).type());
    }

    @Test
    void emptyRecordsAreNotWritten() {
        CulinaryProfile profile = new CulinaryProfile(PLAYER);
        profile.meal(ROAST_CHICKEN);
        profile.meal(BEEF_STEW);
        CompoundTag saved = profile.save();
        assertTrue(saved.getCompound("meals").isEmpty(),
                "untouched records must not bloat the profile");
    }

    @Test
    void unknownSectionsSurviveARoundTrip() {
        // Never silently erase data written by a newer build.
        CompoundTag tag = populated().save();
        tag.put("someFutureSection", StringTag.valueOf("keep me"));

        CompoundTag rewritten = CulinaryProfile.load(PLAYER, tag).save();
        assertEquals("keep me", rewritten.getString("someFutureSection"));
    }

    @Test
    void migrationStampsUnversionedProfiles() {
        CompoundTag legacy = populated().save();
        legacy.remove("schemaVersion");

        CompoundTag migrated = ProfileMigrations.migrate(legacy);
        assertEquals(Constants.PROFILE_SCHEMA_VERSION, migrated.getInt("schemaVersion"));
        assertEquals(83L, CulinaryProfile.load(PLAYER, migrated).peekMeal(ROAST_CHICKEN).prepared());
    }

    @Test
    void migrationLeavesNewerProfilesAlone() {
        CompoundTag future = populated().save();
        future.putInt("schemaVersion", Constants.PROFILE_SCHEMA_VERSION + 5);

        CompoundTag result = ProfileMigrations.migrate(future);
        assertEquals(Constants.PROFILE_SCHEMA_VERSION + 5, result.getInt("schemaVersion"));
    }

    @Test
    void orphanedRecordsArePreservedUntilExplicitlyPurged() {
        // Removing an addon for a session must not delete its progress.
        CulinaryProfile profile = populated();
        Set<ResourceLocation> stillInstalled = Set.of(BEEF_STEW);

        assertEquals(List.of(ROAST_CHICKEN), profile.orphanedTargets(stillInstalled));
        assertEquals(83L, profile.peekMeal(ROAST_CHICKEN).prepared(),
                "orphaned progress stays intact");

        assertEquals(1, profile.purgeOrphans(stillInstalled));
        assertEquals(null, profile.peekMeal(ROAST_CHICKEN));
        assertTrue(profile.pinned().isEmpty(), "purging also drops the dangling pin");
    }

    @Test
    void resettingStatisticsKeepsMasteryAndDiscovery() {
        CulinaryProfile profile = populated();
        profile.reset(CulinaryProfile.ResetScope.STATISTICS);

        assertEquals(0L, profile.stats().mealsPrepared());
        assertEquals(0, profile.records().mostMealsInOneDay());
        assertTrue(profile.peekMeal(ROAST_CHICKEN).isDiscovered());
        assertEquals(83L, profile.peekMeal(ROAST_CHICKEN).masteryPoints());
    }

    @Test
    void resettingMasteryKeepsPreparationCounts() {
        CulinaryProfile profile = populated();
        profile.reset(CulinaryProfile.ResetScope.MASTERY);

        MealRecord chicken = profile.peekMeal(ROAST_CHICKEN);
        assertEquals(0L, chicken.masteryPoints());
        assertEquals(83L, chicken.prepared());
        assertTrue(chicken.isDiscovered());
    }

    @Test
    void derivedCountsAgreeWithTheRecords() {
        CulinaryProfile profile = populated();
        profile.meal(BEEF_STEW).discover(50L);
        profile.meal(BEEF_STEW).recordPreparation(50L, 200, CookingMethod.COOKING_POT);
        profile.meal(BEEF_STEW).addMasteryPoints(200L);
        // Roast chicken sits at 83 points, which is Expert on the default curve;
        // only the stew has gone the full 150.

        assertEquals(2, profile.discoveredCount());
        assertEquals(2, profile.uniquePreparedCount());
        assertEquals(1, profile.masteredCount(MasteryCurve.DEFAULT));
        assertEquals(MasteryRank.MASTERED, profile.rankOf(BEEF_STEW, MasteryCurve.DEFAULT));
        assertEquals(MasteryRank.EXPERT, profile.rankOf(ROAST_CHICKEN, MasteryCurve.DEFAULT));
    }

    @Test
    void pinningRespectsTheConfiguredLimit() {
        CulinaryProfile profile = new CulinaryProfile(PLAYER);
        profile.togglePin(ROAST_CHICKEN, 2);
        profile.togglePin(BEEF_STEW, 2);
        profile.togglePin(ONION, 2);

        assertEquals(List.of(BEEF_STEW, ONION), profile.pinned());
        assertFalse(profile.peekMeal(ROAST_CHICKEN).isPinned());
    }

    @Test
    void activityFeedIsBounded() {
        CulinaryProfile profile = new CulinaryProfile(PLAYER);
        for (int i = 0; i < CulinaryProfile.MAX_ACTIVITY_ENTRIES * 2; i++) {
            profile.pushActivity(new ActivityEntry(
                    ActivityEntry.Type.MEAL_PREPARED, ROAST_CHICKEN, i, i));
        }
        assertEquals(CulinaryProfile.MAX_ACTIVITY_ENTRIES, profile.activity().size());
        assertEquals(CulinaryProfile.MAX_ACTIVITY_ENTRIES * 2 - 1,
                profile.activity().get(0).detail(), "newest first");
    }
}
