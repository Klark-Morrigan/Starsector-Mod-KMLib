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
     * {@code sqrt(x^2 + y^2)}. The single home for the magnitude behind
     * {@link #computeDistance} (the length of the difference vector) and the vector
     * normalisations elsewhere, so callers that already hold the components never
     * recompute it.
     *
     * <p>Evaluated through {@link Math#hypot} rather than a literal
     * {@code sqrt(x*x + y*y)} so an extreme component cannot square past the double
     * range and drag the result to infinity (or underflow it to zero) when the true
     * magnitude is finite - {@code hypot} scales the inputs first to stay in range.
     */
    public static double computeVectorLength(double x, double y) {
        return Math.hypot(x, y);
    }

    /**
     * The unit vector along {@code (x, y)} - each component divided by the vector's
     * length - or {@code null} when that length is below {@code minLength} and the
     * direction is therefore ill-defined. The caller sets the degeneracy floor for
     * its own coordinate scale and decides what a {@code null} means: skip the input,
     * or substitute a fallback direction. Centralises the "measure, guard the
     * near-zero magnitude, then divide" step shared by the callers that need a
     * direction out of a raw {x, y}.
     *
     * @param x         the vector's x component
     * @param y         the vector's y component
     * @param minLength the length below which the vector counts as having no
     *                  direction; sized to the caller's coordinate scale
     * @return the unit components as {@code {x, y}}, or {@code null} when the vector
     *         is shorter than {@code minLength}
     */
    static double[] computeUnitVector(double x, double y, double minLength) {
        var length = computeVectorLength(x, y);
        if (length < minLength) {
            return null;
        }
        return new double[] {x / length, y / length};
    }

    /**
     * The bearing in degrees from {@code (x1, y1)} to {@code (x2, y2)},
     * measured counter-clockwise from the positive x-axis, in the range
     * {@code (-180, 180]}. Coincident points yield 0.
     */
    public static double computeAngleDegrees(double x1, double y1, double x2, double y2) {
        return Math.toDegrees(Math.atan2(y2 - y1, x2 - x1));
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
