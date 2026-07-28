package com.xirc.mealmastery.forge;

import com.xirc.mealmastery.Constants;
import com.xirc.mealmastery.MealMastery;
import net.minecraftforge.fml.common.Mod;

@Mod(Constants.MOD_ID)
public final class MealMasteryForge {
    public MealMasteryForge() {
        MealMastery.initialize();
        ForgeNetwork.register();
    }
}
