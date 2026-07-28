package com.xirc.mealmastery.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/**
 * A packet payload, independent of how either loader moves bytes.
 *
 * <p>Fabric sends these as custom payloads directly; Forge wraps them in a
 * single {@code SimpleChannel} message. Neither knows what is inside.</p>
 */
public interface MealMasteryPacket {

    ResourceLocation id();

    void write(FriendlyByteBuf buffer);
}
