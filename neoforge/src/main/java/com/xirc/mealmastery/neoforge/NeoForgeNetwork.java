package com.xirc.mealmastery.neoforge;

import com.xirc.mealmastery.Constants;
import com.xirc.mealmastery.client.ClientPacketHandler;
import com.xirc.mealmastery.network.MealMasteryPacket;
import com.xirc.mealmastery.network.Network;
import com.xirc.mealmastery.network.PacketEnvelope;
import com.xirc.mealmastery.network.ServerPacketHandler;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * NeoForge transport.
 *
 * <p>One registered payload type carries every packet; see
 * {@link PacketEnvelope}, which is shared with the Fabric module because
 * {@code CustomPacketPayload} is vanilla.</p>
 */
public final class NeoForgeNetwork implements Network.Transport {

    private NeoForgeNetwork() {
    }

    static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(Constants.MOD_ID)
                .optional()
                .playBidirectional(PacketEnvelope.TYPE, PacketEnvelope.CODEC,
                        NeoForgeNetwork::receive);
        Network.useTransport(new NeoForgeNetwork());
        ServerPacketHandler.install();
    }

    private static void receive(PacketEnvelope envelope, IPayloadContext context) {
        // enqueueWork puts this on the correct thread for the receiving side.
        context.enqueueWork(() -> {
            MealMasteryPacket packet = envelope.unwrap();
            if (packet == null) {
                return;
            }
            if (context.player() instanceof ServerPlayer sender) {
                ServerPacketHandler.handle(sender, packet);
            } else if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientPacketHandler.handle(packet);
            }
        });
    }

    @Override
    public void toPlayer(ServerPlayer player, MealMasteryPacket packet) {
        PacketDistributor.sendToPlayer(player, PacketEnvelope.of(packet));
    }

    @Override
    public void toServer(MealMasteryPacket packet) {
        PacketDistributor.sendToServer(PacketEnvelope.of(packet));
    }
}
