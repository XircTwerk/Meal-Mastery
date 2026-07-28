# Farmer's Delight: Meal Mastery

Meal Mastery is an addon for [Farmer's Delight](https://www.curseforge.com/minecraft/mc-mods/farmers-delight)
that turns cooking into progression. Every dish you prepare has its own mastery
track, the food you cook is recorded in a culinary journal, and getting good at
a recipe measurably changes what happens when you cook and eat it.

**It is not limited to Farmer's Delight's own food.** Dishes are detected
generically from recipes, so every Farmer's Delight addon — Nether's Delight,
End's Delight, Expanded Delight, More Delight and any addon released after this
one — is picked up automatically. No compatibility pack, no hardcoded item list,
no per-mod code. Vanilla recipes count too.

| | |
|---|---|
| Requires | Farmer's Delight, Fabric API (Fabric builds only) |
| Works with | any food addon that registers ordinary recipes, plus vanilla |
| Minecraft | 1.20.1 (Fabric, Forge) · 1.21.1 (Fabric, NeoForge) |
| Sides | Client and server — both are required |
| New content added | none |
| License | MIT |

---

## Mastery

Each recipe has an independent mastery track running from *Unfamiliar* to
*Mastered*, advanced by preparing that specific dish. By default the top rank
takes 300 preparations of one dish (thresholds `[2, 20, 60, 150, 300]`).

Mastery has mechanical effects, and they are **on by default**. Every value
below is the default and every one is configurable:

- **Cooking speed** — the cooking pot, skillet and stove, plus the vanilla
  smoker, run up to twice as fast at full mastery.
- **Star rating** — cooked food is stamped with ★ to ★★★★★ shown under its name.
- **Bonus effects** — a 25% chance, at full mastery, that eating a dish grants
  20 seconds of regeneration, speed or haste.
- **Saturation** — +0.2 saturation per star. Hunger values are never touched.
- **Perfect dishes** — an 8% chance at full mastery of plating something
  perfect: an extra star and a stronger effect.
- **Extra portions** — a 12% chance the same ingredients stretch further.
- **Knife durability** — a 25% chance a cutting-board preparation refunds a
  point of durability.
- **Signature dish** — middle-click a mastered recipe to make it your signature;
  everything you cook of it plates one star higher.

All chances scale linearly from zero at *Unfamiliar*, so none of it applies to a
first attempt. The real odds for a dish are printed on its tooltip behind the
modifier key, so a retuned modpack states its own numbers instead of leaving
players to guess.

Cooked food carries a stamp recording the cook's stars, whether the attempt was
perfect, and who made it — which is why a chef's food is better for whoever eats
it, and why the chef earns mastery when someone else eats it. This is the only
place the mod writes to an item. An unstamped stack is byte-identical to vanilla
food, and stamped stacks refuse to merge with unstamped ones so a good batch is
not diluted by a bad one.

A server that wants statistics-only behaviour sets `mastery.bonuses.enabled` to
`false` and every mechanical effect above disappears.

## The journal

Opened with a keybind (J by default) or `/mealmastery open`. No item required.

It tracks recipes, ingredients, cooking methods, categories and collections —
all discovered by cooking, not by opening a recipe viewer. Per-mod collections
are generated automatically for every mod that contributes at least one dish.
Unknown entries can be hidden, silhouetted, shown by name, shown with ingredient
hints or shown in full, per client.

## Cooking level and XP

Cooking XP deliberately rewards breadth over repetition. Discovering a new dish
is worth 75 XP; repeating one you already know is worth 6. On top of that,
anti-farming diminishing returns apply per dish per day — the first preparation
counts fully, the next four at 75%, the next ten at 50%, everything after at a
20% floor. The floor is not zero: repetition should be worth less, not
worthless.

Cooking one cheap dish ten thousand times is intentionally a bad strategy.

## Challenges, collections and milestones

Daily challenges are generated from the food that is actually installed, so an
objective is never impossible to complete. Modpack and datapack authors can add
their own:

```
data/<namespace>/mealmastery/challenges/<name>.json
data/<namespace>/mealmastery/collections/<name>.json
data/<namespace>/mealmastery/milestones/<name>.json
```

Fourteen objective types are available (cook a recipe, cook from a category or a
specific mod, use an ingredient or ingredient tag, use a cooking method, master
a recipe, serve portions, reach a level, and so on). Rewards are limited to
things that already exist — items, advancements, XP and cosmetic journal badges.
No custom reward items are invented. Command rewards exist but are gated behind
a server setting that is off by default.

Everything the mod ships is written as datapack files itself, so a pack can
override any of it by reusing the same id.

## It works with food addons it has never heard of

Meal Mastery has **no compile-time dependency on Farmer's Delight.** It reads
recipe types, recipes, ingredients, food properties and tags out of vanilla
registries, so a Nether's Delight cooking recipe and a Farmer's Delight cooking
recipe travel the exact same code path. Nothing is keyed to a mod id.

The practical consequence: install another food addon and the journal grows on
the next launch. There is no compatibility pack, no hardcoded item list, and no
per-addon code — including for any addon released after this mod. This applies
to food from mods that are not Farmer's Delight addons at all, as long as they
register ordinary recipes producing ordinary food.

Farmer's Delight itself is still required to install and run Meal Mastery.

**Vanilla recipes count too, by default.** Bread, a baked potato and anything
out of a smoker earn mastery on the same terms as a Farmer's Delight stew, and
the journal lists them. A pack that wants the journal to be about its food
addons only sets `compatibility.trackVanillaRecipes` to `false`, which turns off
the whole `minecraft` namespace in one go and leaves every other mod untouched.

`/mealmastery debug` reports what the mod thinks about the item in your hand,
which is the fastest way to find out why a given addon's food is or is not in
the journal. `/mealmastery compat` lists every mod detected as contributing
dishes.

## Multiplayer

Cooking credit only goes to a real, identifiable player. Food produced by
hoppers, autocrafters or mod automation earns **nobody** anything by default;
ownership is never guessed. A server can relax this with the `automation.credit`
setting.

Leaderboards are off by default, and opt-in per player on top of that — a player
opts in by giving themselves the `mealmastery_leaderboard` scoreboard tag.

Progression is server-authoritative. A client cannot change it; the server
pushes the curves it uses, so a mismatched client cannot even mis-render a
progress bar.

## Configuration

Two commented TOML files in the config directory:

```
config/mealmastery-server.toml    world behaviour, server-authoritative
config/mealmastery-client.toml    presentation only
```

Every setting is commented in the file itself, including its range and accepted
values. Both reload with `/mealmastery reload`. Out-of-range values are clamped
and logged rather than rejected — a config typo must never cost anyone their
world — and a file that cannot be parsed at all is renamed to `*.invalid` and
replaced with defaults so the original stays recoverable. Configs from an
earlier JSON build are migrated automatically, with the original kept as
`*.json.bak`.

Five presets (`Vanilla+`, `Cozy`, `Completionist`, `Server`, `Cosmetic`) are
available as starting points. None is ever applied automatically, and applying
one only rewrites configuration; it cannot touch a player profile.

## Commands

`/mealmastery`, aliased `/mealmasters` and `/mm`.

| | |
|---|---|
| `open`, `stats`, `recipe <item>` | your own profile |
| `challenges`, `collections`, `titles`, `today` | goals |
| `leaderboard`, `compat`, `export` | server and diagnostics |
| `audit`, `debug`, `reload` | what the registry detected, and why |
| `admin inspect / addxp / setlevel / discover / mastery / reset / purgeorphans` | admin |

## For mod developers

There is a small public API (`MealMasteryApi`) for the two cases generic
detection cannot reach: registering a workstation that neither opens a menu nor
drops its output, and awarding cooking credit directly. Most mods need none of
it — a food addon registering ordinary recipes is already fully supported with
zero integration code.

Meal Mastery is not a kitchen framework and will not become a mandatory
dependency. There is no way to register a food, a recipe, a workstation menu or
a cooking mechanic through the API; those belong to Farmer's Delight and to the
addons themselves.

## What it does not do

- It does not add any food, item, block, in-game texture, model or sound.
- It does not modify Farmer's Delight recipes or the stats of any food item.
- It does not gate anything. No recipe, item or block becomes unavailable
  because of a Meal Mastery level.
- It does not change hunger values, and it does not add a nutrition or
  food-group system.
- It does not replace a recipe viewer. JEI/REI/EMI integration is optional and
  additive.
- It does not hook into other progression mods. Its Cooking Level has no
  relationship to a Project MMO skill level or a LevelZ level, and it neither
  emits nor consumes their XP.
- It does not claim a tooltip section another mod owns. The nutrition line stays
  off by default whenever a nutrition mod is installed.

## Source and issues

<https://github.com/XircTwerk/Meal-Mastery>

Branch `1.20.1` targets Minecraft 1.20.1 (Fabric, Forge). Branch `1.21.1`
targets Minecraft 1.21.1 (Fabric, NeoForge) — Farmer's Delight has no Forge
build for 1.21.1, which is why the loaders differ.

Licensed MIT. Modpacks are free to include it; a credit link is appreciated but
not required.

Meal Mastery is an independent addon. It is not affiliated with, endorsed by or
supported by the authors of Farmer's Delight.
