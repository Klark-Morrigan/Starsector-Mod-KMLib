package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Ring vertex hygiene shared by the polygon reshaping passes.
 *
 * <p>Both {@link PolygonOffsets} and {@link PolygonSmoothing} clean a ring's
 * vertex loop before working it, so the offset and corner math never meets a
 * zero-length edge with an undefined direction. That cleaning is one concept -
 * dropping coincident vertices - so it lives here once rather than in each
 * reshaping class, kept package-private since only they need it.
 */
final class Rings {

    private Rings() {
    }

    // Drops vertices that coincide with their predecessor (within the minimum
    // edge length), including the wrap from last back to first, so the reshaping
    // math never sees a zero-length edge with an undefined direction.
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
