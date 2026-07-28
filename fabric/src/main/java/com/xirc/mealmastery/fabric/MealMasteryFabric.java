package com.xirc.mealmastery.fabric;

import com.xirc.mealmastery.MealMastery;
import net.fabricmc.api.ModInitializer;

public final class MealMasteryFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        MealMastery.initialize();
        FabricEventBridge.register();
        FabricNetwork.registerServer();
    }
}
