package com.xirc.mealmastery.neoforge;

import com.xirc.mealmastery.Constants;
import com.xirc.mealmastery.MealMasteryServer;
import com.xirc.mealmastery.event.CulinaryHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * NeoForge's half of the loader bridge.
 *
 * <p>NeoForge exposes an event for everything Meal Mastery needs, so this
 * module carries no mixins at all; Fabric's equivalent needs exactly one.</p>
 */
@EventBusSubscriber(modid = Constants.MOD_ID)
public final class NeoForgeEventBridge {

    private NeoForgeEventBridge() {
    }

    /**
     * Farmers buy prepared meals at master level. The offer is built from the
     * dishes this server actually has, so no dish is named in code and an
     * addon's food can turn up here too.
     */
    @SubscribeEvent
    public static void onVillagerTrades(
            net.neoforged.neoforge.event.village.VillagerTradesEvent event) {
        if (!event.getType().equals(net.minecraft.world.entity.npc.VillagerProfession.FARMER)) {
            return;
        }
        event.getTrades().get(5).add(
                com.xirc.mealmastery.compat.VillagerFoodTrades.preparedMealListing());
    }

    @SubscribeEvent
    public static void onServerStarting(ServerAboutToStartEvent event) {
        MealMasteryServer.onServerStarting(event.getServer());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        MealMasteryServer.onServerStopping();
    }

    /**
     * Tags bind at the very end of a datapack reload, which is the earliest
     * point the category system can classify anything. Rebuilding again once
     * the server is up costs nothing and covers any ordering surprise.
     */
    @SubscribeEvent
    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) {
            MealMasteryServer.rebuildCulinaryRegistry();
        }
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MealMasteryServer.rebuildCulinaryRegistry();
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        com.xirc.mealmastery.command.MealMasteryCommands.register(
                event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        CulinaryHooks.onServerTick(event.getServer());
    }

    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CulinaryHooks.onBlockUsed(player, event.getPos(),
                    event.getLevel().getBlockState(event.getPos()));
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!event.getLevel().isClientSide && event.getEntity() instanceof ItemEntity item) {
            CulinaryHooks.onItemEntitySpawned(item);
        }
    }

    @SubscribeEvent
    public static void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CulinaryHooks.onItemEaten(player, event.getItem().copy());
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CulinaryHooks.onPlayerJoin(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerQuit(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            CulinaryHooks.onPlayerQuit(player);
        }
    }
}
