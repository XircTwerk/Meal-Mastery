package com.xirc.mealmastery.tracking;

import com.xirc.mealmastery.recipe.CookingMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class PlayerTrackingTest {

    @Test
    void menuSessionConsumesInteractionWindowWithoutLosingItsBaseline() {
        PlayerTracking tracking = new PlayerTracking();
        ResourceLocation dish = ResourceLocation.fromNamespaceAndPath(
                "farmersdelight", "beef_stew");

        tracking.beginWindow(new BlockPos(1, 2, 3), CookingMethod.COOKING_POT,
                100L, Map.of(dish, 1));
        tracking.beginMenuSession(4, CookingMethod.COOKING_POT, Map.of(dish, 2));

        assertFalse(tracking.hasWindow(50L));
        assertNull(tracking.windowPos());
        assertEquals(Map.of(dish, 2), tracking.baseline());
    }
}
