package kmlib.math.geometry;

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
public record Segment(double startX, double startY, double endX, double endY) {

    // The point on segment start..end where a value that is signedStart at start
    // and signedEnd at end crosses zero, at parameter signedStart / (signedStart -
    // signedEnd) along the segment - bounded interpolation between the endpoints,
    // not an infinite-line intersection. The single home for the half-plane clip
    // crossing computation behind LabelledPolygon.clipToHalfPlane. The two signs
    // must straddle zero (opposite signs) - each clip establishes that before
    // asking, so the denominator is never zero. Takes the endpoints loose rather
    // than a Segment instance so the clip walk, which already holds them as
    // {x, y} arrays, need not allocate a Segment per edge.
    static double[] computeCrossingPoint(
            double[] start, double[] end,
            double signedStart,
            double signedEnd) {
        var fraction = signedStart / (signedStart - signedEnd);
        return new double[] {
            start[0] + fraction * (end[0] - start[0]),
            start[1] + fraction * (end[1] - start[1]),
        };
    }
}
