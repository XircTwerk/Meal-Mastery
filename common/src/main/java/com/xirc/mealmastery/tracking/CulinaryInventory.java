package com.xirc.mealmastery.tracking;

import com.xirc.mealmastery.recipe.CulinaryRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.FurnaceResultSlot;
import net.minecraft.world.inventory.InventoryMenu;
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
     * <p>Without this a crafting grid is a hole in the snapshot: putting a
     * carrot into it reads as a loss and taking the same carrot back out reads
     * as a gain, so shuffling one item in and out of a bench credits a fresh
     * preparation every time, batch bonus and all. Counting the inputs makes
     * that move net zero, while a real result — which arrives from a slot
     * nothing can be placed into — still shows up as a gain.</p>
     *
     * <p>Only the menus the tracker recognises by class are read. A workstation
     * identified by an interaction window keeps its finished dish in a slot of
     * its own, and counting that would credit the dish the moment it appeared,
     * to whoever happened to have the screen open rather than to whoever took
     * it.</p>
     */
    private static void addMenuInputs(Map<ResourceLocation, Integer> counts,
                                      AbstractContainerMenu menu, CulinaryRegistry registry) {
        if (!(menu instanceof CraftingMenu || menu instanceof InventoryMenu
                || menu instanceof AbstractFurnaceMenu)) {
            return;
        }
        for (Slot slot : menu.slots) {
            // The player's own slots are already counted, and an output slot is
            // where a gain is supposed to come from.
            if (slot.container instanceof Inventory || slot instanceof ResultSlot
                    || slot instanceof FurnaceResultSlot) {
                continue;
            }
            add(counts, slot.getItem(), registry);
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
