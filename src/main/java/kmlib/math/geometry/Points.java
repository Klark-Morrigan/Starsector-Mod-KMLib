package kmlib.math.geometry;

import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Operations on 2D points.
 */
public final class Points {

    private Points() {
    }

    /**
     * The mean position of a point cloud, as {@code {x, y}} - each coordinate averaged
     * over every point.
     *
     * <p>The cloud's balance point, which is what a caller wants when it needs one point
     * to stand for the whole set: the anchor a label or marker hangs on, or a probe into
     * a shape that must land somewhere unambiguously within it rather than near an edge.
     * An unweighted mean, so it answers where the vertices are, not where a polygon's area
     * is - a ring with its vertices bunched along one side means toward that side.
     *
     * @param points the {@code {x, y}} points to average; must be non-empty
     * @return the mean position as {@code {x, y}}
     * @throws IllegalArgumentException if {@code points} is empty (a mean is undefined
     *         with nothing to average)
     */
    public static double[] computeMean(List<double[]> points) {
        return computeMean(points, point -> point[0], point -> point[1]);
    }

    /**
     * The mean position of a list of vectors - {@link #computeMean(List)} for points laid out in
     * the float coordinates the game's UI works in.
     *
     * <p>Named apart from {@code computeMean} rather than overloading it: a {@code List<Vector2f>}
     * and a {@code List<double[]>} erase to the same parameter type, so Java cannot hold both.
     *
     * @param points the points to average; must be non-empty
     * @return the mean position
     * @throws IllegalArgumentException if {@code points} is empty (a mean is undefined
     *         with nothing to average)
     */
    public static Vector2f computeMeanOfVectors(List<Vector2f> points) {

        var mean = computeMean(points, point -> point.x, point -> point.y);
        return new Vector2f((float) mean[0], (float) mean[1]);
    }

    /**
     * The signed projection of {@code (x, y)} onto an axis - {@code point . axis}, how far
     * along that direction the point sits, measured from the origin.
     *
     * <p>The one home for the arithmetic every axis-relative measure is built from: a point's
     * position along a direction, its perpendicular offset (the same projection taken onto
     * that direction's normal), and the bounds {@link #projectExtentOnto} takes over a cloud.
     * Stated once here so a caller names what its projection means rather than restating the
     * formula, where a transposed component reads as plausible code.
     *
     * <p>The axis need not be unit length: scaling it scales the projection by the same
     * factor, so the sign and the ordering of two projections survive either way, while only
     * a unit axis makes the result a true world distance.
     *
     * @param x     x of the point to project
     * @param y     y of the point to project
     * @param axisX x of the direction to project onto
     * @param axisY y of the direction to project onto
     * @return the signed projection along the axis
     */
    public static double projectPointOnto(double x, double y, double axisX, double axisY) {
        return x * axisX + y * axisY;
    }

    /**
     * The signed projection of an {@code {x, y}} point onto an {@code {x, y}} axis.
     */
    public static double projectPointOnto(double[] point, double[] axis) {
        return projectPointOnto(point[0], point[1], axis[0], axis[1]);
    }

    /**
     * The extent of a point cloud projected onto an axis, as {@code {min, max}} - the
     * lowest and highest of each point's signed projection {@code point . axis}. The
     * width of the cloud along that direction is {@code max - min}: measuring a spread
     * along an axis, or spacing parallel lines across a shape, both read from it.
     *
     * <p>The axis need not be unit length - scaling it scales every projection equally,
     * so the extent scales with it while the point that attains each bound is unchanged;
     * a caller that needs a true distance passes a unit axis. Projecting relative to a
     * reference point or to the origin gives the same {@code max - min}, since a shared
     * offset shifts both bounds together.
     *
     * @param points the {@code {x, y}} points to project; must be non-empty
     * @param axisX  x of the direction to project onto
     * @param axisY  y of the direction to project onto
     * @return the {@code {min, max}} projections
     * @throws IllegalArgumentException if {@code points} is empty (an extent is
     *         undefined with nothing to project)
     */
    public static double[] projectExtentOnto(
            List<double[]> points,
            double axisX,
            double axisY) {

        if (points.isEmpty()) {
            throw new IllegalArgumentException("Cannot project an extent of no points");
        }

        var min = Double.POSITIVE_INFINITY;
        var max = Double.NEGATIVE_INFINITY;

        for (var point : points) {

            var projection = projectPointOnto(point[0], point[1], axisX, axisY);

            min = Math.min(min, projection);
            max = Math.max(max, projection);
        }
        return new double[] {min, max};
    }

    /**
     * The combined extent of several point groups projected onto an axis, as
     * {@code {min, max}} - the lowest and highest projection across every point of every
     * group. Generalises {@link #projectExtentOnto} to a shape made of several clouds (a
     * polygon's outer ring plus its holes, a multi-part region): the union spans them
     * all, so {@code max - min} is the whole shape's width along the axis.
     *
     * <p>The axis-length and reference-point remarks on {@link #projectExtentOnto} carry
     * over unchanged, since each group projects the same way.
     *
     * @param pointGroups the groups of {@code {x, y}} points to project; must be
     *                    non-empty, and each group must itself be non-empty
     * @param axisX       x of the direction to project onto
     * @param axisY       y of the direction to project onto
     * @return the {@code {min, max}} projections across all groups
     * @throws IllegalArgumentException if {@code pointGroups} is empty, or any group is
     *         empty (an extent is undefined with nothing to project)
     */
    public static double[] projectCombinedExtentOnto(
            List<List<double[]>> pointGroups,
            double axisX,
            double axisY) {

        if (pointGroups.isEmpty()) {
            throw new IllegalArgumentException(
                "Cannot project a combined extent of no point groups");
        }

        var min = Double.POSITIVE_INFINITY;
        var max = Double.NEGATIVE_INFINITY;

        for (var group : pointGroups) {

            var extent = projectExtentOnto(group, axisX, axisY);
            min = Math.min(min, extent[0]);
            max = Math.max(max, extent[1]);
        }
        return new double[] {min, max};
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
     * The squared Euclidean distance between {@code (x1, y1)} and {@code (x2, y2)}.
     * The companion to {@link #computeDistance} for the common case of comparing a
     * distance against a threshold: squaring the threshold instead lets the test skip
     * the square root, so a proximity or move check reads
     * {@code computeDistanceSquared(a, b) <= radius * radius} with no {@code sqrt} on
     * the hot path.
     */
    public static double computeDistanceSquared(double x1, double y1, double x2, double y2) {

        var deltaX = x1 - x2;
        var deltaY = y1 - y2;

        return deltaX * deltaX + deltaY * deltaY;
    }

    /**
     * The squared Euclidean distance between two {@code {x, y}} points.
     */
    public static double computeDistanceSquared(double[] a, double[] b) {
        return computeDistanceSquared(a[0], a[1], b[0], b[1]);
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
    public static double[] computeUnitVector(double x, double y, double minLength) {

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
     * The bearing in degrees between two {@code {x, y}} points, measured
     * counter-clockwise from the positive x-axis, in the range
     * {@code (-180, 180]}. Coincident points yield 0.
     */
    public static double computeAngleDegrees(double[] a, double[] b) {
        return computeAngleDegrees(a[0], a[1], b[0], b[1]);
    }

    /**
     * The bearing in degrees from {@code a} to {@code b}, measured
     * counter-clockwise from the positive x-axis, in the range
     * {@code (-180, 180]}. Coincident points yield 0.
     */
    public static double computeAngleDegrees(Vector2f a, Vector2f b) {
        return computeAngleDegrees(a.x, a.y, b.x, b.y);
    }

    /**
     * The unsigned angle in radians between vectors {@code (ax, ay)} and
     * {@code (bx, by)}, in the range {@code [0, PI]}: 0 when they point the same
     * way, PI when opposite. The opening between two directions, blind to which
     * side the turn is on - a left turn and a right turn of equal sharpness read
     * alike.
     *
     * <p>Companion to {@link #computeAngleDegrees}, which is the bearing of one
     * directed segment; this is the angle between two. Returns {@link Double#NaN}
     * when either vector is shorter than {@code minLength} and so has no
     * direction, leaving the caller to decide what a degenerate input means
     * rather than baking a convention in here.
     *
     * @param ax        x of the first vector
     * @param ay        y of the first vector
     * @param bx        x of the second vector
     * @param by        y of the second vector
     * @param minLength shortest a vector may be and still have a direction; below
     *                  it the angle is {@link Double#NaN}
     * @return the angle in radians in {@code [0, PI]}, or {@code NaN} when either
     *         vector is degenerate
     */
    public static double computeAngleBetween(
            double ax,
            double ay,
            double bx,
            double by,
            double minLength) {

        var aLength = computeVectorLength(ax, ay);
        var bLength = computeVectorLength(bx, by);

        if (aLength < minLength || bLength < minLength) {
            return Double.NaN;
        }

        var cosine = (ax * bx + ay * by) / (aLength * bLength);

        // Clamp against rounding drift just outside [-1, 1] before acos.
        return Math.acos(Math.max(-1.0, Math.min(1.0, cosine)));
    }

    // The averaging walk both public forms share. Coordinates are read through the two accessors
    // so neither form copies its points into the other's type first.
    private static <P> double[] computeMean(
            List<P> points,
            ToDoubleFunction<P> readX,
            ToDoubleFunction<P> readY) {

        if (points.isEmpty()) {
            throw new IllegalArgumentException("Cannot average no points");
        }

        var sumX = 0.0;
        var sumY = 0.0;

        for (var point : points) {

            sumX += readX.applyAsDouble(point);
            sumY += readY.applyAsDouble(point);
        }
        return new double[] {sumX / points.size(), sumY / points.size()};
    }
}
