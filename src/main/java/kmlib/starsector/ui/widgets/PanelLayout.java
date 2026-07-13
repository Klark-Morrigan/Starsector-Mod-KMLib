package kmlib.starsector.ui.widgets;

import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.layout.CappedStripLayout;
import kmlib.starsector.ui.layout.ControlStripLayout;

import java.util.List;

/**
 * Composes a panel: it frames a tab row over a body of controls. The frame, tab row, and body-region
 * geometry is the reusable {@link TabPanel} (an outer border wrapping a vanilla-styled tab strip over a
 * framed body), and the body itself is the reusable {@link ControlStripLayout} (capped and scrollable via
 * {@link CappedStripLayout}); this class owns only the composition - measure the control strip, size the
 * panel around it, then place the strip inside the framed body - so a host's concrete content is just one
 * tab's body and another tab plugs in without touching the frame math or the strip math.
 *
 * <p>Measuring the strip once and reusing its row dimensions to place the controls lets a renderer and an
 * input listener share one {@link PanelPlacement}, so what is drawn is exactly what the player clicks. UI
 * coordinates throughout (origin bottom-left, y grows up); text snapping runs through the injected {@link
 * LineWidthMeasurer}, so the layout depends on a width measurement rather than a concrete font and stays a
 * pure computation. The panel hangs from the screen's top-left by its paddings and caps its height to a
 * bottom margin; that anchoring is the caller's to supply through the paddings.
 */
public final class PanelLayout {
    // The tab row's height and the tab-label face size, measured here and drawn by a renderer at the one
    // value each, so a snapped tab width matches the text painted into it. TAB_FONT_SIZE is public so a
    // renderer draws at the same size this measured. The body's own geometry lives on ControlStripLayout,
    // since the body is the reusable control strip, not this composition's.
    static final float TAB_HEIGHT = 24f;
    public static final double TAB_FONT_SIZE = 15d;

    // Slack added to each measured tab label so text does not touch the tab edges, and a floor so a very
    // short label still gives a clickable box.
    static final float TAB_TEXT_PADDING = 16f;
    static final float MIN_TAB_WIDTH = 48f;

    private PanelLayout() {
    }

    /**
     * Lays the panel out for the given screen height, padding, border, tabs, and body controls. The panel
     * frames the tab row and (when the active tab carries controls) a body sized to hold them; this then
     * places each control inside that framed body. An empty {@code bodyControls} leaves the tab row with
     * no body.
     *
     * @param screenHeight    the UI-coordinate screen height, giving the top edge to hang from
     * @param paddingTop      pixels from the screen top to the box's top edge
     * @param paddingLeft     pixels from the screen left to the box's left edge
     * @param paddingBottom   pixels kept clear at the screen bottom; the body caps to this margin and its
     *                        scrolling control gives up the difference
     * @param borderWidth     the outer border thickness framing the footprint; 0 leaves no inset
     * @param tabContents     the tabs' labels and shortcuts, in row order left to right
     * @param bodyControls    the active tab's body controls, top to bottom (empty for no body)
     * @param measurer        measures each label's rendered width for text snapping
     * @param rawScrollOffset the requested scroll offset for the body's scrolling control, in pixels;
     *                        clamped to its overflow by the capped layout
     * @return the box, body, tabs, laid-out body controls, and the scroll geometry, in UI coordinates
     */
    public static PanelPlacement computePlacement(float screenHeight, int paddingTop,
            int paddingLeft, int paddingBottom, int borderWidth, List<VanillaTabContent> tabContents,
            List<ControlSpec> bodyControls, LineWidthMeasurer measurer, float rawScrollOffset) {
        // Measure the control strip first so the panel can size the box around both the tab row and the
        // body; the measured row dimensions are reused to place each control once the body is framed. An
        // empty strip carries no rows, which sizes the body to the panel's absent size so a bodyless tab
        // reserves nothing beneath the tab row.
        var strip = ControlStripLayout.measureStrip(bodyControls, measurer);
        // Cap the body so the box never runs past the bottom margin: the room the tab row and both border
        // edges do not take, less the margin to keep clear. The capped layout shrinks only the scrolling
        // control (nothing when the body already fits or has no scrolling control), so the box stays put.
        var flexIndex = CappedStripLayout.findScrollingIndex(bodyControls);
        var maxBodyHeight = screenHeight - paddingTop - 2f * borderWidth - TAB_HEIGHT - paddingBottom;
        var bodyHeight = CappedStripLayout.capBodyHeight(strip, flexIndex, maxBodyHeight);
        var bodySize = strip.rowHeights().isEmpty()
                ? TabPanelBodySize.NONE
                : new TabPanelBodySize(strip.bodyWidth(), bodyHeight);
        var placement = TabPanel.layout(screenHeight, paddingTop, paddingLeft, borderWidth,
                TAB_HEIGHT, TAB_TEXT_PADDING, MIN_TAB_WIDTH, TAB_FONT_SIZE, tabContents, bodySize,
                measurer);
        var capped = CappedStripLayout.layoutCappedControls(placement.body(), bodyControls,
                strip.rowHeights(), strip.rowWidths(), flexIndex, rawScrollOffset);
        return new PanelPlacement(placement, capped.controls(), capped.flexViewport(),
                capped.scrollOffset(), capped.scrollOverflow());
    }
}
