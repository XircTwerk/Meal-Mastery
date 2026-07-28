package com.xirc.mealmastery.tracking;

import com.xirc.mealmastery.recipe.CulinaryRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
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
     * not mistaken for a loss.
     */
    public static Map<ResourceLocation, Integer> snapshot(ServerPlayer player,
                                                          CulinaryRegistry registry) {
        Map<ResourceLocation, Integer> counts = new HashMap<>();
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            add(counts, inventory.getItem(slot), registry);
        }
        add(counts, player.containerMenu == null
                ? ItemStack.EMPTY : player.containerMenu.getCarried(), registry);
        return counts;
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
