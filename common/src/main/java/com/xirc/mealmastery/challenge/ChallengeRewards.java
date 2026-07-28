package com.xirc.mealmastery.challenge;

import com.xirc.mealmastery.MealMasteryLog;
import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.config.ServerConfig;
import com.xirc.mealmastery.culinary.CulinaryProfile;
import com.xirc.mealmastery.event.CulinaryEvents;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementProgress;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Hands out what a challenge promised.
 *
 * <p>Every branch is individually guarded: a datapack that names an item or an
 * advancement that does not exist logs a warning and the rest of the reward is
 * still granted.</p>
 */
public final class ChallengeRewards {

    private ChallengeRewards() {
    }

    public static void grant(ServerPlayer player, CulinaryProfile profile, ChallengeReward reward) {
        if (reward == null || reward == ChallengeReward.NONE) {
            return;
        }
        ServerConfig config = ConfigManager.server();

        if (reward.cookingXp() > 0) {
            long xp = Math.round(reward.cookingXp() * config.challenges.challengeXpMultiplier
                    * config.progression.xpMultiplier);
            if (xp > 0L) {
                profile.addCookingXp(xp, config.progression.toCurve());
                CulinaryEvents.COOKING_XP_GAINED.fire(new CulinaryEvents.CookingXpGained(
                        player, profile, xp, null));
            }
        }
        if (reward.vanillaXp() > 0) {
            player.giveExperiencePoints(reward.vanillaXp());
        }
        if (reward.badge() != null) {
            profile.awardBadge(reward.badge());
        }
        if (reward.advancement() != null) {
            grantAdvancement(player, reward.advancement());
        }
        for (String entry : reward.items()) {
            giveItem(player, entry);
        }
        if (!reward.commands().isEmpty()) {
            if (!config.challenges.allowCommandRewards) {
                MealMasteryLog.LOGGER.debug(
                        "Skipping {} command reward(s): challenges.allowCommandRewards is off",
                        reward.commands().size());
            } else {
                for (String command : reward.commands()) {
                    runCommand(player, command);
                }
            }
        }
    }

    private static void grantAdvancement(ServerPlayer player, ResourceLocation id) {
        Advancement advancement = player.server.getAdvancements().getAdvancement(id);
        if (advancement == null) {
            MealMasteryLog.LOGGER.warn("Challenge reward names an unknown advancement: {}", id);
            return;
        }
        AdvancementProgress progress = player.getAdvancements().getOrStartProgress(advancement);
        for (String criterion : progress.getRemainingCriteria()) {
            player.getAdvancements().award(advancement, criterion);
        }
    }

    /** Accepts {@code "modid:item"} or {@code "modid:item 3"}. */
    private static void giveItem(ServerPlayer player, String entry) {
        String[] halves = entry.trim().split("\\s+", 2);
        ResourceLocation id = ResourceLocation.tryParse(halves[0]);
        if (id == null) {
            MealMasteryLog.LOGGER.warn("Challenge item reward is not a valid id: '{}'", entry);
            return;
        }
        Item item = BuiltInRegistries.ITEM.get(id);
        if (item == null || !BuiltInRegistries.ITEM.containsKey(id)) {
            MealMasteryLog.LOGGER.warn("Challenge item reward names an unknown item: {}", id);
            return;
        }
        int count = 1;
        if (halves.length == 2) {
            try {
                count = Math.max(1, Integer.parseInt(halves[1].trim()));
            } catch (NumberFormatException ignored) {
                MealMasteryLog.LOGGER.warn("Challenge item reward has a bad count: '{}'", entry);
            }
        }
        ItemStack stack = new ItemStack(item, count);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void runCommand(ServerPlayer player, String command) {
        String resolved = command.replace("@s", player.getGameProfile().getName());
        try {
            player.server.getCommands().performPrefixedCommand(
                    ChallengeManager.rewardSource(player), resolved);
        } catch (RuntimeException failure) {
            MealMasteryLog.LOGGER.error("Challenge command reward failed: {}", resolved, failure);
        }
    }
}
