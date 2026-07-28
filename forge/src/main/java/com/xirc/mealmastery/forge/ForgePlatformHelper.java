package com.xirc.mealmastery.forge;

import com.xirc.mealmastery.platform.IPlatformHelper;
import com.xirc.mealmastery.util.ModIdNames;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.versions.forge.ForgeVersion;
import net.minecraftforge.versions.mcp.MCPVersion;

import java.nio.file.Path;

public final class ForgePlatformHelper implements IPlatformHelper {
    @Override
    public String loaderName() {
        return "Forge";
    }

    @Override
    public String loaderVersion() {
        return ForgeVersion.getVersion();
    }

    @Override
    public String minecraftVersion() {
        return MCPVersion.getMCVersion();
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
}
