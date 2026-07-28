package com.xirc.mealmastery.culinary;

import com.xirc.mealmastery.data.NbtUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * One line of the journal's Recent Activity feed.
 *
 * <p>Stored as a compact triple rather than pre-rendered text so the feed can
 * be re-translated when the player changes language, and so a renamed item
 * still shows its current name.</p>
 *
 * @param type    what happened
 * @param subject the dish, ingredient, method or challenge involved; may be null
 * @param detail  a small numeric payload whose meaning depends on {@code type}
 * @param day     the Minecraft day it happened on
 */
public record ActivityEntry(Type type, ResourceLocation subject, long detail, long day) {

    public enum Type {
        RECIPE_DISCOVERED("recipe_discovered"),
        MEAL_PREPARED("meal_prepared"),
        MASTERY_RANK("mastery_rank"),
        MEAL_MASTERED("meal_mastered"),
        LEVEL_UP("level_up"),
        METHOD_DISCOVERED("method_discovered"),
        INGREDIENT_DISCOVERED("ingredient_discovered"),
        CHALLENGE_COMPLETED("challenge_completed"),
        MILESTONE_REACHED("milestone_reached"),
        COLLECTION_COMPLETED("collection_completed");

        private static final Type[] VALUES = values();
        private final String id;

        Type(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }

        public String translationKey() {
            return "mealmastery.activity." + id;
        }

        public static Type byId(String id) {
            for (Type type : VALUES) {
                if (type.id.equals(id)) {
                    return type;
                }
            }
            return MEAL_PREPARED;
        }
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("type", type.id());
        NbtUtil.putIfPresent(tag, "subject", subject);
        NbtUtil.putIfNonZero(tag, "detail", detail);
        tag.putLong("day", day);
        return tag;
    }

    public static ActivityEntry load(CompoundTag tag) {
        return new ActivityEntry(
                Type.byId(tag.getString("type")),
                NbtUtil.readId(tag, "subject"),
                tag.getLong("detail"),
                tag.getLong("day"));
    }
}
