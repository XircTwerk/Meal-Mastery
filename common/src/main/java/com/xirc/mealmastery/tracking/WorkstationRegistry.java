package com.xirc.mealmastery.tracking;

import com.xirc.mealmastery.Constants;
import com.xirc.mealmastery.MealMasteryLog;
import com.xirc.mealmastery.recipe.CookingMethod;
import com.xirc.mealmastery.recipe.CulinaryTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Which blocks count as cooking workstations.
 *
 * <p>This is the one place a Farmer's Delight identifier is written down, and
 * it is deliberate: the four workstation blocks belong to the mod Meal Mastery
 * declares as a hard dependency, not to an addon. Food addons overwhelmingly
 * reuse those same blocks, which is why they need no entry here at all — the
 * generic detection in {@code CulinaryRegistry} already picked up their
 * recipes.</p>
 *
 * <p>Addons with a genuinely unique workstation extend this through the
 * {@code mealmastery:workstations} block tag, the public API, or the
 * {@code compatibility.extraWorkstationBlocks} config list — never by a code
 * change here.</p>
 */
public final class WorkstationRegistry {

    private static final Map<ResourceLocation, CookingMethod> DEFAULTS = defaults();

    private static final Map<ResourceLocation, CookingMethod> registered =
            new ConcurrentHashMap<>(DEFAULTS);

    private WorkstationRegistry() {
    }

    private static Map<ResourceLocation, CookingMethod> defaults() {
        Map<ResourceLocation, CookingMethod> map = new LinkedHashMap<>();
        String fd = Constants.FARMERS_DELIGHT_ID;
        map.put(new ResourceLocation(fd, "cooking_pot"), CookingMethod.COOKING_POT);
        map.put(new ResourceLocation(fd, "cutting_board"), CookingMethod.CUTTING_BOARD);
        map.put(new ResourceLocation(fd, "skillet"), CookingMethod.of(fd, "skillet"));
        map.put(new ResourceLocation(fd, "stove"), CookingMethod.of(fd, "stove"));
        // Vanilla's only cooking block without a screen. Everything else -
        // furnace, smoker, crafting table - is recognised by its menu instead,
        // which is why a campfire counted for nothing until it was listed here.
        map.put(new ResourceLocation("campfire"), CookingMethod.CAMPFIRE);
        map.put(new ResourceLocation("soul_campfire"), CookingMethod.CAMPFIRE);
        return map;
    }

    /**
     * @return the method to credit, or {@code null} when the block is not a
     *         workstation. Storage blocks never match, which is what stops
     *         shuffling cooked food around a chest from counting as cooking.
     */
    public static CookingMethod methodFor(BlockState state) {
        ResourceLocation blockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        if (blockId == null) {
            return null;
        }
        CookingMethod method = registered.get(blockId);
        if (method != null) {
            return method;
        }
        // Placed feasts are workstations for serving purposes: right-clicking
        // one with a bowl hands the player a portion.
        if (state.is(CulinaryTags.FEAST_BLOCKS)) {
            return CookingMethod.FEAST_SERVING;
        }
        // Anything a datapack has named. UNKNOWN because the tag carries no
        // method; that credits mastery for the dish without claiming to know
        // how the block cooked it.
        if (state.is(CulinaryTags.WORKSTATION_BLOCKS)) {
            return CookingMethod.UNKNOWN;
        }
        return null;
    }

    public static boolean isWorkstation(BlockState state) {
        return methodFor(state) != null;
    }

    public static void register(ResourceLocation blockId, CookingMethod method) {
        registered.put(blockId, method);
    }

    /** Rebuilt from configuration on every reload so removing an entry takes effect. */
    public static void applyConfiguration(Iterable<String> extraBlocks) {
        registered.clear();
        registered.putAll(DEFAULTS);
        for (String raw : extraBlocks) {
            // "modid:block" or "modid:block=modid:method"
            String[] halves = raw.split("=", 2);
            ResourceLocation blockId = ResourceLocation.tryParse(halves[0].trim());
            if (blockId == null) {
                MealMasteryLog.LOGGER.warn(
                        "compatibility.extraWorkstationBlocks contains an unparseable block id: '{}'",
                        raw);
                continue;
            }
            CookingMethod method = CookingMethod.UNKNOWN;
            if (halves.length == 2) {
                ResourceLocation methodId = ResourceLocation.tryParse(halves[1].trim());
                if (methodId == null) {
                    MealMasteryLog.LOGGER.warn(
                            "compatibility.extraWorkstationBlocks contains an unparseable method id: "
                                    + "'{}'", raw);
                    continue;
                }
                method = CookingMethod.of(methodId);
            }
            registered.put(blockId, method);
        }
    }

    public static Map<ResourceLocation, CookingMethod> all() {
        return Map.copyOf(registered);
    }
}
