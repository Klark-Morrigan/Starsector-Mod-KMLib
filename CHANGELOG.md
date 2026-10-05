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

### Fixed

- **A shortcut key shown after a button's words stays on the button.** In a translation whose words lack the key's letter, or in English with the key rebound, a key such as `[M]` wrapped onto a second line outside the button.
- **Map layers drawn by KM mods stay above the nebulae with the campaign speed-up on.** Every layer stayed under the nebulae on every map open, and neither a save reload nor a Starscape toggle cleared it. - Reported by **MiniRockytheOracle** [at **USC**](https://discord.com/channels/187635036525166592/1549091275167240272/1551829173037957170).
  - **A layering fix that gave up is tried again on the next map open**, rather than staying off for the rest of the session.

### Added

- **Simplified Chinese (简体中文).** A second zip, `KMLib-<version>-zh-hans.zip`, carries the compatibility notice, the settings tab and the mod list entry in Simplified Chinese. Install the [Chinese localisation](https://github.com/TruthOriginem/Starsector-Localization-CN) over `starsector-core` first: the game's own fonts hold no Chinese characters, so without it every one draws as `?`. The Log verbosity options stay in English, so the setting carries over between the two zips.
- **An in-game notice says when a KM mod cannot integrate with another mod, Fast Rendering or the game itself.** It names the mod, both versions, what stops working and what does not, and whether to update, downgrade or wait. The failure costs only the feature built on that integration, and each one shows once per session.
  - **A KM mod's own feature that fails and is switched off is reported the same way**, asking for a report to the mod's developer.
  - **A failure found while a map is open can show over the map**, without waiting for the campaign screen.

### Test fixtures

- **`SaveFormatFixture`**: a mod's persisted objects driven through XStream, the serialiser the game writes a save with - the element paths a graph writes, sorted, to compare against a checked-in list, and the graph read back as a load reads it. A class or field rename orphans a value in every existing save or fails its load, and neither shows in a suite that never serialises. Collaborators the graph points at without owning are named when the fixture is built and written as empty elements, so their fields still appear in the paths while what they hold is left to the suite about them. No XStream type crosses its surface, so a consuming suite needs the game's XStream jar only at run time, where the conventions already put every core jar.
- **Test JVMs open what the game's JVM opens**: the shared Starsector conventions open `java.util`, `java.lang.reflect`, `java.text` and `java.awt.font` to unnamed modules on every test task, as the game's own `vmparams` does. XStream 1.4.10 cannot be constructed under a modern JDK without them, and a suite gains nothing the running game does not have.
- **`CoreUiReachFailures`**: the two ways a reach through `CoreUiTree` fails on a game build it does not recognise - the game's own failure wrapped in a checked exception and thrown undeclared, and a member that no longer links - each answering any type, so it stands in for a reach taken as a supplier. A case throwing the unchecked exception that is easiest to write passes against a boundary that catches too little.
- **`StarsectorSettingsFake` answers game versions**: `answerGameVersion()` for the running game's and `answerModGameVersions()` for the one each mod declares, with `ModStateScopes.runWithGameVersions()` standing both up for one mod.
- **`GameReachRecordFixture`**: a reporter for a reach into the game filing into a record of the case's own, and that record read back, so whether a probe filed does not depend on which suites ran before it.
- **`LogAppenderFake.getThrowables()`**: what a capture's entries were logged with, in order, leaving out an entry that carried none - the reading for a case asking whether a failure was logged with its trace.
- **`LogAppenderFake.captureLogOf(loggingClass, capturedLevel, work)`**: a capture holding the logger at a stated level rather than at every level, for a case about what a level lets through - a guarded trace that must cost nothing while its level is off.
- **`StaticSeams`**: the static seams one arrangement has standing, closed innermost first on the way out, for an arrangement standing in for several classes at once. `openSeam()` opens one over a class; `holdSeam()` takes one a fixture opened with answers of its own - `Global`'s, from `StubbedGlobalLogger.openGlobalAnsweringLoggers()` - so it closes in its place with the rest.
- **A bare stand-in for `Global` fails the build**: the Starsector conventions allow `mockStatic(Global.class)` in `StubbedGlobalLogger` alone, and `openSeam(Global.class)` nowhere, across every KM package root. A class whose static logger is first resolved inside a stand-in that answers no logger keeps its null for the rest of the JVM, failing a later suite on a line it never wrote; the failure names the call to write instead. Matched as Java spells the call, so a Kotlin suite is not held.
- **List widget fixtures**: `Anomaly` and `AnomalySortMode`, a picker row and a sort vocabulary declared outside the list package, and `ListPickerBlockReads`, which reaches into a built picker block for the columns selector, the sort row, the sort selector or the item list. The block's order lives there rather than in each suite that tests a list, so a row inserted into it breaks one file.
- **`MemoryKeyAddresses`**: two stand-in addresses, for suites storing a value at one point on an axis without being about what the axis is.
- **`CompatibilityFailureFixture`** and **`CompatibilitySlotTemplates`**: one representative compatibility failure with a builder per slot a case varies, plus the two subject keys and the two consumers a suite records under - each consumer losing something the other does not, so a case about two of them being told apart cannot pass on one sentence standing for both - and the notice's templates as stand-ins that expose their slots, so a suite about the record, the notice or the wording names the one slot it is about. `drainSessionRecord()` empties the process's own record, which outlives a case, for any suite driving a guard that records into it. `createFeatureFailure()` builds the representative consumer's own feature failure, and `takeNextBindingFailure()` takes the next report off a record as a binding failure, so a case reading its subject or breakage need not cast.
- **`StarsectorSettingsFake.installSettingsWithModNames()` and `ModStateScopes.runWithModNamed()`**: a mod manager that knows mods by name as well as by enablement, for a subject that shows a mod's own name. With every ID answering nothing, the name a report found and the ID it fell back to are the same string, so the branch that matters could not be told from the branch that does not.
- **`TooltipMakerFake`**: a tooltip element that records the paragraphs, their spacing, the runs highlighted on each one's label and the buttons added to it, instead of laying anything out. A proxy rather than a hand-written stand-in, the element API being far too wide to implement for the handful of calls a body makes - which is what lets a suite about what a panel is made of run with no game around it.
- **`ShippedStrings`**: reads a mod's shipped `data/strings/strings.json` - the one path the engine reads, published as `STRINGS_JSON` - and the string IDs its holder class names, for the guard that holds those two together, and any strings file by category or with its categories flattened. The file is parsed through `ShippedJson`, so every key the game reads is read here, whatever its spelling, and a key declared in two categories fails the flattened reading rather than leaving the guard comparing against whichever came last.
- **`StringTemplates.countFormatArguments()`**: how many arguments `String.format` takes from a shipped template, for the guard that holds a template's slots to the call site filling them. A template that gained a slot renders as the fallback sentinel, and one that lost a slot renders with its last figure silently dropped, the formatter ignoring surplus arguments; neither shows in a suite that stubs the lookup. The count is the part that is easy to get subtly wrong: a positional specifier may repeat an index, so it implies the highest index rather than the number of specifiers, and `%%` and `%n` take no argument at all.
- **`SalvageEntityMock`**: vanilla's drop roller held still, owning the static mock's lifetime and capturing each roll's multipliers, drop lists and random source. Every capture pins the roll count as well, so code rolling twice where it should roll once fails the case rather than passing on the first roll's arguments. Holds the seven-argument roller only, a static mock stubbing each overload on its own.
- **`ShippedSpreadsheet`**: a mod's shipped CSVs, in whichever of four shapes a suite reads them - rows by header name, rows keyed by an ID column, one column's values, or lines of cells by position. Here rather than in each mod's suite because the parse is the part that can be subtly wrong: a shipped table quotes the fields carrying commas, and a reading that splits on the comma instead lands one column left of what it meant to read, on exactly the rows that quote, and keeps passing against the wrong cell. The positional reading is not a convenience but the only one some tables allow - LunaLib's settings table leaves several of its column names blank, and a parser asked to key on that header refuses the file outright. Blank-ID rows are dropped by the keyed readings as the spacing they are, and kept by the positional one, a caller reading by index judging its own rows. `LunaSettingsTable` reads through it rather than splitting lines itself. Apache Commons CSV comes with the fixtures variant as an `api` dependency, a consumer needing the parser on its own test runtime to read a row back. Deliberately no JSON shaping: a mod whose table parser takes the engine's row-object shape builds it from these rows in its own suite, so no mod reading a table needs `json.jar` on its test compile classpath.
- **`ShippedJson`**: a mod's shipped JSON read the way the engine reads it - the engine's `#` comment strip, copied character for character, in front of the org.json in the game's own `json.jar`. What the game loads reads here, trailing commas and comments included, and what it refuses fails here: a duplicated key, and a byte-order mark, named rather than reported as "must begin with '{'". Objects come back as sorted maps, the game's parser keeping no member order, and no org.json type crosses the boundary. Carries the shape checks a reading states its fields through, an unknown key among them, so a misspelt field fails rather than reading as absent, and names the file when a value built from it refuses what was read.
- **`IntelManagerFake`**: an intel manager that holds what is added to it, for a subject that adds an intel and then asks the manager for it - a cap checked against what was already recorded, an entry installed only if absent. It applies the game's rules for what it keeps: an intel already held is not added twice, an ended one not at all, and a read by class answers everything assignable to it in the order it was added. What the game does beside holding - stamping when the player saw an intel, telling it it was shown or removed, posting a message - reaches into the running game and is left to a suite about it.
- **`FaceLineHeightReaderFake`**: line heights from a table, for composing native faces with no install. `createVanillaLineHeights()` answers what vanilla's descriptors state, and `answeringLineHeight()` states a different atlas under one basename, for a suite pinning that a size was read rather than written down.
- **`LazyFontLineHeightReaderMock`**: the live line-height reader held still and answering from a `FaceLineHeightReaderFake`, for a suite over a look composed statically per paint with no reader handed in.
- **`GlyphCoverageReaderFake`**: glyph coverage from a rule. `createLatinOnlyCoverage()` is a vanilla install, every face holding Latin-1 alone; `coveringEveryCharacter()` widens one face, as a localised install's replaced atlas does.
- **`StoredMemoryFake`**: the map-backed memory on its own, with nothing behind it. `SectorMemoryFake` reaches memory the way the game does, through `Global.getSector()`, and holds a static stand-in open for its lifetime to do it - which a suite that already holds its own stand-in for `Global`, or that hangs memory off a planet rather than the sector, cannot use at all, a second stand-in for one type throwing. Such a suite poses this and attaches the memory itself, and gets the same stored values and the same write and removal counts. `SectorMemoryFake` is built from it and answers every reading through it, so its own surface is unchanged.
- **`LabelHighlightRule`**: which runs a label's highlight call leaves plain, by the game's rule. A run highlights only where the characters beside it are whitespace or ASCII punctuation, or where it starts or ends the text, and each run is searched for from where the previous one matched. So a run touching Chinese text or a full-width mark such as a Chinese full stop draws plain, with no error. Takes a text and its runs, or a `HighlightedParagraph` as it hands its runs over. Copied from the renderer, whose classes do not load outside a running game. `UnhighlightedRun.describeIn()` words a run left plain as a finding.
- **`HighlightedTooltipMock`**: a stand-in tooltip that gives every paragraph added to it a label of its own, so every highlight a subject draws into it can be held to that rule afterwards.
- **`ButtonLabelFake`**: a button's words as the label the game hands them over in - what they say, the runs lit in them and the colour they are lit in. It measures text at `CHARACTER_WIDTH` per character and records the width it was last fitted to, so a suite can pin that lengthened words are re-fitted rather than wrapped.
- **`LunaSettingsHighlights`** and **`LunaSettingsTable.readHighlightedTextsByFieldId()`**: a settings cell as LunaLib draws it, its brackets removed and the runs it highlights listed, and the cells LunaLib draws that way: a value row's description and a prose row's words.

#### Localisation

A mod's player-facing files can be kept per language, one bundle per locale under `localisation/<locale>/`, with a manifest naming the locales, the default and where each bundle file lands in the mod. Each translated locale's directory also holds a translation of the mod's `CHANGELOG.md`. These fixtures read that layout and compare its locales. Every file is read the way the mod's shipped copy of it is read - JSON through `ShippedJson`, strings through `ShippedStrings`, a settings table through `LunaSettingsTable` - so a bundle is never parsed a second way.

- **`LocalisationDirectory`**: a mod's `localisation/` directory. Reads the manifest at its root, lists every bundle directory beside it whether declared or not, opens a bundle only for a locale the manifest declares, and reads the `mod_info.base.json` beside it where the mod commits one.
- **`LocaleManifest`**: which locales exist, which is the default, and which files a bundle holds and where each lands - the single source of truth for all three. Every key it may carry is known and any other is refused, so a misspelt `coreLocalization` fails rather than reading as absent. Its data paths are held inside the mod root however a manifest is built, a copy onto one being a write into the repository.
- **`DeclaredLocale`**: one declared locale - a lowercased BCP 47 tag, its name in its own language, and, where the vanilla atlases lack its glyphs, the https project its players install over `starsector-core`.
- **`LocaleBundle`**: one locale's directory. Reads its strings file, its settings table and its launcher fragment, and resolves any other bundle file by bare name only, so no bundle reaches into another.
- **`ModInfoFragment`**: the launcher text a locale translates - `name`, `description`, `author` and dependency names, each optional and falling back to the base file. Any functional field is refused, a fragment varying the version or the jar list building a different mod per locale. `listFallbackFieldNames()` names each field a fragment leaves to the base.
- **`ModInfoBase`**: what a fragment reaches of `mod_info.base.json` - the text fields it falls back to and the dependency IDs it may name.
- **`LocaleParity`**: holds every locale to the default, each check answering findings worded as the edit to make. Strict where a gap has no fallback - a missing directory, file, string or row, a string left blank in any locale, a slot taking another argument, a row varying what it stores, a tab split or merged, text outside Latin-1 with no `coreLocalisation` named, a fragment naming an undeclared dependency - and reporting the launcher fields a locale leaves to the base. A Radio's options are held identical, LunaLib storing the label picked. A translated changelog must match the root one point for point: the same version headings in the same order, the same sections, the same number of list items at each depth, and the same code spans in each section. Its prose is not compared. Every bracketed run in every locale's settings table, the default's included, must be one the game can highlight where it stands. Each defect is found once, and a file the manifest does not map is not compared. `findAllMismatches()` gathers every check, so a mod's suite asserts once; a mod shipping no settings table opens it without a field ID prefix.
- **`LunaSettingsTable.readBehavioursByFieldId()`, `readDisplayedTexts()` and `readTabsByFieldId()`**: a row split into what it does and what it says - its `FieldBehaviour` of type, stored default, options and bounds, which `describeDifferencesFrom()` compares column by column, the cells the screen draws, and its tab. A field ID declared twice fails a keyed reading rather than being dropped from it.
- **`StringTemplates.readArgumentConversions()`**: which argument each slot of a template takes and as what, whatever order the slots are written in - what two wordings of one string must agree on for one call site to fill both.
- **`ShippedJson.requireList()` and `locateElement()`**: the array counterparts of the object check and the member location.
- **`ChangelogOutline`**: the outline of a Keep a Changelog file: its versions by heading, each version's sections, how many list items each section holds at each nesting depth, and each section's code spans. The prose is not read, so a translation has the same outline as its original until a point is added, dropped or given other identifiers in one file only.
- **`LocaleBundle.readStringSource()`**: a locale's strings in the shape the settings stand-in answers them, so text composed under it reads in that locale.
- **`ShippedLocales`**: the locales a mod ships, read from its own `localisation/` directory, for a case composing text once per locale: `listLocaleTags()` as a parameterised source, and `installLocaleStrings()` standing one locale's strings up as the game's.

### Changed

- **Under Fast Rendering, the map follows the cursor only from `v0.9.1rc1` on.** The cursor reading is current to the frame rather than a frame or two behind while the map pans. On an earlier release the map does not respond to the cursor, and a notice names the release to update to.
- **The Log verbosity setting's description ends with its default on a line of its own**, so the default highlights in Chinese too.
- **The mod list names the mod Klark Morrigan's Library (KMLib).** Update checkers show the same name.
- **Each language ships as its own zip**: `KMLib-<version>-en.zip` and `KMLib-<version>-zh-hans.zip`, side by side on the release page. The release notes say which is which, and update checkers keep working across the change.

### Public contracts changed (**breaking**)

- Every read reaching into the game's own screens takes the `GameReachReporter` of the mod that loses something where it fails: `CampaignMapView`'s three map reads and `resolveSectorMapState()`, `MapFilterRows.resolveShownMapFilterRow()`, `MapFilterToggle.appendToRow()`, `VanillaButtonLabel.resolveLabelOf()`, `CoreUiOverlayPanels.attachOverlayPanel()`, `CoreUiDialogView.isModalDialogShowing()` and `resolveModalPresence()`, `CodexView.isCodexShowing()` and `MapIconLayeringProbe.readLayeringOf()` take it as a last argument, and `MapPresence`, `VanillaIntelScreenView` and `VanillaMapTooltipProbe` at construction, their no-argument constructors gone. A caller with no player-visible loss passes `GameReachReporter.UNREPORTED`.
- `CompatibilityFailures.takeNextUnreported()` returns a `ReportedFailure`, and `CompatibilityNoticePanel.showFailure()` takes one, where both used `CompatibilityFailure`. A record now holds a mod's own failed features beside its broken bindings. A caller that only shows or logs what it took compiles as before; one reading `subject()` or `breakage()` checks for a `CompatibilityFailure` first.
- `ReflectiveCoreUiComponentRepainter` is no longer an enum, and its `INSTANCE` is gone: a caller constructs one with its reporter, a repaint that fails being filed as that caller's loss.
- `MapProbeWarnings` is `RearmableWarnings`, in `kmlib.logging` beside `SessionWarning`, and `createSharedWarning()` is the public `createRearmableWarning()`. The warnings it gathers are every owner's that keeps one per class, not the map probes' alone. A caller re-arming them changes its import; `rearmAllWarnings()` is unchanged.
- `KmLogging` is gone. A mod binds its log level with `LunaLogLevelBinding.bindLogLevel(modId, loggerRoot, fieldId)`, in `kmlib.settings` with the rest of KMLib's LunaLib reads, and the library default is `LunaLogLevelBinding.DEFAULT_LEVEL`. The overload taking a fallback level is not carried over; an unset or unrecognised field applies the default.
- `GlStateGuard` is gone. It only forwarded to `GlPasses.runWithSavedState`, which a caller now names directly; the saved state and the restore on a throw are unchanged.
- `ModelviewMatrixReaders.selectForActiveRenderer()` takes the `CompatibilityConsumer` taking the reading; the no-argument form is gone. A caller now names the key its records latch under and the sentence naming what it loses, both read only where the read is refused. The sentence is the caller's because what a refused read costs is knowledge of the feature built over the reading: the library knows the renderer, both versions and what was thrown, and nothing about what was drawn with it.
- `FastRenderingModelviewMatrixReader` is no longer an enum, and its `INSTANCE` is gone: `ModelviewMatrixReaders` now builds one for the consumer that asked. The reader records the refusal it meets, and one that could not say which mod it serves could record it against nobody - what a refused read costs being the taking mod's to state. That one map is on screen at a time is a fact about the map rather than about how many mods draw over it. Nothing outside the package could construct one either way, the class being reachable only through the selection.
- `VerticalTableSpec` loses its `scrolls` component and its `asScrolling()` refinement; what scrolls is stated by the `ScrollingSection` a host puts a run inside. A host that marked its list now wraps it: `new ScrollingSectionSpec(List.of(list))`. The table's canonical constructor takes seven arguments where it took eight.
- The `ControlSpec` variants are top-level types in a new `kmlib.starsector.ui.controls.specs` package, one file per variant, each suffixed `Spec`: `ControlSpec.Checkbox` is now `CheckboxSpec`, `ControlSpec.Interactive` is `InteractiveSpec`, and so on for all twelve. Nested in one 878-line file they could not be opened, reviewed or blamed apart, and a variant's name could not be read without its enclosing type. The whole family moves together because a sealed type and its permitted variants must share a package, and `ControlAction`, `ControlHoverReport`, `RadioAlignment`, `ReselectBehaviour`, `RowGeometry` and `SegmentSizing` move with them so the new package depends on nothing in the old one. A consumer changes its imports and drops the `ControlSpec.` prefix; nothing else about a spec changed.
- `RowStack.layoutRows()` takes a `RowDimensions` in place of the per-row heights and widths as two adjacent lists, and the uniform-height overload is gone. The two overloads took five arguments each and both opened with three floats, where the third was a row height in one and the gap in the other, so a call site read the same whichever was meant. A run whose rows share a height is now `RowDimensions.createUniform(rowHeight, rowWidths)`.
- `ControlStripLayout.layoutControls()` takes the `StripMeasurement` the rows were measured as, in place of its `rowHeights` and `rowWidths` lists. They are one reading of one strip, and parted they could arrive from different readings - two lists of different lengths, or the heights of a strip the widths were never measured from - neither of which the placement could notice.
- `ExtensionPoint.settleWorkOutcome()` is gone: a point's work is offered through `offerWork(work)`, which calls the installed implementation itself. A port that called the implementation and settled the outcome afterwards left the call outside any boundary, and a point offering both would leave that way open. `registerImplementation()` takes a fourth argument, what the registrant is told when its implementation fails and is taken out.
- `ColonisationRoutines.registerRoutine()`, `OwnershipTransferRoutines.registerRoutine()`, `OwnerSubmarketRules.registerRule()` and `ModdedSystemAccessRoutes.registerRoute()` take a `Supplier<ModIntegration>`: which mod the implementation comes from and what the registering mod loses without it, composed only where the implementation fails.
- `mod-release.yml` names a localised mod's release assets by locale: `<folder>-<version>-<locale>.zip` and `<mod-id>-<locale>.version` for each locale in its `localisation/manifest.json`, with no unsuffixed zip. The default locale's version file is attached a second time as `<mod-id>.version`, which an install from before the change still polls, so no update check breaks. A caller already committing a manifest changes what it publishes on the release that moves its pin to this version.
- `StubbedGlobalLogger.answerLoggersOn()` is gone: a stand-in for `Global` is opened answering loggers through `openGlobalAnsweringLoggers()`, which a suite stubs the sector or the settings on, and an arrangement holding several seams hands it to `StaticSeams.holdSeam()`.
- `StarsectorFont.getNativeSize()` is gone, and the enum states no size: the size an atlas draws 1:1 at is read off the loaded face through `FaceLineHeightReader`. A value written down for vanilla's atlas draws a localised install's atlas under the same basename scaled - blurred, on a pixel face. A caller writes `InstalledFaces.createNativeFace(font)`.
- `TextStyle.createStyle()` takes a `TextFace` in place of a `StarsectorFont`, the style no longer knowing a native size to build one at. A caller that named a face alone passes `InstalledFaces.createNativeFace(...)`.
- `TextFace` holds a `FontAtlas` as `atlas()` in place of a `StarsectorFont` as `font()`, so a text can draw in the face the game's settings declare whether or not the enum names it. A caller reading `face.font()` reads `face.atlas()`, which answers the path and the smoothing it read before; a `StarsectorFont` is a `FontAtlas`, so every face built from one compiles unchanged. `LazyFontCache.loadByFace()`, `FaceLineHeightReader`, `GlyphCoverageReader`, `WidgetStyle.bodyFont` and the body face `StripTextMeasurers.loadFaceMeasurers()` takes are a `FontAtlas` likewise.
- The font package is split in three. `kmlib.starsector.ui.font` keeps the faces, the ports asked of them and the fallback rules, none of which loads an atlas, so `StarsectorFont`, `AtlasSmoothing` and `TextFace` stay where they were. `LazyFontCache` and `DrawableStringCache` move to `kmlib.starsector.ui.font.installed`, beside the other readers of the running game's atlases. `LineWidthMeasurer`, `TextSpanMeasurer`, `LazyFontMeasurer`, `LazyFontSpanMeasurer` and `StripTextMeasurers` move to `kmlib.starsector.ui.font.measure`, and the fixture `LineWidthMeasurerFake` to `kmlib.testfixtures.starsector.ui.font.measure`. A caller changes its imports; nothing else about the classes changed.
- `SortDirection.fromKeyOrDefault()` and `ListColumns.fromKeyOrDefault()` are gone: a stored key resolves through `PersistedChoices.fromKey(options, key, fallback)`, the one lookup every keyed option set shares. A caller reading a stored column count writes `PersistedChoices.fromKey(ListColumns.values(), key, ListColumns.DEFAULT)`.

### For developers

<details>
<summary>API, build tooling and test fixtures</summary>

#### Fixed

- **Shortcut key on a button:** `VanillaButtonLabel.announceShortcut()` widens the label to the words it carries. The game sizes a label once, to the words it was built with, and wraps anything longer.
- **Map layers under the speed-up:** `MapIconReseater` puts an icon back only once the widget has dropped it, bounded by `MAX_ADVANCES_DETACHED` advances and ended when the map closes. The speed-up advances scripts several times a frame, so a removal and a put-back landed in one frame and the widget never re-layered the icon.
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
  - `compose-locale-note`: the release body's *Builds by language* list, linking each locale's core localisation, and each translation's notes collapsed under its name. A translation missing the version's section fails the release.
  - `fill-version-file-template` takes an optional `locale`, so an install polls its own language's version file.
- **KMLib's own text per locale**: strings and settings under `localisation/<locale>/`, launcher text in `mod_info.base.json`.
  - `localisation/zh-hans/CHANGELOG.md` translates this changelog in full.
  - `LocaleParityIntegrationTests` holds every locale to the default.
  - The Chinese bundle's `README.md` is the base terminology reference each consumer mod's builds on.

#### Changed

- **Map cursor under Fast Rendering:** the modelview is read through `glGetFloat(GL_MODELVIEW_MATRIX)`, which `v0.9.1rc1` answers inline. KMLib compiles against no part of `fr.jar`, so a refactor inside Fast Rendering cannot break the read.
- **Mod list name:** a mod depending on KMLib may use the same name in its `dependencies` entry. The launcher shows it only when KMLib is missing.
- **`MapIconReseater` logs what it saw**, on KMLib's own logger. The package README lists the lines.
  - Map open and close are traced at DEBUG, each with the lift count since the icon was last seen clear.
  - Two states warn once per session: a map up with no placeable icon, and a stand-down, with the readings behind it.
- **`VoronoiCellBuilder` puts a corner adjacent cells share in the same place in both**, worked out from the two sites and the reach. Chained frontier edges meet without gaps, and the segment count sets only how smoothly an arc is drawn.
  - Cell vertices move by up to a chord's sagitta at those corners: about eight units at the shipped reach and default count. Re-baseline any assertion tighter than that.
- **The Starsector conventions read a mod's metadata through the game's `json.jar`**, as the launcher does, so the two agree on whether it parses. Configuring the build therefore needs the install.

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
