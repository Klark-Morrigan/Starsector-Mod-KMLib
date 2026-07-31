package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.BoxEdge;
import kmlib.math.geometry.Rectangle;

/**
 * The geometry of a bordered box: the inner area its contents inset into behind a frame's stroked edges,
 * so they never overlap the border. Only a stroked edge insets - an edge left open frames nothing there,
 * so the content runs flush to that side. Substrate-independent - it computes rectangles and renders
 * nothing - so either a GL or a UI-API renderer can size a panel's contents against it. The raw-GL paint
 * lives in {@link kmlib.starsector.ui.render.gl.BorderedBoxRenderer}.
 */
public final class BorderedBox {
    private BorderedBox() {
    }

    /**
     * The area inside {@code outer} once {@code border} is inset on each stroked edge, so panel contents
     * laid within it clear the stroke. An open edge frames nothing, so the content runs flush to that side
     * rather than insetting from it - matching a box that drops the border it shares with a neighbour. A
     * zero-width border returns the outer box unchanged; a border wider than half the box collapses that
     * axis to zero rather than inverting it.
     *
     * @param outer  the box's full footprint
     * @param border the frame to clear: its width and which edges are stroked
     * @return the content rectangle inside the stroked edges
     */
    public static Rectangle computeContentBounds(Rectangle outer, BoxBorder border) {

        var leftInset = border.computeEdgeInset(BoxEdge.LEFT);
        var rightInset = border.computeEdgeInset(BoxEdge.RIGHT);
        var topInset = border.computeEdgeInset(BoxEdge.TOP);
        var bottomInset = border.computeEdgeInset(BoxEdge.BOTTOM);
        var width = Math.max(0f, outer.width() - leftInset - rightInset);
        var height = Math.max(0f, outer.height() - topInset - bottomInset);

        return new Rectangle(
            outer.x() + leftInset,
            outer.y() + bottomInset,
            width,
            height);
    }
}
