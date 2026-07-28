package com.xirc.mealmastery.client.journal;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.Component;

/**
 * The journal's look, drawn entirely in code.
 *
 * <p>Zero custom art is a hard rule, so every surface here is a
 * rectangle, a gradient or a one-pixel line. The palette is warm and earthy to
 * sit next to Farmer's Delight rather than looking like a generic grey config
 * menu, and every colour pair was picked to stay legible against the
 * panel it is drawn on.</p>
 */
public final class JournalTheme {

    private JournalTheme() {
    }

    // Warm parchment and cooked-wood tones, not Minecraft's stone greys.
    public static final int PANEL_TOP = 0xF22B2119;
    public static final int PANEL_BOTTOM = 0xF2201811;
    public static final int PANEL_BORDER = 0xFF5C4632;
    public static final int PANEL_HIGHLIGHT = 0x22FFE9C7;

    public static final int SIDEBAR_TOP = 0xF2231A13;
    public static final int SIDEBAR_BOTTOM = 0xF21B140E;

    public static final int CARD = 0x40000000;
    public static final int CARD_HOVER = 0x66FFE9C7;
    public static final int CARD_SELECTED = 0x33E8A93C;

    public static final int TEXT = 0xFFF3E6D0;
    public static final int TEXT_DIM = 0xFFA79279;
    public static final int TEXT_FAINT = 0xFF6E6153;
    public static final int TEXT_ACCENT = 0xFFE8A93C;
    public static final int TEXT_GOOD = 0xFF8FBF5E;

    public static final int BAR_TRACK = 0xFF120D09;
    public static final int BAR_FILL_START = 0xFFE8A93C;
    public static final int BAR_FILL_END = 0xFFC4762A;
    public static final int BAR_MASTERED_START = 0xFF9BD35E;
    public static final int BAR_MASTERED_END = 0xFF5E9433;

    public static final int DIVIDER = 0x33FFE9C7;

    /** A nine-slice-style panel built from fills: gradient body, lit top edge. */
    public static void panel(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fillGradient(x, y, x + width, y + height, PANEL_TOP, PANEL_BOTTOM);
        border(graphics, x, y, width, height, PANEL_BORDER);
        // A single lit row along the top reads as a bevel without any texture.
        graphics.fill(x + 1, y + 1, x + width - 1, y + 2, PANEL_HIGHLIGHT);
    }

    public static void sidebar(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fillGradient(x, y, x + width, y + height, SIDEBAR_TOP, SIDEBAR_BOTTOM);
        graphics.fill(x + width - 1, y, x + width, y + height, PANEL_BORDER);
    }

    public static void border(GuiGraphics graphics, int x, int y, int width, int height, int colour) {
        graphics.fill(x, y, x + width, y + 1, colour);
        graphics.fill(x, y + height - 1, x + width, y + height, colour);
        graphics.fill(x, y, x + 1, y + height, colour);
        graphics.fill(x + width - 1, y, x + width, y + height, colour);
    }

    public static void divider(GuiGraphics graphics, int x, int y, int width) {
        graphics.fill(x, y, x + width, y + 1, DIVIDER);
    }

    public static void card(GuiGraphics graphics, int x, int y, int width, int height,
                            boolean hovered, boolean selected) {
        graphics.fill(x, y, x + width, y + height, CARD);
        if (selected) {
            graphics.fill(x, y, x + width, y + height, CARD_SELECTED);
            graphics.fill(x, y, x + 2, y + height, TEXT_ACCENT);
        } else if (hovered) {
            graphics.fill(x, y, x + width, y + height, CARD_HOVER & 0x22FFFFFF);
            graphics.fill(x, y, x + 1, y + height, TEXT_DIM);
        }
    }

    /**
     * A progress bar.
     *
     * @param fraction clamped to 0..1; a zero-width fill is skipped so an empty
     *                 bar never shows a stray pixel
     */
    public static void bar(GuiGraphics graphics, int x, int y, int width, int height,
                           float fraction, boolean complete) {
        graphics.fill(x, y, x + width, y + height, BAR_TRACK);
        border(graphics, x, y, width, height, 0xFF3A2C1F);
        int filled = Math.round(Math.max(0.0F, Math.min(1.0F, fraction)) * (width - 2));
        if (filled > 0) {
            graphics.fillGradient(x + 1, y + 1, x + 1 + filled, y + height - 1,
                    complete ? BAR_MASTERED_START : BAR_FILL_START,
                    complete ? BAR_MASTERED_END : BAR_FILL_END);
        }
    }

    /**
     * Mastery shown as filled and empty pips.
     *
     * <p>Drawn as shapes <em>and</em> accompanied by the rank name elsewhere, so
     * rank is never communicated by colour alone.</p>
     */
    public static void pips(GuiGraphics graphics, int x, int y, int filled, int total) {
        for (int i = 0; i < total; i++) {
            int px = x + i * 6;
            if (i < filled) {
                graphics.fill(px, y, px + 4, y + 4, TEXT_ACCENT);
            } else {
                border(graphics, px, y, 4, 4, TEXT_FAINT);
            }
        }
    }

    public static void label(GuiGraphics graphics, Font font, Component text, int x, int y,
                            int colour) {
        graphics.drawString(font, text, x, y, colour, false);
    }

    /** Right-aligned text, used for every number in a label/value row. */
    public static void value(GuiGraphics graphics, Font font, Component text, int right, int y,
                             int colour) {
        graphics.drawString(font, text, right - font.width(text), y, colour, false);
    }

    /** A label on the left and its value on the right of the same row. */
    public static void row(GuiGraphics graphics, Font font, Component label, Component value,
                           int x, int right, int y) {
        label(graphics, font, label, x, y, TEXT_DIM);
        value(graphics, font, value, right, y, TEXT);
    }
}
