package com.xirc.mealmastery.client;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.toasts.Toast;
import net.minecraft.client.gui.components.toasts.ToastComponent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/**
 * Meal Mastery's own toast.
 *
 * <p>Vanilla's {@code SystemToast} would do the job, except that a system
 * toast's identity is a {@code SystemToastId} instance and the only ones that
 * exist are vanilla's own constants. A mod that reads those constants to decide
 * how to present a system toast has nothing to match a freshly constructed id
 * against, and handing it one crashed the game as soon as this mod had anything
 * to announce. Drawing our own toast keeps the mod out of that lookup
 * altogether.</p>
 *
 * <p>No new texture: the background is the same vanilla toast sprite
 * {@code SystemToast} blits, and the layout mirrors it so a Meal Mastery toast
 * does not look foreign next to one of vanilla's.</p>
 */
public final class MealMasteryToast implements Toast {

    /**
     * Shared by every Meal Mastery toast, so a second announcement rewrites the
     * one already on screen instead of starting a column of them.
     */
    public static final Object TOKEN = new Object();

    private static final ResourceLocation BACKGROUND =
            ResourceLocation.withDefaultNamespace("toast/system");
    private static final int TEXTURE_WIDTH = 160;
    private static final int TEXTURE_HEIGHT = 32;
    private static final long DISPLAY_MILLIS = 5000L;

    private Component title;
    private Component subtitle;
    private int width;
    private long shownAt;
    private boolean changed = true;

    public MealMasteryToast(Component title, Component subtitle) {
        set(title, subtitle);
    }

    /** Rewrites an on-screen toast rather than queueing another one. */
    public void reset(Component title, Component subtitle) {
        set(title, subtitle);
        this.changed = true;
    }

    private void set(Component title, Component subtitle) {
        this.title = title;
        this.subtitle = subtitle == null || subtitle.getString().isEmpty() ? null : subtitle;
        this.width = 0;
    }

    @Override
    public Object getToken() {
        return TOKEN;
    }

    @Override
    public int width() {
        return width > 0 ? width : TEXTURE_WIDTH;
    }

    @Override
    public int height() {
        return TEXTURE_HEIGHT;
    }

    @Override
    public Visibility render(GuiGraphics graphics, ToastComponent component, long sinceFirstVisible) {
        if (changed) {
            shownAt = sinceFirstVisible;
            changed = false;
        }
        Font font = component.getMinecraft().font;
        if (width == 0) {
            // Measured here rather than in the constructor: the font is a client
            // resource and a toast can be built before one is available.
            width = Math.max(TEXTURE_WIDTH, 30 + Math.max(font.width(title),
                    subtitle == null ? 0 : font.width(subtitle)));
        }

        renderBackground(graphics);
        if (subtitle == null) {
            graphics.drawString(font, title, 18, 12, 0xFFFF00, false);
        } else {
            graphics.drawString(font, title, 18, 7, 0xFFFF00, false);
            graphics.drawString(font, subtitle, 18, 18, 0xFFFFFF, false);
        }

        double visibleFor = DISPLAY_MILLIS * component.getNotificationDisplayTimeMultiplier();
        return sinceFirstVisible - shownAt < visibleFor ? Visibility.SHOW : Visibility.HIDE;
    }

    /**
     * Draws the sprite at whatever width the text needs.
     *
     * <p>Stretching the sprite would smear its border, so a wider toast is
     * built from the left edge, the middle tiled across, and the right edge —
     * which is how vanilla draws a system toast that outgrew its texture.</p>
     */
    private void renderBackground(GuiGraphics graphics) {
        int total = width();
        if (total == TEXTURE_WIDTH) {
            graphics.blitSprite(BACKGROUND, 0, 0, total, TEXTURE_HEIGHT);
            return;
        }
        int left = 20;
        int right = Math.min(60, total - left);
        graphics.blitSprite(BACKGROUND, TEXTURE_WIDTH, TEXTURE_HEIGHT,
                0, 0, 0, 0, left, TEXTURE_HEIGHT);
        for (int x = left; x < total - right; x += 64) {
            graphics.blitSprite(BACKGROUND, TEXTURE_WIDTH, TEXTURE_HEIGHT,
                    32, 0, x, 0, Math.min(64, total - x - right), TEXTURE_HEIGHT);
        }
        graphics.blitSprite(BACKGROUND, TEXTURE_WIDTH, TEXTURE_HEIGHT,
                TEXTURE_WIDTH - right, 0, total - right, 0, right, TEXTURE_HEIGHT);
    }
}
