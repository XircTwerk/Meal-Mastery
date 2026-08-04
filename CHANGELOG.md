# Changelog

All notable changes to this project are documented here.

This is the **1.21.1** branch (Fabric + NeoForge). For Minecraft 1.20.1
(Fabric + Forge) see the [`1.20.1`](https://github.com/XircTwerk/Meal-Mastery/tree/1.20.1)
branch. There is no `main` branch: each Minecraft version is its own branch.

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
