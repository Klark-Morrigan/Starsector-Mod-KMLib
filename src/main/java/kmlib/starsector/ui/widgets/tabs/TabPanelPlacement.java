package kmlib.starsector.ui.widgets.tabs;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.PanelPlacement;

/**
 * One laid-out tab panel: a headerless {@link PanelPlacement} for the {@code body} with a {@code
 * tabsHeader} control overlaid on the top band of the body's box. All geometry is in UI coordinates, so
 * the same placement a renderer draws is the one a consumer hit-tests, with no conversion. The {@code
 * tabsHeader} is an ordinary laid-out {@link kmlib.starsector.ui.controls.ControlSpec.Tabs} control (its
 * segments split per tab), so it measures, draws, and hit-tests through the generic control path like any
 * body control - the panel owns only where the header sits.
 *
 * <p>The body's {@link PanelPlacement#box()} spans the border, the header band, and the body vertically, so
 * it is the single bordered frame both the header and the body draw within: a tab panel is a panel with a
 * header overlaid, not a header wrapping a second bordered panel, so there is one border, not two. Its
 * width tracks the body alone, so a tab row wider than the body overhangs the frame rather than widening
 * it. A host that needs the footprint reads it off {@code body().box()}.
 *
 * <p>The {@code notch} is the collapse handle: a rect protruding past the box's right border edge, centred
 * on the frame. It rides that edge as the body collapses, so the render pass draws it and the input pass
 * hit-tests it against the one rect, and the handle tracks the shrinking edge to stay reachable when the
 * panel is docked. It is null for a bodyless panel: with no body to collapse the panel is not collapsible,
 * so it exposes no handle, and the render and input passes both skip it. Consumers reading {@code notch}
 * must null-check it.
 *
 * <p>The {@code border} is the frame the box was laid out around, carried on the placement so a renderer
 * strokes the width the layout actually reserved rather than reading it back from wherever the layout read
 * it. Those two reads agreeing is what keeps the stroke inside the box: a frame stroked wider than the inset
 * the layout spent would overlap the content it was supposed to sit outside. A host that strokes a different
 * set of edges than the layout reserved - one dropping an edge it now sits flush against - still composes
 * its own {@link BoxBorder}, since which edges are open is its decision and can only be made once the box
 * has a resolved position; the width is not.
 *
 * @param tabsHeader the laid-out tabs control across the header band, its segments split per tab
 * @param body       the headerless panel placement beneath the header (its box frames the header band too)
 * @param border     the frame the box was laid out around - the width every stroke of it must use
 * @param notch      the collapse-handle rect on the box's right border edge, centred on the frame, or null
 *                   when the panel has no body to collapse and so no handle
 */
public record TabPanelPlacement(
    Control tabsHeader,
    PanelPlacement body,
    BoxBorder border,
    Rectangle notch) {
}
