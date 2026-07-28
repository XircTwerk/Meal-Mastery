package com.xirc.mealmastery.fabric;

import com.xirc.mealmastery.network.MealMasteryPacket;
import com.xirc.mealmastery.network.Network;
import com.xirc.mealmastery.network.PacketEnvelope;
import com.xirc.mealmastery.network.ServerPacketHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/**
 * Fabric transport.
 *
 * <p>One registered payload type carries every packet; see
 * {@link PacketEnvelope}.</p>
 */
public final class FabricNetwork implements Network.Transport {

    private FabricNetwork() {
    }

    public static void registerServer() {
        // Both directions have to be registered before either side can send.
        PayloadTypeRegistry.playS2C().register(PacketEnvelope.TYPE, PacketEnvelope.CODEC);
        PayloadTypeRegistry.playC2S().register(PacketEnvelope.TYPE, PacketEnvelope.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(PacketEnvelope.TYPE, (envelope, context) -> {
            // The handler already runs on the server thread in 1.21.
            MealMasteryPacket packet = envelope.unwrap();
            if (packet != null) {
                ServerPacketHandler.handle(context.player(), packet);
            }
        });
        Network.useTransport(new FabricNetwork());
        ServerPacketHandler.install();
    }

    @Override
    public void toPlayer(ServerPlayer player, MealMasteryPacket packet) {
        ServerPlayNetworking.send(player, PacketEnvelope.of(packet));
    }

    @Override
    public void toServer(MealMasteryPacket packet) {
        FabricClientNetwork.sendToServer(packet);
    }
}
