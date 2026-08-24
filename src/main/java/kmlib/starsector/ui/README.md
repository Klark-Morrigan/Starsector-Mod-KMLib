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

The kinds are a heading, a line of the body, and a footnote - a line about the box rather than
about its subject, which the game's own boxes end their key hints in and set in a smaller,
narrower face than the content above. Most boxes note nothing, so the footnote look defaults to
the body's and a box that does note something layers its own on with `footnotedIn`.

A table row carries one more fact of the same sort: its **subordination level**, how many steps it
stands under the voice the box speaks in, which the host turns into a size through
`TooltipStyle.shrunkPerLevel`. It is deliberately three things it might be mistaken for and is not.
Not the indent - a group's members are inset beneath it while remaining the same kind of statement
it is, so the two move independently. Not tree depth - those members are children of the line above
without being subordinate to it. And not a size, for the reason this whole section exists. It also
says nothing about *why* a line stands under another: a stack of rows may be a breakdown, a
hierarchy, a list with sub-items, or an aside beneath a finding, and all a widget that knows none of
them can name is the one step down they share.

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

Every measurement of room a box spends travels as one
[`TooltipSpacing`](widgets/tooltip/TooltipSpacing.java) on its style, since the three are one subject
and are read together by whatever stacks the content. The line gap within it is a
[`TooltipLineGaps`](widgets/tooltip/TooltipLineGaps.java) rather than a single width, because it is the
one of the three that varies by depth: a box states a gap per subordination level, each statement
governs the tiers nested under it until another is stated, and whatever stands above the shallowest
statement keeps the base gap. Inherited downward because a box cannot know how deep its subject
matter goes - a line below the deepest stated tier belongs to that tier's account, and given the base
gap instead the innermost lines of a listing would be the airiest in the box.
The gap belongs to the tier of the line **just drawn**, not
of the line about to be - a run of lines at one depth is what a reader takes in as a unit, so it is
the run that tightens, and resolved the other way the first line of a run would take its own tier's
gap and pull the whole run up against the line it stands under. A map rather than a width per tier
for the same reason `shrunkPerLevel` is one step rather than a look per level: a listing goes as deep
as its subject matter does. The block partings are not tiered - a boundary is the same width wherever
it falls, and both still win where they apply.

One thing a box measures is not room but a **leader rule**: the stretch a row leads a hairline along
from the end of its label across to the start of its value, since the value column is pinned to the
box's right edge however short the label is and a reader tracking a number back to its name otherwise
has nothing to follow. It is measured with the columns
([`TooltipLeaderLine`](widgets/tooltip/TooltipLeaderLine.java)) rather than derived where it is painted,
because it is worked out from the label and value widths the layout already took - a rule derived from a
second measurement of the row could only drift from the two columns it exists to join. It stands off
each end by the row's *own* face's word space, the same space the label parts its own runs by, so a
mark set among a line's words keeps that line's rhythm at whatever size the row draws at. Where what is
left is shorter than that space it is not led at all: the two columns are already close enough to read
as one line, and the aid is only wanted where the eye could lose it.

Where it sits vertically is not the layout's to say. The rule is set on the middle of the face's
lower-case band, read off that face's own glyph box
([`LazyFontMeasurer`](font/LazyFontMeasurer.java)) rather than taken as a fraction of the line - a
bitmap face's lower-case letters sit wherever its atlas puts them, and the fraction that centres one
face's band rides high on the next. That is a metric only a surface holding a loaded face can read, so
it stays with the [paint](render/gl/tooltip/TooltipLeaderLineRenderer.java).

How *heavily* it lands is neither's to decide. The rule's colour is the paint's - a leader is the
quietest mark on the line by definition, so it takes the engine's grey whatever the box states for its
own text - but its thickness and how far it is let down from the box's opacity are a
[`TooltipLeaderLineStyle`](render/gl/tooltip/TooltipLeaderLineStyle.java) the host states, because the
balance cannot be settled from the code: a solid run covers every pixel it crosses outright where a
glyph stroke of the same shade spends much of its footprint at partial alpha, and by how much depends on
the face, the size, and how that atlas was rasterised. `TEXT_WEIGHTED` is the pair that puts it level
with greyed-out text on the faces a KM tooltip is ordinarily set in; a host with a slider moves from
there.

Blocks nest, so the same reading spaces a listing at every depth: a block holds its own opening
lines over member blocks, and a member takes the narrower `groupBreak` above itself where the
member before it came to more than one line. That parting is spent by the member that follows
and never above a block's first, which is what stops it piling up where several groups close on
the one row - a colony's last term, the colony, and the faction holding it all end together, and
the next faction is set off by one gap rather than three.

## Pairs that look like duplicates

| What it expresses | Neutral | Vanilla-surface form | GL-surface form |
| --- | --- | --- | --- |
| a run of text and its colour | [`TextSpan`](text/TextSpan.java) | [`Highlight`](highlight/Highlight.java) | [`LabelRenderer`](render/gl/LabelRenderer.java) |
| where text sits at its draw point | [`TextAlignment`](text/TextAlignment.java) | `api.ui.Alignment` | `LazyFont.TextAnchor` |
| a line with parts in other colours, or a small image among its words | a label's runs ([`LabelRun`](text/LabelRun.java), composed by [`LabelRuns`](text/LabelRuns.java)), on [`LabelledRow`](widgets/LabelledRow.java), [`TooltipRow`](widgets/tooltip/TooltipRow.java), or a labelled [`ControlSpec`](controls/ControlSpec.java) control | [`HighlightedParagraph`](highlight/HighlightedParagraph.java) | [`CursorTooltipRenderer`](render/gl/tooltip/CursorTooltipRenderer.java) |
| a hover tooltip | [`CursorTooltip`](widgets/tooltip/CursorTooltip.java) | [`Tooltips`](tooltip/Tooltips.java) | [`CursorTooltipRenderer`](render/gl/tooltip/CursorTooltipRenderer.java) |
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
  word space and carried wherever that label goes. A mark belonging to the thing a line names
  is one - it lands where the sentence puts it, at whatever indent the line sits at, which is
  also what lets a centred line's image and its words centre together as a unit.
- [`RowSlot.Image`](widgets/RowSlot.java) is a **column**: reserved at one width across a
  whole stack of rows, so the labels past it line up, and an image-less row in that stack
  still indents to meet them. That alignment is what it is for, so it earns its keep only
  where the stack is flat: rows sitting at differing indents cannot share one gutter without
  the deeper ones drawing their image left of the name it belongs to.

The same crest reads differently in each: as a run it sits where the sentence puts it, as a
slot it anchors to a gutter every row shares. So the choice is a statement about whether the
image belongs to the words or to the table - and the two sets stay separate because a tick
box or a sort triangle is a column that has no reading mid-word, and so is spelled only in
`RowSlot`.

Both carry a tint, and for the same reason: a mark that stands in for words has to read with
them. A slot states the colour of a mark whose *row* reads back from the stack around it - a
picker row that is worth offering but has nothing to show, whose crest has to recede with its
words or the row reads as half-drawn. A run states the colour of a mark set inside a sentence,
which is the same case one line deep: an asset authored to carry on some other surface arrives
as the loudest thing on a line whose meaning is in the words, so a caller hands over the colour
the words are drawn in. Either way the draw multiplies by the stated colour and a null is drawn
as authored, which is what a mark that is a picture in its own right passes.

The colour is the caller's rather than the asset's in both. A sprite cannot say which of the two
kinds of mark it is - the same artwork can be a subject on one line and a shorthand on the next -
so nothing here reads a colour off the asset, and a caller that has one authored beside the path
decides for itself whether this surface is the one that should spend it.

The gutter stays even under that, which is what the column is for: a stack's tints come from
one palette in whatever builds its rows, not from each item, so two receded rows cannot recede
differently. And a tint darkens rather than fades - the alpha is the host's - so a tint stated
with an alpha of its own would put a second opacity into a draw that already has one. Pass an
opaque colour.

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
| [`UiSoundPlayer`](sound/UiSoundPlayer.java) | where a widget's interface sounds go | [`VanillaUiSoundPlayer`](sound/VanillaUiSoundPlayer.java), [`UiSoundPlayerFake`](../../../../../testFixtures/java/kmlib/testfixtures/starsector/ui/sound/UiSoundPlayerFake.java) |
| [`CoreUiComponentRepainter`](coreui/CoreUiComponentRepainter.java) | drawing a core-UI component again, clipped | [`ReflectiveCoreUiComponentRepainter`](coreui/ReflectiveCoreUiComponentRepainter.java), [`CoreUiComponentRepainterFake`](../../../../../testFixtures/java/kmlib/testfixtures/starsector/ui/coreui/CoreUiComponentRepainterFake.java) |
| [`CursorPosition`](input/CursorPosition.java) | where the pointer is, in UI units | [`VanillaCursorPosition`](input/VanillaCursorPosition.java), [`CursorPositionFake`](../../../../../testFixtures/java/kmlib/testfixtures/starsector/ui/input/CursorPositionFake.java) |

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
| which widgets the *other* maps are | [`EmbeddedMapFinder`](map/probes/EmbeddedMapFinder.java) | a mod compositing a map into a panel of its own puts a map surface on screen that none of the reads above reports, and it is on screen precisely when they all answer no |

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
but they are not reading the same thing: the sector read goes through the core tab the
campaign reports, while the intel read walks down to the panel that tab holds. Both answer
for the **core UI in force** - an interaction dialog's own while such a dialog is showing
one, the campaign's otherwise - so they cannot be aimed at two different trees, but nothing
in either rules out both answering yes at once. Treat the exclusivity as the usual case
rather than a guarantee.

Neither goes through `getCurrentCoreTab()` raw, and the correction between them is what
keeps the pair honest while the player is docked.
[`CampaignScreenView`](coreui/CampaignScreenView.java) owns it: a dialog that hosts a core UI
goes on handing it out after the player closes the screen it was showing, and that core -
unlike the campaign's, which closes its tab - keeps naming the tab it last showed for the
rest of the visit. So a reported tab stands only while the core UI it came from is still on
screen, and the walk takes that dialog's core on the same terms. Read raw, opening the intel
screen while docked and closing it again leaves every map-gated overlay believing it is
still up.

`ShownMapTab` does not take the tab id's word for it at all. It recognises the `M` screen's
map by the tab being a `SectorMapAPI` - published API, so obfuscation-proof, and a test of
what the widget **is** rather than of which tab the campaign UI reports, which is what a
layout rule rooted at it actually needs. The visor is asked second and only when the current
tab is not itself a map, since that screen hosts its map below a tab that is not one.

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
from - [`ListPicker`](widgets/lists/ListPicker.java) is a list bundled with the vocabulary
that ranks it, which is what a consumer hands over when *which* list it offers varies, and
[`ListColumns`](widgets/lists/ListColumns.java) is the one-or-two column choice.
The two selectors - [`SortSelectorControl`](widgets/lists/SortSelectorControl.java) and
[`ColumnsSelectorControl`](widgets/lists/ColumnsSelectorControl.java) - draw and drive them.

The vocabulary rides on the sort rather than beside it because nothing here reads one
without the other: a mode with no set around it cannot say what clicking another row would
select, and a set with no active mode cannot say which row is lit. Carrying them apart made
every builder in the family take both and trust that the two matched, which is what
`ListSort`'s constructor now checks instead. `ListPicker` is the same bundling one step
earlier, before a sort is resolved at all: a consumer whose lists rank by different metrics
hands over the list and the vocabulary that can read it as one value, so nothing between the
two can pair a list with a vocabulary that cannot rank it. Its `empty()` is the offers-nothing
answer, and it carries no fallback mode - an empty picker draws nothing, so the item list is
read and found empty before any stored sort is resolved against it.

[`ListPickerControl`](widgets/lists/ListPickerControl.java) is what they compose into: a
rule, the columns selector, a row pairing the sort selector with whatever the consumer
pairs beside it, then a deselectable icon-radio list of the items. The arrangement is not
what makes it worth sharing - three rules that are only obvious after getting them wrong
are. Re-picking the lit row clears the spotlight rather than re-selecting it, the lit index
is resolved against the *ranked* order rather than the order the caller handed over, and an
index outside the rows is ignored rather than trusted.
[`SelectableListItem`](widgets/lists/SelectableListItem.java) is the seam its rows are drawn
from - an id, a label, a crest, whether the row reads back (`isDimmed`), and nothing else -
which a consumer implements on its own item type, so the list ranks through that consumer's own
comparators and nothing is copied into a library value on the way in. `isDimmed` splits the
question the way the rest of the package splits every question: the consumer answers *which*
rows read back, because only it knows what its numbers mean, and the picker answers *how far*,
so one mod's receded row cannot look unlike another's. It defaults to false, so a list whose
every item is equally worth picking implements nothing. The right half of the sort row is a
parameter for the same reason: pairing something with the sort is a layout decision this
package can hold, what sits there is not.

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
| [`controls`](controls/) | neutral | the sealed `ControlSpec` set and its enums; a control's own label is runs like any other label, and a stacked table's rows are `widgets`' own [`LabelledRow`](widgets/LabelledRow.java), so a strip and a tooltip are laid out against one row model. Beside them the seams a paint pass reads what the pointer is doing through: [per cell](controls/ControlHoverSource.java) for one control, and [per strip position](controls/BodyHoverSource.java) for a body of them - two steps rather than one call taking a position and a cell together, so the two ints a slot is made of are never both loose in one call - each with a [press channel](controls/ControlPressSource.java) of its own beside the hover, carried [as a pair](controls/ControlInteractionSources.java) so a layer between the panel's live state and its chrome threads one value rather than one per channel |
| [`widgets`](widgets/) | neutral | the shared `LabelledRow` core, its `RowSlot` flanks, and the row and box content and geometry built on them ([`tooltip`](widgets/tooltip/), [`tabs`](widgets/tabs/), [`scroll`](widgets/scroll/), [`segments`](widgets/segments/), [`lists`](widgets/lists/)) |
| [`widgets/tabs`](widgets/tabs/) | neutral | a tab row's geometry ([`VanillaTabStrip`](widgets/tabs/VanillaTabStrip.java) lays it, [`RaisedButtonTabStrip`](widgets/tabs/RaisedButtonTabStrip.java) stands a button inside each laid tab), the seams a paint pass reads its per-tab [look](widgets/tabs/TabLookSource.java) and [lift](widgets/tabs/TabWashSource.java) through, the tabs' own text, and the tab panel's transient state. What the pointer is doing to a whole panel travels as [one value](widgets/tabs/TabPanelInteractionSources.java) - the header's channels and the body's - because both are resolved off one read of the cursor against the one placement being drawn, and a consumer taking them one at a time could pair a fresh reading with a stale one |
| [`widgets/tabs/style`](widgets/tabs/style/) | neutral | how a tab row looks, as one injected [`TabStyle`](widgets/tabs/style/TabStyle.java): its [chrome](widgets/tabs/style/TabChrome.java), the [box](widgets/tabs/style/TabBox.java) each tab stands in - a stated width, height and neighbour channel, or `SNAPPED` for tabs sized to their own labels, which is what decides whether a label is measured at all - the [palette](widgets/tabs/style/TabPalette.java) of per-state [looks](widgets/tabs/style/TabLook.java) and [lifts](widgets/tabs/style/TabWash.java) over the backing the row stands on - the surface every fill is composited onto, and the one a parted row paints into the channels between its tabs - its [hotkey convention](widgets/tabs/style/HotkeyStyle.java), and the face it is lettered in - which the layout sizes its tabs at, so a row is measured in what it will be drawn in - with the [ring](widgets/tabs/style/TextHalo.java) that face wants around it, copies of the text laid on all four sides that cost no width and so move no tab. Split out from the geometry beside it because a look is what a host varies. One palette type, one factory per chrome, and a fill rule behind each - a tab's shades computed from the engine's tab colours ([`VanillaTabFills`](widgets/tabs/style/VanillaTabFills.java)), a button's from the accent it is built with ([`VanillaButtonFills`](widgets/tabs/style/VanillaButtonFills.java)) - because the engine's own two controls brighten by different rules and neither is the other with a knob |
| [`widgets/tooltip`](widgets/tooltip/) | neutral | the hover box's own content and geometry - [`TooltipRow`](widgets/tooltip/TooltipRow.java) and the [`TooltipSection`](widgets/tooltip/TooltipSection.java) blocks it stacks in, the [`TooltipStyle`](widgets/tooltip/TooltipStyle.java) those are laid against, [`CursorTooltip`](widgets/tooltip/CursorTooltip.java) placing them, and the [`TooltipLeaderLine`](widgets/tooltip/TooltipLeaderLine.java) it measures between a row's label and its value |
| [`layout`](layout/) | neutral | box sizing and placement, strips, padding, and the [tabs row](layout/TabsControlLayout.java) - the one control whose dimensions come from the vanilla tab strip rather than from a body-font label |
| [`label`](label/) | neutral | label fitting, plus the length-estimator port |
| [`colour`](colour/) | neutral | the UI colour vocabulary: [`StarsectorUiColour`](colour/StarsectorUiColour.java), the checked wrapper over vanilla's colour getters, and [`AccentColours`](colour/AccentColours.java), the dark/base/bright set the engine builds a control from. That set sits here rather than with the panel's other shade groups because both tiers compose out of it - a panel's paint pass and the neutral value a tab row is measured and painted from - and the neutral side cannot import across the line |
| [`sound`](sound/) | split | the [roles](sound/StarsectorUiSound.java) KM code answers a moment with - a control's press and mouseover, and the typed tick for a surface with no control on it to have been reached, an overlay drawn onto the sector map answering moments no widget has - the [cue](sound/UiSoundCue.java) binding one of those to a volume, the [scheme](sound/UiSoundScheme.java) saying what each moment sounds like - carried in the panel's [`WidgetStyle`](render/gl/style/WidgetStyle.java), a sound being a property of a look exactly as a fill is - and the player port with its live binding. The choice belongs to the look and the moment to whatever detects it, so no controller names a sound of its own and a panel is silenced by the one value it was built from. Volume is part of that choice rather than being left to the engine's per-id balance, and it travels bound to the role so a moment that sounds at all sounds at the level its own look named, rather than at whatever a call site paired with it. The arrival is the one moment answering at more than one level, so the scheme names its role once and takes a [balance](sound/PointerArrivalVolumes.java) over the [kinds of thing reachable](sound/PointerArrivalTarget.java), answering one cue for the kind a caller says it arrived at. The levels travel as one grouped value for the reason the panel's shade groups do - three bare floats in a row are three positions a caller can transpose silently - and because they are one tuning rather than three settings, meaningful only in their ratio. A list moving under the wheel is a moment beside those rather than a fourth kind of thing reached: nobody got anywhere, the content moved instead - so it carries a cue of its own, and the scheme's two cues are kept apart in every constructor, being the one same-typed pair a caller could transpose with nothing to catch it |
| [`font`](font/) | split | the [face enum](font/StarsectorFont.java) every atlas is named through - each carrying the size it draws 1:1 at and whether its glyphs want [interpolating](font/AtlasSmoothing.java), both of which its own `.fnt` descriptor states and a paint pass reads - the measurement ports, and their LazyFont-bound implementations and [caches](../../../../../../docs/dev/caching.md) |
| [`render/gl`](render/gl/) | GL | the drawing surface itself and the primitives every pass below composes: [fills](render/gl/UiFill.java), composited or added as light, [borders](render/gl/UiBoxes.java), [sprites](render/gl/UiSprite.java), [scissor](render/gl/UiScissor.java), a [label](render/gl/LabelRenderer.java), a triangle, the [atlas filter](render/gl/GlyphAtlasFilter.java) a pixel face is drawn under, and the [state guard](render/gl/GlStateGuard.java). Only three of these actually name `org.lwjgl.opengl`; the subject packages below reach GL through them rather than directly |
| [`render/gl/style`](render/gl/style/) | GL | the look a host builds and hands in whole - [`WidgetStyle`](render/gl/style/WidgetStyle.java) and the shade groups it bundles, the [hovered-cell wash](render/gl/style/ControlHoverWash.java) among them: what a body control's lift under the pointer is *made of* is named beside the accents it lifts over, its pace being the panel's and not the widget's. The [pressed-cell light](render/gl/style/ControlPressLight.java) sits beside that wash and is added over it rather than blended toward it - a press always lands on a cell the pointer is already holding fully washed, so a lift travelling only as far as the hovered shade would show nothing on every press a player actually makes, and the second per-cell paint is what a visible press costs. What a panel is dressed in, never what dresses it |
| [`render/gl/controls`](render/gl/controls/) | GL | [`ControlRenderer`](render/gl/controls/ControlRenderer.java) and the per-kind widget passes it composes - tick box, radio row and grid, toggle, divider, and the segmented-row chrome a radio and a tab strip share. It is also where a motion's progress and the shade it travels toward are bound into finished paint per cell - a [wash](render/gl/controls/CellHoverWashSource.java) for the pointer and a [light](render/gl/controls/CellPressLightSource.java) for a press, handed to a widget [as one value](render/gl/controls/CellPaintSources.java) and drawn in that order over the cell and under its marks - so no widget pass reads a cursor, holds a timing, or learns that a fade exists |
| [`render/gl/tabs`](render/gl/tabs/) | GL | a tab row's chrome, in the two conventions vanilla keeps. The [`TabChrome`](widgets/tabs/style/TabChrome.java) a style names is resolved through [`TabChromeRenderer`](render/gl/tabs/TabChromeRenderer.java) to the sector map's [seamless strip](render/gl/tabs/VanillaTabStripRenderer.java) or the intel screen's [raised buttons](render/gl/tabs/RaisedButtonTabStripRenderer.java), so a panel matches whichever tab convention stands nearest it - and nothing that draws a control branches on a chrome. The enum is substrate-independent and cannot name a GL pass, which is why the binding sits on that seam rather than on the enum. Both chromes read the same laid geometry, take their per-tab [paint](widgets/tabs/style/TabPaint.java) through the [one walk](render/gl/tabs/TabChromeRenderer.java) that composes the look, lift and [light](widgets/tabs/style/TabLight.java) channels in their only valid order - the look settles, the lift moves that surface, and the light is *added* over whatever was drawn, which is how a chrome brightens an interior it left unpainted - and spell a tab out through one [label pass](render/gl/tabs/TabLabelRenderer.java) - so a chrome contributes the surface the shades are laid onto and nothing else. What each lays them onto is where the two part: a strip is a run of solid fills over a ruled baseline, its channels filled with its own backing where its tabs stand parted rather than showing whatever the panel floats on, where a button is a constant two-tone frame over a backing with only its interior answering to the look. Their palettes part with them, each chrome taking the fill rule its vanilla counterpart is painted by |
| [`render/gl/panel`](render/gl/panel/) | GL | a whole panel's chrome: the [box](render/gl/panel/PanelRenderer.java) and its frame, the scrollbar, and the collapse notch with its state and chevron arms |
| [`render/gl/tooltip`](render/gl/tooltip/) | GL | the [hover box](render/gl/tooltip/CursorTooltipRenderer.java), the style it draws from, and the [leader rule](render/gl/tooltip/TooltipLeaderLineRenderer.java) led between a row's label and its value - the GL answer to what [`widgets/tooltip`](widgets/tooltip/) holds neutrally |
| [`debug`](debug/) | GL | the on-screen debug HUD |
| [`input`](input/) | GL | `UiCursor` - the pointer in UI units, which is the LWJGL mouse rescaled along the [screen axis](screen/ScreenAxis.java) it was read from, so an x cannot be rescaled by the screen's height - and the [`CursorPosition`](input/CursorPosition.java) port over it, which is what a rule comparing the pointer against a box takes when that rule has to be exercisable without a display, the panel input controllers, the one hit resolver a hover and a press both read - which control of a laid-out panel a point is on and which cell of that control, answered subject to what is actually on screen (a folded header presents no tabs, a folded body is behind its rail, a scrolled row is behind its viewport) and to nothing else, with whether pressing that cell would *act* left to the firing path alone, which is what lets a tabs row light the tab it is already showing while firing nothing there - and the motions they hold in answer to input: the [hover fades](input/HoverFades.java) saying how far each element has travelled onto its hovered look - keyed by index for a row whose order outlives a fade, and by the [slot](input/BodyCellSlot.java) it stands in for a body strip rebuilt every frame, a hover belonging to the place under the pointer rather than to the widget occupying it, so a strip that changes under a still cursor keeps one continuous lift instead of dipping dark and starting again - the [press lifts](../../animation/PulseEnvelopes.java) saying how far through its lift each pressed element is - a tab's held at its peak while the button is down and released wherever the pointer has got to by then, a press on a tab being an act with a duration of its own, while a body cell's times its own fall, that control having acted on the way down with nothing left to let go of by the time the button comes up. A body cell's lift is keyed by the same slot its fade is and held one level down on the [body's own controller](input/PanelController.java), which is the end a body press lands on - and charged from the panel above it, which steps that lift with its own rather than keeping a second set of envelopes over the same cells. Stepping it is the holder's obligation and cannot be checked from below: a lift is spent by being advanced, so a surface that never runs the pass shows none of its presses rather than showing them stuck. It is a channel of its own rather than a reading composed into the hover, the two answering different questions about one cell - where the pointer is standing, and what it just did there - and what the second one is painted as is [the style's](render/gl/style/ControlPressLight.java). Beside them runs a bound key's blink, which is an envelope like a press but read with the fades - it carries a tab onto the hovered look rather than past it, so the two compose by the greater of them and a blink under the pointer shows nothing. All run on [`EasedFraction`](../../animation/EasedFraction.java) at one [pair of durations](../../animation/TraverseDurations.java), so every animation on a panel eases and paces alike - and every one of them arrives at twice the speed it leaves at, a motion answering the player having to land under the gesture that asked for it while letting go answers nothing. Beside the fractions sit the [arrivals](input/HoverArrival.java) - the *moment* the pointer reaches an element, [keyed](input/KeyedHoverArrival.java) for a row - where a fraction is the position it holds afterwards. Anything owed on reaching an element rather than while on it reads one of those, a fraction parked at 1 saying "on it" and never "just got here". The controllers detect the moments a panel answers audibly - a press landing, one of those arrivals, the wheel moving a list - and ask the look's [`UiSoundScheme`](sound/UiSoundScheme.java) what each sounds like, naming neither role nor volume themselves - only, for an arrival, what kind of thing the pointer reached, that being the one part of the question a detector is what knows. For a body cell that kind is read off the control while the hit-test still holds it and [carried on beside the slot](input/HoveredBodyCell.java), so the slot a fade is keyed by learns nothing about the widget standing in it and no reader downstream has to ask a second time. A body control's press is answered off that same resolver rather than off the firing below it - sounded and lifted from the one cell it resolved, so the two answers cannot part - and a press landing on an inert cell - a re-pick of a lit segment, the one press the screen answers with no change at all - therefore still answers like the press it was, while chrome resolving no cell stays silent and unlit by the same rule rather than by a second one. The wheel is answered where the wheel is - on the headerless [`PanelController`](input/PanelController.java), so a panel with no tabs presses and scrolls audibly too - and on the list having *moved* rather than on the notch having turned, which is why the offset settles against the drawn overflow at the event rather than at the next layout: a request that will be pulled back is not a list that went anywhere. That movement is also what the body's arrival latch [adopts](input/KeyedHoverArrival.java) on, taking whatever the scroll carried under the cursor without announcing it. An arrival is the player reaching something, and rows sliding past a parked pointer were reached by nobody - so one wheel turn is one sound rather than one per row it swept, and a thumb drag, silent by the same rule, cannot announce the rows it carries either. Beside them sits [how a claim is made](input/PointerParking.java), which differs by the kind of event and matters to the screen the panel is drawn over. A press or a wheel is consumed, the panel acting on it being the reason the screen must not. A move is not: a consumed event is invisible to the screen, and a vanilla control only lets go of its hover on hearing a move that is not on it, so consuming one leaves whatever was lit when the pointer crossed onto the panel lit for as long as the pointer stays there. So a move is claimed by parking the pointer instead - the event is left unconsumed and moved far off every widget - and each widget beneath then reaches the true conclusion that the pointer is not on it, the lit control letting go while nothing behind the panel takes its place. One rule for every control on the screen, including ones a later game version adds. The real event is moved rather than a substitute passed, because the engine hands input listeners a copy of the frame's list and gives the original to the screen: only a change to the event itself reaches both. The setters that move it are on the game's own event type rather than the modding interface, so they are reached by name off the instance in hand, and a move that cannot be made is consumed instead - the stale hover, never a live pointer at its real position |
| [`highlight`](highlight/) | vanilla | `Highlight`, `HighlightedParagraph`, `HighlightedMessage` |
| [`tooltip`](tooltip/) | vanilla | [`Tooltips`](tooltip/Tooltips.java), the `TooltipCreator` boilerplate wrapper - the vanilla surface's answer to what [`widgets/tooltip`](widgets/tooltip/) holds neutrally |
| [`intel`](intel/) | split | the screen-view port and its vanilla implementation |
| [`coreui`](coreui/) | split | [`CoreUiTree`](coreui/CoreUiTree.java), the by-name reach into the live widget tree that every screen's probes walk; the [repaint port](coreui/CoreUiComponentRepainter.java) with its [reflective implementation](coreui/ReflectiveCoreUiComponentRepainter.java), which drives a component's own draw through that same reach, clipped to a region a caller hands it - the one thing here that writes to the tree rather than reading it; and [`CampaignScreenView`](coreui/CampaignScreenView.java), which answers the coarse questions about the core UI without walking into any one screen: which core tab is up, and whether *none* of it is and the player is looking at the campaign world itself (that second one being the first plus "no interaction dialog"). The tab is published API with one correction, and the correction is why the read exists rather than each caller asking the campaign UI: a dialog goes on handing out the core UI of a screen the player has closed, still naming the tab it was showing, so a tab it reported stands only while that core is still on screen - the one thing here that reaches the tree, and only far enough to ask whether a panel is still drawn |
| [`screen`](screen/) | split | how big the window is, in each of the two spaces a UI pass straddles - the UI units a widget is laid out in, and the framebuffer pixels the mouse and a GL clip are stated in. They coincide only at a pixel scale of 1, so a number taken from the wrong one is right on the machine it was written on and wrong on a scaled display. [`ScreenAxis`](screen/ScreenAxis.java) is one axis measured in both at once and converts between them, which is where the guard against that lives: a pixel length is never reachable apart from its own UI partner, so there is no arrangement of the two floats for a caller to transpose, and the conversion is pure arithmetic exercisable with no display. [`VanillaScreen`](screen/VanillaScreen.java) is what measures a real one - the whole screen as a box, the UI axes loose beside it for the callers comparing a widget rather than converting, and the axes bound for those that are. Read afresh at every ask, a window being resizable mid-session |
| [`suppression`](suppression/) | vanilla | the write that stops a widget rendering at all: [`OffScreenWidgetSuppressor`](suppression/OffScreenWidgetSuppressor.java), the script that holds a widget's own opacity at zero while its owner has it parked clear of the screen, and hands back the value it was found at when it returns. A component drawn to nothing renders no subtree, which is the work saved - and, where that subtree is something the game reads answers from, the second answer removed from a frame meant to carry one: a sector map composited into somebody's panel iterates the sector's terrain and binds a transform of its own whether or not anyone can see it. Which widget it may be pointed at is a port, writing into another mod's panel being a decision only the caller can take, and the [screen](screen/VanillaScreen.java) the box is compared against is read live, so a resized window moves both sides of the comparison. The reading is where the widget *is* rather than what shows of it, the two parting the moment this writes: a read that sifted out widgets drawn to nothing would stop reporting the very widget it had just switched off, leaving it parked for good |
| [`map/transform`](map/transform/) | split | the modelview-matrix port, its GL and Fast Rendering implementations, the [selector](map/transform/ModelviewMatrixReaders.java) between them, and the [transform](map/transform/CampaignMapTransform.java) and [cursor read](map/transform/MapCursor.java) built over it |
| [`map/presence`](map/presence/) | vanilla | what the game is showing: the sector map's own [view state](map/presence/CampaignMapView.java) as one [classified answer](map/presence/SectorMapState.java), plus the host-blind [reads](map/presence/MapPresence.java) that fold it together with the intel screen's map |
| [`map/icons`](map/icons/) | vanilla | the one write into the map widget's draw order: [`MapIconReseater`](map/icons/MapIconReseater.java), the script that takes a caller's entity out of its location for a single advance so its icon re-enters at the tail, over the [rule](map/icons/MapIconReseatDecision.java) that says when one is owed - stated over where the icon sits rather than over a map having opened, so a re-seeding nobody witnessed corrects itself. Which entity, which maps matter, and where that icon currently sits are all ports: a rule about the first two would be a guess about somebody's content, and reading the third itself would make this package depend on the probes |
| [`map/probes`](map/probes/) | vanilla | the live reads into the map's widget tree over [`CoreUiTree`](coreui/CoreUiTree.java)'s by-name reach: [`ShownMapTab`](map/probes/ShownMapTab.java), the [surface bounds](map/probes/MapSurfaceBounds.java) that pick the map out of that tab by shape as a [surface area](map/probes/MapSurfaceArea.java) carrying the chrome drawn with it, the [finder](map/probes/EmbeddedMapFinder.java) that answers where the maps which are *not* that tab stand, as [one value per map](map/probes/EmbeddedMap.java) carrying the chain it hangs under and answering, live at each ask, the box it is currently drawn in - never kept, a panel a mod slides on and off screen having moved by the time a held one is read - and, for a caller whose subject is the widget rather than what shows of it, that map as a component, which answers where the box read does not since a map drawn to nothing is a widget still, and - off that same ancestry - the outermost widget the map is held inside, the one its owner added to the tree, so no caller has to index into a chain and be right about which end of it is which; [`VanillaMapTooltipProbe`](map/probes/VanillaMapTooltipProbe.java) that hands the shown tooltip back rather than a yes/no, so a consumer can step aside for it or draw over it, searching under a root the caller supplies rather than the core tab, since a docked map surface is no tab at all and the frames one is up are frames where no tab is - what a vanilla tooltip *is* stays the probe's, which surfaces exist is the caller's, the [widget trace](map/probes/MapTabWidgetTrace.java) that describes what the cursor is inside, the [host trace](map/probes/EmbeddedMapHostTrace.java) that names whoever owns an embedded map off that same finder's walk, the [icon-order trace](map/probes/MapIconOrderTrace.java) that describes which terrain the widget paints over which - all three handing the line back for a consumer to log as its own - and the [layering probe](map/probes/MapIconLayeringProbe.java) that answers the same subject for one entity as a value to act on. Both icon reads reach the icon map through one shared [reader](map/probes/MapWidgetIcons.java), so the walk and the one warning about it stopping working exist once. The two probes that want *the first* widget under a root answering for something - that icon reader and the tooltip probe - descend through one [subtree search](map/probes/SubtreeSearch.java); the walks that collect rather than stop do not, each carrying something different down with it (a full ancestry, a parent and a depth, a capped running set), so folding them together would need a visit result and a context object to save five lines of skeleton apiece |

The map is four packages rather than one. Three of them read different things - GL state, the
campaign's persisted UI data, the live widget tree - and the fourth reads nothing at all: it
acts, moving an entity so the widget seeds its icon later. **No class in any of them references
another's.** That independence is the reason the split is worth keeping: a probe that started
reaching for the transform, or a presence read that had to walk the tree, would be the signal
that one of these has taken on a job belonging to another.

[`MapIconLayering`](map/MapIconLayering.java) sits above the four rather than in one of them, and
is the only thing they share. `map/probes` reads a placement and `map/icons` acts on one, so both
need the word for it and neither can own it - putting it in either would be exactly the reference
the rule above forbids. The act half takes its placement as a port, wired by the consuming mod, so
it still reads nothing.

`map/icons` and the icon reads in `map/probes` are the write and the read of one subject, and they
are apart on the tier the rest of this table splits on: a probe describes the live tree and never
touches it, which is a contract worth keeping literal. Those reads are also the only way to check
the reseat landed, since where an icon sits is an insertion-order artefact the engine promises
nothing about.

Two packages are named `tooltip`, and the pair is the tier split rather than a collision to
resolve. `widgets/tooltip` is what a hover box *is* - lines, blocks, and where they land, naming
no drawing surface; `tooltip` is what the vanilla widget API needs to be handed one; and
`render/gl` paints the neutral model itself. One subject, three homes, exactly as the table of
[pairs](#pairs-that-look-like-duplicates) above sets out. An import naming the simple name alone
is the thing to look twice at.

`layout.VanillaPositions` is the one deliberate exception in a neutral package: it holds
vanilla screen coordinates, which are a fact about the game's own layout rather than
about any KM content. It states a position rather than fetching one, which is the line
that keeps it the only exception - `screen.VanillaScreen` is the same subject read live,
and reaching the running game for its numbers is exactly what puts it outside the neutral
tier however naturally it would have sat beside the layout maths it feeds. Its counterpart on the test side is
[`PositionFake`](../../../../../testFixtures/java/kmlib/testfixtures/starsector/ui/layout/PositionFake.java), a widget position
already laid out at a given [`Rectangle`](../../math/geometry/Rectangle.java) - anything that reads
where a widget is takes one of those instead of stubbing six of `PositionAPI`'s thirty-odd methods,
and a consuming mod's tests can lay a widget out without a mocking framework to describe a box.
