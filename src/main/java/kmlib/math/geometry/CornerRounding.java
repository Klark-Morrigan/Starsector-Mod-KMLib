package kmlib.math.geometry;

/**
 * The shape a rounding pass gives a corner: how far back from it the arc starts, how
 * finely that arc is sampled, and how sharp a corner has to be before it is cut flat
 * instead of arced.
 *
 * <p>Held as one value because the three only mean anything together - a radius says
 * nothing about the result without the segment count that samples it, and the chamfer
 * threshold decides whether either applies at all. Kept apart they also read as three
 * bare numbers at a call site, where a {@code double, int, double} run transposes
 * without the compiler noticing.
 *
 * @param radius                 corner radius in the polygon's units; clamped per
 *                               corner to half the shorter adjacent edge, so
 *                               neighbouring corners cannot eat into each other.
 *                               Non-positive rounds nothing
 * @param segmentsPerCorner      arc segments per rounded corner; higher is smoother.
 *                               Below one rounds nothing
 * @param bevelBelowAngleRadians corners with an interior angle below this are
 *                               chamfered flat rather than arced, an arc that tight
 *                               reading as a nick; non-positive arcs every corner
 */
public record CornerRounding(
    double radius,
    int segmentsPerCorner,
    double bevelBelowAngleRadians) {
}
