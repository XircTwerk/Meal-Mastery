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
 * Challenge progress.
 *
 * <p>Incomplete challenges sort first so the page opens on what there is left
 * to do rather than on a wall of ticks.</p>
 */
public final class ChallengesPage extends BasePage {

    private final ScrollList<Packets.ChallengeView> list = new ScrollList<>(32, 3, this::renderCard);

    @Override
    public String id() {
        return "challenges";
    }

    @Override
    public Component title() {
        return Component.translatable("mealmastery.page.challenges");
    }

    @Override
    protected void onLayout() {
        list.bounds(x, y, width, height);
        refresh();
    }

    private void refresh() {
        List<Packets.ChallengeView> challenges = new ArrayList<>(ClientJournalState.challenges());
        challenges.sort(Comparator.comparing(Packets.ChallengeView::completed));
        list.items(challenges);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (ClientJournalState.challenges().size() != list.items().size()) {
            refresh();
        }
        if (list.isEmpty()) {
            renderEmpty(graphics);
        } else {
            list.render(graphics, mouseX, mouseY);
        }
    }

    private void renderCard(GuiGraphics graphics, Packets.ChallengeView challenge, int cardX,
                            int cardY, int cardWidth, int cardHeight, boolean hovered) {
        JournalTheme.card(graphics, cardX, cardY, cardWidth, cardHeight, hovered,
                challenge.completed());

        Component marker = Component.literal(challenge.completed() ? "✔" : "•");
        graphics.drawString(font(), marker, cardX + 5, cardY + 4,
                challenge.completed() ? JournalTheme.TEXT_GOOD : JournalTheme.TEXT_FAINT, false);
        graphics.drawString(font(), Component.translatable(challenge.titleKey()), cardX + 16,
                cardY + 4, challenge.completed() ? JournalTheme.TEXT_GOOD : JournalTheme.TEXT,
                false);

        Component description = Component.translatable(challenge.descriptionKey());
        List<net.minecraft.util.FormattedCharSequence> lines =
                font().split(description, cardWidth - 22);
        if (!lines.isEmpty()) {
            graphics.drawString(font(), lines.get(0), cardX + 16, cardY + 15,
                    JournalTheme.TEXT_FAINT, false);
        }

        if (!challenge.completed() && challenge.goal() > 0) {
            float fraction = Math.min(1.0F, (float) challenge.progress() / challenge.goal());
            JournalTheme.bar(graphics, cardX + 16, cardY + 25, cardWidth - 22, 4, fraction, false);
            if (!JournalText.cozy()) {
                JournalTheme.value(graphics, font(), Component.literal(
                                challenge.progress() + " / " + challenge.goal()),
                        cardX + cardWidth - 6, cardY + 4, JournalTheme.TEXT_DIM);
            }
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return list.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public Component emptyMessage() {
        return Component.translatable("mealmastery.empty.challenges");
    }
}
