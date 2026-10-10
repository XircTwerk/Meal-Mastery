package com.xirc.mealmastery.tracking;

import com.xirc.mealmastery.MinecraftBootstrap;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rule that decides whether a slot in an open menu holds food the player is
 * still carrying, or food a workstation has produced for them.
 *
 * <p>Getting this wrong in either direction is a reported bug. Treat an input
 * as an output and shuffling a dish in and out of a Cooking Pot's grid reads as
 * cooking it again, which paid out XP, mastery and duplicate portions. Treat an
 * output as an input and taking a finished dish out of a workstation stops
 * counting at all.</p>
 */
class CulinaryInventoryTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftBootstrap.ensure();
    }

    private static Slot filled(Container container) {
        container.setItem(0, new ItemStack(Items.BREAD, 4));
        return new Slot(container, 0, 0, 0);
    }

    @Test
    void aSlotThePlayerCanFillCountsAsSomethingTheyAreCarrying() {
        assertTrue(CulinaryInventory.acceptsPlacement(filled(new SimpleContainer(1))));
    }

    @Test
    void aSlotThatRefusesPlacementIsAnOutput() {
        Container container = new SimpleContainer(1);
        container.setItem(0, new ItemStack(Items.BREAD, 4));
        Slot output = new Slot(container, 0, 0, 0) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        };
        assertFalse(CulinaryInventory.acceptsPlacement(output));
    }

    @Test
    void anEmptySlotHasNothingToCount() {
        assertFalse(CulinaryInventory.acceptsPlacement(new Slot(new SimpleContainer(1), 0, 0, 0)));
    }

    @Test
    void aSlotThatThrowsIsTreatedAsAnOutputRatherThanBreakingTracking() {
        Container container = new SimpleContainer(1);
        container.setItem(0, new ItemStack(Items.BREAD, 4));
        Slot hostile = new Slot(container, 0, 0, 0) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                throw new IllegalStateException("a slot from a mod that dislikes being asked");
            }
        };
        assertFalse(CulinaryInventory.acceptsPlacement(hostile));
    }
}
