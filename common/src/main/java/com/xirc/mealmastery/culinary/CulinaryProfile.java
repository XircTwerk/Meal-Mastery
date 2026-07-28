package com.xirc.mealmastery.culinary;

import com.xirc.mealmastery.Constants;
import com.xirc.mealmastery.challenge.ChallengeState;
import com.xirc.mealmastery.data.NbtUtil;
import com.xirc.mealmastery.mastery.MasteryCurve;
import com.xirc.mealmastery.mastery.MasteryRank;
import com.xirc.mealmastery.recipe.CookingMethod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * One player's entire culinary history.
 *
 * <p>This is the aggregate root: it owns the per-dish records, the ingredient
 * and method journals, lifetime statistics, records and streaks, and it is the
 * only thing persisted per player. It is <em>server-authoritative</em> — the
 * client receives projections of it and never sends one back.</p>
 *
 * <p>Unrecognised top-level sections are round-tripped untouched so a profile
 * written by a newer build survives a downgrade instead of being silently
 * truncated.</p>
 */
public final class CulinaryProfile {
    /** Recent Activity is capped; the cap itself is client configuration. */
    public static final int MAX_ACTIVITY_ENTRIES = 64;

    private static final Set<String> KNOWN_SECTIONS = Set.of(
            "schemaVersion", "cookingXp", "meals", "ingredients", "methods",
            "stats", "records", "streaks", "activity", "pinned", "title",
            "challenges", "badges", "milestones", "collections", "signature");

    private final UUID playerId;

    private int schemaVersion = Constants.PROFILE_SCHEMA_VERSION;
    private long cookingXp;

    private final Map<ResourceLocation, MealRecord> meals = new LinkedHashMap<>();
    private final Map<ResourceLocation, IngredientRecord> ingredients = new LinkedHashMap<>();
    private final Map<CookingMethod, MethodRecord> methods = new LinkedHashMap<>();

    private CulinaryStats stats = new CulinaryStats();
    private PersonalRecords records = new PersonalRecords();
    private StreakState streaks = new StreakState();

    private final Deque<ActivityEntry> activity = new ArrayDeque<>();
    /** Ordered because the HUD tracker renders pins in the order they were added. */
    private final List<ResourceLocation> pinned = new ArrayList<>();
    private ResourceLocation selectedTitle;
    private ResourceLocation signatureDish;

    private final Map<ResourceLocation, ChallengeState> challenges = new LinkedHashMap<>();
    private final Set<ResourceLocation> badges = new LinkedHashSet<>();
    private final Set<ResourceLocation> milestones = new LinkedHashSet<>();
    private final Set<ResourceLocation> completedCollections = new LinkedHashSet<>();

    private final CompoundTag preservedSections = new CompoundTag();

    private transient boolean dirty;

    public CulinaryProfile(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID playerId() {
        return playerId;
    }

    public int schemaVersion() {
        return schemaVersion;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markDirty() {
        dirty = true;
    }

    public void clearDirty() {
        dirty = false;
    }

    // ------------------------------------------------------------ progression

    public long cookingXp() {
        return cookingXp;
    }

    public int cookingLevel(LevelCurve curve) {
        return curve.levelForXp(cookingXp);
    }

    /**
     * @return the level after the grant, so callers can detect a level-up by
     *         comparing against the level they read before calling
     */
    public int addCookingXp(long amount, LevelCurve curve) {
        if (amount > 0L) {
            cookingXp += amount;
            markDirty();
        }
        return curve.levelForXp(cookingXp);
    }

    public void setCookingXp(long amount) {
        cookingXp = Math.max(0L, amount);
        markDirty();
    }

    // ---------------------------------------------------------------- meals

    /** Returns the existing record without creating one; may be {@code null}. */
    public MealRecord peekMeal(ResourceLocation target) {
        return meals.get(target);
    }

    public MealRecord meal(ResourceLocation target) {
        return meals.computeIfAbsent(target, MealRecord::new);
    }

    public Map<ResourceLocation, MealRecord> meals() {
        return Collections.unmodifiableMap(meals);
    }

    public boolean isDiscovered(ResourceLocation target) {
        MealRecord record = meals.get(target);
        return record != null && record.isDiscovered();
    }

    public int discoveredCount() {
        int count = 0;
        for (MealRecord record : meals.values()) {
            if (record.isDiscovered()) {
                count++;
            }
        }
        return count;
    }

    public int masteredCount(MasteryCurve curve) {
        int count = 0;
        for (MealRecord record : meals.values()) {
            if (record.rank(curve).isMastered()) {
                count++;
            }
        }
        return count;
    }

    public int uniquePreparedCount() {
        int count = 0;
        for (MealRecord record : meals.values()) {
            if (record.hasEverBeenPrepared()) {
                count++;
            }
        }
        return count;
    }

    // ---------------------------------------------------------- ingredients

    public IngredientRecord ingredient(ResourceLocation item) {
        return ingredients.computeIfAbsent(item, IngredientRecord::new);
    }

    public IngredientRecord peekIngredient(ResourceLocation item) {
        return ingredients.get(item);
    }

    public Map<ResourceLocation, IngredientRecord> ingredients() {
        return Collections.unmodifiableMap(ingredients);
    }

    public int ingredientsDiscoveredCount() {
        return ingredients.size();
    }

    // -------------------------------------------------------------- methods

    public MethodRecord method(CookingMethod method) {
        return methods.computeIfAbsent(method, MethodRecord::new);
    }

    public MethodRecord peekMethod(CookingMethod method) {
        return methods.get(method);
    }

    public Map<CookingMethod, MethodRecord> methods() {
        return Collections.unmodifiableMap(methods);
    }

    // ------------------------------------------------------- stats & records

    public CulinaryStats stats() {
        return stats;
    }

    public PersonalRecords records() {
        return records;
    }

    public StreakState streaks() {
        return streaks;
    }

    // ------------------------------------------------------------- activity

    public void pushActivity(ActivityEntry entry) {
        activity.addFirst(entry);
        while (activity.size() > MAX_ACTIVITY_ENTRIES) {
            activity.removeLast();
        }
        markDirty();
    }

    /** Most recent first. */
    public List<ActivityEntry> activity() {
        return List.copyOf(activity);
    }

    // ------------------------------------------------- favourites and pins

    public List<ResourceLocation> favorites() {
        List<ResourceLocation> result = new ArrayList<>();
        meals.forEach((id, record) -> {
            if (record.isFavorite()) {
                result.add(id);
            }
        });
        return result;
    }

    public boolean toggleFavorite(ResourceLocation target) {
        MealRecord record = meal(target);
        record.setFavorite(!record.isFavorite());
        markDirty();
        return record.isFavorite();
    }

    public List<ResourceLocation> pinned() {
        return Collections.unmodifiableList(pinned);
    }

    /**
     * @param limit maximum simultaneous pins allowed by client configuration
     * @return {@code true} if the recipe is pinned afterwards
     */
    public boolean togglePin(ResourceLocation target, int limit) {
        MealRecord record = meal(target);
        markDirty();
        if (pinned.remove(target)) {
            record.setPinned(false);
            return false;
        }
        pinned.add(target);
        record.setPinned(true);
        while (pinned.size() > Math.max(1, limit)) {
            ResourceLocation dropped = pinned.remove(0);
            MealRecord droppedRecord = meals.get(dropped);
            if (droppedRecord != null) {
                droppedRecord.setPinned(false);
            }
        }
        return true;
    }

    public ResourceLocation selectedTitle() {
        return selectedTitle;
    }

    /** The one dish this cook is known for; plates a star above the rest. */
    public ResourceLocation signatureDish() {
        return signatureDish;
    }

    public void setSignatureDish(ResourceLocation dish) {
        this.signatureDish = dish;
        markDirty();
    }

    public void setSelectedTitle(ResourceLocation title) {
        this.selectedTitle = title;
        markDirty();
    }

    // --------------------------------------- challenges, badges, milestones

    public ChallengeState challenge(ResourceLocation id, int objectiveCount) {
        return challenges.computeIfAbsent(id, unused -> new ChallengeState(objectiveCount));
    }

    public ChallengeState peekChallenge(ResourceLocation id) {
        return challenges.get(id);
    }

    public Map<ResourceLocation, ChallengeState> challenges() {
        return Collections.unmodifiableMap(challenges);
    }

    public boolean hasCompletedChallenge(ResourceLocation id) {
        ChallengeState state = challenges.get(id);
        return state != null && state.isCompleted();
    }

    /** Drops a rotating challenge's progress when its period rolls over. */
    public void clearChallenge(ResourceLocation id) {
        challenges.remove(id);
        markDirty();
    }

    public Set<ResourceLocation> badges() {
        return Collections.unmodifiableSet(badges);
    }

    /** @return {@code true} when the badge is newly earned */
    public boolean awardBadge(ResourceLocation badge) {
        if (badge == null || !badges.add(badge)) {
            return false;
        }
        markDirty();
        return true;
    }

    public Set<ResourceLocation> milestones() {
        return Collections.unmodifiableSet(milestones);
    }

    public boolean reachMilestone(ResourceLocation milestone) {
        if (milestone == null || !milestones.add(milestone)) {
            return false;
        }
        markDirty();
        return true;
    }

    public Set<ResourceLocation> completedCollections() {
        return Collections.unmodifiableSet(completedCollections);
    }

    public boolean completeCollection(ResourceLocation collection) {
        if (collection == null || !completedCollections.add(collection)) {
            return false;
        }
        markDirty();
        return true;
    }

    // ------------------------------------------------------- orphaned data

    /**
     * Records whose dish no longer exists in the current registry.
     *
     * <p>They stay on disk untouched so removing an addon for one session does
     * not destroy the progress it earned; they are simply hidden from the
     * journal until the addon returns.</p>
     */
    public List<ResourceLocation> orphanedTargets(Collection<ResourceLocation> knownTargets) {
        List<ResourceLocation> orphans = new ArrayList<>();
        for (ResourceLocation id : meals.keySet()) {
            if (!knownTargets.contains(id)) {
                orphans.add(id);
            }
        }
        return orphans;
    }

    public int purgeOrphans(Collection<ResourceLocation> knownTargets) {
        List<ResourceLocation> orphans = orphanedTargets(knownTargets);
        for (ResourceLocation id : orphans) {
            meals.remove(id);
            pinned.remove(id);
        }
        if (!orphans.isEmpty()) {
            markDirty();
        }
        return orphans.size();
    }

    // ---------------------------------------------------------------- resets

    public enum ResetScope {
        LEVEL, MASTERY, DISCOVERY, STATISTICS, ACTIVITY, CHALLENGES, EVERYTHING
    }

    public void reset(ResetScope scope) {
        switch (scope) {
            case LEVEL -> cookingXp = 0L;
            case MASTERY -> meals.values().forEach(record -> {
                CompoundTag saved = record.save();
                saved.remove("masteryPoints");
                saved.remove("announcedRank");
                meals.put(record.target(), MealRecord.load(record.target(), saved));
            });
            case DISCOVERY -> meals.values().forEach(record -> {
                CompoundTag saved = record.save();
                saved.remove("discovered");
                saved.remove("discoveredDay");
                meals.put(record.target(), MealRecord.load(record.target(), saved));
            });
            case STATISTICS -> {
                stats.reset();
                records = new PersonalRecords();
                streaks = new StreakState();
            }
            case ACTIVITY -> activity.clear();
            case CHALLENGES -> {
                challenges.clear();
                badges.clear();
                milestones.clear();
                completedCollections.clear();
            }
            case EVERYTHING -> {
                cookingXp = 0L;
                meals.clear();
                ingredients.clear();
                methods.clear();
                stats = new CulinaryStats();
                records = new PersonalRecords();
                streaks = new StreakState();
                activity.clear();
                pinned.clear();
                selectedTitle = null;
                signatureDish = null;
                challenges.clear();
                badges.clear();
                milestones.clear();
                completedCollections.clear();
            }
        }
        markDirty();
    }

    // --------------------------------------------------------- serialisation

    public CompoundTag save() {
        CompoundTag tag = preservedSections.copy();
        tag.putInt("schemaVersion", Constants.PROFILE_SCHEMA_VERSION);
        NbtUtil.putIfNonZero(tag, "cookingXp", cookingXp);
        tag.put("meals", NbtUtil.writeMap(meals, MealRecord::save));
        tag.put("ingredients", NbtUtil.writeMap(ingredients, IngredientRecord::save));

        CompoundTag methodTag = new CompoundTag();
        methods.forEach((method, record) -> {
            CompoundTag entry = record.save();
            if (!entry.isEmpty()) {
                methodTag.put(method.id().toString(), entry);
            }
        });
        tag.put("methods", methodTag);

        tag.put("stats", stats.save());
        tag.put("records", records.save());
        tag.put("streaks", streaks.save());

        if (!activity.isEmpty()) {
            ListTag activityTag = new ListTag();
            activity.forEach(entry -> activityTag.add(entry.save()));
            tag.put("activity", activityTag);
        }
        if (!pinned.isEmpty()) {
            tag.put("pinned", NbtUtil.writeIds(pinned));
        }
        NbtUtil.putIfPresent(tag, "title", selectedTitle);
        NbtUtil.putIfPresent(tag, "signature", signatureDish);

        if (!challenges.isEmpty()) {
            tag.put("challenges", NbtUtil.writeMap(challenges, ChallengeState::save));
        }
        if (!badges.isEmpty()) {
            tag.put("badges", NbtUtil.writeIds(badges));
        }
        if (!milestones.isEmpty()) {
            tag.put("milestones", NbtUtil.writeIds(milestones));
        }
        if (!completedCollections.isEmpty()) {
            tag.put("collections", NbtUtil.writeIds(completedCollections));
        }
        return tag;
    }

    public static CulinaryProfile load(UUID playerId, CompoundTag tag) {
        CulinaryProfile profile = new CulinaryProfile(playerId);
        profile.schemaVersion = tag.contains("schemaVersion")
                ? tag.getInt("schemaVersion") : Constants.PROFILE_SCHEMA_VERSION;
        profile.cookingXp = tag.getLong("cookingXp");

        NbtUtil.readMap(tag, "meals", profile.meals, MealRecord::load);
        NbtUtil.readMap(tag, "ingredients", profile.ingredients, IngredientRecord::load);

        if (tag.contains("methods", Tag.TAG_COMPOUND)) {
            CompoundTag methodTag = tag.getCompound("methods");
            for (String rawId : methodTag.getAllKeys()) {
                ResourceLocation id = ResourceLocation.tryParse(rawId);
                if (id == null) {
                    continue;
                }
                CookingMethod method = CookingMethod.of(id);
                profile.methods.put(method, MethodRecord.load(method, methodTag.getCompound(rawId)));
            }
        }

        profile.stats = CulinaryStats.load(tag.getCompound("stats"));
        profile.records = PersonalRecords.load(tag.getCompound("records"));
        profile.streaks = StreakState.load(tag.getCompound("streaks"));

        if (tag.contains("activity", Tag.TAG_LIST)) {
            ListTag activityTag = tag.getList("activity", Tag.TAG_COMPOUND);
            for (int i = 0; i < activityTag.size() && i < MAX_ACTIVITY_ENTRIES; i++) {
                profile.activity.addLast(ActivityEntry.load(activityTag.getCompound(i)));
            }
        }
        NbtUtil.forEachId(tag, "pinned", profile.pinned::add);
        profile.selectedTitle = NbtUtil.readId(tag, "title");
        profile.signatureDish = NbtUtil.readId(tag, "signature");

        NbtUtil.readMap(tag, "challenges", profile.challenges,
                (id, entry) -> ChallengeState.load(entry, 0));
        NbtUtil.forEachId(tag, "badges", profile.badges::add);
        NbtUtil.forEachId(tag, "milestones", profile.milestones::add);
        NbtUtil.forEachId(tag, "collections", profile.completedCollections::add);

        for (String key : tag.getAllKeys()) {
            if (!KNOWN_SECTIONS.contains(key)) {
                profile.preservedSections.put(key, tag.get(key).copy());
            }
        }
        return profile;
    }

    /** Convenience for the journal header and the {@code /mealmastery stats} output. */
    public MasteryRank rankOf(ResourceLocation target, MasteryCurve curve) {
        MealRecord record = meals.get(target);
        return record == null ? MasteryRank.UNFAMILIAR : record.rank(curve);
    }
}
