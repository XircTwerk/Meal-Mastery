# Per-file upload settings

Four jars, one per branch/loader combination. All are version `0.1.0`.

Values below come from `gradle.properties` on each branch. Do not widen a
version range here that the jar metadata does not declare.

---

## 1. Fabric — Minecraft 1.20.1

| | |
|---|---|
| Branch | `1.20.1` |
| Jar | `fabric/build/libs/mealmastery-fabric-1.20.1-0.1.0.jar` |
| Minecraft version | 1.20.1 |
| Mod loader | Fabric |
| Java | 17 |
| Release channel | Beta |
| Display name | Meal Mastery 0.1.0 — 1.20.1 Fabric |

Dependencies:

| Project | Relation | Notes |
|---|---|---|
| Farmer's Delight Refabricated | **Required** | jar declares `>=1.20.1-2.0.0` |
| Fabric API | **Required** | jar declares `*` |

---

## 2. Forge — Minecraft 1.20.1

| | |
|---|---|
| Branch | `1.20.1` |
| Jar | `forge/build/libs/mealmastery-forge-1.20.1-0.1.0.jar` |
| Minecraft version | 1.20.1 |
| Mod loader | Forge |
| Java | 17 |
| Release channel | Beta |
| Display name | Meal Mastery 0.1.0 — 1.20.1 Forge |

Dependencies:

| Project | Relation | Notes |
|---|---|---|
| Farmer's Delight | **Required** | jar declares `[1.20.1-1.2.0,)` |

Forge loader range declared in the jar: `[47,)`.

---

## 3. Fabric — Minecraft 1.21.1

| | |
|---|---|
| Branch | `1.21.1` |
| Jar | `fabric/build/libs/mealmastery-fabric-1.21.1-0.1.0.jar` |
| Minecraft version | 1.21.1 |
| Mod loader | Fabric |
| Java | 21 |
| Release channel | Beta |
| Display name | Meal Mastery 0.1.0 — 1.21.1 Fabric |

Dependencies:

| Project | Relation | Notes |
|---|---|---|
| Farmer's Delight Refabricated | **Required** | jar declares `>=1.21.1-3.0.0` |
| Fabric API | **Required** | jar declares `*` |

---

## 4. NeoForge — Minecraft 1.21.1

| | |
|---|---|
| Branch | `1.21.1` |
| Jar | `neoforge/build/libs/mealmastery-neoforge-1.21.1-0.1.0.jar` |
| Minecraft version | 1.21.1 |
| Mod loader | NeoForge |
| Java | 21 |
| Release channel | Beta |
| Display name | Meal Mastery 0.1.0 — 1.21.1 NeoForge |

Dependencies:

| Project | Relation | Notes |
|---|---|---|
| Farmer's Delight | **Required** | jar declares `[1.21.1-1.2.0,)` |

NeoForge loader range declared in the jar: `[4,)`. There is no Forge build for
1.21.1 because Farmer's Delight has none.

---

## Notes

**Release channel.** Beta is suggested because 0.1.0 is the first public build
and no version has been through outside testing. Both platforms let you promote
later files to Release without touching these.

**Farmer's Delight is a required dependency on all four files.** The mod does
not compile against it, but the loader metadata declares it mandatory, so the
game will refuse to start without it. Declare it required on the upload pages so
that matches.

**Fabric API is required on the Fabric files only.** `fabric.mod.json` depends
on it; the Forge and NeoForge builds do not.

**Sides.** Both jars are client-and-server. On Modrinth set Client side
*Required* and Server side *Required* — the mod exchanges its own packets, so a
one-sided install is not supported.

**Project slugs to search for when adding dependencies:**

| Platform | Farmer's Delight (Forge/NeoForge) | Farmer's Delight (Fabric) | Fabric API |
|---|---|---|---|
| CurseForge | `farmers-delight` | `farmers-delight-refabricated` | `fabric-api` |
| Modrinth | `farmers-delight` | `farmers-delight-refabricated` | `fabric-api` |
