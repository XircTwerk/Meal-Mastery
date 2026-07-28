package com.xirc.mealmastery.neoforge;

import com.mojang.blaze3d.platform.InputConstants;
import com.xirc.mealmastery.Constants;
import com.xirc.mealmastery.client.MealMasteryClient;
import com.xirc.mealmastery.client.hud.MealMasteryHud;
import com.xirc.mealmastery.client.journal.StatsSidePanel;
import com.xirc.mealmastery.client.tooltip.MasteryTooltip;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.lwjgl.glfw.GLFW;

/**
 * NeoForge's client wiring: keybind, HUD layer, tooltips and the tick pump.
 *
 * <p>Split across the two buses — mod-bus registrations in the nested class,
 * game events out here — and confined to {@link Dist#CLIENT} so a dedicated
 * server never touches any of it.</p>
 */
@EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public final class NeoForgeClientBridge {

    static KeyMapping openJournal;

    private NeoForgeClientBridge() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        MealMasteryClient.tick();
        if (openJournal != null) {
            while (openJournal.consumeClick()) {
                MealMasteryClient.openJournal();
            }
        }
    }

    /** Docks the stats panel beside any container screen. */
    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            StatsSidePanel.render(event.getGuiGraphics(),
                    screen.getGuiLeft(), screen.getGuiTop(), screen.getXSize(),
                    screen.width, screen.height, screen.getMenu());
        }
    }

    @SubscribeEvent
    public static void onItemTooltip(ItemTooltipEvent event) {
        MasteryTooltip.append(event.getItemStack(), event.getToolTip());
    }

    @SubscribeEvent
    public static void onLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        MealMasteryClient.onDisconnect();
    }

    @EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT,
            bus = EventBusSubscriber.Bus.MOD)
    public static final class ModBus {

        private ModBus() {
        }

        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            // Reads the client config; the server config is loaded separately
            // when a world starts.
            event.enqueueWork(MealMasteryClient::initialize);
        }

        @SubscribeEvent
        public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
            // No journal item is needed; the keybind is the whole entry point.
            openJournal = new KeyMapping(
                    "mealmastery.key.open_journal",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_J,
                    "mealmastery.key.category");
            event.register(openJournal);
        }

        @SubscribeEvent
        public static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
            // 1.21 replaced GUI overlays with layers.
            event.registerAbove(VanillaGuiLayers.HOTBAR,
                    ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, "hud"),
                    (graphics, deltaTracker) -> MealMasteryHud.render(graphics));
        }
    }
}
