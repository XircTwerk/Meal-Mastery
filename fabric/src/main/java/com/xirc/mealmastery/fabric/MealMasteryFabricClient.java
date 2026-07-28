package com.xirc.mealmastery.fabric;

import com.mojang.blaze3d.platform.InputConstants;
import com.xirc.mealmastery.client.MealMasteryClient;
import com.xirc.mealmastery.client.hud.MealMasteryHud;
import com.xirc.mealmastery.client.tooltip.MasteryTooltip;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import com.xirc.mealmastery.client.journal.StatsSidePanel;
import com.xirc.mealmastery.fabric.mixin.AbstractContainerScreenAccessor;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.lwjgl.glfw.GLFW;

public final class MealMasteryFabricClient implements ClientModInitializer {

    private static KeyMapping openJournal;

    @Override
    public void onInitializeClient() {
        MealMasteryClient.initialize();
        FabricClientNetwork.register();

        // No journal item is needed; the keybind is the whole entry point.
        openJournal = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "mealmastery.key.open_journal",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                "mealmastery.key.category"));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            MealMasteryClient.tick();
            while (openJournal.consumeClick()) {
                MealMasteryClient.openJournal();
            }
        });

        HudRenderCallback.EVENT.register((graphics, tickDelta) -> MealMasteryHud.render(graphics));

        ItemTooltipCallback.EVENT.register((stack, context, type, lines) ->
                MasteryTooltip.append(stack, lines));

        // The stats panel docks beside any container screen, so it is attached
        // as each one is built rather than to a fixed list of screen types.
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> {
            if (!(screen instanceof AbstractContainerScreen<?> container)) {
                return;
            }
            ScreenEvents.afterRender(screen).register((rendered, graphics, mouseX, mouseY, delta) -> {
                AbstractContainerScreenAccessor bounds =
                        (AbstractContainerScreenAccessor) container;
                StatsSidePanel.render(graphics,
                        bounds.mealmastery$leftPos(),
                        bounds.mealmastery$topPos(),
                        bounds.mealmastery$imageWidth(),
                        container.width,
                        container.height,
                        container.getMenu());
            });
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                MealMasteryClient.onDisconnect());
    }
}
