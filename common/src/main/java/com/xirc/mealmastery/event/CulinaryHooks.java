package com.xirc.mealmastery.event;

import com.xirc.mealmastery.tracking.CookingTracker;
import com.xirc.mealmastery.tracking.EatingTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Single entry point every loader-specific hook funnels into.
 *
 * <p>Forge reaches this through its event bus, Fabric through Fabric API
 * callbacks plus one narrow mixin. Keeping the fan-in here means the tracking
 * services never learn which loader they are running on.</p>
 */
public final class CulinaryHooks {
    private CulinaryHooks() {
    }

    /**
     * Fired server-side once a player has finished consuming an item.
     *
     * @param player the eater; always a server player
     * @param stack  a copy of the stack as it was before consumption
     */
    public static void onItemEaten(ServerPlayer player, ItemStack stack) {
        EatingTracker.onItemEaten(player, stack);
    }

    /** Fired when a player right-clicks a block, before the block reacts. */
    public static void onBlockUsed(ServerPlayer player, BlockPos pos, BlockState state) {
        CookingTracker.onWorkstationUsed(player, pos, state);
    }

    /** Fired when an item entity is added to a server level. */
    public static void onItemEntitySpawned(ItemEntity entity) {
        CookingTracker.onItemEntitySpawned(entity);
    }

    /** Fired at the end of every server tick. */
    public static void onServerTick(MinecraftServer server) {
        CookingTracker.onServerTick(server);
        com.xirc.mealmastery.challenge.DailyRotation.tick(server);
        com.xirc.mealmastery.culinary.ProfileManager manager =
                com.xirc.mealmastery.culinary.ProfileManager.get();
        if (manager != null) {
            manager.tickAutosave();
        }
    }

    public static void onPlayerJoin(ServerPlayer player) {
        com.xirc.mealmastery.culinary.ProfileManager manager =
                com.xirc.mealmastery.culinary.ProfileManager.get();
        if (manager != null) {
            manager.onJoin(player);
        }
        // Push the catalogue, profile and rules straight away. Without this the
        // client has nothing to draw until something happens to ask for it, so
        // the journal and the stats panel stayed blank until the player ran a
        // command.
        com.xirc.mealmastery.network.ServerPacketHandler.sendEverything(player);
    }

    public static void onPlayerQuit(ServerPlayer player) {
        CookingTracker.forget(player);
        com.xirc.mealmastery.culinary.ProfileManager manager =
                com.xirc.mealmastery.culinary.ProfileManager.get();
        if (manager != null) {
            manager.onQuit(player);
        }
    }
}
