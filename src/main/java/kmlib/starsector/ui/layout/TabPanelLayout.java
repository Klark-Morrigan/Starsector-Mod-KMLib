package kmlib.starsector.ui.layout;

import kmlib.math.geometry.BoxEdge;
import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.widgets.PanelChrome;
import kmlib.starsector.ui.widgets.PanelPlacement;
import kmlib.starsector.ui.widgets.scroll.ScrollbarThickness;
import kmlib.starsector.ui.widgets.tabs.TabPanelPlacement;
import kmlib.starsector.ui.widgets.tabs.TabPanelViewState;
import kmlib.starsector.ui.widgets.tabs.style.TabStyle;

import java.util.List;

/**
 * Composes a tab panel: a tabs-control header standing ON a bordered body box, the way a tab strip sits on
 * the panel it selects rather than inside it. The row hangs from the panel's own top anchor with no border
 * above it, and the framed body starts where the row ends, so nothing of the body reaches behind the tabs -
 * the row is opaque chrome in its own right. Across, it starts where the body's content does: the frame is
 * drawn down the body alone, so a row laid at the box's outer edge would overhang a frame that is not
 * beside it. It REUSES {@link PanelLayout}'s framing - the same
 * {@link PanelLayout#computeContentOrigin} anchor and {@link PanelLayout#framePlacement} that frame a plain
 * panel - so the body beneath the row is framed exactly as a headerless panel is, and the tab row never
 * drives the box width: a tab row wider than the body overhangs the frame rather than stretching it. It
 * also reuses {@link CappedStripLayout#layoutBodyStrip} for the body and
 * adds {@link TabsControlLayout#layoutHeaderControl} for the header, so the only thing unique here
 * is where the header sits. How tall that header stands is an injected {@link TabStyle} rather than a fixed
 * constant, so two panels composed through this one path can size their tab rows to their own surroundings.
 *
 * <p>A tab whose body is empty is its tab row and nothing else: no frame is laid out beneath it, so the
 * panel claims no footprint under a row it does not fill, and the row alone is what the passes draw and
 * hit-test.
 *
 * <p>The header is laid through {@link TabsControlLayout#layoutHeaderControl}, so a header tab measures,
 * draws, and hit-tests through the same tabs-row geometry a body {@link ControlSpec.Tabs} control uses. UI
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
     * Lays the tab panel out for the given screen height, padding, chrome, tab style, tabs, and body
     * controls: a header band hung from the panel's anchor carrying the tabs control at the style's height,
     * and the body strip framed beneath it (capped to the bottom margin). The returned body's
     * {@link PanelPlacement#box()} frames the body alone - its width the body's and its height the body's -
     * so the frame sits under the tab row rather than around it, and a tab row wider than the body overhangs
     * it. An empty {@code bodyControls} leaves the tab row standing alone: no box, and, with nothing to
     * collapse, no notch either - the placement's collapse handle is absent.
     *
     * @param screenHeight the UI-coordinate screen height, giving the top edge to hang from
     * @param padding      the panel's edge margins: the top-left anchor and the bottom keep-clear
     *                     margin the body caps to (the right inset is unused - a panel grows rightward)
     * @param chrome       the room the panel spends on chrome rather than content: the border framing the
     *                     footprint, where an open edge reserves no inset so the box sits flush against a
     *                     neighbour, and the bar thickness the body reserves a gutter for - a bar past
     *                     what the body's own padding holds clear widens the body and with it the box
     * @param tabStyle     the tab dimensions the header band is laid to, so two panels sharing this one
     *                     layout can still stand their tab rows at different heights; the band height is
     *                     content-space, standing under the top border rather than including it
     * @param tabsSpec     the tabs control (labels + per-tab shortcuts) drawn across the header band
     * @param bodyControls the active tab's body controls, top to bottom (empty for no body)
     * @param measurer     measures each label's rendered width for text snapping
     * @param viewState    how far the panel is scrolled and folded
     * @return the laid-out tabs header, how much of that header the fold leaves on screen, the body
     *         placement carrying the framed box, the border it was framed around, and the collapse-handle
     *         notch on the box's right border edge - null when {@code bodyControls} is empty, since a
     *         bodyless panel has nothing to collapse
     */
    public static TabPanelPlacement computePlacement(
            float screenHeight,
            Padding padding,
            PanelChrome chrome,
            TabStyle tabStyle,
            ControlSpec.Tabs tabsSpec,
            List<ControlSpec> bodyControls,
            LineWidthMeasurer measurer,
            TabPanelViewState viewState) {

        // How tall the tabs stand, which is what everything below frames against. Not the band: the two
        // part where a chrome states a box shorter than its band, and the pixel between them is the row's
        // to rule its baseline in. The band itself is the header's own business, spent inside the call
        // that lays the row out.
        var tabHeight = tabStyle.resolveTabHeight();
        var border = chrome.border();

        // The body is framed as a plain headerless panel would be against a screen ending where the TABS
        // end, not where their band does: its box hangs from the tabs' own bottom edge, so its top border
        // is drawn in the very row the strip rules its baseline in and the two coincide instead of stacking
        // into a two-pixel rule with a dead pixel between them. Neither side is told about the other - the
        // row rules under its tabs and the body frames from its own top - and they meet because both are
        // measured from the same edge. Resolved before the row because the row is laid against it.
        var origin = PanelLayout.computeContentOrigin(
            screenHeight - tabHeight,
            padding,
            border);

        // Header: the tabs control hung from the panel's own top anchor, taking no border inset above it -
        // the row stands on the frame rather than inside it, so the frame starts below the row. Reuses the
        // same tab measurement and segment split a body tabs row uses, so it is not bespoke tab-strip
        // framing.
        //
        // Across, it starts at the body's own content edge rather than at the box's outer one. A row laid
        // at the outer edge overhangs its own frame by the border's width, which reads as a row a pixel out
        // of step with whatever the panel was aligned to - and the frame is drawn down the body alone, so
        // there is nothing beside the row for it to have been flush with. Taken from the shared content
        // origin rather than re-derived here, so the row and the body beneath it cannot come to disagree
        // about where the panel's content starts; an unstroked left edge insets by nothing, and both follow
        // it together.
        var tabsHeader = TabsControlLayout.layoutHeaderControl(
            tabsSpec,
            origin.contentX(),
            screenHeight - padding.top(),
            tabStyle,
            measurer);

        // Body: the same shared composition a plain panel frames, hung beneath the header band and capped so
        // the row and the box together clear the bottom margin - the band height counted against the
        // vertical budget the same way a plain panel counts only its own border. The vertical budget spends
        // the border only on the edges that are stroked, so an open top or bottom returns that width to the
        // body.
        // Charged the tab height rather than the band, matching the box's own top: the pixel the band keeps
        // under its tabs is where the body's top border now goes, so it is the body's room and not a strip
        // of nothing above it. The panel's overall footprint is unchanged - the box starts a pixel higher
        // and is allowed a pixel more, so its bottom lands where it always did.
        var maxBodyHeight = screenHeight
            - padding.top()
            - border.computeEdgeInset(BoxEdge.TOP)
            - border.computeEdgeInset(BoxEdge.BOTTOM)
            - tabHeight
            - padding.bottom();

        var bodyStrip = CappedStripLayout.layoutBodyStrip(
            origin.contentX(),
            origin.contentTopY(),
            maxBodyHeight,
            chrome.scrollbarThickness(),
            bodyControls,
            measurer,
            viewState.rawScrollOffset());

        // Collapse the interior horizontally by the fraction: the controls keep their laid-out positions
        // (the renderer clips them to the shrinking box), so only the framed body's width interpolates,
        // from its full width down to nothing at full collapse. The fraction arrives already clamped to the
        // unit range, so an overshooting animation value cannot invert the width here.
        var fullBody = bodyStrip.bounds();
        var framedBody = new Rectangle(
            fullBody.x(),
            fullBody.y(),
            fullBody.width() * (1f - viewState.collapseFraction()),
            fullBody.height());

        var isBodyless = bodyControls.isEmpty();

        // Reuse the plain panel's framing, sizing the box to the interpolated body's width so the tab row
        // never widens it and a collapse narrows the box with the interior. A tab row wider than the body
        // overhangs the frame. The body placement carries that box; the tab panel pairs it with the header
        // and the collapse-handle notch.
        var bodyPlacement = isBodyless
            ? buildBodylessPlacement(padding.left(), origin.boxTopY())
            : PanelLayout.framePlacement(
                padding.left(),
                origin.boxTopY(),
                chrome,
                framedBody,
                bodyStrip);

        // No body controls means nothing to collapse, so the panel is not collapsible and exposes no
        // handle: the notch is left absent. An empty-body panel is just its tab row, and a collapse handle
        // protruding off it would fold a body that is not there.
        var notch = isBodyless
            ? null
            : computeNotchRect(bodyPlacement.box());

        // The border travels on the placement so the pass that strokes it uses the width this layout
        // just spent on insets, rather than reading the same source a second time and hoping the two
        // agree.
        return new TabPanelPlacement(
            tabsHeader,
            computeDrawnHeaderBand(
                tabsHeader.bounds(),
                bodyPlacement.box(),
                // A panel with no body never folds - there is nothing to fold and no handle to ask for it -
                // so a fraction another tab's body left standing in the animation cannot wipe this row.
                !isBodyless && viewState.isFolding()),
            bodyPlacement,
            border,
            notch);
    }

    // What the fold leaves of the tab row on screen. While the body is folding, the row is wiped with it -
    // clipped to the box's own span, so the panel narrows as one piece down to the docked rail; at rest the
    // whole row stands, so a row wider than its body overhangs the frame rather than being cut off at it.
    // One rect for both passes: the row is drawn to it and the pointer is tested against it, so a panel can
    // never claim a strip of screen where its tabs are no longer painted.
    private static Rectangle computeDrawnHeaderBand(
            Rectangle headerBand,
            Rectangle box,
            boolean isFolding) {

        if (!isFolding) {
            return headerBand;
        }
        return headerBand.intersectWith(new Rectangle(
            box.x(),
            headerBand.y(),
            box.width(),
            headerBand.height()));
    }

    // The body placement of a panel that has no body: an empty box at the anchor, carrying no controls. A
    // tab row with nothing beneath it is the whole panel, so there is no frame to stroke and no footprint to
    // claim under the row - framing a zero body would instead leave a border-sized square hanging off the
    // row's left end, drawn and clickable with nothing in it.
    //
    // It carries the default thickness rather than the caller's: there is no body to scroll and so no bar
    // to size, and a placement must still name one.
    private static PanelPlacement buildBodylessPlacement(int leftX, float boxTopY) {

        var emptyBox = new Rectangle(leftX, boxTopY, 0f, 0f);

        return new PanelPlacement(
            emptyBox,
            emptyBox,
            List.of(),
            emptyBox,
            0f,
            0f,
            ScrollbarThickness.DEFAULT);
    }

    // The collapse-handle notch: a rect protruding past the box's right border edge, vertically centred on
    // the frame (NOTCH_CENTRE_OFFSET shifts it off centre). It rides the box's right edge, so as the body
    // collapses leftward the handle tracks the shrinking edge and stays reachable to expand again.
    private static Rectangle computeNotchRect(Rectangle box) {
        var notchY = box.computeCenterY()
            - NOTCH_HEIGHT / 2f
            + NOTCH_CENTRE_OFFSET;
        return new Rectangle(
            box.x() + box.width(),
            notchY,
            NOTCH_WIDTH,
            NOTCH_HEIGHT);
    }
}
