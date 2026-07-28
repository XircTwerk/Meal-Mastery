# Changelog

All notable changes to this project are documented here.

## [Unreleased]

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
  Defaults leave Farmer's Delight completely unchanged: cosmetic-only mastery
  rewards, no automation credit, personal discovery, no leaderboards, no HUD.
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
