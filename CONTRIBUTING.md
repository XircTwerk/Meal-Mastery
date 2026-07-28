# Contributing

Meal Mastery targets Java 17, Minecraft 1.20.1, Fabric and Forge.

The `1.21.1` branch targets Minecraft 1.21.1 on Fabric and NeoForge;
Farmer's Delight has no Forge build for that version.

Run the complete verification suite before submitting changes:

```shell
./gradlew build
```

## Rules that are not negotiable

1. **No compile-time dependency on Farmer's Delight.** Everything is resolved
   through vanilla registries, recipe types and tags. See
   `docs/architecture.md`.
2. **No custom art assets.** No PNG, model, blockstate, animation, particle or
   sound. UI is built from vanilla sprites, code-drawn shapes and item
   rendering only.
3. **No hardcoded per-addon database.** Do not add `if (modLoaded("x"))`
   branches for food addons. If something needs per-mod knowledge, it belongs
   in a datapack or in config.
4. **Fail open.** A Meal Mastery failure must never stop Farmer's Delight from
   cooking food.
5. **Server authoritative.** No client packet may claim progression.
6. Loader-independent code lives in `common`; `fabric` and `forge` carry only
   loader hooks and packet transport.
7. Mixins need a narrow target, a documented purpose and a compatibility note.

## Development launches

```bash
./gradlew :fabric:runClient
./gradlew :forge:runClient -Pdev_farmersdelight=true
```

Farmer's Delight is loaded automatically on Fabric. It is a hard runtime
dependency of the shipped mod, so a launch without it cannot start — the loader
refuses with "requires farmersdelight, which is missing". Opt out with
`-Pdev_farmersdelight=false` only if you want to see that failure.

**Forge is the exception and must be asked for explicitly.** Neither default is
good there, so the one that at least boots was chosen; see below.

Two dev-environment quirks, neither of which affects the shipped jar:

* **Fabric.** Farmer's Delight Refabricated ships its dependencies as nested
  jars and Loom does not unpack those for a plain Maven `modRuntimeOnly`, so
  Fabric ASM and the exact Porting Lib modules the jar bundles are named
  individually in `fabric/build.gradle`. Substituting the aggregate
  `porting_lib` mod does not work: newer builds of it demand a newer Fabric
  Loader than this branch targets.
* **Forge.** Farmer's Delight for Forge ships an SRG-mapped refmap for its own
  mixins, which ModDevGradle's Mojang-mapped dev runtime cannot resolve —
  `CuttingBoardDispenserMixin` fails on `DispenserBlock` and the game does not
  start. That is why it stays opt-in here even though Fabric does not: enabling
  it by default would break `:forge:runClient` outright. A real Forge
  installation is unaffected. Use the Fabric launch to exercise the mod against
  Farmer's Delight.
