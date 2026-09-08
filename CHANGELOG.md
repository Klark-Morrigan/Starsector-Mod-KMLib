# Changelog

All notable changes to KMLib are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/) and the project
adheres to [Semantic Versioning](https://semver.org/). Versioning triggers
for KMLib and its consumer mods are defined in
[docs/dev/versioning.md](docs/dev/versioning.md).

The reusable release workflow extracts the section matching the released
version into the GitHub release body, so every released version must have a
section here.

## Index

- [0.1.0](#010---2026-08-30)

## [0.1.0] - 2026-08-30

First tagged release, so there is no prior version to diff against: this is
the whole public surface - the console commands a player types, then one entry
per package naming what it offers. A test fixtures variant ships alongside the
jar for consumers' own suites.

### Added

#### Console commands

Seven campaign-only commands, registered through `data/console/commands.csv`
and reached only where Console Commands is installed. Arguments, defaults and
the cases each refuses are in that file, which is what `help <command>` prints
in-game.

- `kmlib_activate_gate` - activates a gate by id.
- `kmlib_colonise` - founds a colony on a body carrying only survey data.
- `kmlib_list_factions` - every faction with what it holds and where.
- `kmlib_list_map_spoilers` - faction-owned systems as a tree, flagging
  cut-off systems and undiscovered markets.
- `kmlib_list_system_entities` - the current system's entities as an orbit
  tree, then its unorbited ones and fleets.
- `kmlib_spawn` - spawns a gate or a jump point at the fleet position.
- `kmlib_transfer_market` - hands an existing colony to another owner.

Both colony commands defer to Nexerelin's own colonisation and transfer where
that mod is enabled.

#### Game-agnostic helpers

No Starsector API on the signature.

- **`kmlib`** - the mod plugin entry point; at application load it installs
  KMLib's own LunaLib settings bindings, registers the optional-mod
  integrations, and states which GL renderer every KM draw call reaches. Each
  step is guarded, so a failure costs its own step rather than every mod
  depending on the library.
- **`kmlib.animation`** - stepped envelopes the caller advances each frame
  (pulse, held pulse, keyed pulse sets, eased fraction) and read ones that are
  a function of the instant they are asked at (phase clock, per-subject phase,
  steady-beat and SOS patterns), over shared rise and fall durations.
- **`kmlib.collections`** - joining an iterable into a delimited string
  through a per-element formatter.
- **`kmlib.colour`** - AWT Color to normalised GL channels folding in an alpha
  multiplier, plus darkening, RGB and full blends, flattening onto a backdrop,
  and additive overlay and light.
- **`kmlib.extensions`** - a named extension point one implementation
  registers into, the last registration winning, with a sealed
  executed-or-declined outcome and whether the caller may fall back to library
  defaults.
- **`kmlib.input`** - rising-edge click detection, for polled input with no
  discrete event to consume.
- **`kmlib.logging`** - log4j level control over one mod's package subtree
  bound to a LunaLib setting, and warn-once-per-session reporting.
- **`kmlib.math.easing`** - ease-in-out over the unit range.
- **`kmlib.math.geometry`** - 2D value types (rectangle, disk, segment,
  directed line, half-plane, bounds, principal axis) and the polygon passes
  over them: convex and mitre inset, selective edge inset, corner rounding,
  spike removal, signed area and point containment, ring grouping into regions
  with holes, Voronoi cell building, polyline stroking to triangles, edge-ring
  chaining, inset ring tracing with clear-arc search, angle and span
  arithmetic, and the degenerate-shape thresholds they share.
- **`kmlib.math.hashing`** - avalanche bit mixing, multi-part content
  fingerprints, and the fixed share of the unit range a name holds.
- **`kmlib.math.motion`** - which keyed positions moved between two
  observations, over a movement threshold.
- **`kmlib.math.random`** - bounded jitter around a value.
- **`kmlib.math.ranges`** - clamping to the unit range or into arbitrary
  bounds.
- **`kmlib.math.solving`** - bisection to the largest value passing a monotone
  predicate, with the step count a tolerance needs, and picking the higher of
  two by a score.
- **`kmlib.opengl`** - primitive emission (lines, dashed segments, quads,
  triangles, vertex runs), the saved-state scope a blended 2D pass draws
  inside, blend modes and texture filters, polygon tessellation to triangles
  or boundary loops including intersections, viewport and scissor reads, and
  whether Fast Rendering is in force plus how to read its matrix.
- **`kmlib.opengl.hatch`** - hatch fills across a polygon, with a tally of how
  cleanly the runs join.
- **`kmlib.profiling`**, with `snapshot`, `recording` and `report` under it -
  the profiler a mod binds to hear what the library measures, silent until one
  does: named sections opened as nesting scopes or collected by measure or
  record, the counters a scope tallies, the tag a caller names one call by, and
  the sections whose calls run a loop, declared with the steps one turn is
  split into and opened as a scope that counts the turns and charges each step
  to its slot. A root names the origin it and everything under it was measured
  in, and a section opened under no root lands in a reserved group rather than
  being dropped. A shared read holding no scope of its own counts onto whatever
  section is open, and onto a reserved row of that group when none is. A
  section may state what one of its calls is allowed - an amount of a counter,
  a duration the caller states as the call closes, or both - and a call that
  breaks it marks the row, takes the row's worst-call record whatever it took,
  and is reported once. A capture comes back as a tree of rows per origin, each
  row carrying its count, total, min, max, average and self time, its counters
  with their spread per call, its worst call kept with that call's tag and
  counters, what it broke of what its section allows, what its loops ran per
  step and per turn with the slowest turn named, and where its calls fell
  across duration bands doubling from a microsecond up - rendered as an
  indented table, one group of roots per origin.
- **`kmlib.settings`** - LunaLib settings read and write, immediate and
  deferred with flush and removal, change callbacks, and resolving a labelled
  choice back from its label.
- **`kmlib.text`** - number formatting (scientific, signed delta, a signed
  reading on a scale whose middle is a real position and so draws unsigned,
  grouped integer, compact decimal) and the reads over a string every surface
  shares:
  is there text here, what are its words, initials, whole-word search, and
  dropping the stutter left where one phrase was appended to another ending on
  the same word.
- **`kmlib.time`** - nanosecond conversion to microseconds, milliseconds and
  seconds and back, and the duration formats a diagnostic line prints.

#### Starsector-facing wrappers and seams

- **`kmlib.console`** - the Console Commands base class, and the seven command
  implementations listed above.
- **`kmlib.console.input`** - one command invocation, its context, argument
  text and output channel, with the campaign and star-system requirements a
  command states before parsing.
- **`kmlib.console.output`** - where a command's messages go, behind an
  interface so a test can take them.
- **`kmlib.console.parsing`** - declarative parameter specs with required and
  defaulted parameters, typed value parsers (text, decimal, non-negative
  decimal), and a parsed result reporting validity and which parameters were
  supplied.
- **`kmlib.console.targets`** - what a command was pointed at, found or
  refused with a reason under one sealed answer: a market by id or the nearest
  one meeting a stated requirement, its owner, and the faction the command
  acts for.
- **`kmlib.console.validation`** - the campaign and star-system checks a
  command runs before it does anything, with the player-facing feedback they
  print.
- **`kmlib.mods.consolecommands`** - Console Commands as something to stand
  down for: whether the mod is enabled, and whether its overlay is taking text
  entry this frame, behind a fail-open reader.
- **`kmlib.mods.nexerelin`** - founding a colony, handing one over, and
  applying submarkets through Nexerelin's own rules, registered with the
  market operations at load and only where the mod is enabled.
- **`kmlib.mods.rat`** - Random Assortment of Things: Abyssal Fracture
  matching registered as a means of arrival, and the campaign-minimap role
  answered for its mini-map.
- **`kmlib.starsector`** - describing a sector to a reader who has to match it
  back to a save: its seed with the player's name beside it, the pair a save
  browser shows, and the seed alone where there is no player yet. Plus what the
  sector's shared reads report having traversed - the walks they made, and the
  systems, markets, entities and colonies those walks touched - counted where
  the sector is walked and charged to whichever section the caller had open.
- **`kmlib.starsector.colonies`** - the shared colony set every "who is here"
  read selects through, stated once over a location and read per kind of place
  above it: a star system, hyperspace, and the whole sector. Each colony
  carries its market and the listing it was found in and answers concealment,
  discovery and ownership; what may be shown of the set is the consumer's,
  handed in through a known-colony reader.
- **`kmlib.starsector.entities`** - spawning orbiting custom entities and jump
  points, circular orbits with vanilla jitter and focus-chain reads, jump
  point name generation, gate activation, and the nameplate pairing an
  entity's name with the map glyph it is marked with.
- **`kmlib.starsector.factions`** - player-faction established check, asked of
  the running game or of a named sector, and display-name normalisation across
  vanilla and Nex defaults, faction primary and secondary palettes, the neutral
  colour, crest paths, and the territorial flag.
- **`kmlib.starsector.fleet`** - whether the player fleet is in orbit of a
  planet.
- **`kmlib.starsector.geometry`** - distance and bearing between campaign
  entities, and how a nearest search settles an equal distance; the game-typed
  sibling of `kmlib.math.geometry`.
- **`kmlib.starsector.graphics`** - sprite lookup by path.
- **`kmlib.starsector.intel`** - intel-plugin base classes: a tab-tag mix-in,
  with auto-removal and active-instance lookup layered on top.
- **`kmlib.starsector.map`** - which systems the sector map marks with a star.
- **`kmlib.starsector.markets`** - market reads that never change one:
  ownership, settlement, military and station status, stability, submarket
  plugins and nameplate; visibility as a colony or an ungoverned one and
  player discovery; patrol counts by size; decivilised and revealed
  decivilised worlds, and what a bare sighting of one is worth against the
  survey bar; colocation and largest-per-faction selection; and the
  searches over a location, including the nearest market meeting a predicate.
- **`kmlib.starsector.markets.colonisation`** - whether a body carrying only
  survey data can be colonised, the owner-neutral founding sequence, the owner
  it is founded under, and the register whatever colonisation this install
  supplies is offered the founding through.
- **`kmlib.starsector.markets.ownership`** - what holding a colony makes true
  of it (flag, submarkets, tariff), handing an existing colony to another
  owner, and the registers a mod's own transfer and submarket rules install
  into.
- **`kmlib.starsector.memory`** - typed sector-memory accessors for flags and
  strings, over the raw memory read.
- **`kmlib.starsector.relation`** - where a faction stands with the player as
  one value (level, signed reputation, and the colour the game paints them in,
  off a single read walking its own three-tier colour fallback), a faction with
  no standing reported by no value rather than by a nought; that standing
  worded the way the engine words it; and whether a disposition clears the step
  from indifference to goodwill - asked of a faction in hand, or as a pair test
  over faction ids bound to one sector.
- **`kmlib.starsector.scripts`** - registering an every-frame script on the
  sector only when one of its type is not already there.
- **`kmlib.starsector.settings`** - whether a mod is enabled, answered the
  same way for every optional-mod gate and answering "not installed" before
  the game is up; and the common-data folder a preference about the interface
  is kept in, which is per user and per install rather than per save. A port,
  so what keeps the file is the caller's to choose, and one that fails open at
  both ends - an absent, unopenable or hand-edited file answers nothing, and a
  write that will not land is reported rather than thrown.
- **`kmlib.starsector.strings`** - settings.json lookups reporting a loud
  REDACTED on a missing or malformed entry, formatted lookups filling numeric
  slots, and the truncating percent those slots use.
- **`kmlib.starsector.systems`** - star system queries (display name, stars,
  centremost star, markets, nearest market, reachability, claim override,
  entity search by id), sector-wide indexing and hyperspace positions, the
  per-pass colony index so a system is walked once however many readers ask,
  movement tracking across frames, and the register of arrival routes the
  engine does not model.
- **`kmlib.starsector.systems.claims`** - the claiming faction behind a port,
  and the scored contest behind a second one: per-market breakdowns down to
  size, siblings, military bonus and admission, and standings that state their
  own kind - weighed on the market the mechanic scored, or presence-only at a
  nought - with a claim by decree reported separately.
- **`kmlib.starsector.time`** - campaign clock constants.
- **`kmlib.starsector.ui.buttons`** - the words on a button the game built,
  reached and written for a caller decorating a widget it did not draw. The
  published text accessors on a button serve one of the several kinds the game
  builds and quietly do nothing for the rest, reading back null and writing
  nowhere, so the words are found instead as the published label the engine
  draws them with - through the two accessors the game leaves unobfuscated, and
  accepted only where the label already reads what the button was built with,
  since a widget holds several and writing into the wrong one changes nothing
  anyone can see. What it says there is the game's own rule for a key: the key's
  name lit where the words already hold it, spelled out after them where they do
  not. Not finding the words costs what was going to be said and nothing else.
  Beside that, the ID a caller put on a widget found again in the pair of
  objects the game hands an action listener: the API names neither position and
  which one carries the ID depends on the widget, so it is looked for handed
  over directly in either and carried by a button in either. A reader that
  committed to one position answers some of its controls and silently drops
  every press from the rest.
- **`kmlib.starsector.ui.colour`** - the UI palette enum resolving through
  Misc, and the dark, base and bright accent triple a look is built from.
- **`kmlib.starsector.ui.controls`** - declarative control specs - label,
  checkbox, toggle, horizontal radio, tabs, vertical table, side-by-side
  columns, divider - with their actions, reselect behaviour and segment
  sizing, the hover and press sources a body resolves per cell, and the hover
  report a control tells its host which cell the pointer is on through -
  carried by the vertical table, defaulted to reporting nowhere elsewhere.
- **`kmlib.starsector.ui.coreui`** - name-based reach into the game's concrete
  UI classes: child walks, offered hops, no-arg and argument invokes, showing
  checks, the core UI behind a host, the current tab, the campaign screen's
  shown tab, and what a modal a core screen has raised in front of itself is
  doing - whether it is up, which the published dialog state never reports,
  that answering for the campaign's own conversations, and how far through its
  own fade it stands, so a caller can thin out against its backdrop rather than
  cut away from it. Beside that, whether the codex stands over the screen - a
  reading of its own rather than a case of the modal, the codex being raised
  outside the core UI entirely, into a second screen panel the campaign state
  keeps for it, so no walk of the core UI reaches it however deep it goes. It is
  read off the app state instead, which reports the codex up whichever way it
  was raised, over a screen or from the campaign itself. Presence only: the
  codex fades in over a few tenths of a second, but on a panel this never walks
  to, and at that length a caller standing down at once does not read as a cut.
  And a shape-based reach
  for the members no name can
  find: what a class declares and what it publishes, each member's parameters,
  return type and a way to call it, so a caller can recognise an obfuscated
  member by its signature rather than by a name the next game build regenerates.
  The reach itself is deliberately policy-free - a hop either answers or throws,
  and what a failure means is the caller's to decide.
- **`kmlib.starsector.ui.debug`** - a quadrant-anchored on-screen debug HUD,
  drawn at the corners or around the cursor.
- **`kmlib.starsector.ui.font`** - the face enum every caller names an atlas
  through, the font and glyph-run caches behind it, and line and span width
  measurers including the lowercase band drop.
- **`kmlib.starsector.ui.highlight`** - highlights, paragraphs and messages
  that render to a text panel, a tooltip, a label, or a MessageIntel.
- **`kmlib.starsector.ui.input`** - pointer and key controllers driving panel
  and tab-panel state (scroll, drag, collapse, hotkey blink), hover arrival
  and fades with keyed variants of both, cursor position and button-hold
  ports, pointer event claiming, the hovered tab, body cell and notch a
  frame resolves, and the report of that body cell back to the host that
  built the control - once per change, and once more as the pointer or the
  panel leaves.
- **`kmlib.starsector.ui.intel`** - obf-cast seam onto the intel screen:
  whether its tab is open, the map visor rect and widget, and that map's
  starscape flag. Fails closed.
- **`kmlib.starsector.ui.label`** - label length estimation from a measured
  font or a bare aspect ratio, line wrapping, and fitting the largest box or a
  band inside a region chord under name and band specifications.
- **`kmlib.starsector.ui.layout`** - pure placement maths: padding, row
  stacks, control strips and their measurement, height-capped strips with a
  flex region, panel and tab-panel placement, tab header layout - including the
  panel's own band button, laid where the tabs leave off so the band grows by
  one box, at its own look but pinned to the band the panel was given - tooltip
  box placement and the height a box wraps a content stack to, and vanilla
  PositionAPI to rectangle. It is also where a scrollbar's gutter is spent: a
  capped strip knows what its own padding already holds clear, so a bar no
  fatter than that costs the body nothing, while a fatter one widens the body -
  and the box framed around it - by the excess, leaving the rows at the width
  they measured to.
- **`kmlib.starsector.ui.map`** - how readable a map icon is under the nebulae
  drawn over it.
- **`kmlib.starsector.ui.map.controls`** - the writes into the map screen's own
  furniture, sitting above the reads in `map.probes` and the one map package
  that names another's classes, since a write has to be aimed by a reading.
  `MapFilterRows` reaches the row of toggles a map is furnished from - the `M`
  screen's strip and the intel screen's map visor alike, both being one widget
  reached by one accessor off the map itself - and hands it back as a
  `MapFilterRow`, a handle that answers which row it is, and where it and the
  last button on it were laid out, rather than the widget itself.
  `MapFilterToggle` stands one more toggle at the end of such a row, sized off
  the row so one path serves both screens, and declines a row it cannot measure
  or that has no room left. What comes back reads and sets the button, hangs
  the hover tooltip the row's own buttons carry, binds the key that ticks it -
  live wherever the button is on screen and nowhere else, and unbound rather
  than bound to the cleared code - says that key in the button's own words
  through `ui.buttons`, and says whether it is still standing on the row
  currently on screen.
- **`kmlib.starsector.ui.map.icons`** - reseating a map icon once the layering
  over it settles.
- **`kmlib.starsector.ui.map.presence`** - which campaign map surface is up
  and in what mode, folded across hosts, plus the campaign-minimap role for a
  surface that replaces the radar rather than opening as a screen.
- **`kmlib.starsector.ui.map.probes`** - obf-cast reads of the live map: the
  shown map tab, its surface area and chrome boxes, embedded maps and their
  drawn boxes, icon layering, the vanilla tooltip, and traces describing
  hosts, icon order and the widgets under the cursor.
- **`kmlib.starsector.ui.map.transform`** - the campaign map's screen-to-world
  transform captured during a map pass, cursor unprojection, and the modelview
  matrix readers behind them for both the GL and Fast Rendering paths.
- **`kmlib.starsector.ui.render.gl`** - the GL drawing surface: quad, additive
  quad and triangle fills, box borders, sprites, scissor push and pop with
  clipped runs, label rendering under an atlas filter, text anchors, direction
  triangles, and the paint - colour with alpha - a caller hands them.
- **`kmlib.starsector.ui.render.gl.controls`** - painting a control: checkbox,
  toggle button, horizontal and vertical radio rows, icon radio lists, segment
  washes, seam dividers and channel fills, over per-cell hover wash and press
  light sources. A control is drawn against the panel's alpha pair rather than
  one compounded number, and spends the two apart: its chrome honours the look's
  translucency and the moment, its words the moment alone, so a see-through body
  never costs a coloured reading its colour.
- **`kmlib.starsector.ui.render.gl.panel`** - painting a panel: bordered box,
  scrollbar, and the collapse notch with its chevron, all against the alpha pair
  it hands on to each body control.
- **`kmlib.starsector.ui.render.gl.style`** - the look a host hands in: box
  colours, accents, hover wash and press light resolved to a paint at a
  fraction, notch colours, body font, tab style and sound scheme, gathered
  into one widget style - restatable with a different tab style in it, so a
  panel drawing two tab-shaped things in different chromes draws both through
  the one control renderer.
- **`kmlib.starsector.ui.render.gl.tabs`** - painting both tab chromes,
  vanilla strip and raised button, the panel they head, and centred tab
  labels, behind one chrome-selecting renderer. Each surface of a tab panel
  takes its own channel from the panel's alpha, so a row opaque over a
  translucent body still fades out with the panel around it. The panel's own
  band button is painted with the tabs as one piece of chrome - same clip, same
  state save, same alpha - but in its own tab style, and with its image drawn
  into the tab it stands as rather than over the whole band, which would cover
  the line a chrome keeps under its tabs.
- **`kmlib.starsector.ui.render.gl.tooltip`** - painting a cursor tooltip and
  the leader lines ruling its rows, with redaction darkening, and the box
  height and screen budget a caller weighs its content against before the box
  is drawn - or one draw that fits the box to the screen on the way in.
- **`kmlib.starsector.ui.screen`** - the UI screen box, its width and height,
  and the pixel-to-UI axis conversions over it.
- **`kmlib.starsector.ui.sound`** - the engine's interface sounds a KM control
  answers with, the cue binding one to a volume, per-arrival-target volumes,
  vanilla and silent schemes, and the player port behind them.
- **`kmlib.starsector.ui.suppression`** - hiding a widget while it sits off
  the surface it belongs to.
- **`kmlib.starsector.ui.text`** - substrate-neutral text look (face, colour,
  casing, alignment) and the runs a label is built from: text spans, image
  spans, and redacted spans laid out as word bars, with run offsets, widths
  and joined-run handling.
- **`kmlib.starsector.ui.tooltip`** - attaching a vanilla TooltipMakerAPI
  tooltip to a component, either from a surface the caller already holds or
  from one made for the call, for a caller decorating a widget it did not
  build and so holding no surface of its own.
- **`kmlib.starsector.ui.widgets`** - widget geometry over one shared
  labelled-row core: bordered box content bounds, checkbox tick boxes,
  icon-label rows with trailing slots and direction triangles, radio grids and
  segments, the row slots a label carries (text, runs, image, tick, triangle,
  empty) with the painter role a surface draws them through - a method per kind,
  so a kind added later stops every painting surface from building rather than
  leaving one column silently unpainted - box borders per edge, panel placement
  with its scroll region, what a panel spends on chrome rather than on content
  as one value - the border around its footprint and the thickness of the bar
  its body keeps a gutter clear for, together because both are room reserved
  before a control is placed and a panel is never laid out knowing one without
  the other - and the
  two alphas a panel paints at - how see-through its body is meant to be, and
  how much of the panel is on screen at all - carried together because chrome
  standing opaque on that body still has to leave with the panel, and asked for
  separately by what honours the look and what honours only the moment: a
  panel's chrome takes both channels, while its words take the moment alone, so
  a see-through body never costs the reading its colour.
- **`kmlib.starsector.ui.widgets.lists`** - the spotlight picker: sort modes
  with direction, comparator and the trailing value a ranked row draws for them
  - answered as runs, so a value the engine gives a colour to, or a range whose
  two ends read differently, keeps its own shades inside the one column -
  column-count and sort selectors, the item
  seam rows are drawn from, a consumer's live read of its three stored picks
  as one reading, the store it persists those picks into and hears the row
  under the pointer through - named by the item it stands for, so a host can
  preview what picking it would do - and a revision memo holding a built list
  until its inputs change.
- **`kmlib.starsector.ui.widgets.scroll`** - scroll offset state with
  clamping, and scrollbar track, thumb and grab-column geometry over a scroll
  region or a panel placement, at a bar thickness the caller sets rather than
  a width the geometry holds - carried with the two fixed gaps flanking the
  track, so the one dimension a bar is judged by varies while its spacing does
  not, and stating the gutter a container reserves for it. A width of nothing
  takes the bar away rather than drawing one of no width - no track, no thumb,
  and nothing claiming presses over that inset - answered as one reading on the
  placement so the pass that draws the bar and the pass that grabs its thumb
  cannot disagree about whether there is one. It is a reading apart from whether
  the list overruns at all, which stays the wheel's question: the wheel is how a
  list with no bar is moved.
- **`kmlib.starsector.ui.widgets.segments`** - splitting a row into segments
  under uniform, snapped or fixed sizing, and the dividers and channels
  between them.
- **`kmlib.starsector.ui.widgets.tabs`** - tab strip geometry for both
  chromes, collapse with a docked start, hotkey lookup, shortcut text runs,
  and the hover, pulse, look, wash and light sources a tab header and its
  panel read. A panel's band travels as one value - the row's look, its tabs,
  and the panel's own button after them - because a layout can use none of the
  three alone. That button is the panel's own chrome rather than any tab's, so
  a press on it fires the panel's action and moves no selection; it is laid and
  hit through the same one-cell tabs geometry a tab is, carries a look of its
  own (usually the row's with only the box changed, a button being as wide as
  the one thing it shows), and may show an image in place of a word. It sits
  outside the tabs control on purpose: a cell in that control which is not a tab
  would shift every index the selection, the lit tab and any bound keys are
  resolved by.
- **`kmlib.starsector.ui.widgets.tabs.style`** - the tab look a host varies:
  chrome choice, tab box sizing, palette with hover and click states, hotkey
  underlining, text halo, and the vanilla tab and button fills a glow is
  resolved against. A look can be restated in a different band or a different
  tab box without its other six components being rebuilt by hand, which is what
  lets one row's chrome and colours be worn at a width of their own.
- **`kmlib.starsector.ui.widgets.tooltip`** - cursor tooltip content and
  layout: table and centred rows carrying crests, values and indentation,
  nested sections, per-level line gaps and section breaks, the header,
  paragraph and footnote styles they read in, how tall those blocks stand
  answered on its own, before a box is laid out anywhere, and the fit that
  answers an overflow by giving up size rather than lines - re-anchoring the
  level shrink at the deepest line shown, so it keeps its size while the tiers
  above it draw smaller and closer together.
