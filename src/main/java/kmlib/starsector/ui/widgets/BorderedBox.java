package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.BoxEdge;
import kmlib.math.geometry.Rectangle;

import java.util.Set;

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
     * The area inside {@code outer} once a border of {@code borderWidth} is inset on each stroked edge,
     * so panel contents laid within it clear the stroke. An edge absent from {@code borderedEdges} frames
     * nothing, so the content runs flush to that side rather than insetting from it - matching a box that
     * drops the border it shares with a neighbour. A zero border returns the outer box unchanged; a border
     * wider than half the box collapses that axis to zero rather than inverting it.
     *
     * @param outer         the box's full footprint
     * @param borderWidth   the border thickness inset on each stroked edge
     * @param borderedEdges which edges are stroked; only these inset the content, an open edge stays flush
     * @return the content rectangle inside the stroked edges
     */
    public static Rectangle computeContentBounds(
            Rectangle outer, float borderWidth, Set<BoxEdge> borderedEdges) {
        var leftInset = edgeInset(borderedEdges, BoxEdge.LEFT, borderWidth);
        var rightInset = edgeInset(borderedEdges, BoxEdge.RIGHT, borderWidth);
        var topInset = edgeInset(borderedEdges, BoxEdge.TOP, borderWidth);
        var bottomInset = edgeInset(borderedEdges, BoxEdge.BOTTOM, borderWidth);
        var width = Math.max(0f, outer.width() - leftInset - rightInset);
        var height = Math.max(0f, outer.height() - topInset - bottomInset);
        return new Rectangle(outer.x() + leftInset, outer.y() + bottomInset, width, height);
    }

    // The inset one edge contributes: the border width where that edge is stroked, nothing where it is
    // open, so the content stays flush to an open side rather than pulling in from a border that is not
    // drawn there.
    private static float edgeInset(Set<BoxEdge> borderedEdges, BoxEdge edge, float borderWidth) {
        return borderedEdges.contains(edge) ? borderWidth : 0f;
    }
}
