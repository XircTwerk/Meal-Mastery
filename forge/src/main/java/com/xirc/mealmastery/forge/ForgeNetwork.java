package com.xirc.mealmastery.forge;

import com.xirc.mealmastery.Constants;
import com.xirc.mealmastery.client.ClientPacketHandler;
import com.xirc.mealmastery.network.MealMasteryPacket;
import com.xirc.mealmastery.network.Network;
import com.xirc.mealmastery.network.ServerPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.util.function.Supplier;

/**
 * Forge transport.
 *
 * <p>Forge's {@code SimpleChannel} wants a registered class per message, which
 * would duplicate the packet list. Instead one envelope carries the packet id
 * and its bytes, and the loader-neutral decoder in {@link Network} does the
 * rest — the same payloads travel over both loaders with no per-loader
 * registration to keep in sync.</p>
 */
public final class ForgeNetwork implements Network.Transport {
    private static final String PROTOCOL = "1";

    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Constants.MOD_ID, "main"),
            () -> PROTOCOL,
            PROTOCOL::equals,
            PROTOCOL::equals);

    private ForgeNetwork() {
    }

    /** An id plus the payload bytes, so one registration covers every packet. */
    private record Envelope(ResourceLocation id, byte[] payload) {

        static void encode(Envelope envelope, FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(envelope.id());
            buffer.writeByteArray(envelope.payload());
        }

        static Envelope decode(FriendlyByteBuf buffer) {
            return new Envelope(buffer.readResourceLocation(), buffer.readByteArray());
        }
    }

    public static void register() {
        CHANNEL.registerMessage(0, Envelope.class, Envelope::encode, Envelope::decode,
                ForgeNetwork::receive);
        Network.useTransport(new ForgeNetwork());
        ServerPacketHandler.install();
    }

    private static void receive(Envelope envelope, Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            FriendlyByteBuf buffer = new FriendlyByteBuf(
                    io.netty.buffer.Unpooled.wrappedBuffer(envelope.payload()));
            MealMasteryPacket packet = Network.decode(envelope.id(), buffer);
            if (packet == null) {
                return;
            }
            if (context.getDirection() == NetworkDirection.PLAY_TO_SERVER) {
                ServerPlayer sender = context.getSender();
                if (sender != null) {
                    ServerPacketHandler.handle(sender, packet);
                }
            } else if (FMLEnvironment.dist == Dist.CLIENT) {
                ClientPacketHandler.handle(packet);
            }
        });
        context.setPacketHandled(true);
    }

    private static Envelope wrap(MealMasteryPacket packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        packet.write(buffer);
        byte[] payload = new byte[buffer.readableBytes()];
        buffer.readBytes(payload);
        return new Envelope(packet.id(), payload);
    }

    @Override
    public void toPlayer(ServerPlayer player, MealMasteryPacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), wrap(packet));
    }

    @Override
    public void toServer(MealMasteryPacket packet) {
        CHANNEL.sendToServer(wrap(packet));
    }
}
