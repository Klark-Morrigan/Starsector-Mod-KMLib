package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * A closed ring walked as a path: clockwise, from the ring's top centre, with every position on it stated as the
 * arc length from that start.
 *
 * <p>A ring alone settles none of these. Its winding and its first vertex are whatever the pass that built it
 * produced. Anything laid out along a ring needs all three settled the same way for every ring, or two shapes
 * carrying one layout draw it from different corners in opposite directions.
 *
 * <ul>
 *   <li>Direction: clockwise (negative signed area, with y pointing up), whatever winding the ring arrived in.</li>
 *   <li>Start: the highest point where the vertical line through a caller's anchor crosses the ring.</li>
 *   <li>Position: arc length from the start, wrapping at the perimeter.</li>
 * </ul>
 *
 * <p>The path also answers which stretches of it are free to lay something along ({@link #findClearStretches})
 * and where a layout of a given length sits on one ({@link #placeSpanNearestStart}). Both work in the arc lengths
 * a layout is measured in, so a caller never leaves that one coordinate.
 */
public final class RingPath {

    private final List<double[]> points;

    // Entry i is the distance from the start to points[i], so entry 0 is zero and the final entry, one past the
    // last vertex, is the perimeter. Held because every position query searches it.
    private final double[] arcLengthAtPoint;

    // The stretches that failed the inset the path was traced at, as {start, end} arc lengths: sorted, disjoint
    // and within [0, perimeter]. A layout may no more lie on them than on a stretch a keep-out covers.
    private final List<double[]> overrunArcs;

    private RingPath(
            List<double[]> points,
            double[] arcLengthAtPoint,
            List<double[]> overrunArcs) {

        this.points = points;
        this.arcLengthAtPoint = arcLengthAtPoint;
        this.overrunArcs = overrunArcs;
    }

    /**
     * Traces the path around {@code ring} inset by {@code insetDistance}, starting at the top centre above
     * {@code topAnchor} and running clockwise.
     *
     * <p>The inset is mitred ({@link PolygonOffsets#insetPolygonByMiter}), so a concavity survives it. The ring is
     * wound counter-clockwise first, because the miter inset grows a clockwise ring instead of shrinking it.
     *
     * <p>Where the shape is narrower than twice the inset, the inset can come back looking sound while standing
     * nearer the ring than asked. So each corner's clearance is measured, and the stretches either side of a
     * corner that fails are left out of what {@link #findClearStretches} offers.
     *
     * @param ring            the closed ring to trace inside, as {x, y} vertices in either winding
     * @param insetDistance   how far inside the ring the path runs
     * @param miterSpikeLimit a convex corner whose miter would reach farther than this multiple of the inset is
     *                        bevelled instead, as {@link PolygonOffsets#insetPolygonByMiter} describes
     * @param topAnchor       the {x, y} the start is found above: the path begins at the highest crossing of the
     *                        vertical line through it, or at the inset ring's topmost corner where that line
     *                        misses it
     * @return the traced path; or {@link #nothingLeftToTrace()} when the inset left no area
     */
    public static RingPath traceInsetRing(
            List<double[]> ring,
            double insetDistance,
            double miterSpikeLimit,
            double[] topAnchor) {

        var counterClockwise = PolygonRegions.orientRing(ring, true);

        // Told the winding to keep rather than reading it off the net area: a thin tab long enough that its fold
        // outweighs the body nets clockwise, and the body would be spliced out instead of the fold. Every pair of
        // edges is compared, because sides an inset pushed past each other can sit anywhere along the ring.
        var inset = PolygonOffsets.removeReversedLoops(
            PolygonOffsets.insetPolygonByMiter(
                counterClockwise,
                insetDistance,
                miterSpikeLimit),
            true,
            PolygonOffsets.COMPARE_EVERY_EDGE_PAIR);

        var cleaned = Rings.removeConsecutiveDuplicates(inset);
        if (cleaned.size() < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            return nothingLeftToTrace();
        }

        var traced = RingPathStart.rotateToTopCentre(RingPathStart.reverseRing(cleaned), topAnchor);
        var arcLengths = measureArcLengths(traced);

        // Measured on the traced ring, so an overrun stretch is stated in the arc lengths a layout uses.
        return new RingPath(
            traced,
            arcLengths,
            RingPathArcs.mergeOverlappingArcs(
                RingPathArcs.collectOverrunArcs(counterClockwise, traced, arcLengths, insetDistance)));
    }

    /**
     * The path a ring with no room for it leaves.
     *
     * <p>Named so a caller reads that the ring had no room, rather than an empty list it has to interpret.
     *
     * @return the empty path
     */
    public static RingPath nothingLeftToTrace() {
        return new RingPath(new ArrayList<>(), new double[] {0}, new ArrayList<>());
    }

    /**
     * @return true when the ring left no path to walk, and the position queries therefore have nothing to answer
     *         with
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
     * @return the path's corners in traversal order, the start first, with no repeated closing vertex, as a fresh
     *         list of fresh {x, y} points
     */
    public List<double[]> getPoints() {

        var copies = new ArrayList<double[]>(points.size());

        for (var point : points) {
            copies.add(point.clone());
        }

        return copies;
    }

    /**
     * The point sitting {@code arcLength} along the path from its start.
     *
     * <p>Distances wrap: one past the perimeter is one past the start, and a negative distance measures backward
     * from the start. A layout running off the end continues round without its caller splitting it.
     *
     * @param arcLength how far along the path to measure, in the ring's own units
     * @return the {x, y} point there
     * @throws IllegalStateException when the path is empty and has no points to measure between
     */
    public double[] computePointAt(double arcLength) {

        requireSomethingToWalk();

        var wrapped = wrapArcLength(arcLength);
        var edge = findEdgeAt(wrapped);
        var from = points.get(edge);
        var to = points.get((edge + 1) % points.size());

        // No division guard: the corners were deduplicated at Limits.MIN_EDGE_LENGTH before the arc lengths were
        // measured, so no edge is short enough to divide by ~zero.
        var alongEdge = (wrapped - arcLengthAtPoint[edge])
            / (arcLengthAtPoint[edge + 1] - arcLengthAtPoint[edge]);

        return new double[] {
            from[0] + alongEdge * (to[0] - from[0]),
            from[1] + alongEdge * (to[1] - from[1])};
    }

    /**
     * The stretches of the path that no keep-out shape covers and that held the inset the path was traced at.
     *
     * <p>Every shape is tested, whatever it belongs to: a shape reaching in from outside covers the path as much
     * as one raised over it. The stretches that failed the inset are left out too, so a caller taking the longest
     * of what is left needs no second list.
     *
     * <p>The stretches do not wrap. A shape covering the path's start leaves the pieces after and before it as the
     * first and last stretches, so a layout beginning at the start begins on the first.
     * {@link #fuseStretchAcrossStart} joins them where a caller wants one run.
     *
     * @param keepOutRings the shapes to keep clear of, each a closed ring of {x, y} vertices in either winding;
     *                     concave rings are handled
     * @return the clear stretches, ascending and disjoint; empty when the path is empty or nothing of it is clear
     */
    public List<RingStretch> findClearStretches(List<List<double[]>> keepOutRings) {

        if (isEmpty()) {
            return new ArrayList<>();
        }

        var covered = RingPathArcs.collectCoveredArcs(points, arcLengthAtPoint, keepOutRings);

        covered.addAll(overrunArcs);

        return RingPathArcs.invertToClearArcs(
            RingPathArcs.mergeOverlappingArcs(covered),
            getPerimeter());
    }

    /**
     * The stretches of the path that held the inset it was traced at, with nothing else kept clear of them.
     *
     * <p>The answer about the shape the path was traced inside, where {@link #findClearStretches} also answers
     * about what a caller put on it. A caller choosing between insets, or falling back when its keep-outs left
     * nothing, asks this one.
     *
     * @return the stretches that held the inset, ascending and disjoint; empty when the path is empty or was
     *         overrun the whole way round
     */
    public List<RingStretch> findStretchesHoldingItsInset() {
        return RingPathArcs.invertToClearArcs(overrunArcs, getPerimeter());
    }

    /**
     * The stretches of the path that failed the inset it was traced at: the exact complement of
     * {@link #findStretchesHoldingItsInset}.
     *
     * <p>A caller reporting on the ring draws both, since what survived alone cannot show that a ring was carved.
     * A carve across the path's start comes back as two stretches, one at each end, for the reason
     * {@link #findClearStretches} gives.
     *
     * @return the carved stretches, ascending and disjoint; empty when the whole path held its inset or the path
     *         is empty
     */
    public List<RingStretch> findStretchesFailingItsInset() {

        var failing = new ArrayList<RingStretch>(overrunArcs.size());

        for (var arc : overrunArcs) {
            failing.add(new RingStretch(arc[0], arc[1]));
        }

        return failing;
    }

    /**
     * Whether any of the path held the inset it was traced at.
     *
     * <p>A ring overrun in one place still holds the rest of itself. Only a ring overrun everywhere holds nothing.
     *
     * @return true when at least one stretch held the inset; false for an empty path
     */
    public boolean hasStretchHoldingItsInset() {
        return !findStretchesHoldingItsInset().isEmpty();
    }

    /**
     * The stretch of path between two distances along it, as a polyline: the point at {@code startArcLength},
     * every corner the path turns at on the way, and the point at {@code endArcLength}.
     *
     * <p>The corners are kept because a stretch spanning one bends there, and whatever gives the stretch girth has
     * to bend with it.
     *
     * <p>The walk only runs forward. A stretch reaching past the perimeter carries on round the ring. One longer
     * than the perimeter is cut to one lap. An end behind its start gives a stretch of no length, rather than a
     * walk almost all the way round.
     *
     * @param startArcLength how far along the path the stretch begins
     * @param endArcLength   how far along it ends, from the same origin; past the perimeter is expected
     * @return the stretch as fresh {x, y} points in traversal order, no two consecutive points coincident; a
     *         stretch of no length is the single point it sits at
     * @throws IllegalStateException when the path is empty and has no points to walk between
     */
    public List<double[]> collectPointsBetween(double startArcLength, double endArcLength) {

        requireSomethingToWalk();

        var start = wrapArcLength(startArcLength);
        var span = Math.min(Math.max(endArcLength - startArcLength, 0.0), getPerimeter());
        var walked = new ArrayList<double[]>();

        walked.add(computePointAt(start));

        var edge = findEdgeAt(start);
        var walkedLength = arcLengthAtPoint[edge + 1] - start;

        // Bounded at one lap as well as by the span, so a rounding whisker between the two cannot send the walk
        // round again.
        for (var step = 0; step < points.size() && walkedLength < span; step++) {

            edge = (edge + 1) % points.size();
            appendUnlessCoincident(walked, points.get(edge).clone());
            walkedLength += arcLengthAtPoint[edge + 1] - arcLengthAtPoint[edge];
        }

        appendUnlessCoincident(walked, computePointAt(start + span));

        return walked;
    }

    /**
     * Each stretch as the polyline {@link #collectPointsBetween} walks it, corners included.
     *
     * @param stretches the stretches to walk; one closing past the perimeter carries on round
     * @return one polyline per stretch, in the stretches' own order
     * @throws IllegalStateException when the path is empty and there is a stretch to walk
     */
    public List<List<double[]>> collectStretchPoints(List<RingStretch> stretches) {

        var polylines = new ArrayList<List<double[]>>(stretches.size());

        for (var stretch : stretches) {
            polylines.add(collectPointsBetween(stretch.startArcLength(), stretch.endArcLength()));
        }

        return polylines;
    }

    /**
     * Spans of the given lengths laid end to end from {@code startArcLength}, each as the polyline
     * {@link #collectPointsBetween} walks it.
     *
     * <p>Each span opens exactly where the one before it closed, so the pieces of one layout meet without a gap
     * or an overlap however the lengths round, and the caller keeps no running position.
     *
     * @param startArcLength where the first span opens
     * @param spanLengths    each span's length, in layout order; a span of no length is the single point it
     *                       sits at
     * @return one polyline per span, in the order the lengths were given
     * @throws IllegalStateException when the path is empty and there is a span to walk
     */
    public List<List<double[]>> collectSpansEndToEnd(double startArcLength, double[] spanLengths) {

        var spans = new ArrayList<List<double[]>>(spanLengths.length);
        var cursor = startArcLength;

        for (var spanLength : spanLengths) {

            var spanEnd = cursor + spanLength;

            spans.add(collectPointsBetween(cursor, spanEnd));
            cursor = spanEnd;
        }

        return spans;
    }

    /**
     * The same stretches with the pair meeting at the path's start fused into one, closing past the perimeter.
     *
     * <p>{@link #findClearStretches} does not wrap, so a run through the start arrives as two stretches. A caller
     * choosing between stretches (the longest, the first that fits) would otherwise judge that run on its bigger
     * half alone.
     *
     * <p>Two stretches meet at the start only when one opens the path and the other closes it. A single stretch
     * never fuses: it is either the whole path or covered at both ends.
     *
     * @param clearStretches the stretches to read, ascending and disjoint as {@link #findClearStretches} hands
     *                       them back
     * @return the same stretches with any pair meeting at the start fused into one, placed last; the list
     *         unchanged where none do
     */
    public List<RingStretch> fuseStretchAcrossStart(List<RingStretch> clearStretches) {

        if (clearStretches.size() < 2) {
            return clearStretches;
        }

        var first = clearStretches.get(0);
        var last = clearStretches.get(clearStretches.size() - 1);

        if (first.startArcLength() > Limits.MIN_EDGE_LENGTH
                || last.endArcLength() < getPerimeter() - Limits.MIN_EDGE_LENGTH) {

            return clearStretches;
        }

        var fused = new ArrayList<>(clearStretches.subList(1, clearStretches.size() - 1));

        fused.add(new RingStretch(
            last.startArcLength(),
            first.endArcLength() + getPerimeter()));

        return fused;
    }

    /**
     * Where along {@code stretch} a layout of {@code spanLength} begins, to sit as near the path's start as the
     * stretch allows.
     *
     * <p>The path's start is the one position every path shares, so a layout placed against it reads from a
     * landmark. Nearness is measured on the layout's start, not its centre: a layout is read from its start, and
     * pulled in by its middle it would straddle the landmark.
     *
     * <p>The rule is one clamp: start at the path's start, pulled into
     * {@code [stretchStart, stretchEnd - spanLength]} the shortest way round. Where the path's start lies off the
     * stretch, the nearer of those two bounds wins, and a tie takes the stretch's start so the answer never rests
     * on rounding. A span longer than its stretch opens at the stretch's start, since sizing the layout is the
     * caller's job.
     *
     * @param stretch    the stretch the layout may occupy; one closing past the perimeter is expected
     * @param spanLength how far along the path the layout reaches
     * @return the arc length the layout begins at, past the perimeter where the stretch runs past it
     * @throws IllegalStateException when the path is empty and has no lap to place within
     */
    public double placeSpanNearestStart(RingStretch stretch, double spanLength) {

        requireSomethingToWalk();

        var latestStart = Math.max(
            stretch.startArcLength(),
            stretch.endArcLength() - spanLength);

        var pathStart = alignStartWithStretch(stretch.startArcLength());

        if (pathStart <= latestStart) {
            return pathStart;
        }

        if (pathStart <= stretch.endArcLength()) {
            return latestStart;
        }

        return selectNearerEndOfStretch(stretch, latestStart, pathStart);
    }

    // The path's start on the stretch's own lap: the first multiple of the perimeter at or after the stretch
    // opens. A stretch fused across the start runs past the perimeter, so the start it holds is a lap on, not
    // zero.
    private double alignStartWithStretch(double startArcLength) {
        return Math.ceil(startArcLength / getPerimeter()) * getPerimeter();
    }

    // Which bound a layout takes when the path's start lies off the stretch: the stretch's own start, or the
    // latest start it allows. Measured to those two starts rather than to the stretch's ends, because nearness is
    // nearness of the layout's start.
    private double selectNearerEndOfStretch(
            RingStretch stretch,
            double latestStart,
            double pathStart) {

        var forwardToStretchStart = stretch.startArcLength() + getPerimeter() - pathStart;
        var backwardToLatestStart = pathStart - latestStart;

        return forwardToStretchStart <= backwardToLatestStart
            ? stretch.startArcLength()
            : latestStart;
    }

    // How far each corner sits from the start, plus the perimeter one past the last.
    private static double[] measureArcLengths(List<double[]> points) {

        var arcLengths = new double[points.size() + 1];

        for (var i = 0; i < points.size(); i++) {

            arcLengths[i + 1] = arcLengths[i]
                + Points.computeDistance(points.get(i), points.get((i + 1) % points.size()));
        }

        return arcLengths;
    }

    // A stretch can begin or end exactly on a corner, and the zero-length step that leaves is a degenerate piece
    // for whatever gives the polyline girth.
    private static void appendUnlessCoincident(List<double[]> walked, double[] point) {

        if (!Rings.isSamePoint(walked.get(walked.size() - 1), point)) {
            walked.add(point);
        }
    }

    // The distance brought into [0, perimeter).
    private double wrapArcLength(double arcLength) {

        var wrapped = arcLength % getPerimeter();
        return wrapped < 0
            ? wrapped + getPerimeter()
            : wrapped;
    }

    // The edge a distance falls on: the last one starting at or before it. Searched from the far end so a distance
    // landing exactly on a corner belongs to the edge leaving that corner, and a walk resuming there steps forward.
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
}
