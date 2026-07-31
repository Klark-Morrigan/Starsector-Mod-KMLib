package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.Arrays;
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
 *
 * <p>The hygiene is stated once too, as indices rather than as a cleaned ring
 * ({@link #findSurvivingVertices}). A ring often travels with a value per edge -
 * a label, an offset distance - and dropping a vertex without dropping the right
 * one of those slides the two out of step. Handing back which vertex each survivor
 * takes its position and its outgoing edge from lets every caller carry its own
 * payload through the same judgement, instead of each restating the walk around
 * the payload type it happens to have.
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
        return collectPointsAt(polygon, findSurvivingVertices(polygon)
            .pointIndices());
    }

    // As removeConsecutiveDuplicates(List) for a ring whose edges carry labels, so a
    // caller need not choose between the hygiene and the labels.
    static LabelledPolygon removeConsecutiveDuplicates(LabelledPolygon polygon) {
        var vertices = polygon.getVertices();
        var labels = polygon.getEdgeLabels();
        var survivors = findSurvivingVertices(vertices);
        var outgoingEdges = survivors.outgoingEdgeIndices();
        var cleanedLabels = new int[outgoingEdges.length];
        for (var i = 0; i < outgoingEdges.length; i++) {
            cleanedLabels[i] = labels[outgoingEdges[i]];
        }
        return LabelledPolygon.fromLabelledEdges(
            collectPointsAt(vertices, survivors.pointIndices()), cleanedLabels);
    }

    // Which of a ring's vertices survive the dedup, and where each of the two halves
    // of a survivor comes from - the whole of the hygiene, with the caller left to
    // gather its own values through it.
    //
    // A run of coincident vertices is one corner the ring records several times, and
    // the two halves of that corner come from opposite ends of the run. Its position
    // is the first vertex's, so the answer does not drift by a rounding whisker down
    // the run. The edge that really leaves it is the last vertex's outgoing edge -
    // every earlier one in the run is the zero-length step to the next duplicate,
    // which names nothing - so that is where a per-edge value must be taken from.
    static SurvivingVertices findSurvivingVertices(List<double[]> polygon) {
        var pointIndices = new int[polygon.size()];
        var outgoingEdgeIndices = new int[polygon.size()];
        var count = 0;
        for (var i = 0; i < polygon.size(); i++) {
            if (count > 0 && isSamePoint(polygon.get(pointIndices[count - 1]), polygon.get(i))) {
                outgoingEdgeIndices[count - 1] = i;
            } else {
                pointIndices[count] = i;
                outgoingEdgeIndices[count] = i;
                count++;
            }
        }
        // The ring wraps onto its own first corner, so the last survivor is that
        // corner recorded once more. It goes, and with it the zero-length edge closing
        // the ring - the edge reaching it is a real one, and keeps what it carries.
        var isLastSurvivorDuplicateOfFirst = count > 1
            && isSamePoint(
                polygon.get(pointIndices[0]),
                polygon.get(pointIndices[count - 1]));
        if (isLastSurvivorDuplicateOfFirst) {
            count--;
        }
        return new SurvivingVertices(
            Arrays.copyOf(pointIndices, count),
            Arrays.copyOf(outgoingEdgeIndices, count));
    }

    // The points of {@code polygon} at {@code indices}, in that order.
    static List<double[]> collectPointsAt(List<double[]> polygon, int[] indices) {
        var points = new ArrayList<double[]>(indices.length);
        for (var index : indices) {
            points.add(polygon.get(index));
        }
        return points;
    }

    // Whether two points sit within the minimum edge length of each other, so the
    // dedup treats them as one vertex.
    static boolean isSamePoint(double[] a, double[] b) {
        return Points.computeDistance(a, b) < Limits.MIN_EDGE_LENGTH;
    }

    /**
     * The vertices of a ring that survive the dedup, as indices into the ring they
     * came from. Entry {@code i} of each array describes survivor {@code i}, in
     * winding order.
     *
     * @param pointIndices        the vertex each survivor takes its position from
     * @param outgoingEdgeIndices the vertex each survivor takes its outgoing edge
     *                            from - and so the entry to read a per-edge value at
     */
    record SurvivingVertices(int[] pointIndices, int[] outgoingEdgeIndices) {
    }
}
