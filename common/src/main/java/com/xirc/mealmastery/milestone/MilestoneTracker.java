package com.xirc.mealmastery.milestone;

import com.xirc.mealmastery.MealMasteryLog;
import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.config.ServerConfig;
import com.xirc.mealmastery.culinary.ActivityEntry;
import com.xirc.mealmastery.culinary.CulinaryClock;
import com.xirc.mealmastery.culinary.CulinaryProfile;
import com.xirc.mealmastery.event.CulinaryEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Awards milestones when the numbers behind them cross a threshold.
 *
 * <p>Evaluated on the same events that change those numbers rather than on a
 * timer, and each milestone is awarded once ever.</p>
 */
public final class MilestoneTracker {
    private static volatile List<Milestone> milestones = new ArrayList<>(Milestone.builtIns());
    private static boolean registered;

    private MilestoneTracker() {
    }

    public static synchronized void install() {
        if (registered) {
            return;
        }
        registered = true;
        CulinaryEvents.MEAL_PREPARED.register(event ->
                evaluate(event.player(), event.profile()));
        CulinaryEvents.RECIPE_DISCOVERED.register(event ->
                evaluate(event.player(), event.profile()));
        CulinaryEvents.INGREDIENT_DISCOVERED.register(event ->
                evaluate(event.player(), event.profile()));
        CulinaryEvents.MASTERY_RANK_CHANGED.register(event ->
                evaluate(event.player(), event.profile()));
        CulinaryEvents.COOKING_LEVEL_CHANGED.register(event ->
                evaluate(event.player(), event.profile()));
        CulinaryEvents.PORTIONS_SERVED.register(event ->
                evaluate(event.player(), event.profile()));
    }

    public static void replace(Collection<Milestone> definitions) {
        List<Milestone> combined = new ArrayList<>(definitions);
        if (combined.isEmpty()) {
            combined.addAll(Milestone.builtIns());
        }
        milestones = List.copyOf(combined);
        MealMasteryLog.LOGGER.info("Loaded {} milestone(s)", milestones.size());
    }

    public static List<Milestone> all() {
        return milestones;
    }

    public static void evaluate(ServerPlayer player, CulinaryProfile profile) {
        ServerConfig config = ConfigManager.server();
        long day = CulinaryClock.day(player.server);
        for (Milestone milestone : milestones) {
            if (profile.milestones().contains(milestone.id())) {
                continue;
            }
            long value = milestone.valueFor(profile, config.progression.toCurve(),
                    config.mastery.toCurve());
            if (value < milestone.threshold()) {
                continue;
            }
            if (!profile.reachMilestone(milestone.id())) {
                continue;
            }
            profile.pushActivity(new ActivityEntry(
                    ActivityEntry.Type.MILESTONE_REACHED, milestone.id(),
                    milestone.threshold(), day));
            ResourceLocation badge = milestone.badge();
            if (badge != null) {
                profile.awardBadge(badge);
            }
            CulinaryEvents.MILESTONE_REACHED.fire(new CulinaryEvents.MilestoneReached(
                    player, profile, milestone.id()));
        }
    }
}
