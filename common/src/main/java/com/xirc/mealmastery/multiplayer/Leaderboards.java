package com.xirc.mealmastery.multiplayer;

import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.config.ServerConfig;
import com.xirc.mealmastery.culinary.CulinaryProfile;
import com.xirc.mealmastery.culinary.ProfileManager;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Optional server leaderboards.
 *
 * <p>Disabled by default, opt-in per player by default on top of that, and
 * built only from online players' culinary statistics. Nothing else about a
 * player is exposed — no coordinates, no inventory, nothing a leaderboard has
 * no business knowing.</p>
 */
public final class Leaderboards {

    private Leaderboards() {
    }

    public enum Category {
        COOKING_LEVEL,
        RECIPES_MASTERED,
        UNIQUE_RECIPES,
        PORTIONS_SERVED
    }

    /**
     * @param name  the player's display name
     * @param value the single number this category ranks by
     */
    public record Entry(String name, long value) {
    }

    public static boolean enabled() {
        return ConfigManager.server().multiplayer.leaderboardsEnabled;
    }

    public static List<Entry> top(MinecraftServer server, Category category) {
        ServerConfig config = ConfigManager.server();
        if (!config.multiplayer.leaderboardsEnabled) {
            return List.of();
        }
        ProfileManager profiles = ProfileManager.get();
        if (profiles == null) {
            return List.of();
        }

        List<Entry> entries = new ArrayList<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (config.multiplayer.leaderboardsOptInOnly && !hasOptedIn(player)) {
                continue;
            }
            CulinaryProfile profile = profiles.of(player);
            entries.add(new Entry(player.getGameProfile().getName(),
                    valueOf(profile, category, config)));
        }
        entries.sort(Comparator.comparingLong(Entry::value).reversed());
        return entries.size() > config.multiplayer.leaderboardSize
                ? List.copyOf(entries.subList(0, config.multiplayer.leaderboardSize))
                : List.copyOf(entries);
    }

    private static long valueOf(CulinaryProfile profile, Category category, ServerConfig config) {
        return switch (category) {
            case COOKING_LEVEL -> profile.cookingLevel(config.progression.toCurve());
            case RECIPES_MASTERED -> profile.masteredCount(config.mastery.toCurve());
            case UNIQUE_RECIPES -> profile.uniquePreparedCount();
            case PORTIONS_SERVED -> profile.stats().portionsServed();
        };
    }

    /**
     * Opting in is a scoreboard tag, so it needs no extra storage and a server
     * can grant it with a plain vanilla command.
     */
    private static boolean hasOptedIn(ServerPlayer player) {
        return player.getTags().contains("mealmastery_leaderboard");
    }
}
