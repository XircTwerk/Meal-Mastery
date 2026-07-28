package com.xirc.mealmastery.fabric.mixin;

import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Reads the container GUI's box so the stats panel can dock beside it.
 *
 * <p><b>Why:</b> {@code leftPos}, {@code topPos} and {@code imageWidth} are
 * protected on {@code AbstractContainerScreen} and vanilla exposes no getters.
 * NeoForge adds them through its extension interface; Fabric has no equivalent,
 * so this accessor mixin stands in.</p>
 *
 * <p>Read-only: it adds getters and injects no code, so it cannot alter how any
 * screen behaves or conflict with another mod touching the same class.</p>
 */
@Mixin(AbstractContainerScreen.class)
public interface AbstractContainerScreenAccessor {

    @Accessor("leftPos")
    int mealmastery$leftPos();

    @Accessor("topPos")
    int mealmastery$topPos();

    @Accessor("imageWidth")
    int mealmastery$imageWidth();
}
