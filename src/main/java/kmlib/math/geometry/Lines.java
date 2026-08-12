package kmlib.math.geometry;

/**
 * Operations on infinite 2D lines - each given as a point and a direction (or
 * normal) that extends both ways without end.
 *
 * <p>The home for the line math the polygon passes lean on where the answer can
 * lie anywhere on the infinite line, not just between two endpoints: the miter
 * join and arc centre take a crossing that can sit far past the edges that
 * spawned it, and the half-plane clip only cares which side of a line a point
 * falls on. That unbounded character is what separates these from {@link
 * Segment}, whose operations stay within its two endpoints.
 */
final class Lines {

    private Lines() {
    }

    // Intersection of the line through {@code pointA} with direction
    // {@code (dirAX, dirAY)} and the line through {@code pointB} with direction
    // {@code (dirBX, dirBY)}; null when the two directions are parallel and the lines
    // never cross. The shared "where do these two lines meet" step behind the miter
    // join and the arc centre.
    static double[] intersectLines(
            double[] pointA,
            double dirAX,
            double dirAY,
            double[] pointB,
            double dirBX,
            double dirBY) {

        var cross = dirAX * dirBY - dirAY * dirBX;

        if (Math.abs(cross) < Limits.MIN_EDGE_LENGTH) {
            return null;
        }

        var fraction = ((pointB[0] - pointA[0]) * dirBY - (pointB[1] - pointA[1]) * dirBX) / cross;

        return new double[] {
            pointA[0] + fraction * dirAX,
            pointA[1] + fraction * dirAY};
    }

    // Where two SEGMENTS cross, or null when they do not. The bounded sibling of
    // {@link #intersectLines}: that one asks where two infinite lines meet, which is the
    // question a miter join asks, and answers it even for edges that pass nowhere near
    // each other. This asks whether the two spans actually touch, which is the question a
    // self-intersection asks - a ring folds over itself only where real edges cross, not
    // where their lines would if extended.
    static double[] intersectSegments(
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

    // Perpendicular distance from {@code point} to the infinite line through
    // {@code lineA} and {@code lineB} - the height a corner rises above the chord
    // joining its neighbours. Twice the triangle's signed area (the cross product)
    // over the base length is that height. Falls back to the distance to
    // {@code lineA} when the two line points coincide and give no direction.
    static double computePerpendicularDistance(double[] point, double[] lineA, double[] lineB) {

        var baseX = lineB[0] - lineA[0];
        var baseY = lineB[1] - lineA[1];
        var baseLength = Points.computeVectorLength(baseX, baseY);

        if (baseLength < Limits.MIN_EDGE_LENGTH) {
            return Points.computeDistance(point, lineA);
        }

        var twiceArea = (point[0] - lineA[0]) * baseY
            - (point[1] - lineA[1]) * baseX;

        return Math.abs(twiceArea) / baseLength;
    }

    // The signed offset of {@code point} from {@code boundary}'s line: (point - boundary
    // origin) projected onto the boundary normal. Positive on the side the normal points
    // to, zero on the line, negative on the far side. It equals the signed perpendicular
    // distance only when the normal is unit length; otherwise the magnitude scales with
    // |normal|. Callers therefore rely on the sign (which side) and on ratios of two
    // offsets (where the shared scale cancels), never on the raw magnitude - the
    // half-plane test the polygon clip keys its keep/discard decision and crossing point on.
    static double computeSignedOffsetFromLine(double[] point, HalfPlane boundary) {
        
        return Points.projectPointOnto(
            point[0] - boundary.pointX(),
            point[1] - boundary.pointY(),
            boundary.normalX(),
            boundary.normalY());
    }
}
