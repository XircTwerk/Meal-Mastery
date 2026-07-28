package com.xirc.mealmastery.client.tooltip;

import com.xirc.mealmastery.client.ClientJournalState;
import com.xirc.mealmastery.client.journal.JournalText;
import com.xirc.mealmastery.config.ClientConfig;
import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.network.Packets;
import com.xirc.mealmastery.platform.Services;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * The optional Meal Mastery tooltip section.
 *
 * <p>Collapsed to a single hint line unless the modifier is held, so a normal
 * Farmer's Delight tooltip is never buried. Nutrition stays off by default and
 * is additionally suppressed whenever a nutrition mod is installed, because
 * that tooltip section belongs to them.</p>
 */
public final class MasteryTooltip {

    private static final List<String> NUTRITION_PROVIDERS =
            List.of("appleskin", "diet", "nutrition", "a_balanced_diet");

    private MasteryTooltip() {
    }

    /** Appends Meal Mastery's lines to an existing tooltip. */
    public static void append(ItemStack stack, List<Component> lines) {
        ClientConfig config = ConfigManager.client();
        if (!config.tooltips.enabled || stack.isEmpty()) {
            return;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id == null) {
            return;
        }
        Packets.DishView dish = ClientJournalState.dish(id);
        if (dish == null) {
            return;
        }
        Packets.RecordView record = ClientJournalState.record(id);
        boolean discovered = record != null && record.discovered();

        // A stamped stack describes itself: its quality belongs to whoever
        // cooked it, not to whoever is holding it.
        com.xirc.mealmastery.culinary.MealStamp stamp =
                com.xirc.mealmastery.culinary.MealStamp.read(stack);
        if (stamp != null && stamp.isMeaningful()) {
            if (stamp.perfect()) {
                lines.add(Component.translatable("mealmastery.tooltip.perfect")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD));
            }
            lines.add(Component.literal(JournalText.starsOf(stamp.effectiveStars()))
                    .withStyle(ChatFormatting.GOLD));
            if (!stamp.cookName().isEmpty()) {
                lines.add(Component.translatable("mealmastery.tooltip.cooked_by",
                        stamp.cookName()).withStyle(ChatFormatting.DARK_GRAY));
            }
        }

        // Stars sit directly under the name, always visible: they are the one
        // piece of Meal Mastery information that changes how the food behaves,
        // so hiding them behind a modifier would hide the mechanic itself.
        if (stamp == null && config.journal.showStars && discovered
                && ClientJournalState.rank(id).ordinal() > 0) {
            lines.add(Component.literal(JournalText.stars(id))
                    .withStyle(ChatFormatting.GOLD)
                    .append(Component.literal("  ").append(JournalText.rankOf(id))
                            .withStyle(ChatFormatting.DARK_GRAY)));
        }

        if (config.tooltips.requireModifier && !modifierHeld(config)) {
            lines.add(Component.translatable("mealmastery.tooltip.hint",
                            modifierName(config))
                    .withStyle(ChatFormatting.DARK_GRAY));
            if (!discovered && config.journal.showNewIndicator) {
                lines.add(Component.translatable("mealmastery.tooltip.new")
                        .withStyle(ChatFormatting.GOLD));
            }
            return;
        }

        lines.add(Component.translatable("mealmastery.tooltip.header")
                .withStyle(ChatFormatting.GOLD));

        if (!discovered) {
            lines.add(Component.translatable("mealmastery.tooltip.undiscovered")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }
        if (config.tooltips.showMastery) {
            lines.add(Component.translatable("mealmastery.tooltip.mastery",
                    JournalText.rankOf(id)).withStyle(ChatFormatting.GRAY));
        }
        if (config.tooltips.showPreparedCount) {
            lines.add(Component.translatable("mealmastery.tooltip.prepared",
                    JournalText.number(record.prepared())).withStyle(ChatFormatting.GRAY));
        }
        if (record.firstPreparedDay() != Long.MIN_VALUE) {
            lines.add(Component.translatable("mealmastery.tooltip.first_cooked",
                    record.firstPreparedDay()).withStyle(ChatFormatting.DARK_GRAY));
        }
        appendOdds(lines, id, stamp);

        if (config.tooltips.showNutrition && !nutritionModPresent()) {
            lines.add(Component.literal("Nutrition " + dish.nutrition()
                            + "  Saturation " + String.format("%.1f", dish.saturation()))
                    .withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    /**
     * States the actual odds at this dish's current rank.
     *
     * <p>These are real numbers from the server's configuration, not the
     * defaults: a pack that retunes them says so here rather than leaving the
     * player to guess why a bonus did or did not happen.</p>
     */
    private static void appendOdds(List<Component> lines, ResourceLocation dish,
                                   com.xirc.mealmastery.culinary.MealStamp stamp) {
        Packets.SyncRules rules = ClientJournalState.rules();
        if (rules == null || !rules.bonusesEnabled()) {
            return;
        }
        int max = com.xirc.mealmastery.mastery.MasteryRank.highestIndex();
        int cookStars = ClientJournalState.rank(dish).ordinal();
        int eatStars = stamp != null ? stamp.effectiveStars() : cookStars;

        // Say so explicitly at nothing-yet rather than printing no line at all:
        // a silent tooltip reads as "this mod has no bonuses", not as "you have
        // not earned them".
        if (eatStars <= 0 && cookStars <= 0) {
            lines.add(Component.translatable("mealmastery.tooltip.odds.none")
                    .withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        if (eatStars > 0) {
            double scale = (double) eatStars / max;
            lines.add(Component.translatable("mealmastery.tooltip.odds.effect",
                    percent(rules.effectChance() * scale),
                    rules.effectSeconds()).withStyle(ChatFormatting.DARK_GRAY));
            float saturation = rules.saturationPerRank() * eatStars;
            if (saturation > 0.0F) {
                lines.add(Component.translatable("mealmastery.tooltip.odds.saturation",
                        String.format("%.1f", saturation)).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        if (cookStars > 0) {
            double scale = (double) cookStars / max;
            lines.add(Component.translatable("mealmastery.tooltip.odds.cooking",
                    percent(rules.perfectChance() * scale),
                    percent(rules.extraPortionChance() * scale),
                    percent(rules.cookingSpeed() * scale)).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    private static String percent(double fraction) {
        return Math.round(fraction * 100.0) + "%";
    }

    private static boolean nutritionModPresent() {
        for (String modId : NUTRITION_PROVIDERS) {
            if (Services.PLATFORM.isModLoaded(modId)) {
                return true;
            }
        }
        return false;
    }

    private static boolean modifierHeld(ClientConfig config) {
        return switch (config.tooltips.modifier) {
            case SHIFT -> Screen.hasShiftDown();
            case CONTROL -> Screen.hasControlDown();
            case ALT -> Screen.hasAltDown();
            case NONE -> true;
        };
    }

    private static Component modifierName(ClientConfig config) {
        return Component.literal(switch (config.tooltips.modifier) {
            case SHIFT -> "Shift";
            case CONTROL -> "Ctrl";
            case ALT -> "Alt";
            case NONE -> "";
        });
    }
}
