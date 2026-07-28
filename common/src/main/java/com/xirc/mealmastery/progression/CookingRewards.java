package com.xirc.mealmastery.progression;

import com.xirc.mealmastery.MealMasteryLog;
import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.config.ServerConfig;
import com.xirc.mealmastery.culinary.CulinaryProfile;
import com.xirc.mealmastery.culinary.MealStamp;
import com.xirc.mealmastery.mastery.MasteryRank;
import com.xirc.mealmastery.recipe.CookingMethod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What a cook gets out of a preparation beyond the numbers: quality stamped
 * onto the food, the occasional perfect result, an occasional extra portion,
 * and the tool perks that come with practising a method.
 *
 * <p>All of it scales with the cook's rank in the dish, so none of it applies
 * to a first attempt.</p>
 */
public final class CookingRewards {

    private CookingRewards() {
    }

    /**
     * Stamps freshly cooked food and rolls the extras.
     *
     * @param amount how many were produced
     * @return the number of bonus portions granted, so the caller can rebaseline
     */
    public static int onPrepared(ServerPlayer player, CulinaryProfile profile,
                                 ResourceLocation meal, int amount, CookingMethod method) {
        ServerConfig config = ConfigManager.server();
        ServerConfig.Bonuses bonuses = config.mastery.bonuses;
        if (!bonuses.enabled || !config.mastery.enabled) {
            return 0;
        }
        int stars = starsFor(profile, meal, config);

        boolean perfect = stars > 0 && bonuses.perfectChanceAtMaxRank > 0.0
                && player.getRandom().nextDouble()
                < bonuses.perfectChanceAtMaxRank * stars / MasteryRank.highestIndex();

        MealStamp stamp = new MealStamp(stars, perfect, player.getUUID(),
                player.getGameProfile().getName());
        if (stamp.isMeaningful()) {
            stampFresh(player, meal, amount, stamp);
        }

        int bonusPortions = rollBatch(player, meal, amount, stars, bonuses, stamp);
        refundTool(player, method, profile, bonuses);

        if (perfect) {
            player.displayClientMessage(Component.translatable("mealmastery.notify.perfect",
                    Component.translatable(itemOf(meal).getDescriptionId())), true);
        }
        return bonusPortions;
    }

    private static int starsFor(CulinaryProfile profile, ResourceLocation meal,
                                ServerConfig config) {
        int stars = profile.rankOf(meal, config.mastery.toCurve()).ordinal();
        // A signature dish is the one you are known for, so it plates a step
        // above the rest.
        if (meal.equals(profile.signatureDish())) {
            stars = Math.min(MasteryRank.highestIndex(), stars + 1);
        }
        return stars;
    }

    /**
     * Writes the stamp onto the portions that were just produced.
     *
     * <p>Only unstamped stacks are touched, and only up to {@code amount}
     * items, so food already sitting in the inventory is not retroactively
     * relabelled. A partial stack is split so a batch of ten does not become
     * ten perfect portions.</p>
     */
    private static void stampFresh(ServerPlayer player, ResourceLocation meal, int amount,
                                   MealStamp stamp) {
        Item item = itemOf(meal);
        if (item == null) {
            return;
        }
        Inventory inventory = player.getInventory();
        int remaining = amount;

        for (int slot = 0; slot < inventory.getContainerSize() && remaining > 0; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty() || stack.getItem() != item || MealStamp.isStamped(stack)) {
                continue;
            }
            if (stack.getCount() <= remaining) {
                stamp.write(stack);
                remaining -= stack.getCount();
                continue;
            }
            // Split: only the newly cooked portion carries the stamp.
            ItemStack stamped = stack.copy();
            stamped.setCount(remaining);
            stamp.write(stamped);
            stack.shrink(remaining);
            remaining = 0;
            if (!inventory.add(stamped)) {
                player.drop(stamped, false);
            }
        }
        consolidate(inventory, item);
    }

    /**
     * Merges stacks that ended up split by stamping.
     *
     * <p>Vanilla merges on insert, and at that moment a freshly cooked portion
     * is still unstamped while the stack already in the slot is stamped — so it
     * refuses to merge and takes a new slot, then gets stamped a moment later.
     * The result is a row of identical one-item stacks. This pass runs
     * afterwards and merges anything that now matches.</p>
     */
    private static void consolidate(Inventory inventory, Item item) {
        for (int target = 0; target < inventory.getContainerSize(); target++) {
            ItemStack into = inventory.getItem(target);
            if (into.isEmpty() || into.getItem() != item
                    || into.getCount() >= into.getMaxStackSize()) {
                continue;
            }
            for (int source = target + 1; source < inventory.getContainerSize(); source++) {
                ItemStack from = inventory.getItem(source);
                if (from.isEmpty() || from.getItem() != item || !sameStamp(into, from)) {
                    continue;
                }
                int room = into.getMaxStackSize() - into.getCount();
                int moved = Math.min(room, from.getCount());
                into.grow(moved);
                from.shrink(moved);
                if (from.isEmpty()) {
                    inventory.setItem(source, ItemStack.EMPTY);
                }
                if (into.getCount() >= into.getMaxStackSize()) {
                    break;
                }
            }
        }
    }

    /** Isolated: 1.21 renamed this to isSameItemSameComponents. */
    private static boolean sameStamp(ItemStack left, ItemStack right) {
        return ItemStack.isSameItemSameTags(left, right);
    }

    /** A practised cook occasionally gets more out of the same ingredients. */
    private static int rollBatch(ServerPlayer player, ResourceLocation meal, int amount, int stars,
                                 ServerConfig.Bonuses bonuses, MealStamp stamp) {
        if (stars <= 0 || bonuses.extraPortionChanceAtMaxRank <= 0.0) {
            return 0;
        }
        double chance = bonuses.extraPortionChanceAtMaxRank * stars / MasteryRank.highestIndex();
        if (player.getRandom().nextDouble() >= chance) {
            return 0;
        }
        Item item = itemOf(meal);
        if (item == null) {
            return 0;
        }
        ItemStack bonus = new ItemStack(item, 1);
        if (stamp.isMeaningful()) {
            stamp.write(bonus);
        }
        if (!player.getInventory().add(bonus)) {
            player.drop(bonus, false);
        }
        return 1;
    }

    /**
     * Method perks. A cook who has used a tool often enough stops ruining it
     * quite so fast — implemented as an occasional refund of one durability
     * point rather than by intercepting the damage, which belongs to whoever
     * owns the workstation.
     */
    private static void refundTool(ServerPlayer player, CookingMethod method,
                                   CulinaryProfile profile, ServerConfig.Bonuses bonuses) {
        if (method == null || !CookingMethod.CUTTING_BOARD.equals(method)
                || bonuses.toolRefundChanceAtMaxTier <= 0.0) {
            return;
        }
        var record = profile.peekMethod(method);
        if (record == null) {
            return;
        }
        int tier = (int) Math.min(MasteryRank.highestIndex(), record.preparations() / 25L);
        if (tier <= 0) {
            return;
        }
        double chance = bonuses.toolRefundChanceAtMaxTier * tier / MasteryRank.highestIndex();
        if (player.getRandom().nextDouble() >= chance) {
            return;
        }
        ItemStack tool = player.getMainHandItem();
        if (tool.isDamageableItem() && tool.getDamageValue() > 0) {
            tool.setDamageValue(tool.getDamageValue() - 1);
        }
    }

    private static Item itemOf(ResourceLocation meal) {
        try {
            return BuiltInRegistries.ITEM.get(meal);
        } catch (RuntimeException failure) {
            MealMasteryLog.LOGGER.debug("Could not resolve dish item {}", meal, failure);
            return null;
        }
    }
}
