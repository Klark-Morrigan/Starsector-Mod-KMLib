package kmlib.math.geometry;

import org.lwjgl.util.vector.Vector2f;

/**
 * Operations on 2D points.
 */
public final class Points {

    private Points() {
    }

    /**
     * The Euclidean distance between {@code (x1, y1)} and {@code (x2, y2)} - the
     * length of the vector between them.
     */
    public static double computeDistance(double x1, double y1, double x2, double y2) {
        return computeVectorLength(x1 - x2, y1 - y2);
    }

    /**
     * The Euclidean distance between two {@code {x, y}} points.
     */
    public static double computeDistance(double[] a, double[] b) {
        return computeDistance(a[0], a[1], b[0], b[1]);
    }

    /**
     * The Euclidean distance between two points {@code a} and {@code b}.
     */
    public static double computeDistance(Vector2f a, Vector2f b) {
        return computeDistance(a.x, a.y, b.x, b.y);
    }

    /**
     * The Euclidean length (magnitude) of the 2D vector {@code (x, y)}:
     * {@code sqrt(x^2 + y^2)}. The single home for the sum-of-squares root behind
     * {@link #computeDistance} (the length of the difference vector) and the vector
     * normalisations elsewhere, so callers that already hold the components never
     * recompute the magnitude.
     */
    public static double computeVectorLength(double x, double y) {
        return Math.sqrt(x * x + y * y);
    }

    /**
     * The bearing in degrees from {@code (x1, y1)} to {@code (x2, y2)},
     * measured counter-clockwise from the positive x-axis, in the range
     * {@code (-180, 180]}. Coincident points yield 0.
     */
    public static double computeAngleDegrees(double x1, double y1, double x2, double y2) {
        return Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
    }

    // The signed offset of {@code point} from the line through (lineX, lineY) with
    // direction normal (normalX, normalY): (point - lineOrigin) projected onto the
    // normal. Positive on the side the normal points to, zero on the line, negative
    // on the far side. It equals the signed perpendicular distance only when the
    // normal is unit length; otherwise the magnitude scales with |normal|. Callers
    // therefore rely on the sign (which side) and on ratios of two offsets (where
    // the shared scale cancels), never on the raw magnitude - the half-plane test
    // both polygon clips key their keep/discard decision and crossing point on.
    static double computeSignedOffsetFromLine(double[] point,
            double lineX, double lineY, double normalX, double normalY) {
        return (point[0] - lineX) * normalX + (point[1] - lineY) * normalY;
    }

    // The point on segment a..b where a value that is signedA at a and signedB at
    // b crosses zero, at parameter signedA / (signedA - signedB) along the
    // segment. The single home for the half-plane clip crossing computation,
    // shared by Polygons.clipToHalfPlane and LabelledPolygon.clipToHalfPlane. The
    // two signs must straddle zero (opposite signs) - each clip establishes that
    // before asking, so the denominator is never zero.
    static double[] computeCrossingPoint(double[] a, double[] b, double signedA, double signedB) {
        var fraction = signedA / (signedA - signedB);
        return new double[] {
                a[0] + fraction * (b[0] - a[0]),
                a[1] + fraction * (b[1] - a[1]),
        };
    }

    /**
     * The bearing in degrees from {@code a} to {@code b}, measured
     * counter-clockwise from the positive x-axis, in the range
     * {@code (-180, 180]}. Coincident points yield 0.
     */
    public static double computeAngleDegrees(Vector2f a, Vector2f b) {
        return computeAngleDegrees(a.x, a.y, b.x, b.y);
    }
}
