# Changelog

All notable changes to KMLib are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project adheres to [Semantic Versioning](https://semver.org/). Versioning triggers for KMLib and its consumer mods are defined in [docs/dev/versioning.md](docs/dev/versioning.md).

The reusable release workflow extracts the section matching the released version into the GitHub release body, so every released version must have a section here.

## Index

- [Unreleased](#unreleased)
- [0.4.0](#040---2026-09-15)
- [0.3.1](#031---2026-09-15)
- [0.3.0](#030---2026-09-15)
- [0.2.0](#020---2026-09-14)
- [0.1.0](#010---2026-09-14)

## [Unreleased]

### Added

- **`ControlSpec.VerticalRadio`**: a column of option cells stacked top to bottom, one lit - the shape an option set of more than two or three reads as, where the same options laid across a row letter too narrow to tell apart. Carries no segment sizing and no trailing caption, both being row-only.
- **`ControlSpec.Radio`**: the sealed interface `HorizontalRadio` and `VerticalRadio` sit under, carrying the re-pick rule both answer. A reader acting on any radio names it rather than each alignment. `HorizontalRadio` is otherwise unchanged and every existing call site compiles as it stands.

## [0.4.0] - 2026-09-15

### Fixed

- **Release pipeline** ran on pushes to master carrying no version bump, and didn't detect version numbers going down.

### Added

- **Release gate**: a `CHANGELOG.md` carrying an `## Index` must list the version being released, so an index link cannot resolve to nothing. Changelogs with no index are unaffected.

### Public contracts changed (**breaking**)

- `check-version` takes a required `version` input and no longer emits `version`. It asks git about the version it is given rather than reading `mod_info.json` itself, so the gate and the rest of the pipeline cannot rule on different strings. Callers of the reusable `mod-release.yml` need no change; a workflow calling the action directly must now pass `version`.
- `Hatching.computeHatchRun` takes a `HatchPattern` - the new record carrying the spacing, angle and join tolerance a cut is made to - in place of those three loose doubles. A caller can now also hold what shapes its hatch geometry apart from how it strokes the result, and cache against it.

## [0.3.1] - 2026-09-15

### Dependency changes

- Reflection utils are lifted from **MagicLib** per **Numan**'s recommendation. Scoped to **coreui** package.
- **MagicLib** dependency is removed.
- The project is relicenced under under **LGPL-3.0-only** to comply with licencing of donor code.

## [0.3.0] - 2026-09-15

### Fixed

- **Crash on Linux**. EventsPanel.getMap() returns an obfuscated type that isn't the same on different platforms. - reported at **USC** by **Elia Rowan (zinzrinz)** and **MattTheMatt2**, localised and fix suggested by **WolframSegler**.
- **Crash**. **Starscape** Map terrain reseat failure on a mismatched widget signature in now handled and logged, resulting in terrain reseating standing down for the rest of the section.
- **Altered map render state**. Failed terrain reseating now restores reseated terrain placement before standing down.
- **Duplicate map entity**. A location that refuses to give an entity up no longer leaves the reseat owing a put-back for an entity that never left, which added a second copy of it on the next advance.

### Added

- `IntelScreenView.readMapVisorState()` and `MapVisorState` - the visor's presence and its Starscape filter as one reading. Asking `getMapVisorRect()` and `isMapStarscapeModeOn()` in turn answers the same question and walks the live widget tree twice to do it, which `MapPresence` was paying for on every frame of a campaign.
- `MapIconOrderWidgetFake` - a test fixture standing for the map widget's icon order, so a rule about where an icon sits can be driven without a running game.

### Public contracts changed (**breaking**)

- `MapIconReseater`'s third constructor argument is now `Function<SectorEntityToken, MapIconLayering>` - a read asked about an entity - where it was `Supplier<MapIconLayering>`, a reading that a caller had to keep aimed at the same entity as the second argument with nothing checking that it was. Callers pass the placement read itself (`MapIconLayeringProbe::readLayeringOf`) rather than a closure over their own entity lookup.

## [0.2.0] - 2026-09-14

### Added

- `Ranges.clampInto(int, int, int)` - the integer form of the existing clamp, on the same terms as the double one and including its empty-range rule. A caller whose value and bounds are all integers - a pixel width, a count, a cadence in seconds - had to cast at each end of the double-only form, which is why such callers inlined a `min`/`max` pair instead and left the one clamp unread. The new form is answered by the double one rather than by a second copy of the rule, so the empty case cannot drift between them.

## [0.1.0] - 2026-09-14

First tagged release, so there is no prior version to diff against: this is the whole public surface - the commands a player types, the packages a consumer imports, and the test fixtures that ship as a second artifact for consumers' own suites.

### Added

#### Console commands

Seven campaign-only commands, registered through `data/console/commands.csv` and reached only where Console Commands is installed. Both colony commands defer to Nexerelin's own colonisation and transfer where that mod is enabled.

- `kmlib_activate_gate`
- `kmlib_colonise`
- `kmlib_list_factions`
- `kmlib_list_map_spoilers`
- `kmlib_list_system_entities`
- `kmlib_spawn`
- `kmlib_transfer_market`

#### Game-agnostic helpers

No Starsector API on the signature.

- `kmlib.animation`
- `kmlib.collections`
- `kmlib.colour`
- `kmlib.extensions`
- `kmlib.input`
- `kmlib.logging`
- `kmlib.math.easing`
- `kmlib.math.geometry`
- `kmlib.math.hashing`
- `kmlib.math.motion`
- `kmlib.math.random`
- `kmlib.math.ranges`
- `kmlib.math.solving`
- `kmlib.opengl`
- `kmlib.opengl.hatch`
- `kmlib.profiling`
- `kmlib.profiling.budget`
- `kmlib.profiling.recording`
- `kmlib.profiling.report`
- `kmlib.profiling.snapshot`
- `kmlib.settings`
- `kmlib.text`
- `kmlib.time`

#### Starsector-facing wrappers and seams

- `kmlib` - the mod plugin the launcher loads, rather than a package a consumer imports
- `kmlib.mods.console`
- `kmlib.mods.console.commands`
- `kmlib.mods.console.commands.input`
- `kmlib.mods.console.commands.output`
- `kmlib.mods.console.commands.parsing`
- `kmlib.mods.console.commands.targets`
- `kmlib.mods.console.commands.validation`
- `kmlib.mods.nexerelin`
- `kmlib.mods.rat`
- `kmlib.starsector`
- `kmlib.starsector.entities`
- `kmlib.starsector.factions`
- `kmlib.starsector.factions.relation`
- `kmlib.starsector.fleet`
- `kmlib.starsector.geometry`
- `kmlib.starsector.graphics`
- `kmlib.starsector.intel`
- `kmlib.starsector.listeners`
- `kmlib.starsector.map`
- `kmlib.starsector.markets`
- `kmlib.starsector.markets.colonies`
- `kmlib.starsector.markets.colonisation`
- `kmlib.starsector.markets.ownership`
- `kmlib.starsector.memory`
- `kmlib.starsector.scripts`
- `kmlib.starsector.settings`
- `kmlib.starsector.settings.modmanager`
- `kmlib.starsector.strings`
- `kmlib.starsector.systems`
- `kmlib.starsector.systems.claims`
- `kmlib.starsector.time`
- `kmlib.starsector.ui.buttons`
- `kmlib.starsector.ui.colour`
- `kmlib.starsector.ui.controls`
- `kmlib.starsector.ui.coreui`
- `kmlib.starsector.ui.debug`
- `kmlib.starsector.ui.font`
- `kmlib.starsector.ui.highlight`
- `kmlib.starsector.ui.input`
- `kmlib.starsector.ui.intel`
- `kmlib.starsector.ui.label`
- `kmlib.starsector.ui.layout`
- `kmlib.starsector.ui.map`
- `kmlib.starsector.ui.map.controls`
- `kmlib.starsector.ui.map.icons`
- `kmlib.starsector.ui.map.presence`
- `kmlib.starsector.ui.map.probes`
- `kmlib.starsector.ui.map.transform`
- `kmlib.starsector.ui.render.gl`
- `kmlib.starsector.ui.render.gl.controls`
- `kmlib.starsector.ui.render.gl.panel`
- `kmlib.starsector.ui.render.gl.style`
- `kmlib.starsector.ui.render.gl.tabs`
- `kmlib.starsector.ui.render.gl.tooltip`
- `kmlib.starsector.ui.screen`
- `kmlib.starsector.ui.sound`
- `kmlib.starsector.ui.suppression`
- `kmlib.starsector.ui.text`
- `kmlib.starsector.ui.tooltip`
- `kmlib.starsector.ui.widgets`
- `kmlib.starsector.ui.widgets.lists`
- `kmlib.starsector.ui.widgets.scroll`
- `kmlib.starsector.ui.widgets.segments`
- `kmlib.starsector.ui.widgets.tabs`
- `kmlib.starsector.ui.widgets.tabs.style`
- `kmlib.starsector.ui.widgets.tooltip`

#### Test fixtures

Shipped as a second artifact beside the jar, for a consumer's own suites. Fakes stand in for a seam the library inverted; fixtures build a world a case is posed against.

- `kmlib.testfixtures.logging`
- `kmlib.testfixtures.mods.console`
- `kmlib.testfixtures.mods.console.commands.output`
- `kmlib.testfixtures.profiling`
- `kmlib.testfixtures.starsector`
- `kmlib.testfixtures.starsector.listeners`
- `kmlib.testfixtures.starsector.markets`
- `kmlib.testfixtures.starsector.markets.colonies`
- `kmlib.testfixtures.starsector.memory`
- `kmlib.testfixtures.starsector.settings`
- `kmlib.testfixtures.starsector.systems`
- `kmlib.testfixtures.starsector.systems.claims`
- `kmlib.testfixtures.starsector.ui.coreui`
- `kmlib.testfixtures.starsector.ui.font`
- `kmlib.testfixtures.starsector.ui.input`
- `kmlib.testfixtures.starsector.ui.intel`
- `kmlib.testfixtures.starsector.ui.label`
- `kmlib.testfixtures.starsector.ui.layout`
- `kmlib.testfixtures.starsector.ui.map`
- `kmlib.testfixtures.starsector.ui.map.controls`
- `kmlib.testfixtures.starsector.ui.map.presence`
- `kmlib.testfixtures.starsector.ui.map.probes`
- `kmlib.testfixtures.starsector.ui.map.transform`
- `kmlib.testfixtures.starsector.ui.sound`
