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
 * - so a host's concrete content is just a column of controls and the frame math stays here. Its framing
 * primitives ({@link #computeContentOrigin}, {@link #framePlacement}) are shared: {@link TabPanelLayout}
 * reuses them, framing its body rectangle beneath a header band, so a tab panel is this panel's framing
 * plus a header - one border and all.
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
    public static PanelPlacement computePlacement(
            float screenHeight,
            Padding padding,
            int borderWidth,
            List<ControlSpec> bodyControls,
            LineWidthMeasurer measurer,
            float rawScrollOffset) {
        // The box hangs from the screen's top-left; the content is inset by the border on every edge.
        var origin = computeContentOrigin(screenHeight, padding, borderWidth);
        // The shared body composition, capped so the box never runs past the bottom margin.
        var maxBodyHeight = screenHeight
                - padding.top()
                - 2f * borderWidth
                - padding.bottom();
        var bodyStrip = CappedStripLayout.layoutBodyStrip(
                origin.contentX(),
                origin.contentTopY(),
                maxBodyHeight,
                bodyControls,
                measurer,
                rawScrollOffset);
        // A plain panel's framed content is just the body, with no header band above it.
        return framePlacement(
                padding.left(),
                origin.boxTopY(),
                borderWidth,
                bodyStrip.bounds(),
                0f,
                bodyStrip);
    }

    /**
     * The panel's framing origin: the box's top edge and the border-inset content top-left both a plain
     * panel and a tab panel hang their content from. Shared with {@link TabPanelLayout} so the two anchor
     * identically and only their content differs.
     *
     * @param screenHeight the UI-coordinate screen height, giving the top edge to hang from
     * @param padding      the panel's edge margins
     * @param borderWidth  the outer border thickness inset on every edge
     * @return the box top edge and the inset content top-left, in UI coordinates
     */
    static ContentOrigin computeContentOrigin(float screenHeight, Padding padding, int borderWidth) {
        var boxTopY = screenHeight - padding.top();
        return new ContentOrigin(boxTopY, padding.left() + (float) borderWidth, boxTopY - borderWidth);
    }

    /**
     * Frames a body rectangle in a bordered box and assembles the panel placement: the box wraps that body
     * plus any header band above it with the border on every edge, hung from {@code boxTopY} at {@code
     * leftX}, and the placement carries the box with the same body rectangle and the strip's controls and
     * scroll geometry. The shared framing both a plain panel and a tab panel use - they differ only in the
     * header band (a plain panel passes none) - so {@link TabPanelLayout} reuses it rather than re-framing.
     * It frames the passed body rectangle rather than a bordered box, so a tab panel reusing it frames one
     * border, not two.
     *
     * <p>The box reports the passed {@code framedBody} as its interior, so {@code box == body + border on
     * each edge} holds even when a collapsing tab panel hands in a body narrower than the strip it laid its
     * controls into: the box and interior collapse together while the controls keep their laid-out
     * positions for the renderer to clip.
     *
     * @param leftX           the box's left edge (the panel's left margin), in UI coordinates
     * @param boxTopY         the box's top edge, in UI coordinates
     * @param borderWidth     the outer border thickness inset on every edge
     * @param framedBody      the body rectangle the box frames and reports as its interior
     * @param headerBandHeight the height of the header band above the body (0 for a plain panel)
     * @param bodyStrip       the laid-out body strip whose controls and scroll geometry the placement carries
     * @return the panel placement: the bordered box plus the framed body, controls, and scroll geometry
     */
    static PanelPlacement framePlacement(
            int leftX,
            float boxTopY,
            int borderWidth,
            Rectangle framedBody,
            float headerBandHeight,
            CappedStripLayout.BodyStrip bodyStrip) {
        var contentHeight = headerBandHeight + framedBody.height();
        var box = new Rectangle(
                leftX,
                boxTopY - (contentHeight + 2f * borderWidth),
                framedBody.width() + 2f * borderWidth,
                contentHeight + 2f * borderWidth);
        var capped = bodyStrip.placement();
        return new PanelPlacement(
                box,
                framedBody,
                capped.controls(),
                capped.flexViewport(),
                capped.scrollOffset(),
                capped.scrollOverflow());
    }

    /**
     * A panel's framing origin: the box's top edge and the border-inset content top-left.
     *
     * @param boxTopY     the box's top edge, in UI coordinates
     * @param contentX    the content's left edge, inset from the box left by the border
     * @param contentTopY the content's top edge, inset from the box top by the border
     */
    record ContentOrigin(float boxTopY, float contentX, float contentTopY) {
    }
}
