package com.xirc.mealmastery.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.xirc.mealmastery.MealMasteryLog;
import com.xirc.mealmastery.recipe.CulinaryTags;
import com.xirc.mealmastery.recipe.EligibilityRules;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Reads and writes the two config documents.
 *
 * <p>Loading is defensive on purpose. A malformed file is renamed aside and
 * replaced with defaults rather than crashing the server, because a config
 * typo must never cost anyone their world; the original is kept so the mistake
 * can be found and fixed.</p>
 */
public final class ConfigManager {
    private static final String SERVER_FILE = "mealmastery-server.toml";
    private static final String CLIENT_FILE = "mealmastery-client.toml";
    private static final String LEGACY_SERVER_FILE = "mealmastery-server.json";
    private static final String LEGACY_CLIENT_FILE = "mealmastery-client.json";

    private static final List<String> SERVER_HEADER = List.of(
            "Meal Mastery - server settings",
            "",
            "Authoritative on the server. In multiplayer only the server's copy",
            "matters; a client's file cannot change progression.",
            "",
            "Values outside a supported range are clamped, not rejected - a typo",
            "must never cost anyone their world. Corrections are written back and",
            "named in the log. A file that cannot be parsed at all is renamed to",
            "*.invalid and replaced with defaults, so the original is recoverable.",
            "",
            "Apply changes without a restart:  /mealmastery reload");

    private static final List<String> CLIENT_HEADER = List.of(
            "Meal Mastery - client settings",
            "",
            "Presentation only. Everything here is also reachable in game, from",
            "the settings button on the journal screen.");

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private static volatile ServerConfig server = new ServerConfig();
    private static volatile ClientConfig client = new ClientConfig();
    private static volatile Path directory;

    private ConfigManager() {
    }

    public static ServerConfig server() {
        return server;
    }

    public static ClientConfig client() {
        return client;
    }

    public static void initialize(Path configDirectory) {
        directory = configDirectory;
        reload();
    }

    public static void reload() {
        Path dir = directory;
        if (dir == null) {
            return;
        }
        server = load(dir.resolve(SERVER_FILE), dir.resolve(LEGACY_SERVER_FILE),
                ServerConfig.class, ServerConfig::new, ServerConfig::validate, SERVER_HEADER);
        client = load(dir.resolve(CLIENT_FILE), dir.resolve(LEGACY_CLIENT_FILE),
                ClientConfig.class, ClientConfig::new, ClientConfig::validate, CLIENT_HEADER);
    }

    public static void saveServer(ServerConfig config) {
        config.validate();
        server = config;
        write(SERVER_FILE, config, SERVER_HEADER);
    }

    public static void saveClient(ClientConfig config) {
        config.validate();
        client = config;
        write(CLIENT_FILE, config, CLIENT_HEADER);
    }

    private interface Validator<T> {
        List<String> validate(T config);
    }

    private static <T> T load(Path file, Path legacy, Class<T> type,
                              java.util.function.Supplier<T> factory,
                              Validator<T> validator, List<String> header) {
        T config;
        if (!Files.exists(file)) {
            // A JSON file from before the format changed is read once and
            // rewritten as TOML, so nobody loses a tuned server by updating.
            config = Files.exists(legacy) ? migrate(legacy, type, factory) : factory.get();
            validator.validate(config);
            write(file, config, header);
            return config;
        }
        try {
            config = Toml.read(Files.readString(file, StandardCharsets.UTF_8), type);
        } catch (IOException | RuntimeException failure) {
            MealMasteryLog.LOGGER.error("Could not read {}; falling back to defaults.",
                    file.getFileName(), failure);
            quarantine(file);
            config = factory.get();
            validator.validate(config);
            write(file, config, header);
            return config;
        }

        List<String> issues = validator.validate(config);
        if (!issues.isEmpty()) {
            MealMasteryLog.LOGGER.warn("{} contained {} value(s) outside the supported range:",
                    file.getFileName(), issues.size());
            issues.forEach(issue -> MealMasteryLog.LOGGER.warn("  {}", issue));
        }
        // Always write back, not only on a correction. A file written by an
        // older build is missing whatever sections have been added since, and
        // leaving it alone means the player silently runs new defaults they
        // cannot see or edit. Rewriting adds the new keys and the comments
        // explaining them, and preserves every value they had already set.
        write(file, config, header);
        return config;
    }

    /**
     * Reads a pre-TOML JSON file, then renames it aside.
     *
     * <p>The old file is kept rather than deleted: if the conversion misreads
     * something, the only copy of a server's tuning should not be gone.</p>
     */
    private static <T> T migrate(Path legacy, Class<T> type, java.util.function.Supplier<T> factory) {
        try (Reader reader = Files.newBufferedReader(legacy, StandardCharsets.UTF_8)) {
            T config = GSON.fromJson(reader, type);
            if (config == null) {
                throw new JsonSyntaxException("file was empty");
            }
            MealMasteryLog.LOGGER.info("Converted {} to TOML; the original was kept as {}.bak",
                    legacy.getFileName(), legacy.getFileName());
            return config;
        } catch (IOException | RuntimeException failure) {
            MealMasteryLog.LOGGER.error("Could not convert {}; starting from defaults.",
                    legacy.getFileName(), failure);
            return factory.get();
        } finally {
            try {
                Files.move(legacy, legacy.resolveSibling(legacy.getFileName() + ".bak"),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException failure) {
                MealMasteryLog.LOGGER.warn("Could not rename {} aside", legacy.getFileName());
            }
        }
    }

    private static void quarantine(Path file) {
        try {
            Path backup = file.resolveSibling(file.getFileName() + ".invalid");
            Files.move(file, backup, StandardCopyOption.REPLACE_EXISTING);
            MealMasteryLog.LOGGER.error("The unreadable file was kept as {}", backup.getFileName());
        } catch (IOException failure) {
            MealMasteryLog.LOGGER.error("Could not set the unreadable config aside", failure);
        }
    }

    private static void write(String fileName, Object config, List<String> header) {
        Path dir = directory;
        if (dir != null) {
            write(dir.resolve(fileName), config, header);
        }
    }

    private static void write(Path file, Object config, List<String> header) {
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write(Toml.write(config, header));
            }
        } catch (IOException | RuntimeException failure) {
            MealMasteryLog.LOGGER.error("Could not write {}", file.getFileName(), failure);
        }
    }

    /**
     * Translates the compatibility section into recipe-eligibility rules.
     *
     * <p>Ids that fail to parse are reported and dropped rather than silently
     * ignored: a typo in an exclusion list is exactly the kind of thing that
     * otherwise gets blamed on the addon.</p>
     */
    public static EligibilityRules toEligibilityRules(ServerConfig config) {
        EligibilityRules.Builder builder = EligibilityRules.builder()
                .requireEdibleOutput(config.compatibility.requireEdibleOutput)
                .ignoreUnpacking(config.compatibility.ignoreUnpackingRecipes);

        for (String raw : config.compatibility.excludedRecipeTypes) {
            parseId(raw, "excludedRecipeTypes").ifPresent(builder::denyRecipeType);
        }
        for (String raw : config.compatibility.excludedItems) {
            parseId(raw, "excludedItems").ifPresent(builder::denyItem);
        }
        if (!config.compatibility.trackVanillaRecipes) {
            builder.denyMod("minecraft");
        }
        for (String raw : config.compatibility.excludedMods) {
            builder.denyMod(raw);
        }
        for (String raw : config.compatibility.includedOnlyMods) {
            builder.allowMod(raw);
        }
        for (String raw : config.compatibility.requiredItemTags) {
            parseId(raw, "requiredItemTags")
                    .ifPresent(id -> builder.requireTag(CulinaryTags.item(id.getNamespace(), id.getPath())));
        }
        return builder.build();
    }

    private static java.util.Optional<ResourceLocation> parseId(String raw, String field) {
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            MealMasteryLog.LOGGER.warn("compatibility.{} contains an unparseable id: '{}'", field, raw);
            return java.util.Optional.empty();
        }
        return java.util.Optional.of(id);
    }
}
