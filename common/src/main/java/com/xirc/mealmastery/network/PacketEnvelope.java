package com.xirc.mealmastery.network;

import com.xirc.mealmastery.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * The single custom payload every Meal Mastery packet travels inside.
 *
 * <p>1.21 requires each packet to be a registered {@code CustomPacketPayload}
 * with its own codec. Registering one per packet would duplicate the packet
 * list on both loaders, so instead one envelope carries the packet id and its
 * bytes, and the loader-neutral decoder in {@link Network} does the rest.</p>
 *
 * <p>{@code CustomPacketPayload} is vanilla, so unlike the 1.20.1 branch — where
 * Fabric used raw channels and Forge needed a wrapper of its own — both loaders
 * share this one type.</p>
 */
public record PacketEnvelope(ResourceLocation packetId, byte[] payload)
        implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PacketEnvelope> TYPE =
            new CustomPacketPayload.Type<>(
                    ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "envelope"));

    public static final StreamCodec<FriendlyByteBuf, PacketEnvelope> CODEC =
            StreamCodec.of(PacketEnvelope::write, PacketEnvelope::read);

    private static void write(FriendlyByteBuf buffer, PacketEnvelope envelope) {
        buffer.writeResourceLocation(envelope.packetId());
        buffer.writeByteArray(envelope.payload());
    }

    private static PacketEnvelope read(FriendlyByteBuf buffer) {
        return new PacketEnvelope(buffer.readResourceLocation(), buffer.readByteArray());
    }

    public static PacketEnvelope of(MealMasteryPacket packet) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        packet.write(buffer);
        byte[] payload = new byte[buffer.readableBytes()];
        buffer.readBytes(payload);
        return new PacketEnvelope(packet.id(), payload);
    }

    /** @return the decoded packet, or {@code null} for an id this build does not know */
    public MealMasteryPacket unwrap() {
        return Network.decode(packetId,
                new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(payload)));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
