package com.xirc.mealmastery.tracking;

import com.xirc.mealmastery.MealMasteryLog;
import com.xirc.mealmastery.config.ConfigManager;
import com.xirc.mealmastery.config.ServerConfig;
import com.xirc.mealmastery.network.Network;
import com.xirc.mealmastery.network.Packets;
import com.xirc.mealmastery.progression.ProgressionService;
import com.xirc.mealmastery.recipe.CookingMethod;
import com.xirc.mealmastery.recipe.CulinaryRegistries;
import com.xirc.mealmastery.recipe.CulinaryRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.BlastFurnaceMenu;
import net.minecraft.world.inventory.CraftingMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.SmokerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Works out who cooked what.
 *
 * <p>Three surfaces, all of them vanilla (see {@code docs/architecture.md}):</p>
 * <ol>
 *   <li><b>Menu sessions.</b> While a player has a crafting, furnace or
 *       registered workstation screen open, their culinary items are diffed
 *       each tick. Storage screens never qualify, so moving cooked food around
 *       a chest is not cooking.</li>
 *   <li><b>Interaction windows.</b> Right-clicking a workstation opens a short
 *       window; culinary items gained inside it are credited. This is how the
 *       skillet, serving a pot with a bowl and taking a feast portion are
 *       caught.</li>
 *   <li><b>Drops inside a window.</b> Item entities spawning next to the block
 *       the player just used — the cutting board.</li>
 * </ol>
 *
 * <p>Anything that produces food with no window and no menu open is not
 * credited at all. Guessing an owner would be worse than tracking nothing
 *.</p>
 */
public final class CookingTracker {

    private static final Map<UUID, PlayerTracking> TRACKING = new ConcurrentHashMap<>();

    private CookingTracker() {
    }

    public static void forget(ServerPlayer player) {
        TRACKING.remove(player.getUUID());
    }

    public static void reset() {
        TRACKING.clear();
    }

    /**
     * A one-line summary of what tracking currently believes about a player,
     * for {@code /mealmastery debug}. This is the fastest way to tell whether a
     * workstation was recognised at all.
     */
    public static String describe(ServerPlayer player, long tick) {
        PlayerTracking state = TRACKING.get(player.getUUID());
        if (state == null) {
            return "no tracking state";
        }
        StringBuilder text = new StringBuilder();
        text.append(state.hasMenuSession()
                ? "menu session via " + state.menuMethod().id()
                : "no menu session");
        if (state.windowPos() != null) {
            text.append(", window at ").append(state.windowPos().toShortString())
                    .append(state.hasWindow(tick) ? " (open)" : " (expired)");
        } else {
            text.append(", no interaction window");
        }
        return text.toString();
    }

    private static PlayerTracking tracking(ServerPlayer player) {
        return TRACKING.computeIfAbsent(player.getUUID(), unused -> new PlayerTracking());
    }

    // ------------------------------------------------------------------ tick

    public static void onServerTick(MinecraftServer server) {
        CulinaryRegistry registry = CulinaryRegistries.current();
        if (registry.size() == 0) {
            return;
        }
        long tick = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            try {
                tickPlayer(player, registry, tick);
            } catch (RuntimeException | LinkageError failure) {
                // One player's odd inventory state must not stop the server tick.
                MealMasteryLog.LOGGER.error("Culinary tracking failed for {}; "
                        + "their session has been reset.", player.getGameProfile().getName(), failure);
                tracking(player).clearAll();
            }
        }
    }

    private static void tickPlayer(ServerPlayer player, CulinaryRegistry registry, long tick) {
        PlayerTracking state = tracking(player);
        AbstractContainerMenu menu = player.containerMenu;
        int menuId = menu == null ? -1 : menu.containerId;

        if (state.hasMenuSession() && !stillCulinary(player, menu, menuId, state, tick)) {
            // The screen changed, or stopped being a cooking surface: settle up
            // before the old baseline is lost.
            settle(player, state, registry, CulinaryCredit.Source.MENU, state.menuMethod());
            state.endMenuSession();
            state.forgetWorkstation();
            // The client draws its stats panel from this, so it has to be told
            // the moment a cooking screen closes.
            Network.toPlayer(player, new Packets.MenuContext(false, null));
        }

        if (!state.hasMenuSession() && menu != null) {
            CookingMethod method = culinaryMethodFor(player, menu, state, tick);
            if (method != null) {
                state.beginMenuSession(menuId, method,
                        CulinaryInventory.snapshot(player, registry));
                // 2x2 crafting is still tracked, but the client is not told to
                // put a panel beside the inventory screen for it.
                if (menu != player.inventoryMenu) {
                    Network.toPlayer(player, new Packets.MenuContext(true, method.id()));
                }
            }
        }

        if (state.hasMenuSession()) {
            settle(player, state, registry, CulinaryCredit.Source.MENU, state.menuMethod());
            com.xirc.mealmastery.progression.CookingSpeed.tick(player, state.workstationPos(),
                    state.menuMethod(), menu);
            return;
        }

        if (state.windowPos() != null) {
            if (!state.windowIsDropOnly()) {
                settle(player, state, registry, CulinaryCredit.Source.INTERACTION,
                        state.windowMethod());
            }
            com.xirc.mealmastery.progression.CookingSpeed.tick(player, state.windowPos(),
                    state.windowMethod(), null);
            if (!state.hasWindow(tick)) {
                state.endWindow();
                state.forgetWorkstation();
            }
        }
    }

    /**
     * Whether an open session should stay open.
     *
     * <p>A different screen always ends it. Beyond that, only the player's own
     * inventory menu is re-examined, and that distinction is the whole point:
     * every other screen gets a fresh container id, but the inventory menu
     * keeps id 0 for the entire session. Ending sessions on id alone therefore
     * left a 2x2 crafting session open forever, after which every item the
     * player ever picked up — a creative give, a chest withdrawal, a mob drop —
     * was credited as if they had cooked it.</p>
     *
     * <p>A workstation screen stays culinary for as long as it is open, even
     * after the interaction window that identified it has expired.</p>
     */
    private static boolean stillCulinary(ServerPlayer player, AbstractContainerMenu menu,
                                         int menuId, PlayerTracking state, long tick) {
        if (menu == null || state.menuId() != menuId) {
            return false;
        }
        if (menu == player.inventoryMenu) {
            return hasItemsInInventoryCraftingGrid(menu);
        }
        return true;
    }

    /**
     * @return the method to credit while this menu is open, or {@code null}
     *         when the menu is not a cooking surface
     */
    private static CookingMethod culinaryMethodFor(ServerPlayer player, AbstractContainerMenu menu,
                                                   PlayerTracking state, long tick) {
        if (menu instanceof CraftingMenu) {
            return CookingMethod.CRAFTING;
        }
        if (menu instanceof SmokerMenu) {
            return CookingMethod.SMOKING;
        }
        if (menu instanceof BlastFurnaceMenu) {
            // Blasting never produces food; treated as not culinary.
            return null;
        }
        if (menu instanceof AbstractFurnaceMenu) {
            return CookingMethod.SMELTING;
        }
        if (menu instanceof InventoryMenu) {
            // The player's own screen is open constantly, so it only counts
            // while something is actually sitting in the 2x2 grid.
            return hasItemsInInventoryCraftingGrid(menu) ? CookingMethod.CRAFTING : null;
        }
        // Any other screen only counts if the player opened it by using a
        // registered workstation moments ago.
        if (state.hasWindow(tick)) {
            return state.windowMethod();
        }
        return null;
    }

    private static boolean hasItemsInInventoryCraftingGrid(AbstractContainerMenu menu) {
        // InventoryMenu slot layout: 0 is the result, 1..4 are the 2x2 grid.
        for (int slot = 1; slot <= 4 && slot < menu.slots.size(); slot++) {
            if (menu.getSlot(slot).hasItem()) {
                return true;
            }
        }
        return false;
    }

    // ----------------------------------------------------------- interaction

    /** Called from the loader's right-click-block event. */
    public static void onWorkstationUsed(ServerPlayer player, BlockPos pos, BlockState state) {
        CookingMethod method = WorkstationRegistry.methodFor(state);
        if (method == null) {
            return;
        }
        CulinaryRegistry registry = CulinaryRegistries.current();
        if (registry.size() == 0) {
            return;
        }
        ServerConfig config = ConfigManager.server();
        PlayerTracking tracking = tracking(player);
        if (tracking.hasMenuSession()) {
            // A menu session is already watching this player's inventory.
            return;
        }
        // A campfire cooks slowly and unattended, and always drops its result
        // as an item at the block. It needs a window long enough to still be
        // open when that happens, and one that credits nothing else meanwhile.
        boolean unattended = CookingMethod.CAMPFIRE.equals(method);
        int ticks = unattended
                ? config.automation.unattendedWindowTicks
                : config.automation.attributionWindowTicks;
        tracking.beginWindow(pos.immutable(), method,
                player.server.getTickCount() + ticks,
                CulinaryInventory.snapshot(player, registry), unattended);
    }

    /** Called when an item entity is added to a server level. */
    public static void onItemEntitySpawned(ItemEntity entity) {
        CulinaryRegistry registry = CulinaryRegistries.current();
        if (registry.size() == 0 || TRACKING.isEmpty()) {
            return;
        }
        ItemStack stack = entity.getItem();
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (itemId == null || !registry.isTracked(itemId)) {
            return;
        }
        if (!(entity.level() instanceof net.minecraft.server.level.ServerLevel level)) {
            return;
        }
        ServerConfig config = ConfigManager.server();
        double radiusSquared = config.automation.attributionRadius * config.automation.attributionRadius;
        long tick = level.getServer().getTickCount();

        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            PlayerTracking state = TRACKING.get(player.getUUID());
            if (state == null || !state.hasWindow(tick) || player.level() != level) {
                continue;
            }
            BlockPos pos = state.windowPos();
            if (pos.distToCenterSqr(entity.getX(), entity.getY(), entity.getZ()) > radiusSquared) {
                continue;
            }
            // Credit now, and remember it so picking the drop up later does not
            // count a second time.
            state.noteCredited(itemId, stack.getCount());
            ProgressionService.credit(new CulinaryCredit(player, itemId, stack.getCount(),
                    state.windowMethod(), CulinaryCredit.Source.DROP,
                    Set.copyOf(state.consumedIngredients()), false));
            return;
        }
    }

    // --------------------------------------------------------------- settling

    private static void settle(ServerPlayer player, PlayerTracking state, CulinaryRegistry registry,
                               CulinaryCredit.Source source, CookingMethod sessionMethod) {
        Map<ResourceLocation, Integer> now = CulinaryInventory.snapshot(player, registry);
        Map<ResourceLocation, Integer> before = state.baseline();

        for (ResourceLocation consumed : CulinaryInventory.losses(before, now).keySet()) {
            if (registry.allIngredients().contains(consumed)) {
                state.noteConsumed(consumed);
            }
        }

        Map<ResourceLocation, Integer> gains =
                CulinaryInventory.gains(before, now, state.creditedThisSession());
        if (!gains.isEmpty() && creditingAllowed(player)) {
            Set<ResourceLocation> ingredients = new LinkedHashSet<>(state.consumedIngredients());
            gains.forEach((itemId, amount) -> {
                if (!registry.isTracked(itemId)) {
                    return;
                }
                CookingMethod method = resolveMethod(sessionMethod, registry, itemId);
                if (!plausible(registry, itemId, method)) {
                    return;
                }
                ProgressionService.credit(new CulinaryCredit(player, itemId, amount, method,
                        source, ingredients, false));
            });
        }
        // Re-snapshot rather than reusing `now`: crediting can itself add items
        // (a mastery batch bonus), and those must not look like a fresh gain on
        // the next tick.
        state.rebaseline(CulinaryInventory.snapshot(player, registry));
    }

    /**
     * Creative players earn mastery for cooking exactly like anyone else. What
     * they must not earn it for is pulling a stack out of the creative tabs.
     *
     * <p>Those two look identical from the server's side, because the creative
     * screen is client-only: grabbing an item sends a set-slot packet while
     * {@code containerMenu} stays the player's own inventory menu. So the rule
     * is narrow — in creative, a gain arriving while nothing but the inventory
     * menu is open is not credited. Every real workstation (pot, crafting
     * table, furnace, skillet, cutting board) opens its own menu or an
     * interaction window, and still counts.</p>
     *
     * <p>The cost is that 2x2 inventory crafting earns nothing in creative,
     * which is the right trade: it is indistinguishable from grabbing.</p>
     */
    private static boolean creditingAllowed(ServerPlayer player) {
        return !player.isCreative() || player.containerMenu != player.inventoryMenu;
    }

    /**
     * Whether this dish could actually have come out of this workstation.
     *
     * <p>Gaining bread while stood at a Cooking Pot is not pot cooking, so it
     * is not credited as any. Methods the registry cannot speak for — a
     * workstation added through configuration, or serving a feast, whose
     * portions are made elsewhere — are let through rather than guessed at.</p>
     */
    private static boolean plausible(CulinaryRegistry registry, ResourceLocation itemId,
                                     CookingMethod method) {
        if (method == null || method.equals(CookingMethod.UNKNOWN)
                || method.equals(CookingMethod.FEAST_SERVING)) {
            return true;
        }
        var entry = registry.entry(itemId);
        return entry != null && entry.usesMethod(method);
    }

    /**
     * A workstation added through configuration may not know which method it
     * represents, in which case the dish's own primary method is the better
     * answer than "unknown".
     */
    private static CookingMethod resolveMethod(CookingMethod sessionMethod,
                                               CulinaryRegistry registry,
                                               ResourceLocation itemId) {
        if (sessionMethod != null && !sessionMethod.equals(CookingMethod.UNKNOWN)) {
            return sessionMethod;
        }
        var entry = registry.entry(itemId);
        return entry == null ? CookingMethod.UNKNOWN : entry.primaryMethod();
    }
}
