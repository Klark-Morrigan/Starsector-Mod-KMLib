package kmlib.math.geometry;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Chains a bag of directed edge segments into ordered closed rings.
 *
 * <p>The input is the boundary of some region as loose, unordered {@link Segment}s,
 * each directed so the region lies consistently to one side.
 * The segments meet end-to-start at shared corners but are handed over in no
 * particular order and computed independently, so a corner's two segments can
 * report it at coordinates that differ by a rounding whisker. This welds
 * coincident endpoints within a tolerance, then walks each segment to the next
 * whose start meets its end, emitting the closed loops that walk out.
 *
 * <p>The region merging this backs (fusing same-owner Voronoi cells into a bloc,
 * then tracing the bloc's national border) drops the shared interior edges, so the
 * surviving boundary segments form clean loops: each welded corner has exactly one
 * segment arriving and one leaving. Disjoint pieces of the region and holes inside
 * it come back as their own rings. Pathological input (a corner with more than one
 * way out, or a strand that never closes) is walked defensively rather than trusted
 * - an unclosed strand is dropped rather than emitted as a stray open ring. A segment
 * shorter than the weld tolerance is likewise defended against: both its ends land on
 * one corner, so it is retired as a corner rather than walked as a step, and the loop
 * it sat in still closes around it.
 *
 * <p>A caller that computed a per-segment attribute before handing the segments over
 * (an offset distance, a class, a colour) recovers it aligned to the chained edges
 * with {@link #chainIntoRingsWithEdgeValues}: the walk carries each segment's value
 * onto the ring edge it becomes, so a value decided per loose segment survives the
 * re-ordering into rings.
 */
public final class EdgeRings {

    private EdgeRings() {
    }

    /**
     * A chained ring paired with a per-edge value carried from the segment that
     * formed each edge - the output of {@link #chainIntoRingsWithEdgeValues}.
     *
     * @param corners    the ring's {@code {x, y}} corners in winding order, as
     *                   {@link #chainIntoRings} returns them
     * @param edgeValues parallel to {@code corners}: entry {@code k} is the value of
     *                   the segment forming the edge from corner {@code k} to corner
     *                   {@code (k + 1)} modulo the count, so an offset or class
     *                   decided per loose segment lands on the ring edge it became
     */
    public record RingWithEdgeValues(List<double[]> corners, double[] edgeValues) {
    }

    /**
     * Chains directed segments into ordered closed rings.
     *
     * <p>Each segment is directed from its start to its end. Two endpoints closer
     * than {@code weldTolerance} are treated as the
     * same corner, absorbing the rounding drift between two independent
     * computations of a shared vertex. Every segment is consumed once; the walk
     * follows each segment's end to a segment whose start welds to it, closing a
     * ring when it returns to where it began.
     *
     * @param segments      the boundary segments, in any order
     * @param weldTolerance the largest gap between two endpoints still treated as
     *                      the same corner; must exceed the coordinates' rounding
     *                      drift yet stay well below the smallest real edge
     * @return the closed rings, each a list of {@code {x, y}} corners in the input
     *         segments' winding; rings of fewer than three corners and strands that
     *         never close are omitted
     */
    public static List<List<double[]>> chainIntoRings(
            List<Segment> segments,
            double weldTolerance) {
        var rings = new ArrayList<List<double[]>>();
        for (var indexRing : chainIntoSegmentIndexRings(segments, weldTolerance)) {
            rings.add(collectCorners(segments, indexRing));
        }
        return rings;
    }

    /**
     * Chains directed segments into closed rings as {@link #chainIntoRings} does, but
     * also hands back, per ring edge, the value of the segment that formed it.
     *
     * <p>The chaining re-orders a loose bag of segments, so a per-segment attribute
     * computed before the walk (a signed offset, an edge class) cannot be matched back
     * by index afterward. This carries each segment's value along the walk onto the
     * ring edge it becomes, so the returned {@code edgeValues} stay parallel to the
     * ring's edges - the corner-{@code k}-to-corner-{@code (k + 1)} edge carries the
     * value of the segment chained at that step.
     *
     * @param segments      the boundary segments, in any order
     * @param segmentValues parallel to {@code segments}: the value to carry for each
     * @param weldTolerance the largest gap between two endpoints still treated as the
     *                      same corner, as in {@link #chainIntoRings}
     * @return the closed rings, each with its per-edge values; rings of fewer than
     *         three corners and strands that never close are omitted
     * @throws IllegalArgumentException when {@code segmentValues} is not parallel to
     *         {@code segments}
     */
    public static List<RingWithEdgeValues> chainIntoRingsWithEdgeValues(
            List<Segment> segments,
            double[] segmentValues,
            double weldTolerance) {
        if (segmentValues.length != segments.size()) {
            throw new IllegalArgumentException(
                "segmentValues must be parallel to the segments: "
                    + segmentValues.length
                    + " vs "
                    + segments.size());
        }
        var rings = new ArrayList<RingWithEdgeValues>();
        for (var indexRing : chainIntoSegmentIndexRings(segments, weldTolerance)) {
            var edgeValues = new double[indexRing.size()];
            for (var k = 0; k < indexRing.size(); k++) {
                edgeValues[k] = segmentValues[indexRing.get(k)];
            }
            rings.add(new RingWithEdgeValues(collectCorners(segments, indexRing), edgeValues));
        }
        return rings;
    }

    // Chains the segments into rings expressed as the ordered segment indices they
    // walk through, the shared core both public entry points map to their own output:
    // corners come from each index's segment start, and a carried per-edge value from
    // each index's segment value. Welding to integer IDs and the defensive walk are
    // unchanged; only the ring is recorded as segment indices rather than corners, so
    // the segment behind each edge stays recoverable.
    private static List<List<Integer>> chainIntoSegmentIndexRings(
            List<Segment> segments,
            double weldTolerance) {
        var indexRings = new ArrayList<List<Integer>>();
        if (segments.isEmpty()) {
            return indexRings;
        }

        // Weld first so the walk can compare corners as exact integer IDs rather
        // than by tolerance at every hop: a segment's end welds to the same ID as
        // the next segment's start, so chaining is a plain map lookup.
        var welder = new VertexWelder(weldTolerance);
        var startId = new int[segments.size()];
        var endId = new int[segments.size()];
        // Segments leaving each corner, so the walk finds the continuation in O(1).
        var outgoingByCorner = new HashMap<Integer, ArrayDeque<Integer>>();
        for (var i = 0; i < segments.size(); i++) {
            var segment = segments.get(i);
            startId[i] = welder.weld(segment.startX(), segment.startY());
            endId[i] = welder.weld(segment.endX(), segment.endY());
            outgoingByCorner.computeIfAbsent(startId[i], corner -> new ArrayDeque<>()).add(i);
        }

        // A segment whose two ends weld to one corner says nothing at this resolution: it is
        // shorter than the drift the weld exists to absorb, so it names a corner rather than a
        // step between two. Retiring it up front is what keeps it from breaking the walk - left
        // in, it is either seeded into a one-segment ring that is then dropped, or walked into
        // mid-ring as a repeated corner, and either way the loop it belonged to is lost. Removing
        // it disconnects nothing, since the segments on both sides already weld to that very
        // corner and go on meeting there.
        var consumed = new boolean[segments.size()];
        for (var i = 0; i < segments.size(); i++) {
            if (startId[i] == endId[i]) {
                consumed[i] = true;
            }
        }
        for (var seed = 0; seed < segments.size(); seed++) {
            if (consumed[seed]) {
                continue;
            }
            var ring = walkRing(seed, startId, endId, outgoingByCorner, consumed);
            if (ring != null && ring.size() >= Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
                indexRings.add(ring);
            }
        }
        return indexRings;
    }

    // Walks from a seed segment, hopping end-to-start, until the chain returns to
    // the seed's start corner. Returns the segment indices walked in order, or null
    // when the strand dead-ends before closing (its segments are still marked
    // consumed so a later seed does not re-walk the same dead end).
    private static List<Integer> walkRing(
            int seed,
            int[] startId,
            int[] endId,
            Map<Integer, ArrayDeque<Integer>> outgoingByCorner,
            boolean[] consumed) {
        var ringStartCorner = startId[seed];
        var ring = new ArrayList<Integer>();
        var current = seed;
        while (current != -1 && !consumed[current]) {
            consumed[current] = true;
            ring.add(current);
            if (endId[current] == ringStartCorner) {
                return ring;
            }
            current = takeOutgoing(outgoingByCorner.get(endId[current]), consumed);
        }
        // Fell off the end without returning to the start: an open strand, dropped.
        return null;
    }

    // Maps a ring of segment indices to its corners - each edge's start point, in the
    // walked order - the {@code {x, y}} loop both public entry points return.
    private static List<double[]> collectCorners(
            List<Segment> segments,
            List<Integer> indexRing) {
        var corners = new ArrayList<double[]>(indexRing.size());
        for (var index : indexRing) {
            corners.add(segments.get(index).readStart());
        }
        return corners;
    }

    // Removes and returns an unused segment leaving a corner, discarding any that a
    // prior walk already consumed; -1 when none remain (a dead end).
    private static int takeOutgoing(
            ArrayDeque<Integer> outgoing,
            boolean[] consumed) {
        if (outgoing == null) {
            return -1;
        }
        while (!outgoing.isEmpty()) {
            var candidate = outgoing.poll();
            if (!consumed[candidate]) {
                return candidate;
            }
        }
        return -1;
    }

}
