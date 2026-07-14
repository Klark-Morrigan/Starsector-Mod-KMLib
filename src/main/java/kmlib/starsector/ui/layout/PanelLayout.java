package kmlib.starsector.ui.layout;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.widgets.PanelPlacement;

import java.util.List;

/**
 * Composes a headerless panel: it frames a bordered box around the shared body composition. The frame is
 * a {@link BorderedBox} (an outer border wrapping the inset body), and the body is {@link
 * CappedStripLayout#layoutBodyStrip} - the measured, capped, scrollable control strip; this class owns
 * only the framing - hang that body from the inset content top and wrap it with the border on every edge
 * - so a host's concrete content is just a column of controls and the frame math stays here. {@link
 * TabPanelLayout} is a sibling, not a subclass or caller: it frames the same body composition beneath a
 * tabs header rather than reusing this class.
 *
 * <p>The one shared {@link PanelPlacement} a renderer draws and an input listener hit-tests is what keeps
 * the drawn box and the clickable box in step. UI coordinates throughout (origin bottom-left, y grows
 * up); text snapping runs through the injected {@link LineWidthMeasurer}, so the layout depends on a width
 * measurement rather than a concrete font and stays a pure computation. The panel hangs from the screen's
 * top-left by its paddings and caps its height to a bottom margin; that anchoring is the caller's to
 * supply through the paddings.
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
     * @param padding         the panel's edge margins: the top-left anchor and the bottom keep-clear
     *                        margin the body caps to (the right inset is unused - a panel grows rightward)
     * @param borderWidth     the outer border thickness framing the footprint; 0 leaves no inset
     * @param bodyControls    the body controls, top to bottom (empty for no body)
     * @param measurer        measures each label's rendered width for text snapping
     * @param rawScrollOffset the requested scroll offset for the body's scrolling control, in pixels;
     *                        clamped to its overflow by the capped layout
     * @return the box, body, laid-out body controls, and the scroll geometry, in UI coordinates
     */
    public static PanelPlacement computePlacement(float screenHeight, Padding padding,
            int borderWidth, List<ControlSpec> bodyControls, LineWidthMeasurer measurer,
            float rawScrollOffset) {
        // The box hangs from the screen's top-left by its paddings; the content is inset by the border on
        // every edge, so the body clears the stroke and hangs from the inset content top.
        var boxTopY = screenHeight - padding.top();
        var contentX = padding.left() + (float) borderWidth;
        var contentTopY = boxTopY - borderWidth;
        // The shared body composition, capped so the box never runs past the bottom margin - the room the
        // box's own border edges leave, less the margin to keep clear.
        var maxBodyHeight = screenHeight - padding.top() - 2f * borderWidth - padding.bottom();
        var bodyStrip = CappedStripLayout.layoutBodyStrip(contentX, contentTopY, maxBodyHeight,
                bodyControls, measurer, rawScrollOffset);
        var body = bodyStrip.bounds();

        // The box wraps the body with the border on every edge.
        var boxWidth = body.width() + 2f * borderWidth;
        var boxHeight = body.height() + 2f * borderWidth;
        var box = new Rectangle(padding.left(), boxTopY - boxHeight, boxWidth, boxHeight);

        var capped = bodyStrip.placement();
        return new PanelPlacement(box, body, capped.controls(), capped.flexViewport(),
                capped.scrollOffset(), capped.scrollOverflow());
    }
}
