package com.xirc.mealmastery.forge;

import com.mojang.blaze3d.platform.InputConstants;
import com.xirc.mealmastery.Constants;
import com.xirc.mealmastery.client.MealMasteryClient;
import com.xirc.mealmastery.client.hud.MealMasteryHud;
import com.xirc.mealmastery.client.journal.StatsSidePanel;
import com.xirc.mealmastery.client.tooltip.MasteryTooltip;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * Forge's client wiring: keybind, HUD overlay, tooltips and the tick pump.
 *
 * <p>Split across the two Forge buses — mod-bus registrations in the nested
 * class, game events out here — and confined to {@link Dist#CLIENT} so a
 * dedicated server never touches any of it.</p>
 */
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT)
public final class ForgeClientBridge {

    static KeyMapping openJournal;

    private ForgeClientBridge() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
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

    @Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.MOD)
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
        public static void onRegisterOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "mealmastery_hud",
                    (gui, graphics, partialTick, width, height) -> MealMasteryHud.render(graphics));
        }
    }
}
