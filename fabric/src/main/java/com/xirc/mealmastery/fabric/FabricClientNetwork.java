package com.xirc.mealmastery.fabric;

import com.xirc.mealmastery.client.ClientPacketHandler;
import com.xirc.mealmastery.network.MealMasteryPacket;
import com.xirc.mealmastery.network.Network;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * Client half of the Fabric transport. Kept separate so the dedicated server
 * never loads a client-only Fabric API class.
 */
@Environment(EnvType.CLIENT)
public final class FabricClientNetwork {

    private FabricClientNetwork() {
    }

    public static void register() {
        for (ResourceLocation id : Network.packetIds()) {
            ClientPlayNetworking.registerGlobalReceiver(id,
                    (client, handler, buffer, responseSender) -> {
                        FriendlyByteBuf copy = PacketByteBufs.copy(buffer);
                        client.execute(() -> {
                            MealMasteryPacket packet = Network.decode(id, copy);
                            if (packet != null) {
                                ClientPacketHandler.handle(packet);
                            }
                        });
                    });
        }
    }

    static void sendToServer(MealMasteryPacket packet) {
        if (!ClientPlayNetworking.canSend(packet.id())) {
            return;
        }
        FriendlyByteBuf buffer = PacketByteBufs.create();
        packet.write(buffer);
        ClientPlayNetworking.send(packet.id(), buffer);
    }
}
