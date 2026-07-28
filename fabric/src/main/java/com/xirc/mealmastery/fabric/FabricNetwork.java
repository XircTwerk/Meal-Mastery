package com.xirc.mealmastery.fabric;

import com.xirc.mealmastery.network.MealMasteryPacket;
import com.xirc.mealmastery.network.Network;
import com.xirc.mealmastery.network.ServerPacketHandler;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric transport: one custom payload channel per packet id.
 */
public final class FabricNetwork implements Network.Transport {

    private FabricNetwork() {
    }

    public static void registerServer() {
        Network.useTransport(new FabricNetwork());
        for (ResourceLocation id : Network.packetIds()) {
            ServerPlayNetworking.registerGlobalReceiver(id,
                    (server, player, handler, buffer, responseSender) -> {
                        // Decode off-thread, act on-thread: packet handlers may
                        // touch profiles and the registry.
                        FriendlyByteBuf copy = PacketByteBufs.copy(buffer);
                        server.execute(() -> {
                            MealMasteryPacket packet = Network.decode(id, copy);
                            if (packet != null) {
                                ServerPacketHandler.handle(player, packet);
                            }
                        });
                    });
        }
        ServerPacketHandler.install();
    }

    @Override
    public void toPlayer(ServerPlayer player, MealMasteryPacket packet) {
        FriendlyByteBuf buffer = PacketByteBufs.create();
        packet.write(buffer);
        ServerPlayNetworking.send(player, packet.id(), buffer);
    }

    @Override
    public void toServer(MealMasteryPacket packet) {
        FabricClientNetwork.sendToServer(packet);
    }
}
