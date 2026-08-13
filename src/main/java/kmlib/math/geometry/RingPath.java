package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A closed ring turned into a path: one traversal direction, one agreed starting
 * point, and every position on it named by how far round from that start it sits.
 *
 * <p>A ring on its own has none of those. Its winding is whatever the pass that built
 * it happened to produce, its first vertex is wherever that pass happened to begin,
 * and a position on it can only be stated as "this far along that edge" - which says
 * nothing until the edge is identified too. Anything laid out <em>along</em> a ring
 * needs all three settled, and settled the same way for every ring, or two shapes
 * carrying the same layout draw it starting from different corners and running in
 * opposite directions.
 *
 * <p>So the three are settled here, once. Traversal is clockwise (negative signed
 * area - clockwise on screen where y points up), whatever winding the ring arrived
 * in. The start is the ring's top centre: the highest point at which the vertical
 * line through a caller-supplied anchor crosses it, so shapes sharing an anchor
 * convention all begin at the top and a layout reads left to right from there.
 * Position is arc length from that start, which makes a layout stated in distances
 * pure arithmetic against the path rather than a walk the caller repeats.
 *
 * <p>Tracing an inset of the ring rather than the ring itself is the same operation
 * on a smaller shape, and it is bundled ({@link #traceInsetRing}) because the inset
 * carries a verdict the caller would otherwise have to reach for itself: a shape
 * narrower than twice the inset has no room for it, and what comes back then is not
 * obviously wrong to look at - it can be a tidy, correctly wound ring that simply is
 * not the inset that was asked for. Answering that here means a non-empty path is
 * always one that can be walked.
 */
public final class RingPath {

    // Compare every pair of edges when splicing out folds, rather than only edges near
    // each other in the ring. An inset that outruns the shape's own width is exactly the
    // case an inset path must survive - a shape too small for the inset asked of it - and
    // there the two sides that cross can sit anywhere in the ring, not only either side
    // of one corner. The rings walked here are outlines of a few dozen vertices, so the
    // quadratic scan costs nothing worth saving.
    private static final int COMPARE_EVERY_EDGE_PAIR = 0;

    private final List<double[]> points;

    // Entry i is the distance from the start to points[i], so entry 0 is zero and the
    // final entry - one past the last vertex, where the closing edge arrives back at the
    // start - is the perimeter. Held rather than recomputed because every position query
    // is a search through it.
    private final double[] arcLengthAtPoint;

    private RingPath(List<double[]> points, double[] arcLengthAtPoint) {
        this.points = points;
        this.arcLengthAtPoint = arcLengthAtPoint;
    }

    /**
     * Traces the path around {@code ring} inset by {@code insetDistance}, starting at
     * the top centre above {@code topAnchor} and running clockwise.
     *
     * <p>The inset is mitred ({@link PolygonOffsets#insetPolygonByMiter}), so a
     * concavity survives it instead of being sheared off, and the folds a miter inset
     * leaves behind are spliced out. The winding is normalised before the offset rather
     * than after: the miter inset shrinks a counter-clockwise ring and grows a clockwise
     * one, so a ring handed over clockwise would otherwise be pushed <em>outward</em>,
     * putting the path outside the shape it was meant to run within.
     *
     * @param ring            the closed ring to trace inside, as {x, y} vertices in
     *                        either winding
     * @param insetDistance   how far inside the ring the path runs
     * @param miterSpikeLimit a convex corner whose miter would reach farther than this
     *                        multiple of the inset is bevelled instead, as
     *                        {@link PolygonOffsets#insetPolygonByMiter} describes
     * @param topAnchor       the {x, y} the start is found above: the path begins at the
     *                        highest crossing of the vertical line through it, or at the
     *                        inset ring's own topmost corner where that line misses it
     * @return the traced path, or {@link #nothingLeftToTrace()} when the ring had no room
     *         for the inset - it shrank past enclosing any area, or came back with a
     *         corner standing nearer the ring than the inset it was built from
     */
    public static RingPath traceInsetRing(
            List<double[]> ring,
            double insetDistance,
            double miterSpikeLimit,
            double[] topAnchor) {

        var counterClockwise = orientCounterClockwise(ring);

        var inset = PolygonOffsets.removeReversedLoops(
            PolygonOffsets.insetPolygonByMiter(
                counterClockwise,
                insetDistance,
                miterSpikeLimit),
            COMPARE_EVERY_EDGE_PAIR);

        var cleaned = Rings.removeConsecutiveDuplicates(inset);

        if (!hasRoomForInset(counterClockwise, cleaned, insetDistance)) {
            return nothingLeftToTrace();
        }

        var traced = rotateToTopCentre(reverseRing(cleaned), topAnchor);

        return new RingPath(traced, measureArcLengths(traced));
    }

    /**
     * The path that is not there - the answer when a ring left nothing to walk.
     *
     * <p>Named because the empty vertex list is a statement rather than a construction: a
     * caller reading {@code nothingLeftToTrace()} learns the ring had no room for the path
     * it asked for, where one reading an empty list has to work out whether the trace gave
     * up or was never attempted.
     *
     * @return the empty path
     */
    public static RingPath nothingLeftToTrace() {
        return new RingPath(new ArrayList<>(), new double[] {0});
    }

    /**
     * @return true when the ring left no path to walk, and the position queries
     *         therefore have nothing to answer with
     */
    public boolean isEmpty() {
        return points.isEmpty();
    }

    /**
     * How far it is all the way round, and so the range every position wraps within.
     *
     * @return the path's total length; zero when it is empty
     */
    public double getPerimeter() {
        return arcLengthAtPoint[arcLengthAtPoint.length - 1];
    }

    /**
     * @return the path's corners in traversal order, the start first, with no repeated
     *         closing vertex - the edge from the last corner back to the first is
     *         implied, as for any ring. A fresh list, though the point arrays themselves
     *         are shared
     */
    public List<double[]> getPoints() {
        return new ArrayList<>(points);
    }

    /**
     * The point sitting {@code arcLength} along the path from its start.
     *
     * <p>Distances wrap: one past the perimeter is one past the start, and a negative
     * distance measures backward from the start. So a layout that runs off the end of the
     * path continues round it rather than having to be split by its caller.
     *
     * @param arcLength how far along the path to measure, in the ring's own units
     * @return the {x, y} point there
     * @throws IllegalStateException when the path is empty and has no points to measure
     *         between
     */
    public double[] computePointAt(double arcLength) {

        requireSomethingToWalk();

        var wrapped = wrapArcLength(arcLength);
        var edge = findEdgeAt(wrapped);
        var from = points.get(edge);
        var to = points.get((edge + 1) % points.size());

        // No division guard: the corners were deduplicated at Limits.MIN_EDGE_LENGTH
        // before the arc lengths were measured, so no edge of a traced path is short
        // enough for this to divide by ~zero.
        var alongEdge = (wrapped - arcLengthAtPoint[edge])
            / (arcLengthAtPoint[edge + 1] - arcLengthAtPoint[edge]);

        return new double[] {
            from[0] + alongEdge * (to[0] - from[0]),
            from[1] + alongEdge * (to[1] - from[1])};
    }

    /**
     * The stretch of path between two distances along it, as a polyline: the point at
     * {@code startArcLength}, every corner the path turns at on the way, and the point at
     * {@code endArcLength}.
     *
     * <p>The corners are what makes this more than its two endpoints. A stretch spanning
     * a corner is not the straight line between its ends - it bends there - and whatever
     * gives the stretch girth has to bend with it, so the corners have to survive into
     * what is handed on rather than being flattened away here.
     *
     * <p>The walk only ever runs forward. A stretch reaching past the perimeter carries on
     * round the ring, so a layout running off the end needs no splitting by its caller; a
     * stretch longer than the whole perimeter is cut to one lap, since going round twice
     * would only retrace the same geometry; and an end <em>behind</em> its start is a
     * stretch of no length rather than a walk almost all the way round to reach it - which
     * is the safer reading of what is, at that point, a caller's arithmetic having gone
     * wrong.
     *
     * @param startArcLength how far along the path the stretch begins
     * @param endArcLength   how far along it ends, measured from the same origin as the
     *                       start; past the perimeter is expected rather than an error
     * @return the stretch as {x, y} points in traversal order, with no two consecutive
     *         points coincident - a stretch of no length is the single point it sits at
     * @throws IllegalStateException when the path is empty and has no points to walk
     *         between
     */
    public List<double[]> collectPointsBetween(double startArcLength, double endArcLength) {

        requireSomethingToWalk();

        var start = wrapArcLength(startArcLength);
        var span = Math.min(Math.max(endArcLength - startArcLength, 0.0), getPerimeter());
        var walked = new ArrayList<double[]>();

        walked.add(computePointAt(start));

        var edge = findEdgeAt(start);
        var walkedLength = arcLengthAtPoint[edge + 1] - start;

        // Bounded at one full lap as well as by the span: the span is already cut to the
        // perimeter, and the bound keeps a rounding whisker between the two from sending
        // the walk round again.
        for (var step = 0; step < points.size() && walkedLength < span; step++) {

            edge = (edge + 1) % points.size();
            appendUnlessCoincident(walked, points.get(edge));
            walkedLength += arcLengthAtPoint[edge + 1] - arcLengthAtPoint[edge];
        }
        appendUnlessCoincident(walked, computePointAt(start + span));

        return walked;
    }

    // Whether the offset really moved the whole ring the distance it was asked to: every
    // corner of the result stands at least that far off the ring it came from.
    //
    // The check has to be a measurement, because an offset that overruns the shape does
    // not always announce itself. A shape narrower than twice the inset has its two sides
    // cross over, and while that shows up as a self-intersection where it happens locally
    // - which the fold splice takes out - a shape overrun on every side at once simply
    // turns inside out: a square inset past half its width comes back as a smaller square,
    // correctly wound, self-intersecting nowhere, made of every edge running backwards.
    // Nothing about its shape is wrong; what is wrong is that its corners sit nearer the
    // original ring than the inset they were built from, and only measuring says so.
    //
    // The slack is the shared minimum edge length: the offset arithmetic is exact bar
    // rounding, so a corner is either at its distance to within a whisker or nowhere near.
    private static boolean hasRoomForInset(
            List<double[]> ring,
            List<double[]> inset,
            double insetDistance) {

        if (inset.size() < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            return false;
        }

        for (var corner : inset) {
            if (PolygonRegions.computeDistanceToBoundary(ring, corner)
                    < insetDistance - Limits.MIN_EDGE_LENGTH) {

                return false;
            }
        }
        return true;
    }

    // The ring re-listed to begin at its top centre, in the same order it arrived in: the
    // start point first, then every corner from the far end of the edge it split, round to
    // that edge's near end.
    private static List<double[]> rotateToTopCentre(List<double[]> ring, double[] topAnchor) {

        var start = findTopCentre(ring, topAnchor);
        var count = ring.size();
        var rotated = new ArrayList<double[]>(count + 1);

        rotated.add(start.point());

        for (var step = 1; step <= count; step++) {
            rotated.add(ring.get((start.edgeIndex() + step) % count));
        }

        // The walk ends back at the corner the start's edge leaves, and the start itself
        // may sit exactly on a corner at either end of that edge. Both leave a zero-length
        // edge the dedup takes out - one of them, never both, since that would need the
        // edge's two ends to coincide, which a deduplicated ring has none of. So the
        // rotation hands back at least as many corners as it was given, and a ring that
        // enclosed area still does.
        return Rings.removeConsecutiveDuplicates(rotated);
    }

    // Where the vertical line through the anchor last crosses the ring on the way up - the
    // ring's top at the anchor's x, and the edge that crossing splits.
    //
    // Taking the highest crossing rather than the first one above the anchor keeps the
    // answer defined wherever the anchor sits: a ring is not always convex and an anchor
    // is not always within the shape traced from it (an inset ring can pull away past it),
    // so "the first crossing going up" has cases with no answer where "the topmost
    // crossing" has one. Where the anchor does sit inside a convex ring the two agree.
    private static TopCentre findTopCentre(List<double[]> ring, double[] topAnchor) {

        TopCentre highest = null;
        var count = ring.size();

        for (var i = 0; i < count; i++) {

            var from = ring.get(i);
            var to = ring.get((i + 1) % count);

            // Half-open side test: an edge crosses the line when its ends sit on opposite
            // sides of it, a corner exactly on the line counting to one fixed side. So a
            // corner the line passes through is a crossing of one of its two edges rather
            // than of both or of neither.
            if ((from[0] <= topAnchor[0]) == (to[0] <= topAnchor[0])) {
                continue;
            }

            var alongEdge = (topAnchor[0] - from[0]) / (to[0] - from[0]);
            var crossing = new double[] {
                topAnchor[0],
                from[1] + alongEdge * (to[1] - from[1])};

            if (highest == null || crossing[1] > highest.point()[1]) {
                highest = new TopCentre(crossing, i);
            }
        }

        // The line misses the ring entirely when the anchor sits beyond it to the left or
        // right, which an anchor away from the shape's own centre can. The ring's own
        // topmost corner is the nearest thing to a top centre then, and it keeps every
        // ring starting somewhere at its top rather than dropping the path over an anchor
        // that only says where to look.
        return highest == null ? findTopCorner(ring) : highest;
    }

    // The ring's highest corner, as a start splitting the edge that leaves it - so the
    // rotation begins at that corner and carries on forward from there.
    private static TopCentre findTopCorner(List<double[]> ring) {

        var top = 0;

        for (var i = 1; i < ring.size(); i++) {
            if (ring.get(i)[1] > ring.get(top)[1]) {
                top = i;
            }
        }
        return new TopCentre(ring.get(top), top);
    }

    // The ring wound counter-clockwise, reversed only when it arrived the other way.
    private static List<double[]> orientCounterClockwise(List<double[]> ring) {
        return PolygonRegions.computeSignedArea(ring) < 0 ? reverseRing(ring) : ring;
    }

    // The same ring traced the other way round, as a fresh list - the corners are shared,
    // since nothing here moves one.
    private static List<double[]> reverseRing(List<double[]> ring) {

        var reversed = new ArrayList<>(ring);

        Collections.reverse(reversed);

        return reversed;
    }

    // How far each corner sits from the start, plus the perimeter one past the last - the
    // running total that turns a distance along the path into a corner and a fraction of
    // the edge leaving it.
    private static double[] measureArcLengths(List<double[]> points) {

        var arcLengths = new double[points.size() + 1];

        for (var i = 0; i < points.size(); i++) {
            arcLengths[i + 1] = arcLengths[i]
                + Points.computeDistance(points.get(i), points.get((i + 1) % points.size()));
        }
        return arcLengths;
    }

    // Adds a point unless it lands where the walk already is. A stretch can begin or end
    // exactly on a corner, and the zero-length step that leaves is a degenerate piece for
    // whatever gives the polyline girth.
    private static void appendUnlessCoincident(List<double[]> walked, double[] point) {

        if (!Rings.isSamePoint(walked.get(walked.size() - 1), point)) {
            walked.add(point);
        }
    }

    // The distance brought into [0, perimeter), so a layout running past the end of the
    // path carries on round it and one measured backward from the start counts from the
    // end.
    private double wrapArcLength(double arcLength) {

        var wrapped = arcLength % getPerimeter();
        return wrapped < 0 ? wrapped + getPerimeter() : wrapped;
    }

    // The edge a distance round the ring falls on: the last one starting at or before it.
    // Walked from the far end so a distance landing exactly on a corner belongs to the
    // edge leaving that corner, not to the one arriving at it - which is what makes a walk
    // resuming from that distance step forward rather than back over the corner it is on.
    private int findEdgeAt(double arcLength) {

        for (var edge = points.size() - 1; edge > 0; edge--) {
            if (arcLengthAtPoint[edge] <= arcLength) {
                return edge;
            }
        }
        return 0;
    }

    private void requireSomethingToWalk() {

        if (isEmpty()) {
            throw new IllegalStateException("Cannot measure along a path with no points");
        }
    }

    // Where a path starts, as the point itself plus the edge of the ring it splits - the
    // pair a rotation needs, since the point alone does not say which corner comes next.
    private record TopCentre(
        double[] point,
        int edgeIndex) {
    }
}
