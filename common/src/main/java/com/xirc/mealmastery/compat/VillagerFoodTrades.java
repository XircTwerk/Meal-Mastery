package com.xirc.mealmastery.compat;

import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.culinary.MealStamp;
import com.xirc.mealmastery.recipe.CulinaryRegistries;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.ArrayList;
import java.util.List;

/**
 * Farmers buy prepared meals, and pay more for good ones.
 *
 * <p>Vanilla trade offers are fixed at the moment a villager is created, so a
 * price cannot depend on the quality of the stack a player eventually hands
 * over. The offer therefore asks for a specific dish at a base price, and the
 * <em>quality bonus is paid separately</em> when a stamped stack is traded —
 * which is the only honest way to make stars matter here.</p>
 */
public final class VillagerFoodTrades {

    private VillagerFoodTrades() {
    }

    /**
     * Builds one "buy a prepared meal" offer from whatever dishes this server
     * actually has, so no dish is named in code.
     */
    public static VillagerTrades.ItemListing preparedMealListing() {
        return (trader, random) -> {
            var registry = CulinaryRegistries.current();
            List<ResourceLocation> dishes = new ArrayList<>(registry.targets());
            if (dishes.isEmpty()) {
                return null;
            }
            ResourceLocation dish = dishes.get(random.nextInt(dishes.size()));
            var item = BuiltInRegistries.ITEM.get(dish);
            if (item == null) {
                return null;
            }
            int wanted = 4 + random.nextInt(5);
            return new MerchantOffer(
                    new net.minecraft.world.item.trading.ItemCost(item, wanted),
                    new ItemStack(Items.EMERALD, 1),
                    8, 3, 0.05F);
        };
    }

    /**
     * The extra emeralds a stamped stack is worth on top of the offer.
     *
     * @return additional emeralds, or zero when the stack carries no quality
     */
    public static int qualityBonus(ItemStack stack) {
        if (!ConfigManager.server().mastery.bonuses.enabled) {
            return 0;
        }
        MealStamp stamp = MealStamp.read(stack);
        if (stamp == null || !stamp.isMeaningful()) {
            return 0;
        }
        return Math.max(0, stamp.effectiveStars() / 2) + (stamp.perfect() ? 1 : 0);
    }
}
