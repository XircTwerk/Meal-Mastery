package com.xirc.mealmastery.client.journal.page;

import com.xirc.mealmastery.client.ClientJournalState;
import com.xirc.mealmastery.client.journal.JournalText;
import com.xirc.mealmastery.client.journal.JournalTheme;
import com.xirc.mealmastery.client.journal.ScrollList;
import com.xirc.mealmastery.network.Packets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Per-method statistics.
 *
 * <p>One card per method with four numbers. Deliberately not a skill tree — the
 * design is explicit that this must stay understandable.</p>
 */
public final class MethodsPage extends BasePage {

    private final ScrollList<Packets.MethodView> list = new ScrollList<>(44, 4, this::renderCard);

    @Override
    public String id() {
        return "methods";
    }

    @Override
    public Component title() {
        return Component.translatable("mealmastery.page.methods");
    }

    @Override
    protected void onLayout() {
        list.bounds(x, y, width, height);
        List<Packets.MethodView> methods = new ArrayList<>(ClientJournalState.methods());
        methods.sort(Comparator.comparingLong(Packets.MethodView::preparations).reversed());
        list.items(methods);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        onLayout();
        if (list.isEmpty()) {
            renderEmpty(graphics);
        } else {
            list.render(graphics, mouseX, mouseY);
        }
    }

    private void renderCard(GuiGraphics graphics, Packets.MethodView method, int cardX, int cardY,
                            int cardWidth, int cardHeight, boolean hovered) {
        JournalTheme.card(graphics, cardX, cardY, cardWidth, cardHeight, hovered, false);
        graphics.drawString(font(), JournalText.method(method.method()), cardX + 6, cardY + 4,
                JournalTheme.TEXT_ACCENT, false);

        int rowY = cardY + 16;
        row(graphics, cardX, cardWidth, rowY, "Recipes prepared",
                JournalText.number(method.preparations()));
        row(graphics, cardX, cardWidth, rowY + 10, "Unique recipes",
                JournalText.number(method.uniqueMeals()));
        row(graphics, cardX, cardWidth, rowY + 20, "Mastered recipes",
                JournalText.number(method.masteredMeals()));

        if (method.mostPrepared() != null) {
            JournalTheme.value(graphics, font(), JournalText.nameOf(method.mostPrepared()),
                    cardX + cardWidth - 6, cardY + 4, JournalTheme.TEXT_FAINT);
        }
    }

    private void row(GuiGraphics graphics, int cardX, int cardWidth, int rowY, String label,
                     String value) {
        JournalTheme.row(graphics, font(), Component.literal(label), Component.literal(value),
                cardX + 6, cardX + cardWidth - 6, rowY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return list.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public Component emptyMessage() {
        return Component.translatable("mealmastery.empty.recipes");
    }
}
