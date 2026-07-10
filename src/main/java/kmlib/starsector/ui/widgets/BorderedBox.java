package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;

/**
 * The geometry of a bordered box: the inner area its contents inset into once a border is stroked
 * on every edge, so they never overlap the frame. Substrate-independent - it computes rectangles
 * and renders nothing - so either a GL or a UI-API renderer can size a panel's contents against it.
 * The raw-GL paint lives in {@link kmlib.starsector.ui.render.gl.BorderedBoxRenderer}.
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
}
