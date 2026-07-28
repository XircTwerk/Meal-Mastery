package com.xirc.mealmastery.client.journal.page;

import com.xirc.mealmastery.client.ClientJournalState;
import com.xirc.mealmastery.client.journal.JournalText;
import com.xirc.mealmastery.client.journal.JournalTheme;
import com.xirc.mealmastery.client.journal.ScrollList;
import com.xirc.mealmastery.network.Packets;
import com.xirc.mealmastery.recipe.MealSearch;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The ingredient journal.
 *
 * <p>Only lists ingredients the player has genuinely cooked with — opening a
 * recipe viewer does not put anything here.</p>
 */
public final class IngredientsPage extends BasePage {

    private final ScrollList<Packets.IngredientView> list =
            new ScrollList<>(22, 2, this::renderRow);
    private String query = "";

    @Override
    public String id() {
        return "ingredients";
    }

    @Override
    public Component title() {
        return Component.translatable("mealmastery.page.ingredients");
    }

    @Override
    public boolean usesSearch() {
        return true;
    }

    @Override
    protected void onLayout() {
        list.bounds(x, y + 14, width, height - 14);
        rebuild();
    }

    @Override
    public void onSearchChanged(String query) {
        this.query = query == null ? "" : query;
        rebuild();
        list.scrollToTop();
    }

    private void rebuild() {
        List<Packets.IngredientView> visible = new ArrayList<>();
        for (Packets.IngredientView ingredient : ClientJournalState.ingredients()) {
            if (!query.isBlank() && !JournalText.nameOf(ingredient.item()).getString()
                    .toLowerCase(java.util.Locale.ROOT)
                    .contains(query.toLowerCase(java.util.Locale.ROOT))
                    && !ingredient.item().toString().contains(query.toLowerCase(java.util.Locale.ROOT))) {
                continue;
            }
            visible.add(ingredient);
        }
        visible.sort(Comparator.comparingLong(Packets.IngredientView::timesUsed).reversed());
        list.items(visible);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        Component header = Component.literal(JournalText.fraction(
                ClientJournalState.ingredientsDiscovered(), ClientJournalState.totalIngredients()));
        graphics.drawString(font(), Component.translatable("mealmastery.overview.ingredients"),
                x, y + 2, JournalTheme.TEXT_ACCENT, false);
        JournalTheme.value(graphics, font(), header, right(), y + 2, JournalTheme.TEXT);

        if (list.isEmpty()) {
            renderEmpty(graphics);
        } else {
            list.render(graphics, mouseX, mouseY);
        }
    }

    private void renderRow(GuiGraphics graphics, Packets.IngredientView ingredient, int rowX,
                           int rowY, int rowWidth, int rowHeight, boolean hovered) {
        JournalTheme.card(graphics, rowX, rowY, rowWidth, rowHeight, hovered, false);
        graphics.renderFakeItem(JournalText.stackOf(ingredient.item()), rowX + 3, rowY + 3);
        graphics.drawString(font(), JournalText.nameOf(ingredient.item()), rowX + 23, rowY + 3,
                JournalTheme.TEXT, false);

        Component used = Component.literal("used in " + ingredient.mealCount() + " dish(es)");
        graphics.drawString(font(), used, rowX + 23, rowY + 13, JournalTheme.TEXT_FAINT, false);

        if (!JournalText.cozy()) {
            JournalTheme.value(graphics, font(),
                    Component.literal(JournalText.number(ingredient.timesUsed())),
                    rowX + rowWidth - 4, rowY + 3, JournalTheme.TEXT_DIM);
        }
        if (ingredient.firstUsedDay() != Long.MIN_VALUE) {
            JournalTheme.value(graphics, font(),
                    Component.literal("day " + ingredient.firstUsedDay()),
                    rowX + rowWidth - 4, rowY + 13, JournalTheme.TEXT_FAINT);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return list.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public Component emptyMessage() {
        return query.isBlank()
                ? Component.translatable("mealmastery.empty.ingredients")
                : Component.translatable("mealmastery.empty.search");
    }
}
