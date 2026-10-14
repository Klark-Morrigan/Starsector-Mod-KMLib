package kmlib.math.geometry;

import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Measures closed rings, one at a time or as a set, where the offset and smoothing passes
 * reshape them.
 *
 * <p>Of one ring it measures the area and winding, how often the ring crosses itself, whether a
 * point falls inside, and how far a point sits from the boundary. It also hands a ring back wound
 * a chosen way round, as a reordered copy.
 *
 * <p>Of a set of rings it says which ring holds a point, asking each ring on its own. It also
 * sorts a flat ring soup into the {@link RingRegion}s it bounds.
 *
 * <p>Where a line or a band stays inside a region is {@link RegionSpans}' question.
 */
public final class PolygonRegions {

    /**
     * What {@link #findRingHolding} and {@link #findSmallestEnclosingRing} answer when no ring
     * holds the point - a verdict rather than a failure, since a point outside every ring is a
     * case the caller usually wants to hear about.
     */
    public static final int NO_RING = -1;

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
     * Whether a point falls inside a closed ring of vectors -
     * {@link #isPointInsideRing(List, double, double)} for a ring laid out in the float
     * coordinates the game's UI works in, with the same even-odd rule and the same
     * boundary caveat.
     *
     * @param ring  closed polygon vertices, in any winding
     * @param point the point to test
     * @return true when the point lies within the ring; false when outside, and always
     *         for a ring too short to enclose area
     */
    public static boolean isPointInsideRing(List<Vector2f> ring, Vector2f point) {
        return isPointInsideRing(ring, vertex -> vertex.x, vertex -> vertex.y, point.x, point.y);
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
     * Which of a set of rings holds a point - the first one, in list order, whose interior the
     * point falls within.
     *
     * <p>Each ring is asked on its own by {@link #isPointInsideRing(List, double, double)}, with
     * its even-odd rule and its boundary caveat. That is a different question from the even-odd
     * parity over all the rings together that a region with holes is read by: here a point inside
     * an outer ring and its hole is held by both, and the first of them is the answer.
     *
     * @param rings closed polygon rings as {x, y} vertex lists, in any winding
     * @param x     the point's x coordinate
     * @param y     the point's y coordinate
     * @return the index of the first ring holding the point, or {@link #NO_RING} when none does
     */
    public static int findRingHolding(List<List<double[]>> rings, double x, double y) {

        for (var index = 0; index < rings.size(); index++) {
            if (isPointInsideRing(rings.get(index), x, y)) {
                return index;
            }
        }
        return NO_RING;
    }

    /**
     * Which ring most tightly holds a point - the one of smallest enclosed area among those whose
     * interior the point falls within.
     *
     * <p>Smallest rather than first because nested rings all hold a point inside the innermost,
     * and the innermost is usually the one that point belongs to. Area is compared by magnitude,
     * so a ring wound either way competes on the area it encloses. Where two holding rings
     * enclose exactly the same area, the earlier one in the list wins.
     *
     * @param rings closed polygon rings as {x, y} vertex lists, in any winding
     * @param x     the point's x coordinate
     * @param y     the point's y coordinate
     * @return the index of the smallest ring holding the point, or {@link #NO_RING} when none
     *         does
     */
    public static int findSmallestEnclosingRing(List<List<double[]>> rings, double x, double y) {

        var smallestIndex = NO_RING;
        var smallestArea = Double.MAX_VALUE;

        for (var index = 0; index < rings.size(); index++) {

            var ring = rings.get(index);
            if (!isPointInsideRing(ring, x, y)) {
                continue;
            }
            var area = Math.abs(computeSignedArea(ring));

            if (area < smallestArea) {
                smallestArea = area;
                smallestIndex = index;
            }
        }
        return smallestIndex;
    }

    /**
     * Whether any of a set of rings holds a point - {@link #findRingHolding} for a caller that
     * only needs the yes or no.
     *
     * @param rings closed polygon rings as {x, y} vertex lists, in any winding
     * @param x     the point's x coordinate
     * @param y     the point's y coordinate
     * @return true when at least one ring holds the point; false for none, and for no rings
     */
    public static boolean isPointInsideAnyRing(List<List<double[]>> rings, double x, double y) {
        return findRingHolding(rings, x, y) != NO_RING;
    }

    /**
     * A ring wound the asked way round - the same vertices, reversed when the ring's winding
     * disagrees with the one asked for.
     *
     * <p>Winding is read from the sign of {@link #computeSignedArea}: positive is
     * counter-clockwise. A ring enclosing no area winds neither way and comes back in its own
     * order. Reversing changes which way the path runs, never where its vertices are.
     *
     * @param ring               closed polygon vertices as {x, y} pairs
     * @param isCounterClockwise true for a counter-clockwise ring, false for a clockwise one
     * @return a new list holding the ring's vertices in the asked winding; the input is not
     *         changed, and the vertex arrays are shared with it
     */
    public static List<double[]> orientRing(List<double[]> ring, boolean isCounterClockwise) {

        var oriented = new ArrayList<>(ring);
        var signedArea = computeSignedArea(ring);
        var isWoundAgainst = isCounterClockwise
            ? signedArea < 0
            : signedArea > 0;

        if (isWoundAgainst) {
            Collections.reverse(oriented);
        }
        return oriented;
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
            if (outerIndex != NO_RING) {
                holeRingsByOuterIndex.get(outerIndex).add(holeRing);
            }
        }

        var regions = new ArrayList<RingRegion>(outerRings.size());
        for (var i = 0; i < outerRings.size(); i++) {
            regions.add(new RingRegion(outerRings.get(i), holeRingsByOuterIndex.get(i)));
        }
        return regions;
    }

    // The index of the smallest outer ring enclosing the hole, or NO_RING when none does. A
    // hole is tested by its first vertex: a hole lies strictly within the body it is cut from,
    // so any one of its vertices decides containment, and boundary points - the case the
    // point-in-ring test leaves undefined - do not arise between a hole and its own outer.
    private static int findSmallestContainingOuterRingIndex(
            List<List<double[]>> outerRings,
            List<double[]> holeRing) {

        var vertex = holeRing.get(0);

        return findSmallestEnclosingRing(outerRings, vertex[0], vertex[1]);
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
}
