# Farmer's Delight: Meal Mastery

> Turns every meal you cook into persistent culinary progression.

Meal Mastery adds cooking progression, recipe mastery, discovery, statistics,
challenges and a culinary journal to [Farmer's Delight](https://modrinth.com/mod/farmers-delight)
— without adding a single food, block, item, model, texture or sound.

| | |
|---|---|
| Minecraft | 1.21.1 |
| Loaders | Fabric, NeoForge |
| Requires | Farmer's Delight |
| Custom art assets | **zero** |

Minecraft 1.20.1 (Fabric, Forge) lives on the
[`1.20.1`](https://github.com/XircTwerk/Meal-Mastery/tree/1.20.1) branch.
Farmer's Delight has no Forge build for 1.21.1, which is why the loaders differ.

## What it does

1. **Master every meal.** Every eligible dish has its own mastery track, from
   *Unfamiliar* to *Mastered*, driven by how often you actually prepare it.
2. **Build your culinary journal.** Recipes, ingredients, cooking methods,
   categories and per-mod collections, all discovered by cooking rather than by
   opening a recipe viewer.
3. **Level up by cooking.** Cooking XP rewards experimentation, discovery and
   variety far more than repetition — cooking one cheap dish 10,000 times is
   deliberately a bad strategy.
4. **Complete cooking challenges.** Data-driven challenges generated from the
   food that is actually installed, so they are never impossible.
5. **Automatic addon support.** Install another Farmer's Delight food expansion
   and the journal grows on the next launch. No compatibility pack, no
   hardcoded lists.

## Why it works with addons you have never heard of

Meal Mastery has **no compile-time dependency on Farmer's Delight**. It reads
recipe types, recipes, ingredients, food properties and tags out of vanilla
registries, so a Nether's Delight cooking recipe and a Farmer's Delight cooking
recipe travel the exact same code path.

See [`docs/architecture.md`](docs/architecture.md).

## Mastery actually does something

Cooking a dish you have mastered is better than cooking one you have not, and
this is **on by default**:

* **Cook faster.** Farmer's Delight's pot, skillet and stove, plus the vanilla
  smoker, run quicker the more practised you are — up to twice as fast.
* **Better food.** Eating a dish you have mastered adds saturation and can roll
  a beneficial effect, scaled by your rank with that specific dish.
* **Stars.** Food carries a rating from ★ to ★★★★★, shown under its name.
* **Perfect dishes.** A practised cook occasionally plates something perfect —
  worth an extra star and a stronger effect.
* **Extra portions.** Sometimes the same ingredients stretch further.
* **Cooking for other people counts.** A cooked stack remembers who made it, so
  a chef's food is better for whoever eats it, and the chef earns mastery when
  they do.
* **Signature dish.** Middle-click a mastered recipe to make it your signature;
  everything you cook of it plates a star higher.

None of it touches a recipe or a food's own stats, so Farmer's Delight still
works exactly as designed for anyone who has cooked nothing. Every magnitude is
configurable under `mastery.bonuses`, and a server that wants the old
statistics-only behaviour sets `mastery.bonuses.enabled` to `false`.

## Building

```bash
./gradlew build
```

Artifacts land in `fabric/build/libs` and `neoforge/build/libs`.

Development launches:

```bash
./gradlew :fabric:runClient
./gradlew :neoforge:runClient
```

Farmer's Delight is pulled in as a **runtime-only** dependency for those
launches; it is never on the compile classpath.

## Commands

`/mealmastery`, aliased `/mealmasters` and `/mm`. The journal also has a
keybind (J by default) and needs no item.

| | |
|---|---|
| `open`, `stats`, `recipe <item>` | your own profile |
| `challenges`, `collections`, `titles`, `today` | goals |
| `leaderboard`, `compat`, `export` | server and diagnostics |
| `audit`, `debug`, `reload` | admin: what the registry detected, and why |
| `admin inspect / addxp / setlevel / discover / mastery / reset / purgeorphans` | admin |

`/mealmastery debug` reports what Meal Mastery thinks about the item in your
hand — the fastest way to find out why an addon's food is or is not in the
journal.

## Documentation

* [`docs/upstream-audit.md`](docs/upstream-audit.md) — what was actually read
  out of the Farmer's Delight jars
* [`docs/compatibility-audit.md`](docs/compatibility-audit.md) — overlap survey
  and coexistence rules
* [`docs/architecture.md`](docs/architecture.md) — module layout and the
  attribution model
* [`docs/configuration.md`](docs/configuration.md) — every server and client
  setting
* [`docs/datapacks.md`](docs/datapacks.md) — challenges, collections and
  milestones
* [`docs/api.md`](docs/api.md) — the small API other mods may use

## Licence

[MIT](LICENSE).

Meal Mastery is an independent addon. It is not affiliated with, endorsed by or
supported by the authors of Farmer's Delight.
