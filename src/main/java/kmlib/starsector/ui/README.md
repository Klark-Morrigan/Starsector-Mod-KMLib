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
- [Pairs that look like duplicates](#pairs-that-look-like-duplicates)
  - [`Highlight` versus `TextSpan`](#highlight-versus-textspan)
- [Ports across the boundary](#ports-across-the-boundary)
- [Two span measurers](#two-span-measurers)
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

## Pairs that look like duplicates

| What it expresses | Neutral | Vanilla-surface form | GL-surface form |
| --- | --- | --- | --- |
| a run of text and its colour | [`TextSpan`](text/TextSpan.java) | [`Highlight`](highlight/Highlight.java) | [`LabelRenderer`](render/gl/LabelRenderer.java) |
| where text sits at its draw point | [`TextAlignment`](text/TextAlignment.java) | `api.ui.Alignment` | `LazyFont.TextAnchor` |
| a line with parts in other colours | a label's runs on [`TooltipRow`](widgets/TooltipRow.java) | [`HighlightedParagraph`](highlight/HighlightedParagraph.java) | [`CursorTooltipRenderer`](render/gl/CursorTooltipRenderer.java) |
| a hover tooltip | [`CursorTooltip`](widgets/CursorTooltip.java) | [`Tooltips`](tooltip/Tooltips.java) | [`CursorTooltipRenderer`](render/gl/CursorTooltipRenderer.java) |
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

## Ports across the boundary

Where the neutral middle needs something only a surface can answer, it takes a port and
the surface supplies the implementation:

| Port | Answers | Implementations |
| --- | --- | --- |
| [`TextSpanMeasurer`](font/TextSpanMeasurer.java) | the width of a run in a face | [`LazyFontSpanMeasurer`](font/LazyFontSpanMeasurer.java) |
| [`LineWidthMeasurer`](font/LineWidthMeasurer.java) | the width of a line | [`LazyFontMeasurer`](font/LazyFontMeasurer.java) |
| [`LabelLengthEstimator`](label/LabelLengthEstimator.java) | how long a label will draw | [`FontLabelLengthEstimator`](label/FontLabelLengthEstimator.java), [`AspectLabelLengthEstimator`](label/AspectLabelLengthEstimator.java) |
| [`ModelviewMatrixReader`](map/ModelviewMatrixReader.java) | the campaign map's transform | [`GlModelviewMatrixReader`](map/GlModelviewMatrixReader.java), [`FastRenderingModelviewMatrixReader`](map/FastRenderingModelviewMatrixReader.java) |
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

## Where each package sits

| Package | Tier | Holds |
| --- | --- | --- |
| [`text`](text/) | neutral | `TextSpan`, `TextStyle`, `TextAlignment`, `StyledSpanMeasurer` |
| [`controls`](controls/) | neutral | the sealed `ControlSpec` set and its enums |
| [`widgets`](widgets/) | neutral | row and box content plus their geometry ([`tabs`](widgets/tabs/), [`scroll`](widgets/scroll/), [`segments`](widgets/segments/)) |
| [`layout`](layout/) | neutral | box placement, strips, padding, screen anchors |
| [`label`](label/) | neutral | label fitting, plus the length-estimator port |
| [`color`](color/) | neutral | `StarsectorUiColor`, the checked wrapper over vanilla's colour getters |
| [`font`](font/) | split | measurement ports, and their LazyFont-bound implementations and [caches](../../../../../../docs/dev/caching.md) |
| [`render/gl`](render/gl/) | GL | every raw-GL painter, its styles, and the [state guard](render/gl/GlStateGuard.java) |
| [`debug`](debug/) | GL | the on-screen debug HUD |
| [`input`](input/) | GL | `UiCursor` and the panel input controllers |
| [`highlight`](highlight/) | vanilla | `Highlight`, `HighlightedParagraph`, `HighlightedMessage` |
| [`tooltip`](tooltip/) | vanilla | `Tooltips`, the `TooltipCreator` boilerplate wrapper |
| [`intel`](intel/) | split | the screen-view port and its vanilla implementation |
| [`map`](map/) | split | the transform port, its two implementations, [`VanillaMapTooltip`](map/VanillaMapTooltip.java) |

`layout.VanillaPositions` is the one deliberate exception in a neutral package: it holds
vanilla screen coordinates, which are a fact about the game's own layout rather than
about any KM content.
