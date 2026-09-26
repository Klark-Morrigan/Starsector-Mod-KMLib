package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Interrogates a closed ring, or a region bounded by rings, without reshaping it:
 * its enclosed area and winding, and where a line or a band stays inside it.
 *
 * <p>Read-only counterpart to the offset and smoothing passes. {@link
 * #computeSignedArea} reports a ring's area and winding sign, the fold-guard a
 * caller keys on, and {@link #countSelfCrossings} says whether what came back is
 * drawable at all. {@link #isPointInsideRing} answers whether a single point falls
 * within a ring. {@link #groupRingsIntoRegions} sorts a flat ring soup into the
 * {@link RingRegion}s it bounds, which is the form the two span passes below already
 * take their rings in. {@link #findLineInteriorSpans} and {@link
 * #findBandInteriorSpans} answer where a label's baseline - a zero-width line, or a
 * strip with girth - fits within a province, so text lands inside the fill rather
 * than straddling a border.
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
     * How many times a closed ring crosses itself.
     *
     * <p>Whether a ring is drawable, as a number rather than a verdict. A ring that crosses
     * itself fills to something other than its outline and strokes a line through its own
     * interior, so this is what a caller checks after any pass that can fold a ring - chiefly
     * the miter inset, which crosses wherever a shape pinches narrower than twice its offset.
     * A count rather than a flag because it says how bad, which is what tells a fixture that
     * got worse from one that was never clean.
     *
     * <p>Consecutive edges are not asked about: they share an endpoint by construction, and a
     * ring's neighbours touching is not a fold. Two edges further apart that meet at a point
     * do count - a ring pinched to touch itself is not simple, whether or not it passes
     * through.
     *
     * <p>Every pair is compared, so this costs the square of the ring's length. It is a check
     * on geometry rather than a step in producing it, which is where that is affordable: a
     * test, a probe, or a diagnostic. Nothing drawing per frame should ask.
     *
     * @param ring closed polygon vertices as {x, y} pairs, without a repeated closing point
     * @return how many pairs of non-adjacent edges cross; zero for a ring too short to
     *         enclose area, and zero for a simple ring
     */
    public static int countSelfCrossings(List<double[]> ring) {

        if (ring.size() < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            return 0;
        }
        var count = ring.size();
        var crossings = 0;

        for (var first = 0; first < count; first++) {
            // From the edge after next: the next one shares this edge's far end.
            for (var second = first + 2; second < count; second++) {

                // The last edge shares the first edge's start, so that one pair is adjacent
                // too - and it is the only pair the index walk cannot rule out by distance.
                if (first == 0 && second == count - 1) {
                    continue;
                }
                if (Segments.intersectSegments(
                        ring.get(first),
                        ring.get((first + 1) % count),
                        ring.get(second),
                        ring.get((second + 1) % count)) != null) {

                    crossings++;
                }
            }
        }
        return crossings;
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
        return isPointInsideRing(ring, vertex -> vertex[0], vertex -> vertex[1], x, y);
    }

    /**
     * How far a point sits from a ring's boundary - the shortest distance to any of its
     * edges, whether the point is inside the ring or outside it.
     *
     * <p>The measure {@link #isPointInsideRing} deliberately does not give. That one
     * answers which side of the boundary a point is on, and is arbitrary for a point on
     * it; this one answers how far from the boundary it is, and is exact there (zero).
     * Together they say where a point sits; alone, this one settles the questions phrased
     * as a clearance - whether a point keeps some distance off the boundary, and so
     * whether a shape offset inward really moved that far in.
     *
     * <p>Measured to each edge as a bounded segment, not to the infinite line through it,
     * so the answer is the distance to the boundary as drawn rather than to a line it
     * extends along.
     *
     * @param ring  closed polygon vertices as {x, y} pairs, in any winding
     * @param point the {x, y} point to measure from
     * @return the distance to the nearest point of the ring's boundary
     * @throws IllegalArgumentException if {@code ring} is empty (there is no boundary to
     *         measure to)
     */
    public static double computeDistanceToBoundary(List<double[]> ring, double[] point) {

        if (ring.isEmpty()) {
            throw new IllegalArgumentException("Cannot measure a distance to an empty ring");
        }

        var nearest = Double.POSITIVE_INFINITY;
        var count = ring.size();

        for (var i = 0; i < count; i++) {
            nearest = Math.min(
                nearest,
                Segments.computeDistanceToPoint(ring.get(i), ring.get((i + 1) % count), point));
        }
        return nearest;
    }

    /**
     * Groups a flat ring soup into the regions it bounds - each outer ring paired with the
     * holes cut out of it.
     *
     * <p>Which ring is which is read from its winding, the convention a boundary-only
     * tessellation under the positive rule already emits: a counter-clockwise ring bounds
     * a filled area, a clockwise one cuts a hole in it. So a soup that came back from such a
     * tessellation needs nothing recorded alongside it to be sorted out again.
     *
     * <p>A hole is attributed to the <em>smallest</em> outer ring containing it, not the first
     * found. An outer ring lying inside another ring's hole - an island in a lake - is
     * contained by both, and only the tighter of the two is the body the hole is actually cut
     * from; taking the first would hand an island's hole to the landmass around the lake.
     *
     * <p>A ring too short to enclose area is dropped, and so is a hole no outer ring contains:
     * it cuts nothing out of anything, and carrying it would put a loop in a region whose
     * interior it does not touch.
     *
     * @param rings the soup as {x, y} vertex lists, wound by the convention above
     * @return one region per outer ring, in the order those rings arrived; empty when no ring
     *         encloses area
     */
    public static List<RingRegion> groupRingsIntoRegions(List<List<double[]>> rings) {

        var outerRings = new ArrayList<List<double[]>>();
        var holeRings = new ArrayList<List<double[]>>();

        for (var ring : rings) {
            if (ring.size() < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
                continue;
            }
            // A zero-area ring falls through both arms: it winds neither way and bounds
            // nothing, so it is no more a region than a two-vertex ring is.
            var signedArea = computeSignedArea(ring);

            if (signedArea > 0) {
                outerRings.add(ring);
            } else if (signedArea < 0) {
                holeRings.add(ring);
            }
        }

        // Accumulated per outer ring by index rather than built into the records directly,
        // since a region's holes are only known once every hole has been attributed.
        var holeRingsByOuterIndex = new ArrayList<List<List<double[]>>>(outerRings.size());

        for (var i = 0; i < outerRings.size(); i++) {
            holeRingsByOuterIndex.add(new ArrayList<>());
        }
        for (var holeRing : holeRings) {
            var outerIndex = findSmallestContainingOuterRingIndex(outerRings, holeRing);
            if (outerIndex >= 0) {
                holeRingsByOuterIndex.get(outerIndex).add(holeRing);
            }
        }

        var regions = new ArrayList<RingRegion>(outerRings.size());
        for (var i = 0; i < outerRings.size(); i++) {
            regions.add(new RingRegion(outerRings.get(i), holeRingsByOuterIndex.get(i)));
        }
        return regions;
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

    // The index of the smallest outer ring enclosing the hole, or -1 when none does. A hole
    // is tested by its first vertex: a hole lies strictly within the body it is cut from, so
    // any one of its vertices decides containment, and boundary points - the case the
    // point-in-ring test leaves undefined - do not arise between a hole and its own outer.
    private static int findSmallestContainingOuterRingIndex(
            List<List<double[]>> outerRings,
            List<double[]> holeRing) {

        var vertex = holeRing.get(0);
        var smallestIndex = -1;
        var smallestArea = Double.MAX_VALUE;

        for (var i = 0; i < outerRings.size(); i++) {
            var outerRing = outerRings.get(i);
            if (!isPointInsideRing(outerRing, vertex[0], vertex[1])) {
                continue;
            }
            // Outer rings are positive by construction here, so the raw signed area orders
            // them by size without an absolute value.
            var area = computeSignedArea(outerRing);

            if (area < smallestArea) {
                smallestArea = area;
                smallestIndex = i;
            }
        }
        return smallestIndex;
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

    // The ray-cast walk both public forms of isPointInsideRing share. Vertex coordinates are read
    // through the two accessors so neither form copies its ring into the other's type first.
    private static <P> boolean isPointInsideRing(
            List<P> ring,
            ToDoubleFunction<P> readX,
            ToDoubleFunction<P> readY,
            double x,
            double y) {

        if (ring.size() < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            return false;
        }
        var isInside = false;
        var count = ring.size();
        for (var i = 0; i < count; i++) {

            var startX = readX.applyAsDouble(ring.get(i));
            var startY = readY.applyAsDouble(ring.get(i));
            var endX = readX.applyAsDouble(ring.get((i + 1) % count));
            var endY = readY.applyAsDouble(ring.get((i + 1) % count));

            // Only an edge that straddles the ray's height can cross it. An endpoint
            // exactly at the height counts below, so a shared corner toggles once
            // rather than twice.
            var isStraddlingRayHeight = (startY > y) != (endY > y);
            if (!isStraddlingRayHeight) {
                continue;
            }
            // Where the edge meets the ray's horizontal line. A straddling edge spans
            // the height by definition, so the height delta below is never zero.
            var crossingX = startX + (y - startY) * (endX - startX) / (endY - startY);

            // Only crossings to the point's right count, so the ray runs one way.
            if (x < crossingX) {
                isInside = !isInside;
            }
        }
        return isInside;
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
