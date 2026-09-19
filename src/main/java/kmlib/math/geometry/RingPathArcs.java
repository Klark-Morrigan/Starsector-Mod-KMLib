package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Which stretches of a traced path are covered and which are left clear, stated in the path's own
 * arc lengths.
 *
 * <p>Three questions with one answer shape. Where the caller's keep-out shapes lie over the path,
 * where the inset that built the path overran the ring it was traced inside, and what is left
 * between them all. Each produces intervals that may overlap and arrive in no order, and each is
 * fused and inverted the same way, so the arithmetic is held once rather than per question.
 *
 * <p>Split from {@link RingPath} because it is the one part that reasons about intervals rather
 * than about the path: nothing here walks the ring, and what it is handed is a list of corners and
 * the distances to them.
 */
final class RingPathArcs {

    private RingPathArcs() {
    }

    // Where the keep-out shapes lie over the path, as intervals in its own arc lengths - one
    // per stretch of one edge one shape covers, in no order and free to overlap each other.
    //
    // Each edge is crossed against each shape rather than the whole ring being clipped by it,
    // because the crossing already answers which parts of the line through an edge lie inside
    // the shape; cutting that answer back to the edge's own length is what turns it into a
    // stretch of path, and offsetting it by where the edge starts states it in arc lengths.
    static List<double[]> collectCoveredArcs(
            List<double[]> points,
            double[] arcLengthAtPoint,
            List<List<double[]>> keepOutRings) {

        var covered = new ArrayList<double[]>();
        var nearbyRings = selectRingsOverlappingBounds(
            keepOutRings,
            Bounds.computeEnclosingBounds(points));

        for (var edge = 0; edge < points.size(); edge++) {

            var from = points.get(edge);
            var to = points.get((edge + 1) % points.size());
            var line = new DirectedLine(from[0], from[1], to[0] - from[0], to[1] - from[1]);
            var edgeLength = arcLengthAtPoint[edge + 1] - arcLengthAtPoint[edge];

            for (var ring : nearbyRings) {
                for (var span : PolygonRegions.findLineInteriorSpans(List.of(ring), line)) {
                    appendCoveredArc(covered, arcLengthAtPoint[edge], edgeLength, span);
                }
            }
        }
        return covered;
    }

    // The stretches between the covered ones: from the start of the path to the first, between
    // each consecutive pair, and from the last to the perimeter.
    static List<RingStretch> invertToClearArcs(List<double[]> coveredArcs, double perimeter) {

        var clear = new ArrayList<RingStretch>(coveredArcs.size() + 1);
        var cursor = 0.0;

        for (var arc : coveredArcs) {
            appendClearArc(clear, cursor, arc[0]);
            cursor = arc[1];
        }
        appendClearArc(clear, cursor, perimeter);

        return clear;
    }

    // One shape's cover of one edge, cut back to that edge and stated in arc lengths. The span
    // measures along the whole infinite line the edge lies on, so the part of it beyond either
    // end of the edge covers no point of the path and is dropped.
    static void appendCoveredArc(
            List<double[]> covered,
            double arcLengthAtEdgeStart,
            double edgeLength,
            double[] span) {

        var start = Math.max(span[0], 0.0);
        var end = Math.min(span[1], edgeLength);

        if (end - start > Limits.MIN_EDGE_LENGTH) {
            covered.add(new double[] {
                arcLengthAtEdgeStart + start,
                arcLengthAtEdgeStart + end});
        }
    }

    // A clear stretch, unless it is too short to lay anything along - which is what a shape
    // ending exactly where the next begins, or covering the path from its very start, leaves.
    static void appendClearArc(List<RingStretch> clear, double start, double end) {

        if (end - start > Limits.MIN_EDGE_LENGTH) {
            clear.add(new RingStretch(start, end));
        }
    }

    // The covered intervals sorted and fused into disjoint ones, so that what lies between them
    // is exactly what is clear. Both a shape covering several edges in a row and two shapes
    // overlapping each other arrive as separate intervals describing one covered stretch, and
    // intervals merely touching are fused too - a path pinched between two of them has no
    // stretch left there to lay anything along.
    static List<double[]> mergeOverlappingArcs(List<double[]> coveredArcs) {

        coveredArcs.sort(Comparator.comparingDouble(arc -> arc[0]));

        var merged = new ArrayList<double[]>(coveredArcs.size());

        for (var arc : coveredArcs) {

            var last = merged.isEmpty() ? null : merged.get(merged.size() - 1);

            if (last != null && arc[0] <= last[1] + Limits.MIN_EDGE_LENGTH) {
                last[1] = Math.max(last[1], arc[1]);
            } else {
                merged.add(new double[] {arc[0], arc[1]});
            }
        }
        return merged;
    }

    // The shapes near enough to the path to be worth crossing its every edge against, by a
    // plain bounds overlap. A shape costs one crossing per edge of the path, so a caller
    // handing over every keep-out on a whole map would otherwise pay for the ones lying
    // somewhere else entirely - which, for a path around one small shape, is most of them.
    static List<List<double[]>> selectRingsOverlappingBounds(
            List<List<double[]>> rings,
            Bounds pathBounds) {

        var overlapping = new ArrayList<List<double[]>>(rings.size());

        for (var ring : rings) {
            if (!ring.isEmpty()
                    && pathBounds.overlaps(Bounds.computeEnclosingBounds(ring))) {
                overlapping.add(ring);
            }
        }
        return overlapping;
    }

    // Where the offset failed to move the ring the distance it was asked to, as intervals in
    // the traced path's own arc lengths - one per corner standing nearer the ring than the
    // inset it was built from, in no order and free to overlap each other.
    //
    // The failure has to be measured, because an offset that overruns the shape does not
    // always announce itself. A shape narrower than twice the inset has its two sides cross
    // over, and while that shows up as a self-intersection where it happens locally - which
    // the fold splice takes out - a shape overrun on every side at once simply turns inside
    // out: a square inset past half its width comes back as a smaller square, correctly
    // wound, self-intersecting nowhere, made of every edge running backwards. Nothing about
    // its shape is wrong; what is wrong is that its corners sit nearer the original ring than
    // the inset they were built from, and only measuring says so.
    //
    // Carved rather than answered as a verdict on the whole ring, because a ring pinched in
    // one place is the ordinary case and its wide part is perfectly good to lay a layout on.
    // The verdict survives as what the carve leaves: a ring overrun everywhere fails at every
    // corner, so every stretch is carved and nothing is left.
    //
    // The slack is the shared minimum edge length: the offset arithmetic is exact bar
    // rounding, so a corner is either at its distance to within a whisker or nowhere near.
    static List<double[]> collectOverrunArcs(
            List<double[]> ring,
            List<double[]> traced,
            double[] arcLengthAtPoint,
            double insetDistance) {

        var overrun = new ArrayList<double[]>();
        var count = traced.size();

        for (var corner = 0; corner < count; corner++) {

            if (PolygonRegions.computeDistanceToBoundary(ring, traced.get(corner))
                    >= insetDistance - Limits.MIN_EDGE_LENGTH) {

                continue;
            }

            // From the corner before the failing one to the corner after it. Clearance is
            // sampled at corners, and a mid-edge point can stand nearer the ring than either
            // end of its edge - a spur poking at the middle of a long edge does exactly that
            // - so the carve is deliberately wider than the sample it is drawn from.
            appendOverrunArc(
                overrun,
                corner == 0
                    ? arcLengthAtPoint[count - 1] - arcLengthAtPoint[count]
                    : arcLengthAtPoint[corner - 1],
                arcLengthAtPoint[corner + 1],
                arcLengthAtPoint[count]);
        }
        return overrun;
    }

    // One failing corner's carve, stated within [0, perimeter]. The carve around the path's
    // first corner reaches back past the start, and the intervals a clear-arc search inverts
    // do not wrap, so such a carve is split at the start into the two pieces it is.
    static void appendOverrunArc(
            List<double[]> overrun,
            double startArcLength,
            double endArcLength,
            double perimeter) {

        if (startArcLength < 0) {
            overrun.add(new double[] {startArcLength + perimeter, perimeter});
            overrun.add(new double[] {0, endArcLength});
            return;
        }
        overrun.add(new double[] {startArcLength, endArcLength});
    }
}
