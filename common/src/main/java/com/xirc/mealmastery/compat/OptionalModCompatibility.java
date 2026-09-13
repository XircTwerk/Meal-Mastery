package com.xirc.mealmastery.compat;

import com.xirc.mealmastery.platform.Services;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Small, dependency-free checks for optional integrations. */
public final class OptionalModCompatibility {

    public static final String SPICE_OF_LIFE_VALHEIM = "sol_valheim_reforged";
    public static final String SOPHISTICATED_BACKPACKS = "sophisticatedbackpacks";
    public static final String SOPHISTICATED_STORAGE = "sophisticatedstorage";

    private static final String SOPHISTICATED_PACKAGE = "net.p3pp3rf1y.sophisticated";

    private OptionalModCompatibility() {
    }

    /** Spice of Life replaces vanilla nutrition and saturation with its own food model. */
    public static boolean spiceOfLifeOwnsFoodStats() {
        return Services.PLATFORM.isModLoaded(SPICE_OF_LIFE_VALHEIM);
    }

    /**
     * Sophisticated Backpacks and Storage share menu code from Sophisticated Core.
     * Checking the package keeps the integration optional and avoids a hard dependency.
     */
    public static boolean isSophisticatedMenu(AbstractContainerMenu menu) {
        return menu != null && isSophisticatedClassName(menu.getClass().getName());
    }

    static boolean isSophisticatedClassName(String className) {
        return className != null && className.startsWith(SOPHISTICATED_PACKAGE);
    }
}
