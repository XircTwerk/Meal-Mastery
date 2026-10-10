# Changelog

All notable changes to this project are documented here.

This is the **1.21.1** branch (Fabric + NeoForge). For Minecraft 1.20.1
(Fabric + Forge) see the [`1.20.1`](https://github.com/XircTwerk/Meal-Mastery/tree/1.20.1)
branch. There is no `main` branch: each Minecraft version is its own branch.

## [1.1.4] - 2026-10-10

### Fixed
- Putting a cooked dish into a Cooking Pot's ingredient grid and taking it
  straight back out is no longer treated as cooking it again. It paid out
  Cooking XP, pushed the dish's prepared count and star rating up, and
  eventually handed over a free second portion through the extra-portion roll.
  Food sitting in any workstation's input slots now counts as food the player
  is still carrying, so moving it in and out is worth nothing. Taking a
  finished dish out of a workstation is still credited exactly as before.
  (#2)
- Cooked food fragments into stacks that will not merge far less often. Rating
  a batch used to split the stack it landed in, so cooking three more into a
  pile of twenty left seventeen in one slot and three in another, permanently
  unmergeable — and a pile cooked into repeatedly fragmented further every
  time. A rating is now only written onto a stack that the batch accounts for
  in full.
- Food produced by a cutting board no longer causes a rating to be written onto
  an unrelated stack. The board drops its result on the floor, so there was
  nothing in the inventory to rate; the mod rated whatever matching food the
  player happened to be carrying instead, which is why identical dishes
  sometimes refused to stack.
- Meal Mastery notifications no longer crash the game alongside Fancy Notify.
  The mod announced itself through a vanilla system toast carrying an identity
  it had made up, and a mod that recognises system toasts by vanilla's own set
  had nothing to match it against. Meal Mastery now draws its own toast, using
  the same vanilla sprite and layout, and adds no texture of its own. (#3)
- The settings screen no longer runs its toggles down into the buttons below
  them when there are enough of them to fill the panel.

### Added
- `mastery.bonuses.stampFood`, on by default. Turn it off and no rating is ever
  written onto food, so every stack merges the way vanilla intends. Eating
  bonuses then follow the eater's own mastery with the dish rather than the
  cook's — for your own cooking that is the same number — and cooking for other
  people stops earning mastery.
- Challenge and milestone notifications can be turned off in the settings
  screen. Both already existed in the config file, but the screen had no
  button for either, so turning off a popup meant editing the file by hand.
  (#3)
- `tooltips.collapsedShowRating` and `tooltips.collapsedShowHint`, both on by
  default and both in the settings screen. They control what is left on a
  tooltip while the modifier is not held: turn both off and Meal Mastery adds
  nothing at all until the modifier is pressed. (#1)

## [1.1.3] - 2026-09-13

### Fixed
- Closing a Cooking Pot menu no longer leaves its interaction tracker active,
  preventing repeated open/close cycles from granting cooking XP, mastery or
  bonus dishes for items already in the player's inventory.
- Meal Mastery's saturation bonus now respects Saturation Plus's raised cap
  when that mod is installed.
- HUD, overview highlights, method statistics and records labels can now be
  translated through `lang` files.

### Compatibility
- Manual crafting, smelting and smoking performed through Sophisticated
  Backpacks or Sophisticated Storage upgrades now grants Meal Mastery progress.
  Automated upgrades remain uncredited because they have no responsible player.
- When Spice of Life: Valheim Reforged is installed, Meal Mastery no longer
  displays or applies its vanilla saturation bonus; recipe and consumption
  tracking continue through the existing generic food support.

## [1.1.2] - 2026-08-09

### Added
- Campfires now count as cooking. Every other cooking block is recognised by
  its screen — furnace, smoker and crafting table all have one — and a campfire
  has none, so it was never treated as a workstation at all. Cooking on one now
  earns mastery, XP and discovery like anywhere else.
- Workstations are now a block tag, `mealmastery:workstations`. Any mod or pack
  with a cooking block of its own is added by datapack, with no code change and
  no release. Entries use `required: false`, so one file is safe to ship for a
  whole pack whether or not the mod is installed.
- Cooking for Blockheads works on install: its oven, cooking table and toaster
  are listed in the mod's own copy of that tag. Crafting and cooking in them
  previously earned nothing, because a non-vanilla screen only counted if the
  player had opened it from a workstation Meal Mastery already knew.
- `automation.unattendedWindowTicks` (1200), the attribution window for a
  workstation that cooks unattended. A campfire takes thirty seconds, where the
  existing twenty-tick window expired before the food existed. Only items
  appearing at the block itself are credited during it, so the longer window
  cannot credit something picked up meanwhile.
- `compatibility.ignoreUnpackingRecipes` (on by default).

### Fixed
- Crafting a storage block back into the food it holds no longer pays out.
  Nine potatoes out of a sack or crate was being credited as nine preparations;
  Quark, Farmer's Delight and Farm and Charm all ship recipes of this shape.
  One ingredient in and several of the same food out is not a shape any cooking
  has. Restricted to vanilla crafting, so a cutting board turning one input
  into several portions still counts.
- Stamping a partly-cooked stack no longer routes through `Inventory#add`.
  Splitting a stack to stamp only the newly cooked portion was synthesising an
  inventory insert in the middle of a craft, which mods that hook item pickups
  can observe as a second event. The stamped portion is placed directly into a
  free slot instead, and when there is no free slot the batch stays unstamped
  rather than moving items to force it.

### Unchanged
- Blast furnaces remain excluded. Blasting produces no food, so a pack that
  turns blasted food into charcoal or nuggets correctly earns nothing from it.

## [1.1.1] - 2026-08-04

### Fixed
- Closed an exploit where moving a tracked item into a crafting grid or furnace
  slot and taking it straight back out was credited as a preparation, awarding
  XP, mastery and batch bonuses on repeat. Items held in a menu's input slots
  are now counted as still carried, so the move is net zero.

## [1.1] - 2026-08-01

### Fixed
- Corrected the NeoForge dependency range so Farmer's Delight 1.3.2 is
  accepted.
- Updated the Fabric development runtime and required dependency to Farmer's
  Delight Refabricated 3.3.3.

## [1.0] - 2026-07-28

### Ported to Minecraft 1.21.1
- Replaced the `forge` module with `neoforge`: Farmer's Delight has no Forge
  build for 1.21.1.
- Java 21, NeoForm-based `common` module, Gradle 8.14.
- Recipes read through `RecipeHolder`; food read from the `DataComponents.FOOD`
  component; advancements through `AdvancementHolder`.
- Networking rebuilt on vanilla `CustomPacketPayload`, so a single envelope type
  is now shared by both loaders instead of one wrapper per loader.
- Ingredient categories moved to the unified `c:` conventional tags; the
  `forge:` namespace no longer exists.
- Fabric's eating mixin retargeted to
  `LivingEntity#eat(Level, ItemStack, FoodProperties)`.
- HUD registered as a GUI layer rather than an overlay.

### Features
- Persistent `CulinaryProfile`: per-dish records (discovery, preparation,
  consumption, serving, mastery points, per-method counts, ingredient
  variants), ingredient and cooking-method journals, lifetime statistics,
  personal records and cooking/variety/discovery streaks.
- Configurable Cooking Level and mastery curves, plus profile schema migration
  that preserves unrecognised sections instead of erasing them.
- Culinary registry built from the recipe manager at every reload: recipe
  classification, tag-derived dish and ingredient categories, cooking methods
  discovered from recipe types, and indexes by method, source mod, category and
  ingredient.
- Eligibility rules with allow/deny lists by recipe, item, mod, tag and recipe
  type on top of the default "produces something edible" rule.
- Journal search supporting bare terms and `field:term` selectors.
- Audit report backing `/mealmastery audit`.
- Server and client configuration with range validation, plus the five optional
  presets. Defaults never alter a Farmer's Delight recipe or a food item's own
  stats: `mastery.rewards` is `COSMETIC_ONLY` (that setting shapes the XP side
  only), no automation credit, personal discovery, no leaderboards, no HUD.
  Note that `mastery.bonuses.enabled` *is* `true` by default, so mastery does
  affect cooking speed, saturation and effects out of the box — see
  `docs/configuration.md`.
- Cooking attribution using three vanilla surfaces only: menu sessions, short
  interaction windows after using a workstation, and item drops inside those
  windows. Automation that cannot be attributed grants no credit.
- Progression service applying preparations to profiles: discovery, ingredient
  and method journals, mastery points and ranks, Cooking XP, statistics,
  streaks, personal records and the activity feed.
- XP engine with the documented diminishing-returns curve, a capped
  experimentation bonus and a capped mastery bonus.
- Per-world, per-player profile storage with atomic writes and autosave.
- Datapack-driven challenge engine with fourteen objective types, chained
  challenges, and rewards (Cooking XP, vanilla XP, advancements, existing
  items, cosmetic badges, and commands behind an explicit opt-in).
- Themed collections defined by items, tags, mods or categories, plus per-mod
  collections generated automatically from whatever is installed.
- Data-driven milestones, cosmetic culinary titles, and journal badges built
  from existing item icons.
- Networking: chunked dish catalogue, full and delta profile syncs, per-dish
  detail on request, progression rules pushed by the server, and notification
  packets. The client-to-server surface is two data requests and three personal
  preferences, all re-validated server-side.
- Commands under `/mealmastery` (aliases `/mealmasters`, `/mm`).
- Culinary journal screen with pages for Overview, Recipes, Ingredients,
  Methods, Collections, Challenges, Statistics and Records, all drawn in code.
- Virtualised recipe browser with filters, sorting, search and a detail panel.
- Optional compact HUD with the pinned-recipe tracker, item tooltips behind a
  modifier key, journal keybind, and aggregated rate-limited notifications.
- Feast serving tracked separately from cooking.
- Recipe of the Day and generated daily challenges.
- Optional opt-in leaderboards; compatibility report that never claims "Full".
- Small public API and full English localisation.

### Packaging
- Licensed MIT.
- A 256x256 `assets/mealmastery/icon.png` is shipped as the mod-list icon,
  wired to `icon` in `fabric.mod.json` and `logoFile` in the NeoForge TOML.
  This is the only image in the mod. It is loader-facing metadata, never
  referenced by mod code and never rendered in game, so the no-in-game-art
  rule is unchanged.
- Loader metadata completed: `contact` (homepage, sources, issues) on Fabric;
  `issueTrackerURL` and `displayURL` on NeoForge.

### Verified
- 90 unit tests passing.
- Live Fabric 1.21.1 dev server with Farmer's Delight installed: 92 dishes from
  140 eligible recipes across 2 mods in 5 ms, plus the shipped challenges,
  collections and milestones.
