package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.layout.CappedStripLayout;
import kmlib.starsector.ui.layout.ControlStripLayout;

import java.util.List;

/**
 * Composes a headerless panel: it frames a bordered box around a body of controls. The frame is a
 * {@link BorderedBox} (an outer border wrapping the inset body), and the body itself is the reusable
 * {@link ControlStripLayout} (capped and scrollable via {@link CappedStripLayout}); this class owns only
 * the composition - measure the control strip, size the box around it, then place the strip inside the
 * framed body - so a host's concrete content is just a column of controls and the frame math stays here.
 * A {@link TabPanel} builds on this by reserving a header band and overlaying a tabs control on the top
 * of the same box.
 *
 * <p>Measuring the strip once and reusing its row dimensions to place the controls lets a renderer and an
 * input listener share one {@link PanelPlacement}, so what is drawn is exactly what the player clicks. UI
 * coordinates throughout (origin bottom-left, y grows up); text snapping runs through the injected {@link
 * LineWidthMeasurer}, so the layout depends on a width measurement rather than a concrete font and stays a
 * pure computation. The panel hangs from the screen's top-left by its paddings and caps its height to a
 * bottom margin; that anchoring is the caller's to supply through the paddings.
 */
public final class PanelLayout {
    private PanelLayout() {
    }

    /**
     * Lays the panel out for the given screen height, padding, border, and body controls. The box frames
     * a body sized to hold the controls (capped to the bottom margin), then places each control inside
     * that framed body. An empty {@code bodyControls} leaves a minimal bordered box with no body.
     *
     * @param screenHeight    the UI-coordinate screen height, giving the top edge to hang from
     * @param paddingTop      pixels from the screen top to the box's top edge
     * @param paddingLeft     pixels from the screen left to the box's left edge
     * @param paddingBottom   pixels kept clear at the screen bottom; the body caps to this margin and its
     *                        scrolling control gives up the difference
     * @param borderWidth     the outer border thickness framing the footprint; 0 leaves no inset
     * @param bodyControls    the body controls, top to bottom (empty for no body)
     * @param measurer        measures each label's rendered width for text snapping
     * @param rawScrollOffset the requested scroll offset for the body's scrolling control, in pixels;
     *                        clamped to its overflow by the capped layout
     * @return the box, body, laid-out body controls, and the scroll geometry, in UI coordinates
     */
    public static PanelPlacement computePlacement(float screenHeight, int paddingTop,
            int paddingLeft, int paddingBottom, int borderWidth, List<ControlSpec> bodyControls,
            LineWidthMeasurer measurer, float rawScrollOffset) {
        // Measure the control strip first so the box can size around it; the measured row dimensions are
        // reused to place each control once the body is framed. An empty strip carries no rows, which
        // sizes the body to zero so a bodyless panel reserves nothing beneath the border.
        var strip = ControlStripLayout.measureStrip(bodyControls, measurer);
        // Cap the body so the box never runs past the bottom margin: the room the box's own border edges
        // leave, less the margin to keep clear. The capped layout shrinks only the scrolling control
        // (nothing when the body already fits or has no scrolling control), so the box stays put.
        var flexIndex = CappedStripLayout.findScrollingIndex(bodyControls);
        var maxBodyHeight = screenHeight - paddingTop - 2f * borderWidth - paddingBottom;
        var bodyHeight = CappedStripLayout.capBodyHeight(strip, flexIndex, maxBodyHeight);

        // The box hangs from the screen's top-left by its paddings; the content is inset by the border on
        // every edge, so the body clears the stroke. The body hangs from the inset content top down its
        // capped height, and the box wraps it with the border on every edge.
        var boxTopY = screenHeight - paddingTop;
        var contentX = paddingLeft + (float) borderWidth;
        var contentTopY = boxTopY - borderWidth;
        var bodyWidth = strip.bodyWidth();
        var body = new Rectangle(contentX, contentTopY - bodyHeight, bodyWidth, bodyHeight);
        var boxWidth = bodyWidth + 2f * borderWidth;
        var boxHeight = bodyHeight + 2f * borderWidth;
        var box = new Rectangle(paddingLeft, boxTopY - boxHeight, boxWidth, boxHeight);

        var capped = CappedStripLayout.layoutCappedControls(body, bodyControls, strip.rowHeights(),
                strip.rowWidths(), flexIndex, rawScrollOffset, measurer);
        return new PanelPlacement(box, body, capped.controls(), capped.flexViewport(),
                capped.scrollOffset(), capped.scrollOverflow());
    }
}
