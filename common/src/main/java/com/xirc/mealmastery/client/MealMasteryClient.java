package com.xirc.mealmastery.client;

import com.xirc.mealmastery.client.hud.MealMasteryHud;
import com.xirc.mealmastery.client.journal.JournalScreen;
import com.xirc.mealmastery.client.journal.JournalText;
import com.xirc.mealmastery.config.ClientConfig;
import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.network.Packets;
import com.xirc.mealmastery.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/**
 * Client-side bootstrap: the journal keybind, the per-tick pump, and the
 * notification queue.
 *
 * <p>Notifications are aggregated and rate limited before they reach the toast
 * system: a burst of small XP gains becomes one "+12 Cooking XP" rather than
 * six separate popups, and no more toasts are shown per second than the
 * player allows.</p>
 */
public final class MealMasteryClient {

    private static long pendingXp;
    private static int xpTimer;
    private static long lastToastTick;
    private static long clientTick;

    private MealMasteryClient() {
    }

    public static void initialize() {
        ConfigManager.initialize(Services.PLATFORM.configDirectory());
    }

    /** Called once per client tick from the loader bridge. */
    public static void tick() {
        clientTick++;
        MealMasteryHud.tick();

        if (ClientPacketHandler.consumeOpenRequest()) {
            openJournal();
        }
        drainNotifications();
        flushXpPopup();
    }

    public static void openJournal() {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player != null && !(minecraft.screen instanceof JournalScreen)) {
            minecraft.setScreen(new JournalScreen());
        }
    }

    public static void onDisconnect() {
        // A journal from one world must never leak into the next.
        ClientJournalState.clear();
        pendingXp = 0L;
        xpTimer = 0;
    }

    private static void drainNotifications() {
        ClientConfig config = ConfigManager.client();
        Packets.Notify notify;
        while ((notify = ClientJournalState.pollNotification()) != null) {
            if (notify.kind() == Packets.Notify.Kind.XP) {
                // Accumulate rather than showing each gain individually.
                pendingXp += notify.amount();
                xpTimer = config.notifications.xpPopupAggregationTicks;
                continue;
            }
            if (!allowed(config, notify.kind())) {
                continue;
            }
            long minimumGap = Math.max(1L, 20L / Math.max(1, config.notifications.maxToastsPerSecond));
            if (clientTick - lastToastTick < minimumGap) {
                // Dropped rather than queued: a backlog of stale toasts is worse
                // than missing one.
                continue;
            }
            lastToastTick = clientTick;
            showToast(notify, config);
        }
    }

    private static void flushXpPopup() {
        if (pendingXp <= 0L) {
            return;
        }
        if (--xpTimer > 0) {
            return;
        }
        ClientConfig config = ConfigManager.client();
        if (config.notifications.xpPopups && Minecraft.getInstance().player != null) {
            Minecraft.getInstance().player.displayClientMessage(
                    Component.translatable("mealmastery.notify.xp", JournalText.number(pendingXp))
                            .withStyle(ChatFormatting.GOLD), true);
        }
        pendingXp = 0L;
    }

    private static boolean allowed(ClientConfig config, Packets.Notify.Kind kind) {
        return switch (kind) {
            case DISCOVERY -> config.notifications.discoveries;
            case MASTERY_RANK, MASTERED -> config.notifications.masteryRanks;
            case LEVEL_UP -> config.notifications.levelUps;
            case CHALLENGE -> config.notifications.challenges;
            case MILESTONE -> config.notifications.milestones;
            case XP -> config.notifications.xpPopups;
        };
    }

    private static void showToast(Packets.Notify notify, ClientConfig config) {
        Minecraft minecraft = Minecraft.getInstance();
        Component title = switch (notify.kind()) {
            case DISCOVERY -> Component.translatable("mealmastery.notify.discovery");
            case MASTERY_RANK -> Component.translatable("mealmastery.notify.mastery");
            case MASTERED -> Component.translatable("mealmastery.notify.mastered");
            case LEVEL_UP -> Component.translatable("mealmastery.notify.level_up", notify.amount());
            case CHALLENGE -> Component.translatable("mealmastery.notify.challenge");
            case MILESTONE -> Component.translatable("mealmastery.notify.milestone");
            case XP -> Component.translatable("mealmastery.notify.xp", notify.amount());
        };
        Component subtitle = notify.subject() == null
                ? Component.empty() : JournalText.nameOf(notify.subject());

        // SystemToast is vanilla's own widget, so no texture is added.
        // Reusing one id means a new notification replaces the previous one
        // rather than stacking a backlog down the screen.
        minecraft.getToasts().addToast(new SystemToast(
                SystemToast.SystemToastIds.TUTORIAL_HINT, title, subtitle));

        if (config.notifications.playSounds && minecraft.player != null) {
            // An existing vanilla sound; no custom audio asset.
            minecraft.player.playSound(
                    notify.kind() == Packets.Notify.Kind.MASTERED
                            ? SoundEvents.PLAYER_LEVELUP
                            : SoundEvents.NOTE_BLOCK_BELL.value(),
                    0.5F, 1.2F);
        }
    }

    public static SoundSource soundSource() {
        return SoundSource.PLAYERS;
    }
}
