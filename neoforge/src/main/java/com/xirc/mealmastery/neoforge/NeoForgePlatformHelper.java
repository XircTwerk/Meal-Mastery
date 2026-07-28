package com.xirc.mealmastery.neoforge;

import com.xirc.mealmastery.platform.IPlatformHelper;
import com.xirc.mealmastery.util.ModIdNames;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.internal.versions.neoforge.NeoForgeVersion;

import java.nio.file.Path;

public final class NeoForgePlatformHelper implements IPlatformHelper {
    @Override
    public String loaderName() {
        return "NeoForge";
    }

    @Override
    public String loaderVersion() {
        return NeoForgeVersion.getVersion();
    }

    @Override
    public String minecraftVersion() {
        return FMLLoaderVersions.minecraftVersion();
    }

    @Override
    public Path gameDirectory() {
        return FMLPaths.GAMEDIR.get();
    }

    @Override
    public Path configDirectory() {
        return FMLPaths.CONFIGDIR.get();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLEnvironment.production;
    }

    @Override
    public String modDisplayName(String modId) {
        return ModList.get().getModContainerById(modId)
                .map(container -> container.getModInfo().getDisplayName())
                .orElseGet(() -> ModIdNames.prettify(modId));
    }

    /** Split out so the version lookup stays in one place if NeoForge moves it again. */
    private static final class FMLLoaderVersions {
        private FMLLoaderVersions() {
        }

        static String minecraftVersion() {
            return net.neoforged.fml.loading.FMLLoader.versionInfo().mcVersion();
        }
    }
}
