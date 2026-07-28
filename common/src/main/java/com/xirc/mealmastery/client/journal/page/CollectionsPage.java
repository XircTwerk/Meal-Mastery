package com.xirc.mealmastery.client.journal.page;

import com.xirc.mealmastery.client.ClientJournalState;
import com.xirc.mealmastery.client.journal.JournalText;
import com.xirc.mealmastery.client.journal.JournalTheme;
import com.xirc.mealmastery.client.journal.ScrollList;
import com.xirc.mealmastery.network.Packets;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Themed and per-mod collections.
 *
 * <p>The per-mod entries are what make installing another food expansion
 * visibly expand the journal, and they are generated rather than authored, so
 * their titles arrive as display text and are not translated again.</p>
 */
public final class CollectionsPage extends BasePage {

    private final ScrollList<Packets.CollectionView> list = new ScrollList<>(30, 3, this::renderCard);

    @Override
    public String id() {
        return "collections";
    }

    @Override
    public Component title() {
        return Component.translatable("mealmastery.page.collections");
    }

    @Override
    protected void onLayout() {
        list.bounds(x, y, width, height);
        list.items(new ArrayList<>(ClientJournalState.collections()));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        List<Packets.CollectionView> collections = ClientJournalState.collections();
        if (collections.size() != list.items().size()) {
            list.items(new ArrayList<>(collections));
        }
        if (list.isEmpty()) {
            renderEmpty(graphics);
        } else {
            list.render(graphics, mouseX, mouseY);
        }
    }

    private void renderCard(GuiGraphics graphics, Packets.CollectionView collection, int cardX,
                            int cardY, int cardWidth, int cardHeight, boolean hovered) {
        boolean complete = collection.total() > 0
                && collection.discovered() == collection.total();
        JournalTheme.card(graphics, cardX, cardY, cardWidth, cardHeight, hovered, complete);

        if (collection.icon() != null) {
            graphics.renderFakeItem(JournalText.stackOf(collection.icon()), cardX + 4, cardY + 4);
        }
        Component title = collection.literalTitle()
                ? Component.literal(collection.titleKey())
                : Component.translatable(collection.titleKey());
        graphics.drawString(font(), title, cardX + 24, cardY + 4,
                complete ? JournalTheme.TEXT_GOOD : JournalTheme.TEXT, false);

        if (!JournalText.cozy()) {
            JournalTheme.value(graphics, font(), Component.literal(
                            JournalText.fraction(collection.discovered(), collection.total())),
                    cardX + cardWidth - 6, cardY + 4, JournalTheme.TEXT_DIM);
        }
        float fraction = collection.total() == 0 ? 0.0F
                : (float) collection.discovered() / collection.total();
        JournalTheme.bar(graphics, cardX + 24, cardY + 16, cardWidth - 30, 5, fraction, complete);

        if (collection.mastered() > 0 && !JournalText.cozy()) {
            JournalTheme.value(graphics, font(),
                    Component.literal(collection.mastered() + " mastered"),
                    cardX + cardWidth - 6, cardY + 22, JournalTheme.TEXT_FAINT);
        }
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
