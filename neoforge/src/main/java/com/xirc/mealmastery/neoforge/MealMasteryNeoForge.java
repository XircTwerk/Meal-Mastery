package com.xirc.mealmastery.neoforge;

import com.xirc.mealmastery.Constants;
import com.xirc.mealmastery.MealMastery;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(Constants.MOD_ID)
public final class MealMasteryNeoForge {
    public MealMasteryNeoForge(IEventBus modBus) {
        MealMastery.initialize();
        // NeoForge hands the mod bus to the constructor rather than exposing a
        // static one, so packet registration is wired here.
        modBus.addListener(NeoForgeNetwork::register);
    }
}
