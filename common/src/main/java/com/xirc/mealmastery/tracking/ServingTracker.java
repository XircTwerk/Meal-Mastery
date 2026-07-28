package com.xirc.mealmastery.tracking;

import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.culinary.CulinaryClock;
import com.xirc.mealmastery.culinary.CulinaryProfile;
import com.xirc.mealmastery.culinary.MealRecord;
import com.xirc.mealmastery.culinary.ProfileManager;
import com.xirc.mealmastery.event.CulinaryEvents;
import com.xirc.mealmastery.recipe.CookingMethod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/**
 * Feast serving.
 *
 * <p>What is honestly observable is "this player took a portion out of a placed
 * feast". Who eventually eats that portion is not, so nothing here claims it.
 * "Portions served" therefore means portions this player dispensed — the
 * journal says exactly that, and personal consumption stays a separate figure
 *.</p>
 */
public final class ServingTracker {

    private ServingTracker() {
    }

    /**
     * @return {@code true} when the credit was a feast portion and has been
     *         recorded as serving instead of preparation
     */
    public static boolean recordIfServing(CulinaryCredit credit) {
        if (!CookingMethod.FEAST_SERVING.equals(credit.method())) {
            return false;
        }
        ProfileManager profiles = ProfileManager.get();
        if (profiles == null) {
            return false;
        }
        ServerPlayer player = credit.player();
        CulinaryProfile profile = profiles.of(player);
        ResourceLocation meal = credit.meal();
        long day = CulinaryClock.day(player.server);

        MealRecord record = profile.meal(meal);
        record.recordServed(credit.amount());
        profile.stats().addPortionsServed(credit.amount());
        profile.records().offerFeastPortionsInOneDay(credit.amount());

        if (ConfigManager.server().mastery.enabled
                && ConfigManager.server().mastery.pointsPerServing > 0) {
            record.addMasteryPoints(
                    (long) ConfigManager.server().mastery.pointsPerServing * credit.amount());
        }
        profile.markDirty();

        CulinaryEvents.PORTIONS_SERVED.fire(new CulinaryEvents.PortionsServed(
                player, profile, meal, credit.amount(), day));
        return true;
    }
}
