# Carpet-RTS-Addition

A [Carpet](https://modrinth.com/mod/carpet) extension for **Minecraft 26.3**.

It currently adds one rule, `rtsRecipes`, which controls whether the mod's own crafting
recipes can be used.

Author: **Miraitowa-alt**

## Toolchain

| Component | Version |
|---|---|
| Minecraft | 26.3 |
| Fabric Loader | 0.19.5 |
| Fabric Loom | 1.18-SNAPSHOT |
| Fabric API | 0.164.0+26.3 (required: loads the mod's data pack) |
| Carpet | 26.3 |
| Java | 25 |

Minecraft 26.x ships with official (Mojang) names, so the build does not declare a
Yarn mappings dependency.

> Gradle is pinned to JDK 25 via `org.gradle.java.home` in `gradle.properties`.
> Update that path if JDK 25 is installed somewhere else.

## Source layout

| File | Responsibility |
|---|---|
| `CarpetRtsAdditionMod.java` | Fabric `ModInitializer`: reads the mod version, starts the extension |
| `CarpetRtsAdditionServer.java` | `CarpetExtension` singleton: registers rules, commands and translations |
| `settings/CarpetRtsAdditionSettings.java` | Rule declarations (`rtsRecipes`) |
| `settings/CarpetRuleRegistrar.java` | Hands rule classes to Carpet's `SettingsManager` |
| `translations/RtsTranslations.java` | Loads `assets/carpet_rts_addition/lang/<lang>.json` for Carpet |
| `commands/RegisterCommands.java` | Command registration entry point (currently empty) |
| `mixins/RecipeMapMixin.java` | Hides this mod's recipes from recipe lookups while `rtsRecipes` is off |
| `utils/` | Shared helpers (empty) |
| `fabric.mod.json` | Mod id, entrypoint, dependencies, mixin config, access widener |
| `carpet-rts-addition.accesswidener` | Access widener, header only |
| `data/carpet_rts_addition/recipe/*.json` | 39 crafting recipes (registry path, singular `recipe`) |

## Rule: `rtsRecipes`

| | |
|---|---|
| Default | `false` |
| Command | `/carpet rtsRecipes true` / `false` |
| Effect | While off, the mod's recipes are filtered out of recipe matching (`RecipeMap.getRecipesFor`) and id lookup (`RecipeMap.byKey`), so they cannot be crafted or unlocked. Toggling takes effect immediately, no reload needed. |

Recipes are data pack registry entries in 26.x and cannot be added or removed at runtime,
which is why the rule filters lookups instead of the registry. The recipe-book *display*
built at world load may still list them while off; using them fails.

## Adding another rule

1. Add a `public static` field annotated with `carpet.api.settings.Rule` to
   `settings/CarpetRtsAdditionSettings.java` — the field name becomes the rule name.
2. Add its description to `assets/carpet_rts_addition/lang/en_us.json` under
   `carpet.rule.<field>.desc` (descriptions are not part of the annotation).
3. Add commands in `commands/RegisterCommands.java`; register mixin classes in
   `carpet-rts-addition.mixins.json`.

Rule descriptions and translation keys are documented with verified bytecode evidence in
`D:\skill\minecraft-carpet-extension\SKILL.md`.

## Gotchas found while building this

- **Mod data needs Fabric API.** Without `fabric-resource-loader-v0`, `/datapack list` only
  shows `vanilla` and every `data/` file is ignored — recipes silently never load.
- **Recipe path is singular.** 26.x recipes are registry entries: `data/<namespace>/recipe/`,
  not `recipes/`.
- **Tag ingredients are strings:** `"#minecraft:logs"`, not `{"tag": "#minecraft:logs"}`.
  A malformed entry makes `RegistryDataLoader` fail with `Failed to load registries due to
  errors`, i.e. the world will not load at all.
- **Access widener**: first line must be `classTweaker v1 official`, and the file must be
  saved **without a BOM** — Loom rejects both a BOM and the `named` namespace.

## Build

```bash
# Windows
.\gradlew.bat build

# macOS / Linux
./gradlew build
```

The mod jar is written to `build/libs/`.

## License

This template is available under the CC0 license. Feel free to learn from it and
incorporate it in your own projects.
