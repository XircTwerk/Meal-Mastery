# Datapack Reference

Meal Mastery reads three kinds of file out of any datapack. All of them are
validated on load; a malformed file is named in the log and skipped so the rest
of the pack still works.

```
data/<namespace>/mealmastery/challenges/<name>.json
data/<namespace>/mealmastery/collections/<name>.json
data/<namespace>/mealmastery/milestones/<name>.json
```

The file path becomes the id: `data/mypack/mealmastery/challenges/soup_season.json`
is `mypack:soup_season`.

Everything Meal Mastery ships is written this way too — see
`common/src/main/resources/data/mealmastery/mealmastery/` — so a pack can
override any of it by using the same id.

---

## Challenges

```json
{
  "scope": "PERMANENT",
  "hidden": false,
  "prerequisite": "mypack:first_steps",
  "objectives": [
    { "type": "COOK_CATEGORY", "value": "mealmastery:meal", "amount": 5 },
    { "type": "COOK_RECIPE", "amount": 3, "unique": true }
  ],
  "rewards": {
    "cooking_xp": 120,
    "mastery_xp": 0,
    "vanilla_xp": 20,
    "advancement": "mypack:soup_season",
    "badge": "mypack:soup_specialist",
    "items": ["minecraft:bread 3"],
    "commands": ["say @s finished Soup Season"]
  }
}
```

| Field | Meaning |
|---|---|
| `scope` | `PERMANENT` (default), `DAILY`, `WEEKLY` or `CHAIN` |
| `hidden` | keeps it out of the journal until completed |
| `prerequisite` | another challenge id that must be complete first |
| `objectives` | **all** must be satisfied; at least one is required |
| `rewards` | optional |

### Objective types

`value` is interpreted per type. `amount` defaults to `1`. `unique: true` counts
distinct subjects instead of repeats, so "prepare 10 unique recipes" cannot be
satisfied by cooking one dish ten times.

| Type | `value` | Notes |
|---|---|---|
| `COOK_RECIPE` | dish item id, or blank for any | |
| `COOK_CATEGORY` | category id, e.g. `mealmastery:meal` | |
| `COOK_FROM_MOD` | a plain mod id, not a resource location | |
| `USE_INGREDIENT` | ingredient item id | fires on ingredient discovery |
| `USE_INGREDIENT_TAG` | item tag id | |
| `USE_METHOD` | cooking method id, e.g. `farmersdelight:cutting` | |
| `DISCOVER_RECIPE` | — | |
| `MASTER_RECIPE` | — | |
| `EAT_RECIPE` | dish item id, or blank for any | |
| `SERVE_PORTIONS` | — | feast portions dispensed, and stamped food of yours eaten by someone else |
| `PREPARE_VARIETY` | — | `amount` = consecutive distinct dishes |
| `UNIQUE_RECIPES_IN_ONE_DAY` | — | `amount` = distinct dishes in one day |
| `REACH_LEVEL` | — | `amount` = the Cooking Level |
| `COMPLETE_CHALLENGE` | another challenge id | |

Types that need a `value` are rejected without one, and ids are parsed at load
so a typo is reported rather than silently never matching.

### Rewards

No custom reward items exist and none are invented. A pack may hand out items
that already exist, an advancement, XP, or a cosmetic journal badge.

`commands` only run when the server sets `challenges.allowCommandRewards` to
`true`; otherwise they are skipped with a log line. `@s` is replaced with the
player's name.

---

## Collections

```json
{
  "items": ["farmersdelight:beef_stew"],
  "tags": ["farmersdelight:meals"],
  "categories": ["mealmastery:meal"],
  "mods": ["netherdelight"],
  "icon": "minecraft:mushroom_stew"
}
```

At least one of `items`, `tags`, `categories` or `mods` is required. Membership
resolves against what is actually installed, so a collection mentioning a dish
from an absent mod is simply smaller — never impossible.

`icon` must be an existing item; nothing new is registered. If it is omitted the
first listed item is used.

**Per-mod collections are generated automatically** for every mod contributing
at least one dish, so there is no need to author one for each food addon.

---

## Milestones

```json
{
  "type": "MEALS_PREPARED",
  "threshold": 100,
  "badge": "mypack:centurion"
}
```

| `type` | Counts |
|---|---|
| `MEALS_PREPARED` | lifetime preparations |
| `UNIQUE_RECIPES_PREPARED` | distinct dishes ever prepared |
| `RECIPES_DISCOVERED` | distinct dishes discovered |
| `RECIPES_MASTERED` | dishes at the top mastery rank |
| `INGREDIENTS_DISCOVERED` | distinct ingredients cooked with |
| `COOKING_LEVEL` | Cooking Level reached |
| `PORTIONS_SERVED` | feast portions dispensed |

Defining any milestone replaces the built-in set entirely, so include the ones
you want to keep.

---

## Translation keys

| Thing | Key |
|---|---|
| Challenge title | `mealmastery.challenge.<namespace>.<path>.title` |
| Challenge description | `mealmastery.challenge.<namespace>.<path>.description` |
| Collection | `mealmastery.collection.<namespace>.<path>` |
| Milestone | `mealmastery.milestone.<namespace>.<path>` |
| Badge | `mealmastery.badge.<namespace>.<path>` |

Dish names are never duplicated — the journal uses the item's own translation
key, so an addon's food is already localised in whatever languages that addon
ships.

## Categories and eligibility

Categories are derived from item tags, so tagging a dish is usually all an
addon has to do. Which recipes become journal entries is server configuration
rather than datapack data — see [configuration.md](configuration.md) for the
allow and deny lists.
