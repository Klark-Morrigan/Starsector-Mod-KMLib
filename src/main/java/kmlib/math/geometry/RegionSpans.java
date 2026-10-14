package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Where a line, or a band swept along one, stays inside a region bounded by rings.
 *
 * <p>The answer is a list of {@code {tStart, tEnd}} parameter intervals along the line, the form
 * {@link Spans} works in. A label placed along a chord reads its room from here, so its text lands
 * inside the fill rather than straddling a border.
 *
 * <p>A region is read by the even-odd rule over all its rings together, so a hole needs no special
 * casing. {@link PolygonRegions} asks each ring on its own instead, which is a different question.
 */
public final class RegionSpans {

    // How many parallel rails sample a band's width in findBandInteriorSpans: the
    // centreline, both edges, and two between. Odd, so the centreline is always a
    // rail; enough to catch a border running alongside the band without the cost of
    // a fine sweep.
    private static final int BAND_RAIL_COUNT = 5;

    private RegionSpans() {
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
        var unitLine = line.toUnitLine();

        if (unitLine == null) {
            return spans;
        }

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

            if (!isPointInsideRegion(
                    rings,
                    unitLine.originX() + midT * unitLine.directionX(),
                    unitLine.originY() + midT * unitLine.directionY())) {

                continue;
            }

            // A vertex sitting exactly on the line can split one true span into two
            // abutting intervals; fuse them back so a span is reported whole.
            var last = spans.isEmpty()
                ? null
                : spans.get(spans.size() - 1);

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

        var unitLine = line.toUnitLine();
        if (unitLine == null) {
            return new ArrayList<>();
        }

        // The perpendicular the rails step along; the along-direction origin is
        // invariant under this shift, so every rail's parameters share one frame.
        var normalX = -unitLine.directionY();
        var normalY = unitLine.directionX();

        List<double[]> bandSpans = null;

        for (var rail = 0; rail < BAND_RAIL_COUNT; rail++) {

            // Rails evenly spaced across the full band width, both edges included.
            var offset = halfThickness * (2.0 * rail / (BAND_RAIL_COUNT - 1) - 1.0);
            var railSpans = findLineInteriorSpans(
                rings,
                new DirectedLine(
                    unitLine.originX() + offset * normalX,
                    unitLine.originY() + offset * normalY,
                    unitLine.directionX(),
                    unitLine.directionY()));

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
        var boundary = new HalfPlane(
            line.originX(),
            line.originY(),
            -line.directionY(),
            line.directionX());

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

                var crossing = Segments.computeCrossingPoint(
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
    // holes need no special casing. PolygonRegions.isPointInsideAnyRing answers the
    // other question, where a point in a hole is still held by the ring around it.
    private static boolean isPointInsideRegion(List<List<double[]>> rings, double x, double y) {

        var isInside = false;

        for (var ring : rings) {
            isInside ^= PolygonRegions.isPointInsideRing(ring, x, y);
        }

        return isInside;
    }
}
