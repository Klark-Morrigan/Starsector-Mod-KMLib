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
