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

    // How far {@code point} lies from the segment start..end - from the segment itself,
    // not from the infinite line through it, so a point off past either end measures to
    // that end rather than to a foot of perpendicular the segment never reaches. That
    // bound is the whole difference from Lines.computePerpendicularDistance, and it is
    // what "how close is this to that piece of boundary" means: a point beyond the end of
    // one edge is near whatever edge comes next, not near this one extended. A segment too
    // short to have a direction measures to the point it sits at.
    static double computeDistanceToPoint(double[] start, double[] end, double[] point) {

        var spanX = end[0] - start[0];
        var spanY = end[1] - start[1];
        var spanLength = Points.computeVectorLength(spanX, spanY);

        // Where the perpendicular from the point lands, as a fraction of the segment,
        // clamped into it so an overshoot at either end measures to that end instead.
        var alongSegment = spanLength < Limits.MIN_EDGE_LENGTH
            ? 0.0
            : Math.max(0.0, Math.min(1.0,
                Points.projectPointOnto(
                    point[0] - start[0],
                    point[1] - start[1],
                    spanX,
                    spanY) / (spanLength * spanLength)));

        return Points.computeDistance(
            point,
            new double[] {
                start[0] + alongSegment * spanX,
                start[1] + alongSegment * spanY});
    }
}
