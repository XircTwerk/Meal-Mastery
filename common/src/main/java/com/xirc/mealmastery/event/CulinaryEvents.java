package com.xirc.mealmastery.event;

import com.xirc.mealmastery.MealMasteryLog;
import com.xirc.mealmastery.culinary.CulinaryProfile;
import com.xirc.mealmastery.mastery.MasteryRank;
import com.xirc.mealmastery.recipe.CookingMethod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * The internal event bus everything downstream hangs off.
 *
 * <p>Tracking decides <em>what happened</em>; challenges, notifications,
 * statistics and the API all react to it. Keeping them decoupled is what stops
 * the progression path turning into one enormous manager.</p>
 *
 * <p>Listeners are invoked on the server thread in registration order and are
 * individually guarded: a listener that throws is logged and skipped so one
 * broken integration cannot stop a meal from being recorded.</p>
 */
public final class CulinaryEvents {

    private CulinaryEvents() {
    }

    // ------------------------------------------------------------ event types

    /**
     * @param amount how many of the dish were produced
     * @param method how it was made
     * @param firstEver whether this was the player's first ever preparation
     */
    public record MealPrepared(ServerPlayer player, CulinaryProfile profile, ResourceLocation meal,
                               int amount, CookingMethod method, boolean firstEver, long day) {
    }

    /**
     * @param stamp the quality data the eaten stack carried, or {@code null}
     *              when it was never stamped by a cook
     */
    public record MealEaten(ServerPlayer player, CulinaryProfile profile, ResourceLocation meal,
                            long day, com.xirc.mealmastery.culinary.MealStamp stamp) {
    }

    public record RecipeDiscovered(ServerPlayer player, CulinaryProfile profile,
                                   ResourceLocation meal, long day) {
    }

    public record IngredientDiscovered(ServerPlayer player, CulinaryProfile profile,
                                       ResourceLocation ingredient, long day) {
    }

    public record MethodDiscovered(ServerPlayer player, CulinaryProfile profile,
                                   CookingMethod method, long day) {
    }

    public record MasteryRankChanged(ServerPlayer player, CulinaryProfile profile,
                                     ResourceLocation meal, MasteryRank from, MasteryRank to) {
    }

    public record CookingLevelChanged(ServerPlayer player, CulinaryProfile profile,
                                      int from, int to) {
    }

    public record CookingXpGained(ServerPlayer player, CulinaryProfile profile, long amount,
                                  ResourceLocation reason) {
    }

    public record PortionsServed(ServerPlayer player, CulinaryProfile profile,
                                 ResourceLocation meal, int portions, long day) {
    }

    public record ChallengeCompleted(ServerPlayer player, CulinaryProfile profile,
                                     ResourceLocation challenge) {
    }

    public record MilestoneReached(ServerPlayer player, CulinaryProfile profile,
                                   ResourceLocation milestone) {
    }

    // ------------------------------------------------------------------- bus

    public static final Channel<MealPrepared> MEAL_PREPARED = new Channel<>("MealPrepared");
    public static final Channel<MealEaten> MEAL_EATEN = new Channel<>("MealEaten");
    public static final Channel<RecipeDiscovered> RECIPE_DISCOVERED = new Channel<>("RecipeDiscovered");
    public static final Channel<IngredientDiscovered> INGREDIENT_DISCOVERED =
            new Channel<>("IngredientDiscovered");
    public static final Channel<MethodDiscovered> METHOD_DISCOVERED = new Channel<>("MethodDiscovered");
    public static final Channel<MasteryRankChanged> MASTERY_RANK_CHANGED =
            new Channel<>("MasteryRankChanged");
    public static final Channel<CookingLevelChanged> COOKING_LEVEL_CHANGED =
            new Channel<>("CookingLevelChanged");
    public static final Channel<CookingXpGained> COOKING_XP_GAINED = new Channel<>("CookingXpGained");
    public static final Channel<PortionsServed> PORTIONS_SERVED = new Channel<>("PortionsServed");
    public static final Channel<ChallengeCompleted> CHALLENGE_COMPLETED =
            new Channel<>("ChallengeCompleted");
    public static final Channel<MilestoneReached> MILESTONE_REACHED = new Channel<>("MilestoneReached");

    public static final class Channel<T> {
        private final String name;
        private final List<Consumer<T>> listeners = new CopyOnWriteArrayList<>();

        Channel(String name) {
            this.name = name;
        }

        public void register(Consumer<T> listener) {
            listeners.add(listener);
        }

        public void unregister(Consumer<T> listener) {
            listeners.remove(listener);
        }

        public void fire(T event) {
            for (Consumer<T> listener : listeners) {
                try {
                    listener.accept(event);
                } catch (RuntimeException | LinkageError failure) {
                    MealMasteryLog.LOGGER.error(
                            "A {} listener threw. It has been skipped for this event; cooking is "
                                    + "unaffected.", name, failure);
                }
            }
        }
    }
}
