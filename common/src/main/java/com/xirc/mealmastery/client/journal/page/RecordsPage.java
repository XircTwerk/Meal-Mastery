package com.xirc.mealmastery.client.journal.page;

import com.xirc.mealmastery.client.ClientJournalState;
import com.xirc.mealmastery.client.journal.JournalText;
import com.xirc.mealmastery.client.journal.JournalTheme;
import com.xirc.mealmastery.network.Packets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Comparator;
import java.util.List;

/**
 * Personal records, badges and milestones.
 *
 * <p>Everything here is derived from what the server already sent, so the page
 * costs no extra packet.</p>
 */
public final class RecordsPage extends BasePage {

    @Override
    public String id() {
        return "records";
    }

    @Override
    public Component title() {
        return Component.translatable("mealmastery.page.records");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        int cursorY = y;
        sectionTitle(graphics, Component.translatable("mealmastery.page.records"), cursorY);
        cursorY += 16;

        List<Packets.RecordView> records = ClientJournalState.records();
        Packets.RecordView mostPrepared = records.stream()
                .max(Comparator.comparingLong(Packets.RecordView::prepared)).orElse(null);
        Packets.RecordView mostEaten = records.stream()
                .max(Comparator.comparingLong(Packets.RecordView::eaten)).orElse(null);

        if (mostPrepared != null && mostPrepared.prepared() > 0L) {
            cursorY = dishRow(graphics, cursorY, "Most prepared", mostPrepared.target(),
                    mostPrepared.prepared());
        }
        if (mostEaten != null && mostEaten.eaten() > 0L) {
            cursorY = dishRow(graphics, cursorY, "Most eaten", mostEaten.target(),
                    mostEaten.eaten());
        }
        cursorY = row(graphics, cursorY, "Cooking streak",
                ClientJournalState.cookingStreak() + " day(s)");
        cursorY = row(graphics, cursorY, "Variety streak",
                ClientJournalState.varietyStreak() + " dish(es)");

        if (records.isEmpty()) {
            renderEmpty(graphics);
            return;
        }

        cursorY += 8;
        if (cursorY + 24 < bottom() && !ClientJournalState.milestones().isEmpty()) {
            sectionTitle(graphics, Component.literal("Milestones"), cursorY);
            cursorY += 14;
            for (ResourceLocation milestone : ClientJournalState.milestones()) {
                if (cursorY + 11 > bottom()) {
                    return;
                }
                graphics.drawString(font(), Component.translatable("mealmastery.milestone."
                                + milestone.getNamespace() + "." + milestone.getPath()),
                        x, cursorY, JournalTheme.TEXT_GOOD, false);
                cursorY += 11;
            }
        }
    }

    private int dishRow(GuiGraphics graphics, int cursorY, String label, ResourceLocation dish,
                        long value) {
        if (cursorY + 14 > bottom()) {
            return cursorY;
        }
        graphics.drawString(font(), Component.literal(label), x, cursorY,
                JournalTheme.TEXT_DIM, false);
        graphics.renderFakeItem(JournalText.stackOf(dish), right() - 90, cursorY - 4);
        Component name = JournalText.nameOf(dish)
                .copy().append(JournalText.cozy() ? "" : "  " + JournalText.number(value));
        JournalTheme.value(graphics, font(), name, right(), cursorY, JournalTheme.TEXT);
        return cursorY + 14;
    }

    private int row(GuiGraphics graphics, int cursorY, String label, String value) {
        if (cursorY + 10 > bottom()) {
            return cursorY;
        }
        JournalTheme.row(graphics, font(), Component.literal(label), Component.literal(value),
                x, right(), cursorY);
        return cursorY + 11;
    }

    @Override
    public Component emptyMessage() {
        return Component.translatable("mealmastery.empty.mastery");
    }
}
