package kmlib.math.geometry;

import java.util.ArrayList;
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
 * carries a fact the caller would otherwise have to reach for itself: where a shape is
 * narrower than twice the inset, what comes back is not obviously wrong to look at - it
 * can be a tidy, correctly wound stretch of ring that simply stands nearer the shape than
 * the inset it was built from. Measuring that here means the stretches a caller is offered
 * are always ones that held the inset they were asked for.
 *
 * <p>What is laid out along a path rarely has the whole of it to itself, so the path also
 * answers which stretches of it something else has claimed ({@link #findClearArcs}), reads
 * the pair of those meeting at its start as the one stretch they are
 * ({@link #fuseStretchAcrossStart}), and settles where along a stretch a layout of a given
 * length sits ({@link #placeSpanNearestStart}). All three are in the same arc lengths a
 * layout is measured in, which is why they belong here rather than beside the shapes doing
 * the claiming or the layout being laid: a caller subtracts room and places what is left
 * without ever leaving the one coordinate it laid the layout out in - and the two facts a
 * placement turns on, that the start is at zero and that distances wrap at the perimeter,
 * are this path's own.
 */
public final class RingPath {

    // Compare every pair of edges when splicing out folds, rather than only edges near
    // each other in the ring. An inset that outruns the shape's own width is exactly the
    // case an inset path must survive - a shape too small for the inset asked of it - and
    // there the two sides that cross can sit anywhere in the ring, not only either side
    // of one corner. The rings walked here are outlines of a few dozen vertices, so the
    // quadratic scan costs nothing worth saving.
    private static final int COMPARE_EVERY_EDGE_PAIR = 0;

    // A clear-arc search claiming nothing of its own, which leaves the path's own overrun
    // stretches as the only thing carved from it.
    private static final List<List<double[]>> NOTHING_KEPT_OUT = List.of();

    private final List<double[]> points;

    // Entry i is the distance from the start to points[i], so entry 0 is zero and the
    // final entry - one past the last vertex, where the closing edge arrives back at the
    // start - is the perimeter. Held rather than recomputed because every position query
    // is a search through it.
    private final double[] arcLengthAtPoint;

    // The stretches of this path that failed the inset they were traced at, as {start, end}
    // arc lengths - sorted, disjoint, and within [0, perimeter]. Held because a layout may
    // no more lie on them than on a stretch some other shape covers, so every clear-arc
    // search subtracts them alongside its caller's keep-outs.
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
     * @return the traced path, with the stretches that failed the inset already carved out
     *         of what {@link #findClearArcs} offers; or {@link #nothingLeftToTrace()} when
     *         the inset shrank the ring past enclosing any area at all
     */
    public static RingPath traceInsetRing(
            List<double[]> ring,
            double insetDistance,
            double miterSpikeLimit,
            double[] topAnchor) {

        var counterClockwise = RingPathStart.orientCounterClockwise(ring);

        var inset = PolygonOffsets.removeReversedLoops(
            PolygonOffsets.insetPolygonByMiter(
                counterClockwise,
                insetDistance,
                miterSpikeLimit),
            COMPARE_EVERY_EDGE_PAIR);

        var cleaned = Rings.removeConsecutiveDuplicates(inset);

        if (cleaned.size() < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            return nothingLeftToTrace();
        }

        var traced = RingPathStart.rotateToTopCentre(RingPathStart.reverseRing(cleaned), topAnchor);
        var arcLengths = measureArcLengths(traced);

        // Measured on the traced ring rather than during the offset, so an overrun stretch
        // is stated in the arc lengths a layout is measured in. Measured before the winding
        // was normalised and the ring rotated, every index would have to be carried through
        // both to mean anything.
        return new RingPath(
            traced,
            arcLengths,
            RingPathArcs.mergeOverlappingArcs(
                RingPathArcs.collectOverrunArcs(counterClockwise, traced, arcLengths, insetDistance)));
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
        return new RingPath(new ArrayList<>(), new double[] {0}, new ArrayList<>());
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
     * The stretches of the path no keep-out shape covers, as {@code {startArcLength,
     * endArcLength}} intervals in this path's own arc lengths.
     *
     * <p>What a layout runs along once something else has claimed part of the ring. A shape
     * lying over the path splits it, and the pieces either side are what is left to lay
     * anything out on - so they come back as intervals rather than as geometry, since a
     * caller measuring its layout in distances already reads the path that way.
     *
     * <p>Every shape is tested, whether or not it belongs to whatever this ring belongs to:
     * a keep-out is a fact about the plane, so one reaching in from outside covers the path
     * exactly as much as one raised over it.
     *
     * <p>The path's own overrun stretches are subtracted too, so what comes back is clear of
     * both. A stretch standing nearer the ring than the inset it was traced at is room a
     * layout may not use for the same reason a covered stretch is, and answering with both in
     * one list is what lets a caller take the longest of what is left without knowing there
     * were two reasons a stretch could be missing - or forgetting one of them.
     *
     * <p>The intervals do not wrap: a shape covering the path's start leaves the pieces
     * before and after it as the first and last intervals rather than fusing them into one
     * that straddles the origin. A layout beginning at the start therefore begins at the
     * first interval, which is what a start point means.
     *
     * @param keepOutRings the shapes to keep clear of, each a closed ring of {x, y} vertices
     *                     in either winding; concave rings are handled, and an empty list
     *                     leaves the path clear but for its own overrun stretches
     * @return the uncovered stretches, ascending and disjoint; the whole path as one
     *         stretch when nothing covers it, and empty when the path is empty or the
     *         shapes and the overruns cover all of it
     */
    public List<RingStretch> findClearArcs(List<List<double[]>> keepOutRings) {

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
     * The stretches of the path that held the inset it was traced at, before anything else is
     * kept clear of them - the ring the traced shape's own width leaves.
     *
     * <p>Named rather than left as {@link #findClearArcs} over an empty list, because it is a
     * different question with a different answer: this one is about the shape the path was
     * traced inside, where a clear-arc search is about what a caller has put on it. A caller
     * choosing between insets, or falling back when its keep-outs left nothing, is asking about
     * the ring rather than about the keep-outs.
     *
     * @return the stretches that stood at the inset, ascending and disjoint; empty when the
     *         path is empty or was overrun the whole way round
     */
    public List<RingStretch> findStretchesHoldingItsInset() {
        return findClearArcs(NOTHING_KEPT_OUT);
    }

    /**
     * The stretches of the path that failed the inset it was traced at - the carve itself,
     * and so the exact complement of {@link #findStretchesHoldingItsInset}.
     *
     * <p>Offered because a caller reporting on the ring needs the part a layout may not have
     * as much as the part it may: the two together are the whole path, and a reader shown only
     * what survived cannot tell a ring that was carved from one that was always that short.
     *
     * <p>A carve reaching across the path's start arrives as the two stretches it was split
     * into, one at each end, for the reason {@link #findClearArcs} states.
     *
     * @return the carved stretches, ascending and disjoint; empty when the whole path held its
     *         inset, and empty for a path that was never traced
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
     * <p>The refusal a trace used to answer with, derived rather than pronounced. A ring
     * overrun in one place carves that place and keeps the rest, and a ring overrun
     * everywhere carves every stretch and so has none - one rule covering both, where a
     * verdict on the whole ring gave the second answer to the first case.
     *
     * @return true when at least one stretch of the path stood at the inset it was asked for;
     *         false for an empty path, which held none
     */
    public boolean hasStretchHoldingItsInset() {
        return !findStretchesHoldingItsInset().isEmpty();
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

    /**
     * The same stretches with the one crossing the path's start read as the single stretch it
     * is: the last and the first fused into one running on past the perimeter.
     *
     * <p>{@link #findClearArcs} deliberately does not wrap, which is what makes "a layout
     * beginning at the path's start begins at the first stretch" true and is worth keeping.
     * But a shape lying anywhere but over the start leaves the path's longest run stated as
     * two of those stretches, so a caller choosing between them - the longest, the first that
     * fits - judges that run on whichever half happened to be bigger. Fusing is that caller's
     * to ask for, so only the one needing a wrapping stretch pays for one.
     *
     * <p>Two stretches meet across the start only by reaching it: one opening the path and one
     * closing it. Anything else leaves the start itself covered, where there is nothing to
     * fuse, and a single stretch is either the whole path or one with covered path at both
     * ends - neither crosses the start, and fusing one with itself would double it.
     *
     * @param clearArcs the stretches to read, ascending and disjoint as
     *                  {@link #findClearArcs} hands them back
     * @return the same stretches with any pair meeting at the start fused into one closing
     *         past the perimeter, that one last; the list unchanged where none do
     */
    public List<RingStretch> fuseStretchAcrossStart(List<RingStretch> clearArcs) {

        if (clearArcs.size() < 2) {
            return clearArcs;
        }
        var first = clearArcs.get(0);
        var last = clearArcs.get(clearArcs.size() - 1);

        if (first.startArcLength() > Limits.MIN_EDGE_LENGTH
                || last.endArcLength() < getPerimeter() - Limits.MIN_EDGE_LENGTH) {

            return clearArcs;
        }
        var fused = new ArrayList<>(clearArcs.subList(1, clearArcs.size() - 1));

        fused.add(new RingStretch(
            last.startArcLength(),
            first.endArcLength() + getPerimeter()));

        return fused;
    }

    /**
     * Where along {@code stretch} a layout of {@code spanLength} begins if it is to sit as near
     * the path's start as the stretch allows.
     *
     * <p>A layout shorter than the stretch it was given has room to slide, and where it sits is
     * a decision rather than an accident of where that stretch happened to open. The path's own
     * start is the one position every path shares - it is where a layout begins when nothing is
     * in its way - so a layout that could sit there and instead sits wherever the room opened
     * costs a reader the landmark to read it from.
     *
     * <p>Near is measured on the layout's <em>start</em>, not on its centre or its nearest end,
     * because a layout along a path is ordered from its start: pulled toward the landmark by its
     * middle it would straddle it and put the middle of itself where its opening belongs.
     *
     * <p>One clamp states the whole rule - the layout starts at the path's start, pulled into
     * {@code [stretchStart, stretchEnd - spanLength]} by the shortest way round. That range is
     * empty only if the span outruns the stretch, which a caller sizing its layout to fit has
     * already ruled out; asked anyway, the span is placed at the stretch's own start rather than
     * refused, since a placement cannot answer a length question. Where the path's start lies
     * off the stretch entirely, the two candidates are the stretch's start and the latest start
     * it allows, and the nearer wins with a tie taking the stretch's start - so a stretch lying
     * exactly opposite the path's start places the same way every call rather than on whichever
     * way the last of the rounding fell.
     *
     * @param stretch    the stretch the layout may occupy, from {@link #findClearArcs} or fused
     *                   across the start; one closing past the perimeter is expected rather
     *                   than an error
     * @param spanLength how far along the path the layout reaches
     * @return the arc length the layout begins at, in this path's own arc lengths and past the
     *         perimeter where the stretch it was placed on runs past it
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

    // The path's start stated on the stretch's own lap: the first one at or after the stretch
    // opens.
    //
    // The start is at zero, but a stretch fused across it runs past the perimeter, so the start
    // such a stretch holds is the one a lap on. Measured back to zero instead, the start would
    // sit behind every fused stretch rather than within it - and a stretch crossing the origin
    // is exactly the one whose layout wants placing against it.
    private double alignStartWithStretch(double startArcLength) {
        return Math.ceil(startArcLength / getPerimeter()) * getPerimeter();
    }

    // Which end of the stretch a layout goes to when the path's start lies off the stretch
    // entirely. The two candidate starts are the stretch's own start and the latest start it
    // allows, and the layout takes whichever of them the path's start is nearer to going round.
    //
    // Measured to those two starts rather than to the stretch's two ends, because nearness is
    // nearness of the layout's start: a stretch closing just behind the path's start can still
    // hold a long layout reaching far back round the ring, and aligning to that end would throw
    // the layout's opening to the far side of the path for the sake of a sliver of cover.
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

}
