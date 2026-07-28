package com.xirc.mealmastery.culinary;

import com.xirc.mealmastery.mastery.MasteryRank;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.UUID;

/**
 * Quality data carried by a cooked stack: who made it, how good they were, and
 * whether that particular attempt came out perfect.
 *
 * <p>This is the one place the mod writes to an item, and it is deliberate. The
 * design otherwise keeps food stacks free of mod data, but a chef's cooking
 * cannot be better for the person they hand it to unless the food itself
 * remembers who cooked it.</p>
 *
 * <p>Stored in vanilla's {@code CUSTOM_DATA} component under our own key, so no
 * component type is registered and an unstamped stack is byte-identical to
 * vanilla food. Stamped and unstamped stacks do not merge, which is what keeps
 * a batch of good food from being diluted by a batch of bad.</p>
 *
 * @param stars   the cook's rank with this dish, 0..{@link MasteryRank#highestIndex()}
 * @param perfect whether this attempt rolled a perfect result
 * @param cookId  who cooked it, so they can be credited when it is eaten
 * @param cookName their name at the time, for the tooltip
 */
public record MealStamp(int stars, boolean perfect, UUID cookId, String cookName) {

    private static final String ROOT = "MealMastery";
    private static final String STARS = "stars";
    private static final String PERFECT = "perfect";
    private static final String COOK_ID = "cook";
    private static final String COOK_NAME = "cookName";

    public MealStamp {
        stars = Math.max(0, Math.min(MasteryRank.highestIndex(), stars));
        cookName = cookName == null ? "" : cookName;
    }

    /** Effective stars including the perfect bonus, capped at the maximum. */
    public int effectiveStars() {
        return Math.min(MasteryRank.highestIndex(), perfect ? stars + 1 : stars);
    }

    public boolean isMeaningful() {
        return stars > 0 || perfect;
    }

    public static MealStamp read(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        CompoundTag root = data.copyTag();
        if (!root.contains(ROOT, net.minecraft.nbt.Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag tag = root.getCompound(ROOT);
        UUID cookId = tag.hasUUID(COOK_ID) ? tag.getUUID(COOK_ID) : null;
        return new MealStamp(tag.getInt(STARS), tag.getBoolean(PERFECT), cookId,
                tag.getString(COOK_NAME));
    }

    public static boolean isStamped(ItemStack stack) {
        return read(stack) != null;
    }

    public void write(ItemStack stack) {
        CustomData existing = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag root = existing == null ? new CompoundTag() : existing.copyTag();

        CompoundTag tag = new CompoundTag();
        tag.putInt(STARS, stars);
        if (perfect) {
            tag.putBoolean(PERFECT, true);
        }
        if (cookId != null) {
            tag.putUUID(COOK_ID, cookId);
        }
        if (!cookName.isEmpty()) {
            tag.putString(COOK_NAME, cookName);
        }
        root.put(ROOT, tag);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
    }
}
