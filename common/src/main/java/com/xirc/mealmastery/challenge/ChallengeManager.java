package com.xirc.mealmastery.challenge;

import com.xirc.mealmastery.MealMasteryLog;
import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.config.ServerConfig;
import com.xirc.mealmastery.culinary.ActivityEntry;
import com.xirc.mealmastery.culinary.CulinaryClock;
import com.xirc.mealmastery.culinary.CulinaryProfile;
import com.xirc.mealmastery.event.CulinaryEvents;
import com.xirc.mealmastery.recipe.CulinaryRegistries;
import com.xirc.mealmastery.recipe.MealEntry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/**
 * Advances challenges in response to culinary events.
 *
 * <p>Listens rather than being called: tracking has no idea challenges exist,
 * which is what lets a datapack add objectives without any code changing.</p>
 */
public final class ChallengeManager {
    private static boolean registered;

    private ChallengeManager() {
    }

    public static synchronized void install() {
        if (registered) {
            return;
        }
        registered = true;
        CulinaryEvents.MEAL_PREPARED.register(ChallengeManager::onMealPrepared);
        CulinaryEvents.MEAL_EATEN.register(ChallengeManager::onMealEaten);
        CulinaryEvents.RECIPE_DISCOVERED.register(ChallengeManager::onRecipeDiscovered);
        CulinaryEvents.MASTERY_RANK_CHANGED.register(ChallengeManager::onMasteryChanged);
        CulinaryEvents.COOKING_LEVEL_CHANGED.register(ChallengeManager::onLevelChanged);
        CulinaryEvents.PORTIONS_SERVED.register(ChallengeManager::onPortionsServed);
        CulinaryEvents.INGREDIENT_DISCOVERED.register(ChallengeManager::onIngredientDiscovered);
    }

    // ------------------------------------------------------------- listeners

    private static void onMealPrepared(CulinaryEvents.MealPrepared event) {
        MealEntry entry = CulinaryRegistries.current().entry(event.meal());
        forEachActive(event.player(), event.profile(), (definition, state) -> {
            List<ChallengeObjective> objectives = definition.objectives();
            for (int i = 0; i < objectives.size(); i++) {
                ChallengeObjective objective = objectives.get(i);
                switch (objective.type()) {
                    case COOK_RECIPE -> {
                        if (!objective.hasValue() || event.meal().toString().equals(objective.value())) {
                            if (objective.uniqueOnly()) {
                                state.advanceUnique(i, event.meal());
                            } else {
                                state.advance(i, event.amount());
                            }
                        }
                    }
                    case COOK_CATEGORY -> {
                        if (entry != null && entry.category().id().toString().equals(objective.value())) {
                            advance(state, i, objective, event.meal(), event.amount());
                        }
                    }
                    case COOK_FROM_MOD -> {
                        if (event.meal().getNamespace().equals(objective.value())) {
                            advance(state, i, objective, event.meal(), event.amount());
                        }
                    }
                    case USE_METHOD -> {
                        if (event.method().id().toString().equals(objective.value())) {
                            state.advance(i, event.amount());
                        }
                    }
                    case PREPARE_VARIETY ->
                            state.raise(i, event.profile().streaks().varietyStreak());
                    case UNIQUE_RECIPES_IN_ONE_DAY ->
                            state.raise(i, event.profile().streaks().uniqueMealsToday());
                    default -> {
                    }
                }
            }
        });
    }

    private static void onMealEaten(CulinaryEvents.MealEaten event) {
        forEachActive(event.player(), event.profile(), (definition, state) ->
                forEachObjective(definition, ChallengeObjective.Type.EAT_RECIPE, (index, objective) -> {
                    if (!objective.hasValue() || event.meal().toString().equals(objective.value())) {
                        advance(state, index, objective, event.meal(), 1);
                    }
                }));
    }

    private static void onRecipeDiscovered(CulinaryEvents.RecipeDiscovered event) {
        forEachActive(event.player(), event.profile(), (definition, state) ->
                forEachObjective(definition, ChallengeObjective.Type.DISCOVER_RECIPE,
                        (index, objective) -> advance(state, index, objective, event.meal(), 1)));
    }

    private static void onIngredientDiscovered(CulinaryEvents.IngredientDiscovered event) {
        forEachActive(event.player(), event.profile(), (definition, state) -> {
            List<ChallengeObjective> objectives = definition.objectives();
            for (int i = 0; i < objectives.size(); i++) {
                ChallengeObjective objective = objectives.get(i);
                if (objective.type() == ChallengeObjective.Type.USE_INGREDIENT
                        && event.ingredient().toString().equals(objective.value())) {
                    advance(state, i, objective, event.ingredient(), 1);
                } else if (objective.type() == ChallengeObjective.Type.USE_INGREDIENT_TAG
                        && matchesTag(event.ingredient(), objective.valueAsId())) {
                    advance(state, i, objective, event.ingredient(), 1);
                }
            }
        });
    }

    private static void onMasteryChanged(CulinaryEvents.MasteryRankChanged event) {
        if (!event.to().isMastered()) {
            return;
        }
        forEachActive(event.player(), event.profile(), (definition, state) ->
                forEachObjective(definition, ChallengeObjective.Type.MASTER_RECIPE,
                        (index, objective) -> advance(state, index, objective, event.meal(), 1)));
    }

    private static void onLevelChanged(CulinaryEvents.CookingLevelChanged event) {
        forEachActive(event.player(), event.profile(), (definition, state) ->
                forEachObjective(definition, ChallengeObjective.Type.REACH_LEVEL,
                        (index, objective) -> state.raise(index, event.to())));
    }

    private static void onPortionsServed(CulinaryEvents.PortionsServed event) {
        forEachActive(event.player(), event.profile(), (definition, state) ->
                forEachObjective(definition, ChallengeObjective.Type.SERVE_PORTIONS,
                        (index, objective) -> state.advance(index, event.portions())));
    }

    // -------------------------------------------------------------- internals

    private interface ChallengeVisitor {
        void visit(ChallengeDefinition definition, ChallengeState state);
    }

    private interface ObjectiveVisitor {
        void visit(int index, ChallengeObjective objective);
    }

    private static void advance(ChallengeState state, int index, ChallengeObjective objective,
                                ResourceLocation subject, int amount) {
        if (objective.uniqueOnly()) {
            state.advanceUnique(index, subject);
        } else {
            state.advance(index, amount);
        }
    }

    private static void forEachObjective(ChallengeDefinition definition,
                                         ChallengeObjective.Type type,
                                         ObjectiveVisitor visitor) {
        List<ChallengeObjective> objectives = definition.objectives();
        for (int i = 0; i < objectives.size(); i++) {
            if (objectives.get(i).type() == type) {
                visitor.visit(i, objectives.get(i));
            }
        }
    }

    private static void forEachActive(ServerPlayer player, CulinaryProfile profile,
                                      ChallengeVisitor visitor) {
        ServerConfig config = ConfigManager.server();
        if (!config.challenges.enabled) {
            return;
        }
        long day = CulinaryClock.day(player.server);
        for (ChallengeDefinition definition : ChallengeRegistry.current().all()) {
            if (!isAvailable(definition, profile)) {
                continue;
            }
            ChallengeState state = profile.challenge(definition.id(), definition.objectives().size());
            if (state.isCompleted()) {
                continue;
            }
            try {
                visitor.visit(definition, state);
            } catch (RuntimeException failure) {
                MealMasteryLog.LOGGER.error("Challenge {} failed to advance", definition.id(), failure);
                continue;
            }
            if (state.satisfies(definition.objectives())) {
                complete(player, profile, definition, state, day);
            }
        }
        profile.markDirty();
    }

    private static boolean isAvailable(ChallengeDefinition definition, CulinaryProfile profile) {
        ServerConfig config = ConfigManager.server();
        return switch (definition.scope()) {
            case DAILY -> config.challenges.dailyEnabled;
            case WEEKLY -> config.challenges.weeklyEnabled;
            case CHAIN -> definition.prerequisite() == null
                    || profile.hasCompletedChallenge(definition.prerequisite());
            case PERMANENT -> definition.prerequisite() == null
                    || profile.hasCompletedChallenge(definition.prerequisite());
        };
    }

    private static boolean matchesTag(ResourceLocation itemId, ResourceLocation tagId) {
        if (tagId == null) {
            return false;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (item == null) {
            return false;
        }
        TagKey<Item> tag = TagKey.create(net.minecraft.core.registries.Registries.ITEM, tagId);
        return new ItemStack(item).is(tag);
    }

    private static void complete(ServerPlayer player, CulinaryProfile profile,
                                 ChallengeDefinition definition, ChallengeState state, long day) {
        state.complete(day);
        profile.pushActivity(new ActivityEntry(
                ActivityEntry.Type.CHALLENGE_COMPLETED, definition.id(), 0L, day));
        ChallengeRewards.grant(player, profile, definition.rewards());
        CulinaryEvents.CHALLENGE_COMPLETED.fire(new CulinaryEvents.ChallengeCompleted(
                player, profile, definition.id()));

        // Chains unlock in one step, so a follow-up challenge whose only
        // objective was "complete the previous one" resolves immediately.
        for (ChallengeDefinition candidate : ChallengeRegistry.current().all()) {
            if (!definition.id().equals(candidate.prerequisite())) {
                continue;
            }
            ChallengeState next = profile.challenge(candidate.id(), candidate.objectives().size());
            forEachObjective(candidate, ChallengeObjective.Type.COMPLETE_CHALLENGE,
                    (index, objective) -> {
                        if (definition.id().toString().equals(objective.value())) {
                            next.advance(index, 1);
                        }
                    });
            if (next.satisfies(candidate.objectives()) && !next.isCompleted()) {
                complete(player, profile, candidate, next, day);
            }
        }
    }

    /** Wipes the progress of rotating challenges whose period has rolled over. */
    public static void rotate(CulinaryProfile profile, ChallengeDefinition.Scope scope) {
        for (ChallengeDefinition definition : ChallengeRegistry.current().byScope(scope)) {
            profile.clearChallenge(definition.id());
        }
    }

    static CommandSourceStack rewardSource(ServerPlayer player) {
        return player.createCommandSourceStack().withPermission(2).withSuppressedOutput();
    }
}
