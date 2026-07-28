# Compatibility Audit

An overlap survey, run before the major features were built, so Meal Mastery
ends up with a distinct identity instead of cloning something that already
exists. Surveyed against Minecraft 1.20.1 availability.

## Mods with overlapping ideas

| Mod | What it actually does | Overlap | How Meal Mastery differs |
|---|---|---|---|
| **Project MMO** (+ *Project MMO: Farmer's Delight Compat*) | Generic RPG skill/level framework. The FD compat addon grants **generic Cooking XP** for items produced by FD cooking blocks. | Highest overlap in the ecosystem: "cook food → gain a cooking level". | Project MMO stops at one number. Meal Mastery is about the *history* of what you cooked — per-recipe mastery, discovery, ingredient journals, methods, collections, completion. Cooking Level is one line of an Overview page, not the product. |
| **LevelZ** | Character skill levels that **gate** items and blocks behind level requirements. | Levelling vocabulary. | Meal Mastery never gates anything. Farmer's Delight recipes remain craftable exactly as before. |
| **Spice of Life: Apple Pie Edition** / **Spice of Life Onion** | Rewards *dietary variety* — eating a diverse set of foods gives health/hunger benefits. | Variety as a reward axis. | Those mods reward **eating** variety with gameplay stats. Meal Mastery's variety streak rewards **preparing** different dishes with progression, and separates "prepared" from "eaten" as first-class statistics. |
| **Diet** / **A Balanced Diet** / **Nutrition** | Food-group nutrition systems with real hunger/health consequences. | Food classification by group. | Meal Mastery classifies ingredients for *browsing and collections*, never for hunger mechanics. It also refuses to duplicate their tooltip sections. |
| **AppleSkin** | Hunger/saturation HUD and tooltip overlays. | Food tooltips. | Meal Mastery's nutrition display is opt-in, informational, and suppressed by default when AppleSkin-style providers are present. |

## Result: nothing in the 1.20.1 ecosystem does this

A search of Modrinth for cooking-progression, recipe-mastery and food-journal
mods on 1.20.1 turned up **no mod that tracks per-recipe mastery, recipe
discovery, ingredient journals or culinary collections for Farmer's Delight**.
The closest thing is Project MMO's compat addon, which is a single XP number.

Meal Mastery's identity is therefore the *journal*, not the *level*:

> Farmer's Delight provides the food. Meal Mastery provides the reason to cook,
> discover, master and collect it.

## Coexistence rules adopted from this audit

1. **Never hijack another progression system.** Meal Mastery fires only its own
   internal events. It does not emit, listen for, or scale Project MMO XP, and
   its Cooking Level has no relationship to a Project MMO skill level.
2. **Never gate content.** Unlike LevelZ, no recipe, item or block becomes
   unavailable because of a Meal Mastery level.
3. **Never claim a tooltip section another mod owns.** Nutrition/saturation
   lines stay off by default when a nutrition provider is installed.
4. **Never replace a recipe viewer.** JEI/REI/EMI integration is optional, adds
   a mastery indicator and an "open journal entry" action, and is never required
.
5. **Never claim untested compatibility.** The in-game Compatibility screen
   reports `Detected`, `Generic Support` or `Compatible` — the word "Full" is
   reserved for combinations that have actually been run.

## Farmer's Delight addons surveyed for generic support

These were checked to confirm the generic detection strategy holds — every one
of them registers ordinary `farmersdelight:cooking` / `farmersdelight:cutting`
/ vanilla crafting recipes producing ordinary food items, which is exactly what
the classifier consumes:

Nether's Delight (Refabricated), End's Delight, Ender's Delight, Expanded
Delight, More Delight, Ube's Delight, Rustic Delight, Chef's Delight,
Storage Delight, Autochef's Delight, Farmer's Knives.

**No per-addon code was written for any of them**, and none is referenced by id
anywhere in the mod.
