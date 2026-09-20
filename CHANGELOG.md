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

- **`VerticalRadioSpec`**: a column of option cells stacked top to bottom, one lit - the shape an option set of more than two or three reads as, where the same options laid across a row letter too narrow to tell apart. Carries no segment sizing and no trailing caption, both being row-only.
- **`RadioSpec`**: the sealed interface `HorizontalRadio` and `VerticalRadio` sit under, carrying the re-pick rule both answer. A reader acting on any radio names it rather than each alignment. `HorizontalRadio` is otherwise unchanged and every existing call site compiles as it stands.
- **`ScrollingSectionSpec`**: the run of a body that scrolls, holding any controls rather than being a property of one. A heading, the list under it and the row beside it now travel together inside the viewport the capped layout leaves them, where only a single list could scroll before.
- **`Control.isScrolled()`**: whether a laid-out control was placed inside that section - the one value the clipping renderer and the viewport-limited hit-test both read. A `Control` built without it is pinned, so existing three-argument construction is unchanged.
- **`MemoryKeyAddress`, `AddressedMemoryFlag`, `AddressedMemoryString`**: a stored value held once per point on an axis the consumer declares rather than once per save. The holder states its base key and names the address each read and write means; the key is composed in one place, so no holder can spell the segments differently or drop one and quietly share a slot with another.
- **`KmlibStringKeys.get()` and `format()`**: lookups bound to KMLib's own category, so a call site names a key alone rather than repeating the category beside it.
- **`KmlibStrings.requireText()`**: the blank-rejecting counterpart of `Objects.requireNonNull`, for a component that is a name or a sentence.
- **`GlMatrix.FLOAT_COUNT`**: the sixteen floats a GL matrix takes, stated once for every reader and writer that sizes a buffer or rejects a wrong-sized array by it. `ModelviewMatrixReader.MATRIX_FLOAT_COUNT` now reads off it.
- **`Colonies.selectColonies(test)` and `Colonies.hasAnyColony(test)`**: the two walks over a colony set, stated on the set rather than hand-rolled beside it. The selection keeps the set's own order, which a consumer mirroring vanilla's tie rules settles a contest by; the emptiness read stops at the first colony that passes, being asked of every place in the sector on a scan and per frame while a map is drawn. An absent test passes nothing, which withholds rather than reporting a colony nobody asked to be shown.

#### Factions

- **`FactionAlliances`**: which factions stand together, as the alliance each allied faction belongs to, with `areFactionsAllied` over it and `buildFrom` to invert a list of records into it. Vanilla keeps no such arrangement, so this is the shape one arrives in whichever mod maintains it. It says who stands with whom and no more - what an alliance is worth is the consumer's, since keeping a secret, fighting a war and sharing a market read the same membership to different ends.
- **`AllianceRecord`**: one alliance flattened to plain data - its stable ID, its display name, and its members ranked by descending market size. Every field is a snapshot taken at read time rather than a live handle back into whatever maintains the alliance.
- **`AllianceSource`**: the port those records arrive through, so whatever folds or weighs them runs with no game around it. A port rather than a snapshot, alliances forming and dissolving in play.
- **`NexerelinAllianceSource`**: Nexerelin's live alliances as those records, behind the presence gate. The only code naming `exerelin.*` for them sits in a class of its own that the gate defers, not even reached by a method signature, so an install without the mod never seeks a Nexerelin class - and a consumer folds records without learning which mod produced them.

#### Geometry

- **`VertexWelder`**: which reports of a corner are one corner, by a tolerance, so edges computed apart can be compared as exact IDs rather than by distance at every hop. Promoted out of `EdgeRings`, where it had been private, once a second caller needed the same thing. Bucketed by a grid one tolerance across, so a lookup scans nine squares rather than the whole set, and the first report of a corner is the one kept - averaging would move a corner after edges had already been welded to it.
- **`Disk.measureSagitta(radius, segments)`**: how far the polygon approximating a disk falls inside it at its worst. The resolution anything drawn against that disk is really at, and so the figure a consumer welds by or discards small features by. It falls out of the radius and the segment count, so it is read rather than restated - four restatements of it across the consumer mods had to move together and did not. Static, taking the two: where the disk is centred has nothing to do with it, and every caller holding the two holds them as knobs rather than as a disk it could ask.
- **`Segment.readStart()` and `readEnd()`**: a segment's ends as `{x, y}` points, the one crossing between a value that names its ends by role and the point arithmetic beside it that takes arrays. Each read is its own array, so a caller keeping one as a corner cannot have it move under them.

#### Fast Rendering compatibility

A binding to Fast Rendering's bridge that stops holding now costs the map's cursor reading for the session rather than the render pass, and the player is told once, in-game, naming Fast Rendering and both versions. The channel it reports through is generic: any binding to third-party code records into it. [`starsector/compatibility/`](src/main/java/kmlib/starsector/compatibility/README.md) sets out how.

- **Built-against version stamped into the jar**: the build generates the Fast Rendering release the bridge adapter was type-checked against into the jar as a constant, so a mismatch report can name both halves of the disagreement. A build against the bridge stubs stamps no version.
- **`CompatibilitySubject`** and **`CompatibilityFailure`**: the third party a report is about with both versions, and one binding to it that stopped holding. An unread version renders as an explicit unknown rather than as `null`.
- **`CompatibilityConsumer`**: the mod that took a binding, as the key its records latch under and the sentence naming what it loses. That sentence is the taking mod's, never the library's.
- **`CompatibilityFailures`**: the session's record, latched per third party and consuming mod, written from any thread and drained by a reporter. The failure is described through a supplier invoked only on the record that is kept, so a reflective probe or a version read is paid once per binding rather than once per frame. Handed over one at a time, since a reporter can only show one at a time, and taken only once a frame can show it - so nothing is ever held outside the record.
  - **`CompatibilityFailures.SESSION_RECORD`**: the one record every binding writes into, held per session rather than per sector. `KMLib_ModPlugin` installs the notice that drains it on every game load.
- **`CompatibilityNotice`**: the transient per-frame script that drains that record and shows each failure as the game's own message dialog, writing the report line before asking for the dialog. One dialog per frame.
- **`UnavailableModelviewMatrixReader`**: the third `ModelviewMatrixReader`, whose every read is no reading. Not a new caller contract: `CampaignMapTransform` already parks on an absent reading.
- **`FastRenderingBridgeDiagnostic`**: once a bridge binding has failed, which of the six mirrored members no longer hold and why, beside the version the installed jar reports and the one the build was type-checked against. Never runs on the healthy path.
- **Guarded binding in `ModelviewMatrixReaders`**: the Fast Rendering branch is taken under a `LinkageError` guard, which covers a class that moved, a member that moved and a signature that changed in one catch. A binding that no longer links now degrades to `UnavailableModelviewMatrixReader` and records one failure, where before it threw out of whichever render pass reached it first - killing the pass and naming KM classes in a trace the player then blamed KM for. The choice is held for the session, so a broken binding probes and records once rather than once a frame.
- **`FastRendering.COMPATIBILITY_SUBJECT_KEY`** and **`COMPATIBILITY_SUBJECT_NAME`**: the identity a failed binding to this renderer is recorded under and the name a report shows for it, published so that every binder spells them the same. Two binders spelling the key differently would turn one renderer into two subjects and put two modals in front of a player over one mismatch.
- **A bridge that breaks where it is called degrades too, not only where it is linked**: a Fast Rendering entry point that throws once the binding has linked used to reach the player as a fatal error on the frame after, from a stack with no KM frame on it to blame. It now costs the map's cursor reading alone: the reading latches unavailable for the session, no stale matrix is reported in its place, and one failure is recorded however many frames the map stays open. Both sides of a binding compose their report the same way, so a player is told one thing about one renderer whichever side noticed. The mechanism is set out in [docs/dev/rendering-environment.md](docs/dev/rendering-environment.md).
- **Each mod over a broken binding is told what it lost**: `ModelviewMatrixReaders.selectForActiveRenderer()` resolves per consumer, so a second mod reading the map is reported to rather than handed the first one's binding. Before this the first caller's choice was held for the session, and every later mod's player was told nothing.

#### Control rows

- **`InteractiveSpec.isSegmented()`** and **`reselectBehaviour()`**: what a control answers about itself, replacing two chains of type tests that each worked it out from outside. Whether a control's cells are hit separately and what a re-pick of a lit cell does are each one rule with two readers, the hit-test that resolves a cell and the narrowing that decides whether pressing it acts; stated on either side, a control would be hit as a row of segments and pressed as a whole row, or the other way about. Every interactive variant now answers both, so neither can be forgotten for one.
- **`RowDimensions`**: each row's height beside its width, as one value. Handed over as two lists they could arrive from different readings, two lists of different lengths or the heights of a run the widths were never measured from, and a stacker had no way to notice. Held together they are checked against each other where they are stated.
  - **`ControlStripLayout.StripMeasurement.rowDimensions()`** answers a measurement's rows in that shape.

### Test fixtures

- **List widget fixtures**: `Anomaly` and `AnomalySortMode`, a picker row and a sort vocabulary declared outside the list package, and `ListPickerBlockReads`, which reaches into a built picker block for the columns selector, the sort row, the sort selector or the item list. The block's order lives there rather than in each suite that tests a list, so a row inserted into it breaks one file.
- **`MemoryKeyAddresses`**: two stand-in addresses, for suites storing a value at one point on an axis without being about what the axis is.
- **`CompatibilityFailureFixture`** and **`CompatibilitySlotTemplates`**: one representative compatibility failure with a builder per slot a case varies, plus the two subject keys and the two consumers a suite records under - each consumer losing something the other does not, so a case about two of them being told apart cannot pass on one sentence standing for both - and the notice's templates as stand-ins that expose their slots, so a suite about the record, the notice or the wording names the one slot it is about.
- **`ShippedStrings`**: reads a mod's shipped `data/strings/strings.json` and the string IDs its holder class names, for the guard that holds those two together. The walk lives here rather than in each mod's suite, where a regex that stopped matching some entries would leave both directions passing over less of the file than they claim.

### Changed

- **`VoronoiCellBuilder` lays the corners its cells share.** Where a neighbour's border meets the radius bound, both cells now put that corner in the same place, worked out from the two sites and the reach rather than from either cell's own seed - so they agree exactly rather than to a tolerance. The seed is a polygon inscribed in the bound, and its flat sides used to move such a corner, or at a coarse segment count cut it away altogether; a consumer chaining adjacent cells' frontier edges into one outline found a gap at every one of those, wide enough at a low count to leak one enclosed region into the next. A corner that is cut away is now put back, by breaking the span it should have stood on and running the boundary through it.

  The segment count therefore decides how smoothly an arc is drawn and nothing else. The corners a cell offers are the same at any count, which is what anything laid against a cell needs. Where three cells meet inside the bound their shared corner is one corner, not two with an empty span between them.

  Cell geometry moves by up to a chord's sagitta at those corners - about eight units at the shipped reach and default count, and four times that at half the count. Consumers asserting cell vertices to tighter than that will need re-baselining.

### Public contracts changed (**breaking**)

- `ModelviewMatrixReaders.selectForActiveRenderer()` takes the `CompatibilityConsumer` taking the reading; the no-argument form is gone. A caller now names the key its records latch under and the sentence naming what it loses, both read only where a binding fails. The sentence is the caller's because what a failed binding costs is knowledge of the feature built over the reading: the library knows the renderer, both versions and the member that moved, and nothing about what was drawn with it.
- `FastRenderingModelviewMatrixReader` is no longer an enum, and its `INSTANCE` is gone: `ModelviewMatrixReaders` now builds one where it takes the binding, for the consumer that asked. The reader records the failures it meets at call time, and one that could not say which mod it serves could record them against nobody - what a broken binding costs being the taking mod's to state. That one map is on screen at a time is a fact about the map rather than about how many mods draw over it. Nothing outside the package could construct one either way, the class being reachable only through the selection.
- `VerticalTableSpec` loses its `scrolls` component and its `asScrolling()` refinement; what scrolls is stated by the `ScrollingSection` a host puts a run inside. A host that marked its list now wraps it: `new ScrollingSectionSpec(List.of(list))`. The table's canonical constructor takes seven arguments where it took eight.
- The `ControlSpec` variants are top-level types in a new `kmlib.starsector.ui.controls.specs` package, one file per variant, each suffixed `Spec`: `ControlSpec.Checkbox` is now `CheckboxSpec`, `ControlSpec.Interactive` is `InteractiveSpec`, and so on for all twelve. Nested in one 878-line file they could not be opened, reviewed or blamed apart, and a variant's name could not be read without its enclosing type. The whole family moves together because a sealed type and its permitted variants must share a package, and `ControlAction`, `ControlHoverReport`, `RadioAlignment`, `ReselectBehaviour`, `RowGeometry` and `SegmentSizing` move with them so the new package depends on nothing in the old one. A consumer changes its imports and drops the `ControlSpec.` prefix; nothing else about a spec changed.
- `RowStack.layoutRows()` takes a `RowDimensions` in place of the per-row heights and widths as two adjacent lists, and the uniform-height overload is gone. The two overloads took five arguments each and both opened with three floats, where the third was a row height in one and the gap in the other, so a call site read the same whichever was meant. A run whose rows share a height is now `RowDimensions.createUniform(rowHeight, rowWidths)`.
- `ControlStripLayout.layoutControls()` takes the `StripMeasurement` the rows were measured as, in place of its `rowHeights` and `rowWidths` lists. They are one reading of one strip, and parted they could arrive from different readings - two lists of different lengths, or the heights of a strip the widths were never measured from - neither of which the placement could notice.

## [0.4.0] - 2026-09-15

### Fixed

- **Release pipeline** ran on pushes to master carrying no version bump, and didn't detect version numbers going down.

### Added

- **Release gate**: a `CHANGELOG.md` carrying an `## Index` must list the version being released, so an index link cannot resolve to nothing. Changelogs with no index are unaffected.

### Public contracts changed (**breaking**)

- `check-version` takes a required `version` input and no longer emits `version`. It asks git about the version it is given rather than reading `mod_info.json` itself, so the gate and the rest of the pipeline cannot rule on different strings. Callers of the reusable `mod-release.yml` need no change; a workflow calling the action directly must now pass `version`.
- `Hatching.computeHatchRun()` takes a `HatchPattern` - the new record carrying the spacing, angle and join tolerance a cut is made to - in place of those three loose doubles. A caller can now also hold what shapes its hatch geometry apart from how it strokes the result, and cache against it.

## [0.3.1] - 2026-09-15

### Dependency changes

- Reflection utils are lifted from **MagicLib** per **Numan**'s recommendation. Scoped to **coreui** package.
- **MagicLib** dependency is removed.
- The project is relicenced under under **LGPL-3.0-only** to comply with licencing of donor code.

## [0.3.0] - 2026-09-15

### Fixed

- **Crash on Linux**. EventsPanel.getMap() returns an obfuscated type that isn't the same on different platforms. - Reported at **USC** by **Elia Rowan (zinzrinz)** and **MattTheMatt2**, localised and fix suggested by **WolframSegler**.
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
