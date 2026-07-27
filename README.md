# Klark Morrigan's Library (KMLib)

Starsector utility mod that hosts shared Java helpers used by the
other KM mod series members
([KMU](https://github.com/Klark-Morrigan/Starsector-Mod-KMU),
[KMO](https://github.com/Klark-Morrigan/Starsector-Mod-KMO)). KMLib
carries no game-loop hooks; it only ships a jar that other mods depend
on at compile and runtime.

## Index

- [Layout](#layout)
- [Build & Test](#build--test)
- [Local linting](#local-linting)
- [Reusable CI / release actions](#reusable-ci--release-actions)
- [Consuming KMLib](#consuming-kmlib)
- [Rendering environment](#rendering-environment)
- [Player Faction Resolution](#player-faction-resolution)
- [UI Colour Palette](#ui-colour-palette)
- [Highlighted Text](#highlighted-text)
- [Intel Base Classes](#intel-base-classes)

## Layout

```
mod_info.json
build.gradle / settings.gradle / gradlew[.bat]
src/main/java/kmlib/
  Game-agnostic helpers (no Starsector API on the signature):
  collections/     - small Collection / Map helpers
  color/           - AWT Color to normalised GL channels, folding in an
                     alpha multiplier so one factor fades a palette
  input/           - rising-edge click detection, for polled input with
                     no discrete event to consume
  logging/         - log4j level control over one mod's package subtree
  math/            - easing/, geometry/ (polygon offsetting, smoothing,
                     regions, rings, Voronoi cells, principal axis,
                     spans, disks), hashing/ (avalanche, content
                     fingerprints), motion/, random/, ranges/, solving/
  opengl/          - GL primitive emission (lines, quads, triangles,
                     vertex runs), polygon tessellation, hatching, and
                     what KM code must know about Fast Rendering
                     (whether it is in force, how to read its matrix)
  profiling/       - section timings + timing report
  text/            - string and number formatting / predicates

  Starsector-facing wrappers and seams:
  console/         - Console Commands base class and KMLib's own
                     commands, with input/, output/, parsing/, and
                     validation/ behind them
  settings/        - LunaLib settings read / write + labelled choices
  starsector/
    entities/      - spawning custom campaign entities, their orbits,
                     and name generation (Gates as a type helper)
    factions/      - player-faction lifecycle (established-check +
                     display-name normaliser; handles vanilla + Nex
                     defaults), faction colours, crests, flags
    fleet/         - player fleet proximity
    geometry/      - distance and bearing between campaign entities,
                     the game-typed sibling of kmlib.math.geometry
    graphics/      - sprite lookup
    intel/         - intel-plugin base classes (BaseTaggedIntelPlugin
                     for tab-tag mix-in; BaseExpiringIntelPlugin layers
                     auto-removal on top)
    map/           - which systems the sector map marks
    markets/       - market queries, decivilised markets, patrol counts
    memory/        - typed sector-memory accessors (flag, string)
    rat/           - Random Assortment of Things entity matching
    relation/      - player relationship formatting
    scripts/       - sector script registration helpers
    strings/       - defensive wrapper around settings.json
                     localisation lookups (loud REDACTED on missing /
                     malformed entries)
    systems/       - star system queries and motion tracking;
                     claims/ reads vanilla system claims behind a port
    testing/       - the no-op SettingsAPI proxy KM tests install into
                     Global before touching Misc (whose static
                     initialiser would otherwise NPE)
    time/          - campaign clock wrapper
    ui/            - UI toolkit, tiered by render substrate:
                     controls -> widgets -> layout -> render.gl
      color/       - palette enum + Misc-backed resolver
      controls/    - declarative control specs and their actions:
                     what a control is, not how it paints
      debug/       - quadrant-anchored on-screen debug HUD
      font/        - the face enum every caller names an atlas through,
                     the font and glyph-run caches, and width measurers
      highlight/   - highlight + paragraph + message types (renders to
                     text panel, tooltip, label, and MessageIntel)
      input/       - pointer / key controllers driving panel and
                     tab-panel state (scroll, drag, collapse)
      intel/       - obf-cast seam onto the intel screen: tab open,
                     map visor rect, that map's starscape flag
      label/       - label length estimation and box fitting
      layout/      - pure placement maths: padding, anchors, strips,
                     panel and tab-panel layout
      map/         - obf-cast seam onto the campaign map: view state,
                     screen/world transform, modelview matrix readers,
                     vanilla map tooltip
      render/gl/   - the GL paint layer: panel, tabs, controls,
                     scrollbar, collapse notch, cursor tooltips, fills,
                     scissor
      tooltip/     - vanilla TooltipMakerAPI helpers
      widgets/     - widget models and their geometry, with scroll/,
                     segments/, and tabs/ beneath
  testfixtures/    - Fakes for KMLib's own ports (claims, fonts, intel
                     screen, modelview, console output). Ships in the
                     MAIN jar so consumer mods' tests can use them
src/bridgestubs/java/ - compile-only mirrors of the Fast Rendering bridge
                        members KMLib reads, so an install without fr.jar
                        still compiles (see Build & Test); never shipped,
                        never loaded
src/test/java/kmlib/  - JUnit 5 + Mockito unit tests
jars/                  - build output (gitignored); KMLib.jar
scripts/
  run-ci-yaml-and-bash.sh / .bat      - MAIN entry: lint suite + bats
                          tests in one go (Git Bash + Docker; see
                          Local linting)
  run-lint-yaml-and-bash.sh / .bat    - lint half only (no bats)
  run-tests-bash.sh / .bat            - bats tests only
  run-tests-gradle.bat                - double-click launcher for
                          `gradlew test`
  run-coverage-gradle.bat             - double-click launcher for
                          `gradlew coverage`
  fix-permissions.sh / .bat - re-stage +x on tracked *.sh files
.gitattributes          - line-ending pins (*.sh + gradlew -> LF,
                          *.bat + gradlew.bat -> CRLF)
.github/
  workflows/ci-yaml.yml        - YAML / Actions lint via Common-Automation
  workflows/ci-bash.yml        - Bash lint + bats via Common-Automation
  actions/read-mod-info/      - derives mod-id, version, runner
    label, dist dir, zip name, and jar source from a caller's
    mod_info.json
  actions/check-version/      - compares mod_info.json's version to
    the latest git tag; gates the release pipeline
  actions/validate-versioning/ - enforces the versioning policy at
    release time (changelog section, mod_info.json version match,
    kmlib dep SemVer pin)
  tests/                       - bats-core tests for the action scripts
  workflows/ci.yml             - KMLib's own CI; runs the bats tests
```

Packages with more behind them than one line can carry:

| Package | Read |
| --- | --- |
| [`starsector/factions/`](src/main/java/kmlib/starsector/factions/) | [Player Faction Resolution](#player-faction-resolution) |
| [`starsector/ui/color/`](src/main/java/kmlib/starsector/ui/color/) | [UI Colour Palette](#ui-colour-palette) |
| [`starsector/ui/highlight/`](src/main/java/kmlib/starsector/ui/highlight/) | [Highlighted Text](#highlighted-text) |
| [`starsector/intel/`](src/main/java/kmlib/starsector/intel/) | [Intel Base Classes](#intel-base-classes) |
| [`opengl/`](src/main/java/kmlib/opengl/), [`starsector/ui/map/`](src/main/java/kmlib/starsector/ui/map/), [`starsector/ui/render/gl/`](src/main/java/kmlib/starsector/ui/render/gl/) | [Rendering environment](#rendering-environment) |
| [`starsector/ui/font/`](src/main/java/kmlib/starsector/ui/font/), [`starsector/ui/label/`](src/main/java/kmlib/starsector/ui/label/) | [Caching](#caching) |
| [`testfixtures/`](src/main/java/kmlib/testfixtures/), [`starsector/testing/`](src/main/java/kmlib/starsector/testing/) | [Build & Test](#build--test) |

The two obf-cast seams, [`starsector/ui/intel/`](src/main/java/kmlib/starsector/ui/intel/)
and [`starsector/ui/map/`](src/main/java/kmlib/starsector/ui/map/), are the only
packages that reach into the game's concrete UI classes. Both compile against the
obfuscated jars and cast rather than using reflection, and both fail closed - an
unresolvable link reports "nothing there" instead of throwing on a live screen.

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
- [validate-versioning](.github/actions/validate-versioning/action.yml)
  takes a `version` input and fails the release if the caller's
  `CHANGELOG.md` has no `## [<version>]` section, `mod_info.json`
  `.version` does not equal the input, or a declared `kmlib` dependency
  lacks a well-formed `MAJOR.MINOR.PATCH` `version`. Policy itself
  lives in [docs/dev/versioning.md](docs/dev/versioning.md).

Cutting the GitHub release itself - extracting the `## [<version>]`
section for the body and attaching the built mod zip - is delegated to
Common-Automation's stack-agnostic `create-github-release` action.
Only the three `mod_info.json`-coupled actions above live in KMLib.

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

One part of the compile classpath varies by machine. KMLib reads the modelview
from Fast Rendering's bridge when that mod is in force, and the bridge ships in
`starsector-core/fr.jar`, which only an install patched by it has. Requiring that
jar would make KMLib buildable only on a patched machine, so the build binds it
when the install has one and falls back to compile-only mirrors of the three
members it reads ([src/bridgestubs/java](src/bridgestubs/java)) when it does not.
Every build logs which of the two it used.

The stubs are never in `KMLib.jar` and never loaded - they exist only so javac has
a signature to resolve. Because a patched install compiles against genir's real
bytes, stub drift shows up as an ordinary compile error there. Either leg can be
built on demand, so neither is only ever exercised on the machine that happens to
select it:

```
./gradlew build -PbridgeStubsOnly=true       # compile as an unpatched install would
./gradlew build -PrequireFastRendering=true  # fail unless the real fr.jar is bound
```

CI runs both on every PR (see
[.github/workflows/ci-gradle.yml](.github/workflows/ci-gradle.yml)), which is why
`kmlib-runner`'s install must be Fast-Rendering-patched. `-PrequireFastRendering`
is what keeps that leg honest: without it an unpatched runner would compile the
stubs and report green for a check that never ran.

For double-click runs from Explorer,
[scripts/run-tests-gradle.bat](scripts/run-tests-gradle.bat) and
[scripts/run-coverage-gradle.bat](scripts/run-coverage-gradle.bat) wrap the
`test` and `coverage` tasks above against the deployed install and pause on
exit.

## Local linting

Two delegating CI workflows lint the repo's non-Gradle surface on every pull
request: [ci-yaml.yml](.github/workflows/ci-yaml.yml) calls Common-Automation's
reusable `ci-yaml.yml` (actionlint, action-validator, yamllint, ansible-lint)
and [ci-bash.yml](.github/workflows/ci-bash.yml) calls its reusable `ci-bash.yml`
(shellcheck, check-sh-executable, bats). Each step auto-skips when its surface is
absent, so a mod with no shell scripts still passes the Bash workflow. The Gradle
build and JUnit tests are NOT part of these workflows - they run through Gradle
(see [Build & Test](#build--test)); these gates cover only YAML / Actions / Bash.

Three sibling shims reproduce that CI surface locally through Git Bash +
Docker, each delegating to Common-Automation's orchestrator:

- [scripts/run-ci-yaml-and-bash.sh](scripts/run-ci-yaml-and-bash.sh) (with the
  [run-ci-yaml-and-bash.bat](scripts/run-ci-yaml-and-bash.bat) launcher for
  `cmd` / PowerShell) is the MAIN entry - it runs BOTH the lint suite AND the
  bats tests in one go, the full local equivalent of ci-yaml.yml + ci-bash.yml.
  This is what most contributors run.
- [scripts/run-lint-yaml-and-bash.sh](scripts/run-lint-yaml-and-bash.sh) (with
  its [.bat](scripts/run-lint-yaml-and-bash.bat) launcher) runs the lint half
  only (shellcheck, actionlint, action-validator, yamllint, ansible-lint); no
  bats.
- [scripts/run-tests-bash.sh](scripts/run-tests-bash.sh) (with its
  [.bat](scripts/run-tests-bash.bat) launcher) runs the bats tests only.

All three are thin shims over Common-Automation's engine, so they require a
Common-Automation checkout as a SIBLING directory (`..\Common-Automation`). The
Gradle build and JUnit tests stay separate - they live in Gradle (see
[Build & Test](#build--test)); these shims cover only the YAML / Actions / Bash
surface.

[scripts/fix-permissions.sh](scripts/fix-permissions.sh) (and its
[.bat](scripts/fix-permissions.bat)) re-stages the executable bit on tracked
`*.sh` files, which Windows checkouts drop; run it after adding a shell script so
the `check-sh-executable` gate stays green.
[.gitattributes](.gitattributes) pins line endings surgically - `*.sh` and
`gradlew` to LF, `*.bat` and `gradlew.bat` to CRLF - and leaves binary / data
assets to git's own detection.

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

## Rendering environment

KMLib's map and UI code draws straight against OpenGL, where two things are not
visible from the source: how the campaign UI sets up its matrices, and how the
widely-installed Fast Rendering mod (`com.genir.renderer`) rebinds GL calls in
every mod jar to its own batching bridge. That bridge tracks the modelview on the
CPU, so GL state reads do not mean what they appear to mean.

[docs/dev/rendering-environment.md](docs/dev/rendering-environment.md) records
those facts, each cited into the decompiled sources cache so it can be
re-verified rather than trusted. Read it before touching GL state, adding a
matrix read, or diagnosing an overlay that misbehaves only for some players.
Consuming mods link there rather than restating it.

[docs/dev/issues/genir-glgetfloat.md](docs/dev/issues/genir-glgetfloat.md) is
the worked example: the bridge ships no buffer-taking `glGetFloat`, so reading
`GL_MODELVIEW_MATRIX` throws `NoSuchMethodError` mid-render on an install that
has Fast Rendering and never on one that does not. It is written up as an
upstream report, and is why the matrix readers in
[`starsector/ui/map/`](src/main/java/kmlib/starsector/ui/map/) are behind a
port with one implementation per environment.

## Player Faction Resolution

[StarsectorPlayerFactionResolver](src/main/java/kmlib/starsector/factions/StarsectorPlayerFactionResolver.java)
centralises faction-display-name normalisation across consuming mods.
The player faction's `getDisplayName()` is always non-empty but its
value varies by environment: vanilla pre-first-colony reports
`"Independent"`, Nexerelin's stock `player.faction` reports the literal
`"player"`, and the user can edit either to a custom name later.
Substituting the raw value into prose (`"Production from a local
player settlement..."`, `"player leader in orbit"`) reads poorly when
the player has not finalised an identity yet. Two static entry points
share one placeholder set (`Independent` / `player` / `Player`):

- `isPlayerFactionEstablished()` returns `true` when the display name
  is NOT in the placeholder set OR `Misc.getPlayerMarkets(false)` is
  non-empty (`false` so Nex commission / governorship markets do not
  count - those put the player under another flag, not their own).
  The OR is deliberate: requiring both signals would mis-classify both
  Nex's custom-faction-at-game-start flow and vanilla's
  keeps-Independent-through-rename flow.
- `resolveDisplayName(faction, fallback)` returns the live display
  name when populated and not in the placeholder set, else the
  caller's `fallback`. Generic - operates on any faction, not just the
  player - so host-faction-in-contested-prose, remote-management-fee
  tooltip, and any other faction-substituting surface share one policy.

A package-private overload of the no-arg `isPlayerFactionEstablished`
takes a `PlayerFactionSource` test seam so unit tests stub the live
`Global` / `Misc` reads without `mockStatic`.

## UI Colour Palette

[StarsectorUiColor](src/main/java/kmlib/starsector/ui/color/StarsectorUiColor.java)
is the palette enum: vanilla shades route through `Misc::...` suppliers
(`GRAY`, `TEXT_WHITE`, `BLUE`, `DARK_BLUE`, `GOLD`, `RED`, `GREEN`) so
they track the game's UI palette automatically, and custom shades hold a
literal `java.awt.Color` (`WHITE`, `DIM_GRAY`, `ORANGE`, `DARK_RED`,
`MUTED_RED`, `BRIGHT_RED`, `DARK_GREEN`, `BRIGHT_GREEN`, `LIGHT_BLUE`).
Call `StarsectorUiColor#resolve()` to obtain the live `Color`; the
resolver null-checks the supplier output and tags the failure with the
enum name (Misc accessors can return null during early engine boot).

## Highlighted Text

[Highlight](src/main/java/kmlib/starsector/ui/highlight/Highlight.java)
binds a substring to the colour it should render in. The Starsector
text APIs natively take two parallel arrays (substrings + colours)
that are easy to drift apart at the call site; binding them once
removes the alignment risk.
[HighlightedParagraph](src/main/java/kmlib/starsector/ui/highlight/HighlightedParagraph.java)
is "one line of text + base colour + highlights" with render methods
for `TextPanelAPI`, `TooltipMakerAPI`, and `LabelAPI`.
[HighlightedMessage](src/main/java/kmlib/starsector/ui/highlight/HighlightedMessage.java)
extends the family to the campaign side panel: it is an ordered list
of paragraphs whose `toMessageIntel()` maps one paragraph per
`MessageIntel.addLine(...)` for vanilla-spaced multi-line
notifications. Callers dispatch the resulting `MessageIntel` themselves
through `Global.getSector().getCampaignUI().addMessage(...)` - KMLib
deliberately stops at the value type so the campaign-API call stays
visible at the call site. Icon, sound, and the rest of the
`MessageIntel` surface are not exposed yet; they'll be added the
first time a consumer needs them.

## Intel Base Classes

[BaseTaggedIntelPlugin](src/main/java/kmlib/starsector/intel/BaseTaggedIntelPlugin.java)
is the bottom of the KMLib intel hierarchy. It extends vanilla's
`BaseIntelPlugin` and accepts a varargs list of mod-defined tab tags
at construction; its `getIntelTags` override calls `super` and mixes
those tags into the result so subclasses become a pure declaration
(`class MyIntel : BaseTaggedIntelPlugin(MyTags.SOMETHING)`) with no
`getIntelTags` boilerplate. Zero varargs is a valid call and yields
a pure pass-through, which is what lets the expiring chain below
support untagged consumers.

[BaseExpiringIntelPlugin](src/main/java/kmlib/starsector/intel/BaseExpiringIntelPlugin.java)
extends `BaseTaggedIntelPlugin` so an expiring intel can declare its
tab tags through the same constructor channel. The no-arg constructor
delegates to the empty-tag form for callers that pin to a vanilla
tab. Captures the creation timestamp and auto-removes from the
`IntelManager` once `getExpiryDays()` elapses; the default window is
one Starsector month. Static `findActive(Class)` helper returns the
first non-expired item of a given subclass so synchronous callers
share one definition of "still within the current window".
