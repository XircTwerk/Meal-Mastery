package com.xirc.mealmastery.progression;

import com.xirc.mealmastery.Constants;
import com.xirc.mealmastery.MealMasteryLog;
import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.config.ServerConfig;
import com.xirc.mealmastery.culinary.CulinaryProfile;
import com.xirc.mealmastery.culinary.MethodRecord;
import com.xirc.mealmastery.culinary.ProfileManager;
import com.xirc.mealmastery.mastery.MasteryCurve;
import com.xirc.mealmastery.mastery.MasteryRank;
import com.xirc.mealmastery.recipe.CookingMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Set;

/**
 * Mastery makes a workstation cook faster.
 *
 * <p>Implemented by ticking the block entity extra times rather than by
 * reaching into anyone's cook timer. That needs no knowledge of how Farmer's
 * Delight counts progress — {@code EntityBlock#getTicker} is vanilla — so there
 * is still no compile-time dependency on Farmer's Delight anywhere.</p>
 *
 * <p>Deliberately limited to the workstations where "cooking faster" means
 * something: Farmer's Delight's pot, skillet and stove, plus the vanilla
 * smoker when {@code bonuses.accelerateSmokers} allows it. A cutting board is
 * instant, and speeding up a blast furnace has nothing to do with food.</p>
 */
public final class CookingSpeed {

    /** Farmer's Delight's own workstations, always accelerated. */
    private static final Set<CookingMethod> ACCELERATED = Set.of(
            CookingMethod.COOKING_POT,
            CookingMethod.of(Constants.FARMERS_DELIGHT_ID, "skillet"),
            CookingMethod.of(Constants.FARMERS_DELIGHT_ID, "stove"));

    /**
     * Whether this workstation is sped up at all.
     *
     * <p>The smoker is asked about separately because it is the one vanilla
     * station in the set, and a server can turn it off without giving up the
     * bonus on Farmer's Delight's own blocks.</p>
     */
    private static boolean accelerates(CookingMethod method, ServerConfig.Bonuses bonuses) {
        return ACCELERATED.contains(method)
                || (bonuses.accelerateSmokers && CookingMethod.SMOKING.equals(method));
    }

    /** Preparations with a method before it counts as one tier of familiarity. */
    private static final int PREPARATIONS_PER_TIER = 25;

    private static boolean warned;

    private CookingSpeed() {
    }

    /**
     * Gives the workstation the player is using extra ticks.
     *
     * @param pos    the workstation being used, or {@code null} if unknown
     * @param method the method that workstation represents
     */
    public static void tick(ServerPlayer player, BlockPos pos, CookingMethod method,
                            AbstractContainerMenu menu) {
        ServerConfig config = ConfigManager.server();
        ServerConfig.Bonuses bonuses = config.mastery.bonuses;
        if (!bonuses.enabled || !config.mastery.enabled || pos == null || method == null) {
            return;
        }
        if (!accelerates(method, bonuses) || bonuses.cookingSpeedAtMaxRank <= 0.0) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level) || !level.isLoaded(pos)) {
            return;
        }

        int tier = masteryTier(player, method, menu, config);
        if (tier <= 0) {
            return;
        }
        double extra = bonuses.cookingSpeedAtMaxRank
                * tier / (double) MasteryRank.highestIndex();

        int whole = (int) extra;
        if (level.getRandom().nextDouble() < extra - whole) {
            whole++;
        }
        for (int i = 0; i < whole; i++) {
            if (!tickOnce(level, pos)) {
                return;
            }
        }
    }

    /**
     * Runs the block entity's own ticker one extra time.
     *
     * @return {@code false} when there is nothing tickable there, so the caller
     *         stops rather than retrying
     */
    private static boolean tickOnce(ServerLevel level, BlockPos pos) {
        try {
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof EntityBlock entityBlock)) {
                return false;
            }
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity == null) {
                return false;
            }
            @SuppressWarnings({"unchecked", "rawtypes"})
            BlockEntityTicker ticker = entityBlock.getTicker(level, state, blockEntity.getType());
            if (ticker == null) {
                return false;
            }
            @SuppressWarnings("unchecked")
            boolean ignored = tickUnchecked(ticker, level, pos, state, blockEntity);
            return true;
        } catch (RuntimeException | LinkageError failure) {
            // A workstation that dislikes being ticked twice must not break
            // cooking; the bonus is silently skipped from then on.
            if (!warned) {
                warned = true;
                MealMasteryLog.LOGGER.warn("A workstation threw while being given a mastery "
                        + "speed tick. The speed bonus is disabled for this session.", failure);
            }
            return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean tickUnchecked(BlockEntityTicker ticker, ServerLevel level, BlockPos pos,
                                         BlockState state, BlockEntity blockEntity) {
        ticker.tick(level, pos, state, blockEntity);
        return true;
    }

    /**
     * How practised the player is here, as 0..{@link MasteryRank#highestIndex()}.
     *
     * <p>Prefers the rank of a dish actually sitting in the workstation, which
     * is what a player would expect. While a pot is still cooking there is
     * nothing in its output yet, so it falls back to how much the player has
     * used that method at all — otherwise the bonus would only ever apply on
     * the tick the food finishes, which is useless.</p>
     */
    private static int masteryTier(ServerPlayer player, CookingMethod method,
                                   AbstractContainerMenu menu, ServerConfig config) {
        ProfileManager profiles = ProfileManager.get();
        if (profiles == null) {
            return 0;
        }
        CulinaryProfile profile = profiles.of(player);
        MasteryCurve curve = config.mastery.toCurve();

        int best = 0;
        if (menu != null) {
            for (Slot slot : menu.slots) {
                if (slot.container instanceof net.minecraft.world.entity.player.Inventory) {
                    continue;
                }
                ItemStack stack = slot.getItem();
                if (stack.isEmpty()) {
                    continue;
                }
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
                if (id != null) {
                    best = Math.max(best, profile.rankOf(id, curve).ordinal());
                }
            }
        }

        MethodRecord record = profile.peekMethod(method);
        if (record != null) {
            int familiarity = (int) Math.min(MasteryRank.highestIndex(),
                    record.preparations() / PREPARATIONS_PER_TIER);
            best = Math.max(best, familiarity);
        }
        return best;
    }
}
