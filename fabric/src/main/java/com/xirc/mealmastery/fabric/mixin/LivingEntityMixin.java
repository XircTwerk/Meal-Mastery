package com.xirc.mealmastery.fabric.mixin;

import com.xirc.mealmastery.event.CulinaryHooks;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fabric has no "finished using item" event, so Meal Mastery observes the one
 * vanilla method every food item funnels through.
 *
 * <p><b>Target:</b> {@code LivingEntity#eat(Level, ItemStack, FoodProperties)} —
 * called from {@code Item#finishUsingItem} and overridden by {@code Player},
 * which delegates back here via {@code super}. 1.21 added the food-properties
 * parameter when food moved into an item component.</p>
 *
 * <p><b>Why a mixin:</b> NeoForge exposes {@code LivingEntityUseItemEvent.Finish}
 * and uses it instead; Fabric API offers no equivalent. The injection is a
 * read-only {@code HEAD} observer that never cancels and never mutates the
 * stack, so it cannot change vanilla eating behaviour or conflict with other
 * mods injecting into the same method.</p>
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "eat(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/item/ItemStack;"
            + "Lnet/minecraft/world/food/FoodProperties;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"))
    private void mealmastery$onEat(Level level, ItemStack stack, FoodProperties food,
                                   CallbackInfoReturnable<ItemStack> cir) {
        if (level.isClientSide) {
            return;
        }
        if ((Object) this instanceof ServerPlayer player) {
            CulinaryHooks.onItemEaten(player, stack.copy());
        }
    }
}
