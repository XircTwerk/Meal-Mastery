package com.xirc.mealmastery.forge;

import com.xirc.mealmastery.MealMasteryServer;
import com.xirc.mealmastery.event.CulinaryHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TagsUpdatedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Forge's half of the loader bridge.
 *
 * <p>Forge exposes an event for everything Meal Mastery needs, so this module
 * carries no mixins at all; Fabric's equivalent needs exactly one.</p>
 */
@Mod.EventBusSubscriber(modid = com.xirc.mealmastery.Constants.MOD_ID)
public final class ForgeEventBridge {

    private ForgeEventBridge() {
    }

    @SubscribeEvent
    public static void onServerStarting(net.minecraftforge.event.server.ServerAboutToStartEvent event) {
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
    public static void onServerStarted(net.minecraftforge.event.server.ServerStartedEvent event) {
        MealMasteryServer.rebuildCulinaryRegistry();
    }

    /**
     * Farmers buy prepared meals at master level. The offer is built from the
     * dishes this server actually has, so no dish is named in code and an
     * addon's food can turn up here too.
     */
    @SubscribeEvent
    public static void onVillagerTrades(
            net.minecraftforge.event.village.VillagerTradesEvent event) {
        if (!event.getType().equals(net.minecraft.world.entity.npc.VillagerProfession.FARMER)) {
            return;
        }
        event.getTrades().get(5).add(
                com.xirc.mealmastery.compat.VillagerFoodTrades.preparedMealListing());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        com.xirc.mealmastery.command.MealMasteryCommands.register(
                event.getDispatcher(), event.getBuildContext());
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            CulinaryHooks.onServerTick(event.getServer());
        }
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
