package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Operations on parameter intervals along a directed line - spans given as
 * {@code {tStart, tEnd}} pairs of distances from a through-point.
 *
 * <p>The interval-arithmetic complement to {@link Polygons#findLineInteriorSpans},
 * which produces such spans: once a line's in-region pieces are known, the next
 * question is which piece stays usable after point obstacles carve their keep-out
 * intervals from it. Working in parameters rather than endpoints keeps the
 * subtraction one-dimensional; a caller maps the winning interval back to points
 * with {@code through + t * direction}.
 */
public final class Spans {

    private Spans() {
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
     *                  (parameters along the direction), each internally ascending
     * @param throughX  x of the point the line passes through (parameter zero)
     * @param throughY  y of the point the line passes through (parameter zero)
     * @param dirX      x of the line's direction
     * @param dirY      y of the line's direction
     * @param obstacles the {x, y} points to keep clear of
     * @param clearance the keep-out radius around each obstacle; non-positive
     *                  blocks nothing, so the longest input span wins whole
     * @return the longest clear {@code {tStart, tEnd}} interval, or {@code null}
     *         when the spans are empty, fully blocked, or the direction is too
     *         short to define a line
     */
    public static double[] findLongestClearSubsegment(List<double[]> spans,
            double throughX, double throughY, double dirX, double dirY,
            Collection<double[]> obstacles, double clearance) {
        var direction = Points.computeUnitVector(dirX, dirY, Limits.MIN_EDGE_LENGTH);
        if (direction == null) {
            return null;
        }
        var blocked = computeBlockedIntervals(throughX, throughY, direction[0], direction[1],
                obstacles, clearance);
        // Sorted by start, the blockers can be walked once per span with a single
        // advancing cursor instead of re-scanning the whole set per gap.
        blocked.sort(Comparator.comparingDouble(interval -> interval[0]));
        double[] longest = null;
        for (var span : spans) {
            longest = pickLonger(longest, findLongestGapWithinSpan(span, blocked));
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

    // The keep-out intervals the obstacles carve from the line: for each obstacle
    // near enough to matter, the chord its clearance circle cuts from the line,
    // centred on the obstacle's projection. In parameter space each is one closed
    // interval, ready to subtract from the candidate spans.
    private static List<double[]> computeBlockedIntervals(double throughX, double throughY,
            double dirX, double dirY, Collection<double[]> obstacles, double clearance) {
        var blocked = new ArrayList<double[]>();
        if (clearance <= 0) {
            return blocked;
        }
        for (var obstacle : obstacles) {
            var offsetX = obstacle[0] - throughX;
            var offsetY = obstacle[1] - throughY;
            var along = offsetX * dirX + offsetY * dirY;
            // Perpendicular distance to the line: the offset projected onto the
            // line's unit normal (-dirY, dirX).
            var perpendicular = Math.abs(offsetX * -dirY + offsetY * dirX);
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
            longest = pickLonger(longest,
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
