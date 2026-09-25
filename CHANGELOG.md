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

- **`MapIconReseater` lifts an icon under the campaign's speed-up.** With the speed-up toggled on, the campaign advances its scripts several times per rendered frame, map open or not, so a removal and a put-back on consecutive advances landed in one frame and the widget never rendered without the icon: every layer stayed under the nebulae on every open, unmoved by a save reload or a Starscape toggle. The put-back now waits until the widget has dropped the icon, bounded by `MAX_ADVANCES_DETACHED` advances and ended at once when the map goes down. - Reported by **MiniRockytheOracle** [at **USC**](https://discord.com/channels/187635036525166592/1549091275167240272/1551829173037957170).
- **A stood-down lift is tried again on the next map open.** The attempt bound abandons a lift for the rest of the open rather than for the session: the next open is a fresh widget with a fresh seeding, and a stand-down that outlived its cause read, from outside, as the lever having stopped working.

### Added

- **`VerticalRadioSpec`**: a column of option cells stacked top to bottom, one lit - the shape an option set of more than two or three reads as, where the same options laid across a row letter too narrow to tell apart. Carries no segment sizing and no trailing caption, both being row-only.
- **`RadioSpec`**: the sealed interface `HorizontalRadio` and `VerticalRadio` sit under, carrying the re-pick rule both answer. A reader acting on any radio names it rather than each alignment. `HorizontalRadio` is otherwise unchanged and every existing call site compiles as it stands.
- **`ScrollingSectionSpec`**: the run of a body that scrolls, holding any controls rather than being a property of one. A heading, the list under it and the row beside it now travel together inside the viewport the capped layout leaves them, where only a single list could scroll before.
- **`Control.isScrolled()`**: whether a laid-out control was placed inside that section - the one value the clipping renderer and the viewport-limited hit-test both read. A `Control` built without it is pinned, so existing three-argument construction is unchanged.
- **`MemoryKeyAddress`, `AddressedMemoryFlag`, `AddressedMemoryString`**: a stored value held once per point on an axis the consumer declares rather than once per save. The holder states its base key and names the address each read and write means; the key is composed in one place, so no holder can spell the segments differently or drop one and quietly share a slot with another.
- **`KmlibStringKeys.get()` and `format()`**: lookups bound to KMLib's own category, so a call site names a key alone rather than repeating the category beside it.
- **`KmlibStrings.requireText()`**: the blank-rejecting counterpart of `Objects.requireNonNull`, for a component that is a name or a sentence.
- **`GlMatrix.FLOAT_COUNT`**: the sixteen floats a GL matrix takes, stated once for every reader and writer that sizes a buffer or rejects a wrong-sized array by it. `ModelviewMatrixReader.MATRIX_FLOAT_COUNT` now reads off it.
- **`GlMatrix.createIdentity()`**: the matrix that transforms nothing, as its own array each call. Read for two unrelated reasons and previously written out for each: as the tell that a modelview describes no render pass, and as the base a transform is composed onto. Written as a diagonal rather than as sixteen literals, those being what a misread layout hides in.
- **`CampaignCountdown`**: a span of campaign days from a start timestamp, read against the running clock for the days left and for whether it is complete. It carries a completion slack, so a countdown driven by uneven frame steps can finish on the frame the player expects rather than a fraction of a day late, and every read of it agrees on when that is. The clock is handed to each read rather than held, so a countdown is plain numbers and names no sector. `BaseExpiringIntelPlugin` reads its window through one with no slack.
- **`Colonies.selectColonies(test)` and `Colonies.hasAnyColony(test)`**: the two walks over a colony set, stated on the set rather than hand-rolled beside it. The selection keeps the set's own order, which a consumer mirroring vanilla's tie rules settles a contest by; the emptiness read stops at the first colony that passes, being asked of every place in the sector on a scan and per frame while a map is drawn. An absent test passes nothing, which withholds rather than reporting a colony nobody asked to be shown.
- **`PersistedChoice` and `PersistedChoices.fromKey()`**: an option a save stores by a key of its own, and the one lookup that reads a stored key back to it - falling back where nothing is stored or no option answers to the key, whether left by an older build or written by another mod. The counterpart of `LabeledChoice`, for a key the option owns rather than a label LunaLib stores. `SortDirection`, `ListColumns` and `ListSortMode` are persisted choices.

#### Factions

- **`FactionAlliances`**: which factions stand together, as the alliance each allied faction belongs to, with `areFactionsAllied` over it and `buildFrom` to invert a list of records into it. Vanilla keeps no such arrangement, so this is the shape one arrives in whichever mod maintains it. It says who stands with whom and no more - what an alliance is worth is the consumer's, since keeping a secret, fighting a war and sharing a market read the same membership to different ends.
- **`AllianceRecord`**: one alliance flattened to plain data - its stable ID, its display name, and its members ranked by descending market size. Every field is a snapshot taken at read time rather than a live handle back into whatever maintains the alliance.
- **`AllianceSource`**: the port those records arrive through, so whatever folds or weighs them runs with no game around it. A port rather than a snapshot, alliances forming and dissolving in play.
- **`NexerelinAllianceSource`**: Nexerelin's live alliances as those records, behind the presence gate. The only code naming `exerelin.*` for them sits in a class of its own that the gate defers, not even reached by a method signature, so an install without the mod never seeks a Nexerelin class - and a consumer folds records without learning which mod produced them.

#### Geometry

- **`VertexWelder`**: which reports of a corner are one corner, by a tolerance, so edges computed apart can be compared as exact IDs rather than by distance at every hop. Promoted out of `EdgeRings`, where it had been private, once a second caller needed the same thing. Bucketed by a grid one tolerance across, so a lookup scans nine squares rather than the whole set, and the first report of a corner is the one kept - averaging would move a corner after edges had already been welded to it.
- **`Disk.measureSagitta(radius, segments)`**: how far the polygon approximating a disk falls inside it at its worst. The resolution anything drawn against that disk is really at, and so the figure a consumer welds by or discards small features by. It falls out of the radius and the segment count, so it is read rather than restated - four restatements of it across the consumer mods had to move together and did not. Static, taking the two: where the disk is centred has nothing to do with it, and every caller holding the two holds them as knobs rather than as a disk it could ask.
- **`Segment.readStart()` and `readEnd()`**: a segment's ends as `{x, y}` points, the one crossing between a value that names its ends by role and the point arithmetic beside it that takes arrays. Each read is its own array, so a caller keeping one as a corner cannot have it move under them.
- **`PolygonRegions.countSelfCrossings(ring)`**: how many times a ring crosses itself, which is whether it is drawable at all - a folded ring fills to something other than its outline and strokes a line through its own interior. A count rather than a flag, so a fixture that got worse can be told from one that was never clean. Consecutive edges are not asked about, sharing an endpoint by construction; two further apart that merely meet at a point do count, a ring pinched to touch itself being no more simple than one that passes through. Every pair is compared, so it costs the square of the ring's length and belongs in a test or a probe rather than in anything drawing per frame.
- **`PolygonOffsets.hasInsetCollapsed(rawRing, insetRing)`**: whether a miter inset folded a ring over rather than offsetting it - too few corners left, a vanishing or sign-flipped area, or an outer ring that grew. Promoted from a consumer's private check once a second body shaped by the per-edge miter needed the same verdict. Asked before any boundary resolve, because a tessellation handed a lone ring wound the wrong way hands it back re-wound as a fill rather than dropping it, holes and all; the fold can only be told while the raw ring is still there to compare against.

#### Third-party compatibility

A binding to third-party code that stops holding now costs the feature built over it for the session rather than the render pass or the load, and the player is told once, in-game, as a block of labelled rows naming the mod that lost something, both versions, what it costs and what it does not - under a heading that points at the log for the mechanics. Two kinds of binding report through the one channel: a call into Fast Rendering's bridge, and a start-up step that integrates with another mod. [`starsector/compatibility/`](src/main/java/kmlib/starsector/compatibility/README.md) sets out how it works and why it is shaped this way; the entries below say only what is new.

- **Built-against version stamped into the jar**: the build generates the Fast Rendering release the bridge adapter was type-checked against into the jar as a constant, so a mismatch report can name both halves of the disagreement. A build against the bridge stubs stamps no version.
- **`CompatibilitySubject`** and **`CompatibilityFailure`**: the third party a report is about with both versions, and one binding to it that stopped holding. The subject also reads how the installed version stands to the targeted one - behind, ahead, the same, or not comparable - by runs of digits, and answers only wording, so a self-report that lies costs a sentence rather than behaviour. A failure answers the player's notice as `CompatibilityNoticeLine`s: a heading naming which mod could not integrate with which third party, a diagnosis advising an update, a downgrade or a wait as the versions decide - and where the two name one release, asking for a report to the mod's own developer, there being no version to move to and a match being exactly what a fault in the integration survives - the rows, and a closing line pointing at the log. Each line carries its wording and the runs of it that stand out, in reading order. The log's block carries the same rows under the same labels in the same order, from literals. Four components of four distinct types, so no two can be transposed, and an unread version renders as an explicit unknown rather than as `null`.
- **`CompatibilityBreakage`**: which guard caught a binding and what no longer holds, as one value. Neither diagnoses anything alone.
- **`CompatibilityConsumer`**: the mod that took a binding - its mod ID, which of its features the binding serves, and the sentences naming what that feature loses and what it does not. The latch key is composed from the ID and the feature rather than supplied whole, so two mods cannot spell one key. Both sentences are the taking mod's, never the library's, and the ID is resolved against the mod manager only when a report is composed.
  - **`resolveConsumerAtPosition()`**: the same mod and sentences under the feature key numbered, which is what the record hands a describer where one mod filed two features under one key.
- **`CompatibilityFailures`**: the session's record, latched per third party, consuming mod and lost-feature sentence, written from any thread and drained by a reporter one failure at a time. The failure is built by a describer invoked only on the record that is kept, so a reflective probe or a version read is paid once per binding rather than once per frame.
  - **A feature key one mod reused reports both features**: the latch is the subject, the consumer key and the lost-feature sentence together, so a mod that spells one feature key for two features has both reported, the second under `<feature>-2`. The sentence is what separates a second feature from the same one recording again, and a numbered key in the log names the reuse where its author will see it.
  - **`CompatibilityFailures.SESSION_RECORD`**: the one record every binding writes into, held per session rather than per sector. `KMLib_ModPlugin` installs the notice that drains it on every game load.
- **`InstalledMods.readModName()`** and **`readModVersion()`**: the display name the game holds for a mod ID and the version that mod declares, or nothing where the game cannot answer. Guarded like `ModPresence` beside it, a caller asking either being one composing a report.
- **`WiringSteps`**: the guard one step of a mod's start-up wiring runs behind, in `starsector/startup/`. A step that throws costs its own registration rather than every step after it or every mod loading behind it. So does a step that cannot link what it binds to, which is how a third party's changed contract arrives: as a `LinkageError`, not an exception. Constructed with the wiring mod's own logger rather than holding one, because a level set through `KmLogging` scopes to a package subtree. One method runs a step that binds to nothing a player could act on; the other takes the integration and reports as well as logs.
- **`ModIntegration`**: a third-party mod a start-up step binds to and what the wiring mod loses where the step does not take, recording the failure it composes from what was thrown. The version pair runs the other way round from a binding to a renderer patch: nothing was compiled against an optional mod, so the built-for row stands at its unknown wording while the installed one is read off the mod manager. Supplied to the guard as a supplier, so the wording and the version read stay off the load path of every install where nothing broke.
- **`KmlibMod.MOD_ID`**: the library's own ID, held apart from the plugin so a class can say who it belongs to without loading a `BaseModPlugin` subclass for a string. The library files under it as a consumer of its own compatibility channel.
- **`NexerelinPresence.MOD_NAME`**, **`RandomAssortmentOfThingsPresence.MOD_NAME`**, **`KmlibLunaSettings.LUNALIB_MOD_ID`** and **`LUNALIB_MOD_NAME`**: each third party's identity beside the code that binds to it, so a record latched under an ID and a report naming the mod cannot drift into two mods. LunaLib's sit under `settings/` rather than `mods/`: that tree is for the mods a consumer may run without, and LunaLib is a declared dependency.
- **A start-up step that does not integrate is reported, not just logged**: `KMLib_ModPlugin` files its LunaLib settings binding, its Nexerelin routines and its Random Assortment of Things access routes through the compatibility channel under the library's own mod ID. Before this, a registration that threw was swallowed into the log, and a player who enabled a mod found out it had not integrated by playing a session without it. The compatibility notice's own install stays logged alone - a failure to install the reporter has nowhere to be reported to.
- **A failure found on a map screen is told there**: `ScreenCompatibilityNotices` stands a panel in the core UI's own widget tree on the frame the failure is found, and the dialog takes whatever it declines. A panel hung in that tree is advanced by the screen holding it, where a transient script on the sector is not advanced at all while a core screen is up - which is why a report raised from a map pass needs a surface of its own. Both drain the one record, so neither repeats the other and nothing is lost between them. Only the guards the game's own thread runs attempt a raise; a binding that fails on a deferred renderer's thread is left for the dialog.
  - **`CompatibilityNoticePanel`**: that panel. Built from the game's own widgets and stood up through `CoreUiOverlayPanels`, it paints its own backdrop and box, frames the box in the base colour the game frames its own dialogs with, and claims the events its widgets have not taken, because the game dims nothing behind a panel added this way. It fades in and out at the pace the game's own prompts do, so whatever thins itself underneath rides the same curve rather than being cut away and snapped back; it hands the screen back on the press and paints on for the length of the fall, claiming nothing for it. Four ways out - the button, escape, enter and space, matching the game's own one-button dialog, which binds both its keyboard confirm and its keyboard cancel to the single option.
  - **`ModalOverlays`**, in `starsector/ui/coreui/`: the overlays this library has raised over a core screen and that hold it while up. `CoreUiDialogView` reads it beside the game's own modal base, so anything that stands aside for a game dialog - a sidebar's input, a fade against a modal - stands aside for a panel of ours on the same read, with no change on its side. An overlay counts while it is still fading as well as while it holds the screen, so a rider has the curve all the way down.
  - **`OverlayPresence.isShowing()`**: whether any of an overlay is on screen, which is wider than whether it holds it. What rides a fade asks this; what routes input asks the flag.
  - **Emphasis in the notice is named, not marked up**: names, versions and the log's file name are brought forward, what is wrong warns - the phrase naming the failure, the state the install is in, and the instruction for fixing it - what goes on working regardless is set at ease, and everything else reads plain. A mod is brought forward wherever it is named, inside a warning included, so the instruction to downgrade or wait is split into runs rather than warned whole; the version inside it stays part of the warning, being what the player is told to move to rather than a party to the mismatch. Every run that stands out is a value filled into a template, so the composition already holds it and nothing parses the wording; a phrase meant to stand out has a string key of its own. Runs are given in reading order because the engine matches each from where the last one ended, which is also what lets a name appear once brought forward and again inside a warned phrase. No font the game ships is monospaced - in `victor14` an `i` advances two pixels where an `M` advances seven - so colour is what separates a row's answer from its label on screen.
- **`CompatibilityNotice`**: the transient per-frame script that drains that record and shows each failure as the game's own confirm dialog, carrying a single button, writing the report line before asking for the dialog. The dialog is sized off the longest shape the notice takes rather than by eye, having no way to grow to its content: an unreadable installed version, whose diagnosis runs to three lines, wraps to nineteen lines of the game's default font at that width. Too small silently cuts the closing line, which is the one pointing at the log. One dialog per frame. A confirm dialog rather than a message dialog, which is how the game puts up its own one-button notices and is the only one of the two that takes a size and answers whether it opened; a refusal is logged, so a modal the player never saw still leaves a trace.
- **`UnavailableModelviewMatrixReader`**: the third `ModelviewMatrixReader`, whose every read is no reading. Not a new caller contract: `CampaignMapTransform` already parks on an absent reading.
- **`FastRenderingBridgeDiagnostic`**: once a bridge binding has failed, which of the six mirrored members no longer hold and why, beside the version the installed jar reports and the one the build was type-checked against. Never runs on the healthy path.
- **Guarded binding in `ModelviewMatrixReaders`**: the Fast Rendering branch is taken under a `LinkageError` guard, which covers a class that moved, a member that moved and a signature that changed in one catch. A binding that no longer links now degrades to `UnavailableModelviewMatrixReader` and records one failure, where before it threw out of whichever render pass reached it first - killing the pass and naming KM classes in a trace the player then blamed KM for.
- **`FastRendering.COMPATIBILITY_SUBJECT_KEY`** and **`COMPATIBILITY_SUBJECT_NAME`**: the identity a failed binding to this renderer is recorded under and the name a report shows for it, published so that every binder spells them the same.
- **A bridge that breaks where it is called degrades too, not only where it is linked**: a Fast Rendering entry point that throws once the binding has linked used to reach the player as a fatal error - on the frame after, from a stack with no KM frame on it to blame, and out of the render pass itself where the game thread called it. Both sides now cost the map's cursor reading alone: the reading latches unavailable for the session, no stale matrix is reported in its place, and one failure is recorded however many frames the map stays open. This matters most from Fast Rendering `v0.8.9`, whose facade declares LWJGL's whole surface and refuses the parts it does not implement, so that breakage links cleanly and a link-time guard never sees it. The mechanism is set out in [docs/dev/rendering-environment.md](docs/dev/rendering-environment.md).
- **Each mod over a broken binding is told what it lost**: `ModelviewMatrixReaders.selectForActiveRenderer()` resolves per consumer, so a second mod reading the map is reported to rather than handed the first one's binding. Before this the first caller's choice was held for the session, and every later mod's player was told nothing.

#### Control rows

- **`InteractiveSpec.isSegmented()`** and **`reselectBehaviour()`**: what a control answers about itself, replacing two chains of type tests that each worked it out from outside. Whether a control's cells are hit separately and what a re-pick of a lit cell does are each one rule with two readers, the hit-test that resolves a cell and the narrowing that decides whether pressing it acts; stated on either side, a control would be hit as a row of segments and pressed as a whole row, or the other way about. Every interactive variant now answers both, so neither can be forgotten for one.
- **`RowDimensions`**: each row's height beside its width, as one value. Handed over as two lists they could arrive from different readings, two lists of different lengths or the heights of a run the widths were never measured from, and a stacker had no way to notice. Held together they are checked against each other where they are stated.
  - **`ControlStripLayout.StripMeasurement.rowDimensions()`** answers a measurement's rows in that shape.

### Test fixtures

- **List widget fixtures**: `Anomaly` and `AnomalySortMode`, a picker row and a sort vocabulary declared outside the list package, and `ListPickerBlockReads`, which reaches into a built picker block for the columns selector, the sort row, the sort selector or the item list. The block's order lives there rather than in each suite that tests a list, so a row inserted into it breaks one file.
- **`MemoryKeyAddresses`**: two stand-in addresses, for suites storing a value at one point on an axis without being about what the axis is.
- **`CompatibilityFailureFixture`** and **`CompatibilitySlotTemplates`**: one representative compatibility failure with a builder per slot a case varies, plus the two subject keys and the two consumers a suite records under - each consumer losing something the other does not, so a case about two of them being told apart cannot pass on one sentence standing for both - and the notice's templates as stand-ins that expose their slots, so a suite about the record, the notice or the wording names the one slot it is about.
- **`RendererModelviewMatrices`**: one map pass's modelview in each of the two layouts a renderer holds it in, with the pan and the column-major slots it lands in. One belief rather than one value - which slot carries the pan depends on which renderer was asked - so a suite restating it for itself was restating the thing the code under it exists to get right. `kmlib.opengl.FastRenderingTest` deliberately does not use it: that suite tests the transpose, so its input stays written out longhand beside the assertion.
- **`StarsectorSettingsFake.installSettingsWithModNames()` and `ModStateScopes.runWithModNamed()`**: a mod manager that knows mods by name as well as by enablement, for a subject that shows a mod's own name. With every ID answering nothing, the name a report found and the ID it fell back to are the same string, so the branch that matters could not be told from the branch that does not.
- **`TooltipMakerFake`**: a tooltip element that records the paragraphs, their spacing, the runs highlighted on each one's label and the buttons added to it, instead of laying anything out. A proxy rather than a hand-written stand-in, the element API being far too wide to implement for the handful of calls a body makes - which is what lets a suite about what a panel is made of run with no game around it.
- **`ShippedStrings`**: reads a mod's shipped `data/strings/strings.json` - the one path the engine reads, published as `STRINGS_JSON` - and the string IDs its holder class names, for the guard that holds those two together, and any strings file by category or with its categories flattened. The file is parsed through `ShippedJson`, so every key the game reads is read here, whatever its spelling, and a key declared in two categories fails the flattened reading rather than leaving the guard comparing against whichever came last.
- **`StringTemplates.countFormatArguments()`**: how many arguments `String.format` takes from a shipped template, for the guard that holds a template's slots to the call site filling them. A template that gained a slot renders as the fallback sentinel, and one that lost a slot renders with its last figure silently dropped, the formatter ignoring surplus arguments; neither shows in a suite that stubs the lookup. The count is the part that is easy to get subtly wrong: a positional specifier may repeat an index, so it implies the highest index rather than the number of specifiers, and `%%` and `%n` take no argument at all.
- **`SalvageEntityMock`**: vanilla's drop roller held still, owning the static mock's lifetime and capturing each roll's multipliers, drop lists and random source. Every capture pins the roll count as well, so code rolling twice where it should roll once fails the case rather than passing on the first roll's arguments. Holds the seven-argument roller only, a static mock stubbing each overload on its own.
- **`ShippedSpreadsheet`**: a mod's shipped CSVs, in whichever of four shapes a suite reads them - rows by header name, rows keyed by an ID column, one column's values, or lines of cells by position. Here rather than in each mod's suite because the parse is the part that can be subtly wrong: a shipped table quotes the fields carrying commas, and a reading that splits on the comma instead lands one column left of what it meant to read, on exactly the rows that quote, and keeps passing against the wrong cell. The positional reading is not a convenience but the only one some tables allow - LunaLib's settings table leaves several of its column names blank, and a parser asked to key on that header refuses the file outright. Blank-ID rows are dropped by the keyed readings as the spacing they are, and kept by the positional one, a caller reading by index judging its own rows. `LunaSettingsTable` reads through it rather than splitting lines itself. Apache Commons CSV comes with the fixtures variant as an `api` dependency, a consumer needing the parser on its own test runtime to read a row back. Deliberately no JSON shaping: a mod whose table parser takes the engine's row-object shape builds it from these rows in its own suite, so no mod reading a table needs `json.jar` on its test compile classpath.
- **`ShippedJson`**: a mod's shipped JSON read the way the engine reads it - the engine's `#` comment strip, copied character for character, in front of the org.json in the game's own `json.jar`. What the game loads reads here, trailing commas and comments included, and what it refuses fails here: a duplicated key, and a byte-order mark, named rather than reported as "must begin with '{'". Objects come back as sorted maps, the game's parser keeping no member order, and no org.json type crosses the boundary. Carries the shape checks a reading states its fields through, an unknown key among them, so a misspelt field fails rather than reading as absent, and names the file when a value built from it refuses what was read.
- **`StoredMemoryFake`**: the map-backed memory on its own, with nothing behind it. `SectorMemoryFake` reaches memory the way the game does, through `Global.getSector()`, and holds a static stand-in open for its lifetime to do it - which a suite that already holds its own stand-in for `Global`, or that hangs memory off a planet rather than the sector, cannot use at all, a second stand-in for one type throwing. Such a suite poses this and attaches the memory itself, and gets the same stored values and the same write and removal counts. `SectorMemoryFake` is built from it and answers every reading through it, so its own surface is unchanged.

#### Localisation

A mod's player-facing files can be kept per language, one bundle per locale under `localisation/<locale>/`, with a manifest naming the locales, the default and where each bundle file lands in the mod. These fixtures read that layout and compare its locales, and a build task writes one locale out of it. Every file is read the way the mod's shipped copy of it is read - JSON through `ShippedJson`, strings through `ShippedStrings`, a settings table through `LunaSettingsTable` - so a bundle is never parsed a second way.

- **`LocalisationDirectory`**: a mod's `localisation/` directory. Reads the manifest at its root, lists every bundle directory beside it whether declared or not, opens a bundle only for a locale the manifest declares, and reads the `mod_info.base.json` beside it where the mod commits one.
- **`LocaleManifest`**: which locales exist, which is the default, and which files a bundle holds and where each lands - the single source of truth for all three. Every key it may carry is known and any other is refused, so a misspelt `coreLocalization` fails rather than reading as absent. Its data paths are held inside the mod root however a manifest is built, a copy onto one being a write into the repository.
- **`DeclaredLocale`**: one declared locale - a lowercased BCP 47 tag, its name in its own language, and, where the vanilla atlases lack its glyphs, the https project its players install over `starsector-core`.
- **`LocaleBundle`**: one locale's directory. Reads its strings file, its settings table and its launcher fragment, and resolves any other bundle file by bare name only, so no bundle reaches into another.
- **`ModInfoFragment`**: the launcher text a locale translates - `name`, `description`, `author` and dependency names, each optional and falling back to the base file. Any functional field is refused, a fragment varying the version or the jar list building a different mod per locale. `listFallbackFieldNames()` names each field a fragment leaves to the base.
- **`ModInfoBase`**: what a fragment reaches of `mod_info.base.json` - the text fields it falls back to and the dependency IDs it may name.
- **`LocaleParity`**: holds every locale to the default, each check answering findings worded as the edit to make. Strict where a gap has no fallback - a missing directory, file, string or row, a slot taking another argument, a row varying what it stores, a tab split or merged, text outside Latin-1 with no `coreLocalisation` named, a fragment naming an undeclared dependency - and reporting the launcher fields a locale leaves to the base. A Radio's options are held identical, LunaLib storing the label picked. Each defect is found once, and a file the manifest does not map is not compared.
- **`LunaSettingsTable.readBehavioursByFieldId()`, `readDisplayedTexts()` and `readTabsByFieldId()`**: a row split into what it does and what it says - its `FieldBehaviour` of type, stored default, options and bounds, which `describeDifferencesFrom()` compares column by column, the cells the screen draws, and its tab. A field ID declared twice fails a keyed reading rather than being dropped from it.
- **`StringTemplates.readArgumentConversions()`**: which argument each slot of a template takes and as what, whatever order the slots are written in - what two wordings of one string must agree on for one call site to fill both.
- **`ShippedJson.requireList()` and `locateElement()`**: the array counterparts of the object check and the member location.
- **`writeLocaleFiles`**: writes one locale's bundle into the files the game reads, registered by the shared Starsector conventions for a mod that commits `localisation/manifest.json`. `-Plocale=<tag>` selects, and the manifest's default is built otherwise. Each mapped file is copied byte for byte, and where the mod commits `mod_info.base.json` the locale's launcher fragment is merged over it - text fields only, any functional field refused. A mapping outside the mod root and a bundle missing a mapped file both fail the build. The manifest and the fragments are read through `shipped-json-reader.gradle`, the build-time counterpart of `ShippedJson` over the same `json.jar`, so the build and the checks agree on what parses. `jar` depends on it and `test` takes its outputs as inputs, so a locale switch re-runs both.

### Changed

- **`MapIconReseater` says in the log what it saw.** A layering that degrades over a session cannot be diagnosed from the picture, and the moves alone do not say why one stopped taking. The map coming and going is traced at DEBUG, each edge carrying the count of lifts the icon has not been seen clear since and, on the close, whether it was seen clear at all while the map was up. Two states are reported at WARN, each once per session so a failure that does not heal costs one line rather than one per open: a map that stays up with the entity in its location and no icon placeable for it, the map read and the placement read then disagreeing about what is on screen; and the stand-down, carrying the readings that led to it so it says whether the lifts were never observed or observed and undone. All on the library's own logger, which `KmlibLunaSettings` binds to KMLib's verbosity field; the package README lists the lines.
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
- `SortDirection.fromKeyOrDefault()` and `ListColumns.fromKeyOrDefault()` are gone: a stored key resolves through `PersistedChoices.fromKey(options, key, fallback)`, the one lookup every keyed option set shares. A caller reading a stored column count writes `PersistedChoices.fromKey(ListColumns.values(), key, ListColumns.DEFAULT)`.

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
