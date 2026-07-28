package com.xirc.mealmastery.client.journal;

import com.xirc.mealmastery.client.ClientJournalState;
import com.xirc.mealmastery.config.ConfigManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Shared formatting for the journal.
 *
 * <p>Dish names come from the item's own translation key, so an addon's food is
 * already localised in whatever languages that addon ships — Meal Mastery never
 * duplicates a translation it does not own.</p>
 */
public final class JournalText {

    private static final NumberFormat NUMBERS = NumberFormat.getIntegerInstance(Locale.ROOT);

    private JournalText() {
    }

    public static ItemStack stackOf(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        return item == null ? ItemStack.EMPTY : new ItemStack(item);
    }

    public static Component nameOf(ResourceLocation id) {
        ItemStack stack = stackOf(id);
        return stack.isEmpty() ? Component.literal(id.getPath()) : stack.getHoverName();
    }

    public static String number(long value) {
        return NUMBERS.format(value);
    }

    /** "84 / 127", or just "84" when there is no meaningful denominator. */
    public static String fraction(long value, long total) {
        return total > 0 ? number(value) + " / " + number(total) : number(value);
    }

    public static String percent(float fraction) {
        return Math.round(Math.max(0.0F, Math.min(1.0F, fraction)) * 100.0F) + "%";
    }

    /**
     * Cozy mode hides raw numbers and percentages in favour of plain labels,
     * without changing any of the underlying data.
     */
    public static boolean cozy() {
        return ConfigManager.client().journal.cozyMode;
    }

    public static Component maybeNumber(long value) {
        return cozy() ? Component.empty() : Component.literal(number(value));
    }

    /**
     * A dish's mastery as filled and empty stars.
     *
     * <p>Text glyphs rather than a texture, because this mod ships no art
     *. The rank name is always shown alongside them, so rank is never
     * communicated by shape alone.</p>
     */
    public static String stars(ResourceLocation dish) {
        return starsOf(ClientJournalState.rank(dish).ordinal());
    }

    /** Stars for an explicit count, used for the rating stamped onto a stack. */
    public static String starsOf(int filled) {
        int total = com.xirc.mealmastery.mastery.MasteryRank.highestIndex();
        StringBuilder text = new StringBuilder(total);
        for (int i = 0; i < total; i++) {
            text.append(i < filled ? '\u2605' : '\u2606');
        }
        return text.toString();
    }

    public static Component rankOf(ResourceLocation dish) {
        return Component.translatable(ClientJournalState.rank(dish).translationKey());
    }

    /** How far into the current mastery rank a dish is, as 0..1. */
    public static float masteryFraction(ResourceLocation dish) {
        var record = ClientJournalState.record(dish);
        if (record == null) {
            return 0.0F;
        }
        long points = record.masteryPoints();
        long needed = ClientJournalState.masteryCurve().pointsForNextRank(points);
        if (needed <= 0L) {
            return 1.0F;
        }
        return (float) ClientJournalState.masteryCurve().progressIntoRank(points) / needed;
    }

    public static Component method(ResourceLocation methodId) {
        return Component.translatable(
                "mealmastery.method." + methodId.getNamespace() + "." + methodId.getPath());
    }

    public static Component category(ResourceLocation categoryId) {
        return Component.translatable(
                "mealmastery.category." + categoryId.getNamespace() + "." + categoryId.getPath());
    }
}
