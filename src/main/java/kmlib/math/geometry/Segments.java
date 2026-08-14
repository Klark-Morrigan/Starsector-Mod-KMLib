package kmlib.math.geometry;

/**
 * Operations on finite segments - each bounded by its two endpoints, unlike the infinite
 * lines {@link Lines} works on.
 *
 * <p>That bound is the whole distinction, and it is the one that decides which of the two a
 * caller wants. A miter join asks where two edges' lines would meet and accepts an answer far
 * past either edge; a self-intersection asks whether two edges actually touch, and a point
 * the lines share beyond both of them is not an answer to that. The same split runs through
 * the distance measures: to the infinite line, or to the nearest place on the span.
 *
 * <p>Separate from {@link Segment} because that is the value - two endpoints named by role -
 * and this is what can be asked about a pair of them, the same way {@code Rectangles} stands
 * beside {@code Rectangle}. These take endpoints loose as {@code {x, y}} arrays rather than
 * {@code Segment} instances, because every caller is a walk over a ring that already holds
 * its vertices that way and would otherwise allocate one per edge.
 */
public final class Segments {

    private Segments() {
    }

    /**
     * Where two segments cross, or null when they do not.
     *
     * <p>The bounded sibling of an infinite-line intersection, which answers where two
     * lines would meet even for spans that pass nowhere near each other. This asks whether
     * the two spans actually touch - the question a self-intersection asks, since a ring
     * folds over itself only where real edges cross and not where their lines would if
     * extended.
     *
     * <p>Two segments meeting at a shared endpoint count as crossing, because they do. A
     * caller walking a ring, whose consecutive edges always touch that way, has to exclude
     * its own neighbours rather than expect this to.
     *
     * <p>Two that lie along the same line and overlap report nothing, which is the one case
     * where null does not mean "these do not touch". They touch along a whole span, and a
     * span has no single crossing point to return. A caller that has to tell an overlap from
     * a miss cannot learn it here.
     *
     * @param firstFrom  where the first segment starts
     * @param firstTo    where the first segment ends
     * @param secondFrom where the second starts
     * @param secondTo   where the second ends
     * @return the crossing point, or null when the spans miss each other or run parallel
     */
    public static double[] intersectSegments(
            double[] firstFrom,
            double[] firstTo,
            double[] secondFrom,
            double[] secondTo) {

        var firstX = firstTo[0] - firstFrom[0];
        var firstY = firstTo[1] - firstFrom[1];

        var secondX = secondTo[0] - secondFrom[0];
        var secondY = secondTo[1] - secondFrom[1];

        var cross = firstX * secondY - firstY * secondX;

        if (Math.abs(cross) < Limits.MIN_EDGE_LENGTH) {
            return null;
        }

        var offsetX = secondFrom[0] - firstFrom[0];
        var offsetY = secondFrom[1] - firstFrom[1];

        var alongFirst = (offsetX * secondY - offsetY * secondX) / cross;
        var alongSecond = (offsetX * firstY - offsetY * firstX) / cross;

        if (alongFirst < 0 || alongFirst > 1 || alongSecond < 0 || alongSecond > 1) {
            return null;
        }

        return new double[] {
            firstFrom[0] + firstX * alongFirst,
            firstFrom[1] + firstY * alongFirst};
    }

    /**
     * How far a point lies from the segment start..end.
     *
     * <p>From the segment itself, not from the infinite line through it, so a point off
     * past either end measures to that end rather than to a foot of perpendicular the
     * segment never reaches. That bound is the whole difference from a perpendicular
     * distance, and it is what "how close is this to that piece of boundary" means: a point
     * beyond the end of one edge is near whatever edge comes next, not near this one
     * extended.
     *
     * @param start the segment's start
     * @param end   its end; a segment too short to have a direction measures to the point
     *              it sits at
     * @param point the point to measure
     * @return the distance from the point to the nearest place on the segment
     */
    public static double computeDistanceToPoint(double[] start, double[] end, double[] point) {

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
