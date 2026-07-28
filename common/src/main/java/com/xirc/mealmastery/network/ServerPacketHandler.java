package com.xirc.mealmastery.network;

import com.xirc.mealmastery.challenge.ChallengeDefinition;
import com.xirc.mealmastery.challenge.ChallengeRegistry;
import com.xirc.mealmastery.challenge.ChallengeState;
import com.xirc.mealmastery.collection.CollectionDefinition;
import com.xirc.mealmastery.collection.CollectionRegistry;
import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.config.ServerConfig;
import com.xirc.mealmastery.culinary.CulinaryProfile;
import com.xirc.mealmastery.culinary.CulinaryTitle;
import com.xirc.mealmastery.culinary.MealRecord;
import com.xirc.mealmastery.culinary.ProfileManager;
import com.xirc.mealmastery.event.CulinaryEvents;
import com.xirc.mealmastery.mastery.MasteryCurve;
import com.xirc.mealmastery.recipe.CookingMethod;
import com.xirc.mealmastery.recipe.CulinaryRegistries;
import com.xirc.mealmastery.recipe.CulinaryRegistry;
import com.xirc.mealmastery.recipe.MealEntry;
import com.xirc.mealmastery.recipe.RecipeEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

/**
 * Everything the server does with a packet from a client, and everything it
 * pushes out on its own.
 *
 * <p>The client-to-server surface is deliberately tiny and none of it can claim
 * progress: two data requests and three personal preferences, each re-checked
 * against the server's own registry before anything is stored.</p>
 */
public final class ServerPacketHandler {

    private ServerPacketHandler() {
    }

    public static void install() {
        CulinaryEvents.RECIPE_DISCOVERED.register(event -> Network.toPlayer(event.player(),
                new Packets.Notify(Packets.Notify.Kind.DISCOVERY, event.meal(), 0L)));
        CulinaryEvents.MASTERY_RANK_CHANGED.register(event -> Network.toPlayer(event.player(),
                new Packets.Notify(event.to().isMastered()
                        ? Packets.Notify.Kind.MASTERED : Packets.Notify.Kind.MASTERY_RANK,
                        event.meal(), event.to().ordinal())));
        CulinaryEvents.COOKING_LEVEL_CHANGED.register(event -> Network.toPlayer(event.player(),
                new Packets.Notify(Packets.Notify.Kind.LEVEL_UP, null, event.to())));
        CulinaryEvents.CHALLENGE_COMPLETED.register(event -> Network.toPlayer(event.player(),
                new Packets.Notify(Packets.Notify.Kind.CHALLENGE, event.challenge(), 0L)));
        CulinaryEvents.MILESTONE_REACHED.register(event -> Network.toPlayer(event.player(),
                new Packets.Notify(Packets.Notify.Kind.MILESTONE, event.milestone(), 0L)));
        CulinaryEvents.COOKING_XP_GAINED.register(event -> Network.toPlayer(event.player(),
                new Packets.Notify(Packets.Notify.Kind.XP, event.reason(), event.amount())));

        // Any preparation changes at least one record, so the client is kept in
        // step with a delta rather than a fresh full profile.
        CulinaryEvents.MEAL_PREPARED.register(event ->
                sendRecordDelta(event.player(), event.profile(), event.meal()));
        CulinaryEvents.MEAL_EATEN.register(event ->
                sendRecordDelta(event.player(), event.profile(), event.meal()));
    }

    public static void handle(ServerPlayer player, MealMasteryPacket packet) {
        if (packet instanceof Packets.RequestProfile) {
            sendEverything(player);
        } else if (packet instanceof Packets.RequestDishDetail request) {
            sendDishDetail(player, request.target());
        } else if (packet instanceof Packets.SetPreference preference) {
            applyPreference(player, preference);
        }
    }

    // ------------------------------------------------------------- outgoing

    /** Sent on join and whenever the journal asks for a refresh. */
    public static void sendEverything(ServerPlayer player) {
        sendRules(player);
        sendCatalogue(player);
        sendProfile(player);
        sendExtras(player);
    }

    /**
     * Challenges, collections, the ingredient journal and method statistics.
     *
     * <p>All of these mix definitions with the player's own progress, so they
     * are computed here and sent as finished numbers rather than shipping the
     * definitions and letting the client work them out.</p>
     */
    public static void sendExtras(ServerPlayer player) {
        ProfileManager profiles = ProfileManager.get();
        if (profiles == null) {
            return;
        }
        CulinaryProfile profile = profiles.of(player);
        ServerConfig config = ConfigManager.server();
        MasteryCurve masteryCurve = config.mastery.toCurve();
        CulinaryRegistry registry = CulinaryRegistries.current();

        List<Packets.ChallengeView> challenges = new ArrayList<>();
        for (ChallengeDefinition definition : ChallengeRegistry.current().all()) {
            ChallengeState state = profile.peekChallenge(definition.id());
            boolean completed = state != null && state.isCompleted();
            if (definition.hidden() && !completed) {
                continue;
            }
            challenges.add(new Packets.ChallengeView(definition.id(), definition.titleKey(),
                    definition.descriptionKey(), completed,
                    state == null ? 0 : state.progress(0),
                    definition.objectives().get(0).amount()));
        }

        List<Packets.CollectionView> collections = new ArrayList<>();
        for (CollectionDefinition definition : CollectionRegistry.current().defined()) {
            CollectionDefinition.Progress progress =
                    definition.progress(registry, profile, masteryCurve);
            collections.add(new Packets.CollectionView(definition.id(), definition.titleKey(),
                    false, definition.icon(), progress.total(), progress.discovered(),
                    progress.mastered()));
        }
        // Per-mod collections are generated rather than authored, so their
        // titles are already display text and must not be translated.
        for (CollectionRegistry.ModCollection mod : CollectionRegistry.current().modCollections()) {
            int discovered = 0;
            int mastered = 0;
            for (ResourceLocation member : mod.members()) {
                if (profile.isDiscovered(member)) {
                    discovered++;
                }
                if (profile.rankOf(member, masteryCurve).isMastered()) {
                    mastered++;
                }
            }
            collections.add(new Packets.CollectionView(
                    ResourceLocation.fromNamespaceAndPath(mod.modId(), "mod_collection"),
                    mod.displayName(), true,
                    mod.members().isEmpty() ? null : mod.members().get(0),
                    mod.members().size(), discovered, mastered));
        }

        List<Packets.IngredientView> ingredients = new ArrayList<>();
        profile.ingredients().forEach((item, record) -> ingredients.add(new Packets.IngredientView(
                item, record.timesUsed(), record.firstUsedDay(), record.usedInMeals().size(),
                record.methods().stream().map(CookingMethod::id).toList())));

        List<Packets.MethodView> methods = new ArrayList<>();
        profile.methods().forEach((method, record) -> {
            int mastered = 0;
            ResourceLocation mostPrepared = null;
            long best = 0L;
            for (ResourceLocation meal : record.uniqueMeals()) {
                if (profile.rankOf(meal, masteryCurve).isMastered()) {
                    mastered++;
                }
                MealRecord mealRecord = profile.peekMeal(meal);
                if (mealRecord != null && mealRecord.prepared() > best) {
                    best = mealRecord.prepared();
                    mostPrepared = meal;
                }
            }
            methods.add(new Packets.MethodView(method.id(), record.preparations(),
                    record.uniqueMealCount(), mastered, mostPrepared));
        });

        // The activity feed is capped here rather than client-side: there is no
        // reason to ship history the journal will never draw.
        List<Packets.ActivityView> activity = new ArrayList<>();
        for (com.xirc.mealmastery.culinary.ActivityEntry entry : profile.activity()) {
            if (activity.size() >= 16) {
                break;
            }
            activity.add(new Packets.ActivityView(entry.type().id(), entry.subject(),
                    entry.detail(), entry.day()));
        }

        Network.toPlayer(player, new Packets.SyncJournalExtras(challenges, collections,
                ingredients, methods, activity, registry.allIngredients().size()));
    }

    public static void sendRules(ServerPlayer player) {
        ServerConfig config = ConfigManager.server();
        Network.toPlayer(player, new Packets.SyncRules(
                config.progression.levelCurveBase,
                config.progression.levelCurveLinear,
                config.progression.levelCurveQuadratic,
                config.progression.maxLevel,
                config.mastery.thresholds,
                config.mastery.rewards.name(),
                config.multiplayer.leaderboardsEnabled,
                config.mastery.bonuses.enabled && config.mastery.enabled,
                config.mastery.bonuses.effectChanceAtMaxRank,
                config.mastery.bonuses.perfectChanceAtMaxRank,
                config.mastery.bonuses.extraPortionChanceAtMaxRank,
                config.mastery.bonuses.cookingSpeedAtMaxRank,
                config.mastery.bonuses.saturationPerRank,
                config.mastery.bonuses.effectDurationSeconds));
    }

    /**
     * The dish catalogue, chunked. A thousand-dish modpack is several hundred
     * kilobytes, so it is split rather than sent as one oversized packet.
     */
    public static void sendCatalogue(ServerPlayer player) {
        CulinaryRegistry registry = CulinaryRegistries.current();
        int pageSize = Math.max(8, ConfigManager.server().advanced.journalPageSize);

        List<MealEntry> entries = registry.entries();
        if (entries.isEmpty()) {
            Network.toPlayer(player, new Packets.SyncCatalogue(true, true, List.of()));
            return;
        }
        for (int start = 0; start < entries.size(); start += pageSize) {
            int end = Math.min(entries.size(), start + pageSize);
            List<Packets.DishView> page = new ArrayList<>(end - start);
            for (MealEntry entry : entries.subList(start, end)) {
                page.add(new Packets.DishView(
                        entry.target(),
                        entry.sourceModId(),
                        entry.category().id(),
                        entry.methods().stream().map(CookingMethod::id).toList(),
                        entry.nutrition(),
                        entry.saturation()));
            }
            Network.toPlayer(player, new Packets.SyncCatalogue(start == 0, end == entries.size(), page));
        }
    }

    public static void sendProfile(ServerPlayer player) {
        ProfileManager profiles = ProfileManager.get();
        if (profiles == null) {
            return;
        }
        CulinaryProfile profile = profiles.of(player);
        List<Packets.RecordView> records = new ArrayList<>();
        profile.meals().forEach((target, record) -> records.add(view(target, record)));

        Network.toPlayer(player, new Packets.SyncProfile(
                true,
                profile.cookingXp(),
                profile.stats().mealsPrepared(),
                profile.stats().mealsEaten(),
                profile.stats().portionsServed(),
                profile.ingredientsDiscoveredCount(),
                profile.streaks().cookingStreak(),
                profile.streaks().varietyStreak(),
                records,
                List.copyOf(profile.badges()),
                List.copyOf(profile.milestones())));
    }

    private static void sendRecordDelta(ServerPlayer player, CulinaryProfile profile,
                                        ResourceLocation target) {
        MealRecord record = profile.peekMeal(target);
        if (record == null) {
            return;
        }
        Network.toPlayer(player, new Packets.SyncProfile(
                false,
                profile.cookingXp(),
                profile.stats().mealsPrepared(),
                profile.stats().mealsEaten(),
                profile.stats().portionsServed(),
                profile.ingredientsDiscoveredCount(),
                profile.streaks().cookingStreak(),
                profile.streaks().varietyStreak(),
                List.of(view(target, record)),
                List.copyOf(profile.badges()),
                List.copyOf(profile.milestones())));
    }

    private static Packets.RecordView view(ResourceLocation target, MealRecord record) {
        return new Packets.RecordView(target, record.isDiscovered(), record.prepared(),
                record.eaten(), record.served(), record.masteryPoints(), record.isFavorite(),
                record.isPinned(), record.firstPreparedDay(), record.lastPreparedDay());
    }

    private static void sendDishDetail(ServerPlayer player, ResourceLocation target) {
        MealEntry entry = CulinaryRegistries.current().entry(target);
        if (entry == null) {
            return;
        }
        Network.toPlayer(player, new Packets.SyncDishDetail(
                target,
                List.copyOf(entry.ingredients()),
                entry.recipes().stream().map(RecipeEntry::recipeId).toList()));
    }

    // ------------------------------------------------------------- incoming

    private static void applyPreference(ServerPlayer player, Packets.SetPreference preference) {
        ProfileManager profiles = ProfileManager.get();
        if (profiles == null || preference.target() == null) {
            return;
        }
        CulinaryProfile profile = profiles.of(player);

        switch (preference.kind()) {
            case FAVORITE -> {
                // A client may not favourite something this server cannot make.
                if (!CulinaryRegistries.current().isTracked(preference.target())) {
                    return;
                }
                profile.toggleFavorite(preference.target());
                sendRecordDelta(player, profile, preference.target());
            }
            case PIN -> {
                if (!CulinaryRegistries.current().isTracked(preference.target())) {
                    return;
                }
                profile.togglePin(preference.target(),
                        ConfigManager.server().advanced.maxPinnedRecipes);
                sendProfile(player);
            }
            case SIGNATURE -> {
                // A signature has to be earned: only a mastered dish qualifies,
                // so this cannot be used to hand yourself the extra star.
                if (profile.rankOf(preference.target(),
                        ConfigManager.server().mastery.toCurve()).isMastered()) {
                    profile.setSignatureDish(preference.target());
                    sendProfile(player);
                }
            }
            case TITLE -> {
                ServerConfig config = ConfigManager.server();
                boolean unlocked = CulinaryTitle.unlockedFor(profile,
                        config.progression.toCurve(), config.mastery.toCurve()).stream()
                        .anyMatch(title -> title.id().equals(preference.target()));
                if (unlocked) {
                    profile.setSelectedTitle(preference.target());
                }
            }
        }
    }
}
