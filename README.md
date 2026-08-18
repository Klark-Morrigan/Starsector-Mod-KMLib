# Klark Morrigan's Library (KMLib)

Starsector utility mod that hosts shared Java helpers used by the
other KM mod series members
([KMU](https://github.com/Klark-Morrigan/Starsector-Mod-KMU),
[KMO](https://github.com/Klark-Morrigan/Starsector-Mod-KMO)). KMLib
carries no game-loop hooks; it only ships a jar that other mods depend
on at compile and runtime.

## Index

- [Requirements](#requirements)
- [Layout](#layout)
- [Build & Test](#build--test)
- [Local linting](#local-linting)
- [Reusable CI / release actions](#reusable-ci--release-actions)
- [Consuming KMLib](#consuming-kmlib)
  - [Test fixtures in the main jar](#test-fixtures-in-the-main-jar)
- [Rendering environment](#rendering-environment)
- [Caching](#caching)
- [Player Faction Resolution](#player-faction-resolution)
- [UI Colour Palette](#ui-colour-palette)
- [Highlighted Text](#highlighted-text)
- [Intel Base Classes](#intel-base-classes)

## Requirements

Hard dependencies:

- **LazyLib** - exposes game fonts to be used for drawing labels directly with GL:
  [UI primitives](src/main/java/kmlib/starsector/ui/README.md).
- **LunaLib** - backs in-game mod settings, including the log-verbosity binding every KM
  mod registers through `KmLogging` and KMLib's own (`kmlib_logLevel`, on its Dev tab).
  The library needs a switch of its own because log4j scopes a level to a package subtree:
  a mod's verbosity governs that mod's lines and cannot reach `kmlib` beneath them, and a
  mod that set `kmlib` would be setting it for every other mod in the game.
- **MagicLib** - provides code reflection utilities.

Soft dependencies:

- **Console Commands** - for KMLib's console commands.

Compatibility coded in:

- **Random Assortment of Things** - KMLib recognises RAT's Abyssal Fractures, and
  reports whether its mini-map has replaced the campaign radar.

## Layout

```
mod_info.json
kmlib.version.template  - VersionChecker template; filled into kmlib.version
                          by a release and by `gradlew jar`, generated output
                          that is never committed (see Build & Test, and
                          Reusable CI / release actions)
data/
  config/LunaSettings.csv          - LunaLib settings declarations
  config/version/version_files.csv - names the .version file
                                     VersionChecker reads
  console/commands.csv             - Console Commands registrations
  strings/strings.json             - localisation lookups
build.gradle / settings.gradle / gradlew[.bat]
gradle/
  starsector-mod.gradle - Starsector build conventions every KM mod applies
                          by path: the game's API jars on the compile/test
                          classpath, mod_info.json as the version source,
                          the jar output location the launcher expects
  tasks/checks/report-kmlib-version-mismatch.gradle - warns when a mod
                          compiles against one KMLib and asks players for
                          another
  tasks/release/write-version-file.gradle - registers writeVersionFile for
                          a mod that commits a template (see Build & Test)
src/main/java/kmlib/
  Game-agnostic helpers (no Starsector API on the signature):
  animation/       - positions between two ends that time moves: a linear
                     fraction advanced toward a target and eased on read,
                     so a retarget mid-flight carries on from where it is,
                     the pulse envelope that rides one out and back again -
                     on a single trigger for an act already over, or held
                     at its peak until released for one still being made -
                     and the pair of durations that
                     pace the two directions apart, a motion answering
                     input arriving quicker than it lets go
  collections/     - small Collection / Map helpers
  colour/          - AWT Color to normalised GL channels, folding in an
                     alpha multiplier so one factor fades a palette
  input/           - rising-edge click detection, for polled input with
                     no discrete event to consume
  logging/         - log4j level control over one mod's package subtree, and the
                   scoped names that put library work under the mod it was done for
  math/            - easing/, geometry/ (2D shapes, polygon passes and the
                     point arithmetic under them - see its own README),
                     hashing/ (avalanche, content fingerprints), motion/,
                     random/, ranges/, solving/
  opengl/          - GL primitive emission (lines, quads, triangles,
                     vertex runs), the saved-state scope a blended 2D
                     pass draws inside, how a pass blends and how a
                     texture is sampled, polygon tessellation, hatching,
                     and what KM code must know about Fast Rendering
                     (whether it is in force, how to read its matrix)
  profiling/       - section timings + timing report
  text/            - string and number formatting, plus the reads over a
                     string every surface shares: is there text here, what
                     are its words, and the stutter left where one phrase
                     was appended to another ending on the same word

  Starsector-facing wrappers and seams:
  console/         - Console Commands base class and KMLib's own
                     commands, with input/, output/, parsing/, and
                     validation/ behind them
  settings/        - LunaLib settings read / write + labelled choices
  starsector/
    entities/      - spawning custom campaign entities, their orbits,
                     name generation, and how an entity is identified to
                     a reader - its name paired with the map glyph it is
                     marked with (Gates as a type helper)
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
    rat/           - Random Assortment of Things: Abyssal Fracture matching,
                     and the campaign-minimap role answered for its mini-map
    relation/      - player relationship formatting
    scripts/       - sector script registration helpers
    strings/       - defensive wrapper around settings.json
                     localisation lookups (loud REDACTED on missing /
                     malformed entries), plus the number-to-copy
                     shaping that fills their numeric slots
                     (StarsectorFormat's truncating percent)
    systems/       - star system queries and motion tracking, plus the
                     shared colony set every "who is in this system"
                     read selects through - one rule, one entry per
                     place and owner, unfogged with the visibility
                     filter as one named projection over it, and a
                     per-pass index so a system is walked once however
                     many readers ask about it;
                     claims/ reads vanilla system claims behind a port,
                     with a second port for the scored contest behind
                     one - down to the terms each market's score is the
                     sum of - for callers that must justify a claim
                     rather than merely colour by it. A standing in that
                     contest states its own kind: weighed, resting on the
                     market the mechanic scored, or presence-only at a
                     nought, for a faction holding nothing the mechanic
                     ever reached
    time/          - campaign clock wrapper
    ui/            - UI toolkit, tiered by render substrate: a spec
                     names content (controls, built out of widgets'
                     shared rows), a layout places it, render.gl
                     paints it
      colour/      - palette enum + Misc-backed resolver
      controls/    - declarative control specs and their actions:
                     what a control is, not how it paints
      debug/       - quadrant-anchored on-screen debug HUD
      font/        - the face enum every caller names an atlas through,
                     the font and glyph-run caches, and width measurers
      highlight/   - highlight + paragraph + message types (renders to
                     text panel, tooltip, label, and MessageIntel)
      input/       - pointer / key controllers driving panel and
                     tab-panel state (scroll, drag, collapse), the hover
                     fades and press lifts its parts animate by, and the
                     moments it answers audibly - detected here, with
                     which sound each makes left to the look
      intel/       - obf-cast seam onto the intel screen: tab open,
                     map visor rect, that map's starscape flag
      label/       - label length estimation and box fitting
      layout/      - pure placement maths: padding, anchors, strips,
                     panel and tab-panel layout
      map/         - obf-cast seam onto the campaign map: view state,
                     the cross-host "is a starscape map up" fold-in,
                     the campaign-minimap role for the surface that
                     replaces the radar rather than opening as a screen,
                     screen/world transform, modelview matrix readers,
                     vanilla map tooltip
      render/gl/   - the GL paint layer. The root is the drawing surface
                     itself - fills, borders, sprites, scissor, labels -
                     with a package per subject composed over it:
                     style/ (the look a host hands in), controls/,
                     tabs/ (both tab chromes), panel/ (box, scrollbar,
                     collapse notch), tooltip/
      sound/       - the engine's interface sounds a KM control answers
                     with, the cue binding one to a volume, and the
                     scheme naming what each moment sounds like -
                     carried in the panel's look, so a control inherits
                     its sound as it inherits its accent - and the
                     player port behind them
      text/        - substrate-neutral text look: the face, colour,
                     casing, and anchoring a run of text draws with,
                     with each render substrate owning the adapter into
                     its own anchors, plus a run's width in a settled look
      tooltip/     - vanilla TooltipMakerAPI helpers
      widgets/     - widget models and their geometry: one shared
                     labelled-row core - a label read as one sentence
                     with a slot to either side - and the rows and boxes
                     built on it, with lists/, scroll/, segments/, and
                     tabs/ beneath - the last splitting its geometry from
                     the tabs/style/ a host varies; lists/ is the
                     spotlight picker - the
                     sort-mode, direction and column-count model it ranks
                     and wraps by, the item seam it draws rows from, and
                     the memo a consumer holds its list in - holding no
                     store of its own
  testfixtures/    - Fakes for KMLib's own ports (claims, fonts, intel
                     screen, modelview, campaign minimap, console
                     output), the settings proxy every mod installs into
                     Global - with the three mod-set states an optional
                     dependency is read against, and the third-party mod
                     ids those states are named with - and for the core
                     UI - the hops down to the screen that is up, and the
                     widget tree a layout rule walks once there - plus
                     builders for the values those ports report. Ships in
                     the MAIN jar so consumer mods' tests can use them
    starsector/
      settings/    - the no-op SettingsAPI proxy KM tests install into
                     Global before touching Misc (whose static
                     initialiser would otherwise NPE)
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
    version shape, kmlib dep SemVer pin)
  actions/check-dependency-release/ - confirms a pinned dependency
    version exists as a published release of the repo shipping it,
    and emits that release's URL
  actions/compose-dependency-note/ - composes the release-body line
    naming that dependency release and linking it
  actions/fill-version-file-template/ - fills the caller's
    committed <mod-id>.version.template from mod_info.json, producing
    the VersionChecker file for the release being cut
  actions/_lib/mod_info.sh    - what mod_info.json contains and what
    shape its fields take; sourced by the four scripts above
  tests/                       - bats-core tests for the action scripts
  workflows/ci-gradle.yml      - the Gradle gate, on the self-hosted
    kmlib-runner; built twice, with and without Fast Rendering's jar
  workflows/release.yml        - release entry point; delegates to
    mod-release.yml below
  workflows/mod-release.yml    - the reusable release pipeline this
    repo hosts for the whole KM series
```

Packages with more behind them than one line can carry:

| Package | Read |
| --- | --- |
| [`opengl/`](src/main/java/kmlib/opengl/), [`starsector/ui/map/`](src/main/java/kmlib/starsector/ui/map/), [`starsector/ui/render/gl/`](src/main/java/kmlib/starsector/ui/render/gl/) | [Rendering environment](#rendering-environment) |
| [`math/geometry/`](src/main/java/kmlib/math/geometry/) | [2D shapes and polygon passes](src/main/java/kmlib/math/geometry/README.md) |
| [`starsector/factions/`](src/main/java/kmlib/starsector/factions/) | [Player Faction Resolution](#player-faction-resolution) |
| [`starsector/intel/`](src/main/java/kmlib/starsector/intel/) | [Intel Base Classes](#intel-base-classes) |
| [`testfixtures/`](src/main/java/kmlib/testfixtures/) | [Test fixtures in the main jar](#test-fixtures-in-the-main-jar) |
| [`starsector/ui/`](src/main/java/kmlib/starsector/ui/) | [UI primitives, tiered by surface](src/main/java/kmlib/starsector/ui/README.md) |
| [`starsector/ui/colour/`](src/main/java/kmlib/starsector/ui/colour/) | [UI Colour Palette](#ui-colour-palette) |
| [`starsector/ui/font/`](src/main/java/kmlib/starsector/ui/font/), [`starsector/ui/label/`](src/main/java/kmlib/starsector/ui/label/) | [Caching](#caching) |
| [`starsector/ui/highlight/`](src/main/java/kmlib/starsector/ui/highlight/) | [Highlighted Text](#highlighted-text) |
| [`starsector/ui/text/`](src/main/java/kmlib/starsector/ui/text/) | [Two span measurers](src/main/java/kmlib/starsector/ui/README.md#two-span-measurers) |

Three packages reach into the game's concrete UI classes, by two mechanisms that
differ in what a broken link costs the caller.

The obf-cast seams, [`starsector/ui/intel/`](src/main/java/kmlib/starsector/ui/intel/)
and [`starsector/ui/map/`](src/main/java/kmlib/starsector/ui/map/), compile against
the obfuscated jars and cast. Both fail closed - an unresolvable link reports
"nothing there" instead of throwing on a live screen.

[`starsector/ui/coreui/`](src/main/java/kmlib/starsector/ui/coreui/) reaches the same
classes by *name* instead, which is the only way in for members an obfuscated build
leaves unwritable in Java source. It is deliberately policy-free: a hop either
answers or throws, and what a failure means is the caller's to decide, since a read
that suppresses a feature and one that draws it want opposite defaults. So a
consumer of that package writes its own guard - over `Throwable`, the reach
declaring nothing - where a consumer of the cast seams inherits one.

## Reusable CI / release actions

KMLib hosts composite actions and reusable workflows that other mods in
the KM series consume via
`uses: <owner>/KMLib/.github/actions/<name>@<tag>`. Each action
delegates to a shell script under its own `scripts/` directory so the
logic stays unit-testable with bats-core; the matching tests live in
[.github/tests/](.github/tests/). Run them with `bats .github/tests/`
from the KMLib root (requires `bats-core` and `jq`).
[ci-bash.yml](.github/workflows/ci-bash.yml) runs the same bats suite on
every pull request, through Common-Automation's reusable bash workflow.
[action_outputs.bats](.github/tests/action_outputs.bats) covers the seam
between the two halves of an action rather than either one's logic: an
action.yml and the script behind it declare and emit their outputs in
separate files, and a key present in only one of them costs a consumer a
blank string rather than an error.
The Gradle build is gated separately in
[ci-gradle.yml](.github/workflows/ci-gradle.yml), because it needs
Starsector binaries and so runs on the self-hosted `kmlib-runner`.

- [read-mod-info](.github/actions/read-mod-info/action.yml) reads the
  caller's `mod_info.json` and emits the values every other workflow
  derives from it by convention: the mod id and version verbatim, plus
  the `<mod-id>-runner` label, the first declared jar, the repos to clone
  beside the checkout, and - named after that jar rather than after the
  mod id, so a zip install and a hand-deployed one share one folder
  layout - the shipped folder name, the `dist/<mod-folder-name>/`
  directory, and the `<mod-folder-name>-<version>.zip` release name.
  `version-file-name` goes the other way, `<mod-id>.version`, because
  the hand-written `version_files.csv` points at it by that name.
  `kmlib-dependency-version` reports which KMLib the caller pins, or
  nothing when it declares no such dependency, which is how the release
  pipeline tells a consumer apart from KMLib releasing itself, and
  `kmlib-repo` names where that KMLib is published - emitted from the
  same constant the sibling checkout set is built from, so the release a
  pin is checked against is the repository the build compiles it against.
  The action's own `outputs:` block is the statement of that convention.
- [check-version](.github/actions/check-version/action.yml) compares
  `mod_info.json`'s `.version` to the latest git tag in the caller
  checkout and emits `version` plus `version-updated`, which gates the
  release pipeline.
- [validate-versioning](.github/actions/validate-versioning/action.yml)
  takes a `version` input and fails the release if the caller's
  `CHANGELOG.md` has no `## [<version>]` section, `mod_info.json`
  `.version` does not equal the input, or either that version or a
  declared `kmlib` dependency pin is not well-formed `MAJOR.MINOR.PATCH`.
  The shape is enforced here because every downstream consumer of a
  malformed version degrades in silence rather than erroring. Policy
  itself lives in [docs/dev/versioning.md](docs/dev/versioning.md).
- [check-dependency-release](.github/actions/check-dependency-release/action.yml)
  takes a `repo` and a `version` and fails unless that version exists as a
  published release of that repository, emitting the release's
  `release-url`. The release pipeline runs it against a consumer's `kmlib`
  pin before building anything, so a pin bumped ahead of the KMLib it names
  stops the release instead of publishing a mod the game refuses to load.
  One API call answers both questions, and the URL is read from the
  response rather than assembled from the tag, so the link the release body
  carries cannot name a release nothing confirmed. An absent release and a
  lookup that could not be completed both fail, with different messages:
  the first is a verdict on the pin, the second explicitly is not.
- [compose-dependency-note](.github/actions/compose-dependency-note/action.yml)
  takes a `dependency-name`, a `version` and a `release-url` and emits the
  `note` the release body carries below its changelog section: a markdown
  line naming that dependency release and linking it. An action rather than
  an inline step in the release workflow because the line is player-facing
  copy with a condition attached - an absent version emits no line, which
  is the case for KMLib releasing itself, and an absent URL emits no line
  either, a dropped line beating one whose link goes nowhere.
  takes an `output-path` and a `zip-name` and writes the mod's
  VersionChecker `.version` file there. The caller commits a complete
  `<mod-id>.version.template` whose release-varying values are written as
  tokens - `{{modName}}`, `{{major}}` / `{{minor}}` / `{{patch}}`,
  `{{starsectorVersion}}`, `{{directDownloadURL}}` - and the action
  substitutes each from `mod_info.json`, so no version number is restated
  by hand outside that file. `{{directDownloadURL}}` is the one token not
  read from there: it is built from the version, the `zip-name` input, and
  the publishing repository the Actions runtime exports, so the link cannot
  name a repository or an asset other than the one being released. Both of
  those have a fallback for callers outside Actions, which is what lets the
  local Gradle task run this same script rather than a second implementation
  of it: an omitted `zip-name` is derived from `mod_info.json` `jars[0]` by
  the same lib rule `read-mod-info` derives it by, and an absent
  `GITHUB_REPOSITORY` falls back to the checkout's `origin` remote.
  Substitution is by whole value, which is how
  the version components come out as JSON numbers rather than quoted
  digits; every other key is carried through untouched, so the template
  states the published shape. A token left unsubstituted fails the release
  rather than shipping literal braces to players. The committed name carries
  the `.template` suffix because a dev install symlinks the repo into
  `mods/`: `version_files.csv` names the bare `<mod-id>.version`, and under
  one name the committed file would hand VersionChecker quoted tokens where
  it expects numbers. The bare name exists only where something generated
  it. `mod-root` says where to
  read `mod_info.json` and the template from and defaults to the working
  directory, which is where a job with the mod checked out at the workspace
  root already stands; the release pipeline sets it because its checkout is
  one level down and Actions permits no `working-directory` on a `uses:`
  step. `output-path` is unaffected by it - always relative to the job, so
  a caller writing into the checkout and one writing beside it each state
  the path they would state anyway.

All four read `mod_info.json`, so what that file contains and what shape
its fields take live once in
[_lib/mod_info.sh](.github/actions/_lib/mod_info.sh), which they source: the
filename, the SemVer shape, the "this field is present" check, and the names
that follow from `jars[0]` - the shipped mod folder and the release zip. The
zip name is there rather than in one script because two of them need it and
neither may guess: the pipeline uploads an asset under that name while the
version file points a download URL at it, so a rule spelled twice would give
a working link and a 404 the same spelling. It sits
under `actions/` rather than beside it so it is where the scripts sourcing
it look, each reaching it relative to its own location. The leading
underscore marks it as not-an-action.

`mod-release.yml` names these four in registry form
(`Klark-Morrigan/Starsector-Mod-KMLib/.github/actions/<name>@master`), which
the runner resolves without checking this repo out into the consumer's
workspace. That form accepts no token, so a consumer's release can only
reach them once this repository is public or shared for Actions use; KMLib's
own release is unaffected, a workflow always reaching its own repository.

Cutting the GitHub release itself - extracting the `## [<version>]`
section for the body and attaching the assets - is delegated to
Common-Automation's stack-agnostic `create-github-release` action.
Only the four `mod_info.json`-coupled actions above live in KMLib.

A release carries two assets. The mod zip is what a player downloads, and
the generated `.version` file rides inside it as well, so an install knows
which version it is. The same file is attached in its own right because
that is the only form an update checker can reach: it polls
`releases/latest/download/<mod-id>.version` without downloading the mod,
and a copy sealed inside the zip answers nothing. GitHub excludes
prereleases from `releases/latest`, so a mod marked prerelease publishes a
URL that resolves to an earlier release or to nothing - which is why
nothing in this pipeline can mark one.

## Build & Test

KMLib uses Gradle with the Java plugin. JDK 17+ must be on PATH; the
toolchain is intentionally not auto-provisioned so the project compiles
on whichever JDK is already installed.

```
./gradlew test       # JUnit 5 unit tests
./gradlew coverage   # tests + JaCoCo HTML/XML report in build/reports/
./gradlew jar        # writes jars/KMLib.jar and kmlib.version
```

The version file comes from `writeVersionFile`, which `jar` depends on: it fills
`kmlib.version.template` into `kmlib.version` at the repo root by running the same
[fill-version-file-template](.github/actions/fill-version-file-template/action.yml)
script the release pipeline runs, so a checkout symlinked into `mods/` as a dev
install reports to VersionChecker exactly what a published zip would. The task is
registered by
[gradle/tasks/release/write-version-file.gradle](gradle/tasks/release/write-version-file.gradle),
which the shared Starsector conventions apply, rather than by this repo's
`build.gradle`, so every mod applying those conventions gets it; KMLib is one
consumer of its own conventions among several. A mod opts in by committing a
`<mod-id>.version.template` - no template, no task, which is where the KM mods that
publish no update information stay. Tied to the jar rather than to `assemble`
because refreshing a dev install is what building
the jar is: the install picks up the new jar immediately, and a version file left
behind would report a version that is no longer there. It needs a bash, which on
Windows is located from the git on `PATH`. Everything else the generated file
states is the script's to work out: with no release to name the zip and no
`GITHUB_REPOSITORY` outside Actions, it derives the zip name from
`mod_info.json` and reads the repository half of the download URL out of the
checkout's own `origin` remote, so a local build cannot name a repository this
clone does not push to and the build restates neither rule.
`mod_info.json`, the template and the script are its inputs, so it
re-runs only when one of them (or the remote) changes.

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
./gradlew build -PvanillaOnly=true           # build as an unpatched install would
./gradlew build -PrequireFastRendering=true  # fail unless the real fr.jar is bound
```

Each flag names the install it stands in for; the bridge stubs are how the
vanilla one is arranged, which is why the two words are not interchangeable
here. The CI legs carry the same two names.

Both flags refuse to degrade quietly, in opposite directions.
`-PrequireFastRendering` fails rather than falling back to the stubs. The
vanilla binding - whether forced by `-PvanillaOnly` or reached because the
install has no `fr.jar` - fails if the stub source set turns up empty, naming
the directories Gradle actually read. Without that check an absent stub tree
produces four `package com.genir.renderer.bridge.* does not exist` errors that
point at the file importing the stubs rather than at the stubs that went
missing. The case that motivated it: a source set named `bridgeStubs` reads
`src/bridgeStubs/java` by convention, which is the same directory as
`src/bridgestubs` on Windows and a different one on Linux.

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
`testCompileOnly` / `testRuntimeOnly`. KMLib's hard dependencies apply to every mod
that depends on it - see [Requirements](#requirements).

### Test fixtures in the main jar

[`testfixtures/`](src/main/java/kmlib/testfixtures/) holds the fakes a consuming
mod's tests stand its subjects on - KMLib's own ports (claims, fonts, the intel
screen, the modelview, console output), the core-UI hops and widget tree a layout
rule walks, the builders for the values those ports report, and
[`starsector/settings/`](src/main/java/kmlib/testfixtures/starsector/settings/)'s
no-op `SettingsAPI` proxy, which a test installs into `Global` before touching
`Misc` (whose static initialiser would otherwise NPE).

They live in the production source set rather than `src/test` because of how the
jar travels: a consumer resolves KMLib as a flat `files(...)` dependency, which
carries no Gradle variants, so `java-test-fixtures` has nothing to publish
through and a `src/test` class is unreachable downstream. Shipping them in the
main jar is the one mechanism that makes a fake reusable across KMU and KMO with
no new wiring; the classes are never instantiated in play, so a player pays
nothing for them.

Two consequences worth knowing. They are excluded from the JaCoCo report - being
production-located but test-only, counting them would flatter the coverage
signal. And the `Fake` suffix gate scans only `src/test`, so the suffix here is
held by hand.

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

## Caching

KMLib holds three caches, all of them in front of font work: the loaded faces, the
glyph runs minted from them, and the balanced line wraps a label fitter searches
through. All three are safe to hold indefinitely because none of them derives from
the campaign - a cached value here cannot disagree with the sector, which is why
nothing in this library carries an invalidation signal.

[docs/dev/caching.md](docs/dev/caching.md) records what each one keys on, how long
it lives, and the two traps worth knowing (a never-evicting cache fed
per-frame-varying strings, and who owns a GL text buffer's disposal). It also
states what is deliberately *not* cached: every Starsector-facing wrapper reads
live, because only a consumer knows which campaign changes it must react to.

Consuming mods that cache derived campaign state should read it alongside their own
invalidation model - KMU's is the worked example.

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

[StarsectorUiColour](src/main/java/kmlib/starsector/ui/colour/StarsectorUiColour.java)
is the palette enum: vanilla shades route through `Misc::...` suppliers
(`GRAY`, `TEXT_WHITE`, `BLUE`, `DARK_BLUE`, `GOLD`, `RED`, `GREEN`) so
they track the game's UI palette automatically, and custom shades hold a
literal `java.awt.Color` (`WHITE`, `DIM_GRAY`, `ORANGE`, `DARK_RED`,
`MUTED_RED`, `BRIGHT_RED`, `DARK_GREEN`, `BRIGHT_GREEN`, `LIGHT_BLUE`).
Call `StarsectorUiColour#resolve()` to obtain the live `Color`; the
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
