package com.xirc.mealmastery.tracking;

import com.xirc.mealmastery.progression.ProgressionService;
import com.xirc.mealmastery.recipe.CulinaryRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * Records meals a player personally eats.
 *
 * <p>Kept separate from preparation on purpose: a chef who cooks for a whole
 * server should see "Prepared 412 / Personally Eaten 91", not one merged
 * number.</p>
 */
public final class EatingTracker {

    private EatingTracker() {
    }

    public static void onItemEaten(ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null || !CulinaryRegistries.current().isTracked(itemId)) {
            return;
        }
        // The stamp travels with the stack, so who cooked it survives being
        // traded, dropped or stored in a chest before it is eaten.
        ProgressionService.recordMeal(player, itemId,
                com.xirc.mealmastery.culinary.MealStamp.read(stack));
    }
}
