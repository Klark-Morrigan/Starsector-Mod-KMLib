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
