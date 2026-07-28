package com.xirc.mealmastery.client.journal;

import net.minecraft.client.gui.GuiGraphics;

import java.util.List;

/**
 * A virtualised vertical list.
 *
 * <p>Only the rows inside the viewport are drawn, which is what keeps a
 * thousand-dish modpack scrolling smoothly instead of building a thousand
 * widgets.</p>
 */
public final class ScrollList<T> {

    /** Draws one row; the list handles scissoring, hover and positioning. */
    public interface RowRenderer<T> {
        void render(GuiGraphics graphics, T item, int x, int y, int width, int height,
                    boolean hovered);
    }

    private final int rowHeight;
    private final int rowSpacing;
    private final RowRenderer<T> renderer;

    private List<T> items = List.of();
    private int x;
    private int y;
    private int width;
    private int height;
    private double scroll;

    public ScrollList(int rowHeight, int rowSpacing, RowRenderer<T> renderer) {
        this.rowHeight = rowHeight;
        this.rowSpacing = rowSpacing;
        this.renderer = renderer;
    }

    public void bounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        clampScroll();
    }

    public void items(List<T> items) {
        this.items = items == null ? List.of() : items;
        // A filter change can shorten the list past the current offset.
        clampScroll();
    }

    public List<T> items() {
        return items;
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

    private int stride() {
        return rowHeight + rowSpacing;
    }

    private int contentHeight() {
        return items.isEmpty() ? 0 : items.size() * stride() - rowSpacing;
    }

    private int maxScroll() {
        return Math.max(0, contentHeight() - height);
    }

    private void clampScroll() {
        scroll = Math.max(0.0, Math.min(maxScroll(), scroll));
    }

    public void render(GuiGraphics graphics, int mouseX, int mouseY) {
        if (items.isEmpty()) {
            return;
        }
        graphics.enableScissor(x, y, x + width, y + height);

        int first = Math.max(0, (int) (scroll / stride()));
        int last = Math.min(items.size(), first + height / stride() + 2);
        int barWidth = maxScroll() > 0 ? 4 : 0;

        for (int index = first; index < last; index++) {
            int rowY = y + index * stride() - (int) scroll;
            boolean hovered = mouseX >= x && mouseX < x + width - barWidth
                    && mouseY >= rowY && mouseY < rowY + rowHeight
                    && mouseY >= y && mouseY < y + height;
            renderer.render(graphics, items.get(index), x, rowY, width - barWidth, rowHeight,
                    hovered);
        }
        graphics.disableScissor();

        if (barWidth > 0) {
            renderScrollbar(graphics, barWidth);
        }
    }

    private void renderScrollbar(GuiGraphics graphics, int barWidth) {
        int trackX = x + width - barWidth;
        graphics.fill(trackX, y, trackX + barWidth, y + height, 0x33000000);

        int thumbHeight = Math.max(16, height * height / Math.max(1, contentHeight()));
        int travel = height - thumbHeight;
        int thumbY = y + (int) (travel * (scroll / Math.max(1, maxScroll())));
        graphics.fill(trackX, thumbY, trackX + barWidth, thumbY + thumbHeight,
                JournalTheme.TEXT_FAINT);
    }

    /** @return the item under the cursor, or {@code null} */
    public T itemAt(double mouseX, double mouseY) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return null;
        }
        int index = (int) ((mouseY - y + scroll) / stride());
        if (index < 0 || index >= items.size()) {
            return null;
        }
        // Reject the gap between rows so a click never lands on the wrong item.
        double offsetInRow = (mouseY - y + scroll) % stride();
        return offsetInRow < rowHeight ? items.get(index) : null;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (mouseX < x || mouseX >= x + width || mouseY < y || mouseY >= y + height) {
            return false;
        }
        scroll -= delta * stride();
        clampScroll();
        return true;
    }

    public void scrollToTop() {
        scroll = 0.0;
    }
}
