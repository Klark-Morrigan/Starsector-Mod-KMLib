package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;
import kmlib.math.ranges.Ranges;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.widgets.tabs.TabPanelPlacement;

import java.util.List;

/**
 * Composes a tab panel: a tabs-control header over the shared body composition, wrapped in one bordered
 * box. It REUSES {@link PanelLayout}'s framing - the same {@link PanelLayout#computeContentOrigin} anchor
 * and {@link PanelLayout#framePlacement} that frame a plain panel - passing the framed body rectangle and
 * the header band height, so the one border wraps the header band while the tab row never drives the box
 * width: a tab row wider than the body overhangs the frame rather than stretching it. Because {@code
 * framePlacement} frames a body rectangle, not a bordered box, reusing it frames one border, not two. It
 * also reuses {@link CappedStripLayout#layoutBodyStrip} for the body and
 * adds {@link ControlStripLayout#layoutTabsHeader} for the flush header, so the only thing unique here is
 * where the header sits.
 *
 * <p>The header is laid flush at the interior top (no body inset) through {@link
 * ControlStripLayout#layoutTabsHeader}, so a header tab measures, draws, and hit-tests through the same
 * generic control path a body {@link ControlSpec.Tabs} control uses. UI
 * coordinates throughout (origin bottom-left, y grows up); text snapping runs through the injected
 * {@link LineWidthMeasurer}, so the layout is a pure computation. The panel hangs from the screen's
 * top-left by its paddings and caps its height to a bottom margin; that anchoring is the caller's to
 * supply through the paddings.
 *
 * <p>The body collapses horizontally on a fraction: at 0 it lays out at its full width, and as the
 * fraction climbs to 1 the interior narrows to nothing and the box reduces to a border-only rail at the
 * left anchor - the two borders meeting into the single vertical line the collapsed panel docks to. The
 * collapse is horizontal only, so the box keeps its full height throughout. A notch rides the box's right
 * border edge as the collapse handle; exposed on the placement so the render pass draws it and the input
 * pass hit-tests it against one rect, and tracking the shrinking edge so the handle stays reachable to
 * expand the docked panel again.
 */
public final class TabPanelLayout {
    /**
     * The collapse-handle notch's footprint past the right border: how far it protrudes ({@code
     * NOTCH_WIDTH}), how tall it stands ({@code NOTCH_HEIGHT}), and the vertical shift off the frame's
     * centre ({@code NOTCH_CENTRE_OFFSET}, 0 keeping it centred). Public so the renderer draws the notch
     * and the input pass hit-tests it against the one rect the layout exposes on the placement.
     */
    public static final float NOTCH_WIDTH = 16f;
    public static final float NOTCH_HEIGHT = 24f;
    public static final float NOTCH_CENTRE_OFFSET = 0f;

    private TabPanelLayout() {
    }

    /**
     * Lays the tab panel out for the given screen height, padding, border, tabs, and body controls: a
     * {@link ControlStripLayout#TAB_HEIGHT} header band flush under the top border carrying the tabs
     * control, and the body strip framed beneath it (capped to the bottom margin). The returned body's
     * {@link PanelPlacement#box()} sizes its width to the body alone and its height to the header band plus
     * the body, so the one border wraps the header band while a tab row wider than the body overhangs it.
     * An empty {@code bodyControls} leaves the bordered tab row with no body beneath, and, with nothing to
     * collapse, no notch either - the placement's collapse handle is absent.
     *
     * @param screenHeight    the UI-coordinate screen height, giving the top edge to hang from
     * @param padding         the panel's edge margins: the top-left anchor and the bottom keep-clear
     *                        margin the body caps to (the right inset is unused - a panel grows rightward)
     * @param borderWidth     the outer border thickness framing the footprint; 0 leaves no inset
     * @param tabsSpec        the tabs control (labels + per-tab shortcuts) drawn across the header band
     * @param bodyControls    the active tab's body controls, top to bottom (empty for no body)
     * @param measurer        measures each label's rendered width for text snapping
     * @param rawScrollOffset the requested scroll offset for the body's scrolling control, in pixels;
     *                        clamped to its overflow by the capped layout
     * @param collapseFraction how far the body is collapsed horizontally: 0 lays it out at full width, 1
     *                        docks it to the border-only rail; clamped to the unit range
     * @return the laid-out tabs header, the body placement carrying the whole-footprint box, and the
     *         collapse-handle notch on the box's right border edge - null when {@code bodyControls} is
     *         empty, since a bodyless panel has nothing to collapse
     */
    public static TabPanelPlacement computePlacement(
            float screenHeight,
            Padding padding,
            int borderWidth,
            ControlSpec.Tabs tabsSpec,
            List<ControlSpec> bodyControls,
            LineWidthMeasurer measurer,
            float rawScrollOffset,
            float collapseFraction) {
        // The box hangs from the screen's top-left, same anchor a plain panel uses; the header sits flush
        // under the top border and the body hangs beneath the header band.
        var origin = PanelLayout.computeContentOrigin(screenHeight, padding, borderWidth);

        // Header: the tabs control laid flush at the content top, reusing the strip's tab measurement and
        // segment split so it is not bespoke tab-strip framing.
        var tabsHeader = ControlStripLayout.layoutTabsHeader(
                tabsSpec,
                origin.contentX(),
                origin.contentTopY(),
                measurer);
        var bodyTopY = origin.contentTopY() - ControlStripLayout.TAB_HEIGHT;

        // Body: the same shared composition a plain panel frames, hung beneath the header band and capped
        // so the box (header included) clears the bottom margin - the header height counted against the
        // vertical budget the same way a plain panel counts only its own border.
        var maxBodyHeight = screenHeight
                - padding.top()
                - 2f * borderWidth
                - ControlStripLayout.TAB_HEIGHT
                - padding.bottom();
        var bodyStrip = CappedStripLayout.layoutBodyStrip(
                origin.contentX(),
                bodyTopY,
                maxBodyHeight,
                bodyControls,
                measurer,
                rawScrollOffset);

        // Collapse the interior horizontally by the fraction: the controls keep their laid-out positions
        // (the renderer clips them to the shrinking box), so only the framed body's width interpolates,
        // from its full width down to nothing at full collapse.
        var fullBody = bodyStrip.bounds();
        var clampedFraction = Ranges.clampToUnit(collapseFraction);
        var framedBody = new Rectangle(
                fullBody.x(),
                fullBody.y(),
                fullBody.width() * (1f - clampedFraction),
                fullBody.height());

        // Reuse the plain panel's framing, sizing the box to the interpolated body's width so the tab row
        // never widens it and a collapse narrows the box with the interior, and adding the header band so
        // the one border wraps it. A tab row wider than the body overhangs the frame. The body placement
        // carries that box; the tab panel pairs it with the header and the collapse-handle notch.
        var bodyPlacement = PanelLayout.framePlacement(
                padding.left(),
                origin.boxTopY(),
                borderWidth,
                framedBody,
                ControlStripLayout.TAB_HEIGHT,
                bodyStrip);
        // No body controls means nothing to collapse, so the panel is not collapsible and exposes no
        // handle: the notch is left absent. An empty-body panel is just its bordered tab row, and a
        // collapse handle protruding off it would fold a body that is not there.
        var notch = bodyControls.isEmpty() ? null : computeNotchRect(bodyPlacement.box());
        return new TabPanelPlacement(tabsHeader, bodyPlacement, notch);
    }

    // The collapse-handle notch: a rect protruding past the box's right border edge, vertically centred on
    // the frame (NOTCH_CENTRE_OFFSET shifts it off centre). It rides the box's right edge, so as the body
    // collapses leftward the handle tracks the shrinking edge and stays reachable to expand again.
    private static Rectangle computeNotchRect(Rectangle box) {
        var notchY = box.computeCenterY() - NOTCH_HEIGHT / 2f + NOTCH_CENTRE_OFFSET;
        return new Rectangle(box.x() + box.width(), notchY, NOTCH_WIDTH, NOTCH_HEIGHT);
    }
}
