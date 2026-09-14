package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.BoxEdge;
import kmlib.math.geometry.Rectangle;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * How a box's border is stroked: its width and which of the four edges to draw. Bundles the two so a frame
 * travels as one value rather than a width and an edge set threaded side by side, and so a box drawn flush
 * against another's edge names the sides it drops in one place.
 *
 * <p>It also answers what one edge contributes to the geometry around it, since which edges are stroked and
 * how much room each takes are the same question: a stroked edge reserves the border's width, an open edge
 * reserves nothing. Keeping that answer on the border itself means a layout growing a box outward and a
 * renderer insetting content inward read one rule rather than each deriving its own. Substrate-independent,
 * so a layout pass and a GL pass can both frame against it.
 *
 * @param width the border thickness in pixels; 0 draws no border and reserves no room
 * @param edges which edges to stroke; the rest are left open
 */
public record BoxBorder(float width, Set<BoxEdge> edges) {

    /**
     * A full border of the given width, stroking every edge.
     *
     * @param width the border thickness in pixels; 0 draws no border
     */
    public BoxBorder(float width) {
        this(width, BoxEdge.ALL);
    }

    /**
     * The room one edge reserves: the border's width where that edge is stroked, nothing where it is open.
     * An open edge lets content sit flush against whatever the box abuts rather than leaving a bare strip
     * where a border would have been.
     *
     * @param edge the edge to measure
     * @return the inset that edge contributes, in pixels
     */
    public float computeEdgeInset(BoxEdge edge) {
        return edges.contains(edge) ? width : 0f;
    }

    /**
     * The quads that stroke this border inside {@code bounds}, one per drawn edge and no two of them
     * overlapping: the horizontal edges run the box's full width, and the vertical ones run only between
     * them, giving up the width of whichever horizontal edge is actually drawn at each end. Empty for a
     * zero width, which is a border that strokes nothing.
     *
     * <p>The four are cut this way because a corner covered twice is a corner drawn twice: every shade
     * here composites, so a translucent frame - which is what the engine's own controls are drawn in -
     * comes out with four bright corner pixels and reads as a box with tacks in it. Solved by placing the
     * quads rather than by the pass that fills them, so every surface that strokes a border inherits the
     * cut, and so the arithmetic is checkable without a drawing surface.
     *
     * <p>An open edge gives up nothing: a vertical edge meeting a side that is not stroked runs to the
     * box's own edge, since there is no quad there to leave room for.
     *
     * @param bounds the footprint to frame, in UI coordinates (origin bottom-left)
     * @return the edge quads to fill, in no particular order; empty when nothing is stroked
     */
    public List<Rectangle> computeStrokeBoxes(Rectangle bounds) {

        var strokes = new ArrayList<Rectangle>(edges.size());
        if (width <= 0f) {
            return List.of();
        }
        var bottomInset = computeEdgeInset(BoxEdge.BOTTOM);
        var topInset = computeEdgeInset(BoxEdge.TOP);

        // The verticals run between whatever horizontals were drawn. Floored at zero so a box shorter than
        // the two edges it is asked for collapses its sides rather than inverting them through its corners.
        var sideHeight = Math.max(0f, bounds.height() - bottomInset - topInset);

        if (edges.contains(BoxEdge.BOTTOM)) {
            strokes.add(new Rectangle(bounds.x(), bounds.y(), bounds.width(), width));
        }
        if (edges.contains(BoxEdge.TOP)) {
            strokes.add(new Rectangle(
                bounds.x(),
                bounds.y() + bounds.height() - width,
                bounds.width(),
                width));
        }
        if (edges.contains(BoxEdge.LEFT)) {
            strokes.add(new Rectangle(bounds.x(), bounds.y() + bottomInset, width, sideHeight));
        }
        if (edges.contains(BoxEdge.RIGHT)) {
            strokes.add(new Rectangle(
                bounds.x() + bounds.width() - width,
                bounds.y() + bottomInset,
                width,
                sideHeight));
        }
        return List.copyOf(strokes);
    }
}
