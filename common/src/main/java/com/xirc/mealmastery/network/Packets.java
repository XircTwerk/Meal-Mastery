package com.xirc.mealmastery.network;

import com.xirc.mealmastery.Constants;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Every packet Meal Mastery sends.
 *
 * <p>The split follows the client is given the smallest projection that
 * lets it draw a screen, never a copy of the server's databases. The dish
 * catalogue is sent once in chunks and cached; per-dish detail is pulled on
 * demand; profile updates after the initial sync are deltas.</p>
 *
 * <p>Nothing travelling client to server can assert progression — the only
 * client-to-server packets are a data request and three personal preferences,
 * all of which the server re-validates.</p>
 */
public final class Packets {

    private Packets() {
    }

    private static ResourceLocation packetId(String path) {
        return ResourceLocation.fromNamespaceAndPath(Constants.MOD_ID, path);
    }

    // ------------------------------------------------------- server to client

    /** One dish, as the recipe browser needs it. */
    public record DishView(ResourceLocation target, String sourceModId, ResourceLocation category,
                           List<ResourceLocation> methods, int nutrition, float saturation) {

        public DishView {
            methods = List.copyOf(methods);
        }

        void write(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(target);
            buffer.writeUtf(sourceModId, 64);
            buffer.writeResourceLocation(category);
            buffer.writeVarInt(methods.size());
            methods.forEach(buffer::writeResourceLocation);
            buffer.writeVarInt(nutrition);
            buffer.writeFloat(saturation);
        }

        static DishView read(FriendlyByteBuf buffer) {
            ResourceLocation target = buffer.readResourceLocation();
            String modId = buffer.readUtf(64);
            ResourceLocation category = buffer.readResourceLocation();
            int methodCount = buffer.readVarInt();
            List<ResourceLocation> methods = new ArrayList<>(methodCount);
            for (int i = 0; i < methodCount; i++) {
                methods.add(buffer.readResourceLocation());
            }
            return new DishView(target, modId, category, methods,
                    buffer.readVarInt(), buffer.readFloat());
        }
    }

    /**
     * A slice of the dish catalogue.
     *
     * @param first whether the client should discard whatever it had cached;
     *              set on the first chunk of a sync so a datapack reload
     *              replaces the catalogue rather than appending to it
     */
    public record SyncCatalogue(boolean first, boolean last, List<DishView> dishes)
            implements MealMasteryPacket {

        public static final ResourceLocation ID = packetId("sync_catalogue");

        public SyncCatalogue {
            dishes = List.copyOf(dishes);
        }

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeBoolean(first);
            buffer.writeBoolean(last);
            buffer.writeVarInt(dishes.size());
            dishes.forEach(dish -> dish.write(buffer));
        }

        public static SyncCatalogue read(FriendlyByteBuf buffer) {
            boolean first = buffer.readBoolean();
            boolean last = buffer.readBoolean();
            int count = buffer.readVarInt();
            List<DishView> dishes = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                dishes.add(DishView.read(buffer));
            }
            return new SyncCatalogue(first, last, dishes);
        }
    }

    /** One dish's entry in the player's own history. */
    public record RecordView(ResourceLocation target, boolean discovered, long prepared, long eaten,
                             long served, long masteryPoints, boolean favorite, boolean pinned,
                             long firstPreparedDay, long lastPreparedDay) {

        void write(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(target);
            byte flags = (byte) ((discovered ? 1 : 0) | (favorite ? 2 : 0) | (pinned ? 4 : 0));
            buffer.writeByte(flags);
            buffer.writeVarLong(Math.max(0L, prepared));
            buffer.writeVarLong(Math.max(0L, eaten));
            buffer.writeVarLong(Math.max(0L, served));
            buffer.writeVarLong(Math.max(0L, masteryPoints));
            buffer.writeLong(firstPreparedDay);
            buffer.writeLong(lastPreparedDay);
        }

        static RecordView read(FriendlyByteBuf buffer) {
            ResourceLocation target = buffer.readResourceLocation();
            byte flags = buffer.readByte();
            return new RecordView(target, (flags & 1) != 0,
                    buffer.readVarLong(), buffer.readVarLong(), buffer.readVarLong(),
                    buffer.readVarLong(), (flags & 2) != 0, (flags & 4) != 0,
                    buffer.readLong(), buffer.readLong());
        }
    }

    /**
     * The player's own profile.
     *
     * @param full when false this is a delta and {@code records} only contains
     *             what changed
     */
    public record SyncProfile(boolean full, long cookingXp, long mealsPrepared, long mealsEaten,
                              long portionsServed, int ingredientsDiscovered, int cookingStreak,
                              int varietyStreak, List<RecordView> records,
                              List<ResourceLocation> badges, List<ResourceLocation> milestones)
            implements MealMasteryPacket {

        public static final ResourceLocation ID = packetId("sync_profile");

        public SyncProfile {
            records = List.copyOf(records);
            badges = List.copyOf(badges);
            milestones = List.copyOf(milestones);
        }

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeBoolean(full);
            buffer.writeVarLong(Math.max(0L, cookingXp));
            buffer.writeVarLong(Math.max(0L, mealsPrepared));
            buffer.writeVarLong(Math.max(0L, mealsEaten));
            buffer.writeVarLong(Math.max(0L, portionsServed));
            buffer.writeVarInt(ingredientsDiscovered);
            buffer.writeVarInt(cookingStreak);
            buffer.writeVarInt(varietyStreak);
            buffer.writeVarInt(records.size());
            records.forEach(record -> record.write(buffer));
            buffer.writeVarInt(badges.size());
            badges.forEach(buffer::writeResourceLocation);
            buffer.writeVarInt(milestones.size());
            milestones.forEach(buffer::writeResourceLocation);
        }

        public static SyncProfile read(FriendlyByteBuf buffer) {
            boolean full = buffer.readBoolean();
            long xp = buffer.readVarLong();
            long prepared = buffer.readVarLong();
            long eaten = buffer.readVarLong();
            long served = buffer.readVarLong();
            int ingredients = buffer.readVarInt();
            int cookingStreak = buffer.readVarInt();
            int varietyStreak = buffer.readVarInt();

            int recordCount = buffer.readVarInt();
            List<RecordView> records = new ArrayList<>(recordCount);
            for (int i = 0; i < recordCount; i++) {
                records.add(RecordView.read(buffer));
            }
            List<ResourceLocation> badges = readIds(buffer);
            List<ResourceLocation> milestones = readIds(buffer);
            return new SyncProfile(full, xp, prepared, eaten, served, ingredients,
                    cookingStreak, varietyStreak, records, badges, milestones);
        }
    }

    /**
     * The progression rules the client needs to draw a progress bar correctly.
     * Sent by the server so a client with different config cannot mis-render.
     */
    public record SyncRules(int levelBase, int levelLinear, int levelQuadratic, int maxLevel,
                            List<Integer> masteryThresholds, String masteryRewards,
                            boolean leaderboardsEnabled, boolean bonusesEnabled,
                            double effectChance, double perfectChance, double extraPortionChance,
                            double cookingSpeed, float saturationPerRank, int effectSeconds)
            implements MealMasteryPacket {

        public static final ResourceLocation ID = packetId("sync_rules");

        public SyncRules {
            masteryThresholds = List.copyOf(masteryThresholds);
        }

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(levelBase);
            buffer.writeVarInt(levelLinear);
            buffer.writeVarInt(levelQuadratic);
            buffer.writeVarInt(maxLevel);
            buffer.writeVarInt(masteryThresholds.size());
            masteryThresholds.forEach(buffer::writeVarInt);
            buffer.writeUtf(masteryRewards, 32);
            buffer.writeBoolean(leaderboardsEnabled);
            buffer.writeBoolean(bonusesEnabled);
            buffer.writeDouble(effectChance);
            buffer.writeDouble(perfectChance);
            buffer.writeDouble(extraPortionChance);
            buffer.writeDouble(cookingSpeed);
            buffer.writeFloat(saturationPerRank);
            buffer.writeVarInt(effectSeconds);
        }

        public static SyncRules read(FriendlyByteBuf buffer) {
            int base = buffer.readVarInt();
            int linear = buffer.readVarInt();
            int quadratic = buffer.readVarInt();
            int maxLevel = buffer.readVarInt();
            int count = buffer.readVarInt();
            List<Integer> thresholds = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                thresholds.add(buffer.readVarInt());
            }
            return new SyncRules(base, linear, quadratic, maxLevel, thresholds,
                    buffer.readUtf(32), buffer.readBoolean(), buffer.readBoolean(),
                    buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                    buffer.readDouble(), buffer.readFloat(), buffer.readVarInt());
        }
    }

    /** Detail for one dish, requested when its page is opened. */
    public record SyncDishDetail(ResourceLocation target, List<ResourceLocation> ingredients,
                                 List<ResourceLocation> recipes) implements MealMasteryPacket {

        public static final ResourceLocation ID = packetId("sync_dish_detail");

        public SyncDishDetail {
            ingredients = List.copyOf(ingredients);
            recipes = List.copyOf(recipes);
        }

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(target);
            buffer.writeVarInt(ingredients.size());
            ingredients.forEach(buffer::writeResourceLocation);
            buffer.writeVarInt(recipes.size());
            recipes.forEach(buffer::writeResourceLocation);
        }

        public static SyncDishDetail read(FriendlyByteBuf buffer) {
            ResourceLocation target = buffer.readResourceLocation();
            return new SyncDishDetail(target, readIds(buffer), readIds(buffer));
        }
    }

    /** A single toast-worthy event. Rate limiting happens on the client. */
    public record Notify(Kind kind, ResourceLocation subject, long amount)
            implements MealMasteryPacket {

        public static final ResourceLocation ID = packetId("notify");

        public enum Kind {
            DISCOVERY, MASTERY_RANK, MASTERED, LEVEL_UP, CHALLENGE, MILESTONE, XP;

            private static final Kind[] VALUES = values();

            static Kind byOrdinal(int ordinal) {
                return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : XP;
            }
        }

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(kind.ordinal());
            buffer.writeBoolean(subject != null);
            if (subject != null) {
                buffer.writeResourceLocation(subject);
            }
            buffer.writeVarLong(Math.max(0L, amount));
        }

        public static Notify read(FriendlyByteBuf buffer) {
            Kind kind = Kind.byOrdinal(buffer.readVarInt());
            ResourceLocation subject = buffer.readBoolean() ? buffer.readResourceLocation() : null;
            return new Notify(kind, subject, buffer.readVarLong());
        }
    }

    /** Tells the client to open the journal, in response to {@code /mealmastery open}. */
    public record OpenJournal() implements MealMasteryPacket {
        public static final ResourceLocation ID = packetId("open_journal");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
        }

        public static OpenJournal read(FriendlyByteBuf buffer) {
            return new OpenJournal();
        }
    }


    /** One challenge as the journal draws it. */
    public record ChallengeView(ResourceLocation id, String titleKey, String descriptionKey,
                                boolean completed, int progress, int goal) {

        void write(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(id);
            buffer.writeUtf(titleKey, 256);
            buffer.writeUtf(descriptionKey, 256);
            buffer.writeBoolean(completed);
            buffer.writeVarInt(progress);
            buffer.writeVarInt(goal);
        }

        static ChallengeView read(FriendlyByteBuf buffer) {
            return new ChallengeView(buffer.readResourceLocation(), buffer.readUtf(256),
                    buffer.readUtf(256), buffer.readBoolean(), buffer.readVarInt(),
                    buffer.readVarInt());
        }
    }

    /**
     * One collection and how far into it the player is.
     *
     * @param titleKey  a translation key for datapack collections, or a literal
     *                  mod name for the automatically generated per-mod ones
     * @param literalTitle whether {@code titleKey} is already display text
     */
    public record CollectionView(ResourceLocation id, String titleKey, boolean literalTitle,
                                 ResourceLocation icon, int total, int discovered, int mastered) {

        void write(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(id);
            buffer.writeUtf(titleKey, 256);
            buffer.writeBoolean(literalTitle);
            buffer.writeBoolean(icon != null);
            if (icon != null) {
                buffer.writeResourceLocation(icon);
            }
            buffer.writeVarInt(total);
            buffer.writeVarInt(discovered);
            buffer.writeVarInt(mastered);
        }

        static CollectionView read(FriendlyByteBuf buffer) {
            ResourceLocation id = buffer.readResourceLocation();
            String title = buffer.readUtf(256);
            boolean literal = buffer.readBoolean();
            ResourceLocation icon = buffer.readBoolean() ? buffer.readResourceLocation() : null;
            return new CollectionView(id, title, literal, icon,
                    buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt());
        }
    }

    /** One ingredient in the player's journal. */
    public record IngredientView(ResourceLocation item, long timesUsed, long firstUsedDay,
                                 int mealCount, List<ResourceLocation> methods) {

        public IngredientView {
            methods = List.copyOf(methods);
        }

        void write(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(item);
            buffer.writeVarLong(Math.max(0L, timesUsed));
            buffer.writeLong(firstUsedDay);
            buffer.writeVarInt(mealCount);
            buffer.writeVarInt(methods.size());
            methods.forEach(buffer::writeResourceLocation);
        }

        static IngredientView read(FriendlyByteBuf buffer) {
            ResourceLocation item = buffer.readResourceLocation();
            long used = buffer.readVarLong();
            long firstDay = buffer.readLong();
            int mealCount = buffer.readVarInt();
            int methodCount = buffer.readVarInt();
            List<ResourceLocation> methods = new ArrayList<>(methodCount);
            for (int i = 0; i < methodCount; i++) {
                methods.add(buffer.readResourceLocation());
            }
            return new IngredientView(item, used, firstDay, mealCount, methods);
        }
    }

    /** One cooking method's statistics. */
    public record MethodView(ResourceLocation method, long preparations, int uniqueMeals,
                             int masteredMeals, ResourceLocation mostPrepared) {

        void write(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(method);
            buffer.writeVarLong(Math.max(0L, preparations));
            buffer.writeVarInt(uniqueMeals);
            buffer.writeVarInt(masteredMeals);
            buffer.writeBoolean(mostPrepared != null);
            if (mostPrepared != null) {
                buffer.writeResourceLocation(mostPrepared);
            }
        }

        static MethodView read(FriendlyByteBuf buffer) {
            ResourceLocation method = buffer.readResourceLocation();
            long preparations = buffer.readVarLong();
            int unique = buffer.readVarInt();
            int mastered = buffer.readVarInt();
            return new MethodView(method, preparations, unique, mastered,
                    buffer.readBoolean() ? buffer.readResourceLocation() : null);
        }
    }

    /**
     * Everything else the journal draws, recomputed server-side.
     *
     * <p>All of it depends on the player's own progress, so it is derived on the
     * server and sent as finished numbers rather than shipping definitions and
     * having the client work them out.</p>
     *
     * @param totalIngredients how many ingredients the installed recipes use in
     *                         total, which is the denominator in "47 / 82"
     */
    public record SyncJournalExtras(List<ChallengeView> challenges,
                                    List<CollectionView> collections,
                                    List<IngredientView> ingredients,
                                    List<MethodView> methods,
                                    List<ActivityView> activity,
                                    int totalIngredients) implements MealMasteryPacket {

        public static final ResourceLocation ID = packetId("sync_journal_extras");

        public SyncJournalExtras {
            challenges = List.copyOf(challenges);
            collections = List.copyOf(collections);
            ingredients = List.copyOf(ingredients);
            methods = List.copyOf(methods);
            activity = List.copyOf(activity);
        }

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(challenges.size());
            challenges.forEach(challenge -> challenge.write(buffer));
            buffer.writeVarInt(collections.size());
            collections.forEach(collection -> collection.write(buffer));
            buffer.writeVarInt(ingredients.size());
            ingredients.forEach(ingredient -> ingredient.write(buffer));
            buffer.writeVarInt(methods.size());
            methods.forEach(method -> method.write(buffer));
            buffer.writeVarInt(activity.size());
            activity.forEach(entry -> entry.write(buffer));
            buffer.writeVarInt(totalIngredients);
        }

        public static SyncJournalExtras read(FriendlyByteBuf buffer) {
            int challengeCount = buffer.readVarInt();
            List<ChallengeView> challenges = new ArrayList<>(challengeCount);
            for (int i = 0; i < challengeCount; i++) {
                challenges.add(ChallengeView.read(buffer));
            }
            int collectionCount = buffer.readVarInt();
            List<CollectionView> collections = new ArrayList<>(collectionCount);
            for (int i = 0; i < collectionCount; i++) {
                collections.add(CollectionView.read(buffer));
            }
            int ingredientCount = buffer.readVarInt();
            List<IngredientView> ingredients = new ArrayList<>(ingredientCount);
            for (int i = 0; i < ingredientCount; i++) {
                ingredients.add(IngredientView.read(buffer));
            }
            int methodCount = buffer.readVarInt();
            List<MethodView> methods = new ArrayList<>(methodCount);
            for (int i = 0; i < methodCount; i++) {
                methods.add(MethodView.read(buffer));
            }
            int activityCount = buffer.readVarInt();
            List<ActivityView> activity = new ArrayList<>(activityCount);
            for (int i = 0; i < activityCount; i++) {
                activity.add(ActivityView.read(buffer));
            }
            return new SyncJournalExtras(challenges, collections, ingredients, methods,
                    activity, buffer.readVarInt());
        }
    }

    /** One line of the Recent Activity feed. */
    public record ActivityView(String type, ResourceLocation subject, long detail, long day) {

        void write(FriendlyByteBuf buffer) {
            buffer.writeUtf(type, 64);
            buffer.writeBoolean(subject != null);
            if (subject != null) {
                buffer.writeResourceLocation(subject);
            }
            buffer.writeVarLong(Math.max(0L, detail));
            buffer.writeLong(day);
        }

        static ActivityView read(FriendlyByteBuf buffer) {
            String type = buffer.readUtf(64);
            ResourceLocation subject = buffer.readBoolean() ? buffer.readResourceLocation() : null;
            return new ActivityView(type, subject, buffer.readVarLong(), buffer.readLong());
        }
    }

    /**
     * Tells the client whether the screen it currently has open is a cooking
     * surface.
     *
     * <p>The server already works this out for attribution - it is the same
     * check that decides whether taking an item counts as cooking - so the
     * client is told rather than guessing. That is what makes the stats panel
     * appear beside a Cooking Pot and any addon workstation, but not beside a
     * chest or the plain inventory.</p>
     */
    public record MenuContext(boolean culinary, ResourceLocation method)
            implements MealMasteryPacket {

        public static final ResourceLocation ID = packetId("menu_context");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeBoolean(culinary);
            buffer.writeBoolean(method != null);
            if (method != null) {
                buffer.writeResourceLocation(method);
            }
        }

        public static MenuContext read(FriendlyByteBuf buffer) {
            boolean culinary = buffer.readBoolean();
            return new MenuContext(culinary,
                    buffer.readBoolean() ? buffer.readResourceLocation() : null);
        }
    }

    // ------------------------------------------------------- client to server


    public record RequestDishDetail(ResourceLocation target) implements MealMasteryPacket {
        public static final ResourceLocation ID = packetId("request_dish_detail");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeResourceLocation(target);
        }

        public static RequestDishDetail read(FriendlyByteBuf buffer) {
            return new RequestDishDetail(buffer.readResourceLocation());
        }
    }

    /** Asks for a fresh full profile; sent when the journal is opened. */
    public record RequestProfile() implements MealMasteryPacket {
        public static final ResourceLocation ID = packetId("request_profile");

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
        }

        public static RequestProfile read(FriendlyByteBuf buffer) {
            return new RequestProfile();
        }
    }

    /**
     * A personal preference, not a progression claim: the server stores the
     * flag and echoes back the authoritative value.
     */
    public record SetPreference(Kind kind, ResourceLocation target) implements MealMasteryPacket {
        public static final ResourceLocation ID = packetId("set_preference");

        public enum Kind {
            FAVORITE, PIN, TITLE, SIGNATURE;

            private static final Kind[] VALUES = values();

            static Kind byOrdinal(int ordinal) {
                return ordinal >= 0 && ordinal < VALUES.length ? VALUES[ordinal] : FAVORITE;
            }
        }

        @Override
        public ResourceLocation id() {
            return ID;
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeVarInt(kind.ordinal());
            buffer.writeBoolean(target != null);
            if (target != null) {
                buffer.writeResourceLocation(target);
            }
        }

        public static SetPreference read(FriendlyByteBuf buffer) {
            Kind kind = Kind.byOrdinal(buffer.readVarInt());
            return new SetPreference(kind,
                    buffer.readBoolean() ? buffer.readResourceLocation() : null);
        }
    }

    private static List<ResourceLocation> readIds(FriendlyByteBuf buffer) {
        int count = buffer.readVarInt();
        List<ResourceLocation> ids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ids.add(buffer.readResourceLocation());
        }
        return ids;
    }
}
