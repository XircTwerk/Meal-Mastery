package com.xirc.mealmastery.client.journal.page;

import com.xirc.mealmastery.client.ClientJournalState;
import com.xirc.mealmastery.client.journal.JournalText;
import com.xirc.mealmastery.client.journal.JournalTheme;
import com.xirc.mealmastery.network.Packets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.Comparator;
import java.util.List;

/**
 * Chef statistics and the most-efficient-meals list.
 *
 * <p>The nutrition list is presented as "highest nutrition", never as "best":
 * effects and modded mechanics make that comparison ambiguous and the
 * design says not to claim it.</p>
 */
public final class StatisticsPage extends BasePage {

    @Override
    public String id() {
        return "statistics";
    }

    @Override
    public Component title() {
        return Component.translatable("mealmastery.page.statistics");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int cursorY = y;
        sectionTitle(graphics, Component.translatable("mealmastery.page.statistics"), cursorY);
        cursorY += 16;

        cursorY = row(graphics, cursorY, "Meals prepared", ClientJournalState.mealsPrepared());
        cursorY = row(graphics, cursorY, "Meals personally eaten", ClientJournalState.mealsEaten());
        cursorY = row(graphics, cursorY, "Portions served", ClientJournalState.portionsServed());
        cursorY = row(graphics, cursorY, "Unique recipes", ClientJournalState.discoveredCount());
        cursorY = row(graphics, cursorY, "Mastered recipes", ClientJournalState.masteredCount());
        cursorY = row(graphics, cursorY, "Ingredients used",
                ClientJournalState.ingredientsDiscovered());

        for (Packets.MethodView method : ClientJournalState.methods()) {
            if (cursorY + 10 > bottom()) {
                return;
            }
            JournalTheme.row(graphics, font(), JournalText.method(method.method()),
                    Component.literal(JournalText.number(method.preparations())),
                    x, right(), cursorY);
            cursorY += 11;
        }

        cursorY += 6;
        if (cursorY + 24 < bottom()) {
            renderNutritionList(graphics, cursorY);
        }
    }

    private void renderNutritionList(GuiGraphics graphics, int cursorY) {
        sectionTitle(graphics, Component.literal("Highest nutrition, discovered"), cursorY);
        cursorY += 14;

        List<Packets.DishView> best = ClientJournalState.dishes().stream()
                .filter(dish -> ClientJournalState.isDiscovered(dish.target()))
                .sorted(Comparator.comparingInt(Packets.DishView::nutrition).reversed())
                .limit(6)
                .toList();

        for (Packets.DishView dish : best) {
            if (cursorY + 12 > bottom()) {
                return;
            }
            graphics.renderFakeItem(JournalText.stackOf(dish.target()), x, cursorY - 2);
            graphics.drawString(font(), JournalText.nameOf(dish.target()), x + 20, cursorY,
                    JournalTheme.TEXT, false);
            JournalTheme.value(graphics, font(),
                    Component.literal(dish.nutrition() + " / " + String.format("%.1f", dish.saturation())),
                    right(), cursorY, JournalTheme.TEXT_DIM);
            cursorY += 14;
        }
    }

    private int row(GuiGraphics graphics, int cursorY, String label, long value) {
        if (cursorY + 10 > bottom()) {
            return cursorY;
        }
        JournalTheme.row(graphics, font(), Component.literal(label),
                Component.literal(JournalText.number(value)), x, right(), cursorY);
        return cursorY + 11;
    }

    @Override
    public Component emptyMessage() {
        return Component.translatable("mealmastery.empty.activity");
    }
}
