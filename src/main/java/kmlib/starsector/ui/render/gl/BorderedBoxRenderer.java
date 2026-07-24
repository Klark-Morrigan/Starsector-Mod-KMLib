package kmlib.starsector.ui.render.gl;

import kmlib.math.geometry.Rectangle;

/**
 * Raw-GL paint for a {@link kmlib.starsector.ui.widgets.BorderedBox}: fills the footprint and, when
 * a border width is given, strokes its outer edge, both faded by one opacity so the box lightens as
 * a unit. The inset geometry lives on the substrate-independent widget; this is the GL passthrough
 * (over {@link UiFill#renderQuad} and {@link UiBoxes}), exercised in-engine. The fill composites over
 * whatever is behind it by its opacity, so a low-opacity backdrop reveals rather than blocking it.
 */
public final class BorderedBoxRenderer {
    private BorderedBoxRenderer() {
    }

    /**
     * Fills {@code boxFootprint} with {@code fillPaint} and, when the border width is positive, strokes
     * the {@code border}'s edges around {@code boxFootprint} with {@code borderPaint}. A zero-width
     * border draws only the fill, and an omitted edge leaves that side of the frame open so a box flush
     * against another's edge can drop the border there. Sharing one opacity across both paints is the
     * caller's to arrange, so the box can fade as one.
     *
     * @param boxFootprint the box's full footprint, in UI coordinates
     * @param border       the border width and which edges to stroke; a zero width draws no border
     * @param fillPaint    the backdrop colour and alpha
     * @param borderPaint  the edge colour and alpha
     */
    public static void render(
            Rectangle boxFootprint,
            BoxBorder border,
            UiElementPaint fillPaint,
            UiElementPaint borderPaint) {

        UiFill.renderQuad(boxFootprint, fillPaint);

        if (border.width() > 0f) {
            UiBoxes.renderBorder(boxFootprint, border, borderPaint);
        }
    }
}
