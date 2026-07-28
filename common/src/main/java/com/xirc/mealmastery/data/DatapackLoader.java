package com.xirc.mealmastery.data;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.xirc.mealmastery.MealMasteryLog;
import com.xirc.mealmastery.challenge.ChallengeDefinition;
import com.xirc.mealmastery.challenge.ChallengeParseException;
import com.xirc.mealmastery.collection.CollectionDefinition;
import com.xirc.mealmastery.milestone.Milestone;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Reads challenges, collections and milestones out of datapacks.
 *
 * <p>Read straight from the server's {@code ResourceManager} at the same moment
 * the culinary registry is rebuilt, rather than through a reload listener.
 * That keeps the two in step — a challenge is validated against the recipes
 * that exist right now — and avoids needing a separate listener registration on
 * each loader.</p>
 *
 * <p>Every file is validated individually. A malformed one is named in the log
 * and skipped; the rest of the pack loads.</p>
 */
public final class DatapackLoader {
    private static final Gson GSON = new Gson();
    private static final String SUFFIX = ".json";

    private DatapackLoader() {
    }

    public record Result(List<ChallengeDefinition> challenges,
                         List<CollectionDefinition> collections,
                         List<Milestone> milestones,
                         List<String> problems) {
    }

    public static Result load(ResourceManager resources) {
        List<String> problems = new ArrayList<>();
        return new Result(
                read(resources, "mealmastery/challenges", ChallengeDefinition::fromJson, problems),
                read(resources, "mealmastery/collections", CollectionDefinition::fromJson, problems),
                read(resources, "mealmastery/milestones", Milestone::fromJson, problems),
                problems);
    }

    private static <T> List<T> read(ResourceManager resources, String directory,
                                    BiFunction<ResourceLocation, JsonObject, T> parser,
                                    List<String> problems) {
        List<T> loaded = new ArrayList<>();
        Map<ResourceLocation, Resource> files =
                resources.listResources(directory, path -> path.getPath().endsWith(SUFFIX));

        for (Map.Entry<ResourceLocation, Resource> file : files.entrySet()) {
            ResourceLocation path = file.getKey();
            ResourceLocation id = idFrom(path, directory);
            if (id == null) {
                continue;
            }
            try (Reader reader = file.getValue().openAsReader()) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json == null) {
                    throw new ChallengeParseException("file is empty");
                }
                loaded.add(parser.apply(id, json));
            } catch (Exception failure) {
                String message = path + ": " + failure.getMessage();
                problems.add(message);
                MealMasteryLog.LOGGER.warn("Skipping {} — {}", path, failure.getMessage());
            }
        }
        return loaded;
    }

    /** {@code data/ns/mealmastery/challenges/soup_season.json} to {@code ns:soup_season}. */
    private static ResourceLocation idFrom(ResourceLocation path, String directory) {
        String raw = path.getPath();
        String prefix = directory + "/";
        if (!raw.startsWith(prefix) || !raw.endsWith(SUFFIX)) {
            return null;
        }
        String name = raw.substring(prefix.length(), raw.length() - SUFFIX.length());
        return name.isEmpty() ? null : new ResourceLocation(path.getNamespace(), name);
    }
}
