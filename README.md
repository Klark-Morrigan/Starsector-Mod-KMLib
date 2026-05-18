# Klark Morrigan's Library (KMLib)

Starsector utility mod that hosts shared Java helpers used by the
other KM mod series members ([KMU](../KMU), [KMO](../KMO)). KMLib
carries no game-loop hooks; it only ships a jar that other mods depend
on at compile and runtime.

## Index

- [Layout](#layout)
- [Build & Test](#build--test)
- [Consuming KMLib](#consuming-kmlib)
- [UI Colour Palette](#ui-colour-palette)

## Layout

```
mod_info.json
build.gradle / settings.gradle / gradlew[.bat]
src/main/java/kmlib/
  starsector/ui/color/ - palette enum + Misc-backed resolver
src/test/java/kmlib/  - JUnit 5 + Mockito unit tests
jars/                  - build output (gitignored); KMLib.jar
```

## Build & Test

KMLib uses Gradle with the Java plugin. JDK 17+ must be on PATH; the
toolchain is intentionally not auto-provisioned so the project compiles
on whichever JDK is already installed.

```
./gradlew test       # JUnit 5 unit tests
./gradlew coverage   # tests + JaCoCo HTML/XML report in build/reports/
./gradlew jar        # writes jars/KMLib.jar
```

The Starsector install root is discovered in this order:
`-PstarsectorRoot=<path>` -> `STARSECTOR_HOME` env -> `../..` from this
folder (the canonical layout when the mod lives at
`<starsector>/mods/KMLib`).

## Consuming KMLib

Downstream mods declare KMLib as a hard dependency in `mod_info.json`:

```json
{
  "dependencies": [
    { "id": "kmlib", "name": "Klark Morrigan's Library" }
  ]
}
```

and pull the built jar as `compileOnly` in `build.gradle` so the file is
visible at compile time and supplied by Starsector's mod classloader at
runtime:

```groovy
compileOnly files("${configuredStarsectorRoot}/mods/KMLib/jars/KMLib.jar")
```

Tests in consuming mods that touch KMLib types also add the same jar as
`testCompileOnly` / `testRuntimeOnly`.

## UI Colour Palette

[StarsectorUiColor](src/main/java/kmlib/starsector/ui/color/StarsectorUiColor.java)
is the palette enum: vanilla shades route through `Misc::...` suppliers
(`GRAY`, `TEXT_WHITE`, `BLUE`, `DARK_BLUE`, `GOLD`, `RED`, `GREEN`) so
they track the game's UI palette automatically, and custom shades hold a
literal `java.awt.Color` (`WHITE`, `DIM_GRAY`, `ORANGE`, `DARK_RED`,
`MUTED_RED`, `BRIGHT_RED`, `DARK_GREEN`, `BRIGHT_GREEN`, `LIGHT_BLUE`).
[StarsectorUiColorProvider.get](src/main/java/kmlib/starsector/ui/color/StarsectorUiColorProvider.java)
resolves an entry to its `Color`, throwing on null input and rejecting
entries that somehow carry neither source. The split exists so tests can
stub `Misc` statically without booting Starsector while custom shades
stay pure-data and need no runtime at all.
