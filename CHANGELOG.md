# Changelog

All notable changes to this project are documented here.

This is the **1.20.1** branch (Fabric + Forge). For Minecraft 1.21.1
(Fabric + NeoForge) see the [`1.21.1`](https://github.com/XircTwerk/Meal-Mastery/tree/1.21.1)
branch. There is no `main` branch: each Minecraft version is its own branch.

## [1.0.2] - 2026-08-09

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

## [1.0.1] - 2026-08-04

### Fixed
- Closed an exploit where moving a tracked item into a crafting grid or furnace
  slot and taking it straight back out was credited as a preparation, awarding
  XP, mastery and batch bonuses on repeat. Items held in a menu's input slots
  are now counted as still carried, so the move is net zero.

## [1.0] - 2026-07-28

### Added
- Multiloader project scaffold (Fabric + Forge + shared `common`) for Minecraft
  1.20.1 on Java 17.
- `ServiceLoader`-backed platform abstraction.
- Upstream audit of Farmer's Delight `1.20.1-1.3.2` (Forge) and
  `1.20.1-2.4.1+refabricated` (Fabric).
- Compatibility audit of overlapping progression, nutrition and journal mods.
- Architecture notes covering the no-compile-time-dependency design and the
  cooking attribution model.
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
- Server and client configuration documents with range validation, plus the
  five optional presets (Vanilla+, Cozy, Completionist, Server, Cosmetic).
  Defaults never alter a Farmer's Delight recipe or a food item's own stats:
  `mastery.rewards` is `COSMETIC_ONLY` (that setting shapes the XP side only),
  no automation credit, personal discovery, no leaderboards, no HUD. Note that
  `mastery.bonuses.enabled` *is* `true` by default, so mastery does affect
  cooking speed, saturation and effects out of the box — see
  `docs/configuration.md`.
- Cooking attribution using three vanilla surfaces only: menu sessions,
  short interaction windows after using a workstation, and item drops inside
  those windows. Automation that cannot be attributed grants no credit.
- Progression service applying preparations to profiles: discovery, ingredient
  and method journals, mastery points and ranks, Cooking XP, statistics,
  streaks, personal records and the activity feed.
- XP engine with the documented diminishing-returns curve, a capped
  experimentation bonus and a capped mastery bonus.
- Per-world, per-player profile storage with atomic writes and autosave.
- Internal event bus; loader bridges for Forge (events only) and Fabric
  (Fabric API callbacks plus one narrow read-only mixin on eating).
- Datapack-driven challenge engine with fourteen objective types, chained
  challenges, and rewards (Cooking XP, vanilla XP, advancements, existing
  items, cosmetic badges, and commands behind an explicit opt-in).
- Themed collections defined by items, tags, mods or categories, plus per-mod
  collections generated automatically from whatever is installed.
- Data-driven milestones, cosmetic culinary titles, and journal badges built
  from existing item icons.
- Built-in challenges, collections and milestones shipped as ordinary datapack
  files, so a pack can override any of them.
- Networking: chunked dish catalogue, full and delta profile syncs, per-dish
  detail on request, progression rules pushed by the server, and notification
  packets. The client-to-server surface is two data requests and three personal
  preferences, all re-validated server-side.
- Commands under `/mealmastery` (aliases `/mealmasters`, `/mm`): stats, recipe,
  challenges, collections, titles, export, plus permission-gated audit, debug,
  reload and the admin subtree (inspect, addxp, setlevel, discover, mastery,
  reset, purgeorphans).
- Full English localisation.
- Culinary journal screen: persistent sidebar, header with level, search box,
  and pages for Overview, Recipes, Ingredients, Methods, Collections,
  Challenges, Statistics and Records. Every surface is drawn from code — panels,
  gradients, bars and rank pips — plus item icons other mods already registered.
- Virtualised recipe browser with filters, sorting, search and a detail panel;
  undiscovered dishes respect the configured display mode.
- Optional compact HUD with the pinned-recipe tracker, item tooltips behind a
  modifier key, journal keybind, and aggregated rate-limited notifications.
- Feast serving tracked separately from cooking: taking a portion out of a
  placed feast counts as serving, and the mod never claims to know who ate it.
- Recipe of the Day and generated daily challenges, seeded by the Minecraft day
  and built only from dishes the server can actually make.
- Optional opt-in leaderboards exposing culinary statistics and nothing else.
- Compatibility report distinguishing Detected, Generic Support and Compatible;
  "Full Compatibility" is never claimed.
- `/mealmastery leaderboard`, `compat` and `today`.
- Small public API: workstation registration, direct credit, progress queries
  and event subscription.
- Client settings screen with the compatibility list.
- Documentation for configuration, datapacks and the API.
- Verified on a live Fabric dev server with Farmer's Delight installed:
  92 dishes from 140 eligible recipes across 2 mods in 16 ms, plus the shipped
  challenges, collections and milestones.

### Packaging
- Licensed MIT.
- A 256x256 `assets/mealmastery/icon.png` is shipped as the mod-list icon,
  wired to `icon` in `fabric.mod.json` and `logoFile` in the Forge TOML. This
  is the only image in the mod. It is loader-facing metadata, never referenced
  by mod code and never rendered in game, so the no-in-game-art rule is
  unchanged.
- Loader metadata completed: `contact` (homepage, sources, issues) on Fabric;
  `issueTrackerURL` and `displayURL` on Forge.
