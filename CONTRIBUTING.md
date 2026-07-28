# Contributing

Meal Mastery targets Java 21, Minecraft 1.21.1, Fabric and NeoForge.

Farmer's Delight has no Forge build for 1.21.1. The `1.20.1` branch targets
Minecraft 1.20.1 on Fabric and Forge.

Run the complete verification suite before submitting changes:

```shell
./gradlew build
```

## Rules that are not negotiable

1. **No compile-time dependency on Farmer's Delight.** Everything is resolved
   through vanilla registries, recipe types and tags. See
   `docs/architecture.md`.
2. **No in-game art assets.** No model, blockstate, animation, particle, sound
   or in-game texture. UI is built from vanilla sprites, code-drawn shapes and
   item rendering only. The sole exception is the mod-list icon
   (`assets/mealmastery/icon.png`), which the loader reads from the mod
   metadata and which no mod code may reference or render.
3. **No hardcoded per-addon database.** Do not add `if (modLoaded("x"))`
   branches for food addons. If something needs per-mod knowledge, it belongs
   in a datapack or in config.
4. **Fail open.** A Meal Mastery failure must never stop Farmer's Delight from
   cooking food.
5. **Server authoritative.** No client packet may claim progression.
6. Loader-independent code lives in `common`; `fabric` and `neoforge` carry
   only loader hooks and packet transport.
7. Mixins need a narrow target, a documented purpose and a compatibility note.

## Development launches

```bash
./gradlew :fabric:runClient
./gradlew :neoforge:runClient
```

Farmer's Delight is pulled into dev launches automatically. It is a hard
runtime dependency of the shipped mod, so a launch without it cannot start —
the loader refuses with "requires farmersdelight, which is missing". Opt out
only if you want to see that failure:

```bash
./gradlew :fabric:runClient -Pdev_farmersdelight=false
```

The dev-launch copy of Farmer's Delight Refabricated is pinned below the latest
release; `gradle.properties` explains why. It never touches the compile path.
