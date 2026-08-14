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
public record Segment(
    double startX,
    double startY,
    double endX,
    double endY) {
}
