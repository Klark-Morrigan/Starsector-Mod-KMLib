# KMLib UI

Part of [KMLib](../../../../../../README.md). The mods this package forces on a consumer
are listed under [Requirements](../../../../../../README.md#requirements).

The UI primitives a KM mod draws with, tiered by the surface they reach the screen
through. The tiering is the point of the package: content is authored once in types that
name no surface, and each surface has its own thin edge that paints or projects that
content. Pairs that look like duplication across the tiers usually are not - see
[pairs that look like duplicates](#pairs-that-look-like-duplicates).

## Index

- [Two surfaces](#two-surfaces)
- [The neutral middle](#the-neutral-middle)
- [A row states its kind, the box states the look](#a-row-states-its-kind-the-box-states-the-look)
- [Pairs that look like duplicates](#pairs-that-look-like-duplicates)
  - [`Highlight` versus `TextSpan`](#highlight-versus-textspan)
  - [`ImageSpan` versus `RowSlot.Image`](#imagespan-versus-rowslotimage)
- [Ports across the boundary](#ports-across-the-boundary)
- [Two span measurers](#two-span-measurers)
- [Two hosts, one map widget](#two-hosts-one-map-widget)
- [The list picker holds no store](#the-list-picker-holds-no-store)
- [Where each package sits](#where-each-package-sits)

## Two surfaces

A KM UI reaches the screen one of two ways:

- **The game's widget API** - `TooltipMakerAPI`, `TextPanelAPI`, `LabelAPI`. Available
  wherever a screen offers a panel to hang a component on.
- **Raw GL**, in a [render pass](../../../../../../docs/dev/rendering-environment.md). The
  only option on a core screen that offers no such
  panel: the sector map and the intel screen are opaque surfaces composited over, so
  anything a mod puts there is painted directly, in UI coordinates, above the pass the
  screen itself drew.

Neither surface is a superset of the other, and which one is available is a fact about
the screen rather than about the content. So the content model sits between them and
names neither.

## The neutral middle

`text`, `controls`, `widgets`, `layout`, and `label` hold content and geometry that name
no drawing surface. A row says what it carries; a layout says where its parts land;
neither says what paints them.

`TextAlignment` is the clearest statement of why, and its own Javadoc makes the argument:
both surfaces already have a nine-position vocabulary - LazyLib's `LazyFont.TextAnchor`
for GL, `com.fs.starfarer.api.ui.Alignment` for vanilla widgets - so adopting either
would tie every styled control to one surface and force a translation out of a vocabulary
it has no part in.

There is a second reason specific to the GL side, and it is the reason LazyLib is the one
mod KMLib declares a hard dependency on.

The game exposes no way to draw text at arbitrary coordinates in a render pass, so every
GL surface here goes through LazyLib's `LazyFont`. That dependency cannot be made
optional the way LunaLib and Console Commands are: each of those gates behind a
mod-enabled check that does nothing when the mod is absent, whereas an absent LazyLib raises
`NoClassDefFoundError` from inside a render pass, once per frame. No guard prevents it -
`LabelRenderer` names `LazyFont.TextAnchor` in its own signature, so a caller resolves the
class before any check could run. `LazyFontCache` catches `FontException`, which is a
missing or malformed `.fnt` and a different failure entirely. The launcher enforcing the
dependency is what replaces the guard that cannot exist.

Declaring it does not license the neutral tier to name it. A look value is carried by
every styled control, including in mods that draw no text and load no drawing class, so
naming a LazyLib type in one would pull the library into paths that have no use for it -
and would tie the value to a single surface, which is the first argument again.

## A row states its kind, the box states the look

The neutral middle's rule applied to typography: a row carries no face. It says which kind
of line it is - [`TooltipLineStyle`](widgets/tooltip/TooltipLineStyle.java) - and the box hosting it
says what each kind draws in - [`TooltipStyle`](widgets/tooltip/TooltipStyle.java), a
[`TextStyle`](text/TextStyle.java) per kind.

Splitting it there follows the owners. Whatever knows the subject matter knows a line is a
heading; whatever knows the surface knows what a heading looks like on it. So a row builder
holds no opinion about faces, two builders feeding one box cannot disagree about them, and one
decision on the box restyles every line of a kind at once - on either surface, since a GL box
reads those styles into its own draws while a vanilla-widget tooltip reads the same two into
`setTitleFont` / `setParaFont`.

Sizing follows the kind rather than the box: each row is measured on the face its own kind
resolved to, or a heading in a wider face overflows the box that was sized for it. That is
what [`TextSpanMeasurer`](font/TextSpanMeasurer.java) takes a face per call for.

Spacing is settled the same way, and from structure rather than from any line's own request.
A box's content reaches [`CursorTooltip`](widgets/tooltip/CursorTooltip.java) as
[`TooltipSection`](widgets/tooltip/TooltipSection.java) blocks: lines within one block stand a line gap
apart, and two blocks the box's own `sectionBreak`. A parting is a fact about two blocks
meeting, so neither of them can own it - carried as a flag on the line that opens a block, it
varies with whatever size that line happens to be, and the gap under a box's title then comes
out different from the gaps between its body blocks for no reason a reader can see.

## Pairs that look like duplicates

| What it expresses | Neutral | Vanilla-surface form | GL-surface form |
| --- | --- | --- | --- |
| a run of text and its colour | [`TextSpan`](text/TextSpan.java) | [`Highlight`](highlight/Highlight.java) | [`LabelRenderer`](render/gl/LabelRenderer.java) |
| where text sits at its draw point | [`TextAlignment`](text/TextAlignment.java) | `api.ui.Alignment` | `LazyFont.TextAnchor` |
| a line with parts in other colours, or a small image among its words | a label's runs ([`LabelRun`](text/LabelRun.java), composed by [`LabelRuns`](text/LabelRuns.java)), on [`LabelledRow`](widgets/LabelledRow.java), [`TooltipRow`](widgets/tooltip/TooltipRow.java), or a labelled [`ControlSpec`](controls/ControlSpec.java) control | [`HighlightedParagraph`](highlight/HighlightedParagraph.java) | [`CursorTooltipRenderer`](render/gl/CursorTooltipRenderer.java) |
| a hover tooltip | [`CursorTooltip`](widgets/tooltip/CursorTooltip.java) | [`Tooltips`](tooltip/Tooltips.java) | [`CursorTooltipRenderer`](render/gl/CursorTooltipRenderer.java) |
| the width of a run | [`TextSpanMeasurer`](font/TextSpanMeasurer.java) | - | [`LazyFontSpanMeasurer`](font/LazyFontSpanMeasurer.java) |

### `Highlight` versus `TextSpan`

Identical shape - text plus a colour - and they must not be merged. The difference is how
each addresses the text it colours:

- `TextSpan` is a **run**: a stretch of characters that carries its own colour by
  position. Exact, and unambiguous by construction.
- `Highlight` is a **token**: vanilla's `LabelAPI.setHighlight(String[])` searches the
  paragraph for that substring. "3 of 3 markets" cannot say which "3" it means.

The token form is lossy, and it is lossy because the vanilla API is - `Highlight` exists
to bind a substring to its colour so the two cannot drift apart on the way into the
engine's parallel arrays, which is the most that API allows. The precise form therefore
belongs in the model and the lossy one at the adapter: runs project down to tokens when
they reach a vanilla surface, and never the other way, because recovering positions from
a substring search is exactly the ambiguity being avoided.

`HighlightedParagraph` stays on the vanilla side for the same reason, and additionally
carries its own `addTo` render methods - it is an adapter, not content.

### `ImageSpan` versus `RowSlot.Image`

Both name a small image sized off its line, and they must not be merged. The difference is
what the image is attached to:

- [`ImageSpan`](text/ImageSpan.java) is a **run**: part of a label's sentence, spaced by a
  word gap and carried wherever that label goes. A centred line's crest is one, which is
  what lets crest and words centre together as a unit.
- [`RowSlot.Image`](widgets/RowSlot.java) is a **column**: reserved at one width across a
  whole stack of rows, so the labels past it line up. A breakdown's per-faction crests are
  these, which is why a crest-less row in that stack still indents to meet them.

The same crest reads differently in each: as a run it sits where the sentence puts it, as a
slot it anchors to a gutter every row shares. So the choice is a statement about whether the
image belongs to the words or to the table - and the two sets stay separate because a tick
box or a sort triangle is a column that has no reading mid-word, and so is spelled only in
`RowSlot`.

## Ports across the boundary

Where the neutral middle needs something only a surface can answer, it takes a port and
the surface supplies the implementation:

| Port | Answers | Implementations |
| --- | --- | --- |
| [`TextSpanMeasurer`](font/TextSpanMeasurer.java) | the width of a run in a face | [`LazyFontSpanMeasurer`](font/LazyFontSpanMeasurer.java) |
| [`LineWidthMeasurer`](font/LineWidthMeasurer.java) | the width of a line | [`LazyFontMeasurer`](font/LazyFontMeasurer.java) |
| [`LabelLengthEstimator`](label/LabelLengthEstimator.java) | how long a label will draw | [`FontLabelLengthEstimator`](label/FontLabelLengthEstimator.java), [`AspectLabelLengthEstimator`](label/AspectLabelLengthEstimator.java) |
| [`ModelviewMatrixReader`](map/transform/ModelviewMatrixReader.java) | the campaign map's transform | [`GlModelviewMatrixReader`](map/transform/GlModelviewMatrixReader.java), [`FastRenderingModelviewMatrixReader`](map/transform/FastRenderingModelviewMatrixReader.java) |
| [`IntelScreenView`](intel/IntelScreenView.java) | what the intel screen is showing | [`VanillaIntelScreenView`](intel/VanillaIntelScreenView.java) |

`ControlSpec` and `ControlAction` split the same way within `controls`: the sealed spec is
content, the action is behaviour the container owns. A content model that held its own
`ControlAction` would have absorbed interaction it has no business holding.

## Two span measurers

[`TextSpanMeasurer`](font/TextSpanMeasurer.java) takes the face per call;
[`StyledSpanMeasurer`](text/StyledSpanMeasurer.java) has the look bound and takes the span
alone. Reach for the bound one wherever a piece of content is asked its own width - a
[`RowSlot`](widgets/RowSlot.java) holding a value, say. Such a piece holds no face, and
could not supply one without first learning which style its line resolved to, nor apply
the casing that style shouts the text in - and text measured as authored measures narrower
than it paints, so a box sized from that measurement clips what is drawn into it. Whatever
resolved the style binds the measurement once and passes it down.

Neither a port nor a duplicate, which is why it appears in neither table above. Nothing
implements it per surface: the binding closes over `TextSpanMeasurer`, so the LazyLib
adapter is still the only thing that knows a glyph width. And it lives in `text` rather
than beside its sibling in `font` because `text` already reads `font` (a `TextStyle` holds
a `TextFace`), so a port in `font` naming `TextSpan` would close that into a cycle.

## Two hosts, one map widget

The game shows the same map widget in two places, and each keeps its own filter state: the
sector map on the `M` screen, and the intel screen's embedded map preview (its "map visor").
So "is a map showing, and in which mode" has two answers at once, and which one a caller
wants depends on what it is deciding.

| Question | Read | Why |
| --- | --- | --- |
| about the sector map | [`CampaignMapView`](map/presence/CampaignMapView.java) | the `M` screen's own tab, sub-view and filter |
| about the intel screen | [`IntelScreenView`](intel/IntelScreenView.java) | the visor's own widget, rectangle and filter |
| about whichever screen is up | [`MapPresence`](map/presence/MapPresence.java) | either screen counts, and the asker cannot tell which it was called from - one read per look, plus one indifferent to it, over one pair of sources |
| which widget the map is | [`ShownMapTab`](map/probes/ShownMapTab.java) | a rule about map-tab layout has to be rooted at the map tab, wherever it is |

The host-blind reads exist for one situation: code reached through a hook that is not told
which host invoked it, so it cannot ask a host-specific question even though it would prefer
to. Anything deciding **where** to draw, or whether to put controls over a particular
surface, asks that surface instead - a blind read says only that some host is showing such a
map, and is true while a different host entirely is the one showing it. That class states
that limit and leaves the reasoning below to this section, so it is described once.

Its three reads are three states the game is in rather than three settings of one, which is
why none is written as another's negation: a screen showing no map leaves all three false.
The sector map reaches them as a single [`SectorMapState`](map/presence/SectorMapState.java)
rather than as a boolean per read, so "not showing" cannot arrive carrying a filter setting.

That is a disjunction rather than a switch, and the reason is worth knowing before trusting
either read too far. One core tab shows at a time, so the two usually exclude each other -
but they are not reading the same thing. The sector read goes through `getCurrentCoreTab()`,
which answers for an **interaction dialog's own** core UI whenever such a dialog is up,
while the intel read always walks the **main** core UI. So the pair can be aimed at two
different core UIs, and nothing in either read rules out both answering yes at once. Treat
the exclusivity as the usual case rather than a guarantee.

`ShownMapTab` sidesteps that pairing rather than living with it. It recognises the `M`
screen's map by the tab being a `SectorMapAPI` - published API, so obfuscation-proof, and a
test of what the widget **is** rather than of which tab the campaign UI reports. That keeps
the reading and the widget it describes in the same tree, which the tab-id route cannot
promise. The visor is asked second and only when the current tab is not itself a map, since
that screen hosts its map below a tab that is not one.

Filter state follows the same split, and it is per map rather than global. Each map widget
binds to the filter its params carry, falling back to the campaign's persisted filter when
they carry none. The core-UI map tab carries none - so the `M` screen and a dialog-hosted map
tab share the persisted filter, which is the one the sector read reports. Every embedded
preview supplies its own instead, the intel visor included, which is why that screen is asked
about its own state and never about the campaign's.

Those settings are independent, and they start out disagreeing: the persisted filter is built
with Starscape on - a new game's first look at the sector map is the starfield - while every
embedded filter is built with it off. Toggling one leaves the other alone, which is the whole
reason the host-blind read is an OR over two sources rather than one flag consulted once.

All three fail closed, so an unreadable link answers "not showing" rather than guessing.

## The list picker holds no store

A picker list that ranks by a chosen metric, flips direction when the lit metric is
re-picked, wraps across one or two columns, and spotlights the row it is clicked on is the
same mechanism in every mod that draws one. [`widgets/lists`](widgets/lists/) is that
mechanism, its own folder beside [`tabs`](widgets/tabs/) and
[`segments`](widgets/segments/) because it is a family rather than a piece:
[`ListSortMode`](widgets/lists/ListSortMode.java) is the seam a consumer's own
vocabulary implements, [`ListSortModes`](widgets/lists/ListSortModes.java) bundles that
vocabulary with the fallback an unrecognised key lands on,
[`ListSort`](widgets/lists/ListSort.java) is how a list is ranked - the active mode, its
[`SortDirection`](widgets/lists/SortDirection.java), and the vocabulary both were chosen
from - and [`ListColumns`](widgets/lists/ListColumns.java) is the one-or-two column choice.
The two selectors - [`SortSelectorControl`](widgets/lists/SortSelectorControl.java) and
[`ColumnsSelectorControl`](widgets/lists/ColumnsSelectorControl.java) - draw and drive them.

The vocabulary rides on the sort rather than beside it because nothing here reads one
without the other: a mode with no set around it cannot say what clicking another row would
select, and a set with no active mode cannot say which row is lit. Carrying them apart made
every builder in the family take both and trust that the two matched, which is what
`ListSort`'s constructor now checks instead.

[`ListPickerControl`](widgets/lists/ListPickerControl.java) is what they compose into: a
rule, the columns selector, a row pairing the sort selector with whatever the consumer
pairs beside it, then a deselectable icon-radio list of the items. The arrangement is not
what makes it worth sharing - three rules that are only obvious after getting them wrong
are. Re-picking the lit row clears the spotlight rather than re-selecting it, the lit index
is resolved against the *ranked* order rather than the order the caller handed over, and an
index outside the rows is ignored rather than trusted.
[`SelectableListItem`](widgets/lists/SelectableListItem.java) is the seam its rows are drawn
from - an id, a label, a crest, and nothing else - which a consumer implements on its own
item type, so the list ranks through that consumer's own comparators and nothing is copied
into a library value on the way in. The right half of the sort row is a parameter for the
same reason: pairing something with the sort is a layout decision this package can hold,
what sits there is not.

[`RevisionMemo`](widgets/lists/RevisionMemo.java) is where a consumer holds the resolved
list between frames, since a body is built twice a frame (render and hit-test) and a picker
list is typically a full pass over whatever the consumer scores its items from. What belongs
in the revision is the caller's judgement; the key also carries the sector identity, weakly
held, which is the half worth having once rather than per mod - a save reloaded in the same
session is a fresh sector whose revision may well match the last, so without it the picker
serves the previous save's items.

The division that makes all of it shareable is that **this package owns the model and the
resolution rule; the consuming mod owns where the answer is kept**. Nothing here reads or
writes a save. The stored mode and direction keys arrive as arguments to
`ListSort.resolveStored`, each selector reports its pick back through a callback, and the
picker's three picks report together through
[`ListPickerStore`](widgets/lists/ListPickerStore.java) - write-only, because the live values
arrive as the picker's own parameters and nothing here needs a read path into a save. A mod's
own thin binder is what ties the two ends to its sector-memory keys or settings fields, and
that binder is the only place those keys appear.

Labels follow the same rule for the same reason. A string id only means something against
the category that registered it, and [`StarsectorStrings`](../strings/StarsectorStrings.java)
takes `(category, key)` on every call precisely so nothing here has to know which mod is
asking - so `ListSortMode.resolveLabelText()` and the columns selector's caption arrive as
drawn text. `ListColumns` is the deliberate exception: its segment labels are the literals
`1` and `2`, because the rule exists to keep player-visible *prose* translatable and a digit
standing for a count is not prose.

## Where each package sits

| Package | Tier | Holds |
| --- | --- | --- |
| [`text`](text/) | neutral | `TextStyle`, `TextAlignment`, `StyledSpanMeasurer`, the sealed [`LabelRun`](text/LabelRun.java) set a label is made of (`TextSpan`, `ImageSpan`), and [`LabelRuns`](text/LabelRuns.java) - how those runs compose into one line, read by every surface that lays one |
| [`controls`](controls/) | neutral | the sealed `ControlSpec` set and its enums; a control's own label is runs like any other label, and a stacked table's rows are `widgets`' own [`LabelledRow`](widgets/LabelledRow.java), so a strip and a tooltip are laid out against one row model |
| [`widgets`](widgets/) | neutral | the shared `LabelledRow` core, its `RowSlot` flanks, and the row and box content and geometry built on them ([`tooltip`](widgets/tooltip/), [`tabs`](widgets/tabs/), [`scroll`](widgets/scroll/), [`segments`](widgets/segments/), [`lists`](widgets/lists/)) |
| [`widgets/tooltip`](widgets/tooltip/) | neutral | the hover box's own content and geometry - [`TooltipRow`](widgets/tooltip/TooltipRow.java) and the [`TooltipSection`](widgets/tooltip/TooltipSection.java) blocks it stacks in, the [`TooltipStyle`](widgets/tooltip/TooltipStyle.java) those are laid against, and [`CursorTooltip`](widgets/tooltip/CursorTooltip.java) placing them |
| [`layout`](layout/) | neutral | box placement, strips, padding, screen anchors, and the [tabs row](layout/TabsControlLayout.java) - the one control whose dimensions come from the vanilla tab strip rather than from a body-font label |
| [`label`](label/) | neutral | label fitting, plus the length-estimator port |
| [`colour`](colour/) | neutral | `StarsectorUiColour`, the checked wrapper over vanilla's colour getters |
| [`font`](font/) | split | the [face enum](font/StarsectorFont.java) every atlas is named through, the measurement ports, and their LazyFont-bound implementations and [caches](../../../../../../docs/dev/caching.md) |
| [`render/gl`](render/gl/) | GL | every raw-GL painter, its styles, and the [state guard](render/gl/GlStateGuard.java) |
| [`debug`](debug/) | GL | the on-screen debug HUD |
| [`input`](input/) | GL | `UiCursor`, the panel input controllers, and the motions they hold in answer to input: the [hover fades](input/HoverFades.java) saying how far each element has travelled onto its hovered look, the [click pulses](../../animation/PulseEnvelopes.java) saying how far through its lift each tab is, and a bound key's blink, which is an envelope like a click but read with the fades - it carries a tab onto the hovered look rather than past it, so the two compose by the greater of them and a blink under the pointer shows nothing. All run on [`EasedFraction`](../../animation/EasedFraction.java) at one duration, so every animation on a panel eases and paces alike |
| [`highlight`](highlight/) | vanilla | `Highlight`, `HighlightedParagraph`, `HighlightedMessage` |
| [`tooltip`](tooltip/) | vanilla | [`Tooltips`](tooltip/Tooltips.java), the `TooltipCreator` boilerplate wrapper - the vanilla surface's answer to what [`widgets/tooltip`](widgets/tooltip/) holds neutrally |
| [`intel`](intel/) | split | the screen-view port and its vanilla implementation |
| [`coreui`](coreui/) | vanilla | [`CoreUiTree`](coreui/CoreUiTree.java), the by-name reach into the live widget tree that every screen's probes walk |
| [`map/transform`](map/transform/) | split | the modelview-matrix port, its GL and Fast Rendering implementations, the [selector](map/transform/ModelviewMatrixReaders.java) between them, and the [transform](map/transform/CampaignMapTransform.java) and [cursor read](map/transform/MapCursor.java) built over it |
| [`map/presence`](map/presence/) | vanilla | what the game is showing: the sector map's own [view state](map/presence/CampaignMapView.java) as one [classified answer](map/presence/SectorMapState.java), plus the host-blind [reads](map/presence/MapPresence.java) that fold it together with the intel screen's map |
| [`map/probes`](map/probes/) | vanilla | the live reads into the map's widget tree over [`CoreUiTree`](coreui/CoreUiTree.java)'s by-name reach: [`ShownMapTab`](map/probes/ShownMapTab.java), the [surface bounds](map/probes/MapSurfaceBounds.java) that pick the map out of that tab by shape as a [surface area](map/probes/MapSurfaceArea.java) carrying the chrome drawn with it, [`VanillaMapTooltip`](map/probes/VanillaMapTooltip.java), and the [widget trace](map/probes/MapTabWidgetTrace.java) that describes what the cursor is inside for a consumer to log as its own |

The map is three packages rather than one because the three read different things - GL state,
the campaign's persisted UI data, the live widget tree - and **no class in any of them
references another's**. That independence is the reason the split is worth keeping: a probe
that started reaching for the transform, or a presence read that had to walk the tree, would
be the signal that one of these has taken on a job belonging to another.

Two packages are named `tooltip`, and the pair is the tier split rather than a collision to
resolve. `widgets/tooltip` is what a hover box *is* - lines, blocks, and where they land, naming
no drawing surface; `tooltip` is what the vanilla widget API needs to be handed one; and
`render/gl` paints the neutral model itself. One subject, three homes, exactly as the table of
[pairs](#pairs-that-look-like-duplicates) above sets out. An import naming the simple name alone
is the thing to look twice at.

`layout.VanillaPositions` is the one deliberate exception in a neutral package: it holds
vanilla screen coordinates, which are a fact about the game's own layout rather than
about any KM content.
