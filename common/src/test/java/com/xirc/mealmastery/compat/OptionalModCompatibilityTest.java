package com.xirc.mealmastery.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OptionalModCompatibilityTest {

    @Test
    void recognizesAllSophisticatedMenuPackagesWithoutMatchingUnrelatedClasses() {
        assertTrue(OptionalModCompatibility.isSophisticatedClassName(
                "net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer"));
        assertTrue(OptionalModCompatibility.isSophisticatedClassName(
                "net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu"));
        assertTrue(OptionalModCompatibility.isSophisticatedClassName(
                "net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase"));
        assertFalse(OptionalModCompatibility.isSophisticatedClassName(
                "net.minecraft.world.inventory.ChestMenu"));
        assertFalse(OptionalModCompatibility.isSophisticatedClassName(null));
    }
}
