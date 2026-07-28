package com.xirc.mealmastery.client.journal;

import com.xirc.mealmastery.client.ClientJournalState;
import com.xirc.mealmastery.config.ClientConfig;
import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.network.Packets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The stats panel docked beside a cooking screen.
 *
 * <p>Shows culinary progress next to whatever the player is cooking on, so
 * mastery is visible while cooking rather than only inside the journal.</p>
 *
 * <p>Drawn entirely from code and item icons. The caller supplies the GUI
 * box because {@code leftPos} and {@code imageWidth} are protected on
 * {@code AbstractContainerScreen} and each loader reaches them differently.</p>
 *
 * <p>Every label/value row is measured before it is drawn: values are
 * right-aligned, so a long translation would otherwise run straight into its
 * own label.</p>
 */
public final class StatsSidePanel {

    private static final int WIDTH = 150;
    private static final int GAP = 6;
    private static final int PADDING = 7;
    private static final int LINE = 11;

    private StatsSidePanel() {
    }

    /**
     * @param guiLeft/guiTop/guiWidth the container GUI's box, in screen pixels
     * @param screenWidth/screenHeight the window, in the same scaled pixels
     */
    public static void render(GuiGraphics graphics, int guiLeft, int guiTop, int guiWidth,
                              int screenWidth, int screenHeight, AbstractContainerMenu menu) {
        ClientConfig config = ConfigManager.client();
        if (config.journal.sidePanel == ClientConfig.SidePanel.OFF
                || !ClientJournalState.isCatalogueComplete()) {
            return;
        }

        // Never beside the player's own inventory, whatever else is true. The
        // server does flag it as a cooking surface while something sits in the
        // 2x2 grid - correctly, for attribution - but a stats panel hanging off
        // the inventory screen is not what anyone wants to look at.
        if (isPersonalScreen(menu)) {
            return;
        }

        // CULINARY trusts the server, which already decides this for
        // attribution, plus the menus that are cooking surfaces by class. There
        // is deliberately no "this screen contains food" fallback: a chest or a
        // creative tab full of dishes is not a kitchen.
        if (config.journal.sidePanel == ClientConfig.SidePanel.CULINARY
                && !ClientJournalState.isMenuCulinary()
                && !isVanillaCookingMenu(menu)) {
            return;
        }

        ResourceLocation focus = focusedDish(menu);

        Font font = Minecraft.getInstance().font;
        int height = measure(focus);

        // Prefer the right of the GUI, fall back to the left, and if neither
        // side has room, sit against the nearest edge. The old code gave up and
        // drew nothing in that case, which is why the panel wandered off or
        // disappeared at anything but a wide window: at common GUI scales there
        // genuinely is not 150px spare beside a 176px container.
        int x = guiLeft + guiWidth + GAP;
        if (x + WIDTH > screenWidth) {
            int onLeft = guiLeft - WIDTH - GAP;
            x = onLeft >= 0 ? onLeft : Math.max(0, screenWidth - WIDTH);
        }
        // Keep the whole panel on screen vertically too: it is taller when a
        // dish is in focus, and a short window would otherwise clip the bottom.
        int y = Math.max(0, Math.min(guiTop, screenHeight - height));

        JournalTheme.panel(graphics, x, y, WIDTH, height);

        int left = x + PADDING;
        int right = x + WIDTH - PADDING;
        int cursorY = y + PADDING;

        cursorY = renderHeader(graphics, font, left, cursorY);
        cursorY = renderLevel(graphics, font, left, right, cursorY);
        cursorY = renderTotals(graphics, font, left, right, cursorY);

        if (focus != null) {
            JournalTheme.divider(graphics, left, cursorY, right - left);
            cursorY += 6;
            renderDish(graphics, font, left, right, cursorY, focus);
        }
    }

    /** Height is computed up front so the panel never clips its own content. */
    private static int measure(ResourceLocation focus) {
        int height = PADDING * 2
                + LINE + 10          // header + method line
                + LINE + 8           // level line + xp bar
                + 6 + LINE * 3;      // divider + three totals
        if (focus != null) {
            height += 6 + 20 + LINE + 8 + LINE;
        }
        return height;
    }

    private static int renderHeader(GuiGraphics graphics, Font font, int left, int cursorY) {
        graphics.drawString(font, Component.translatable("mealmastery.journal.title"),
                left, cursorY, JournalTheme.TEXT_ACCENT, false);
        cursorY += LINE;

        // The method sits on its own line rather than beside the title: at this
        // width "Meal Mastery" and "Cooking Pot" would collide.
        ResourceLocation method = ClientJournalState.menuMethod();
        if (method != null) {
            graphics.drawString(font, JournalText.method(method), left, cursorY,
                    JournalTheme.TEXT_FAINT, false);
        }
        return cursorY + 10;
    }

    private static int renderLevel(GuiGraphics graphics, Font font, int left, int right,
                                   int cursorY) {
        Component level = Component.literal("Lv. " + ClientJournalState.cookingLevel());
        graphics.drawString(font, level, left, cursorY, JournalTheme.TEXT, false);

        long into = ClientJournalState.xpIntoLevel();
        long needed = ClientJournalState.xpForCurrentLevel();
        if (!JournalText.cozy()) {
            Component xp = Component.literal(needed < 0
                    ? JournalText.number(into) + " xp"
                    : into + " / " + needed);
            if (font.width(level) + font.width(xp) + 6 <= right - left) {
                JournalTheme.value(graphics, font, xp, right, cursorY, JournalTheme.TEXT_FAINT);
            }
        }
        cursorY += LINE;
        JournalTheme.bar(graphics, left, cursorY, right - left, 5,
                needed <= 0 ? 1.0F : (float) into / needed, needed < 0);
        return cursorY + 8;
    }

    private static int renderTotals(GuiGraphics graphics, Font font, int left, int right,
                                    int cursorY) {
        JournalTheme.divider(graphics, left, cursorY, right - left);
        cursorY += 6;

        int total = ClientJournalState.dishCount();
        cursorY = row(graphics, font, left, right, cursorY, "mealmastery.panel.discovered",
                JournalText.fraction(ClientJournalState.discoveredCount(), total));
        cursorY = row(graphics, font, left, right, cursorY, "mealmastery.panel.mastered",
                JournalText.number(ClientJournalState.masteredCount()));
        cursorY = row(graphics, font, left, right, cursorY, "mealmastery.panel.prepared",
                JournalText.number(ClientJournalState.mealsPrepared()));
        return cursorY;
    }

    private static void renderDish(GuiGraphics graphics, Font font, int left, int right,
                                   int cursorY, ResourceLocation dish) {
        graphics.renderFakeItem(JournalText.stackOf(dish), left, cursorY);
        drawTruncated(graphics, font, JournalText.nameOf(dish), left + 20, cursorY + 4,
                right - left - 20, JournalTheme.TEXT);
        cursorY += 20;

        if (!ClientJournalState.isDiscovered(dish)) {
            graphics.drawString(font, Component.translatable("mealmastery.tooltip.undiscovered"),
                    left, cursorY, JournalTheme.TEXT_FAINT, false);
            return;
        }

        boolean mastered = ClientJournalState.rank(dish).isMastered();
        graphics.drawString(font, JournalText.rankOf(dish), left, cursorY,
                mastered ? JournalTheme.TEXT_GOOD : JournalTheme.TEXT_DIM, false);
        if (ConfigManager.client().journal.showStars) {
            JournalTheme.value(graphics, font, Component.literal(JournalText.stars(dish)),
                    right, cursorY, JournalTheme.TEXT_ACCENT);
        }
        cursorY += LINE;
        JournalTheme.bar(graphics, left, cursorY, right - left, 5,
                JournalText.masteryFraction(dish), mastered);
        cursorY += 8;

        Packets.RecordView record = ClientJournalState.record(dish);
        if (record != null && !JournalText.cozy()) {
            // One combined line: two label/value rows would not fit at this width.
            graphics.drawString(font, Component.literal(
                            "Cooked " + JournalText.number(record.prepared())
                                    + "  ·  Eaten " + JournalText.number(record.eaten())),
                    left, cursorY, JournalTheme.TEXT_FAINT, false);
        }
    }

    private static int row(GuiGraphics graphics, Font font, int left, int right, int cursorY,
                           String labelKey, String value) {
        Component label = Component.translatable(labelKey);
        Component shown = Component.literal(value);
        graphics.drawString(font, label, left, cursorY, JournalTheme.TEXT_DIM, false);
        if (font.width(label) + font.width(shown) + 6 <= right - left) {
            JournalTheme.value(graphics, font, shown, right, cursorY, JournalTheme.TEXT);
        }
        return cursorY + LINE;
    }

    private static void drawTruncated(GuiGraphics graphics, Font font, Component text, int x,
                                      int y, int maxWidth, int colour) {
        if (font.width(text) <= maxWidth) {
            graphics.drawString(font, text, x, y, colour, false);
            return;
        }
        String clipped = font.plainSubstrByWidth(text.getString(), maxWidth - font.width("..."));
        graphics.drawString(font, clipped + "...", x, y, colour, false);
    }

    /**
     * Screens that are the player's own pockets rather than a workstation: the
     * survival inventory and the creative tabs.
     */
    private static boolean isPersonalScreen(AbstractContainerMenu menu) {
        if (menu instanceof net.minecraft.world.inventory.InventoryMenu) {
            return true;
        }
        // The creative picker has its own package-private menu class, so it is
        // recognised by the screen instead.
        return Minecraft.getInstance().screen
                instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
    }

    /** Cooking surfaces recognisable from the menu class alone. */
    private static boolean isVanillaCookingMenu(AbstractContainerMenu menu) {
        return menu instanceof net.minecraft.world.inventory.CraftingMenu
                || menu instanceof net.minecraft.world.inventory.AbstractFurnaceMenu;
    }

    /**
     * The dish this panel should talk about.
     *
     * <p>Only dishes this workstation can actually produce qualify. Without
     * that check the ingredients count too — tomatoes and wheat dough are
     * themselves craftable food, so a pot part-way through a recipe would
     * report "Tomato, undiscovered" while cooking something else entirely.</p>
     *
     * <p>Player inventory slots are skipped so the hotbar never hijacks the
     * panel, and ties go to whichever dish is furthest along.</p>
     */
    private static ResourceLocation focusedDish(AbstractContainerMenu menu) {
        if (menu == null) {
            return null;
        }
        ResourceLocation method = ClientJournalState.menuMethod();
        List<ResourceLocation> candidates = new ArrayList<>();
        for (Slot slot : menu.slots) {
            if (slot.container instanceof net.minecraft.world.entity.player.Inventory) {
                continue;
            }
            ItemStack stack = slot.getItem();
            if (stack.isEmpty()) {
                continue;
            }
            ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
            if (id == null) {
                continue;
            }
            Packets.DishView dish = ClientJournalState.dish(id);
            if (dish != null && producedHere(dish, method)) {
                candidates.add(id);
            }
        }
        return candidates.stream()
                .max(Comparator.comparingLong(StatsSidePanel::preparedCount))
                .orElse(null);
    }

    /**
     * Whether this workstation makes this dish.
     *
     * <p>When the server has not named a method — a configured workstation, or
     * the packet has not landed — nothing is assumed, and the panel simply
     * shows no dish rather than picking one at random.</p>
     */
    private static boolean producedHere(Packets.DishView dish, ResourceLocation method) {
        return method != null && dish.methods().contains(method);
    }

    private static long preparedCount(ResourceLocation dish) {
        Packets.RecordView record = ClientJournalState.record(dish);
        return record == null ? 0L : record.prepared();
    }
}
