# Changelog

All notable changes to KMLib are documented here. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project adheres to [Semantic Versioning](https://semver.org/). Versioning triggers for KMLib and its consumer mods are defined in [docs/dev/versioning.md](docs/dev/versioning.md).

The reusable release workflow extracts the section matching the released version into the GitHub release body, so every released version must have a section here.

## Index

- [Unreleased](#unreleased)
- [0.5.1](#051---2026-10-06)
- [0.5.0](#050---2026-10-05)
- [0.4.0](#040---2026-09-15)
- [0.3.1](#031---2026-09-15)
- [0.3.0](#030---2026-09-15)
- [0.2.0](#020---2026-09-14)
- [0.1.0](#010---2026-09-14)

## [Unreleased]

## [0.5.1] - 2026-10-06

### Fixed

- **[KMU] Occasional drawing of every map label letter as a solid filled rectangle** caused by failed load of the map label font on an install with Fast Rendering and VRAM Optimiser. _Reported by **lChronosl** [at **USC**](https://discord.com/channels/187635036525166592/1549091275167240272/1556768476436111471)._
- **JSON fields carrying object-holding arrays in `mod_info.json` are put at the bottom of the file**. There are third-party mod installers that naively read the first occurrence of the `id` field whether it's at the root or at records nested under `dependencies`.

### For developers

<details>
<summary>API, build tooling and test fixtures</summary>

#### Fixed

- **`writeLocaleFiles` writes the members of `mod_info.json` that hold objects last**, after the sorted rest.

#### Changed

- **`mod-release.yml` re-runs every PR gate before it packages**: `ci-yaml` runs beside `ci-bash` and `ci-gradle`, so a YAML lint error that fails a pull request also fails the release.
- **`mod-release.yml` packages with the JDK on the mod's runner**, the one `ci-gradle` tests with, rather than downloading a Temurin JDK on every release.

</details>

## [0.5.0] - 2026-10-05

### Fixed

- **A crash under Fast Rendering `0.9.0` and later.** Thanks to **Genir**, [Fast Rendering now implements the missing OpenGL method](https://github.com/Halke1986/starsector-render/issues/11), the one the game itself answers when Fast Rendering is not installed. Under Fast Rendering, the map follows the cursor only from `v0.9.1rc1` on.
- **Map layers drawn through KMLib stay above the nebulae with the campaign speed-up on.** Every layer stayed under the nebulae on every map open, and neither a save reload nor a Starscape toggle cleared it. - Reported by **MiniRockytheOracle** [at **USC**](https://discord.com/channels/187635036525166592/1549091275167240272/1551829173037957170).
  - **A layering fix that gave up is tried again on the next map open**, rather than staying off for the rest of the session.
- **A shortcut key shown after a button's words stays on the button.** In a translation whose words lack the key's letter, or in English with the key rebound, a key such as `[M]` wrapped onto a second line outside the button.

### Added

- **Simplified Chinese (简体中文).** A second zip, `KMLib-<version>-zh-hans.zip`, carries the compatibility notice, the settings tab and the mod list entry in Simplified Chinese. Install the [Chinese localisation](https://github.com/TruthOriginem/Starsector-Localization-CN) over `starsector-core` first: the game's own fonts hold no Chinese characters, so without it every one draws as `?`. The Log verbosity options stay in English, so the setting carries over between the two zips.
- **An in-game notice says when a mod using KMLib cannot integrate with another mod, Fast Rendering or the game itself.** It names the mod, both versions, what stops working and what does not, and whether to update, downgrade or wait. The failure costs only the feature built on that integration, and each one shows once per session.
  - **That mod's own feature that fails and is switched off is reported the same way**, asking for a report to the mod's developer.
  - **A failure found while a map is open can show over the map**, without waiting for the campaign screen.

### For developers

<details>
<summary>API, build tooling and test fixtures</summary>

#### Fixed

- **Fast Rendering crash:** the map's modelview is read through `glGetFloat(GL_MODELVIEW_MATRIX)`, which `v0.9.1rc1` answers inline. KMLib compiles against no part of `fr.jar`, so a refactor inside Fast Rendering cannot break the read.
- **Map layers under the speed-up:** `MapIconReseater` puts an icon back only once the widget has dropped it, bounded by `MAX_ADVANCES_DETACHED` advances and ended when the map closes. The speed-up advances scripts several times a frame, so a removal and a put-back landed in one frame and the widget never re-layered the icon.
- **Shortcut key on a button:** `VanillaButtonLabel.announceShortcut()` widens the label to the words it carries. The game sizes a label once, to the words it was built with, and wraps anything longer.
- **`GlRuns`, `GlLines`, `GlQuads` and `GlTriangles` call `glEnd` in a `finally` block.** A draw that threw left the pipeline inside `glBegin`, spoiling the GL state of everything drawn after it.
- **A `LunaSettingsReader.runOnSettingsChange` callback that throws is logged at error with its trace.** LunaLib reports it only at debug, without the trace. The next change is tried as usual, and the log-level binding uses the same relay.
- **A failed reach into the game's screens is caught at its boundary** instead of ending the frame. Both ways it fails are caught: the game's own failure thrown undeclared, and a `LinkageError` from a member a new game build dropped.
  - `ShownMapTab.isMapTabShowing()`: the screen check the notice panel and any panel over a map make.
  - `CoreUiTree.readHopIfOffered`: asks for the method name inside the catch that answers null.
  - `CampaignMapView`: answers the map as not showing, with one warning.
  - `VanillaIntelScreenView`: answers no visor where the intel panel's fader will not link.
- **Switching a diagnostic on re-arms every class-wide warning about a reach into the game's widgets**: the filter-row control's, its label's and `CampaignMapView`'s as well as the map probes'.
- **`HighlightedParagraph` hands its runs to the game in text order**, so a translation may number its slots in any order. A run named twice takes its next occurrence each time, and one the text lacks goes last.

#### Added

##### Compatibility

The API behind the in-game notice. [`starsector/compatibility/`](src/main/java/kmlib/starsector/compatibility/README.md) sets out how it works and why.

- **`CompatibilityFailures`**: the session's record of failures, latched per third party, consuming mod and lost-feature sentence. Any thread writes it, and a reporter drains it one failure at a time.
  - `CompatibilityFailures.SESSION_RECORD`: the one record every binding writes into. `KMLib_ModPlugin` installs the notice that drains it.
  - `recordFeatureFailureOnce()`: records a mod's own failed feature as a `FeatureFailure`.
  - A mod that reuses one feature key for two features has both reported, the second under `<feature>-2`.
- **`ReportedFailure`**: the sealed type the notice draws.
  - `CompatibilityFailure`: one binding to a third party that stopped holding. It composes the notice's heading, diagnosis, rows and closing line as `CompatibilityNoticeLine`s.
  - `FeatureFailure`: a mod's own feature that threw and was switched off.
- **What a report is made of**:
  - `CompatibilitySubject`: the third party, with the targeted and installed versions and how they compare.
  - `CompatibilityBreakage`: which guard caught a binding and what no longer holds.
  - `CompatibilityConsumer`: the mod that took the binding, the feature it serves, and the sentences naming what it loses and what it does not. `resolveConsumerAtPosition()` numbers a reused feature key.
- **Start-up wiring**:
  - `WiringSteps`: runs one start-up step behind a guard, in `starsector/startup/`. A step that throws or fails to link costs only its own registration.
  - `ModIntegration`: the third-party mod a step binds to, and what the wiring mod loses without it.
  - `KMLib_ModPlugin` reports its LunaLib, Nexerelin and Random Assortment of Things wiring through the channel, under KMLib's own mod ID.
- **Integrations that break on first call**: an optional-mod adapter that fails at a founding, a hand-over, a counters decision, a reachability read or an alliance read is taken out for the session and reported once.
  - `IntegrationFailureReporter`: files that failure from whichever boundary caught it, never throwing.
  - `ExtensionPoint.offerWork()`: offers work to the installed implementation inside that boundary.
  - `describeIntegration()` on `NexerelinIntegration` and `RandomAssortmentOfThingsIntegration`, and `KmlibLunaSettings.describeLunaLibIntegration()`: each integration's report, beside the code that binds it.
- **Reaches into the game's own code**: the widget walks and class reads behind the map, the intel screen, the filter row, overlays, the codex, tooltips and icon order report through the mod that loses something, with the game as the third party.
  - `GameReachReporter`: one mod's reporter. `UNREPORTED` files nothing, for diagnostic reads.
  - `InstalledMods.readModGameVersion()`: the game version a mod declares.
- **Fast Rendering's refused read**: a release before `v0.9.1rc1` throws on the modelview read. `FastRenderingModelviewMatrixReader` catches it and records one failure; [docs/dev/rendering-environment.md](docs/dev/rendering-environment.md) sets out the mechanism.
  - `FastRendering.FIRST_MODELVIEW_READ_RELEASE` and `readInstalledVersion()`: the two versions a refused read is reported between.
  - `FastRendering.COMPATIBILITY_SUBJECT_KEY` and `COMPATIBILITY_SUBJECT_NAME`: the renderer's identity in a report.
  - `UnavailableModelviewMatrixReader`: the reader whose every read is no reading.
  - `ModelviewMatrixReaders.selectForActiveRenderer()` resolves per consumer, so each mod over a refused read is told what it lost.
- **Notice surfaces**:
  - `CompatibilityNotice`: shows each failure as the game's own one-button confirm dialog.
  - `ScreenCompatibilityNotices`: shows a failure found on a map screen there, in a `CompatibilityNoticePanel`.
  - `CompatibilityNoticePanel`: stands in the core UI's widget tree, fades like the game's own prompts, and closes on the button, escape, enter or space.
  - `ShownMapTab.isMapTabShowing()`: whether a map is on screen, failing closed. The panel comes down with the map through it.
  - `ModalOverlays`: the overlays KMLib holds a core screen with, in `starsector/ui/coreui/`. `CoreUiDialogView` reads it beside the game's own modals.
  - `OverlayPresence.isShowing()`: whether any of an overlay is on screen, fading included.
  - Emphasis in the notice comes from the values filled into its templates, never from parsing its wording.
- **Mod identities**, each beside the code that binds to it:
  - `KmlibMod.MOD_ID`: KMLib's own ID, readable without loading its plugin class.
  - `NexerelinPresence.MOD_NAME` and `RandomAssortmentOfThingsPresence.MOD_NAME`.
  - `KmlibLunaSettings.LUNALIB_MOD_ID` and `LUNALIB_MOD_NAME`.
  - `InstalledMods.readModName()` and `readModVersion()`: a mod's display name and declared version, as the game holds them.

##### Fonts

Text falls back to a face that holds every character of it, so a localised install draws its script rather than `?`.

- **`FaceResolver`**: picks the face a text draws in. It keeps the face asked for where its atlas holds every character, and otherwise walks down to the first face that does.
  - `StarsectorFont.resolveLowerResolutionFont()`: the next smaller cut of a face's family, one step of that walk.
  - `FaceResolver.LAST_RESORT_FONT`: `insignia15LTaa`, answered where nothing on the walk draws the text.
  - `listFallbackWalk()`: the faces a text is tried in, in order.
  - `InstalledFaces.createFaceResolver()`: the resolver over the running game's atlases.
- **`SettledFaceMemo`**: the face each kind of text settles on, resolved once and held until `discardFaces()`. Hold one per sector rather than resolving per paint.
  - `createUnsettled()`: keeps every face as asked, for a caller with no texts to read.
  - `InstalledFaces.createFaceMemo(reader)`: the memo over the running game's atlases.
- **Text readers** to settle a face against.
  - `SectorStarSystems.listSystemNames(sector)` and `SectorMarkets.listMarketNames(sector)`: every star system's and market's name, discovered or not.
  - `StarsectorStrings.listCategoryStrings(category)`: one category of `data/strings/strings.json`, as the game merges it across enabled mods.
  - `KmlibStrings.collectTexts(items, readText)`: each item's text in order, nulls and blanks left out.
- **`FontAtlas`**: the sealed type over the atlases text draws in - a `StarsectorFont` KM names, or a `DeclaredFontAtlas`.
  - `DeclaredFontAtlas`: the atlas the game's `defaultFont` setting names, where no `StarsectorFont` does.
  - `AtlasSmoothing.resolveFromInfoLine()`: an atlas's smoothing, read off its descriptor's first line.
- **`StarsectorFont`**:
  - `getBasename()`: the atlas's basename, how a log line names a face.
  - `findFontByPath()`: the face at an atlas path, if the enum names one.
  - `VANILLA_INSIGNIA_21` and `VANILLA_INSIGNIA_25`: the body face's middle and larger cuts, the steps down from `insignia42LTaa`.
- **`GameDefaultFontReader`**: the game's own `defaultFont`, read the way vanilla's `Fonts.DEFAULT_SMALL` is, as a `StarsectorFont` or a `DeclaredFontAtlas`.
- **Installed-atlas readers**: ports that ask the loaded atlas, since a localisation replaces atlases under vanilla's file names.
  - `FaceLineHeightReader`: the line height an atlas draws 1:1 at; `isFaceLoadable()` says whether it loaded.
  - `LazyFontLineHeightReader`: its live adapter, answering `NO_LINE_HEIGHT` for a face that will not load.
  - `GlyphCoverageReader`: whether an atlas draws every character of a text as itself rather than as `?`.
  - `LazyFontGlyphCoverageReader`: its live adapter.
- **Native faces**: a face at its atlas's own 1:1 size, for a caller with no size of its own.
  - `TextFace.createNativeFace(atlas, lineHeights)`: from line heights the caller holds.
  - `InstalledFaces.createNativeFace(atlas)`: sized off the running game's install.
- **`InstalledFaceCheck`**: loads every face once at start-up and logs each one's line height, or that it would not load. `KMLib_ModPlugin` runs it.
- **Font build gates**: every build needs the network and the game's fonts.
  - `test` checks every face on the build's install, on each localisation edition the lock pins and on any install `-PfontInstallRoots` names.
  - `checkFontEditions` fails the build when an edition publishes new descriptors, against the SHAs in `font-editions.lock.json`; `writeFontEditionsLock` updates the lock.

##### Factions

- **Alliances**: which factions stand together, from whichever mod keeps them. Vanilla keeps none.
  - `FactionAlliances`: the alliance each allied faction belongs to, with `areFactionsAllied` and `buildFrom`.
  - `AllianceRecord`: one alliance as plain data: ID, display name, and members by descending market size.
  - `AllianceSource`: the port the records arrive through.
  - `NexerelinAllianceSource`: Nexerelin's live alliances, behind the presence gate.
- **`FactionNames`** and **`FactionNameForm`**: a faction's authored names, read in one place.
  - `resolveName`: the short or the long name, as authored.
  - `resolveFullestName`: the long name, or the short one where the long is blank.
  - `listEveryName`: every faction's names, each text once.
- **`StarsectorFactionColours.findPalette()`**: a faction's bright and dark pair with no grey fallback, null where the faction is absent. `resolvePalette` falls back through it.

##### Geometry

- **`VertexWelder`**: merges reports of a corner within a tolerance, so edges computed apart compare by exact ID. Moved out of `EdgeRings`.
- **`Disk.measureSagitta(radius, segments)`**: how far a disk's polygon falls inside the disk at worst - the resolution anything drawn against it really has.
- **`Segment`** and `{x, y}` points:
  - `readStart()` and `readEnd()`: a segment's ends, each a fresh array.
  - `joinPoints(start, end)`: the segment between two points.
- **Polygon checks**:
  - `PolygonRegions.countSelfCrossings(ring)`: how many times a ring crosses itself. Quadratic, so for tests and probes.
  - `PolygonOffsets.hasInsetCollapsed(rawRing, insetRing)`: whether a miter inset folded a ring over rather than offsetting it.
  - `PolygonOffsets.removeReversedLoops(polygon, isCounterClockwise, windowVertexCount)`: the fold splicer told the intended winding, for a ring whose folds outweigh its body. Both forms splice a fold that straddles the ring's start, and only a loop that does not cross itself, innermost first.
- **`Points.measurePathLength(points)`**: an open path's length, end to end.
- **`PolygonShapes.computeRegularVertices`**: a regular polygon's vertices from a centre, radius, side count and start angle, as `{x, y}` pairs or `Vector2f`s. `LabelledPolygon.createRegularPolygon` seeds its clips through it.
- **`Vector2f` forms**, for shapes laid out in the game's float UI coordinates:
  - `Points.computeMeanOfVectors`
  - `PolygonRegions.isPointInsideRing(ring, point)`
  - `Rectangle.computeEnclosingRectangle`: answers a `Rectangle`, placed by a corner and a size.

##### Controls

- **Radios**:
  - `RadioSpec`: the sealed interface over horizontal and vertical radios, carrying the re-pick rule. Existing `HorizontalRadio` call sites compile unchanged.
  - `VerticalRadioSpec`: a column of option cells, one lit, for option sets too long for a row.
- **Scrolling**:
  - `ScrollingSectionSpec`: a run of controls that scrolls together, such as a heading, its list and the row beside it.
  - `Control.isScrolled()`: whether a laid-out control sits in that section. A `Control` built without it is pinned.
- **`InteractiveSpec.isSegmented()`** and **`reselectBehaviour()`**: a control states whether its cells are hit separately and what re-picking a lit cell does, so the hit-test and the press agree.
- **`RowDimensions`**: each row's height and width as one checked value.
  - `ControlStripLayout.StripMeasurement.rowDimensions()`: a measurement's rows in that shape.
- **`TooltipRow.createRow(List<LabelRun>)`** and **`LabelledRow.createRow(List<LabelRun>)`**: a row over a label already composed as runs.

##### Campaign and saves

- **Memory per address**: a stored value held once per point on an axis the mod declares, rather than once per save.
  - `MemoryKeyAddress`: composes the key in one place, so no two holders share a slot by accident.
  - `AddressedMemoryFlag` and `AddressedMemoryString`: the holders.
- **Forms naming a sector**, for code handed its sector rather than reading the running one:
  - `SectorMemoryString` and `SectorMemoryFlag`: every read and write, over `SectorMemoryAccess.readSectorMemory(sector)`.
  - `BaseExpiringIntelPlugin.findActive(sector, intelClass)`, `isExpired(clock)` and a constructor taking the clock.
- **Calendar**:
  - `CampaignCountdown`: a span of campaign days from a start, with an optional completion slack so it finishes on the frame the player expects. `BaseExpiringIntelPlugin` reads its window through one.
  - `CampaignMonth`: one month of the campaign calendar. `formatKey()` spells it `<cycle>-<month>`, the key a monthly job saves its last run under.
- **`Colonies.selectColonies(test)`** and **`Colonies.hasAnyColony(test)`**: filter a colony set in its own order, or stop at the first match. An absent test passes nothing.
- **`PersistedChoice`** and **`PersistedChoices.fromKey()`**: an option a save stores by its own key, and the lookup back with a fallback. `SortDirection`, `ListColumns` and `ListSortMode` are persisted choices.

##### Core utilities

- **Strings**:
  - `KmlibStringKeys.get()` and `format()`: lookups in KMLib's own category.
  - `KmlibStrings.requireText()`: rejects a null or blank name or sentence.
- **`GlMatrix`**:
  - `FLOAT_COUNT`: the sixteen floats a GL matrix takes. `ModelviewMatrixReader.MATRIX_FLOAT_COUNT` reads off it.
  - `createIdentity()`: a fresh identity matrix each call.
- **Data files**:
  - `SpreadsheetRows`: a merged spreadsheet's data rows, without blank-ID spacers and `#` comments, with list-valued cells split. `FactionSourceMods` reads `factions.csv` through it.
  - `ScriptClasses.instantiateScript(className, scriptType)`: builds a class a data file names without `java.lang.reflect`, which the game refuses mod code. A bad name is refused naming the class.
- **`Jitter.roll(jitterSize, random)`**: a jitter drawn from a caller's `Random`, to replay from a seed. The band's upper end is open.

##### Build and release

The build and release side of the per-language zips. The localisation fixtures under Test fixtures check what these write.

- **`writeLocaleFiles`**: writes one locale's bundle into the files the game reads, for a mod committing `localisation/manifest.json`. `-Plocale=<tag>` picks the locale, and the manifest's default is built otherwise.
  - Where the mod commits `mod_info.base.json`, the locale's launcher fragment is merged over it, text fields only.
  - JSON is read through `shipped-json-reader.gradle`, over the same `json.jar` as `ShippedJson`.
  - `jar` and `test` re-run on a locale switch. `test` also re-runs when `localisation/` or the root `CHANGELOG.md` changes.
- **`mod_info.base.json`**: a mod with per-locale launcher text commits the base and gitignores the `mod_info.json` written from it. Every build script and release action reads the base where one exists.
  - `mod-info-reader.gradle`: reads that metadata once, as `modInfo` and `modInfoFile`, with `readModInfo(File)` for another checkout.
- **Per-locale releases**: `mod-release.yml` releases a mod committing `localisation/manifest.json` as one zip and one version file per locale, from one jar. A mod with no manifest releases as before.
  - `read-locales`: the locales a mod releases in, default first.
  - `package-release`: writes, zips and fills the version file for each locale. Each zip carries its own locale's `CHANGELOG.md`.
  - `compose-locale-note`: the release body's _Builds by language_ list, linking each locale's core localisation, and each translation's notes collapsed under its name. A translation missing the version's section fails the release.
  - `fill-version-file-template` takes an optional `locale`, so an install polls its own language's version file.
- **KMLib's own text per locale**: strings and settings under `localisation/<locale>/`, launcher text in `mod_info.base.json`.
  - `localisation/zh-hans/CHANGELOG.md` translates this changelog in full.
  - `LocaleParityIntegrationTests` holds every locale to the default.
  - The Chinese bundle's `README.md` is the base terminology reference each consumer mod's builds on.

##### Test fixtures

- **`SaveFormatFixture`**: drives a mod's persisted objects through XStream, the game's save serialiser, so a class or field rename that would orphan saved values fails a suite. It lists the element paths a graph writes and reads the graph back as a load does.
- **Test conventions**, in the shared Starsector conventions:
  - Every test task opens `java.util`, `java.lang.reflect`, `java.text` and `java.awt.font` to unnamed modules, as the game's `vmparams` does. XStream 1.4.10 needs them.
  - `mockStatic(Global.class)` outside `StubbedGlobalLogger`, or any `openSeam(Global.class)`, fails the build. A stand-in answering no logger leaves a static logger null for the rest of the JVM.
- **Static seams**:
  - `StaticSeams`: the seams one arrangement has open, closed innermost first. `openSeam()` opens one, and `holdSeam()` takes one a fixture opened, such as `StubbedGlobalLogger.openGlobalAnsweringLoggers()`'s.
  - `SalvageEntityMock`: vanilla's drop roller held still, capturing each roll and pinning the roll count. Seven-argument roller only.
- **Game and mod state**:
  - `StarsectorSettingsFake`: `answerGameVersion()` and `answerModGameVersions()` answer game versions, and `installSettingsWithModNames()` knows mods by name.
  - `ModStateScopes.runWithGameVersions()` and `runWithModNamed()`: stand those up for one mod.
  - `IntelManagerFake`: holds the intel added to it, by the game's rules for what it keeps.
  - `StoredMemoryFake`: map-backed memory with nothing behind it, for a suite that already stands in for `Global` or hangs memory off a planet. `SectorMemoryFake` is built from it.
  - `MemoryKeyAddresses`: two stand-in addresses.
- **Compatibility**:
  - `CompatibilityFailureFixture`: a representative failure with a builder per slot, two subject keys, and two consumers that lose different things. `drainSessionRecord()`, `createFeatureFailure()` and `takeNextBindingFailure()` reset and read records.
  - `CompatibilitySlotTemplates`: the notice's templates as stand-ins exposing their slots.
  - `CoreUiReachFailures`: the two ways a reach through `CoreUiTree` fails on an unknown game build, for a boundary that must catch both.
  - `GameReachRecordFixture`: a game-reach reporter filing into a record of the case's own.
- **Logging**:
  - `LogAppenderFake.getThrowables()`: the throwables a capture's entries were logged with.
  - `LogAppenderFake.captureLogOf(loggingClass, capturedLevel, work)`: a capture at a stated level, for what a level lets through.
- **UI**:
  - `TooltipMakerFake`: a tooltip element recording paragraphs, spacing, highlighted runs and buttons, without layout.
  - `HighlightedTooltipMock`: a tooltip giving each paragraph its own label, so its highlights can be checked.
  - `LabelHighlightRule`: which runs the game's highlight call leaves plain - any run not bordered by whitespace, ASCII punctuation or the text's ends, such as one touching Chinese text. `UnhighlightedRun.describeIn()` words one as a finding.
  - `ButtonLabelFake`: a button's label, measured at `CHARACTER_WIDTH` per character, recording the width it was fitted to.
  - `Anomaly`, `AnomalySortMode` and `ListPickerBlockReads`: a picker row, a sort vocabulary, and reads into a built picker block.
- **Fonts**:
  - `FaceLineHeightReaderFake`: line heights from a table. `createVanillaLineHeights()` answers vanilla's, and `answeringLineHeight()` states another atlas under one basename.
  - `LazyFontLineHeightReaderMock`: the live line-height reader, answering from a `FaceLineHeightReaderFake`.
  - `GlyphCoverageReaderFake`: glyph coverage by rule. `createLatinOnlyCoverage()` is a vanilla install, and `coveringEveryCharacter()` widens one face.
- **Shipped files**, each read the way the game reads it:
  - `ShippedJson`: the engine's `#` comment strip in front of the game's own `json.jar`. It refuses what the game refuses, a duplicate key and a byte-order mark included, and its shape checks fail a misspelt field.
  - `ShippedStrings`: a mod's `data/strings/strings.json`, published as `STRINGS_JSON`, and the string IDs its holder class names.
  - `ShippedSpreadsheet`: a mod's CSVs by header, by ID column, by one column or by position, with quoted commas parsed right. It brings Apache Commons CSV as an `api` dependency, and `LunaSettingsTable` reads through it.
  - `StringTemplates.countFormatArguments()`: how many arguments `String.format` takes from a template, counting a repeated position once and `%%` and `%n` not at all.
  - `LunaSettingsHighlights` and `LunaSettingsTable.readHighlightedTextsByFieldId()`: a settings cell as LunaLib draws it, brackets removed and highlighted runs listed.

##### Localisation fixtures

These read a mod's `localisation/` layout - one bundle per locale under `localisation/<locale>/`, named by a manifest - and compare its locales. Each file is read the way its shipped copy is.

- **`LocaleParity`**: holds every locale to the default, wording each finding as the edit to make. `findAllMismatches()` gathers every check, so a mod's suite asserts once.
  - Strict where a gap has no fallback: a missing file, string or row, a blank string, a slot taking another argument, a row storing something else, a moved tab, non-Latin-1 text with no `coreLocalisation`, an undeclared dependency.
  - A Radio's options must match, since LunaLib stores the label picked.
  - A translated changelog must match the root one point for point.
  - Every bracketed run in every settings table must be one the game can highlight.
- **Layout readers**:
  - `LocalisationDirectory`: a mod's `localisation/` directory, its manifest and its bundles.
  - `LocaleManifest`: the locales, the default, and where each bundle file lands. An unknown key is refused, so a misspelt `coreLocalization` fails.
  - `DeclaredLocale`: a lowercased BCP 47 tag, the locale's own name, and the core localisation its players install over `starsector-core`.
  - `LocaleBundle`: one locale's directory. `readStringSource()` stands its strings up for the settings stand-in.
  - `ModInfoFragment`: the launcher text a locale translates: `name`, `description`, `author` and dependency names. `listFallbackFieldNames()` names the fields left to the base.
  - `ModInfoBase`: what a fragment reaches of `mod_info.base.json`.
- **Comparison readers**:
  - `ChangelogOutline`: a Keep a Changelog file's versions, sections, list items per depth and code spans, without its prose.
  - `LunaSettingsTable.readBehavioursByFieldId()`, `readDisplayedTexts()` and `readTabsByFieldId()`: a settings row split into what it does, as a `FieldBehaviour` that `describeDifferencesFrom()` compares, what it says, and its tab.
  - `StringTemplates.readArgumentConversions()`: which argument each slot takes and as what.
  - `ShippedJson.requireList()` and `locateElement()`: the array counterparts of the object check and the member location.
- **`ShippedLocales`**: the locales a mod ships, for a case run once per locale. `listLocaleTags()` is a parameterised source, and `installLocaleStrings()` stands one locale's strings up as the game's.

#### Changed

- **`MapIconReseater` logs what it saw**, on KMLib's own logger. The package README lists the lines.
  - Map open and close are traced at DEBUG, each with the lift count since the icon was last seen clear.
  - Two states warn once per session: a map up with no placeable icon, and a stand-down, with the readings behind it.
- **`VoronoiCellBuilder` puts a corner adjacent cells share in the same place in both**, worked out from the two sites and the reach. Chained frontier edges meet without gaps, and the segment count sets only how smoothly an arc is drawn.
  - Cell vertices move by up to a chord's sagitta at those corners: about eight units at the shipped reach and default count. Re-baseline any assertion tighter than that.
- **The Starsector conventions read a mod's metadata through the game's `json.jar`**, as the launcher does, so the two agree on whether it parses. Configuring the build therefore needs the install.

#### Public contracts changed (**breaking**)

| Before | After | Why |
| --- | --- | --- |
| `CampaignMapView`'s three map reads and `resolveSectorMapState()`, `MapFilterRows.resolveShownMapFilterRow()`, `MapFilterToggle.appendToRow()`, `VanillaButtonLabel.resolveLabelOf()`, `CoreUiOverlayPanels.attachOverlayPanel()`, `CoreUiDialogView.isModalDialogShowing()` and `resolveModalPresence()`, `CodexView.isCodexShowing()`, `MapIconLayeringProbe.readLayeringOf()` | Each takes a `GameReachReporter` as its last argument. Pass `GameReachReporter.UNREPORTED` where nothing player-visible is lost. | A failed reach into the game is reported as the calling mod's loss. |
| No-argument constructors of `MapPresence`, `VanillaIntelScreenView` and `VanillaMapTooltipProbe` | Construct each with a `GameReachReporter`. | The same. |
| `ReflectiveCoreUiComponentRepainter.INSTANCE` | Construct one with the caller's reporter. | A failed repaint is filed as the caller's loss. |
| `CompatibilityFailures.takeNextUnreported()` and `CompatibilityNoticePanel.showFailure()` use `CompatibilityFailure` | Both use `ReportedFailure`. Check for a `CompatibilityFailure` before reading `subject()` or `breakage()`. | The record also holds a mod's own failed features. |
| `ModelviewMatrixReaders.selectForActiveRenderer()` with no argument | Pass the `CompatibilityConsumer` taking the reading. | Only the caller knows what a refused read costs it. |
| `FastRenderingModelviewMatrixReader.INSTANCE` | `ModelviewMatrixReaders` builds one per consumer. | A reader records a refusal against the mod it serves. |
| `ExtensionPoint.settleWorkOutcome()` | `offerWork(work)`. `registerImplementation()` takes a fourth argument: what the registrant is told when its implementation is taken out. | The call into the implementation runs inside the failure boundary. |
| `ColonisationRoutines.registerRoutine()`, `OwnershipTransferRoutines.registerRoutine()`, `OwnerSubmarketRules.registerRule()`, `ModdedSystemAccessRoutes.registerRoute()` | Each takes a `Supplier<ModIntegration>`. | A failing implementation is reported as its integration's. |
| `MapProbeWarnings` and `createSharedWarning()` | `RearmableWarnings` in `kmlib.logging`, and `createRearmableWarning()`. `rearmAllWarnings()` is unchanged. | It gathers every per-class warning, not only the map probes'. |
| `KmLogging` | `LunaLogLevelBinding.bindLogLevel(modId, loggerRoot, fieldId)`, defaulting to `LunaLogLevelBinding.DEFAULT_LEVEL`. The fallback-level overload is gone. | It sits in `kmlib.settings` with KMLib's other LunaLib reads. |
| `GlStateGuard` | `GlPasses.runWithSavedState` | It only forwarded there. |
| `ControlSpec.Checkbox`, `ControlSpec.Interactive` and the other ten variants | `CheckboxSpec`, `InteractiveSpec` and so on, in `kmlib.starsector.ui.controls.specs`, with `ControlAction`, `ControlHoverReport`, `RadioAlignment`, `ReselectBehaviour`, `RowGeometry` and `SegmentSizing`. | One file per variant, instead of one 878-line file. |
| `VerticalTableSpec`'s `scrolls` and `asScrolling()` | Wrap the list: `new ScrollingSectionSpec(List.of(list))`. The canonical constructor takes seven arguments. | A scrolling section holds any controls, not one table. |
| `RowStack.layoutRows()` with height and width lists, and its uniform-height overload | Pass a `RowDimensions`, or `RowDimensions.createUniform(rowHeight, rowWidths)` for one height. | The overloads' third float was a row height in one and the gap in the other. |
| `ControlStripLayout.layoutControls()` with `rowHeights` and `rowWidths` | Pass the `StripMeasurement`. | The two lists come from one measurement, so they cannot disagree. |
| `SortDirection.fromKeyOrDefault()` and `ListColumns.fromKeyOrDefault()` | `PersistedChoices.fromKey(options, key, fallback)`, such as `PersistedChoices.fromKey(ListColumns.values(), key, ListColumns.DEFAULT)`. | One lookup for every keyed option set. |
| `StarsectorFont.getNativeSize()` | `InstalledFaces.createNativeFace(font)`, sized through `FaceLineHeightReader`. | A localised install replaces atlases under the same names at other sizes. |
| `TextStyle.createStyle()` with a `StarsectorFont` | Pass a `TextFace`, such as `InstalledFaces.createNativeFace(...)`. | The style no longer knows a native size. |
| `TextFace.font()`, a `StarsectorFont` | `TextFace.atlas()`, a `FontAtlas`. `LazyFontCache.loadByFace()`, `FaceLineHeightReader`, `GlyphCoverageReader`, `WidgetStyle.bodyFont` and `StripTextMeasurers.loadFaceMeasurers()` take one too. | Text can draw in the game's declared default face. A face built from a `StarsectorFont` compiles unchanged. |
| `LazyFontCache` and `DrawableStringCache` in `kmlib.starsector.ui.font` | `kmlib.starsector.ui.font.installed` | Readers of the running game's atlases live apart from the faces. |
| `LineWidthMeasurer`, `TextSpanMeasurer`, `LazyFontMeasurer`, `LazyFontSpanMeasurer` and `StripTextMeasurers` in `kmlib.starsector.ui.font`, and the fixture `LineWidthMeasurerFake` | `kmlib.starsector.ui.font.measure`, and `kmlib.testfixtures.starsector.ui.font.measure` for the fixture. | Measuring lives apart from the faces. |
| `StubbedGlobalLogger.answerLoggersOn()` | `openGlobalAnsweringLoggers()`, handed to `StaticSeams.holdSeam()` alongside other seams. | One stand-in for `Global`, closed with the other seams. |
| `mod-release.yml` releasing a mod with `localisation/manifest.json` as one unsuffixed zip | `<folder>-<version>-<locale>.zip` and `<mod-id>-<locale>.version` per locale. `<mod-id>.version` is still attached, so older installs keep polling it. | One zip per language. |

</details>

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

- Reflection utils are lifted from **MagicLib** per **Numan**'s recommendation [at USC](https://discord.com/channels/187635036525166592/1549091275167240272/1549267098415403011) (he's working on dev builds of **MagicLib**). Scoped to **coreui** package.
- **MagicLib** dependency is removed.
- The project is relicenced under under **LGPL-3.0-only** to comply with licencing of donor code.

## [0.3.0] - 2026-09-15

### Fixed

- **Crash on Linux**. EventsPanel.getMap() returns an obfuscated type that isn't the same on different platforms. - Reported at **USC** by [**Elia Rowan (zinzrinz)**](https://discord.com/channels/187635036525166592/1549091275167240272/1549127910084972614) and [**MattTheMatt2**](https://discord.com/channels/187635036525166592/1549091275167240272/1549149360644948121), localised and fix suggested by [**WolframSegler**](https://discord.com/channels/187635036525166592/1549091275167240272/1549130150312935506).
- **Crash**. **Starscape** Map terrain reseat failure on a mismatched widget signature is now handled and logged, resulting in terrain reseating standing down for the rest of the section.
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
