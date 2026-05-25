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
- [Player Faction Resolution](#player-faction-resolution)
- [UI Colour Palette](#ui-colour-palette)
- [Highlighted Text](#highlighted-text)
- [Intel Base Classes](#intel-base-classes)

## Layout

```
mod_info.json
build.gradle / settings.gradle / gradlew[.bat]
src/main/java/kmlib/
  text/                     - Starsector-agnostic string predicates
                              (hasText)
  starsector/factions/      - player-faction lifecycle helpers
                              (established-check + display-name
                              normaliser; handles vanilla + Nex defaults)
  starsector/strings/       - defensive wrapper around settings.json
                              localisation lookups (loud REDACTED on
                              missing / malformed entries)
  starsector/ui/color/      - palette enum + Misc-backed resolver
  starsector/ui/highlight/  - highlight + paragraph + message types
                              (renders to text panel, tooltip, label,
                              and MessageIntel)
  starsector/intel/         - intel-plugin base classes
                              (BaseTaggedIntelPlugin for tab-tag
                              mix-in; BaseExpiringIntelPlugin layers
                              auto-removal on top)
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
