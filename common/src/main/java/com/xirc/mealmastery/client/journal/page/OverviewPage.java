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
 * The landing page.
 *
 * <p>Two columns: progress on the left, what you have actually been doing on
 * the right. The design warns against forty numbers at once, but an
 * overview that is mostly empty space is the opposite failure — so this shows
 * headline figures, completion, records and the recent-activity feed, and
 * leaves the exhaustive lists to their own tabs.</p>
 */
public final class OverviewPage extends BasePage {

    private static final int COLUMN_GAP = 12;

    @Override
    public String id() {
        return "overview";
    }

    @Override
    public Component title() {
        return Component.translatable("mealmastery.page.overview");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Two columns when there is room, one when the window is narrow.
        boolean split = width >= 250;
        int columnWidth = split ? (width - COLUMN_GAP) / 2 : width;
        int rightX = x + columnWidth + COLUMN_GAP;

        renderProgress(graphics, x, columnWidth);
        if (split) {
            renderActivityColumn(graphics, rightX, width - columnWidth - COLUMN_GAP);
        }
    }

    // ------------------------------------------------------------ left column

    private void renderProgress(GuiGraphics graphics, int columnX, int columnWidth) {
        int right = columnX + columnWidth;
        int cursorY = y;

        graphics.drawString(font(), Component.translatable("mealmastery.overview.profile"),
                columnX, cursorY, JournalTheme.TEXT_ACCENT, false);
        JournalTheme.divider(graphics, columnX, cursorY + 10, columnWidth);
        cursorY += 16;

        cursorY = renderLevel(graphics, columnX, right, cursorY);
        cursorY += 6;

        int total = ClientJournalState.dishCount();
        int discovered = ClientJournalState.discoveredCount();
        int mastered = ClientJournalState.masteredCount();

        cursorY = row(graphics, columnX, right, cursorY, "mealmastery.overview.discovered",
                JournalText.fraction(discovered, total));
        cursorY = row(graphics, columnX, right, cursorY, "mealmastery.overview.mastered",
                JournalText.fraction(mastered, total));
        cursorY = row(graphics, columnX, right, cursorY, "mealmastery.overview.prepared",
                JournalText.number(ClientJournalState.mealsPrepared()));
        cursorY = row(graphics, columnX, right, cursorY, "mealmastery.overview.eaten",
                JournalText.number(ClientJournalState.mealsEaten()));
        if (ClientJournalState.portionsServed() > 0L) {
            cursorY = row(graphics, columnX, right, cursorY, "mealmastery.overview.served",
                    JournalText.number(ClientJournalState.portionsServed()));
        }
        cursorY = row(graphics, columnX, right, cursorY, "mealmastery.overview.ingredients",
                JournalText.fraction(ClientJournalState.ingredientsDiscovered(),
                        ClientJournalState.totalIngredients()));

        cursorY += 6;
        cursorY = renderCompletion(graphics, columnX, right, cursorY, discovered, mastered, total);
        cursorY += 6;
        renderStreaks(graphics, columnX, right, cursorY);
    }

    private int renderLevel(GuiGraphics graphics, int columnX, int right, int cursorY) {
        graphics.drawString(font(), Component.translatable("mealmastery.overview.level")
                        .append(" " + ClientJournalState.cookingLevel()),
                columnX, cursorY, JournalTheme.TEXT, false);

        long into = ClientJournalState.xpIntoLevel();
        long needed = ClientJournalState.xpForCurrentLevel();
        if (!JournalText.cozy()) {
            Component xp = Component.literal(needed < 0
                    ? JournalText.number(into) + " XP"
                    : JournalText.number(into) + " / " + JournalText.number(needed));
            JournalTheme.value(graphics, font(), xp, right, cursorY, JournalTheme.TEXT_DIM);
        }
        cursorY += 11;
        JournalTheme.bar(graphics, columnX, cursorY, right - columnX, 6,
                needed <= 0 ? 1.0F : (float) into / needed, needed < 0);
        return cursorY + 8;
    }

    /**
     * Discovery and mastery are shown as two labelled bars rather than averaged
     * into one figure, because they mean different things.
     */
    private int renderCompletion(GuiGraphics graphics, int columnX, int right, int cursorY,
                                 int discovered, int mastered, int total) {
        if (total <= 0) {
            return cursorY;
        }
        graphics.drawString(font(), Component.translatable("mealmastery.overview.completion"),
                columnX, cursorY, JournalTheme.TEXT_ACCENT, false);
        cursorY += 12;

        cursorY = labelledBar(graphics, columnX, right, cursorY,
                Component.translatable("mealmastery.overview.discovered"),
                (float) discovered / total);
        cursorY = labelledBar(graphics, columnX, right, cursorY,
                Component.translatable("mealmastery.overview.mastered"),
                (float) mastered / total);
        return cursorY;
    }

    private int labelledBar(GuiGraphics graphics, int columnX, int right, int cursorY,
                            Component label, float fraction) {
        graphics.drawString(font(), label, columnX, cursorY, JournalTheme.TEXT_DIM, false);
        if (!JournalText.cozy()) {
            JournalTheme.value(graphics, font(), Component.literal(JournalText.percent(fraction)),
                    right, cursorY, JournalTheme.TEXT);
        }
        cursorY += 10;
        JournalTheme.bar(graphics, columnX, cursorY, right - columnX, 5, fraction,
                fraction >= 1.0F);
        return cursorY + 9;
    }

    private void renderStreaks(GuiGraphics graphics, int columnX, int right, int cursorY) {
        if (ClientJournalState.cookingStreak() > 0) {
            cursorY = row(graphics, columnX, right, cursorY, "mealmastery.overview.streak",
                    ClientJournalState.cookingStreak() + " day(s)");
        }
        if (ClientJournalState.varietyStreak() > 1) {
            row(graphics, columnX, right, cursorY, "mealmastery.overview.variety",
                    ClientJournalState.varietyStreak() + " dish(es)");
        }
    }

    // ----------------------------------------------------------- right column

    private void renderActivityColumn(GuiGraphics graphics, int columnX, int columnWidth) {
        int right = columnX + columnWidth;
        int cursorY = y;

        cursorY = renderHighlights(graphics, columnX, right, cursorY);
        cursorY += 6;
        cursorY = renderChallenges(graphics, columnX, right, cursorY);
        cursorY += 6;
        renderActivity(graphics, columnX, right, cursorY);
    }

    private int renderHighlights(GuiGraphics graphics, int columnX, int right, int cursorY) {
        List<Packets.RecordView> records = ClientJournalState.records();
        Packets.RecordView mostPrepared = records.stream()
                .filter(record -> record.prepared() > 0L)
                .max(Comparator.comparingLong(Packets.RecordView::prepared)).orElse(null);
        Packets.RecordView mostEaten = records.stream()
                .filter(record -> record.eaten() > 0L)
                .max(Comparator.comparingLong(Packets.RecordView::eaten)).orElse(null);

        if (mostPrepared == null && mostEaten == null) {
            return cursorY;
        }
        graphics.drawString(font(), Component.literal("Highlights"), columnX, cursorY,
                JournalTheme.TEXT_ACCENT, false);
        JournalTheme.divider(graphics, columnX, cursorY + 10, right - columnX);
        cursorY += 16;

        if (mostPrepared != null) {
            cursorY = dishRow(graphics, columnX, right, cursorY, "Most prepared",
                    mostPrepared.target(), mostPrepared.prepared());
        }
        if (mostEaten != null) {
            cursorY = dishRow(graphics, columnX, right, cursorY, "Most eaten",
                    mostEaten.target(), mostEaten.eaten());
        }
        return cursorY;
    }

    private int renderChallenges(GuiGraphics graphics, int columnX, int right, int cursorY) {
        List<Packets.ChallengeView> open = ClientJournalState.challenges().stream()
                .filter(challenge -> !challenge.completed())
                .limit(3)
                .toList();
        if (open.isEmpty() || cursorY + 24 > bottom()) {
            return cursorY;
        }
        graphics.drawString(font(), Component.translatable("mealmastery.page.challenges"),
                columnX, cursorY, JournalTheme.TEXT_ACCENT, false);
        JournalTheme.divider(graphics, columnX, cursorY + 10, right - columnX);
        cursorY += 16;

        for (Packets.ChallengeView challenge : open) {
            if (cursorY + 10 > bottom()) {
                return cursorY;
            }
            Component name = Component.translatable(challenge.titleKey());
            graphics.drawString(font(), name, columnX, cursorY, JournalTheme.TEXT_DIM, false);
            if (!JournalText.cozy() && challenge.goal() > 0) {
                JournalTheme.value(graphics, font(),
                        Component.literal(challenge.progress() + " / " + challenge.goal()),
                        right, cursorY, JournalTheme.TEXT_FAINT);
            }
            cursorY += 11;
        }
        return cursorY;
    }

    private void renderActivity(GuiGraphics graphics, int columnX, int right, int cursorY) {
        List<Packets.ActivityView> activity = ClientJournalState.activity();
        if (cursorY + 22 > bottom()) {
            return;
        }
        graphics.drawString(font(), Component.translatable("mealmastery.overview.recent"),
                columnX, cursorY, JournalTheme.TEXT_ACCENT, false);
        JournalTheme.divider(graphics, columnX, cursorY + 10, right - columnX);
        cursorY += 16;

        if (activity.isEmpty()) {
            for (var line : font().split(
                    Component.translatable("mealmastery.empty.activity"), right - columnX)) {
                graphics.drawString(font(), line, columnX, cursorY, JournalTheme.TEXT_FAINT, false);
                cursorY += 10;
            }
            return;
        }

        for (Packets.ActivityView entry : activity) {
            if (cursorY + 10 > bottom()) {
                return;
            }
            Component subject = entry.subject() == null
                    ? Component.empty() : JournalText.nameOf(entry.subject());
            Component line = Component.translatable(
                    "mealmastery.activity." + entry.type(), subject);
            for (var wrapped : font().split(line, right - columnX)) {
                if (cursorY + 10 > bottom()) {
                    return;
                }
                graphics.drawString(font(), wrapped, columnX, cursorY,
                        JournalTheme.TEXT_DIM, false);
                cursorY += 10;
                break;
            }
        }
    }

    // ----------------------------------------------------------------- pieces

    private int dishRow(GuiGraphics graphics, int columnX, int right, int cursorY, String label,
                        ResourceLocation dish, long value) {
        if (cursorY + 16 > bottom()) {
            return cursorY;
        }
        graphics.drawString(font(), Component.literal(label), columnX, cursorY,
                JournalTheme.TEXT_DIM, false);
        cursorY += 11;
        graphics.renderFakeItem(JournalText.stackOf(dish), columnX, cursorY - 3);
        graphics.drawString(font(), JournalText.nameOf(dish), columnX + 20, cursorY,
                JournalTheme.TEXT, false);
        if (!JournalText.cozy()) {
            JournalTheme.value(graphics, font(), Component.literal(JournalText.number(value)),
                    right, cursorY, JournalTheme.TEXT_FAINT);
        }
        return cursorY + 15;
    }

    private int row(GuiGraphics graphics, int columnX, int right, int cursorY, String labelKey,
                    String value) {
        if (cursorY + 10 > bottom()) {
            return cursorY;
        }
        JournalTheme.row(graphics, font(), Component.translatable(labelKey),
                Component.literal(value), columnX, right, cursorY);
        return cursorY + 11;
    }

    @Override
    public Component emptyMessage() {
        return Component.translatable("mealmastery.empty.activity");
    }
}
