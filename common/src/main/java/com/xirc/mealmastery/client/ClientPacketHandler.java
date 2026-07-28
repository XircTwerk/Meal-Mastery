package com.xirc.mealmastery.client;

import com.xirc.mealmastery.network.MealMasteryPacket;
import com.xirc.mealmastery.network.Packets;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Routes incoming server packets into {@link ClientJournalState}.
 *
 * <p>Deliberately free of any Minecraft client class so the dedicated server
 * can load it without a dist check. Anything that needs to draw reads the
 * state instead.</p>
 */
public final class ClientPacketHandler {

    /** Set when the server asks the journal to open; the screen layer consumes it. */
    private static final AtomicBoolean OPEN_REQUESTED = new AtomicBoolean();

    private ClientPacketHandler() {
    }

    public static void handle(MealMasteryPacket packet) {
        if (packet instanceof Packets.SyncCatalogue catalogue) {
            ClientJournalState.acceptCatalogue(catalogue);
        } else if (packet instanceof Packets.SyncProfile profile) {
            ClientJournalState.acceptProfile(profile);
        } else if (packet instanceof Packets.SyncRules rules) {
            ClientJournalState.acceptRules(rules);
        } else if (packet instanceof Packets.SyncJournalExtras extras) {
            ClientJournalState.acceptExtras(extras);
        } else if (packet instanceof Packets.SyncDishDetail detail) {
            ClientJournalState.acceptDetail(detail);
        } else if (packet instanceof Packets.MenuContext context) {
            ClientJournalState.acceptMenuContext(context);
        } else if (packet instanceof Packets.Notify notify) {
            ClientJournalState.queueNotification(notify);
        } else if (packet instanceof Packets.OpenJournal) {
            OPEN_REQUESTED.set(true);
        }
    }

    /** @return {@code true} once per request, so the screen only opens once */
    public static boolean consumeOpenRequest() {
        return OPEN_REQUESTED.compareAndSet(true, false);
    }
}
