package com.xirc.mealmastery.client.journal.page;

import com.xirc.mealmastery.client.ClientJournalState;
import com.xirc.mealmastery.client.journal.JournalText;
import com.xirc.mealmastery.client.journal.JournalTheme;
import com.xirc.mealmastery.client.journal.ScrollList;
import com.xirc.mealmastery.config.ConfigEnums;
import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.network.Network;
import com.xirc.mealmastery.network.Packets;
import com.xirc.mealmastery.platform.Services;
import com.xirc.mealmastery.recipe.MealSearch;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The recipe browser.
 *
 * <p>Virtualised so a thousand-dish pack scrolls smoothly, filtered and sorted
 * in memory, and searched through the same matcher the rest of the mod uses.
 * Undiscovered dishes are drawn according to the player's chosen display mode
 * — with no custom art available, a silhouette is the item icon drawn
 * dark rather than a bespoke texture.</p>
 */
public final class RecipesPage extends BasePage {

    private enum Filter {
        ALL("all"),
        DISCOVERED("discovered"),
        UNDISCOVERED("undiscovered"),
        MASTERED("mastered"),
        UNMASTERED("unmastered"),
        FAVORITES("favorites"),
        NEVER_COOKED("never_cooked");

        private final String key;

        Filter(String key) {
            this.key = key;
        }

        Component label() {
            return Component.translatable("mealmastery.filter." + key);
        }
    }

    private enum Sort {
        ALPHABETICAL("alphabetical"),
        MOST_COOKED("most_cooked"),
        LEAST_COOKED("least_cooked"),
        MASTERY("mastery"),
        NUTRITION("nutrition"),
        SOURCE("source");

        private final String key;

        Sort(String key) {
            this.key = key;
        }

        Component label() {
            return Component.translatable("mealmastery.sort." + key);
        }
    }

    private final ScrollList<Packets.DishView> list = new ScrollList<>(22, 2, this::renderRow);

    private String query = "";
    private Filter filter = Filter.ALL;
    private Sort sort = Sort.ALPHABETICAL;
    private ResourceLocation selected;

    private int listWidth;
    private int detailX;

    @Override
    public String id() {
        return "recipes";
    }

    @Override
    public Component title() {
        return Component.translatable("mealmastery.page.recipes");
    }

    @Override
    public boolean usesSearch() {
        return true;
    }

    @Override
    protected void onLayout() {
        // The detail panel only earns its space on a wide enough window.
        boolean wide = width >= 260;
        listWidth = wide ? width * 3 / 5 : width;
        detailX = x + listWidth + 8;
        list.bounds(x, y + 14, listWidth, height - 14);
        rebuild();
    }

    @Override
    public void onSearchChanged(String query) {
        this.query = query == null ? "" : query;
        rebuild();
        list.scrollToTop();
    }

    private void rebuild() {
        List<Packets.DishView> visible = new ArrayList<>();
        ConfigEnums.UnknownRecipeDisplay unknownDisplay =
                ConfigManager.client().journal.unknownDisplay;

        for (Packets.DishView dish : ClientJournalState.dishes()) {
            Packets.RecordView record = ClientJournalState.record(dish.target());
            boolean discovered = record != null && record.discovered();

            if (!discovered && unknownDisplay == ConfigEnums.UnknownRecipeDisplay.HIDDEN) {
                continue;
            }
            if (!matchesFilter(dish, record, discovered)) {
                continue;
            }
            if (!query.isBlank() && !MealSearch.matches(index(dish, discovered), query)) {
                continue;
            }
            visible.add(dish);
        }
        visible.sort(comparator());
        list.items(visible);
    }

    private boolean matchesFilter(Packets.DishView dish, Packets.RecordView record,
                                  boolean discovered) {
        return switch (filter) {
            case ALL -> true;
            case DISCOVERED -> discovered;
            case UNDISCOVERED -> !discovered;
            case MASTERED -> ClientJournalState.rank(dish.target()).isMastered();
            case UNMASTERED -> !ClientJournalState.rank(dish.target()).isMastered();
            case FAVORITES -> record != null && record.favorite();
            case NEVER_COOKED -> record == null || record.prepared() == 0L;
        };
    }

    /**
     * Undiscovered dishes are indexed by everything except their name and
     * ingredients, so searching cannot be used to reveal what a server chose to
     * hide.
     */
    private MealSearch.Indexed index(Packets.DishView dish, boolean discovered) {
        String modName = Services.PLATFORM.modDisplayName(dish.sourceModId());
        List<String> methods = dish.methods().stream()
                .map(method -> JournalText.method(method).getString()).toList();
        String category = JournalText.category(dish.category()).getString();

        if (!discovered) {
            return new MealSearch.Indexed("", dish.target().toString(), modName,
                    dish.sourceModId(), methods, category, List.of());
        }
        Packets.SyncDishDetail detail = ClientJournalState.detail(dish.target());
        List<String> ingredients = detail == null ? List.of()
                : detail.ingredients().stream()
                .map(id -> JournalText.nameOf(id).getString()).toList();
        return new MealSearch.Indexed(JournalText.nameOf(dish.target()).getString(),
                dish.target().toString(), modName, dish.sourceModId(), methods, category,
                ingredients);
    }

    private Comparator<Packets.DishView> comparator() {
        Comparator<Packets.DishView> byName = Comparator.comparing(
                dish -> JournalText.nameOf(dish.target()).getString(), String.CASE_INSENSITIVE_ORDER);
        return switch (sort) {
            case ALPHABETICAL -> byName;
            case MOST_COOKED -> Comparator.comparingLong(this::preparedCount).reversed()
                    .thenComparing(byName);
            case LEAST_COOKED -> Comparator.comparingLong(this::preparedCount).thenComparing(byName);
            case MASTERY -> Comparator.comparingLong(this::masteryPoints).reversed()
                    .thenComparing(byName);
            case NUTRITION -> Comparator.comparingInt(Packets.DishView::nutrition).reversed()
                    .thenComparing(byName);
            case SOURCE -> Comparator.comparing(Packets.DishView::sourceModId)
                    .thenComparing(byName);
        };
    }

    private long preparedCount(Packets.DishView dish) {
        Packets.RecordView record = ClientJournalState.record(dish.target());
        return record == null ? 0L : record.prepared();
    }

    private long masteryPoints(Packets.DishView dish) {
        Packets.RecordView record = ClientJournalState.record(dish.target());
        return record == null ? 0L : record.masteryPoints();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderControls(graphics, mouseX, mouseY);
        if (list.isEmpty()) {
            renderEmpty(graphics);
        } else {
            list.render(graphics, mouseX, mouseY);
        }
        if (detailX < right() && selected != null) {
            renderDetail(graphics);
        }
    }

    private void renderControls(GuiGraphics graphics, int mouseX, int mouseY) {
        Component filterLabel = filter.label();
        Component sortLabel = sort.label();

        int filterWidth = font().width(filterLabel) + 10;
        boolean filterHovered = hovered(mouseX, mouseY, x, y, filterWidth, 12);
        JournalTheme.card(graphics, x, y, filterWidth, 12, filterHovered, false);
        graphics.drawString(font(), filterLabel, x + 5, y + 2,
                filterHovered ? JournalTheme.TEXT : JournalTheme.TEXT_DIM, false);

        int sortWidth = font().width(sortLabel) + 10;
        int sortX = x + filterWidth + 4;
        boolean sortHovered = hovered(mouseX, mouseY, sortX, y, sortWidth, 12);
        JournalTheme.card(graphics, sortX, y, sortWidth, 12, sortHovered, false);
        graphics.drawString(font(), sortLabel, sortX + 5, y + 2,
                sortHovered ? JournalTheme.TEXT : JournalTheme.TEXT_DIM, false);

        Component count = Component.literal(JournalText.number(list.items().size()));
        JournalTheme.value(graphics, font(), count, x + listWidth - 2, y + 2,
                JournalTheme.TEXT_FAINT);
    }

    private void renderRow(GuiGraphics graphics, Packets.DishView dish, int rowX, int rowY,
                           int rowWidth, int rowHeight, boolean hovered) {
        boolean discovered = ClientJournalState.isDiscovered(dish.target());
        boolean isSelected = dish.target().equals(selected);
        JournalTheme.card(graphics, rowX, rowY, rowWidth, rowHeight, hovered, isSelected);

        ItemStack stack = JournalText.stackOf(dish.target());
        renderIcon(graphics, stack, rowX + 3, rowY + 3, discovered);

        Component name = displayName(dish, discovered);
        graphics.drawString(font(), name, rowX + 23, rowY + 3,
                discovered ? JournalTheme.TEXT : JournalTheme.TEXT_FAINT, false);

        Component source = Component.literal(
                Services.PLATFORM.modDisplayName(dish.sourceModId()));
        graphics.drawString(font(), source, rowX + 23, rowY + 13, JournalTheme.TEXT_FAINT, false);

        if (discovered) {
            int rank = ClientJournalState.rank(dish.target()).ordinal();
            JournalTheme.pips(graphics, rowX + rowWidth - 6 - 5 * 6, rowY + 4, rank, 5);
            Packets.RecordView record = ClientJournalState.record(dish.target());
            if (record != null && record.prepared() > 0L && !JournalText.cozy()) {
                Component prepared = Component.literal(JournalText.number(record.prepared()));
                JournalTheme.value(graphics, font(), prepared, rowX + rowWidth - 4, rowY + 13,
                        JournalTheme.TEXT_FAINT);
            }
            if (record != null && record.favorite()) {
                graphics.drawString(font(), Component.literal("★"), rowX + rowWidth - 10,
                        rowY + 3, JournalTheme.TEXT_ACCENT, false);
            }
        } else if (ConfigManager.client().journal.showNewIndicator) {
            graphics.drawString(font(), Component.translatable("mealmastery.tooltip.new"),
                    rowX + rowWidth - 26, rowY + 13, JournalTheme.TEXT_ACCENT, false);
        }
    }

    /**
     * Draws the dish icon, dimmed to a silhouette when undiscovered.
     *
     * <p>With no custom art allowed, "silhouette" means the real item icon with
     * a dark overlay on top of it — recognisable as a shape, not as a dish
     *.</p>
     */
    private void renderIcon(GuiGraphics graphics, ItemStack stack, int iconX, int iconY,
                            boolean discovered) {
        if (stack.isEmpty()) {
            return;
        }
        graphics.renderFakeItem(stack, iconX, iconY);
        if (!discovered && ConfigManager.client().journal.unknownDisplay
                == ConfigEnums.UnknownRecipeDisplay.SILHOUETTE) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, 200.0F);
            graphics.fill(iconX, iconY, iconX + 16, iconY + 16, 0xE0140F0A);
            graphics.pose().popPose();
        }
    }

    private Component displayName(Packets.DishView dish, boolean discovered) {
        if (discovered) {
            return JournalText.nameOf(dish.target());
        }
        return switch (ConfigManager.client().journal.unknownDisplay) {
            case NAME_ONLY, INGREDIENT_HINTS, FULL_RECIPE -> JournalText.nameOf(dish.target());
            default -> Component.translatable("mealmastery.tooltip.undiscovered");
        };
    }

    private void renderDetail(GuiGraphics graphics) {
        int detailWidth = right() - detailX;
        Packets.DishView dish = ClientJournalState.dish(selected);
        if (dish == null || detailWidth < 60) {
            return;
        }
        boolean discovered = ClientJournalState.isDiscovered(selected);
        int cursorY = y;

        graphics.renderFakeItem(JournalText.stackOf(selected), detailX, cursorY);
        graphics.drawString(font(), displayName(dish, discovered), detailX + 20, cursorY + 4,
                JournalTheme.TEXT_ACCENT, false);
        cursorY += 22;

        graphics.drawString(font(), Component.literal(
                        Services.PLATFORM.modDisplayName(dish.sourceModId())),
                detailX, cursorY, JournalTheme.TEXT_DIM, false);
        cursorY += 11;
        graphics.drawString(font(), JournalText.category(dish.category()), detailX, cursorY,
                JournalTheme.TEXT_DIM, false);
        cursorY += 13;

        if (!discovered) {
            graphics.drawString(font(), Component.translatable("mealmastery.tooltip.undiscovered"),
                    detailX, cursorY, JournalTheme.TEXT_FAINT, false);
            return;
        }

        Packets.RecordView record = ClientJournalState.record(selected);
        graphics.drawString(font(), JournalText.rankOf(selected), detailX, cursorY,
                JournalTheme.TEXT, false);
        cursorY += 11;
        JournalTheme.bar(graphics, detailX, cursorY, detailWidth, 5,
                JournalText.masteryFraction(selected),
                ClientJournalState.rank(selected).isMastered());
        cursorY += 11;

        if (record != null && !JournalText.cozy()) {
            cursorY = detailRow(graphics, detailX, detailWidth, cursorY, "Prepared", record.prepared());
            cursorY = detailRow(graphics, detailX, detailWidth, cursorY, "Eaten", record.eaten());
            cursorY = detailRow(graphics, detailX, detailWidth, cursorY, "Served", record.served());
            if (record.firstPreparedDay() != Long.MIN_VALUE) {
                JournalTheme.row(graphics, font(), Component.literal("First prepared"),
                        Component.literal("day " + record.firstPreparedDay()),
                        detailX, detailX + detailWidth, cursorY);
                cursorY += 11;
            }
        }

        cursorY += 4;
        for (ResourceLocation method : dish.methods()) {
            if (cursorY + 10 > bottom()) {
                break;
            }
            graphics.drawString(font(), JournalText.method(method), detailX, cursorY,
                    JournalTheme.TEXT_DIM, false);
            cursorY += 11;
        }

        Packets.SyncDishDetail detail = ClientJournalState.detail(selected);
        if (detail != null && !detail.ingredients().isEmpty() && cursorY + 20 <= bottom()) {
            cursorY += 4;
            int iconX = detailX;
            for (ResourceLocation ingredient : detail.ingredients()) {
                if (iconX + 18 > detailX + detailWidth) {
                    iconX = detailX;
                    cursorY += 18;
                }
                if (cursorY + 16 > bottom()) {
                    break;
                }
                graphics.renderFakeItem(JournalText.stackOf(ingredient), iconX, cursorY);
                iconX += 18;
            }
        }
    }

    private int detailRow(GuiGraphics graphics, int rowX, int rowWidth, int cursorY, String label,
                          long value) {
        if (cursorY + 10 > bottom()) {
            return cursorY;
        }
        JournalTheme.row(graphics, font(), Component.literal(label),
                Component.literal(JournalText.number(value)), rowX, rowX + rowWidth, cursorY);
        return cursorY + 11;
    }

    private boolean hovered(int mouseX, int mouseY, int boxX, int boxY, int boxWidth,
                            int boxHeight) {
        return mouseX >= boxX && mouseX < boxX + boxWidth
                && mouseY >= boxY && mouseY < boxY + boxHeight;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        Component filterLabel = filter.label();
        int filterWidth = font().width(filterLabel) + 10;
        if (hovered((int) mouseX, (int) mouseY, x, y, filterWidth, 12)) {
            filter = Filter.values()[(filter.ordinal() + 1) % Filter.values().length];
            rebuild();
            list.scrollToTop();
            return true;
        }
        int sortX = x + filterWidth + 4;
        int sortWidth = font().width(sort.label()) + 10;
        if (hovered((int) mouseX, (int) mouseY, sortX, y, sortWidth, 12)) {
            sort = Sort.values()[(sort.ordinal() + 1) % Sort.values().length];
            rebuild();
            list.scrollToTop();
            return true;
        }

        Packets.DishView clicked = list.itemAt(mouseX, mouseY);
        if (clicked == null) {
            return false;
        }
        if (button == 1) {
            // Right-click favourites. The server decides; the echo updates the UI.
            Network.toServer(new Packets.SetPreference(
                    Packets.SetPreference.Kind.FAVORITE, clicked.target()));
            return true;
        }
        if (button == 2) {
            // Middle-click sets your signature dish. The server rejects it
            // unless the dish is actually mastered.
            Network.toServer(new Packets.SetPreference(
                    Packets.SetPreference.Kind.SIGNATURE, clicked.target()));
            return true;
        }
        selected = clicked.target();
        if (ClientJournalState.detail(selected) == null) {
            Network.toServer(new Packets.RequestDishDetail(selected));
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        return list.mouseScrolled(mouseX, mouseY, delta);
    }

    @Override
    public Component emptyMessage() {
        return query.isBlank()
                ? Component.translatable("mealmastery.empty.recipes")
                : Component.translatable("mealmastery.empty.search");
    }
}
