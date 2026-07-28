package com.xirc.mealmastery.network;

import com.xirc.mealmastery.MealMasteryLog;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * The loader-neutral half of networking: what a packet id decodes to, and how
 * to hand a decoded packet to whoever handles it.
 *
 * <p>Transport lives in the loader modules. Everything else — payloads,
 * decoding, dispatch and the server-side handlers — is here.</p>
 */
public final class Network {

    /** Set by the loader module at startup. */
    private static volatile Transport transport = Transport.NOOP;

    private static final Map<ResourceLocation, Function<FriendlyByteBuf, MealMasteryPacket>> DECODERS =
            new LinkedHashMap<>();

    static {
        DECODERS.put(Packets.SyncCatalogue.ID, Packets.SyncCatalogue::read);
        DECODERS.put(Packets.SyncProfile.ID, Packets.SyncProfile::read);
        DECODERS.put(Packets.SyncRules.ID, Packets.SyncRules::read);
        DECODERS.put(Packets.SyncDishDetail.ID, Packets.SyncDishDetail::read);
        DECODERS.put(Packets.SyncJournalExtras.ID, Packets.SyncJournalExtras::read);
        DECODERS.put(Packets.MenuContext.ID, Packets.MenuContext::read);
        DECODERS.put(Packets.Notify.ID, Packets.Notify::read);
        DECODERS.put(Packets.OpenJournal.ID, Packets.OpenJournal::read);
        DECODERS.put(Packets.RequestDishDetail.ID, Packets.RequestDishDetail::read);
        DECODERS.put(Packets.RequestProfile.ID, Packets.RequestProfile::read);
        DECODERS.put(Packets.SetPreference.ID, Packets.SetPreference::read);
    }

    private Network() {
    }

    /** Implemented once per loader. */
    public interface Transport {
        Transport NOOP = new Transport() {
            @Override
            public void toPlayer(ServerPlayer player, MealMasteryPacket packet) {
            }

            @Override
            public void toServer(MealMasteryPacket packet) {
            }
        };

        void toPlayer(ServerPlayer player, MealMasteryPacket packet);

        void toServer(MealMasteryPacket packet);
    }

    public static void useTransport(Transport implementation) {
        transport = implementation == null ? Transport.NOOP : implementation;
    }

    public static void toPlayer(ServerPlayer player, MealMasteryPacket packet) {
        try {
            transport.toPlayer(player, packet);
        } catch (RuntimeException failure) {
            MealMasteryLog.LOGGER.error("Failed to send {} to {}", packet.id(),
                    player.getGameProfile().getName(), failure);
        }
    }

    public static void toServer(MealMasteryPacket packet) {
        try {
            transport.toServer(packet);
        } catch (RuntimeException failure) {
            MealMasteryLog.LOGGER.error("Failed to send {} to the server", packet.id(), failure);
        }
    }

    public static Iterable<ResourceLocation> packetIds() {
        return DECODERS.keySet();
    }

    /**
     * @return the decoded packet, or {@code null} for an unknown id — which is
     *         normal when a client and server run different builds and must not
     *         be treated as an error
     */
    public static MealMasteryPacket decode(ResourceLocation id, FriendlyByteBuf buffer) {
        Function<FriendlyByteBuf, MealMasteryPacket> decoder = DECODERS.get(id);
        if (decoder == null) {
            return null;
        }
        try {
            return decoder.apply(buffer);
        } catch (RuntimeException failure) {
            MealMasteryLog.LOGGER.warn("Discarding malformed {} packet", id, failure);
            return null;
        }
    }
}
