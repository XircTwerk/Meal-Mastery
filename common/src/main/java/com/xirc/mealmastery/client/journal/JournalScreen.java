package com.xirc.mealmastery.client.journal;

import com.xirc.mealmastery.client.ClientJournalState;
import com.xirc.mealmastery.client.journal.page.ChallengesPage;
import com.xirc.mealmastery.client.journal.page.CollectionsPage;
import com.xirc.mealmastery.client.journal.page.IngredientsPage;
import com.xirc.mealmastery.client.journal.page.MethodsPage;
import com.xirc.mealmastery.client.journal.page.OverviewPage;
import com.xirc.mealmastery.client.journal.page.RecipesPage;
import com.xirc.mealmastery.client.journal.page.RecordsPage;
import com.xirc.mealmastery.client.journal.page.StatisticsPage;
import com.xirc.mealmastery.network.Network;
import com.xirc.mealmastery.network.Packets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * The culinary journal.
 *
 * <p>A persistent sidebar on the left, a header with the player's level and a
 * search box, and one page's content on the right. Everything is drawn from
 * code-generated panels and item icons that other mods already registered —
 * there is no texture in this mod.</p>
 *
 * <p>The layout is proportional and clamped, so it works at any GUI scale and
 * on both narrow and ultrawide windows.</p>
 */
public final class JournalScreen extends Screen {

    private static final int MIN_WIDTH = 320;
    private static final int MAX_WIDTH = 480;
    private static final int MIN_HEIGHT = 200;
    private static final int MAX_HEIGHT = 300;
    private static final int SIDEBAR_WIDTH = 92;
    private static final int HEADER_HEIGHT = 26;
    private static final int PADDING = 8;

    private final List<JournalPage> pages = new ArrayList<>();
    private JournalPage active;

    private int frameX;
    private int frameY;
    private int frameWidth;
    private int frameHeight;

    private EditBox search;

    public JournalScreen() {
        super(Component.translatable("mealmastery.journal.title"));
    }

    @Override
    protected void init() {
        // Ask for a fresh profile every time the journal opens: the server is
        // the only source of truth and may have changed while it was closed.
        Network.toServer(new Packets.RequestProfile());

        frameWidth = Math.max(MIN_WIDTH, Math.min(MAX_WIDTH, width - 40));
        frameHeight = Math.max(MIN_HEIGHT, Math.min(MAX_HEIGHT, height - 40));
        frameX = (width - frameWidth) / 2;
        frameY = (height - frameHeight) / 2;

        if (pages.isEmpty()) {
            pages.add(new OverviewPage());
            pages.add(new RecipesPage());
            pages.add(new IngredientsPage());
            pages.add(new MethodsPage());
            pages.add(new CollectionsPage());
            pages.add(new ChallengesPage());
            pages.add(new StatisticsPage());
            pages.add(new RecordsPage());
            active = pages.get(0);
        }

        int searchWidth = 96;
        search = new EditBox(font, frameX + frameWidth - searchWidth - PADDING,
                frameY + 6, searchWidth, 14, Component.translatable("mealmastery.journal.search"));
        search.setHint(Component.literal("Search..."));
        search.setResponder(query -> {
            if (active != null) {
                active.onSearchChanged(query);
            }
        });
        addRenderableWidget(search);

        addRenderableWidget(net.minecraft.client.gui.components.Button.builder(
                        Component.literal("\u2699"), button -> minecraft.setScreen(
                                new ClientSettingsScreen(this)))
                .bounds(frameX + frameWidth - PADDING - 12, frameY + frameHeight - 18, 12, 12)
                .build());

        layoutActivePage();
    }

    private void layoutActivePage() {
        if (active == null) {
            return;
        }
        int contentX = frameX + SIDEBAR_WIDTH + PADDING;
        int contentY = frameY + HEADER_HEIGHT + PADDING;
        active.layout(this,
                contentX,
                contentY,
                frameX + frameWidth - PADDING - contentX,
                frameY + frameHeight - PADDING - contentY);
        search.setVisible(active.usesSearch());
        search.setEditable(active.usesSearch());
        if (active.usesSearch()) {
            active.onSearchChanged(search.getValue());
        }
    }

    public void select(JournalPage page) {
        active = page;
        layoutActivePage();
    }

    public net.minecraft.client.gui.Font font() {
        return font;
    }

    /**
     * 1.21's {@code Screen#renderBackground} runs a blur post-effect over the
     * world. {@code Screen#render} calls it internally, so suppressing the blur
     * means overriding this hook — replacing the call inside {@code render} is
     * not enough, because {@code super.render} reaches it anyway.
     *
     * <p>A plain dim backdrop is what vanilla's own container screens use.</p>
     */
    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderTransparentBackground(graphics);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {

        JournalTheme.panel(graphics, frameX, frameY, frameWidth, frameHeight);
        JournalTheme.sidebar(graphics, frameX + 1, frameY + HEADER_HEIGHT,
                SIDEBAR_WIDTH - 1, frameHeight - HEADER_HEIGHT - 1);

        renderHeader(graphics);
        renderSidebar(graphics, mouseX, mouseY);

        if (active != null) {
            if (!ClientJournalState.isCatalogueComplete()) {
                centeredMessage(graphics, Component.translatable("mealmastery.journal.loading"));
            } else {
                active.render(graphics, mouseX, mouseY, partialTick);
            }
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphics graphics) {
        graphics.drawString(font, Component.translatable("mealmastery.journal.title"),
                frameX + PADDING, frameY + 9, JournalTheme.TEXT_ACCENT, false);

        Component level = Component.translatable("mealmastery.overview.level")
                .append(" " + ClientJournalState.cookingLevel());
        int right = search != null && search.visible
                ? search.getX() - 6 : frameX + frameWidth - PADDING;
        JournalTheme.value(graphics, font, level, right, frameY + 9, JournalTheme.TEXT);

        JournalTheme.divider(graphics, frameX + 1, frameY + HEADER_HEIGHT - 1, frameWidth - 2);
    }

    private void renderSidebar(GuiGraphics graphics, int mouseX, int mouseY) {
        int y = frameY + HEADER_HEIGHT + 4;
        for (JournalPage page : pages) {
            boolean hovered = mouseX >= frameX + 2 && mouseX < frameX + SIDEBAR_WIDTH - 2
                    && mouseY >= y && mouseY < y + 14;
            JournalTheme.card(graphics, frameX + 2, y, SIDEBAR_WIDTH - 5, 14,
                    hovered, page == active);
            graphics.drawString(font, page.title(), frameX + 8, y + 3,
                    page == active ? JournalTheme.TEXT_ACCENT : JournalTheme.TEXT_DIM, false);
            y += 16;
        }
    }

    private void centeredMessage(GuiGraphics graphics, Component message) {
        int x = frameX + SIDEBAR_WIDTH + (frameWidth - SIDEBAR_WIDTH) / 2;
        graphics.drawString(font, message, x - font.width(message) / 2,
                frameY + frameHeight / 2, JournalTheme.TEXT_DIM, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int y = frameY + HEADER_HEIGHT + 4;
        for (JournalPage page : pages) {
            if (mouseX >= frameX + 2 && mouseX < frameX + SIDEBAR_WIDTH - 2
                    && mouseY >= y && mouseY < y + 14) {
                select(page);
                return true;
            }
            y += 16;
        }
        if (active != null && active.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        // 1.21 split scrolling into two axes; the journal only scrolls vertically.
        if (active != null && active.mouseScrolled(mouseX, mouseY, scrollY)) {
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Keyboard navigation between tabs, so the journal is usable without a
        // mouse.
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_TAB && !hasShiftDown()
                && (search == null || !search.isFocused())) {
            int index = pages.indexOf(active);
            select(pages.get((index + 1) % pages.size()));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
