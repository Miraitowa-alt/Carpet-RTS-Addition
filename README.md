# Carpet-RTS-Addition

A [Carpet](https://modrinth.com/mod/carpet) extension for **Minecraft 26.3**.

Author: **Miraitowa-alt**

## Toolchain

| Component | Version |
|---|---|
| Minecraft | 26.3 |
| Fabric Loader | 0.19.5 |
| Fabric Loom | 1.18-SNAPSHOT |
| Carpet | 26.3 |
| Java | 25 |

Minecraft 26.x ships with official (Mojang) names, so the build does not declare a
Yarn mappings dependency.

> Gradle is pinned to JDK 25 via `org.gradle.java.home` in `gradle.properties`.
> Update that path if JDK 25 is installed somewhere else.

## Source layout

| File | Responsibility |
|---|---|
| `CarpetRtsAddition.java` | Fabric `ModInitializer` / Carpet extension entrypoint |
| `CarpetRules.java` | Rules exposed through `/carpet` |
| `RtsSettings.java` | Independent settings managed by the extension |
| `RtsCommand.java` | `/testcommand` registration and output |
| `mixins/MobMakarenaMixin.java` | Mob update hook for the Makarena behavior |

## Setup

1. Install JDK 25 and make sure Gradle can find it.
2. Adjust the versions in `gradle.properties` if needed.
3. Build the mod:

```bash
# Windows
.\gradlew.bat build

# macOS / Linux
./gradlew build
```

The remapped mod jar is written to `build/libs/`.

## License

This template is available under the CC0 license. Feel free to learn from it and
incorporate it in your own projects.
