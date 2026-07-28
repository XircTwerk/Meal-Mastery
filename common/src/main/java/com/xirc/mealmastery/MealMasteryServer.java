package com.xirc.mealmastery;

import com.xirc.mealmastery.challenge.ChallengeManager;
import com.xirc.mealmastery.challenge.ChallengeRegistry;
import com.xirc.mealmastery.collection.CollectionRegistry;
import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.culinary.ProfileManager;
import com.xirc.mealmastery.data.DatapackLoader;
import com.xirc.mealmastery.milestone.MilestoneTracker;
import com.xirc.mealmastery.platform.Services;
import com.xirc.mealmastery.recipe.CulinaryRegistries;
import com.xirc.mealmastery.recipe.FoodCategory;
import com.xirc.mealmastery.tracking.CookingTracker;
import com.xirc.mealmastery.tracking.WorkstationRegistry;
import net.minecraft.server.MinecraftServer;

/**
 * Server lifecycle, shared by both loaders.
 *
 * <p>The culinary registry is rebuilt from the <em>tags loaded</em> signal
 * rather than from a resource-reload listener. Recipes are available earlier,
 * but item tags are only bound once the whole reload finishes, and categories
 * are derived entirely from tags — building sooner would silently classify
 * every dish as uncategorised.</p>
 */
public final class MealMasteryServer {
    private static volatile MinecraftServer server;

    private MealMasteryServer() {
    }

    public static MinecraftServer server() {
        return server;
    }

    public static void onServerStarting(MinecraftServer startingServer) {
        server = startingServer;
        ConfigManager.initialize(Services.PLATFORM.configDirectory());
        WorkstationRegistry.applyConfiguration(
                ConfigManager.server().compatibility.extraWorkstationBlocks);
        ProfileManager.start(startingServer);
    }

    public static void onServerStopping() {
        ProfileManager.stop();
        CulinaryRegistries.clear();
        ChallengeRegistry.clear();
        com.xirc.mealmastery.challenge.DailyRotation.reset();
        CollectionRegistry.clear();
        CookingTracker.reset();
        server = null;
    }

    /**
     * Rebuilds the culinary registry and everything derived from it. Safe to
     * call repeatedly; a datapack reload mid-session goes through the same path
     * as the initial load.
     */
    public static void rebuildCulinaryRegistry() {
        MinecraftServer current = server;
        if (current == null) {
            return;
        }
        CulinaryRegistries.rebuild(
                current.getRecipeManager().getRecipes(),
                current.registryAccess(),
                ConfigManager.toEligibilityRules(ConfigManager.server()),
                FoodCategory.builtInDishCategories());

        DatapackLoader.Result data = DatapackLoader.load(current.getResourceManager());
        if (!data.problems().isEmpty()) {
            MealMasteryLog.LOGGER.warn("{} Meal Mastery data file(s) were skipped; see above",
                    data.problems().size());
        }
        ChallengeRegistry.replace(data.challenges());
        MilestoneTracker.replace(data.milestones());
        CollectionRegistry.rebuild(data.collections(), CulinaryRegistries.current());

        ChallengeManager.install();
        MilestoneTracker.install();
        com.xirc.mealmastery.progression.CookingBonuses.install();

        // A reload can add or remove dishes, so every connected client needs the
        // new catalogue rather than the one it cached at join.
        for (net.minecraft.server.level.ServerPlayer player
                : current.getPlayerList().getPlayers()) {
            com.xirc.mealmastery.network.ServerPacketHandler.sendEverything(player);
        }
    }

    /** Re-reads configuration and rebuilds everything derived from it. */
    public static void reload() {
        ConfigManager.reload();
        WorkstationRegistry.applyConfiguration(
                ConfigManager.server().compatibility.extraWorkstationBlocks);
        rebuildCulinaryRegistry();
    }
}
