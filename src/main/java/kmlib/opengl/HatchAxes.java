package kmlib.opengl;

import kmlib.math.geometry.Points;

/**
 * A family of parallel hatch lines as the two axes it is defined on - the direction the lines run
 * in, the normal their spacing is measured along, and the spacing itself - so the family travels
 * as one value instead of five loose doubles threaded down a clip walk.
 *
 * <p>Owns the projections both ways: which line of the family a point lies on and how far along
 * that line it sits, and back from a (line, distance) pair to the point itself. Holding the
 * arithmetic here rather than restating it at each use site is what lets a clipped span be carried
 * as the two numbers that name it and rebuilt into points only where it is emitted. That rebuild
 * is the point of the return trip: a point derived from the family's own axes lies exactly on its
 * line, where one carried through from an edge intersection only lies near it.
 */
record HatchAxes(
    double directionX,
    double directionY,
    double normalX,
    double normalY,
    double spacing) {

    // The lines run along the direction; the level axis is that direction turned 90 degrees, so
    // the hatch lines are the loci where the level is an integer multiple of the spacing.
    static HatchAxes computeAxesFromAngle(double angleRadians, double spacing) {
        var directionX = Math.cos(angleRadians);
        var directionY = Math.sin(angleRadians);
        return new HatchAxes(directionX, directionY, -directionY, directionX, spacing);
    }

    // How far along the line direction the point sits - the ordering a clipped span's two ends
    // are picked by.
    double computeDistanceAlong(double x, double y) {
        return Points.projectPointOnto(x, y, directionX, directionY);
    }

    // The point's perpendicular offset, which names the hatch line through it once divided by
    // the spacing.
    double computeLevel(double x, double y) {
        return Points.projectPointOnto(x, y, normalX, normalY);
    }

    // The perpendicular offset of the numbered line. Line zero passes through the origin, so a
    // line's level is its index times the spacing - which is what keeps the pattern anchored to
    // the world rather than to whatever region is being hatched.
    double computeLevelOfLine(int lineIndex) {
        return lineIndex * spacing;
    }

    // The x of the point sitting the given distance down the numbered line: that line's own
    // offset out along the normal, plus the distance along the direction.
    double computeXOnLine(int lineIndex, double distanceAlong) {
        return normalX * computeLevelOfLine(lineIndex) + directionX * distanceAlong;
    }

    // The y of the same point.
    double computeYOnLine(int lineIndex, double distanceAlong) {
        return normalY * computeLevelOfLine(lineIndex) + directionY * distanceAlong;
    }
}
