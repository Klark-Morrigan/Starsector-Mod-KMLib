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

- [Unreleased](#unreleased)
- [0.1.0](#010---2026-08-30)

## [Unreleased]

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
- **`kmlib.profiling`** - named section timings collected by measure or
  record, their count, total, min, max and average snapshot, a formatted
  report, and nanosecond conversions.
- **`kmlib.settings`** - LunaLib settings read and write, immediate and
  deferred with flush and removal, change callbacks, and resolving a labelled
  choice back from its label.
- **`kmlib.text`** - number formatting (scientific, signed delta, grouped
  integer, compact decimal) and the reads over a string every surface shares:
  is there text here, what are its words, initials, whole-word search, and
  dropping the stutter left where one phrase was appended to another ending on
  the same word.

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
- **`kmlib.starsector.factions`** - player-faction established check and
  display-name normalisation across vanilla and Nex defaults, faction primary
  and secondary palettes, the neutral colour, crest paths, and the territorial
  flag.
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
  decivilised worlds; colocation and largest-per-faction selection; and the
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
- **`kmlib.starsector.relation`** - the player relationship formatted as the
  game itself shows it, description and colour, and whether a disposition
  clears the step from indifference to goodwill - asked of a faction in hand,
  or as a pair test over faction ids bound to one sector.
- **`kmlib.starsector.scripts`** - registering an every-frame script on the
  sector only when one of its type is not already there.
- **`kmlib.starsector.settings`** - whether a mod is enabled, answered the
  same way for every optional-mod gate and answering "not installed" before
  the game is up.
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
- **`kmlib.starsector.ui.colour`** - the UI palette enum resolving through
  Misc, and the dark, base and bright accent triple a look is built from.
- **`kmlib.starsector.ui.controls`** - declarative control specs - label,
  checkbox, toggle, horizontal radio, tabs, vertical table, side-by-side
  columns, divider - with their actions, reselect behaviour and segment
  sizing, and the hover and press sources a body resolves per cell.
- **`kmlib.starsector.ui.coreui`** - name-based reach into the game's concrete
  UI classes: child walks, offered hops, no-arg and argument invokes, showing
  checks, the core UI behind a host, the current tab, the campaign screen's
  shown tab, and whether a core screen has raised a modal in front of itself -
  which the published dialog state never reports, that answering for the
  campaign's own conversations. The reach itself is deliberately policy-free -
  a hop either answers or throws, and what a failure means is the caller's to
  decide.
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
  ports, pointer event claiming, and the hovered tab, body cell and notch a
  frame resolves.
- **`kmlib.starsector.ui.intel`** - obf-cast seam onto the intel screen:
  whether its tab is open, the map visor rect and widget, and that map's
  starscape flag. Fails closed.
- **`kmlib.starsector.ui.label`** - label length estimation from a measured
  font or a bare aspect ratio, line wrapping, and fitting the largest box or a
  band inside a region chord under name and band specifications.
- **`kmlib.starsector.ui.layout`** - pure placement maths: padding, row
  stacks, control strips and their measurement, height-capped strips with a
  flex region, panel and tab-panel placement, tab header layout, tooltip box
  placement, and vanilla PositionAPI to rectangle.
- **`kmlib.starsector.ui.map`** - how readable a map icon is under the nebulae
  drawn over it.
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
  light sources.
- **`kmlib.starsector.ui.render.gl.panel`** - painting a panel: bordered box,
  scrollbar, and the collapse notch with its chevron.
- **`kmlib.starsector.ui.render.gl.style`** - the look a host hands in: box
  colours, accents, hover wash and press light resolved to a paint at a
  fraction, notch colours, body font, tab style and sound scheme, gathered
  into one widget style.
- **`kmlib.starsector.ui.render.gl.tabs`** - painting both tab chromes,
  vanilla strip and raised button, the panel they head, and centred tab
  labels, behind one chrome-selecting renderer.
- **`kmlib.starsector.ui.render.gl.tooltip`** - painting a cursor tooltip and
  the leader lines ruling its rows, with redaction darkening.
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
  tooltip to a component.
- **`kmlib.starsector.ui.widgets`** - widget geometry over one shared
  labelled-row core: bordered box content bounds, checkbox tick boxes,
  icon-label rows with trailing slots and direction triangles, radio grids and
  segments, the row slots a label carries (text, runs, image, tick, triangle,
  empty), box borders per edge, and panel placement with its scroll region.
- **`kmlib.starsector.ui.widgets.lists`** - the spotlight picker: sort modes
  with direction and comparator, column-count and sort selectors, the item
  seam rows are drawn from, the store a consumer persists picks into, and a
  revision memo holding a built list until its inputs change.
- **`kmlib.starsector.ui.widgets.scroll`** - scroll offset state with
  clamping, and scrollbar track, thumb and grab-column geometry over a scroll
  region or a panel placement.
- **`kmlib.starsector.ui.widgets.segments`** - splitting a row into segments
  under uniform, snapped or fixed sizing, and the dividers and channels
  between them.
- **`kmlib.starsector.ui.widgets.tabs`** - tab strip geometry for both
  chromes, collapse with a docked start, hotkey lookup, shortcut text runs,
  and the hover, pulse, look, wash and light sources a tab header and its
  panel read.
- **`kmlib.starsector.ui.widgets.tabs.style`** - the tab look a host varies:
  chrome choice, tab box sizing, palette with hover and click states, hotkey
  underlining, text halo, and the vanilla tab and button fills a glow is
  resolved against.
- **`kmlib.starsector.ui.widgets.tooltip`** - cursor tooltip content and
  layout: table and centred rows carrying crests, values and indentation,
  nested sections, per-level line gaps and section breaks, and the header,
  paragraph and footnote styles they read in.
