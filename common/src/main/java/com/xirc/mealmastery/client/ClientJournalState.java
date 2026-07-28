package com.xirc.mealmastery.client;

import com.xirc.mealmastery.culinary.LevelCurve;
import com.xirc.mealmastery.mastery.MasteryCurve;
import com.xirc.mealmastery.mastery.MasteryRank;
import com.xirc.mealmastery.network.Packets;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the client knows, and nothing more.
 *
 * <p>Pure data: no Minecraft client class is touched here, so the dedicated
 * server can safely load this class even though nothing on it ever will. The
 * screens read from it; the packet handler writes to it.</p>
 *
 * <p>This is a cache of what the server said, never a source of truth. Toggling
 * a favourite optimistically here would only be overwritten by the server's
 * echo, so it does not.</p>
 */
public final class ClientJournalState {

    private static final Map<ResourceLocation, Packets.DishView> CATALOGUE = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Packets.RecordView> RECORDS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, Packets.SyncDishDetail> DETAILS = new LinkedHashMap<>();
    private static final Deque<Packets.Notify> PENDING_NOTIFICATIONS = new ArrayDeque<>();

    private static volatile boolean catalogueComplete;
    private static volatile long cookingXp;
    private static volatile long mealsPrepared;
    private static volatile long mealsEaten;
    private static volatile long portionsServed;
    private static volatile int ingredientsDiscovered;
    private static volatile int cookingStreak;
    private static volatile int varietyStreak;
    private static volatile List<ResourceLocation> badges = List.of();
    private static volatile List<ResourceLocation> milestones = List.of();

    private static volatile List<Packets.ChallengeView> challenges = List.of();
    private static volatile List<Packets.CollectionView> collections = List.of();
    private static volatile List<Packets.IngredientView> ingredients = List.of();
    private static volatile List<Packets.MethodView> methods = List.of();
    private static volatile List<Packets.ActivityView> activity = List.of();
    private static volatile int totalIngredients;

    /** Whether the container screen currently open is a cooking surface. */
    private static volatile boolean menuCulinary;
    private static volatile ResourceLocation menuMethod;

    private static volatile LevelCurve levelCurve = LevelCurve.DEFAULT;
    private static volatile MasteryCurve masteryCurve = MasteryCurve.DEFAULT;
    private static volatile String masteryRewards = "COSMETIC_ONLY";
    private static volatile boolean leaderboardsEnabled;
    private static volatile Packets.SyncRules rules;

    private ClientJournalState() {
    }

    // ------------------------------------------------------------- catalogue

    public static synchronized void acceptCatalogue(Packets.SyncCatalogue packet) {
        if (packet.first()) {
            CATALOGUE.clear();
            DETAILS.clear();
            catalogueComplete = false;
        }
        for (Packets.DishView dish : packet.dishes()) {
            CATALOGUE.put(dish.target(), dish);
        }
        if (packet.last()) {
            catalogueComplete = true;
        }
    }

    public static boolean isCatalogueComplete() {
        return catalogueComplete;
    }

    public static synchronized List<Packets.DishView> dishes() {
        return List.copyOf(CATALOGUE.values());
    }

    public static synchronized Packets.DishView dish(ResourceLocation target) {
        return CATALOGUE.get(target);
    }

    public static synchronized int dishCount() {
        return CATALOGUE.size();
    }

    // --------------------------------------------------------------- profile

    public static synchronized void acceptProfile(Packets.SyncProfile packet) {
        if (packet.full()) {
            RECORDS.clear();
        }
        for (Packets.RecordView record : packet.records()) {
            RECORDS.put(record.target(), record);
        }
        cookingXp = packet.cookingXp();
        mealsPrepared = packet.mealsPrepared();
        mealsEaten = packet.mealsEaten();
        portionsServed = packet.portionsServed();
        ingredientsDiscovered = packet.ingredientsDiscovered();
        cookingStreak = packet.cookingStreak();
        varietyStreak = packet.varietyStreak();
        badges = List.copyOf(packet.badges());
        milestones = List.copyOf(packet.milestones());
    }

    public static synchronized Packets.RecordView record(ResourceLocation target) {
        return RECORDS.get(target);
    }

    public static synchronized List<Packets.RecordView> records() {
        return List.copyOf(RECORDS.values());
    }

    public static long cookingXp() {
        return cookingXp;
    }

    public static int cookingLevel() {
        return levelCurve.levelForXp(cookingXp);
    }

    public static long xpIntoLevel() {
        return levelCurve.xpIntoLevel(cookingXp);
    }

    public static long xpForCurrentLevel() {
        return levelCurve.xpToNextLevel(cookingXp);
    }

    public static long mealsPrepared() {
        return mealsPrepared;
    }

    public static long mealsEaten() {
        return mealsEaten;
    }

    public static long portionsServed() {
        return portionsServed;
    }

    public static int ingredientsDiscovered() {
        return ingredientsDiscovered;
    }

    public static int cookingStreak() {
        return cookingStreak;
    }

    public static int varietyStreak() {
        return varietyStreak;
    }

    public static List<ResourceLocation> badges() {
        return badges;
    }

    public static List<ResourceLocation> milestones() {
        return milestones;
    }

    public static synchronized int discoveredCount() {
        int count = 0;
        for (Packets.RecordView record : RECORDS.values()) {
            if (record.discovered()) {
                count++;
            }
        }
        return count;
    }

    public static synchronized int masteredCount() {
        int count = 0;
        for (Packets.RecordView record : RECORDS.values()) {
            if (masteryCurve.rankFor(record.masteryPoints()).isMastered()) {
                count++;
            }
        }
        return count;
    }

    public static synchronized List<ResourceLocation> pinned() {
        List<ResourceLocation> result = new ArrayList<>();
        RECORDS.forEach((target, record) -> {
            if (record.pinned()) {
                result.add(target);
            }
        });
        return result;
    }

    public static boolean isDiscovered(ResourceLocation target) {
        Packets.RecordView record = record(target);
        return record != null && record.discovered();
    }

    public static MasteryRank rank(ResourceLocation target) {
        Packets.RecordView record = record(target);
        return record == null ? MasteryRank.UNFAMILIAR : masteryCurve.rankFor(record.masteryPoints());
    }

    // ----------------------------------------------------------------- rules

    /** The server's bonus numbers, so tooltips can state real odds. */
    public static Packets.SyncRules rules() {
        return rules;
    }

    public static void acceptRules(Packets.SyncRules packet) {
        rules = packet;
        levelCurve = new LevelCurve(packet.levelBase(), packet.levelLinear(),
                packet.levelQuadratic(), packet.maxLevel());
        masteryCurve = new MasteryCurve(packet.masteryThresholds());
        masteryRewards = packet.masteryRewards();
        leaderboardsEnabled = packet.leaderboardsEnabled();
    }

    public static LevelCurve levelCurve() {
        return levelCurve;
    }

    public static MasteryCurve masteryCurve() {
        return masteryCurve;
    }

    public static String masteryRewards() {
        return masteryRewards;
    }

    public static boolean leaderboardsEnabled() {
        return leaderboardsEnabled;
    }

    // ---------------------------------------------------------------- extras

    public static void acceptExtras(Packets.SyncJournalExtras packet) {
        challenges = List.copyOf(packet.challenges());
        collections = List.copyOf(packet.collections());
        ingredients = List.copyOf(packet.ingredients());
        methods = List.copyOf(packet.methods());
        activity = List.copyOf(packet.activity());
        totalIngredients = packet.totalIngredients();
    }

    public static List<Packets.ChallengeView> challenges() {
        return challenges;
    }

    public static List<Packets.CollectionView> collections() {
        return collections;
    }

    public static List<Packets.IngredientView> ingredients() {
        return ingredients;
    }

    public static List<Packets.MethodView> methods() {
        return methods;
    }

    public static int totalIngredients() {
        return totalIngredients;
    }

    public static List<Packets.ActivityView> activity() {
        return activity;
    }

    public static void acceptMenuContext(Packets.MenuContext context) {
        menuCulinary = context.culinary();
        menuMethod = context.method();
    }

    public static boolean isMenuCulinary() {
        return menuCulinary;
    }

    public static ResourceLocation menuMethod() {
        return menuMethod;
    }

    // ---------------------------------------------------------------- detail

    public static synchronized void acceptDetail(Packets.SyncDishDetail detail) {
        DETAILS.put(detail.target(), detail);
    }

    public static synchronized Packets.SyncDishDetail detail(ResourceLocation target) {
        return DETAILS.get(target);
    }

    // --------------------------------------------------------- notifications

    public static synchronized void queueNotification(Packets.Notify notify) {
        // Bounded so a burst of XP gains cannot grow the queue without limit.
        if (PENDING_NOTIFICATIONS.size() < 64) {
            PENDING_NOTIFICATIONS.addLast(notify);
        }
    }

    public static synchronized Packets.Notify pollNotification() {
        return PENDING_NOTIFICATIONS.pollFirst();
    }

    public static synchronized List<Packets.Notify> drainNotifications() {
        List<Packets.Notify> drained = new ArrayList<>(PENDING_NOTIFICATIONS);
        PENDING_NOTIFICATIONS.clear();
        return Collections.unmodifiableList(drained);
    }

    /** Called on disconnect so a second world never inherits the first one's journal. */
    public static synchronized void clear() {
        CATALOGUE.clear();
        RECORDS.clear();
        DETAILS.clear();
        PENDING_NOTIFICATIONS.clear();
        catalogueComplete = false;
        cookingXp = 0L;
        mealsPrepared = 0L;
        mealsEaten = 0L;
        portionsServed = 0L;
        ingredientsDiscovered = 0;
        cookingStreak = 0;
        varietyStreak = 0;
        badges = List.of();
        milestones = List.of();
        challenges = List.of();
        collections = List.of();
        ingredients = List.of();
        methods = List.of();
        activity = List.of();
        totalIngredients = 0;
        menuCulinary = false;
        menuMethod = null;
        levelCurve = LevelCurve.DEFAULT;
        masteryCurve = MasteryCurve.DEFAULT;
        masteryRewards = "COSMETIC_ONLY";
        leaderboardsEnabled = false;
        rules = null;
    }
}
