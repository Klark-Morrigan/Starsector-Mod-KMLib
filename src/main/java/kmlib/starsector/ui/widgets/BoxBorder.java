package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.BoxEdge;

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
}
