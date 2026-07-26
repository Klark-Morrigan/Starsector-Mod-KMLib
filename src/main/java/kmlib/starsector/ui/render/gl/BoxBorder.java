package kmlib.starsector.ui.render.gl;

import kmlib.math.geometry.BoxEdge;

import java.util.Set;

/**
 * How a box's border is stroked: its width and which of the four edges to draw. Bundles the two so a frame
 * travels as one value rather than a width and an edge set threaded side by side, and so a box drawn flush
 * against another's edge names the sides it drops in one place.
 *
 * @param width the border thickness in pixels; 0 draws no border
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
}
