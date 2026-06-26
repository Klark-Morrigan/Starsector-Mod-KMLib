package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Polygon operations for 2D geometry.
 *
 * <p>Pure 2D math: no rendering and no Starsector types, so it can be
 * reasoned about and verified on its own.
 */
public final class Polygons {
    // Edges shorter than this have no well-defined direction (and so no
    // normal); they are skipped rather than dividing by ~zero length.
    private static final double MIN_EDGE_LENGTH = 1e-6;

    private Polygons() {
    }

    /**
     * Offsets every edge of a counter-clockwise convex polygon inward by
     * {@code distance} and returns the offset edges as independent segments.
     *
     * <p>Unlike shrinking the whole polygon, this never collapses: each edge
     * is shifted along its own inward normal and emitted as its own segment,
     * so a thin polygon yields near-overlapping segments rather than
     * vanishing. Drawing a cell's edges this way sits its border a uniform
     * {@code distance} inside its true outline - so two neighbours leave a
     * {@code 2 * distance} channel between them - at the cost of a small open
     * notch at each corner, where adjacent offset edges no longer meet.
     *
     * @param polygon  CCW convex polygon vertices as {x, y} pairs
     * @param distance inward offset applied to each edge
     * @return one segment per edge as {x1, y1, x2, y2}; empty for fewer than
     *         two vertices
     */
    public static List<double[]> offsetEdgesInward(List<double[]> polygon, double distance) {
        var segments = new ArrayList<double[]>();
        var count = polygon.size();
        if (count < 2) {
            return segments;
        }

        for (var i = 0; i < count; i++) {
            var a = polygon.get(i);
            var b = polygon.get((i + 1) % count);
            var edgeX = b[0] - a[0];
            var edgeY = b[1] - a[1];
            var length = Math.sqrt(edgeX * edgeX + edgeY * edgeY);
            if (length < MIN_EDGE_LENGTH) {
                continue;
            }
            // CCW interior is left of the directed edge, so the inward normal
            // of edge (a -> b) is (-edgeY, edgeX) normalized.
            var normalX = -edgeY / length;
            var normalY = edgeX / length;
            segments.add(new double[] {
                    a[0] + normalX * distance, a[1] + normalY * distance,
                    b[0] + normalX * distance, b[1] + normalY * distance,
            });
        }
        return segments;
    }
}
