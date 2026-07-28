# Architecture

## The one decision everything else follows from

**Meal Mastery does not compile against Farmer's Delight.**

Farmer's Delight ships as two distributions with incompatible version numbers
(NeoForge `1.3.2`, Fabric `3.3.3+refabricated`) and different internals, and the
whole selling point of the mod is that *any* sensible food addon works with no
integration code. So the mod talks exclusively to vanilla:

| Needs | Obtained from |
|---|---|
| Recipe types | `BuiltInRegistries.RECIPE_TYPE` by `ResourceLocation` |
| Recipes | `RecipeManager` + `RecipeHolder<?>` and the `Recipe<?>` interface |
| Ingredients | `Recipe#getIngredients` → `Ingredient#getItems` |
| Outputs | `Recipe#getResultItem(RegistryAccess)` |
| Food data | the vanilla `DataComponents.FOOD` component |
| Categories | item/block `TagKey`s under `farmersdelight:` and `c:` |
| Source mod | the namespace of the recipe/item `ResourceLocation` |
| Effects | `BuiltInRegistries.MOB_EFFECT` by `ResourceLocation` |

A Farmer's Delight cooking recipe and a Nether's Delight cooking recipe are
literally the same code path. Nothing is keyed on a mod id.

## Module layout

```
common/     loader-neutral: everything except loader hooks and packet transport
fabric/     Fabric entry points, Fabric API callbacks, one narrow mixin
neoforge/   NeoForge entry points, NeoForge event-bus subscribers
```

Farmer's Delight has no Forge build for 1.21.1, so this branch targets NeoForge
and Fabric.

`fabric` and `neoforge` compile `common`'s sources directly into their own jars
(`compileJava { source(project(':common')...) }`), which is why `common` may
only depend on vanilla Minecraft.

Loader differences are reached through a `ServiceLoader`-backed
`Services.PLATFORM` (`IPlatformHelper`), kept deliberately tiny.

## Package map (`common`)

```
com.xirc.mealmastery
├── culinary/     CulinaryProfile, CulinaryStats, CookingLevel
├── recipe/       CulinaryRegistry, RecipeClassifier, RecipeEntry, IngredientResolver
├── mastery/      MasteryManager, MasteryEntry, MasteryCurve
├── discovery/    DiscoveryManager
├── challenge/    ChallengeManager, ChallengeDefinition, ChallengeObjective
├── tracking/     CookingTracker, EatingTracker, ServingTracker, attribution
├── event/        internal event bus + CulinaryHooks (loader fan-in)
├── config/       server + client config, presets, validation
├── network/      packet definitions (transport is loader-specific)
├── command/      /mealmastery and admin subcommands
├── data/         profile storage, schema migration, orphan handling
├── compat/       optional integrations, capability reporting
├── client/       journal UI, HUD, tooltips, config screen
├── api/          the small public surface other addons may use
└── platform/     IPlatformHelper + Services
```

## Attribution model

Cooking credit must go to a real, identifiable player or to nobody at all
. Three vanilla-only surfaces cover every Farmer's Delight
workstation and, by construction, every addon workstation too:

1. **Menu extraction** — while `ServerPlayer#containerMenu` is a *culinary*
   menu, diff the player's culinary item counts. Storage menus (chests,
   barrels, cabinets, baskets) are explicitly excluded, so moving cooked food
   around a chest never counts as cooking.
2. **Block interaction window** — a right-click on a recognised workstation
   opens a short attribution window; culinary items gained during it are
   credited to that player. Covers the skillet, pot-with-bowl serving and feast
   portions.
3. **Item entity spawn inside a window** — covers the cutting board, whose
   results are dropped on the ground.

Anything that produces food with no window open (hoppers, autocrafters, mod
automation) yields **no credit** by default. `AutomationCredit` may be relaxed
to `OWNER_CREDIT` / `REDUCED_CREDIT` / `FULL_CREDIT` by a server, but ownership
is never guessed.

## Zero custom assets

No PNG, model, blockstate, sound or particle is added. Every UI surface is
built from vanilla GUI sprites, code-drawn panels/gradients, text, and
`ItemRenderer` calls against items that other mods already registered.

## Fail-open rule

If classification, tracking or the journal throws, Farmer's Delight must keep
working. Every hook is wrapped so a failure logs once and disables that hook
rather than propagating into vanilla or Farmer's Delight code.
