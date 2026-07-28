package com.xirc.mealmastery.fabric;

import com.xirc.mealmastery.client.ClientPacketHandler;
import com.xirc.mealmastery.network.MealMasteryPacket;
import com.xirc.mealmastery.network.PacketEnvelope;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client half of the Fabric transport. Kept separate so the dedicated server
 * never loads a client-only Fabric API class.
 */
@Environment(EnvType.CLIENT)
public final class FabricClientNetwork {

    private FabricClientNetwork() {
    }

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(PacketEnvelope.TYPE, (envelope, context) -> {
            MealMasteryPacket packet = envelope.unwrap();
            if (packet != null) {
                ClientPacketHandler.handle(packet);
            }
        });
    }

    static void sendToServer(MealMasteryPacket packet) {
        // A vanilla server, or one without Meal Mastery, will not accept this.
        if (ClientPlayNetworking.canSend(PacketEnvelope.TYPE)) {
            ClientPlayNetworking.send(PacketEnvelope.of(packet));
        }
    }
}
