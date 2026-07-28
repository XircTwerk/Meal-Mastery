# Suggested categories and tags

## CurseForge

Project type: **Mods** (Minecraft: Java Edition).

Primary category:

- **Addons → Farmer's Delight** (id `10754`) — CurseForge has a dedicated
  Farmer's Delight addon subcategory. It is a more precise fit than *Food* and
  it is the list people browse when they already run Farmer's Delight and are
  looking for things that extend it.

Secondary categories:

- **Food** — the general bucket Farmer's Delight itself sits in; worth having
  so the mod also surfaces for people who are not filtering by addon.
- **Adventure and RPG** — progression, levels, challenges.

Do not select **API and Library**. The mod exposes a public API but is not a
library, and it explicitly refuses to become a mandatory dependency.

Do not select **Armor, Tools, and Weapons**, **World Gen**, **Magic** or
**Technology** — nothing in the mod touches any of them.

Other project settings:

| Field | Value |
|---|---|
| Project License | MIT License |
| Allow distribution outside CurseForge-Overwolf | **Allow** — MIT already grants this, so refusing would contradict the licence |
| Primary language | enUS (English) |

## Modrinth

Project type: **Mod**.

Categories:

- **food** — primary fit, same reasoning as CurseForge.
- **game-mechanics** — mastery changes cooking speed, saturation and effects, so
  this is accurate rather than aspirational.
- **utility** — optional third. Justified by the journal, statistics, export and
  diagnostic commands.

Do not select **library**, **adventure**, **decoration**, **equipment**,
**magic**, **optimization**, **storage**, **technology** or **worldgen**.

Environment:

| Field | Value |
|---|---|
| Client side | Required |
| Server side | Required |

Loaders: Fabric, Forge, NeoForge (across all four files).

Game versions: 1.20.1, 1.21.1.

## Search keywords worth having in the description

These already appear in `description.md` and need no keyword stuffing on top:
Farmer's Delight, cooking, mastery, recipe discovery, culinary journal,
progression, challenges, collections, food addon, datapack, Nether's Delight.
