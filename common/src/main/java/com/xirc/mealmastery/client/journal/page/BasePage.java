package com.xirc.mealmastery.client.journal.page;

import com.xirc.mealmastery.client.journal.JournalPage;
import com.xirc.mealmastery.client.journal.JournalScreen;
import com.xirc.mealmastery.client.journal.JournalTheme;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

/**
 * Shared layout bookkeeping for every page: bounds, the font, and the empty
 * state every page needs.
 */
public abstract class BasePage implements JournalPage {

    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected JournalScreen screen;

    @Override
    public void layout(JournalScreen screen, int x, int y, int width, int height) {
        this.screen = screen;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        onLayout();
    }

    protected void onLayout() {
    }

    protected Font font() {
        return Minecraft.getInstance().font;
    }

    protected int right() {
        return x + width;
    }

    protected int bottom() {
        return y + height;
    }

    /** Draws a wrapped, centred "nothing here yet, and here is what to do" message. */
    protected void renderEmpty(GuiGraphics graphics) {
        Component message = emptyMessage();
        int centreX = x + width / 2;
        int lineY = y + height / 3;
        for (var line : font().split(message, Math.min(width - 16, 220))) {
            int lineWidth = font().width(line);
            graphics.drawString(font(), line, centreX - lineWidth / 2, lineY,
                    JournalTheme.TEXT_DIM, false);
            lineY += 11;
        }
    }

    protected void sectionTitle(GuiGraphics graphics, Component title, int titleY) {
        graphics.drawString(font(), title, x, titleY, JournalTheme.TEXT_ACCENT, false);
        JournalTheme.divider(graphics, x, titleY + 10, width);
    }
}
