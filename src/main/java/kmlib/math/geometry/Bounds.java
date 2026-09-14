package kmlib.math.geometry;

import java.util.List;

/**
 * The axis-aligned box a set of points fits inside, stated as its two extreme corners.
 *
 * <p>What it is for is the question that comes before a real measurement: two shapes stated as
 * rings cost an edge-against-edge crossing to compare properly, and most pairs on a map are
 * nowhere near each other. A box apiece answers the cheap half - shapes whose boxes miss cannot
 * meet - so the expensive half is only paid where it can change an answer.
 *
 * <p>Kept apart from {@link Rectangle}, which is the same shape stated differently and for a
 * different substrate: a corner plus a size, in float, laid out for drawing and hit-testing. This
 * is the world-geometry reading - double precision, matching the points it is measured from, and
 * two corners because that is what a min/max walk produces and what an overlap test reads.
 *
 * @param minX the leftmost point's x
 * @param minY the lowest point's y
 * @param maxX the rightmost point's x
 * @param maxY the highest point's y
 */
public record Bounds(
    double minX,
    double minY,
    double maxX,
    double maxY) {

    /**
     * Measures the box a point cloud fits inside.
     *
     * @param points the {@code {x, y}} points to enclose; must be non-empty
     * @return the box they fall inside
     * @throws IllegalArgumentException if {@code points} is empty (a box is undefined with
     *         nothing to enclose)
     */
    public static Bounds computeEnclosingBounds(List<double[]> points) {

        if (points.isEmpty()) {
            throw new IllegalArgumentException("Cannot enclose no points");
        }

        var minX = Double.POSITIVE_INFINITY;
        var minY = Double.POSITIVE_INFINITY;
        var maxX = Double.NEGATIVE_INFINITY;
        var maxY = Double.NEGATIVE_INFINITY;

        for (var point : points) {

            minX = Math.min(minX, point[0]);
            minY = Math.min(minY, point[1]);
            maxX = Math.max(maxX, point[0]);
            maxY = Math.max(maxY, point[1]);
        }
        return new Bounds(minX, minY, maxX, maxY);
    }

    /**
     * Whether two boxes share any area at all.
     *
     * <p>Touching counts as overlapping. The shapes inside two boxes that meet along an edge may
     * still meet, and a box is only ever a stand-in for the shape it encloses - so the test errs
     * toward measuring properly rather than toward discarding a pair it cannot rule out.
     *
     * @param other the box to test against
     * @return whether the two boxes meet or overlap
     */
    public boolean overlaps(Bounds other) {

        return minX <= other.maxX()
            && other.minX() <= maxX
            && minY <= other.maxY()
            && other.minY() <= maxY;
    }
}
