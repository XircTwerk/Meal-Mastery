package com.xirc.mealmastery.tracking;

import com.xirc.mealmastery.recipe.CookingMethod;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Per-player attribution state.
 *
 * <p>Two independent sessions can be open at once conceptually, but never
 * simultaneously in practice: a menu session (the player has a workstation,
 * crafting or furnace screen open) takes precedence over an interaction window
 * (the player just right-clicked a workstation), because a player with the pot
 * open is not also being handed a skillet portion.</p>
 */
public final class PlayerTracking {

    /** {@code containerId} of the menu the current session belongs to; -1 when none. */
    private int menuId = -1;
    private CookingMethod menuMethod = CookingMethod.UNKNOWN;

    private Map<ResourceLocation, Integer> baseline = Map.of();
    private final Map<ResourceLocation, Integer> creditedThisSession = new HashMap<>();
    private final Set<ResourceLocation> consumedIngredients = new LinkedHashSet<>();

    /**
     * The workstation this player last used. Outlives the interaction window,
     * because the mastery speed bonus needs to know which block to accelerate
     * for as long as its screen stays open.
     */
    private BlockPos workstationPos;
    private CookingMethod workstationMethod;

    private BlockPos windowPos;
    private CookingMethod windowMethod;
    private long windowExpiresAtTick = Long.MIN_VALUE;

    public boolean hasMenuSession() {
        return menuId != -1;
    }

    public int menuId() {
        return menuId;
    }

    public CookingMethod menuMethod() {
        return menuMethod;
    }

    public void beginMenuSession(int menuId, CookingMethod method,
                                 Map<ResourceLocation, Integer> baseline) {
        this.menuId = menuId;
        this.menuMethod = method == null ? CookingMethod.UNKNOWN : method;
        this.baseline = baseline;
        creditedThisSession.clear();
        consumedIngredients.clear();
    }

    public void endMenuSession() {
        menuId = -1;
        menuMethod = CookingMethod.UNKNOWN;
        baseline = Map.of();
        creditedThisSession.clear();
        consumedIngredients.clear();
    }

    public boolean hasWindow(long currentTick) {
        return windowPos != null && currentTick <= windowExpiresAtTick;
    }

    public BlockPos windowPos() {
        return windowPos;
    }

    public CookingMethod windowMethod() {
        return windowMethod;
    }

    public BlockPos workstationPos() {
        return workstationPos;
    }

    public CookingMethod workstationMethod() {
        return workstationMethod;
    }

    public void beginWindow(BlockPos pos, CookingMethod method, long expiresAtTick,
                            Map<ResourceLocation, Integer> baseline) {
        this.workstationPos = pos;
        this.workstationMethod = method;
        this.windowPos = pos;
        this.windowMethod = method;
        this.windowExpiresAtTick = expiresAtTick;
        this.baseline = baseline;
        creditedThisSession.clear();
        consumedIngredients.clear();
    }

    public void endWindow() {
        windowPos = null;
        windowMethod = null;
        windowExpiresAtTick = Long.MIN_VALUE;
        if (!hasMenuSession()) {
            baseline = Map.of();
            creditedThisSession.clear();
            consumedIngredients.clear();
        }
    }

    public Map<ResourceLocation, Integer> baseline() {
        return baseline;
    }

    public void rebaseline(Map<ResourceLocation, Integer> snapshot) {
        this.baseline = snapshot;
        creditedThisSession.clear();
    }

    public Map<ResourceLocation, Integer> creditedThisSession() {
        return creditedThisSession;
    }

    /**
     * Marks an amount as already credited by another surface so the next
     * inventory diff ignores it.
     */
    public void noteCredited(ResourceLocation item, int amount) {
        creditedThisSession.merge(item, amount, Integer::sum);
    }

    /**
     * Ingredients seen leaving the player's inventory during this session. This
     * is the only honest way to know which variant of a tagged ingredient was
     * used, and it is empty whenever the inputs were not observable.
     */
    public Set<ResourceLocation> consumedIngredients() {
        return consumedIngredients;
    }

    public void noteConsumed(ResourceLocation item) {
        // Bounded so a long session at a workstation cannot grow without limit.
        if (consumedIngredients.size() < 64) {
            consumedIngredients.add(item);
        }
    }

    public void forgetWorkstation() {
        workstationPos = null;
        workstationMethod = null;
    }

    public void clearAll() {
        endMenuSession();
        endWindow();
        forgetWorkstation();
    }
}
