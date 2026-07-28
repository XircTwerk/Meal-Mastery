package com.xirc.mealmastery.culinary;

import com.xirc.mealmastery.MealMasteryLog;
import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.data.ProfileStorage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the live profiles for one running server.
 *
 * <p>Profiles are loaded on join, kept in memory while the player is online,
 * flushed on quit and on a timer, and dropped when the server stops. Nothing
 * reads a profile from disk on a hot path.</p>
 */
public final class ProfileManager {
    private static volatile ProfileManager instance;

    private final ProfileStorage storage;
    private final Map<UUID, CulinaryProfile> online = new ConcurrentHashMap<>();
    private long lastAutosave = System.currentTimeMillis();

    private ProfileManager(ProfileStorage storage) {
        this.storage = storage;
    }

    public static ProfileManager get() {
        return instance;
    }

    public static void start(MinecraftServer server) {
        instance = new ProfileManager(
                new ProfileStorage(server.getWorldPath(LevelResource.ROOT).resolve("data").normalize()));
        MealMasteryLog.LOGGER.info("Culinary profiles will be stored in {}",
                instance.storage.root());
    }

    public static void stop() {
        ProfileManager manager = instance;
        if (manager != null) {
            manager.flushAll();
            manager.online.clear();
        }
        instance = null;
    }

    public ProfileStorage storage() {
        return storage;
    }

    public CulinaryProfile of(ServerPlayer player) {
        return of(player.getUUID());
    }

    public CulinaryProfile of(UUID playerId) {
        return online.computeIfAbsent(playerId, storage::load);
    }

    /**
     * Reads a profile for a player who may be offline, without adding them to
     * the online cache. Used by {@code /mealmastery admin inspect}.
     */
    public CulinaryProfile peekOffline(UUID playerId) {
        CulinaryProfile cached = online.get(playerId);
        return cached != null ? cached : storage.load(playerId);
    }

    public void onJoin(ServerPlayer player) {
        online.put(player.getUUID(), storage.load(player.getUUID()));
    }

    public void onQuit(ServerPlayer player) {
        CulinaryProfile profile = online.remove(player.getUUID());
        if (profile != null && profile.isDirty()) {
            storage.save(profile);
        }
    }

    /** Called every server tick; only actually writes when the interval has elapsed. */
    public void tickAutosave() {
        int interval = ConfigManager.server().advanced.autosaveIntervalMillis;
        long now = System.currentTimeMillis();
        if (now - lastAutosave < interval) {
            return;
        }
        lastAutosave = now;
        flushAll();
    }

    public void flushAll() {
        for (CulinaryProfile profile : online.values()) {
            if (profile.isDirty()) {
                storage.save(profile);
            }
        }
    }

    /** Writes one profile immediately; used after destructive admin commands. */
    public void flush(UUID playerId) {
        CulinaryProfile profile = online.get(playerId);
        if (profile != null) {
            storage.save(profile);
        }
    }

    public Map<UUID, CulinaryProfile> onlineProfiles() {
        return Map.copyOf(online);
    }
}
