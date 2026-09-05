# Klark Morrigan's Library (KMLib)

Starsector utility mod that hosts shared Java helpers used by the
other KM mod series members
(e.g. [KMU](https://github.com/Klark-Morrigan/Starsector-Mod-KMU)). Its jar is
what those mods depend on at compile and runtime; beyond it KMLib ships a
[settings tab](#requirements), a set of
[console commands](#console-commands), and a mod plugin that binds the
settings and registers its
[optional-mod adapters](#optional-mod-seams) at application load. It runs
nothing per frame of its own.

## Index

- [Requirements](#requirements)
- [Layout](#layout)
  - [Files](#files)
  - [Packages](#packages)
  - [Reaching the game's own UI classes](#reaching-the-games-own-ui-classes)
- [Build & Test](#build--test)
- [Local linting](#local-linting)
- [Reusable CI / release actions](#reusable-ci--release-actions)
- [Consuming KMLib](#consuming-kmlib)
  - [Test fixtures](#test-fixtures)
- [Console commands](#console-commands)
- [Rendering environment](#rendering-environment)
- [Caching](#caching)
- [Optional mod seams](#optional-mod-seams)
- [Player Faction Resolution](#player-faction-resolution)
- [UI Colour Palette](#ui-colour-palette)
- [Highlighted Text](#highlighted-text)
- [Intel Base Classes](#intel-base-classes)

## Requirements

Hard dependencies:

- **LazyLib** - exposes game fonts to be used for drawing labels directly with GL:
  [UI primitives](src/main/java/kmlib/starsector/ui/README.md).
- **LunaLib** - backs in-game mod settings, including the log-verbosity binding
  registered through `KmLogging` and KMLib's own (`kmlib_logLevel`, on its Dev tab).
  The library needs a switch of its own because log4j scopes a level to a package subtree:
  a mod's verbosity governs that mod's lines and cannot reach `kmlib` beneath them, and a
  mod that set `kmlib` would be setting it for every other mod in the game.
- **MagicLib** - provides code reflection utilities.

Soft dependencies - compiled against, absent from `mod_info.json`, and reached only
behind a presence gate, so an install without any of them is ordinary
(see [Optional mod seams](#optional-mod-seams)):

- **Console Commands**
  - KMLib registers its own commands, listed under
    [Console commands](#console-commands);
  - KMLib publishes whether the console is up and taking text entry,
    which anything drawing over the screen should stand down for.  
- **Nexerelin**
  - KMLib's colony related console commands account for Nexerelin implementation.
- **Random Assortment of Things** (RAT)
  - KMLib recognises Abyssal Fractures, and counts them as valid access points into
    attached systems;
  - KMLib reports whether RAT's mini-map has replaced the campaign radar.

## Layout

### Files

What the game reads:

- [`mod_info.json`](mod_info.json) - mod id, version, dependencies, and the
  plugin class the launcher loads.
- [`data/config/LunaSettings.csv`](data/config/LunaSettings.csv) - LunaLib
  settings declarations.
- [`data/config/version/version_files.csv`](data/config/version/version_files.csv)
  - names the `.version` file VersionChecker reads.
- [`data/console/commands.csv`](data/console/commands.csv) - Console Commands
  registrations, and the per-command help the console prints.
- [`data/strings/strings.json`](data/strings/strings.json) - localisation
  lookups.
- `kmlib.version.template` - the VersionChecker template, filled into
  `kmlib.version` by a release and by `gradlew jar`. Generated output, never
  committed - see [Build & Test](#build--test) and
  [Reusable CI / release actions](#reusable-ci--release-actions).

Sources, one tree per audience:

- [`src/main/java/kmlib/`](src/main/java/kmlib/) - the shipped library; see
  [Packages](#packages).
- [`src/bridgestubs/java/`](src/bridgestubs/java/) - compile-only mirrors of
  the Fast Rendering bridge members KMLib reads, so an install without
  `fr.jar` still compiles (see [Build & Test](#build--test)). Never shipped,
  never loaded.
- [`src/testFixtures/java/`](src/testFixtures/java/) - the fakes and builders
  a consumer's tests may take, published as a variant beside the jar rather
  than inside it. See [Test fixtures](#test-fixtures).
- [`src/test/java/`](src/test/java/) - JUnit 5 and Mockito suites, and the
  fixtures only they use.
- `jars/` - build output, gitignored; holds `KMLib.jar`.

Build:

- `build.gradle`, `settings.gradle`, `gradlew[.bat]` - the build entry points.
- [`gradle/starsector-mod.gradle`](gradle/starsector-mod.gradle) - the
  Starsector build conventions every KM mod applies by path: the game's API
  jars on the compile and test classpath, `mod_info.json` as the version
  source, and the jar output location the launcher expects.
- [`gradle/tasks/checks/report-kmlib-version-mismatch.gradle`](gradle/tasks/checks/report-kmlib-version-mismatch.gradle)
  - warns when a mod compiles against one KMLib and asks players for another.
- [`gradle/tasks/release/write-version-file.gradle`](gradle/tasks/release/write-version-file.gradle)
  - registers `writeVersionFile` for a mod that commits a template.
- [`.gitattributes`](.gitattributes) - line-ending pins: `*.sh` and `gradlew`
  to LF, `*.bat` and `gradlew.bat` to CRLF.

Local runners in [`scripts/`](scripts/), all with a `.sh` and a `.bat` face:

- `run-ci-yaml-and-bash` - MAIN entry: the lint suite and bats tests in one
  go, needing Git Bash and Docker. See [Local linting](#local-linting).
- `run-lint-yaml-and-bash` - the lint half only, no bats.
- `run-tests-bash` - the bats tests only.
- `run-tests-gradle.bat` / `run-coverage-gradle.bat` - double-click launchers
  for `gradlew test` and `gradlew coverage`.
- `fix-permissions` - re-stages `+x` on tracked `*.sh` files.

CI and release, under [`.github/`](.github/). The composite actions are what
other KM mods consume; see
[Reusable CI / release actions](#reusable-ci--release-actions):

- [`workflows/ci-gradle.yml`](.github/workflows/ci-gradle.yml) - the Gradle
  gate on the self-hosted `kmlib-runner`, built twice: with and without Fast
  Rendering's jar.
- [`workflows/ci-yaml.yml`](.github/workflows/ci-yaml.yml) and
  [`ci-bash.yml`](.github/workflows/ci-bash.yml) - YAML, Actions and Bash lint
  plus bats, via Common-Automation.
- [`workflows/release.yml`](.github/workflows/release.yml) - the release entry
  point, delegating to
  [`mod-release.yml`](.github/workflows/mod-release.yml), the reusable
  pipeline this repo hosts for the whole KM series.
- [`actions/read-mod-info/`](.github/actions/read-mod-info/) - derives mod id,
  version, runner label, dist dir, zip name and jar source from a caller's
  `mod_info.json`.
- [`actions/check-version/`](.github/actions/check-version/) - compares
  `mod_info.json`'s version to the latest git tag, gating the pipeline.
- [`actions/validate-versioning/`](.github/actions/validate-versioning/) -
  enforces the versioning policy at release time: changelog section,
  `mod_info.json` version match, version shape, and the kmlib dependency's
  SemVer pin.
- [`actions/check-dependency-release/`](.github/actions/check-dependency-release/)
  - confirms a pinned dependency version exists as a published release of the
  repo shipping it, and emits that release's URL.
- [`actions/compose-dependency-note/`](.github/actions/compose-dependency-note/)
  - composes the release-body line naming that dependency release and linking
  it.
- [`actions/fill-version-file-template/`](.github/actions/fill-version-file-template/)
  - fills the caller's committed `<mod-id>.version.template` from
  `mod_info.json`, producing the VersionChecker file for the release being
  cut.
- [`actions/_lib/mod_info.sh`](.github/actions/_lib/mod_info.sh) - what
  `mod_info.json` contains and what shape its fields take; sourced by the four
  scripts above.
- [`tests/`](.github/tests/) - bats-core tests for the action scripts.

### Packages

One line each, saying what the package is for. Where a package has more behind
it than a line can carry, the line ends with where to read it. The current
release's full inventory is in the
[changelog](CHANGELOG.md#010---2026-08-30).

#### Game-agnostic helpers

No Starsector API on the signature.

- [`animation/`](src/main/java/kmlib/animation/) - what time does to a value,
  in two families: stepped envelopes the caller advances each frame, and read
  ones that are a function of the instant they are asked at. Which to reach
  for, and why a triggered lift and a shape read off a phase share the word
  envelope without being alternatives, is in
  [Stepped and read animation](src/main/java/kmlib/animation/README.md).
- [`collections/`](src/main/java/kmlib/collections/) - small Collection and
  Map helpers.
- [`colour/`](src/main/java/kmlib/colour/) - AWT Color to normalised GL
  channels folding in an alpha multiplier so one factor fades a palette, plus
  darkening, blends, flattening onto a backdrop, and additive overlay and
  light.
- [`extensions/`](src/main/java/kmlib/extensions/) - the point an operation
  offers its work to, so what a piece of work is stays the library's and which
  mod on this install does it is settled where the install is composed. One
  implementation, the last registered, since work is taken over whole or not
  at all. The three optional-mod seam shapes are set beside each other in
  [its own README](src/main/java/kmlib/extensions/README.md).
- [`input/`](src/main/java/kmlib/input/) - rising-edge click detection, for
  polled input with no discrete event to consume.
- [`logging/`](src/main/java/kmlib/logging/) - log4j level control over one
  mod's package subtree bound to a LunaLib setting, the scoped names that put
  library work under the mod it was done for, and warn-once-per-session
  reporting.
- [`math/easing/`](src/main/java/kmlib/math/easing/) - ease-in-out over the
  unit range.
- [`math/geometry/`](src/main/java/kmlib/math/geometry/) - 2D shapes, the
  polygon passes over them, and the point arithmetic underneath. See
  [2D shapes and polygon passes](src/main/java/kmlib/math/geometry/README.md).
- [`math/hashing/`](src/main/java/kmlib/math/hashing/) - avalanche bit mixing,
  multi-part content fingerprints, and the fixed share of the unit range a
  name holds.
- [`math/motion/`](src/main/java/kmlib/math/motion/) - which keyed positions
  moved between two observations, over a movement threshold.
- [`math/random/`](src/main/java/kmlib/math/random/) - bounded jitter around a
  value.
- [`math/ranges/`](src/main/java/kmlib/math/ranges/) - clamping to the unit
  range or into arbitrary bounds.
- [`math/solving/`](src/main/java/kmlib/math/solving/) - bisection to the
  largest value passing a monotone predicate, and picking the higher of two by
  a score.
- [`opengl/`](src/main/java/kmlib/opengl/) - GL primitive emission (lines,
  dashed segments, quads, triangles, vertex runs), the saved-state scope a
  blended 2D pass draws inside, how a pass blends and how a texture is
  sampled, polygon tessellation, viewport and scissor reads, and what KM code
  must know about Fast Rendering. See
  [Rendering environment](#rendering-environment).
- [`opengl/hatch/`](src/main/java/kmlib/opengl/hatch/) - hatch fills across a
  polygon, with a tally of how cleanly the runs join.
- [`profiling/`](src/main/java/kmlib/profiling/) - the profiler seam a mod
  binds, silent until it does; sections opened as nesting scopes, their count,
  total, min, max, average and self time snapshotted as a tree, the counters a
  scope tallies and rolls up beside those durations, and the indented report
  over it.
- [`settings/`](src/main/java/kmlib/settings/) - LunaLib settings read and
  write, immediate and deferred, change callbacks, and labelled choices.
- [`text/`](src/main/java/kmlib/text/) - string and number formatting, plus
  the reads over a string every surface shares: is there text here, what are
  its words, and the stutter left where one phrase was appended to another
  ending on the same word.

#### Starsector-facing wrappers and seams

- [`kmlib/`](src/main/java/kmlib/) - the mod plugin the launcher loads. At
  application load it binds the library's own log verbosity, registers the
  optional-mod adapters, and states which GL renderer every KM draw call
  reaches. Each step is guarded on its own, so a failure costs that step
  rather than every mod depending on the library.
- [`console/`](src/main/java/kmlib/console/) - the Console Commands base class
  and KMLib's own commands, listed under
  [Console commands](#console-commands).
- [`console/input/`](src/main/java/kmlib/console/input/) - one command
  invocation, its context, argument text and output channel, with the
  requirements a command states before parsing.
- [`console/output/`](src/main/java/kmlib/console/output/) - where a command's
  messages go, behind an interface so a test can take them.
- [`console/parsing/`](src/main/java/kmlib/console/parsing/) - declarative
  parameter specs with required and defaulted parameters, typed value parsers,
  and a parsed result reporting validity and which parameters were supplied.
- [`console/targets/`](src/main/java/kmlib/console/targets/) - what a command
  was pointed at, found or refused with a reason under one sealed answer:
  which place - named by id anywhere in the sector, or the nearest one meeting
  what the command needs of it - and which faction it acts for, named by id or
  the player's own.
- [`console/validation/`](src/main/java/kmlib/console/validation/) - the
  context checks a command runs before it does anything, with the
  player-facing feedback they print.
- [`mods/`](src/main/java/kmlib/mods/) - every adapter to a third-party mod,
  one package per mod and nothing else here. What belongs is what stands
  behind a presence gate, so a mod this library is compiled against but cannot
  run without - LunaLib under `settings/`, Fast Rendering under `opengl/` - is
  not one of these. How one is written, and which way the arrows run, is in
  [extensions/README.md](src/main/java/kmlib/extensions/README.md).
- [`mods/consolecommands/`](src/main/java/kmlib/mods/consolecommands/) -
  Console Commands as something to stand down for: whether the mod is enabled,
  and whether a console is taking text entry this frame - the latter as a role
  any caller holds, answered by one shared fail-open reader that settles the
  mod state once and warns once naming whichever hop broke.
- [`mods/nexerelin/`](src/main/java/kmlib/mods/nexerelin/) - Nexerelin:
  founding a colony through that mod's own colonisation, handing an existing
  colony over through that mod's own transfer, stated as a hand-over rather
  than a capture, and the trading counters that mod's own rule decides.
  Registered with the operations in `starsector/markets/` at load, and only
  where the mod is enabled.
- [`mods/rat/`](src/main/java/kmlib/mods/rat/) - Random Assortment of Things:
  Abyssal Fracture matching, registered with the reachability read in
  `starsector/systems/` at load as a means of arrival, and only where the mod
  is enabled; plus the campaign-minimap role answered for its mini-map, which
  a caller holds directly rather than reaching through a register.
- [`starsector/colonies/`](src/main/java/kmlib/starsector/colonies/) - the
  shared colony set every "who is here" read selects through - one rule, one
  entry per place and owner, unfogged - stated once over a location and read
  per kind of place above it: a star system, hyperspace, and the whole sector
  as the composition of the two. Each colony carries its market and the
  listing it was found in, and answers concealment, discovery and ownership.
  What may be *shown* of the set is no part of it - withholding a colony is a
  judgement made for a purpose, and it needs facts the sector does not hold -
  so a consumer states its own projection and hands one in through
  `KnownColonyReader`.
- [`starsector/entities/`](src/main/java/kmlib/starsector/entities/) -
  spawning custom campaign entities and jump points, their orbits, name
  generation, gate activation, and how an entity is identified to a reader:
  its name paired with the map glyph it is marked with.
- [`starsector/factions/`](src/main/java/kmlib/starsector/factions/) -
  player-faction lifecycle, faction colours, crests and flags. See
  [Player Faction Resolution](#player-faction-resolution).
- [`starsector/fleet/`](src/main/java/kmlib/starsector/fleet/) - player fleet
  proximity.
- [`starsector/geometry/`](src/main/java/kmlib/starsector/geometry/) -
  distance and bearing between campaign entities, and how a nearest search
  settles an equal distance; the game-typed sibling of `math/geometry/`.
- [`starsector/graphics/`](src/main/java/kmlib/starsector/graphics/) - sprite
  lookup.
- [`starsector/intel/`](src/main/java/kmlib/starsector/intel/) - intel-plugin
  base classes. See [Intel Base Classes](#intel-base-classes).
- [`starsector/map/`](src/main/java/kmlib/starsector/map/) - which systems the
  sector map marks with a star.
- [`starsector/markets/`](src/main/java/kmlib/starsector/markets/) - what a
  market is, read and never changed: the queries (including whether one is a
  derelict station rather than a place anybody lives), visibility and
  discovery, which of several speaks for a place, decivilised markets, patrol
  counts, and the searches over a location - every market in one, and the
  nearest meeting what a caller needs of it. What can be done *to* a market is
  a package in, one per operation, so a class that answers a question and a
  class that rewrites a colony are never the same word shape in the same
  place.
- [`starsector/markets/colonisation/`](src/main/java/kmlib/starsector/markets/colonisation/)
  - founding a colony on a body that carries only survey data: whether it can
  be, the owner-neutral sequence that settles it, the owner it is founded
  under, and the register whatever colonisation this install supplies is
  offered the founding through before that sequence is composed.
- [`starsector/markets/ownership/`](src/main/java/kmlib/starsector/markets/ownership/)
  - what holding a colony makes true of it - flag, submarkets and tariff,
  stated so that either owner can be applied over the other, with the counters
  deferrable to a mod's own rule - and handing an existing colony to another
  owner: what its outgoing one leaves behind (administrator, free port,
  stockpiling, unrest, and the account at the counter their production was
  sold over, settled while the colony is still theirs to bill), with the owner
  it already has refused rather than costing it all of that for nothing, and
  the registers whatever hand-over and submarket rule this install supplies
  are offered their work through.
- [`starsector/memory/`](src/main/java/kmlib/starsector/memory/) - typed
  sector-memory accessors (flag, string).
- [`starsector/relation/`](src/main/java/kmlib/starsector/relation/) - what
  one faction's standing with another comes to: where a faction stands with the
  player as one value (level, signed reputation, and the colour the game paints
  them in, off a single three-tier read), that value worded the way the engine
  words it, and whether a disposition clears the scale's own step from
  indifference to goodwill - the last asked of a faction in hand, or as a pair
  test over faction ids bound to one sector, so a caller composing dispositions
  takes the read rather than writing the lookup.
- [`starsector/scripts/`](src/main/java/kmlib/starsector/scripts/) - sector
  script registration helpers.
- [`starsector/settings/`](src/main/java/kmlib/starsector/settings/) - the
  game's own settings: whether a mod is enabled, answered the same way for
  every optional-mod gate and answering "not installed" before the game is up;
  and the common-data folder behind a preference about the interface, which is
  per user and per install rather than per save, so a choice made once holds
  for every campaign afterwards. A port rather than a static reach, and one
  that fails open at both ends - a file that is absent, will not open or was
  hand-edited into nonsense answers nothing, and a write that will not land is
  reported rather than thrown, neither being worth an exception to a caller
  whose subject is a preference.
- [`starsector/strings/`](src/main/java/kmlib/starsector/strings/) - defensive
  wrapper around settings.json localisation lookups (loud REDACTED on missing
  or malformed entries), plus the number-to-copy shaping that fills their
  numeric slots.
- [`starsector/systems/`](src/main/java/kmlib/starsector/systems/) - star
  system queries and motion tracking, the per-pass index over the colony set
  so a system is walked once however many readers ask about it, and the
  register of the means of arrival the engine does not model, which the mods
  supplying them fill at load, so the read itself names no mod.
- [`starsector/systems/claims/`](src/main/java/kmlib/starsector/systems/claims/)
  - vanilla system claims behind a port, with a second port for the scored
  contest behind one - down to the terms each market's score is the sum of -
  for callers that must justify a claim rather than merely colour by it. A
  standing in that contest states its own kind: weighed, resting on the market
  the mechanic scored, or presence-only at a nought, for a faction holding
  nothing the mechanic ever reached.
- [`starsector/time/`](src/main/java/kmlib/starsector/time/) - campaign clock
  wrapper.

The UI toolkit is tiered by render substrate: a spec names content (controls,
built out of widgets' shared rows), a layout places it, `render/gl/` paints
it. How the tiers meet is in
[UI primitives, tiered by surface](src/main/java/kmlib/starsector/ui/README.md).

- [`starsector/ui/colour/`](src/main/java/kmlib/starsector/ui/colour/) -
  palette enum and Misc-backed resolver, with the accent triple a look is
  built from. See [UI Colour Palette](#ui-colour-palette).
- [`starsector/ui/controls/`](src/main/java/kmlib/starsector/ui/controls/) -
  declarative control specs and their actions: what a control is, not how it
  paints.
- [`starsector/ui/coreui/`](src/main/java/kmlib/starsector/ui/coreui/) -
  name-based reach into the game's concrete UI classes. See
  [Reaching the game's own UI classes](#reaching-the-games-own-ui-classes).
- [`starsector/ui/debug/`](src/main/java/kmlib/starsector/ui/debug/) -
  quadrant-anchored on-screen debug HUD.
- [`starsector/ui/font/`](src/main/java/kmlib/starsector/ui/font/) - the face
  enum every caller names an atlas through, the font and glyph-run caches, and
  width measurers. See [Caching](#caching).
- [`starsector/ui/highlight/`](src/main/java/kmlib/starsector/ui/highlight/) -
  highlight, paragraph and message types, rendering to text panel, tooltip,
  label and MessageIntel. See [Highlighted Text](#highlighted-text).
- [`starsector/ui/input/`](src/main/java/kmlib/starsector/ui/input/) - pointer
  and key controllers driving panel and tab-panel state (scroll, drag,
  collapse), the hover fades and press lifts its parts animate by, and the
  moments it answers audibly - detected here, with which sound each makes left
  to the look.
- [`starsector/ui/intel/`](src/main/java/kmlib/starsector/ui/intel/) -
  obf-cast seam onto the intel screen: tab open, map visor rect, that map's
  starscape flag. See
  [Reaching the game's own UI classes](#reaching-the-games-own-ui-classes).
- [`starsector/ui/label/`](src/main/java/kmlib/starsector/ui/label/) - label
  length estimation and box fitting. See [Caching](#caching).
- [`starsector/ui/layout/`](src/main/java/kmlib/starsector/ui/layout/) - pure
  placement maths: padding, anchors, strips, panel and tab-panel layout.
- [`starsector/ui/map/`](src/main/java/kmlib/starsector/ui/map/) - how
  readable a map icon is under the nebulae drawn over it, with the campaign
  map seam split into the four packages under it. See
  [Rendering environment](#rendering-environment) and
  [Reaching the game's own UI classes](#reaching-the-games-own-ui-classes).
- [`starsector/ui/map/icons/`](src/main/java/kmlib/starsector/ui/map/icons/) -
  reseating a map icon once the layering over it settles.
- [`starsector/ui/map/presence/`](src/main/java/kmlib/starsector/ui/map/presence/)
  - which campaign map surface is up and in what mode, folded across hosts,
  plus the campaign-minimap role for the surface that replaces the radar
  rather than opening as a screen.
- [`starsector/ui/map/probes/`](src/main/java/kmlib/starsector/ui/map/probes/)
  - obf-cast reads of the live map: the shown tab, its surface area, embedded
  maps, icon layering, the vanilla map tooltip, and the traces that describe
  them.
- [`starsector/ui/map/transform/`](src/main/java/kmlib/starsector/ui/map/transform/)
  - screen and world transform for the campaign map, and the modelview matrix
  readers behind it for both the GL and Fast Rendering paths.
- [`starsector/ui/render/gl/`](src/main/java/kmlib/starsector/ui/render/gl/) -
  the GL paint layer, and the drawing surface itself: fills, borders, sprites,
  scissor, labels, and the paint a caller hands them. A package per subject is
  composed over it, below. See
  [Rendering environment](#rendering-environment).
- [`starsector/ui/render/gl/controls/`](src/main/java/kmlib/starsector/ui/render/gl/controls/)
  - painting a control: checkbox, toggle, radio rows and grids, icon lists,
  segment washes and seam dividers, over per-cell hover wash and press light
  sources.
- [`starsector/ui/render/gl/panel/`](src/main/java/kmlib/starsector/ui/render/gl/panel/)
  - painting a panel: bordered box, scrollbar, and the collapse notch with its
  chevron.
- [`starsector/ui/render/gl/style/`](src/main/java/kmlib/starsector/ui/render/gl/style/)
  - the look a host hands in: box colours, accents, the hover wash and press
  light resolved to a paint at a fraction, notch colours, body font, tab style
  and sound scheme, gathered into one widget style.
- [`starsector/ui/render/gl/tabs/`](src/main/java/kmlib/starsector/ui/render/gl/tabs/)
  - painting both tab chromes, the panel they head, and centred tab labels,
  behind one chrome-selecting renderer.
- [`starsector/ui/render/gl/tooltip/`](src/main/java/kmlib/starsector/ui/render/gl/tooltip/)
  - painting a cursor tooltip and the leader lines ruling its rows.
- [`starsector/ui/screen/`](src/main/java/kmlib/starsector/ui/screen/) - the
  UI screen box and the pixel-to-UI axis conversions over it.
- [`starsector/ui/sound/`](src/main/java/kmlib/starsector/ui/sound/) - the
  engine's interface sounds a KM control answers with, the cue binding one to
  a volume, and the scheme naming what each moment sounds like - carried in
  the panel's look, so a control inherits its sound as it inherits its accent
  - and the player port behind them.
- [`starsector/ui/suppression/`](src/main/java/kmlib/starsector/ui/suppression/)
  - hiding a widget while it sits off the surface it belongs to.
- [`starsector/ui/text/`](src/main/java/kmlib/starsector/ui/text/) -
  substrate-neutral text look: the face, colour, casing and anchoring a run of
  text draws with, with each render substrate owning the adapter into its own
  anchors, plus a run's width in a settled look. See
  [Two span measurers](src/main/java/kmlib/starsector/ui/README.md#two-span-measurers).
- [`starsector/ui/tooltip/`](src/main/java/kmlib/starsector/ui/tooltip/) -
  vanilla TooltipMakerAPI helpers.
- [`starsector/ui/widgets/`](src/main/java/kmlib/starsector/ui/widgets/) -
  widget models and their geometry: one shared labelled-row core - a label
  read as one sentence with a slot to either side - and the rows and boxes
  built on it.
- [`starsector/ui/widgets/lists/`](src/main/java/kmlib/starsector/ui/widgets/lists/)
  - the spotlight picker: the sort-mode, direction and column-count model it
  ranks and wraps by, the item seam it draws rows from, and the memo a
  consumer holds its list in - holding no store of its own.
- [`starsector/ui/widgets/scroll/`](src/main/java/kmlib/starsector/ui/widgets/scroll/)
  - scroll offset state and scrollbar geometry.
- [`starsector/ui/widgets/segments/`](src/main/java/kmlib/starsector/ui/widgets/segments/)
  - splitting a row into segments and the dividers and channels between them.
- [`starsector/ui/widgets/tabs/`](src/main/java/kmlib/starsector/ui/widgets/tabs/)
  - tab strip geometry, collapse, hotkeys, and the interaction sources a tab
  header reads, split from the style a host varies.
- [`starsector/ui/widgets/tabs/style/`](src/main/java/kmlib/starsector/ui/widgets/tabs/style/)
  - the tab look a host varies: chrome, box, palette, hotkey style and text
  halo.
- [`starsector/ui/widgets/tooltip/`](src/main/java/kmlib/starsector/ui/widgets/tooltip/)
  - the cursor tooltip's rows and sections, and the spacing they stack at.

### Reaching the game's own UI classes

Three packages reach into the game's concrete UI classes, by two mechanisms
that differ in what a broken link costs the caller.

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

KMLib hosts six composite actions and the release workflow other KM mods
consume, via `uses: <owner>/KMLib/.github/actions/<name>@<tag>`. Each action
delegates to a shell script under its own `scripts/` directory, so the logic
stays unit-testable with bats-core.

Run the tests with `bats .github/tests/` from the KMLib root; they need
`bats-core` and `jq`. [ci-bash.yml](.github/workflows/ci-bash.yml) runs the
same suite on every pull request through Common-Automation's reusable bash
workflow. [action_outputs.bats](.github/tests/action_outputs.bats) covers the
seam rather than either half's logic: an `action.yml` and its script declare
and emit outputs in separate files, and a key present in only one costs a
consumer a blank string rather than an error. The Gradle build is gated
separately in [ci-gradle.yml](.github/workflows/ci-gradle.yml), which needs
Starsector binaries and so runs on the self-hosted `kmlib-runner`.

**[read-mod-info](.github/actions/read-mod-info/action.yml)** reads the
caller's `mod_info.json` and emits what every other workflow derives from it
by convention - its `outputs:` block is the statement of that convention:

- The mod id and version verbatim, the `<mod-id>-runner` label, the first
  declared jar, and the repos to clone beside the checkout.
- The shipped folder name, the `dist/<mod-folder-name>/` directory and the
  `<mod-folder-name>-<version>.zip` release name, all named after that jar
  rather than after the mod id, so a zip install and a hand-deployed one share
  one folder layout.
- `version-file-name` goes the other way, `<mod-id>.version`, because the
  hand-written `version_files.csv` points at it by that name.
- `kmlib-dependency-version`, which KMLib the caller pins or nothing when it
  declares no such dependency - how the release pipeline tells a consumer
  apart from KMLib releasing itself - and `kmlib-repo`, where that KMLib is
  published. The latter is emitted from the same constant the sibling checkout
  set is built from, so the release a pin is checked against is the repository
  the build compiles it against.

**[check-version](.github/actions/check-version/action.yml)** compares
`mod_info.json`'s `.version` to the latest git tag in the caller checkout and
emits `version` plus `version-updated`, which gates the release pipeline.

**[validate-versioning](.github/actions/validate-versioning/action.yml)** takes
a `version` and fails the release if the caller's `CHANGELOG.md` has no
`## [<version>]` section, `mod_info.json` `.version` does not equal the input,
or either that version or a declared `kmlib` pin is not well-formed
`MAJOR.MINOR.PATCH`. The shape is enforced here because every downstream
consumer of a malformed version degrades in silence rather than erroring.
Policy itself lives in [versioning](docs/dev/versioning.md).

**[check-dependency-release](.github/actions/check-dependency-release/action.yml)**
takes a `repo` and a `version`, fails unless that version exists as a published
release of that repository, and emits its `release-url`. The pipeline runs it
against a consumer's `kmlib` pin before building anything, so a pin bumped
ahead of the KMLib it names stops the release instead of publishing a mod the
game refuses to load. One API call answers both questions, and the URL is read
from the response rather than assembled from the tag, so the link the release
body carries cannot name a release nothing confirmed. An absent release and a
lookup that could not be completed both fail, with different messages: the
first is a verdict on the pin, the second explicitly is not.

**[compose-dependency-note](.github/actions/compose-dependency-note/action.yml)**
takes a `dependency-name`, `version` and `release-url`, and emits the `note`
the release body carries below its changelog section: a markdown line naming
and linking that dependency release. It is an action rather than an inline
step because the line is player-facing copy with a condition attached - an
absent version emits no line, which is the case for KMLib releasing itself,
and an absent URL emits no line either, a dropped line beating one whose link
goes nowhere.

**[fill-version-file-template](.github/actions/fill-version-file-template/action.yml)**
takes an `output-path` and a `zip-name` and writes the mod's VersionChecker
`.version` file there. The caller commits a complete
`<mod-id>.version.template` whose release-varying values are tokens -
`{{modName}}`, `{{major}}` / `{{minor}}` / `{{patch}}`,
`{{starsectorVersion}}`, `{{directDownloadURL}}` - each substituted from
`mod_info.json`, so no version number is restated by hand outside that file.

- `{{directDownloadURL}}` is the one token not read from there. It is built
  from the version, the `zip-name` input and the publishing repository the
  Actions runtime exports, so the link cannot name a repository or an asset
  other than the one being released. Both of those have a fallback for callers
  outside Actions, which is what lets the local Gradle task run this same
  script rather than a second implementation: an omitted `zip-name` is derived
  from `mod_info.json` `jars[0]` by the same lib rule `read-mod-info` uses, and
  an absent `GITHUB_REPOSITORY` falls back to the checkout's `origin` remote.
- Substitution is by whole value, which is how the version components come out
  as JSON numbers rather than quoted digits. Every other key is carried through
  untouched, so the template states the published shape, and a token left
  unsubstituted fails the release rather than shipping literal braces to
  players.
- The committed name carries the `.template` suffix because a dev install
  symlinks the repo into `mods/`: `version_files.csv` names the bare
  `<mod-id>.version`, and under one name the committed file would hand
  VersionChecker quoted tokens where it expects numbers. The bare name exists
  only where something generated it.
- `mod-root` says where to read `mod_info.json` and the template from, and
  defaults to the working directory - where a job with the mod checked out at
  the workspace root already stands. The release pipeline sets it because its
  checkout is one level down and Actions permits no `working-directory` on a
  `uses:` step. `output-path` is unaffected by it, always relative to the job,
  so a caller writing into the checkout and one writing beside it each state
  the path they would state anyway.

Four of the six read `mod_info.json`, so what that file contains and what shape
its fields take live once in
[_lib/mod_info.sh](.github/actions/_lib/mod_info.sh), which they source: the
filename, the SemVer shape, the "this field is present" check, and the names
that follow from `jars[0]` - the shipped mod folder and the release zip. The
zip name is there rather than in one script because two of them need it and
neither may guess: the pipeline uploads an asset under that name while the
version file points a download URL at it, so a rule spelled twice would give a
working link and a 404 the same spelling. The file sits under `actions/` rather
than beside it so it is where the scripts sourcing it look, each reaching it
relative to its own location; the leading underscore marks it as
not-an-action.

`mod-release.yml` names the actions in registry form
(`Klark-Morrigan/Starsector-Mod-KMLib/.github/actions/<name>@master`), which
the runner resolves without checking this repo out into the consumer's
workspace. That form accepts no token, so a consumer's release can only reach
them once this repository is public or shared for Actions use. KMLib's own
release is unaffected, a workflow always reaching its own repository.

Cutting the release itself - extracting the `## [<version>]` section for the
body and attaching the assets - is delegated to Common-Automation's
stack-agnostic `create-github-release`; only the six actions above live in
KMLib.

A release carries two assets. The mod zip is what a player downloads, with the
generated `.version` file riding inside it so an install knows which version it
is. The same file is attached in its own right because that is the only form an
update checker can reach: it polls
`releases/latest/download/<mod-id>.version` without downloading the mod, and a
copy sealed inside the zip answers nothing. GitHub excludes prereleases from
`releases/latest`, so a mod marked prerelease would publish a URL resolving to
an earlier release or to nothing - which is why nothing in this pipeline can
mark one.

## Build & Test

KMLib uses Gradle with the Java plugin. JDK 17+ must be on PATH; the
toolchain is intentionally not auto-provisioned so the project compiles
on whichever JDK is already installed.

```
./gradlew test       # JUnit 5 unit tests
./gradlew coverage   # tests + JaCoCo HTML/XML report in build/reports/
./gradlew jar        # writes jars/KMLib.jar and kmlib.version
```

The version file comes from `writeVersionFile`, which `jar` depends on. It fills
`kmlib.version.template` into `kmlib.version` at the repo root by running the same
[fill-version-file-template](.github/actions/fill-version-file-template/action.yml)
script the release pipeline runs, so a checkout symlinked into `mods/` as a dev
install reports to VersionChecker exactly what a published zip would. It needs a
bash, located on Windows from the git on `PATH`.

- Registered by
  [write-version-file.gradle](gradle/tasks/release/write-version-file.gradle),
  which the shared Starsector conventions apply, rather than by this repo's
  `build.gradle` - so every mod applying those conventions gets it, KMLib being one
  consumer of its own conventions among several. A mod opts in by committing a
  `<mod-id>.version.template`; no template, no task, which is where the KM mods
  that publish no update information stay.
- Tied to the jar rather than to `assemble`, because refreshing a dev install is
  what building the jar is: the install picks up the new jar immediately, and a
  version file left behind would report a version that is no longer there.
- Outside Actions there is no release to name the zip and no `GITHUB_REPOSITORY`,
  so the script derives the zip name from `mod_info.json` and reads the repository
  half of the download URL from the checkout's `origin` remote. A local build
  therefore cannot name a repository this clone does not push to, and the build
  restates neither rule.
- `mod_info.json`, the template and the script are its inputs, so it re-runs only
  when one of them - or the remote - changes.

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

These gates cover the YAML / Actions / Bash surface only. The Gradle build and
JUnit tests are no part of them - they run through Gradle, see
[Build & Test](#build--test).

Two delegating workflows run on every pull request:
[ci-yaml.yml](.github/workflows/ci-yaml.yml) calls Common-Automation's reusable
`ci-yaml.yml` (actionlint, action-validator, yamllint, ansible-lint), and
[ci-bash.yml](.github/workflows/ci-bash.yml) calls its reusable `ci-bash.yml`
(shellcheck, check-sh-executable, bats). Each step auto-skips when its surface
is absent, so a mod with no shell scripts still passes the Bash workflow.

Three shims in [`scripts/`](scripts/) reproduce that surface locally through Git
Bash and Docker, each a thin delegate to Common-Automation's orchestrator - so
all three need a Common-Automation checkout as a SIBLING directory
(`..\Common-Automation`). Each has a `.sh` and a `.bat` face, the latter for
`cmd` / PowerShell:

- `run-ci-yaml-and-bash` - the MAIN entry, and what most contributors run: the
  lint suite AND the bats tests in one go, the full local equivalent of
  ci-yaml.yml + ci-bash.yml.
- `run-lint-yaml-and-bash` - the lint half only (shellcheck, actionlint,
  action-validator, yamllint, ansible-lint); no bats.
- `run-tests-bash` - the bats tests only.

[`fix-permissions`](scripts/fix-permissions.sh) re-stages the executable bit on
tracked `*.sh` files, which Windows checkouts drop; run it after adding a shell
script so the `check-sh-executable` gate stays green.
[.gitattributes](.gitattributes) pins line endings surgically - `*.sh` and
`gradlew` to LF, `*.bat` and `gradlew.bat` to CRLF - and leaves binary / data
assets to git's own detection.

## Consuming KMLib

Downstream mods declare KMLib as a hard dependency in `mod_info.json`:

```json
{
  "dependencies": [
    { "id": "kmlib", "name": "Klark Morrigan's Library", "version": "0.1.0" }
  ]
}
```

The `version` is required, not decoration: the game compares it for exact
equality rather than as a minimum, and
[validate-versioning](.github/actions/validate-versioning/action.yml) holds a
consumer's pin to a well-formed SemVer at release time, checking the named
release exists before anything is built. A consumer's workflow pin
(`uses: <owner>/Starsector-Mod-KMLib/.github/workflows/mod-release.yml@<tag>`)
must name the same version, and both move in the same commit - see
[versioning](docs/dev/versioning.md).

Downstream mods pull the built jar as `compileOnly` in `build.gradle` so the file is
visible at compile time and supplied by Starsector's mod classloader at
runtime:

```groovy
compileOnly files("${configuredStarsectorRoot}/mods/KMLib/jars/KMLib.jar")
```

Tests in consuming mods that touch KMLib types also add the same jar as
`testCompileOnly` / `testRuntimeOnly`. KMLib's hard dependencies apply to every mod
that depends on it - see [Requirements](#requirements).

### Test fixtures

[`testfixtures/`](src/testFixtures/java/kmlib/testfixtures/) holds what a consuming
mod's tests stand their subjects on: fakes of KMLib's own ports (claims, fonts, the
intel screen, the modelview, console output, a console overlay up or down as a test
says), the core-UI hops and widget tree a layout rule walks, builders for the values
those ports report, the market and colony shapes a "who is here" read is posed
against, and
[`starsector/settings/`](src/testFixtures/java/kmlib/testfixtures/starsector/settings/)'s
no-op `SettingsAPI` proxy, which a test installs into `Global` before touching `Misc`
(whose static initialiser would otherwise NPE), and beside it a common-data folder that
really holds what is written into it - map-backed rather than stubbed, so a file written
under one name and read under another fails there rather than passing on two stubs that
agree, and posable as a folder that will not open or will not take a write, failing open
being the port's contract rather than an accident of it.

They are a source set of their own, published as a variant beside the jar. A consumer
takes them with `testCompileOnly testFixtures('kmlib:KMLib')`, which resolves through
the included build the same substitution already carries the main artifact over.

Three trees, three audiences: `src/main` is what the game loads, `src/testFixtures` is
what consumers' tests may take, `src/test` stays private. That boundary is the reason
for the split rather than a consequence of it - a fixture is a test artefact, and the
jar the launcher loads must not carry classes that link against a test library. It is
also what lets a fixture here mock a vanilla type, which nothing inside the shipped jar
could do. A fixture only KMLib's own suites use therefore stays in `src/test`: what
moves here is what a consumer actually asks for, so the published surface stays a
decision rather than a default.

## Console commands

KMLib registers seven commands with Console Commands, all campaign-only. They exist
because the library already holds the reads and operations behind them, so the command
is a thin front on work a consuming mod would otherwise have to expose itself.

| Command | Syntax | What it does |
| --- | --- | --- |
| `kmlib_activate_gate` | `<id>` | Activates the gate with that id in the current system. |
| `kmlib_colonise` | `[entity-id] [faction-id]` | Founds a colony on a body that so far carries only survey data, skipping the survey, the outpost cost and the proximity the survey panel asks for. |
| `kmlib_list_factions` | `[markets\|hidden\|discoverable\|no_markets]` | Lists every faction with what it holds - how many places, how many hidden, how many still to find, and the systems they sit in. |
| `kmlib_list_map_spoilers` | _no arguments_ | Lists faction-owned systems as a tree of system, entities and factions, flagging cut-off systems and undiscovered markets. |
| `kmlib_list_system_entities` | `[gates]` | Lists the current system's entities as an orbit tree, then the unorbited ones and fleets with coordinates. |
| `kmlib_spawn` | `<kind> [orbit_focus_id] [speed] [jitter=<frac>]` | Spawns a gate or a jump point at the fleet position, orbiting a focus. |
| `kmlib_transfer_market` | `[entity-id] [faction-id]` | Hands an existing colony to another owner. |

Both colony commands defer to Nexerelin's own colonisation and transfer where that mod
is enabled, through the registers in
[`markets/colonisation/`](src/main/java/kmlib/starsector/markets/colonisation/) and
[`markets/ownership/`](src/main/java/kmlib/starsector/markets/ownership/) - so an install
running Nexerelin gets a Nexerelin colony, intel entry included, rather than a
library-shaped one. See [Optional mod seams](#optional-mod-seams).

Arguments, defaults and the cases a command refuses are documented per command in
[`data/console/commands.csv`](data/console/commands.csv), which is what the console reads
and what `help <command>` prints in-game. That file is the single source for the detail;
the table above only says which command to reach for.

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

## Optional mod seams

An optional mod is reached in one of three shapes, all standing on the same presence gate, an
adapter in the mod's own package, and a facade registering it at load:

- **A routine taken over** - the mod does the whole job instead of us, and ours stands down.
- **A fact the mod publishes** - something only it can answer, held as a role the caller takes.
- **An answer that composes** - the mod adds to an answer we already have, both standing.

Which to reach for, what backs each, and why the third is not the first:
[Optional mod seams](src/main/java/kmlib/extensions/README.md).

Every mod reached any of these ways stays a soft dependency, absent from `mod_info.json`.

## Player Faction Resolution

[StarsectorPlayerFactionResolver](src/main/java/kmlib/starsector/factions/StarsectorPlayerFactionResolver.java)
centralises faction-display-name normalisation across consuming mods. The player
faction's `getDisplayName()` is always non-empty but varies by environment:
vanilla pre-first-colony reports `"Independent"`, Nexerelin's stock
`player.faction` reports the literal `"player"`, and the user can edit either to
a custom name later. Substituting the raw value into prose - "Production from a
local player settlement...", "player leader in orbit" - reads poorly before the
player has settled on an identity. Two rules share one placeholder set
(`Independent` / `player` / `Player`):

- `isPlayerFactionEstablished()` returns `true` when the display name
  is NOT in the placeholder set OR `Misc.getPlayerMarkets(false)` is
  non-empty (`false` so Nex commission / governorship markets do not
  count - those put the player under another flag, not their own).
  The OR is deliberate: requiring both signals would mis-classify both
  Nex's custom-faction-at-game-start flow and vanilla's
  keeps-Independent-through-rename flow.
  `isPlayerFactionEstablished(sector)` is the same rule with both
  signals read off a named sector - the display name off its player
  faction, the market off its own economy, applying the test
  `Misc.getPlayerMarkets(false)` applies. It exists because that helper
  is bound to `Global.getSector()`: a caller drawing anything but the
  running sector would otherwise be told about the wrong one.
- `resolveDisplayName(faction, fallback)` returns the live display
  name when populated and not in the placeholder set, else the
  caller's `fallback`. Generic - operates on any faction, not just the
  player - so host-faction-in-contested-prose, remote-management-fee
  tooltip, and any other faction-substituting surface share one policy.

The established-check itself is one rule over two inputs - the player
faction, and whether any market is player-owned - and where those come
from is a `PlayerFactionSource`. Both public forms delegate to a
package-private overload taking one: the no-arg form reads `Global` /
`Misc`, the sector-bound form reads the sector. A caller already
holding the two inputs - a unit test among them, which is how the live
reads are stubbed without `mockStatic` - passes its own.

## UI Colour Palette

[StarsectorUiColour](src/main/java/kmlib/starsector/ui/colour/StarsectorUiColour.java)
is the palette enum, in two bands that answer differently to a restyled
install. `VANILLA_`-prefixed entries are live engine reads - through `Misc`
suppliers or `settings.json` keys - so they follow a settings restyle or a
player-faction recolour without a consumer doing anything. The rest are frozen
literals that stay put. Which behaviour a callsite wants is the whole of the
choice between them, which is why the bands are kept apart rather than
interleaved by colour name.

Call `resolve()` for the live `Color`; it null-checks the supplier output and
tags the failure with the enum name, since `Misc` accessors can return null
during early engine boot. The enum's own Javadoc carries the per-entry notes
and the reason a composited shade - a fill over its backdrop, a glow added onto
one - is deliberately not an entry here.

## Highlighted Text

[Highlight](src/main/java/kmlib/starsector/ui/highlight/Highlight.java)
binds a substring to the colour it renders in. The Starsector text APIs take
two parallel arrays, substrings and colours, that are easy to drift apart at
the call site; binding them once removes the alignment risk.

[HighlightedParagraph](src/main/java/kmlib/starsector/ui/highlight/HighlightedParagraph.java)
is one line of text plus a base colour and its highlights, with render methods
for `TextPanelAPI`, `TooltipMakerAPI` and `LabelAPI`.

[HighlightedMessage](src/main/java/kmlib/starsector/ui/highlight/HighlightedMessage.java)
extends the family to the campaign side panel: an ordered list of paragraphs
whose `toMessageIntel()` maps one paragraph per `MessageIntel.addLine(...)`,
for vanilla-spaced multi-line notifications. Callers dispatch that
`MessageIntel` themselves through
`Global.getSector().getCampaignUI().addMessage(...)`, KMLib deliberately
stopping at the value type so the campaign-API call stays visible at the call
site. Icon, sound and the rest of the `MessageIntel` surface are not exposed
yet; they will be added the first time a consumer needs them.

## Intel Base Classes

[BaseTaggedIntelPlugin](src/main/java/kmlib/starsector/intel/BaseTaggedIntelPlugin.java)
is the bottom of the hierarchy: it extends vanilla's `BaseIntelPlugin` and
takes mod-defined tab tags as varargs, its `getIntelTags` override calling
`super` and mixing those in. A subclass becomes a pure declaration
(`class MyIntel : BaseTaggedIntelPlugin(MyTags.SOMETHING)`) with no
`getIntelTags` boilerplate. Zero varargs is valid and yields a pass-through,
which is what lets the expiring chain support untagged consumers.

[BaseExpiringIntelPlugin](src/main/java/kmlib/starsector/intel/BaseExpiringIntelPlugin.java)
extends it, so an expiring intel declares its tab tags through the same
constructor channel; the no-arg form is for callers pinning to a vanilla tab.
It captures the creation timestamp and auto-removes from the `IntelManager`
once `getExpiryDays()` elapses, defaulting to one Starsector month. Static
`findActive(Class)` returns the first non-expired item of a given subclass, so
synchronous callers share one definition of "still within the current window".
