# Configuration

Two TOML documents in the config directory:

```
config/mealmastery-server.toml    server-authoritative, world behaviour
config/mealmastery-client.toml    client-only, purely presentation
```

Values outside a supported range are **clamped, not rejected** — a config typo
must never cost anyone their world. Corrections are logged and written back. A
file that cannot be parsed at all is renamed to `*.invalid` and replaced with
defaults so the original is still recoverable.

**Every setting is commented in the file itself**, including its range and the
accepted values for anything enumerated, so the file is usually quicker to read
than this page. It is rewritten on every load: new settings appear with their
comments, and values you have already set are preserved.

A `mealmastery-*.json` left over from before the format changed is read once,
converted, and renamed to `.json.bak` — nothing you had tuned is lost, and the
original is kept in case the conversion misreads something.

Reload both with `/mealmastery reload`.

## What the defaults do

Mastery has real mechanical effects out of the box — see `mastery.bonuses`
below. Everything else stays out of the way:

| | |
|---|---|
| `mastery.bonuses.enabled` | `true` — mastery affects cooking and eating |
| `mastery.rewards` | `COSMETIC_ONLY` — governs the XP-side shaping only |
| `automation.credit` | `NO_CREDIT` — machines earn nobody anything |
| `discovery.shared` | `PERSONAL` |
| `multiplayer.leaderboardsEnabled` | `false` |
| `challenges.useRealTimeSchedule` | `false` |
| `challenges.allowCommandRewards` | `false` |
| `hud.enabled` (client) | `false` |
| `tooltips.showNutrition` (client) | `false` |

---

## Server settings

### `progression`

XP rates and the level curve. `xpForLevel(n) = base + linear*(n-1) + quadratic*(n-1)²`;
the defaults make level 27 cost 1,738 XP.

Bonuses: `discoveryBonusXp` (75) dwarfs a repeat preparation (6), which is what
makes exploring worth more than grinding. `experimentationDailyCap` stops
ingredient variants being cycled for XP.

### `antiFarming`

Diminishing returns per dish per period. Defaults reproduce the documented
curve: first preparation of the day at 100%, the next four at 75%, the next ten
at 50%, everything after at the 20% floor. The floor is deliberately not zero —
repetition should be worth less, not worthless.

Set `enabled: false` to remove the curve entirely.

### `mastery`

`thresholds` is one entry per rank above *Unfamiliar* (default
`[2, 20, 60, 150, 300]`, so the top rank is 300 preparations of one dish).
**Changing them never destroys earned points**; only the displayed rank
re-derives.

`rewards` shapes the **XP** side only: `COSMETIC_ONLY` (default), `LIGHT`,
`STANDARD` or `CUSTOM`. `xpBonusPerMasteredDish` makes mastering dishes speed up
progression rather than changing food.

### `mastery.bonuses`

What mastery does in play. On by default; set `enabled` to `false` for
statistics-only behaviour.

| Field | Default | Effect |
|---|---|---|
| `cookingSpeedAtMaxRank` | `1.0` | Extra workstation ticks at full mastery; `1.0` is twice as fast. Pot, skillet and stove, plus the smoker below |
| `accelerateSmokers` | `true` | Whether the speed bonus reaches the vanilla smoker. Set `false` to leave furnace timings alone |
| `starsEnabled` | `true` | Star rating under food names |
| `effectChanceAtMaxRank` | `0.25` | Chance of a bonus effect when eating a mastered dish |
| `effectDurationSeconds` | `20` | How long that effect lasts |
| `effects` | regeneration, speed, haste | Vanilla effect ids to roll from |
| `effectAmplifierAtMaxRank` | `0` | Amplifier at full mastery, scaled down by rank |
| `saturationPerRank` | `0.2` | Extra saturation per star. Hunger is never touched |
| `perfectChanceAtMaxRank` | `0.08` | Chance a preparation comes out perfect |
| `perfectEffectMultiplier` | `2.0` | How much stronger a perfect dish is |
| `extraPortionChanceAtMaxRank` | `0.12` | Chance of a free extra portion |
| `toolRefundChanceAtMaxTier` | `0.25` | Chance a cutting-board cook refunds a point of knife durability |
| `masteryPerMealServedToOthers` | `2` | Mastery the cook earns when someone else eats their food |

All chances scale linearly from zero at *Unfamiliar* to the configured value at
*Mastered*, so none of it applies to a first attempt. **The real odds for a dish
are printed on its tooltip** behind the modifier key, so a retuned pack states
its own numbers rather than leaving players to guess.

`minecraft:saturation` is deliberately **not** in the default effect list. It is
not a buff: it refills food and saturation every tick it runs, so even a short
one turns any dish into several full meals. A pack can add it back, but that
should be a choice.

**Cooked food carries a stamp** — the cook's stars, whether the attempt was
perfect, and who they were. This is the one place the mod writes to an item. An
unstamped stack is byte-identical to vanilla food, and stamped stacks refuse to
merge with unstamped ones so a good batch is not diluted by a bad one.

**Creative mode** credits cooking normally, with one exception: a gain arriving
while nothing but the inventory screen is open is ignored, because that is
indistinguishable from taking items out of the creative tabs. Every real
workstation still counts.

### `discovery`

`mode` is `COOK_RECIPE` (default), `EAT_OUTPUT`, `OBTAIN_OUTPUT`, `VIEW_RECIPE`
or `ALWAYS_VISIBLE`. `shared` is `PERSONAL`, `TEAM` or `SERVER`.

### `streaks`

`clock` is `MINECRAFT_DAYS` (default), `REAL_DAYS` or `DISABLED`. `graceDays`
is how many idle days are forgiven before a streak resets.

### `challenges`

Daily challenges are generated from what is installed, so an objective is never
impossible. `recipeOfTheDayEnabled` rotates a bonus dish per Minecraft day.
`allowCommandRewards` gates datapack command rewards.

### `multiplayer`

Leaderboards are off by default and opt-in per player on top of that — a player
opts in by having the `mealmastery_leaderboard` scoreboard tag:

```mcfunction
tag @s add mealmastery_leaderboard
```

`inspectPermissionLevel` and `adminPermissionLevel` gate the admin commands.

### `automation`

`credit` is `NO_CREDIT` (default), `OWNER_CREDIT`, `REDUCED_CREDIT` or
`FULL_CREDIT`. `attributionWindowTicks` and `attributionRadius` control how long
after using a workstation, and how close to it, a gain is still credited. Short
on purpose: longer windows start crediting coincidences.

### `compatibility`

The escape hatches for eligibility. The default rule is structural — a recipe
counts when it produces something edible, whichever mod defines it — and these
narrow or widen it.

**Vanilla recipes count by default.** Bread, a baked potato and anything out of
a smoker earn mastery on the same terms as a Farmer's Delight stew, and the
journal lists them. `trackVanillaRecipes: false` turns the whole `minecraft`
namespace off in one go, for packs that want the journal to be about their food
addons; it leaves every other mod untouched.

| Field | Effect |
|---|---|
| `excludedRecipeTypes` | recipe type ids to ignore |
| `excludedItems` | dish item ids to ignore |
| `trackVanillaRecipes` | set `false` so only modded food earns mastery |
| `excludedMods` | mod ids to ignore |
| `includedOnlyMods` | when non-empty, only these mods contribute |
| `requiredItemTags` | a dish must carry one of these tags |
| `requireEdibleOutput` | set `false` to accept non-food outputs |
| `extraWorkstationBlocks` | `"modid:block"` or `"modid:block=modid:method"` |

Unparseable ids are reported in the log rather than silently ignored.

### `advanced`

`autosaveIntervalMillis`, `activityHistorySize`, `maxPinnedRecipes` and
`journalPageSize` (how many dishes travel per catalogue packet).

---

## Client settings

Presentation only. A client cannot change progression — the server pushes the
curves it uses so a mismatched client cannot even mis-render a progress bar.

* `journal` — scale, `unknownDisplay` (`HIDDEN`, `SILHOUETTE`, `NAME_ONLY`,
  `INGREDIENT_HINTS`, `FULL_RECIPE`), `cozyMode`, `completionistMode`,
  `showStars`, and `sidePanel` (`CULINARY` by default, `ALWAYS`, or `OFF`) —
  the stats panel docked beside a cooking screen. It never appears beside the
  inventory or the creative tabs regardless of this setting
* `hud` — off by default; `onlyWhileActive` shows it briefly after XP is gained
* `notifications` — each type suppressible individually, plus aggregation and a
  per-second toast cap
* `tooltips` — modifier key, and a nutrition line that stays off whenever a
  nutrition mod is installed
* `accessibility` — `reducedMotion`, `animationSpeed`, `alwaysShowRankText`,
  `highContrast`

The in-game settings screen is reachable from the journal.

---

## Presets

`Vanilla+`, `Cozy`, `Completionist`, `Server` and `Cosmetic` exist as starting
points. **They are never applied automatically** — a fresh install always writes
plain defaults — and applying one only rewrites configuration; it cannot touch a
player profile.
