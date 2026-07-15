package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Softens a closed ring's corners - rounding or chamfering them, and sanding off
 * the slivers a Voronoi inset throws up - so a province outline reads as a clean
 * shape rather than a faceted polygon.
 *
 * <p>Two passes, run in order by a caller: {@link #removeSpikes} first drops the
 * needle protrusions and cusps whose own edges are too short for rounding to step
 * back along, then {@link #roundCorners} arcs the corners that remain. Both keep
 * the ring's real shape - a genuine peninsula tip or a long straight edge survives
 * - and touch only the corners.
 */
public final class PolygonSmoothing {

    private PolygonSmoothing() {
    }

    /**
     * Rounds a closed polygon's corners with a fixed radius, leaving the
     * straight edges between corners intact, and chamfers corners sharper than
     * {@code bevelBelowAngleRadians} with a flat cut instead.
     *
     * <p>At each vertex it steps back {@code radius} along both adjacent edges
     * and replaces the sharp corner with a true circular arc tangent to both
     * edges at those step-back points, sampled into {@code segmentsPerCorner}
     * segments. Because the cut is a fixed distance, not a fraction of the edge,
     * long edges stay long and only the corners soften - so a big cell does not
     * round off into a blob. The radius is clamped to half of each adjacent edge
     * so neighbouring corners never overlap, which also keeps the result convex
     * for a convex input. A circular arc (rather than a bezier with the vertex as
     * control point) rounds sharp corners uniformly instead of pinching back to a
     * near-point, so a spur is genuinely sanded off rather than left a spike.
     *
     * <p>A corner still sharper than {@code bevelBelowAngleRadians} is cut straight
     * across (a chamfer between the two step-back points) rather than arced, since
     * an arc that tight reads as a nick; a non-positive threshold disables the
     * chamfer and arcs every corner. Vertex cost is at most {@code corners *
     * (segmentsPerCorner + 1)}.
     *
     * @param polygon                closed polygon vertices as {x, y} pairs
     * @param radius                 corner radius in the polygon's units
     * @param segmentsPerCorner      arc segments per rounded corner; higher is
     *                               smoother
     * @param bevelBelowAngleRadians corners with an interior angle below this
     *                               are chamfered flat rather than rounded;
     *                               non-positive rounds every corner
     * @return the rounded polygon; a copy of the input (deduplicated) when it
     *         has fewer than three vertices, or when radius/segments are
     *         non-positive (nothing to round)
     */
    public static List<double[]> roundCorners(
            List<double[]> polygon,
            double radius,
            int segmentsPerCorner,
            double bevelBelowAngleRadians) {
        var vertices = Rings.removeConsecutiveDuplicates(polygon);
        var count = vertices.size();
        if (count < Limits.MIN_VERTICES_TO_ENCLOSE_AREA || radius <= 0 || segmentsPerCorner < 1) {
            return vertices;
        }

        var rounded = new ArrayList<double[]>(count * (segmentsPerCorner + 1));
        for (var i = 0; i < count; i++) {
            var previous = vertices.get((i - 1 + count) % count);
            var corner = vertices.get(i);
            var next = vertices.get((i + 1) % count);
            // Clamp the cut to half of the shorter adjacent edge so two corners
            // sharing an edge cannot eat into each other.
            var cut = Math.min(radius,
                    0.5 * Math.min(Points.computeDistance(corner, previous),
                            Points.computeDistance(corner, next)));
            var arcStart = computePointToward(corner, previous, cut);
            var arcEnd = computePointToward(corner, next, cut);
            // Below the threshold a rounded arc would still read as a spike, so
            // cut straight across the corner: the two step-back points alone.
            if (bevelBelowAngleRadians > 0
                    && computeInteriorAngle(previous, corner, next) < bevelBelowAngleRadians) {
                rounded.add(arcStart);
                rounded.add(arcEnd);
                continue;
            }
            appendCircularArc(
                    rounded,
                    previous,
                    corner,
                    next,
                    arcStart,
                    arcEnd,
                    segmentsPerCorner);
        }
        return rounded;
    }

    /**
     * Removes needle spikes and cusps from a closed ring: a vertex whose interior
     * angle is sharper than {@code maxCornerAngleRadians} and whose perpendicular
     * offset from the straight chord between its two neighbours is under
     * {@code maxSpikeHeight} is dropped, and its neighbours are spliced together.
     * Such a vertex is a thin protrusion (an outward needle) or nick (an inward
     * cusp), not real shape - the sliver a Voronoi bloc's inset throws up at a
     * pinched neck or a same-owner disc waist, which downstream rounding cannot sand
     * off because the spike's own edges are too short to step back along.
     *
     * <p>Both conditions must hold, which is what protects real geometry. A sharp
     * corner that juts far - a genuine peninsula tip - clears the height bar and
     * stays (rounding chamfers it instead); a gently curved run - a smooth arc
     * facet, whose corners are nearly straight - clears the angle bar and stays. Only
     * a corner that is both sharp and shallow, a sliver, is removed. Because the
     * interior angle is unsigned, an outward spike and an inward notch of equal
     * sharpness are treated alike, so the pass sands off both. Removal iterates to a
     * fixed point: splicing one vertex reshapes its neighbours' corners and can
     * expose a spike that hid behind it. It never drops below three vertices, so a
     * ring that is all sliver survives as a triangle for the caller's area check to
     * discard.
     *
     * @param polygon               closed polygon vertices as {x, y} pairs
     * @param maxSpikeHeight        a candidate corner is removed only if it sits
     *                              nearer than this to its neighbour chord; larger
     *                              sands off deeper protrusions. Non-positive
     *                              disables the pass
     * @param maxCornerAngleRadians only corners with an interior angle below this are
     *                              candidates; larger treats gentler corners as
     *                              spikes. Non-positive disables the pass
     * @return the cleaned ring; a copy of the input (deduplicated) when it has fewer
     *         than three vertices or either threshold is non-positive
     */
    public static List<double[]> removeSpikes(
            List<double[]> polygon,
            double maxSpikeHeight,
            double maxCornerAngleRadians) {
        var vertices = Rings.removeConsecutiveDuplicates(polygon);
        if (vertices.size() < Limits.MIN_VERTICES_TO_ENCLOSE_AREA
                || maxSpikeHeight <= 0 || maxCornerAngleRadians <= 0) {
            return vertices;
        }

        // Splicing out one spike reshapes the corners on either side, which can turn
        // a neighbour into a new spike, so re-scan until a full pass removes none.
        // Each removal drops a vertex and the loop stops at three, so it terminates.
        var removedAny = true;
        while (removedAny && vertices.size() > Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            removedAny = false;
            var count = vertices.size();
            for (var i = 0; i < count; i++) {
                var previous = vertices.get((i - 1 + count) % count);
                var corner = vertices.get(i);
                var next = vertices.get((i + 1) % count);
                if (computeInteriorAngle(previous, corner, next) < maxCornerAngleRadians
                        && Lines.computePerpendicularDistance(corner, previous, next)
                                < maxSpikeHeight) {
                    vertices.remove(i);
                    removedAny = true;
                    break;
                }
            }
        }
        return vertices;
    }

    // Appends the circular arc that rounds one corner: the arc tangent to both edges
    // at {@code arcStart} and {@code arcEnd}, sampled into {@code segments} steps.
    // Its centre is where the two edge-perpendiculars through those points meet; the
    // arc sweeps the short way between them, so it bulges toward the corner. Falls
    // back to the two step-back points alone when the edges are collinear (no corner
    // to round, so no centre).
    private static void appendCircularArc(
            List<double[]> out,
            double[] previous,
            double[] corner,
            double[] next,
            double[] arcStart,
            double[] arcEnd,
            int segments) {
        var center = computeArcCenter(previous, corner, next, arcStart, arcEnd);
        if (center == null) {
            out.add(arcStart);
            out.add(arcEnd);
            return;
        }
        var radius = Points.computeDistance(center, arcStart);
        var startAngle = Math.atan2(arcStart[1] - center[1], arcStart[0] - center[0]);
        var endAngle = Math.atan2(arcEnd[1] - center[1], arcEnd[0] - center[0]);
        // Sweep the short way (normalise to (-PI, PI]); that arc is the one on the
        // corner's side, so the rounded corner bulges toward the original vertex.
        var sweep = endAngle - startAngle;
        while (sweep <= -Math.PI) {
            sweep += 2.0 * Math.PI;
        }
        while (sweep > Math.PI) {
            sweep -= 2.0 * Math.PI;
        }
        for (var step = 0; step <= segments; step++) {
            var angle = startAngle + sweep * step / segments;
            out.add(new double[] {
                    center[0] + radius * Math.cos(angle),
                    center[1] + radius * Math.sin(angle),
            });
        }
    }

    // The centre of the arc rounding {@code corner}: the point equidistant from both
    // edges at the step-back points, found as the intersection of the perpendicular
    // to the inbound edge through {@code arcStart} and that to the outbound edge
    // through {@code arcEnd}. Null when those perpendiculars are parallel (the edges
    // are collinear, so there is no corner to round).
    private static double[] computeArcCenter(
            double[] previous,
            double[] corner,
            double[] next,
            double[] arcStart,
            double[] arcEnd) {
        // The centre lies on the perpendicular to each edge (the radius direction,
        // (-dy, dx)) through that edge's step-back point; where those two
        // perpendiculars cross is the centre. Parallel means collinear edges - no
        // corner - so there is no centre.
        return Lines.intersectLines(
                arcStart,
                -(corner[1] - previous[1]),
                corner[0] - previous[0],
                arcEnd,
                -(next[1] - corner[1]),
                next[0] - corner[0]);
    }

    // A point {@code distance} from {@code from} toward {@code to}; returns from
    // itself when the two coincide (no direction).
    private static double[] computePointToward(double[] from, double[] to, double distance) {
        var deltaX = to[0] - from[0];
        var deltaY = to[1] - from[1];
        var length = Points.computeVectorLength(deltaX, deltaY);
        if (length < Limits.MIN_EDGE_LENGTH) {
            return new double[] {from[0], from[1]};
        }
        return new double[] {
                from[0] + deltaX / length * distance,
                from[1] + deltaY / length * distance,
        };
    }

    // Interior angle at {@code corner} in radians [0, PI], measured between the
    // edges to its two neighbours: PI is a straight pass-through and small values
    // are sharp spikes. Builds the two edge vectors and defers the angle to
    // Points; a degenerate (zero-length) edge has no direction, so Points returns
    // NaN and this treats such a corner as straight (PI) - never chamfered or
    // sanded - which is the convention the two smoothing passes want.
    private static double computeInteriorAngle(
            double[] previous,
            double[] corner,
            double[] next) {
        var angle = Points.computeAngleBetween(
                previous[0] - corner[0],
                previous[1] - corner[1],
                next[0] - corner[0],
                next[1] - corner[1],
                Limits.MIN_EDGE_LENGTH);
        return Double.isNaN(angle) ? Math.PI : angle;
    }
}
