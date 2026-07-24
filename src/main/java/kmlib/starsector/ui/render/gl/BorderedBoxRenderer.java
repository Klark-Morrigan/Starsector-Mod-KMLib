package kmlib.starsector.ui.render.gl;

import kmlib.math.geometry.Rectangle;

import java.awt.Color;

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
     * Fills {@code outer} with {@code fill} and, when the border width is positive, strokes the {@code
     * border}'s edges around {@code outer} in {@code borderColor}. Both draws scale their alpha by {@code
     * opacity}, so the box fades as one; a zero-width border draws only the fill, and an omitted edge
     * leaves that side of the frame open so a box flush against another's edge can drop the border there.
     *
     * @param outer       the box's full footprint, in UI coordinates
     * @param border      the border width and which edges to stroke; a zero width draws no border
     * @param fill        the backdrop colour
     * @param borderColor the edge colour
     * @param opacity     overall alpha, 0..1, applied to fill and border alike
     */
    public static void render(
            Rectangle outer,
            BoxBorder border,
            Color fill,
            Color borderColor,
            float opacity) {
        UiFill.renderQuad(
                outer.x(),
                outer.y(),
                outer.width(),
                outer.height(),
                fill,
                opacity);
        if (border.width() > 0f) {
            UiBoxes.renderBorder(
                    outer.x(),
                    outer.y(),
                    outer.width(),
                    outer.height(),
                    border,
                    borderColor,
                    opacity);
        }
    }
}
