# Public API

`com.xirc.mealmastery.api.MealMasteryApi`

**Most mods need none of this.** Dishes are detected from recipes, so a food
addon that registers ordinary recipes producing ordinary food is already fully
supported with zero integration code. This API exists for the two cases generic
detection genuinely cannot reach.

Everything here is server-side and must be called on the server thread.

## Registering an unusual workstation

Needed only for a workstation that neither opens a menu nor drops its output —
Farmer's Delight's own blocks, and every addon that reuses them, already work.

```java
MealMasteryApi.registerWorkstation(
        new ResourceLocation("mymod", "fermentation_barrel"),
        new ResourceLocation("mymod", "fermenting"));
```

Items a player obtains shortly after using that block are then credited to them,
and `mymod:fermenting` becomes a cooking method with its own statistics page.

## Awarding credit directly

```java
MealMasteryApi.awardCooking(player, dishId, 1, methodId);
```

Only call this when a specific player genuinely caused the dish to exist.
Crediting a nearby player, or the owner of an automated machine, is exactly the
guessing Meal Mastery refuses to do — and a server that wants automation to
count can already say so through `automation.credit`.

## Queries

```java
boolean tracked   = MealMasteryApi.isTrackedDish(dishId);
MealEntry entry   = MealMasteryApi.dish(dishId);          // null when untracked
boolean found     = MealMasteryApi.hasDiscovered(player, dishId);
MasteryRank rank  = MealMasteryApi.masteryOf(player, dishId);
long prepared     = MealMasteryApi.timesPrepared(player, dishId);
int level         = MealMasteryApi.cookingLevel(player);
Set<ResourceLocation> all = MealMasteryApi.trackedDishes();
```

`trackedDishes()` and `dish()` are rebuilt on every datapack reload; do not hold
a `MealEntry` across one.

## Events

```java
MealMasteryApi.onMealPrepared(event -> { ... });
MealMasteryApi.onRecipeDiscovered(event -> { ... });
MealMasteryApi.onMasteryChanged(event -> { ... });
MealMasteryApi.onCookingLevelChanged(event -> { ... });
MealMasteryApi.onChallengeCompleted(event -> { ... });
```

Listeners run on the server thread in registration order. A listener that throws
is logged and skipped — it can never stop a meal from being recorded, let alone
stop food being cooked.

## Excluding recipes

There is no API call for this on purpose. Use the `compatibility` section of the
server config or a datapack, so a pack author can undo it without a code change.
See [configuration.md](configuration.md).

## Versioning

`MealMasteryApi.API_VERSION` is bumped when an existing method changes shape.
Purely additive changes do not bump it.

## What this API deliberately is not

Meal Mastery is not a kitchen framework and must never become a mandatory
dependency. There is no way to register a food, a recipe type, a workstation
menu or a cooking mechanic here — those belong to Farmer's Delight and to the
addons themselves.
