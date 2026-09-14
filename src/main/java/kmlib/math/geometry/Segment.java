package kmlib.math.geometry;

import java.util.List;

/**
 * A directed line segment as its two endpoints, named by role so geometry reads
 * {@code start}/{@code end} rather than indexing a four-double array.
 *
 * <p>The shared currency between the edge-offset pass that emits one segment per
 * polygon edge and the chainer that welds a bag of segments back into closed
 * rings: both speak in whole segments, so neither side has to agree on which
 * array slot holds which coordinate. The direction (start to end) is meaningful -
 * a caller tracing a region orients every segment so the interior lies
 * consistently to one side, and the chainer follows each segment's end to the
 * next segment's start.
 */
public record Segment(
    double startX,
    double startY,
    double endX,
    double endY) {

    /**
     * The four corners of the band this segment is the centreline of - the rectangle
     * {@code thickness} wide, half of it either side, running the segment's whole length.
     *
     * <p>Asked of the segment rather than worked out by each caller, because more than one
     * has to agree on it: a band drawn from a centreline and a girth, and anything else
     * reasoning about the room that band takes up, describe the same rectangle, and two
     * derivations of it would let the drawn one and the reserved one part company.
     *
     * <p>The corners come back as a closed ring's vertices (no repeated closing point), so
     * the result drops straight into the polygon operations here.
     *
     * @param thickness the band's full girth across the centreline
     * @return the corners in ring order: both ends of one long edge, then both ends of the
     *         other, walked back. Empty when the segment is too short to take a direction
     *         from, or the girth too small to enclose area - in either case there is no
     *         rectangle to state
     */
    public List<double[]> computeBandCorners(double thickness) {

        var unit = Points.computeUnitVector(
            endX - startX,
            endY - startY,
            Limits.MIN_EDGE_LENGTH);

        if (unit == null || thickness < Limits.MIN_EDGE_LENGTH) {
            return List.of();
        }

        // Half the girth along the centreline's perpendicular - its direction turned a
        // quarter turn - which is the offset from the centreline to each long edge.
        var halfX = -unit[1] * thickness / 2;
        var halfY = unit[0] * thickness / 2;

        return List.of(
            new double[] {startX + halfX, startY + halfY},
            new double[] {endX + halfX, endY + halfY},
            new double[] {endX - halfX, endY - halfY},
            new double[] {startX - halfX, startY - halfY});
    }
}
