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
     * Fills {@code outer} with {@code fill} and, when {@code borderWidth} is positive, strokes its
     * outer edge with {@code border}. Both draws scale their alpha by {@code opacity}, so the box
     * fades as one; a zero border draws only the fill.
     *
     * @param outer       the box's full footprint, in UI coordinates
     * @param borderWidth the border thickness; 0 draws no border
     * @param fill        the backdrop colour
     * @param border      the edge colour
     * @param opacity     overall alpha, 0..1, applied to fill and border alike
     */
    public static void render(Rectangle outer, float borderWidth, Color fill, Color border,
            float opacity) {
        UiFill.renderQuad(outer.x(), outer.y(), outer.width(), outer.height(), fill, opacity);
        if (borderWidth > 0f) {
            UiBoxes.renderBorder(outer.x(), outer.y(), outer.width(), outer.height(), borderWidth,
                    border, opacity);
        }
    }
}
