package com.xirc.mealmastery.data;

import com.xirc.mealmastery.MealMasteryLog;
import com.xirc.mealmastery.culinary.CulinaryProfile;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Reads and writes one compressed NBT file per player, under the world's data
 * directory.
 *
 * <p>Per-player files rather than one big blob: a thousand-dish modpack with a
 * hundred players should never be re-serialised in full because one person
 * cooked a stew, and a corrupted profile can only ever cost one player
 * their history.</p>
 *
 * <p>Storing under the world directory is what keeps profiles per-world:
 * a singleplayer world and a server can never merge Cooking Levels.</p>
 */
public final class ProfileStorage {
    private static final String DIRECTORY = "mealmastery/players";

    private final Path root;

    public ProfileStorage(Path worldDataDirectory) {
        this.root = worldDataDirectory.resolve(DIRECTORY);
    }

    public Path root() {
        return root;
    }

    public CulinaryProfile load(UUID playerId) {
        Path file = fileFor(playerId);
        if (!Files.exists(file)) {
            return new CulinaryProfile(playerId);
        }
        try (InputStream input = Files.newInputStream(file)) {
            CompoundTag tag = NbtIo.readCompressed(input);
            return CulinaryProfile.load(playerId, ProfileMigrations.migrate(tag));
        } catch (IOException | RuntimeException failure) {
            // A profile we cannot read is set aside rather than overwritten, so
            // the player restarts empty but their history is still recoverable.
            MealMasteryLog.LOGGER.error("Could not read the culinary profile for {}; "
                    + "the file has been kept for recovery.", playerId, failure);
            quarantine(file);
            return new CulinaryProfile(playerId);
        }
    }

    public void save(CulinaryProfile profile) {
        Path file = fileFor(profile.playerId());
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try {
            Files.createDirectories(root);
            try (OutputStream output = Files.newOutputStream(temporary)) {
                NbtIo.writeCompressed(profile.save(), output);
            }
            // Write-then-move so a crash mid-save cannot truncate a real profile.
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            profile.clearDirty();
        } catch (IOException | RuntimeException failure) {
            MealMasteryLog.LOGGER.error("Could not write the culinary profile for {}",
                    profile.playerId(), failure);
        }
    }

    public boolean delete(UUID playerId) {
        try {
            return Files.deleteIfExists(fileFor(playerId));
        } catch (IOException failure) {
            MealMasteryLog.LOGGER.error("Could not delete the culinary profile for {}",
                    playerId, failure);
            return false;
        }
    }

    private Path fileFor(UUID playerId) {
        return root.resolve(playerId + ".dat");
    }

    private static void quarantine(Path file) {
        try {
            Files.move(file, file.resolveSibling(file.getFileName() + ".corrupt"),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException failure) {
            MealMasteryLog.LOGGER.error("Could not set the unreadable profile aside", failure);
        }
    }
}
