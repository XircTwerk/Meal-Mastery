package com.xirc.mealmastery.tracking;

import com.xirc.mealmastery.recipe.CookingMethod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.Set;

/**
 * "This player produced this much of this dish, this way."
 *
 * <p>The one thing tracking hands to progression. Credit is only ever created
 * when a specific player can genuinely be held responsible; automation that
 * cannot be attributed produces no credit at all rather than a guess.</p>
 *
 * @param player            who gets the credit
 * @param meal              the dish's mastery target
 * @param amount            how many were produced
 * @param method            how it was made
 * @param source            which attribution surface observed it
 * @param usedIngredients   ingredients observed leaving the player's inventory
 *                          during the same session; empty when unknowable
 * @param automated         whether this came through the automation path and
 *                          should be scaled by the automation credit setting
 */
public record CulinaryCredit(ServerPlayer player,
                             ResourceLocation meal,
                             int amount,
                             CookingMethod method,
                             Source source,
                             Set<ResourceLocation> usedIngredients,
                             boolean automated) {

    public enum Source {
        /** Taken out of a workstation, crafting or furnace menu. */
        MENU,
        /** Obtained by right-clicking a workstation — skillet, pot with a bowl, feast. */
        INTERACTION,
        /** Dropped next to a workstation the player just used — cutting board. */
        DROP,
        /** Granted through the public API by another mod. */
        API
    }

    public CulinaryCredit {
        usedIngredients = Set.copyOf(usedIngredients);
        amount = Math.max(1, amount);
    }

    public static CulinaryCredit of(ServerPlayer player, ResourceLocation meal, int amount,
                                    CookingMethod method, Source source) {
        return new CulinaryCredit(player, meal, amount, method, source, Set.of(), false);
    }
}
