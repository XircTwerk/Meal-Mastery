# Upstream Audit — Farmer's Delight

Everything below was read out of the shipped jars, not from memory. Re-run the
audit (and update this file) before retargeting another Minecraft version.

| | |
|---|---|
| Minecraft | `1.20.1` |
| Java | `17` |
| Forge build audited | `FarmersDelight-1.20.1-1.3.2.jar` (mod id `farmersdelight`, Forge `[47.1.0,)`) |
| Fabric build audited | `FarmersDelight-1.20.1-2.4.1+refabricated.jar` (mod id `farmersdelight`, Fabric API required) |
| Audited on | 2026-07-26 |

Both distributions share the mod id `farmersdelight`, the same package root
(`vectorwing.farmersdelight`), the same recipe type ids, the same item ids and
the same tag ids. **The version numbers differ wildly (1.3.x vs 2.4.x) — never
gate behaviour on the Farmer's Delight version string.**

---

## 1. Recipe types

Counted across every recipe json shipped by each jar.

| Recipe type | Forge 1.3.2 | Fabric 2.4.1 | Meal Mastery treatment |
|---|---:|---:|---|
| `farmersdelight:cooking` | 28 | 27 | Cooking Pot method |
| `farmersdelight:cutting` | 106 | 126 | Cutting Board method |
| `farmersdelight:food_serving` | 1 | 1 | Feast serving (dynamic) |
| `farmersdelight:dough` | 1 | 1 | Crafting (special) |
| `minecraft:crafting_shaped` | 54 | 49 | Crafting method |
| `minecraft:crafting_shapeless` | 86 | 85 | Crafting method |
| `minecraft:smelting` | 10 | 10 | Smelting method |
| `minecraft:smoking` | 9 | 9 | Smoking method |
| `minecraft:campfire_cooking` | 7 | 7 | Campfire method |
| `minecraft:blasting` | 2 | 2 | Not culinary |
| `minecraft:smithing_transform` | 1 | 1 | Not culinary |

Loader-only extras that must not be assumed present:

* `farmersdelight:tool_action` (Forge, 106 uses) — an **ingredient** serializer
  used inside `cutting` recipes' `tool` field, not a recipe type. The Fabric
  port drops it entirely, which is why its `cutting` count is higher (it splits
  recipes instead of using tool actions).
* `forge:conditional`, `forge:mod_loaded`, `farmersdelight:vanilla_crates_enabled` —
  Forge-only recipe conditions. They are resolved before recipes reach the
  registry, so Meal Mastery never sees them.
* Cross-mod recipes shipped for optional integrations (`create:milling`,
  `create:mixing`, `create:filling`, `immersiveengineering:*`) appear in both
  jars. They only load when those mods are present.

### Consequence for Meal Mastery

Recipe types are resolved by `ResourceLocation` out of `BuiltInRegistries.RECIPE_TYPE`
at recipe-reload time. `Recipe#getType`, `#getId`, `#getIngredients` and
`#getResultItem(RegistryAccess)` are all vanilla interface methods, so a
Farmer's Delight cooking recipe and an addon's cooking recipe are read through
exactly the same code path. **Meal Mastery has no compile-time dependency on
Farmer's Delight at all** — see [architecture.md](architecture.md).

## 2. Sample recipe shapes

`farmersdelight:cooking` (`data/farmersdelight/recipes/cooking/baked_cod_stew.json`):

```json
{
  "type": "farmersdelight:cooking",
  "cookingtime": 200,
  "experience": 1.0,
  "ingredients": [
    { "tag": "forge:raw_fishes/cod" },
    { "tag": "forge:crops/potato" },
    { "tag": "forge:eggs" },
    { "tag": "forge:crops/tomato" }
  ],
  "recipe_book_tab": "meals",
  "result": { "item": "farmersdelight:baked_cod_stew" }
}
```

Notable: ingredients are frequently **tags**, which is exactly what the
ingredient-variant tracking in the design needs. `container` is an
optional extra field (bowl/bottle) that we ignore as an ingredient.

`farmersdelight:cutting` (`.../cutting/acacia_log.json`):

```json
{
  "type": "farmersdelight:cutting",
  "ingredients": [{ "item": "minecraft:acacia_log" }],
  "result": [
    { "item": "minecraft:stripped_acacia_log" },
    { "item": "farmersdelight:tree_bark" }
  ],
  "tool": [ { "type": "farmersdelight:tool_action", "action": "axe_strip" },
            { "tag": "minecraft:axes" } ],
  "sound": "minecraft:item.axe.strip"
}
```

Notable: `result` is a **list** and is not exposed through
`Recipe#getResultItem` in a useful way for cutting recipes. Most cutting recipes
are not culinary at all (stripping logs, cutting flowers into dye). Eligibility
therefore has to be driven by "does an output have a `FoodProperties`", not by
"is it a cutting recipe".

## 3. Tags

Farmer's Delight ships tags that map almost perfectly onto the journal's
category system, so no hardcoded item lists are needed.

**Item tags under `farmersdelight:`**

`meals`, `feasts`, `snacks`, `sweets`, `drinks`, `pies`, `serving_containers`,
`flat_on_cutting_board`, `wild_crops`, `mushroom_colonies`, `straw_harvesters`,
`cabinets`, `canvas_signs`, `hanging_canvas_signs`, `tools/*`

`farmersdelight:meals` contains 30 entries including three vanilla soups, which
makes it a good default for "is this a prepared dish".

**Block tags under `farmersdelight:`**

`feasts`, `heat_sources`, `heat_conductors`, `tray_heat_sources`, `pies`,
`cabinets`, `straw_blocks`, `mushroom_colonies`, `wild_crops`, `ropes`,
`compost_activators`, `drops_cake_slice`, `terrain`, `mineable/knife`, …

`farmersdelight:feasts` lists the six feast blocks
(`roast_chicken_block`, `stuffed_pumpkin_block`, `shepherds_pie_block`,
`honey_glazed_ham_block`, `gleaming_salad_block`, `rice_roll_medley_block`).

**Ingredient-classification tags — the loader difference that matters**

| Forge jar | Fabric jar |
|---|---|
| `forge:vegetables`, `forge:crops`, `forge:raw_meat`, `forge:raw_fishes/*`, `forge:cooked_*`, `forge:eggs`, `forge:milk`, `forge:grain`, `forge:dough`, `forge:pasta`, `forge:bread`, `forge:berries`, `forge:seeds`, `forge:salad_ingredients` | the same `forge:` tags **plus** `c:` conventional equivalents (`c:foods`, `c:grains`, `c:milks`, `c:vegetables/*`, `c:crops/*`, `c:salad_ingredients`, `c:dough`, `c:seeds`, …) |

Meal Mastery's ingredient categoriser therefore consults **both** namespaces and
treats a missing tag as "uncategorised" rather than guessing.

## 4. Effects

| Id | Class | Notes |
|---|---|---|
| `farmersdelight:nourishment` | `vectorwing.farmersdelight.common.effect.NourishmentEffect` | Present in both jars. Resolved by `ResourceLocation` for the "nourishing meals prepared" statistic; absent = statistic hidden, never crashes. |
| `farmersdelight:comfort` | `vectorwing.farmersdelight.common.effect.ComfortEffect` | Present in both jars. |

## 5. Blocks, block entities and how food actually reaches a player

Only the *behaviour* matters here; none of these classes are referenced from
Meal Mastery code.

| Workstation | Block entity | How the player obtains output |
|---|---|---|
| Cooking Pot | `CookingPotBlockEntity` (+ `CookingPotMenu`, `CookingPotResultSlot`, `CookingPotMealSlot`) | Either taking the meal out of the **menu**, or right-clicking the pot **holding a serving container** (bowl/bottle) |
| Cutting Board | `CuttingBoardBlockEntity` | Right-click with a knife/tool; results **spawn as `ItemEntity`s** next to the board |
| Skillet | `SkilletBlockEntity` / `SkilletItem` | Right-click the skillet to take the cooked item straight into the inventory |
| Stove | `StoveBlockEntity` extends `AbstractStoveBlockEntity` | Cooks items held in hand into the world; a heat source for pots/skillets |
| Feasts | `FeastBlock` (`farmersdelight:feasts` block tag) | Right-click a placed feast with a serving container to take a portion |
| Basket / Cabinet | `BasketBlockEntity`, `CabinetBlockEntity` | Plain storage — must **never** be treated as a cooking workstation |

This produces exactly three attribution surfaces, all reachable with vanilla
hooks:

1. **Menu extraction** — poll `ServerPlayer#containerMenu` while a culinary
   menu is open and diff the player's culinary items. Covers the Cooking Pot,
   crafting tables, furnaces/smokers and any modded workstation menu.
2. **Block interaction** — Forge `PlayerInteractEvent.RightClickBlock` /
   Fabric `UseBlockCallback` opens a short attribution window. Covers the
   skillet, pot-with-bowl serving and feast portions.
3. **Item entity spawn** — Forge `EntityJoinLevelEvent` / Fabric
   `ServerEntityEvents.ENTITY_LOAD` inside an open attribution window. Covers
   the cutting board.

Eating is the only surface with no shared hook: Forge has
`LivingEntityUseItemEvent.Finish`, Fabric has nothing, so Fabric carries one
narrow read-only mixin on `LivingEntity#eat`.

## 6. Things deliberately *not* assumed

* No Farmer's Delight class, field or method is referenced anywhere in the mod.
* No Farmer's Delight version check — the two distributions disagree by a whole
  major version.
* No `farmersdelight:tool_action` handling — Forge-only.
* No assumption that a cutting recipe produces food, or that a cooking recipe's
  single result is the only output.
* No assumption that `farmersdelight:nourishment` exists.

## 7. Reproducing this audit

```bash
curl -L -o fd-forge.jar https://cdn.modrinth.com/data/R2OftAxM/versions/CsjS7EkP/FarmersDelight-1.20.1-1.3.2.jar
curl -L -o fd-fabric.jar 'https://cdn.modrinth.com/data/7vxePowz/versions/Z8UNayLO/FarmersDelight-1.20.1-2.4.1%2Brefabricated.jar'
mkdir -p ex && (cd ex && unzip -oq ../fd-forge.jar)
grep -rhoE '"type"[[:space:]]*:[[:space:]]*"[^"]+"' ex/data/farmersdelight/recipes/ | sort | uniq -c | sort -rn
ls ex/data/farmersdelight/tags/items ex/data/farmersdelight/tags/blocks
```
