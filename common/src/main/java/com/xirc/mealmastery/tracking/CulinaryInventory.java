package com.xirc.mealmastery.tracking;

import com.xirc.mealmastery.recipe.CulinaryRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.ResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A count of the culinary items a player is carrying.
 *
 * <p>Snapshots are the basis of every attribution decision, so they are kept
 * cheap: only items the registry actually tracks are counted, and a snapshot is
 * only ever taken for a player who has a culinary menu open or has just used a
 * workstation. Nothing scans an inventory on a schedule.</p>
 */
public final class CulinaryInventory {

    private CulinaryInventory() {
    }

    /**
     * Counts tracked dishes and tracked ingredients the player is holding,
     * including whatever is on the cursor so a stack picked up inside a menu is
     * not mistaken for a loss, and whatever they have parked in the open menu's
     * input slots.
     */
    public static Map<ResourceLocation, Integer> snapshot(ServerPlayer player,
                                                          CulinaryRegistry registry) {
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            add(counts, inventory.getItem(slot), registry);
        }
        AbstractContainerMenu menu = player.containerMenu;
        add(counts, menu == null ? ItemStack.EMPTY : menu.getCarried(), registry);
        addMenuInputs(counts, menu, registry);
        return counts;
    }

    /**
     * Counts what is sitting in the open menu's own input slots.
     *
     * <p>Without this the menu is a hole in the snapshot: putting a cooked dish
     * into a Cooking Pot's ingredient grid reads as a loss and taking the same
     * dish back out reads as a gain, so shuffling one meal in and out credits a
     * fresh preparation every time, with the star rating and the batch bonus
     * that follow from it. Counting the inputs makes that move net zero.</p>
     *
     * <p>Output slots are left out, so a dish genuinely taken from a
     * workstation still arrives as a gain and is still credited to whoever took
     * it rather than to whoever was watching when it finished. A slot counts as
     * output when the player cannot put anything into it, which is how every
     * vanilla and Farmer's Delight result slot is built, so a workstation from
     * a mod this one has never heard of is read correctly too.</p>
     */
    private static void addMenuInputs(Map<ResourceLocation, Integer> counts,
                                      AbstractContainerMenu menu, CulinaryRegistry registry) {
        if (menu == null) {
            return;
        }
        for (Slot slot : menu.slots) {
            // The player's own slots were counted above.
            if (slot.container instanceof Inventory) {
                continue;
            }
            if (slot instanceof ResultSlot || slot instanceof FurnaceResultSlot
                    || !acceptsPlacement(slot)) {
                continue;
            }
            add(counts, slot.getItem(), registry);
        }
    }

    /**
     * Whether the player could have put this slot's contents there themselves.
     *
     * <p>The two vanilla result slots are named directly because that test is
     * exact and cheap. Everything else is judged by whether it accepts
     * placement at all, which is what separates a grid from an output without
     * naming a single modded class. A slot that throws is treated as an output,
     * leaving the mod with the behaviour it had before it read menus at all.</p>
     */
    static boolean acceptsPlacement(Slot slot) {
        ItemStack stack = slot.getItem();
        if (stack.isEmpty()) {
            return false;
        }
        try {
            return slot.mayPlace(stack);
        } catch (RuntimeException | LinkageError failure) {
            return false;
        }
    }

    private static void add(Map<ResourceLocation, Integer> counts, ItemStack stack,
                            CulinaryRegistry registry) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return;
        }
        if (registry.isTracked(id) || registry.allIngredients().contains(id)) {
            counts.merge(id, stack.getCount(), Integer::sum);
        }
    }

    /**
     * Items whose count went up between two snapshots.
     *
     * @param alreadyCredited counts handled by another attribution surface in
     *                        the same window, subtracted so a cutting-board drop
     *                        credited on spawn is not credited again on pickup
     */
    public static Map<ResourceLocation, Integer> gains(Map<ResourceLocation, Integer> before,
                                                       Map<ResourceLocation, Integer> after,
                                                       Map<ResourceLocation, Integer> alreadyCredited) {
        Map<ResourceLocation, Integer> gains = new LinkedHashMap<>();
        after.forEach((id, count) -> {
            int delta = count - before.getOrDefault(id, 0)
                    - alreadyCredited.getOrDefault(id, 0);
            if (delta > 0) {
                gains.put(id, delta);
            }
        });
        return gains;
    }

    /** Items whose count went down: what the player fed into the workstation. */
    public static Map<ResourceLocation, Integer> losses(Map<ResourceLocation, Integer> before,
                                                        Map<ResourceLocation, Integer> after) {
        Map<ResourceLocation, Integer> losses = new LinkedHashMap<>();
        before.forEach((id, count) -> {
            int delta = count - after.getOrDefault(id, 0);
            if (delta > 0) {
                losses.put(id, delta);
            }
        });
        return losses;
    }
}
