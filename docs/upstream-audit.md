# Upstream Audit — Farmer's Delight

Everything below was read out of the shipped jars, not from memory. Re-run the
audit (and update this file) before retargeting another Minecraft version.

| | |
|---|---|
| Minecraft | `1.21.1` |
| Java | `21` |
| NeoForge build audited | `FarmersDelight-1.21.1-1.3.2.jar` (mod id `farmersdelight`, NeoForge `[4,)`) |
| Fabric build audited | `FarmersDelight-1.21.1-3.3.3+refabricated.jar` (mod id `farmersdelight`, Fabric API required) |
| Audited on | 2026-07-26 |

**There is no Forge build of Farmer's Delight for 1.21.1** — only NeoForge and
Fabric — which is why this branch replaces the `forge` module with `neoforge`.

Both distributions share the mod id `farmersdelight`, the same package root
(`vectorwing.farmersdelight`), the same recipe type ids, the same item ids and
the same tag ids. **The version numbers differ wildly (1.3.x vs 3.3.x) — never
gate behaviour on the Farmer's Delight version string.**

---

## 1. Recipe types

Counted across every recipe json shipped by each jar.

| Recipe type | NeoForge 1.3.2 | Fabric 3.3.3 | Meal Mastery treatment |
|---|---:|---:|---|
| `farmersdelight:cooking` | 28 | 28 | Cooking Pot method |
| `farmersdelight:cutting` | 106 | 106 | Cutting Board method |
| `minecraft:crafting_shaped` | 54 | 54 | Crafting method |
| `minecraft:crafting_shapeless` | 86 | 86 | Crafting method |
| `minecraft:smelting` | 10 | 10 | Smelting method |
| `minecraft:smoking` | 9 | 9 | Smoking method |
| `minecraft:campfire_cooking` | 7 | 7 | Campfire method |
| `minecraft:blasting` | 2 | 2 | Not culinary |

The two builds now agree exactly on recipe counts, unlike 1.20.1 where the
Fabric port split cutting recipes to avoid tool actions.

Loader-only extras that must not be assumed present:

* `farmersdelight:item_ability` (NeoForge, 105 uses) — an **ingredient**
  serializer used inside `cutting` recipes' `tool` field, not a recipe type.
  Renamed from `tool_action` in 1.20.1.
* `neoforge:mod_loaded`, `neoforge:compound`, `neoforge:difference`,
  `neoforge:tag`, `farmersdelight:vanilla_crates_enabled` — NeoForge-only
  recipe conditions and ingredient types. Conditions are resolved before
  recipes reach the registry, so Meal Mastery never sees them.
* Cross-mod recipes shipped for optional integrations (`create:milling`,
  `create:mixing`, `immersiveengineering:*`) appear in both jars. They only
  load when those mods are present.

### Consequence for Meal Mastery

Recipe types are resolved by `ResourceLocation` out of `BuiltInRegistries.RECIPE_TYPE`
at recipe-reload time. `RecipeHolder#id`, `Recipe#getType`, `#getIngredients`
and `#getResultItem` are all vanilla, so a
Farmer's Delight cooking recipe and an addon's cooking recipe are read through
exactly the same code path. **Meal Mastery has no compile-time dependency on
Farmer's Delight at all** — see [architecture.md](architecture.md).

## 2. Sample recipe shapes

`farmersdelight:cooking` (`data/farmersdelight/recipe/cooking/baked_cod_stew.json`):

```json
{
  "type": "farmersdelight:cooking",
  "experience": 1.0,
  "ingredients": [
    { "tag": "c:foods/raw_cod" },
    { "tag": "c:crops/potato" },
    { "tag": "c:eggs" },
    { "tag": "c:crops/tomato" }
  ],
  "recipe_book_tab": "meals",
  "result": { "count": 1, "id": "farmersdelight:baked_cod_stew" }
}
```

Note the 1.21 shape changes: the recipe directory is `recipe`, not `recipes`,
and a result is `{ "id": ..., "count": ... }` rather than `{ "item": ... }`.
Neither matters to Meal Mastery, which reads recipes through the registry.

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
  "tool": [ { "type": "farmersdelight:item_ability", "action": "axe_strip" },
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

**Ingredient-classification tags — now identical on both loaders**

The `forge:` namespace is gone. Both jars populate the same `c:` conventional
tags:

`c:foods/vegetable`, `c:foods/fruit`, `c:foods/raw_meat`, `c:foods/cooked_meat`,
`c:foods/raw_fish`, `c:foods/cooked_fish`, `c:foods/bread`, `c:foods/dough`,
`c:foods/pasta`, `c:foods/pie`, `c:foods/soup`, `c:foods/cooked_egg`,
`c:crops`, `c:crops/grain`, `c:seeds`, `c:eggs`, `c:drinks/milk`, …

This is a genuine simplification over the 1.20.1 branch, which had to list a
Forge and a Fabric spelling for every category. A missing tag is still treated
as "uncategorised" rather than guessed at.

**Tag directory rename.** 1.21 renamed `tags/items` to `tags/item` and
`tags/blocks` to `tags/block` on disk. Tag *ids* are unchanged, so nothing in
Meal Mastery had to move.

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

Eating is the only surface with no shared hook: NeoForge has
`LivingEntityUseItemEvent.Finish`, Fabric has nothing, so Fabric carries one
narrow read-only mixin on `LivingEntity#eat(Level, ItemStack, FoodProperties)` —
the food-properties parameter is new in 1.21, where food moved into an item
component.

## 6. Things deliberately *not* assumed

* No Farmer's Delight class, field or method is referenced anywhere in the mod.
* No Farmer's Delight version check — the two distributions disagree by a whole
  major version.
* No `farmersdelight:item_ability` handling — NeoForge-only.
* No assumption that a cutting recipe produces food, or that a cooking recipe's
  single result is the only output.
* No assumption that `farmersdelight:nourishment` exists.

## 7. Reproducing this audit

```bash
curl -L -o fd-neoforge.jar https://cdn.modrinth.com/data/R2OftAxM/versions/GbNuOZ4S/FarmersDelight-1.21.1-1.3.2.jar
curl -L -o fd-fabric.jar 'https://cdn.modrinth.com/data/7vxePowz/versions/NCLOIK5z/FarmersDelight-1.21.1-3.3.3%2Brefabricated.jar'
mkdir -p ex && (cd ex && unzip -oq ../fd-neoforge.jar)
grep -rhoE '"type"[[:space:]]*:[[:space:]]*"[^"]+"' ex/data/farmersdelight/recipe/ | sort | uniq -c | sort -rn
ls ex/data/farmersdelight/tags/item ex/data/farmersdelight/tags/block ex/data/c/tags/item
```
