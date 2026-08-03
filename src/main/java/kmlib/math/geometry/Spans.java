package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Operations on parameter intervals along a directed line - spans given as
 * {@code {tStart, tEnd}} pairs of distances from a through-point.
 *
 * <p>The interval-arithmetic complement to {@link PolygonRegions#findLineInteriorSpans},
 * which produces such spans: once a line's in-region pieces are known, the next
 * question is which piece stays usable after point obstacles carve their keep-out
 * intervals from it. Working in parameters rather than endpoints keeps the
 * subtraction one-dimensional; a caller maps the winning interval back to points
 * with {@code through + t * direction}.
 *
 * <p>The obstacle side of that subtraction is available on its own as
 * {@link LineBlockers}, so a caller re-testing one line against many span lists
 * projects the obstacles once instead of once per test.
 */
public final class Spans {

    private Spans() {
    }

    /**
     * The keep-out intervals the obstacles carve from the line, as the
     * {@link LineBlockers} any span list measured along that line can be trimmed by -
     * the obstacle-only half of {@link #findLongestClearSubsegment}, separated so it
     * can be computed once for a line and reused across every span list tested
     * against it.
     *
     * <p>For each obstacle within {@code clearance} of the line, the blocked interval
     * is the chord its keep-out circle cuts: centred on the obstacle's projection onto
     * the line, with half-width {@code sqrt(clearance^2 - perp^2)} where {@code perp}
     * is the obstacle's perpendicular distance. An obstacle farther than
     * {@code clearance} from the line misses it entirely and blocks nothing.
     *
     * @param line      the line the obstacles are projected onto - its origin is
     *                  parameter zero, and its direction is normalised here so a
     *                  parameter reads as a world distance
     * @param obstacles the {x, y} points to keep clear of
     * @param clearance the keep-out radius around each obstacle; non-positive blocks
     *                  nothing, so the intervals come back empty
     * @return the blocked intervals, ascending by start; {@code null} when the
     *         direction is too short to define a line
     */
    public static LineBlockers computeLineBlockers(
            DirectedLine line,
            Collection<double[]> obstacles,
            double clearance) {

        var direction = Points.computeUnitVector(
            line.directionX(),
            line.directionY(),
            Limits.MIN_EDGE_LENGTH);

        if (direction == null) {
            return null;
        }
        var unitLine = new DirectedLine(
            line.originX(),
            line.originY(),
            direction[0],
            direction[1]);

        var blocked = computeBlockedIntervals(unitLine, obstacles, clearance);

        // Sorted by start, the blockers can be walked once per span with a single
        // advancing cursor instead of re-scanning the whole set per gap.
        blocked.sort(Comparator.comparingDouble(interval -> interval[0]));
        return new LineBlockers(blocked);
    }

    /**
     * The longest sub-interval of {@code spans} that keeps every obstacle at least
     * {@code clearance} away, as a {@code {tStart, tEnd}} pair - or {@code null}
     * when nothing clear survives.
     *
     * <p>Each obstacle within {@code clearance} of the line blocks the interval the
     * line spends inside its keep-out circle: centred on the obstacle's projection
     * onto the line, with half-width {@code sqrt(clearance^2 - perp^2)} where
     * {@code perp} is the obstacle's perpendicular distance - the chord the circle
     * cuts from the line. An obstacle farther than {@code clearance} from the line
     * misses it entirely and blocks nothing. The blocked intervals are subtracted
     * from every span and the single longest surviving piece wins, so the result is
     * the roomiest stretch of line that stays clear of every obstacle.
     *
     * @param spans     the candidate intervals as {@code {tStart, tEnd}} pairs
     *                  (parameters along the line's direction), each internally ascending
     * @param line      the line the spans are measured along - its origin is parameter zero
     *                  and its direction sets the parameter scale
     * @param obstacles the {x, y} points to keep clear of
     * @param clearance the keep-out radius around each obstacle; non-positive
     *                  blocks nothing, so the longest input span wins whole
     * @return the longest clear {@code {tStart, tEnd}} interval, or {@code null}
     *         when the spans are empty, fully blocked, or the direction is too
     *         short to define a line
     */
    public static double[] findLongestClearSubsegment(
            List<double[]> spans,
            DirectedLine line,
            Collection<double[]> obstacles,
            double clearance) {

        var blockers = computeLineBlockers(line, obstacles, clearance);
        return blockers == null
            ? null
            : findLongestClearSubsegment(spans, blockers);
    }

    /**
     * The longest sub-interval of {@code spans} left clear by already-projected
     * {@code blockers}, as a {@code {tStart, tEnd}} pair - or {@code null} when
     * nothing clear survives.
     *
     * <p>The form to take when one line is tested repeatedly: the blockers depend on
     * neither the spans nor how often they are asked for, so projecting them once and
     * passing them here does the same subtraction without re-scanning the obstacles.
     * The spans must be measured along the same line the blockers were computed for,
     * since both are parameters in that line's frame.
     *
     * @param spans    the candidate intervals as {@code {tStart, tEnd}} pairs, each
     *                 internally ascending
     * @param blockers the keep-out intervals along the same line, from
     *                 {@link #computeLineBlockers}
     * @return the longest clear {@code {tStart, tEnd}} interval, or {@code null} when
     *         the spans are empty or fully blocked
     */
    public static double[] findLongestClearSubsegment(
            List<double[]> spans,
            LineBlockers blockers) {

        double[] longest = null;
        for (var span : spans) {
            longest = pickLonger(longest, findLongestGapWithinSpan(span, blockers.intervals()));
        }
        return longest;
    }

    /**
     * The longest of {@code spans}, as its {@code {tStart, tEnd}} pair - or
     * {@code null} when {@code spans} is empty or every span is degenerate
     * (non-positive length).
     *
     * <p>The obstacle-free counterpart to {@link #findLongestClearSubsegment}:
     * where that trims each span by the obstacles' keep-out intervals, this takes
     * the roomiest span outright - the fallback a caller shows when the
     * clearance-aware fit leaves nothing usable.
     *
     * @param spans candidate intervals as {@code {tStart, tEnd}} pairs, each
     *              internally ascending
     * @return the longest span, or {@code null} when none has positive length
     */
    public static double[] findLongestSpan(List<double[]> spans) {
        double[] longest = null;
        for (var span : spans) {
            longest = pickLonger(longest, span);
        }
        return longest;
    }

    /**
     * The overlap of two span lists, as ascending disjoint {@code {tStart, tEnd}}
     * pairs - the parameter intervals covered by both {@code a} and {@code b}.
     *
     * <p>The band counterpart to the single-line interior test: a band of nonzero
     * thickness is interior only where every one of its parallel rails is, so
     * {@link PolygonRegions#findBandInteriorSpans} intersects the rails' spans through
     * this. Because shifting a rail's through-point perpendicular to the direction
     * leaves the along-direction origin unchanged, every rail's parameters share one
     * frame, so the intersection is a plain interval overlap. Walked with two
     * cursors over the sorted inputs, advancing past whichever interval ends first.
     *
     * @param a one span list, ascending and disjoint (as
     *          {@link PolygonRegions#findLineInteriorSpans} returns)
     * @param b the other span list, ascending and disjoint
     * @return the intervals covered by both, ascending and disjoint; empty when they
     *         nowhere overlap
     */
    public static List<double[]> intersectSpans(List<double[]> a, List<double[]> b) {
        var overlaps = new ArrayList<double[]>();
        var i = 0;
        var j = 0;
        while (i < a.size() && j < b.size()) {
            var lo = Math.max(a.get(i)[0], b.get(j)[0]);
            var hi = Math.min(a.get(i)[1], b.get(j)[1]);
            // A zero- or negative-length overlap (the intervals only touch, or miss)
            // is no span; the shared length must clear the same floor a real edge does.
            if (hi - lo > Limits.MIN_EDGE_LENGTH) {
                overlaps.add(new double[] {lo, hi});
            }
            // Advance past whichever interval ends first - the other may still overlap
            // the next one along.
            if (a.get(i)[1] < b.get(j)[1]) {
                i++;
            } else {
                j++;
            }
        }
        return overlaps;
    }

    // The keep-out intervals the obstacles carve from the line (its direction assumed
    // unit, so a parameter is a world distance): for each obstacle near enough to matter,
    // the chord its clearance circle cuts from the line, centred on the obstacle's
    // projection. In parameter space each is one closed interval, ready to subtract from
    // the candidate spans.
    private static List<double[]> computeBlockedIntervals(
            DirectedLine line,
            Collection<double[]> obstacles,
            double clearance) {
        var blocked = new ArrayList<double[]>();
        if (clearance <= 0) {
            return blocked;
        }
        for (var obstacle : obstacles) {
            var offsetX = obstacle[0] - line.originX();
            var offsetY = obstacle[1] - line.originY();
            var along = offsetX * line.directionX() + offsetY * line.directionY();

            // Perpendicular distance to the line: the offset projected onto the line's
            // unit normal (perpendicular to its direction).
            var perpendicular = Math.abs(offsetX * -line.directionY() + offsetY * line.directionX());
            if (perpendicular >= clearance) {
                continue;
            }
            var halfWidth = Math.sqrt(clearance * clearance - perpendicular * perpendicular);
            blocked.add(new double[] {along - halfWidth, along + halfWidth});
        }
        return blocked;
    }

    // The longest stretch of one span not covered by any blocked interval: a cursor
    // sweeps the span, jumping over each blocker it meets, and every gap between
    // the cursor and the next blocker (or the span's end) is a candidate.
    private static double[] findLongestGapWithinSpan(double[] span, List<double[]> blocked) {
        double[] longest = null;
        var cursor = span[0];
        for (var interval : blocked) {
            if (cursor > span[1]) {
                break;
            }
            // A blocker wholly before the cursor (or the span) constrains nothing.
            if (interval[1] <= cursor) {
                continue;
            }
            longest = pickLonger(
                longest,
                new double[] {cursor, Math.min(interval[0], span[1])});
            cursor = Math.max(cursor, interval[1]);
        }
        if (cursor < span[1]) {
            longest = pickLonger(longest, new double[] {cursor, span[1]});
        }
        return longest;
    }

    // The longer of two candidate intervals; a null or empty (non-positive length)
    // candidate never wins, and two nulls stay null.
    private static double[] pickLonger(double[] current, double[] candidate) {
        if (candidate == null || candidate[1] - candidate[0] <= 0) {
            return current;
        }
        if (current == null || candidate[1] - candidate[0] > current[1] - current[0]) {
            return candidate;
        }
        return current;
    }
}
