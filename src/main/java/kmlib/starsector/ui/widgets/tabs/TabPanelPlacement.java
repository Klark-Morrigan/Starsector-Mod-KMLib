package kmlib.starsector.ui.widgets.tabs;

import kmlib.starsector.ui.controls.Control;
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
 * @param tabsHeader the laid-out tabs control across the header band, its segments split per tab
 * @param body       the headerless panel placement beneath the header (its box frames the header band too)
 */
public record TabPanelPlacement(Control tabsHeader, PanelPlacement body) {
}
