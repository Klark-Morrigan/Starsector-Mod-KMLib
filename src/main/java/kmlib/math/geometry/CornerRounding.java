package kmlib.math.geometry;

/**
 * The shape a rounding pass gives a corner: how far back from it the arc starts, how
 * finely that arc is sampled, how sharp a corner has to be before it is cut flat
 * instead of arced, and how sharp one has to be before it is touched at all.
 *
 * <p>Held as one value because the four only mean anything together - a radius says
 * nothing about the result without the segment count that samples it, and the two
 * angle thresholds decide whether either applies at all. Kept apart they also read
 * as bare numbers at a call site, where a {@code double, int, double, double} run
 * transposes without the compiler noticing.
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
 * @param roundBelowAngleRadians only corners with an interior angle below this are
 *                               touched at all - flatter ones keep their original
 *                               vertex, so a densely sampled arc is not resampled
 *                               to sand a handful of sharp joins. {@link
 *                               #ROUND_EVERY_CORNER} rounds all of them;
 *                               non-positive rounds none
 */
public record CornerRounding(
    double radius,
    int segmentsPerCorner,
    double bevelBelowAngleRadians,
    double roundBelowAngleRadians) {

    // An interior angle tops out at a straight pass-through of half a turn, so a
    // threshold strictly above that admits every corner there is - including the
    // exactly-straight vertex, which sits AT half a turn and would slip past a
    // threshold set there. Named so a call site says which behaviour it wants
    // rather than smuggling the geometry fact in as a literal.
    public static final double ROUND_EVERY_CORNER = Angles.FULL_TURN;
}
