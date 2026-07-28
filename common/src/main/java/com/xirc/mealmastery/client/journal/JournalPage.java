package com.xirc.mealmastery.client.journal;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * One tab of the journal.
 *
 * <p>Pages own their own content area and nothing else; the frame draws the
 * sidebar, header and search box. A page is rebuilt on resize rather than
 * caching pixel positions.</p>
 */
public interface JournalPage {

    String id();

    Component title();

    /** Called when the page becomes visible and whenever the window resizes. */
    void layout(JournalScreen screen, int x, int y, int width, int height);

    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick);

    default boolean mouseClicked(double mouseX, double mouseY, int button) {
        return false;
    }

    default boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return false;
    }

    /** Pages that show a list react to the frame's search box. */
    default void onSearchChanged(String query) {
    }

    default boolean usesSearch() {
        return false;
    }

    /** Rendered when the page has nothing to show; never leave a blank area. */
    default Component emptyMessage() {
        return Component.translatable("mealmastery.empty.activity");
    }
}
