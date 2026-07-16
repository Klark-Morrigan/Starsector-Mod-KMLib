package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Interrogates a closed ring, or a region bounded by rings, without reshaping it:
 * its enclosed area and winding, and where a line or a band stays inside it.
 *
 * <p>Read-only counterpart to the offset and smoothing passes. {@link
 * #computeSignedArea} reports a ring's area and winding sign, the fold-guard a
 * caller keys on. {@link #isPointInsideRing} answers whether a single point falls
 * within a ring. {@link #findLineInteriorSpans} and {@link #findBandInteriorSpans}
 * answer where a label's baseline - a zero-width line, or a strip with girth - fits
 * within a province, so text lands inside the fill rather than straddling a border.
 */
public final class PolygonRegions {

    // How many parallel rails sample a band's width in findBandInteriorSpans: the
    // centreline, both edges, and two between. Odd, so the centreline is always a
    // rail; enough to catch a border running alongside the band without the cost of
    // a fine sweep.
    private static final int BAND_RAIL_COUNT = 5;

    private PolygonRegions() {
    }

    /**
     * The signed area a closed ring encloses, by the shoelace sum: positive when the
     * ring winds counter-clockwise, negative when clockwise, and its magnitude the
     * enclosed area. So its sign reports winding and comparing two rings' signs
     * detects a fold.
     *
     * @param ring closed polygon vertices as {x, y} pairs
     * @return the signed area; zero for fewer than three vertices
     */
    public static double computeSignedArea(List<double[]> ring) {
        var twiceArea = 0.0;
        var count = ring.size();
        for (var i = 0; i < count; i++) {
            var current = ring.get(i);
            var next = ring.get((i + 1) % count);
            twiceArea += current[0] * next[1] - next[0] * current[1];
        }
        return twiceArea / 2.0;
    }

    /**
     * Whether a point falls inside a closed ring, by the even-odd rule: a horizontal
     * ray cast from the point toggles inside/outside at each edge it crosses, so an
     * odd crossing count puts the point within. Winding-blind and concave-safe, since
     * the rule counts crossings rather than assuming a convex hull.
     *
     * <p>A point exactly on the boundary is not a defined case: an edge endpoint at the
     * ray's height counts as below it, which makes the verdict consistent (a point on an
     * edge resolves the same way every call) but arbitrary in which side it lands on.
     * A caller that must classify boundary points needs a distance test, not this.
     *
     * @param ring closed polygon vertices as {x, y} pairs, in any winding
     * @param x    the point's x coordinate
     * @param y    the point's y coordinate
     * @return true when the point lies within the ring; false when outside, and always
     *         for a ring too short to enclose area
     */
    public static boolean isPointInsideRing(List<double[]> ring, double x, double y) {
        if (ring.size() < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            return false;
        }
        var isInside = false;
        var count = ring.size();
        for (var i = 0; i < count; i++) {
            var edgeStart = ring.get(i);
            var edgeEnd = ring.get((i + 1) % count);
            // The edge straddles the ray's height (an endpoint exactly at the height
            // counts below, so a shared corner toggles once rather than twice), and its
            // crossing with the ray's horizontal line lies to the point's right.
            if ((edgeStart[1] > y) != (edgeEnd[1] > y)
                    && x < edgeStart[0] + (y - edgeStart[1]) * (edgeEnd[0] - edgeStart[0])
                            / (edgeEnd[1] - edgeStart[1])) {
                isInside = !isInside;
            }
        }
        return isInside;
    }

    /**
     * The interior spans of an infinite line crossed through a region bounded by
     * closed rings - an outer ring plus any hole rings - as {@code {tStart, tEnd}}
     * parameter intervals along the line's direction.
     *
     * <p>The line runs through its origin along its direction; parameters are distances
     * from the origin (the direction is normalised internally, so {@code t} is in world
     * units either way). The line is crossed against every ring edge, the crossings sorted by
     * parameter, and an interval between two consecutive crossings kept only when
     * its midpoint lies inside the region - inside the outer ring and outside every
     * hole, by the even-odd rule over all rings together. That midpoint test is
     * what makes the result concave- and hole-safe: two points inside the region do
     * not make the chord between them interior, so a line that leaves and re-enters
     * a pinched region comes back as separate spans, never one chord bridging the
     * gap, and a span that would cross a hole splits around it.
     *
     * @param rings the region's boundary rings as {x, y} vertex lists: one outer ring
     *              and zero or more hole rings, in any winding (the even-odd rule is
     *              winding-blind)
     * @param line  the line to cross against the region - its origin is parameter zero,
     *              its direction the line's heading
     * @return the interior intervals as {@code {tStart, tEnd}} pairs, ascending and
     *         disjoint; empty when the line misses the region, only grazes it, or
     *         the direction is too short to define a line
     */
    public static List<double[]> findLineInteriorSpans(
            List<List<double[]>> rings,
            DirectedLine line) {
        var spans = new ArrayList<double[]>();
        var direction = Points.computeUnitVector(
                line.directionX(),
                line.directionY(),
                Limits.MIN_EDGE_LENGTH);
        if (direction == null) {
            return spans;
        }
        var unitLine = new DirectedLine(
                line.originX(),
                line.originY(),
                direction[0],
                direction[1]);
        var crossings = collectLineCrossingParameters(rings, unitLine);
        crossings.sort(null);
        // Between two consecutive crossings the line stays on one side of every
        // edge, so the whole interval shares its midpoint's inside/outside verdict.
        for (var i = 0; i + 1 < crossings.size(); i++) {
            var tStart = crossings.get(i);
            var tEnd = crossings.get(i + 1);
            // A grazing contact (a corner or tangent) reports two coincident
            // crossings; the zero-length interval between them is no span.
            if (tEnd - tStart < Limits.MIN_EDGE_LENGTH) {
                continue;
            }
            var midT = (tStart + tEnd) / 2.0;
            if (!isPointInsideRings(
                    rings,
                    unitLine.originX() + midT * unitLine.directionX(),
                    unitLine.originY() + midT * unitLine.directionY())) {
                continue;
            }
            // A vertex sitting exactly on the line can split one true span into two
            // abutting intervals; fuse them back so a span is reported whole.
            var last = spans.isEmpty() ? null : spans.get(spans.size() - 1);
            if (last != null && tStart - last[1] < Limits.MIN_EDGE_LENGTH) {
                last[1] = tEnd;
            } else {
                spans.add(new double[] {tStart, tEnd});
            }
        }
        return spans;
    }

    /**
     * The interior spans of a band of width {@code 2 * halfThickness} swept along a
     * line, as {@code {tStart, tEnd}} parameter intervals - the pieces of the line
     * where the whole band, not just its centreline, stays inside the region.
     *
     * <p>Extends {@link #findLineInteriorSpans} from a zero-width line to a strip: a
     * label has girth, so where a border runs alongside the centreline the band's
     * edge can cross it though the centreline clears. The band is sampled as a small
     * fan of parallel rails - the centreline, both edges at {@code +/-halfThickness}
     * along the perpendicular, and a couple between - and only the parameters
     * interior on every rail survive, via {@link Spans#intersectSpans}. Shifting a
     * rail perpendicular to the direction leaves the along-direction origin
     * unchanged, so all rails share one parameter frame and the result is in the
     * centreline's frame - a drop-in for the line test wherever band girth matters.
     *
     * <p>An approximation, not an exact Minkowski erosion: a concave corner poking
     * into the band strictly between two rails, without reaching either, is missed.
     * More rails narrow that gap; the sampling is chosen fine enough for label
     * placement, where the clearance is already a tuned world-space estimate. A
     * non-positive {@code halfThickness} collapses every rail onto the centreline,
     * so the band test reduces exactly to the line test.
     *
     * @param rings         the region's boundary rings as {x, y} vertex lists: one
     *                      outer ring and zero or more hole rings, any winding
     * @param line          the band's centreline - its origin is parameter zero and its
     *                      direction the band runs along
     * @param halfThickness half the band's width; the rails sit this far to each
     *                      side of the centreline
     * @return the intervals where the whole band is interior, as {@code {tStart,
     *         tEnd}} pairs, ascending and disjoint; empty when no band-wide piece is
     *         interior or the direction is too short to define a line
     */
    public static List<double[]> findBandInteriorSpans(
            List<List<double[]>> rings,
            DirectedLine line,
            double halfThickness) {
        var direction = Points.computeUnitVector(
                line.directionX(),
                line.directionY(),
                Limits.MIN_EDGE_LENGTH);
        if (direction == null) {
            return new ArrayList<>();
        }
        // The perpendicular the rails step along; the along-direction origin is
        // invariant under this shift, so every rail's parameters share one frame.
        var normalX = -direction[1];
        var normalY = direction[0];
        List<double[]> bandSpans = null;
        for (var rail = 0; rail < BAND_RAIL_COUNT; rail++) {
            // Rails evenly spaced across the full band width, both edges included.
            var offset = halfThickness * (2.0 * rail / (BAND_RAIL_COUNT - 1) - 1.0);
            var railSpans = findLineInteriorSpans(
                    rings,
                    new DirectedLine(
                            line.originX() + offset * normalX,
                            line.originY() + offset * normalY,
                            direction[0],
                            direction[1]));
            bandSpans = bandSpans == null
                    ? railSpans
                    : Spans.intersectSpans(bandSpans, railSpans);
            // A rail wholly outside leaves nothing for the rest to keep; stop early.
            if (bandSpans.isEmpty()) {
                return bandSpans;
            }
        }
        return bandSpans;
    }

    // The parameters (distances from the origin along the unit direction) at which the
    // infinite line crosses any ring edge. An edge is crossed when its two endpoints sit
    // on opposite sides of the line - the same signed-offset straddle test the half-plane
    // clip keys on, with an endpoint exactly on the line counted to the negative side so a
    // shared corner is not crossed twice.
    private static List<Double> collectLineCrossingParameters(
            List<List<double[]>> rings,
            DirectedLine line) {
        var crossings = new ArrayList<Double>();
        // The half-plane whose boundary is the line: offsets measured against its normal
        // tell an edge endpoint's side of the line.
        var boundary = new HalfPlane(line.originX(), line.originY(),
                -line.directionY(), line.directionX());
        for (var ring : rings) {
            var count = ring.size();
            for (var i = 0; i < count; i++) {
                var edgeStart = ring.get(i);
                var edgeEnd = ring.get((i + 1) % count);
                var offsetStart = Lines.computeSignedOffsetFromLine(edgeStart, boundary);
                var offsetEnd = Lines.computeSignedOffsetFromLine(edgeEnd, boundary);
                if ((offsetStart > 0) == (offsetEnd > 0)) {
                    continue;
                }
                var crossing = Segment.computeCrossingPoint(
                        edgeStart,
                        edgeEnd,
                        offsetStart,
                        offsetEnd);
                crossings.add((crossing[0] - line.originX()) * line.directionX()
                        + (crossing[1] - line.originY()) * line.directionY());
            }
        }
        return crossings;
    }

    // Whether the point lies inside the region the rings bound, by the even-odd rule
    // over every ring together: each ring's own parity is combined, so a point inside
    // the outer ring but also inside a hole ring toggles twice and lands outside -
    // holes need no special casing.
    private static boolean isPointInsideRings(List<List<double[]>> rings, double x, double y) {
        var isInside = false;
        for (var ring : rings) {
            isInside ^= isPointInsideRing(ring, x, y);
        }
        return isInside;
    }
}
