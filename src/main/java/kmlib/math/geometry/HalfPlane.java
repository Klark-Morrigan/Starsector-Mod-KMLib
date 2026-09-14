package kmlib.math.geometry;

/**
 * An oriented line given by a point on it and a normal, dividing the plane into the half
 * the normal points to (the kept side) and the far half. The dual of {@link DirectedLine}:
 * where that names a line by the direction it runs along (for spans placed on it), this
 * names one by its normal (for the side a point falls on and the half a clip keeps).
 *
 * <p>Reifies the point-and-normal quartet the half-plane clip
 * ({@link LabelledPolygon#clipToHalfPlane}) and the side test
 * ({@link Lines#computeSignedOffsetFromLine}) otherwise take as four loose coordinates.
 * The normal need not be unit length: only the sign of the side test matters to a clip,
 * and ratios of two offsets cancel the shared scale, so a caller may pass a raw
 * (unnormalised) normal.
 *
 * @param pointX  x of a point on the boundary line
 * @param pointY  y of a point on the boundary line
 * @param normalX x of the normal pointing to the kept side
 * @param normalY y of the normal pointing to the kept side
 */
public record HalfPlane(
        double pointX,
        double pointY,
        double normalX,
        double normalY) {
}
