package com.xirc.mealmastery.client.hud;

import com.xirc.mealmastery.client.ClientJournalState;
import com.xirc.mealmastery.client.journal.JournalText;
import com.xirc.mealmastery.client.journal.JournalTheme;
import com.xirc.mealmastery.config.ClientConfig;
import com.xirc.mealmastery.config.ConfigManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The optional compact HUD and the pinned-recipe tracker.
 *
 * <p>Off by default, and even when enabled it defaults to appearing only for a
 * moment after XP is gained — the design is explicit that the HUD must not
 * permanently clutter the screen.</p>
 */
public final class MealMasteryHud {

    private static long visibleUntilTick;
    private static long lastKnownXp;
    private static long clientTick;

    /** Cached inventory counts for pinned dishes; refreshed on a slow cadence. */
    private static java.util.Map<ResourceLocation, Integer> inventoryCounts = java.util.Map.of();
    private static long lastInventoryScanTick = Long.MIN_VALUE;

    private MealMasteryHud() {
    }

    /** Called once per client tick. */
    public static void tick() {
        clientTick++;
        long xp = ClientJournalState.cookingXp();
        if (xp != lastKnownXp) {
            lastKnownXp = xp;
            visibleUntilTick = clientTick + ConfigManager.client().hud.visibleTicks;
        }
    }

    public static void render(GuiGraphics graphics) {
        ClientConfig config = ConfigManager.client();
        if (!config.hud.enabled) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui || minecraft.screen != null) {
            return;
        }
        boolean visible = !config.hud.onlyWhileActive || clientTick <= visibleUntilTick;
        if (!visible) {
            return;
        }

        graphics.pose().pushPose();
        graphics.pose().scale(config.hud.scale, config.hud.scale, 1.0F);

        int screenWidth = (int) (graphics.guiWidth() / config.hud.scale);
        int screenHeight = (int) (graphics.guiHeight() / config.hud.scale);
        int panelWidth = 92;
        int x = switch (config.hud.anchor) {
            case TOP_LEFT, BOTTOM_LEFT -> config.hud.offsetX;
            case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - panelWidth - config.hud.offsetX;
        };
        int y = switch (config.hud.anchor) {
            case TOP_LEFT, TOP_RIGHT -> config.hud.offsetY;
            case BOTTOM_LEFT, BOTTOM_RIGHT -> screenHeight - 24 - config.hud.offsetY;
        };

        renderLevel(graphics, x, y, panelWidth);
        if (config.hud.showPinnedRecipes) {
            renderPinned(graphics, x, y + 20, panelWidth);
        }
        graphics.pose().popPose();
    }

    private static void renderLevel(GuiGraphics graphics, int x, int y, int panelWidth) {
        Minecraft minecraft = Minecraft.getInstance();
        Component label = Component.literal("Cooking Lv. " + ClientJournalState.cookingLevel());
        graphics.drawString(minecraft.font, label, x, y, JournalTheme.TEXT, true);

        long into = ClientJournalState.xpIntoLevel();
        long needed = ClientJournalState.xpForCurrentLevel();
        JournalTheme.bar(graphics, x, y + 10, panelWidth, 4,
                needed <= 0 ? 1.0F : (float) into / needed, needed < 0);
    }

    /**
     * The pinned-recipe tracker.
     *
     * <p>Inventory is counted at most once a second and only for the handful of
     * items the pins actually mention, so nothing scans an inventory per frame
     *.</p>
     */
    private static void renderPinned(GuiGraphics graphics, int x, int y, int panelWidth) {
        List<ResourceLocation> pinned = ClientJournalState.pinned();
        if (pinned.isEmpty()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        refreshInventoryCounts(pinned);

        int cursorY = y;
        for (ResourceLocation dish : pinned) {
            var detail = ClientJournalState.detail(dish);
            graphics.drawString(minecraft.font, JournalText.nameOf(dish), x, cursorY,
                    JournalTheme.TEXT_ACCENT, true);
            cursorY += 10;
            if (detail == null) {
                continue;
            }
            for (ResourceLocation ingredient : detail.ingredients()) {
                int have = inventoryCounts.getOrDefault(ingredient, 0);
                Component line = JournalText.nameOf(ingredient).copy()
                        .append("  " + have);
                graphics.drawString(minecraft.font, line, x + 4, cursorY,
                        have > 0 ? JournalTheme.TEXT_GOOD : JournalTheme.TEXT_DIM, true);
                cursorY += 9;
            }
            cursorY += 2;
        }
    }

    private static void refreshInventoryCounts(List<ResourceLocation> pinned) {
        if (clientTick - lastInventoryScanTick < 20L) {
            return;
        }
        lastInventoryScanTick = clientTick;

        java.util.Set<ResourceLocation> wanted = new java.util.HashSet<>();
        for (ResourceLocation dish : pinned) {
            var detail = ClientJournalState.detail(dish);
            if (detail != null) {
                wanted.addAll(detail.ingredients());
            }
        }
        if (wanted.isEmpty()) {
            inventoryCounts = java.util.Map.of();
            return;
        }
        java.util.Map<ResourceLocation, Integer> counts = new java.util.HashMap<>();
        Inventory inventory = Minecraft.getInstance().player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = net.minecraft.core.registries.BuiltInRegistries.ITEM
                    .getKey(stack.getItem());
            if (id != null && wanted.contains(id)) {
                counts.merge(id, stack.getCount(), Integer::sum);
            }
        }
        inventoryCounts = counts;
    }
}
