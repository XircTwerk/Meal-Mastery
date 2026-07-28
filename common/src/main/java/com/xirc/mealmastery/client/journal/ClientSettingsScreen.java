package com.xirc.mealmastery.client.journal;

import com.xirc.mealmastery.compat.CompatibilityReport;
import com.xirc.mealmastery.config.ClientConfig;
import com.xirc.mealmastery.config.ConfigManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * The client settings screen.
 *
 * <p>Styled like the journal rather than like a generic grey options menu, and
 * built from vanilla buttons plus the same code-drawn panels — nothing new is
 * added to the resource pack.</p>
 *
 * <p>Only client preferences appear here. Server settings are deliberately
 * absent: a client cannot change progression, and offering a control that
 * silently does nothing would be worse than not offering it.</p>
 */
public final class ClientSettingsScreen extends Screen {

    private record Toggle(Component label, BooleanSupplier getter, Consumer<Boolean> setter) {
    }

    private final Screen parent;
    private final List<Toggle> toggles = new ArrayList<>();
    private int frameX;
    private int frameY;
    private int frameWidth;
    private int frameHeight;

    public ClientSettingsScreen(Screen parent) {
        super(Component.translatable("mealmastery.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        ClientConfig config = ConfigManager.client();
        toggles.clear();

        frameWidth = Math.max(280, Math.min(420, width - 40));
        frameHeight = Math.max(200, Math.min(280, height - 40));
        frameX = (width - frameWidth) / 2;
        frameY = (height - frameHeight) / 2;

        toggles.add(new Toggle(Component.literal("HUD enabled"),
                () -> config.hud.enabled, value -> config.hud.enabled = value));
        toggles.add(new Toggle(Component.literal("HUD only while active"),
                () -> config.hud.onlyWhileActive, value -> config.hud.onlyWhileActive = value));
        toggles.add(new Toggle(Component.literal("Show pinned recipes"),
                () -> config.hud.showPinnedRecipes, value -> config.hud.showPinnedRecipes = value));
        toggles.add(new Toggle(Component.literal("Discovery notifications"),
                () -> config.notifications.discoveries,
                value -> config.notifications.discoveries = value));
        toggles.add(new Toggle(Component.literal("Mastery notifications"),
                () -> config.notifications.masteryRanks,
                value -> config.notifications.masteryRanks = value));
        toggles.add(new Toggle(Component.literal("Level-up notifications"),
                () -> config.notifications.levelUps, value -> config.notifications.levelUps = value));
        toggles.add(new Toggle(Component.literal("XP popups"),
                () -> config.notifications.xpPopups, value -> config.notifications.xpPopups = value));
        toggles.add(new Toggle(Component.literal("Notification sounds"),
                () -> config.notifications.playSounds,
                value -> config.notifications.playSounds = value));
        toggles.add(new Toggle(Component.literal("Item tooltips"),
                () -> config.tooltips.enabled, value -> config.tooltips.enabled = value));
        toggles.add(new Toggle(Component.literal("Tooltips need modifier"),
                () -> config.tooltips.requireModifier,
                value -> config.tooltips.requireModifier = value));
        toggles.add(new Toggle(Component.literal("Show nutrition in tooltips"),
                () -> config.tooltips.showNutrition, value -> config.tooltips.showNutrition = value));
        toggles.add(new Toggle(Component.literal("Cozy mode"),
                () -> config.journal.cozyMode, value -> config.journal.cozyMode = value));
        toggles.add(new Toggle(Component.literal("Completionist mode"),
                () -> config.journal.completionistMode,
                value -> config.journal.completionistMode = value));
        toggles.add(new Toggle(Component.literal("Reduced motion"),
                () -> config.accessibility.reducedMotion,
                value -> config.accessibility.reducedMotion = value));

        int columns = frameWidth >= 360 ? 2 : 1;
        int buttonWidth = (frameWidth - 24 - (columns - 1) * 8) / columns;
        int rows = (toggles.size() + columns - 1) / columns;

        for (int index = 0; index < toggles.size(); index++) {
            Toggle toggle = toggles.get(index);
            int column = index / rows;
            int row = index % rows;
            int buttonX = frameX + 12 + column * (buttonWidth + 8);
            int buttonY = frameY + 28 + row * 15;
            addRenderableWidget(Button.builder(labelFor(toggle),
                            button -> {
                                toggle.setter().accept(!toggle.getter().getAsBoolean());
                                button.setMessage(labelFor(toggle));
                                ConfigManager.saveClient(ConfigManager.client());
                            })
                    .bounds(buttonX, buttonY, buttonWidth, 13)
                    .build());
        }

        // Three-state, so it gets its own button rather than a Toggle.
        addRenderableWidget(Button.builder(sidePanelLabel(config),
                        button -> {
                            ClientConfig.SidePanel[] modes = ClientConfig.SidePanel.values();
                            config.journal.sidePanel = modes[
                                    (config.journal.sidePanel.ordinal() + 1) % modes.length];
                            button.setMessage(sidePanelLabel(config));
                            ConfigManager.saveClient(ConfigManager.client());
                        })
                .bounds(frameX + 12, frameY + frameHeight - 40, frameWidth - 24, 13)
                .build());

        addRenderableWidget(Button.builder(Component.literal("Done"),
                        button -> onClose())
                .bounds(frameX + frameWidth / 2 - 40, frameY + frameHeight - 22, 80, 16)
                .build());
    }

    private static Component sidePanelLabel(ClientConfig config) {
        return Component.translatable("mealmastery.config.side_panel")
                .copy().append(": ").append(Component.literal(
                        config.journal.sidePanel.name().toLowerCase(java.util.Locale.ROOT)));
    }

    private static Component labelFor(Toggle toggle) {
        return toggle.label().copy()
                .append(": ")
                .append(Component.literal(toggle.getter().getAsBoolean() ? "on" : "off"));
    }

    /**
     * Draws no backdrop at all: no dim, and no blur on the versions that have
     * one.
     *
     * <p>{@code Screen#render} calls this hook itself, so leaving it empty is
     * what suppresses the default — changing what {@code render} calls is not
     * enough, because {@code super.render} reaches this anyway. The journal
     * draws its own panel, and dimming the world behind it only made the
     * widgets look wrong by comparison: they render afterwards, so they stayed
     * at full brightness while everything else went dark.</p>
     */
    @Override
    public void renderBackground(GuiGraphics graphics) {
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        JournalTheme.panel(graphics, frameX, frameY, frameWidth, frameHeight);
        graphics.drawString(font, title, frameX + 12, frameY + 10,
                JournalTheme.TEXT_ACCENT, false);
        JournalTheme.divider(graphics, frameX + 1, frameY + 23, frameWidth - 2);

        renderCompatibility(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    /** The compatibility list lives at the bottom of this screen. */
    private void renderCompatibility(GuiGraphics graphics) {
        List<CompatibilityReport.Line> lines = CompatibilityReport.build();
        int cursorY = frameY + frameHeight - 46;
        int listTop = frameY + 28 + ((toggles.size() + 1) / 2) * 15 + 6;
        if (listTop >= cursorY) {
            return;
        }
        graphics.drawString(font, Component.translatable("mealmastery.config.category.compatibility"),
                frameX + 12, listTop, JournalTheme.TEXT_ACCENT, false);
        listTop += 11;

        for (CompatibilityReport.Line line : lines) {
            if (listTop + 10 > cursorY) {
                break;
            }
            graphics.drawString(font, Component.literal(line.displayName()),
                    frameX + 12, listTop, JournalTheme.TEXT_DIM, false);
            JournalTheme.value(graphics, font,
                    Component.translatable(line.status().translationKey()),
                    frameX + frameWidth - 12, listTop, JournalTheme.TEXT_FAINT);
            listTop += 10;
        }
    }

    @Override
    public void onClose() {
        ConfigManager.saveClient(ConfigManager.client());
        minecraft.setScreen(parent);
    }
}
