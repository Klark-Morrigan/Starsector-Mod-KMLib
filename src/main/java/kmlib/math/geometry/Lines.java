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
    static double[] intersectLines(double[] pointA, double dirAX, double dirAY,
            double[] pointB, double dirBX, double dirBY) {
        var cross = dirAX * dirBY - dirAY * dirBX;
        if (Math.abs(cross) < Limits.MIN_EDGE_LENGTH) {
            return null;
        }
        var fraction = ((pointB[0] - pointA[0]) * dirBY - (pointB[1] - pointA[1]) * dirBX) / cross;
        return new double[] {pointA[0] + fraction * dirAX, pointA[1] + fraction * dirAY};
    }

    // The signed offset of {@code point} from the line through (lineX, lineY) with
    // direction normal (normalX, normalY): (point - lineOrigin) projected onto the
    // normal. Positive on the side the normal points to, zero on the line, negative
    // on the far side. It equals the signed perpendicular distance only when the
    // normal is unit length; otherwise the magnitude scales with |normal|. Callers
    // therefore rely on the sign (which side) and on ratios of two offsets (where
    // the shared scale cancels), never on the raw magnitude - the half-plane test
    // the polygon clip keys its keep/discard decision and crossing point on.
    static double computeSignedOffsetFromLine(double[] point,
            double lineX, double lineY, double normalX, double normalY) {
        return (point[0] - lineX) * normalX + (point[1] - lineY) * normalY;
    }
}
