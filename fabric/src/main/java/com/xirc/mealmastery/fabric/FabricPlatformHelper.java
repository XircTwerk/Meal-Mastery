package com.xirc.mealmastery.fabric;

import com.xirc.mealmastery.platform.IPlatformHelper;
import com.xirc.mealmastery.util.ModIdNames;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public final class FabricPlatformHelper implements IPlatformHelper {
    @Override
    public String loaderName() {
        return "Fabric";
    }

    @Override
    public String loaderVersion() {
        return metadataVersion("fabricloader");
    }

    @Override
    public String minecraftVersion() {
        return metadataVersion("minecraft");
    }

    @Override
    public Path gameDirectory() {
        return FabricLoader.getInstance().getGameDir();
    }

    @Override
    public Path configDirectory() {
        return FabricLoader.getInstance().getConfigDir();
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public String modDisplayName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getName())
                .orElseGet(() -> ModIdNames.prettify(modId));
    }

    private static String metadataVersion(String modId) {
        return FabricLoader.getInstance().getModContainer(modId)
                .map(container -> container.getMetadata().getVersion().getFriendlyString())
                .orElse("unknown");
    }
}
