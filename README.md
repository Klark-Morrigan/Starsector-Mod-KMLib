# Klark Morrigan's Library (KMLib)

Starsector utility mod that hosts shared Java helpers used by the
other KM mod series members ([KMU](../KMU), [KMO](../KMO)). KMLib
carries no game-loop hooks; it only ships a jar that other mods depend
on at compile and runtime.

## Index

- [Layout](#layout)
- [Build & Test](#build--test)
- [Reusable CI / release actions](#reusable-ci--release-actions)
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
.github/
  actions/read-mod-info/      - derives mod-id, version, runner
    label, dist dir, zip name, and jar source from a caller's
    mod_info.json
  actions/check-version/      - compares mod_info.json's version to
    the latest git tag; gates the release pipeline
  actions/extract-changelog/  - writes a CHANGELOG.md section into
    release-notes.md for the GitHub release body
  actions/validate-versioning/ - enforces the versioning policy at
    release time (changelog section, mod_info.json version match,
    kmlib dep SemVer pin)
  tests/                       - bats-core tests for the action scripts
  workflows/ci.yml             - KMLib's own CI; runs the bats tests
```

## Reusable CI / release actions

KMLib hosts composite actions and reusable workflows that other mods in
the KM series consume via
`uses: <owner>/KMLib/.github/actions/<name>@<tag>`. Each action
delegates to a shell script under its own `scripts/` directory so the
logic stays unit-testable with bats-core; the matching tests live in
[.github/tests/](.github/tests/). Run them with `bats .github/tests/`
from the KMLib root (requires `bats-core` and `jq`). KMLib's own
[ci.yml](.github/workflows/ci.yml) workflow runs the same bats suite on
GitHub-hosted `ubuntu-latest` for every pull request, and also exposes
`workflow_call` so other workflows can re-trigger it. The Gradle build
is not gated in this workflow because it depends on Starsector binaries
that the hosted runner does not have.

- [read-mod-info](.github/actions/read-mod-info/action.yml) reads the
  caller's `mod_info.json` and emits the derived values defined in
  [docs/dev/implementation/001-reusable-ci-release-workflows/problem.md](docs/dev/implementation/001-reusable-ci-release-workflows/problem.md#convention-derived-from-modinfojson).
- [check-version](.github/actions/check-version/action.yml) compares
  `mod_info.json`'s `.version` to the latest git tag in the caller
  checkout and emits `version` plus `version-updated`, which gates the
  release pipeline.
- [extract-changelog](.github/actions/extract-changelog/action.yml)
  takes a `version` input, extracts the matching `## [<version>]`
  section from the caller's `CHANGELOG.md`, writes it to
  `release-notes.md`, and emits that path as `notes-file`. Fails if the
  section is missing rather than publishing an empty release body.
- [validate-versioning](.github/actions/validate-versioning/action.yml)
  takes a `version` input and fails the release if the caller's
  `CHANGELOG.md` has no `## [<version>]` section, `mod_info.json`
  `.version` does not equal the input, or a declared `kmlib` dependency
  lacks a well-formed `MAJOR.MINOR.PATCH` `version`. Policy itself
  lives in [docs/dev/versioning.md](docs/dev/versioning.md).

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
