package com.xirc.mealmastery.fabric;

import com.xirc.mealmastery.MealMasteryServer;
import com.xirc.mealmastery.event.CulinaryHooks;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;

/**
 * Fabric's half of the loader bridge.
 *
 * <p>Everything here is a Fabric API callback. The only thing Fabric API 1.20.1
 * has no equivalent for is "a living entity finished eating", which is what the
 * single mixin in this module covers.</p>
 */
public final class FabricEventBridge {

    private FabricEventBridge() {
    }

    public static void register() {
        // Farmers buy prepared meals at master level. The offer is built from
        // the dishes this server actually has, so no dish is named in code.
        net.fabricmc.fabric.api.object.builder.v1.trade.TradeOfferHelper
                .registerVillagerOffers(net.minecraft.world.entity.npc.VillagerProfession.FARMER, 5,
                        factories -> factories.add(
                                com.xirc.mealmastery.compat.VillagerFoodTrades
                                        .preparedMealListing()));

        ServerLifecycleEvents.SERVER_STARTING.register(MealMasteryServer::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> MealMasteryServer.onServerStopping());

        // The registry is rebuilt once the server is up and again after every
        // datapack reload. SERVER_STARTED is used rather than TAGS_LOADED
        // because the latter is not raised on a dedicated server in this Fabric
        // API version, and building before tags bind would classify every dish
        // as uncategorised.
        ServerLifecycleEvents.SERVER_STARTED.register(server ->
                MealMasteryServer.rebuildCulinaryRegistry());
        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resources, success) -> {
            if (success) {
                MealMasteryServer.rebuildCulinaryRegistry();
            }
        });

        ServerTickEvents.END_SERVER_TICK.register(CulinaryHooks::onServerTick);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                com.xirc.mealmastery.command.MealMasteryCommands.register(dispatcher, registryAccess));

        UseBlockCallback.EVENT.register((player, level, hand, hit) -> {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                CulinaryHooks.onBlockUsed(serverPlayer, hit.getBlockPos(),
                        level.getBlockState(hit.getBlockPos()));
            }
            // Purely observational: never consume the interaction.
            return InteractionResult.PASS;
        });

        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof ItemEntity item) {
                CulinaryHooks.onItemEntitySpawned(item);
            }
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                CulinaryHooks.onPlayerJoin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                CulinaryHooks.onPlayerQuit(handler.getPlayer()));
    }
}
