package kmlib.math.geometry;

/**
 * An infinite 2D line in parametric form - the point it passes through and the direction
 * it runs, extending both ways without end. Reifies the point-and-direction pair the
 * geometry line passes ({@link Spans}, {@link Polygons}) otherwise take as four loose
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
}
