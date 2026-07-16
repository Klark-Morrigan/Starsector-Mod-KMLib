package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * When two of a ring's vertices are the same point, and the hygiene that follows
 * from the answer.
 *
 * <p>Ring vertices arrive from clips and intersections, so a corner two routines
 * computed separately lands at coordinates differing by a rounding whisker: exact
 * equality is never the question, and a tolerance decides it. That makes
 * coincidence a judgement rather than a fact, and one every operation on a ring
 * must reach the same way - a corner counted as two vertices by one and as one by
 * another is a shape the two disagree about. So the judgement lives here once,
 * with the hygiene it licenses.
 */
final class Rings {

    private Rings() {
    }

    // Drops vertices that coincide with their predecessor, including the wrap from
    // the last back to the first, so no edge of the returned ring has zero length.
    //
    // Such an edge has no direction, and so no normal and no angle to its
    // neighbours, while contributing no length or area - it is a vertex the ring
    // records twice, not a side of the shape. Dropping it leaves the same region
    // bounded by edges that all have a direction.
    static List<double[]> removeConsecutiveDuplicates(List<double[]> polygon) {
        var cleaned = new ArrayList<double[]>();
        for (var vertex : polygon) {
            if (cleaned.isEmpty() || !isSamePoint(cleaned.get(cleaned.size() - 1), vertex)) {
                cleaned.add(vertex);
            }
        }
        var size = cleaned.size();
        if (size > 1 && isSamePoint(cleaned.get(0), cleaned.get(size - 1))) {
            cleaned.remove(size - 1);
        }
        return cleaned;
    }

    // Whether two points sit within the minimum edge length of each other, so the
    // dedup treats them as one vertex.
    static boolean isSamePoint(double[] a, double[] b) {
        return Points.computeDistance(a, b) < Limits.MIN_EDGE_LENGTH;
    }
}
