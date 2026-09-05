package kmlib.starsector.ui.layout;

import kmlib.math.geometry.BoxEdge;
import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.font.LineWidthMeasurer;
import kmlib.starsector.ui.widgets.BoxBorder;
import kmlib.starsector.ui.widgets.PanelChrome;
import kmlib.starsector.ui.widgets.PanelPlacement;

import java.util.List;

/**
 * Composes a headerless panel: it frames a bordered box around the shared body composition. The frame is
 * a {@link kmlib.starsector.ui.widgets.BorderedBox} (an outer border wrapping the inset body), and the body
 * is {@link CappedStripLayout#layoutBodyStrip} - the measured, capped, scrollable control strip; this class
 * owns only the framing - hang that body from the inset content top and wrap it with the border it was
 * handed, on whichever edges that border strokes - so a host's concrete content is just a column of
 * controls and the frame math stays here. Its framing primitives ({@link #computeContentOrigin},
 * {@link #framePlacement}) are shared: {@link TabPanelLayout} reuses them to frame the body it hangs
 * beneath its tab row, so a tab panel is this panel's framing with a row standing on top of it - one
 * border, around the body alone.
 *
 * <p>The one shared {@link PanelPlacement} a renderer draws and an input listener hit-tests is what keeps
 * the drawn box and the clickable box in step. UI coordinates throughout (origin bottom-left, y grows up);
 * text snapping runs through the injected {@link LineWidthMeasurer}, so the layout depends on a width
 * measurement rather than a concrete font and stays a pure computation. The panel hangs from the screen's
 * top-left by its paddings and caps its height to a bottom margin; that anchoring is the caller's to
 * supply through the paddings.
 */
public final class PanelLayout {
    private PanelLayout() {
    }

    /**
     * Lays the panel out for the given screen height, padding, chrome, and body controls. The box frames
     * a body sized to hold the controls (capped to the bottom margin), then places each control inside
     * that framed body. An empty {@code bodyControls} leaves a minimal bordered box with no body.
     *
     * @param screenHeight    the UI-coordinate screen height, giving the top edge to hang from
     * @param padding         the panel's edge margins: the top-left anchor and the bottom keep-clear
     *                        margin the body caps to (the right inset is unused - a panel grows rightward)
     * @param chrome          the room the panel spends on chrome rather than content: the border framing
     *                        the footprint, and the thickness of the bar its body reserves a gutter for
     * @param bodyControls    the body controls, top to bottom (empty for no body)
     * @param measurer        measures each label's rendered width for text snapping
     * @param rawScrollOffset the requested scroll offset for the body's scrolling control, in pixels;
     *                        clamped to its overflow by the capped layout
     * @return the box, body, laid-out body controls, and the scroll geometry, in UI coordinates
     */
    public static PanelPlacement computePlacement(
            float screenHeight,
            Padding padding,
            PanelChrome chrome,
            List<ControlSpec> bodyControls,
            LineWidthMeasurer measurer,
            float rawScrollOffset) {

        var border = chrome.border();
        var origin = computeContentOrigin(screenHeight, padding, border);

        // The shared body composition, capped so the box never runs past the bottom margin.
        var maxBodyHeight = screenHeight
            - padding.top()
            - border.computeEdgeInset(BoxEdge.TOP)
            - border.computeEdgeInset(BoxEdge.BOTTOM)
            - padding.bottom();

        var bodyStrip = CappedStripLayout.layoutBodyStrip(
            origin.limitBodyTo(maxBodyHeight),
            chrome.scrollbarThickness(),
            bodyControls,
            measurer,
            rawScrollOffset);

        return framePlacement(
            padding.left(),
            origin.boxTopY(),
            chrome,
            bodyStrip.bounds(),
            bodyStrip);
    }

    /**
     * The panel's framing origin: the box's top edge and the border-inset content top-left both a plain
     * panel and a tab panel hang their content from. Shared with {@link TabPanelLayout} so the two anchor
     * identically and only their content differs.
     *
     * @param screenHeight the UI-coordinate screen height, giving the top edge to hang from
     * @param padding      the panel's edge margins
     * @param border       the frame around the footprint; a stroked edge insets its content by the border,
     *                     while an open edge insets by nothing so the content sits flush against that side
     * @return the box top edge and the content top-left, inset only on the stroked edges, in UI coordinates
     */
    static ContentOrigin computeContentOrigin(
            float screenHeight,
            Padding padding,
            BoxBorder border) {

        var boxTopY = screenHeight - padding.top();

        return new ContentOrigin(
            boxTopY,
            padding.left() + border.computeEdgeInset(BoxEdge.LEFT),
            boxTopY - border.computeEdgeInset(BoxEdge.TOP));
    }

    /**
     * Frames a body rectangle in a bordered box and assembles the panel placement: the box wraps that body
     * with the border on every edge, hung from {@code boxTopY} at {@code leftX}, and the placement carries
     * the box with the same body rectangle and the strip's controls and scroll geometry. The shared framing
     * both a plain panel and a tab panel use - a tab panel hangs the same framed body under its tab row -
     * so {@link TabPanelLayout} reuses it rather than re-framing. It frames the passed body rectangle rather
     * than a bordered box, so a tab panel reusing it frames one border, not two.
     *
     * <p>The box reports the passed {@code framedBody} as its interior, so {@code box == body + border on
     * each edge} holds even when a collapsing tab panel hands in a body narrower than the strip it laid its
     * controls into: the box and interior collapse together while the controls keep their laid-out
     * positions for the renderer to clip.
     *
     * @param leftX      the box's left edge (the panel's left margin), in UI coordinates
     * @param boxTopY    the box's top edge, in UI coordinates
     * @param chrome     the panel's chrome: the border the box grows by on each stroked edge and by nothing
     *                   on an open one, so it shrinks to sit flush where it drops a border, and the bar
     *                   thickness the placement carries for the passes that draw and grab it
     * @param framedBody the body rectangle the box frames and reports as its interior
     * @param bodyStrip  the laid-out body strip whose controls and scroll geometry the placement carries
     * @return the panel placement: the bordered box plus the framed body, controls, and scroll geometry
     */
    static PanelPlacement framePlacement(
            int leftX,
            float boxTopY,
            PanelChrome chrome,
            Rectangle framedBody,
            CappedStripLayout.BodyStrip bodyStrip) {

        var border = chrome.border();

        // Each side contributes the border to the box only where it is stroked; an open edge reserves
        // nothing, so the box shrinks against the neighbour it sits flush with rather than leaving a bare
        // strip where the border would have been. The box stays anchored at leftX / boxTopY, so a dropped
        // left or top edge pulls the content out to the anchor instead of moving the box.
        var leftInset = border.computeEdgeInset(BoxEdge.LEFT);
        var rightInset = border.computeEdgeInset(BoxEdge.RIGHT);
        var topInset = border.computeEdgeInset(BoxEdge.TOP);
        var bottomInset = border.computeEdgeInset(BoxEdge.BOTTOM);
        var boxHeight = framedBody.height() + topInset + bottomInset;

        var box = new Rectangle(
            leftX,
            boxTopY - boxHeight,
            framedBody.width() + leftInset + rightInset,
            boxHeight);

        var capped = bodyStrip.placement();

        return new PanelPlacement(
            box,
            framedBody,
            capped.controls(),
            capped.flexViewport(),
            capped.scrollOffset(),
            capped.scrollOverflow(),
            chrome.scrollbarThickness());
    }

    /**
     * A panel's framing origin: the box's top edge and the content top-left, inset only where stroked.
     *
     * @param boxTopY     the box's top edge, in UI coordinates
     * @param contentX    the content's left edge, inset from the box left by the border when the left is
     *                    stroked and flush with it when the left border is open
     * @param contentTopY the content's top edge, inset from the box top by the border when the top is
     *                    stroked and flush with it when the top border is open
     */
    record ContentOrigin(
        float boxTopY,
        float contentX,
        float contentTopY) {

        /**
         * Pairs this origin with a height limit as the room a body is laid into. The only place a {@link
         * BodyRoom} is built in production: its two coordinates come straight off this value rather than
         * being taken apart and passed on, so no caller ever holds them loose beside a third float.
         *
         * @param maxHeight the most the body may stand before its scrolling control caps
         * @return the body's content anchor and its height limit as one value
         */
        BodyRoom limitBodyTo(float maxHeight) {
            return new BodyRoom(contentX, contentTopY, maxHeight);
        }
    }
}
