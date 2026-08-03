package kmlib.math.geometry;

/**
 * An infinite 2D line in parametric form - the point it passes through and the direction
 * it runs, extending both ways without end. Reifies the point-and-direction pair the
 * geometry line passes ({@link Spans}, {@link PolygonRegions}) otherwise take as four loose
 * coordinates, so a line travels as one value.
 *
 * <p>A point on the line is {@code origin + t * direction}; a parameter {@code t} is a
 * distance from the origin scaled by the direction's length, so a unit direction makes
 * {@code t} a world distance. The operations that read a line for interior or clearance
 * spans normalise the direction themselves, so those tolerate a non-unit direction; a
 * caller that projects a parameter straight back to world coordinates (as {@link
 * RegionChord#toSegment} does) supplies a unit direction so the parameter reads as a
 * distance.
 *
 * @param originX    x of the point the line passes through (parameter zero)
 * @param originY    y of the point the line passes through (parameter zero)
 * @param directionX x of the direction the line runs
 * @param directionY y of the direction the line runs
 */
public record DirectedLine(
        double originX,
        double originY,
        double directionX,
        double directionY) {

    /**
     * The same line with its direction normalised, so a parameter along it reads as a
     * world distance - or {@code null} when the direction is too short to define a
     * line at all.
     *
     * <p>The step every operation measuring parameters along a line takes before it can
     * measure anything, kept here so the normalisation and the degenerate verdict that
     * comes with it have one home: two operations asked to read the same line must agree
     * on whether it is a line.
     *
     * @return this line with a unit direction and its origin unmoved, or {@code null}
     *         when the direction is shorter than {@link Limits#MIN_EDGE_LENGTH}
     */
    public DirectedLine toUnitLine() {
        var direction = Points.computeUnitVector(
            directionX,
            directionY,
            Limits.MIN_EDGE_LENGTH);

        if (direction == null) {
            return null;
        }
        return new DirectedLine(originX, originY, direction[0], direction[1]);
    }
}
