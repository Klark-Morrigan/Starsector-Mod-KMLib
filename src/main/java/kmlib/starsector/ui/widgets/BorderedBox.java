package kmlib.starsector.ui.widgets;

import com.fs.starfarer.api.util.Misc;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.render.UiBoxes;

import java.awt.Color;

/**
 * A filled rectangle framed by an outer border of a chosen width, everything faded by a single
 * opacity so the whole box lightens as one. Backs a panel's outermost frame: the fill is its
 * backdrop and the border its edge, with {@link #computeContentBounds} giving the inner area
 * the panel's contents inset into so they never overlap the stroke.
 *
 * <p>{@link #computeContentBounds} is pure geometry and unit-tested; {@link #render} is the raw
 * GL passthrough (over {@link Misc#renderQuadAlpha} and {@link UiBoxes}), exercised in-engine
 * like the other draw helpers.
 */
public final class BorderedBox {
    private BorderedBox() {
    }

    /**
     * The area inside {@code outer} once a border of {@code borderWidth} is inset on every edge,
     * so panel contents laid within it clear the stroke. A zero border returns the outer box
     * unchanged; a border wider than half the box collapses the content area to zero rather than
     * inverting it.
     *
     * @param outer       the box's full footprint
     * @param borderWidth the border thickness inset on each edge
     * @return the content rectangle inside the border
     */
    public static Rectangle computeContentBounds(Rectangle outer, float borderWidth) {
        var width = Math.max(0f, outer.width() - 2f * borderWidth);
        var height = Math.max(0f, outer.height() - 2f * borderWidth);
        return new Rectangle(outer.x() + borderWidth, outer.y() + borderWidth, width, height);
    }

    /**
     * Fills {@code outer} with {@code fill} and, when {@code borderWidth} is positive, strokes
     * its outer edge with {@code border}. Both draws scale their alpha by {@code opacity}, so
     * the box fades as one; a zero border draws only the fill.
     *
     * @param outer       the box's full footprint, in UI coordinates
     * @param borderWidth the border thickness; 0 draws no border
     * @param fill        the backdrop colour
     * @param border      the edge colour
     * @param opacity     overall alpha, 0..1, applied to fill and border alike
     */
    public static void render(Rectangle outer, float borderWidth, Color fill, Color border,
            float opacity) {
        Misc.renderQuadAlpha(outer.x(), outer.y(), outer.width(), outer.height(), fill, opacity);
        if (borderWidth > 0f) {
            UiBoxes.renderBorder(outer.x(), outer.y(), outer.width(), outer.height(), borderWidth,
                    border, opacity);
        }
    }
}
