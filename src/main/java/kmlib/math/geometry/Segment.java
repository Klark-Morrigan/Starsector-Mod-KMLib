package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * A directed line segment as its two endpoints, named by role so geometry reads
 * {@code start}/{@code end} rather than indexing a four-double array.
 *
 * <p>The shared currency between the edge-offset pass that emits one segment per
 * polygon edge and the chainer that welds a bag of segments back into closed
 * rings: both speak in whole segments, so neither side has to agree on which
 * array slot holds which coordinate. The direction (start to end) is meaningful -
 * a caller tracing a region orients every segment so the interior lies
 * consistently to one side, and the chainer follows each segment's end to the
 * next segment's start.
 */
public record Segment(
    double startX,
    double startY,
    double endX,
    double endY) {

    // Below two lanes a band has no boundary between lanes to draw.
    private static final int MIN_LANES_TO_DIVIDE = 2;

    /**
     * The segment of a given length centred on a point and running in a given direction.
     *
     * <p>The shape a centre-anchored run takes - a line of text hung from its middle, say - where the
     * caller knows the middle and the bearing rather than either end.
     *
     * @param centreX          the x the segment's midpoint sits at
     * @param centreY          the y the segment's midpoint sits at
     * @param directionRadians the bearing from start to end, anticlockwise from the x axis
     * @param length           the whole length, half of it either side of the centre
     * @return the segment, its start half the length behind the centre and its end half ahead
     */
    public static Segment createCentredOn(
            double centreX,
            double centreY,
            double directionRadians,
            double length) {

        var halfX = Math.cos(directionRadians) * length / 2;
        var halfY = Math.sin(directionRadians) * length / 2;

        return new Segment(
            centreX - halfX,
            centreY - halfY,
            centreX + halfX,
            centreY + halfY);
    }

    /**
     * The segment from one point to another.
     *
     * @param start where the segment starts, as {@code {x, y}}
     * @param end   where it ends
     * @return the segment, holding copies of the coordinates rather than the arrays
     */
    public static Segment joinPoints(double[] start, double[] end) {
        return new Segment(start[0], start[1], end[0], end[1]);
    }

    /**
     * The four corners of the band this segment is the centreline of - the rectangle
     * {@code thickness} wide, half of it either side, running the segment's whole length.
     *
     * <p>Asked of the segment rather than worked out by each caller, because more than one
     * has to agree on it: a band drawn from a centreline and a girth, and anything else
     * reasoning about the room that band takes up, describe the same rectangle, and two
     * derivations of it would let the drawn one and the reserved one part company.
     *
     * <p>The corners come back as a closed ring's vertices (no repeated closing point), so
     * the result drops straight into the polygon operations here.
     *
     * @param thickness the band's full girth across the centreline
     * @return the corners in ring order: both ends of the left edge, then both ends of the
     *         right edge, walked back. Empty when the segment is too short to take a direction
     *         from, or the girth too small to enclose area - in either case there is no
     *         rectangle to state
     */
    public List<double[]> computeBandCorners(double thickness) {

        var normal = computeLeftUnitNormal();
        if (!canHoldBand(normal, thickness)) {
            return List.of();
        }

        var leftEdge = offsetAlongNormal(normal, thickness / 2);
        var rightEdge = offsetAlongNormal(normal, -thickness / 2);

        return List.of(
            leftEdge.readStart(),
            leftEdge.readEnd(),
            rightEdge.readEnd(),
            rightEdge.readStart());
    }

    /**
     * The bearing from start to end, in radians anticlockwise from the x axis, as
     * {@link Math#atan2} gives it.
     *
     * <p>Radians because that is the unit {@link #createCentredOn} takes, so the two compose
     * without a conversion between them.
     *
     * @return the bearing in {@code [-PI, PI]}; zero for a segment whose ends coincide
     */
    public double computeDirectionRadians() {
        return Math.atan2(endY - startY, endX - startX);
    }

    /**
     * The lines dividing the band this segment is the centreline of into lanes of equal girth - the
     * rules between stacked lines of text, say.
     *
     * <p>Each divider runs the segment's whole length, parallel to it, so a band and its lanes are
     * read off the one centreline and girth {@link #computeBandCorners} reads.
     *
     * @param thickness the band's full girth across the centreline
     * @param laneCount how many lanes the band holds
     * @return {@code laneCount - 1} dividers, from the right of the direction of travel to the left.
     *         Empty for fewer than two lanes, which have no boundary between them, and for a segment
     *         too short to take a direction from
     */
    public List<Segment> computeLaneDividers(double thickness, int laneCount) {

        if (laneCount < MIN_LANES_TO_DIVIDE) {
            return List.of();
        }

        var normal = computeLeftUnitNormal();
        if (!canHoldBand(normal, thickness)) {
            return List.of();
        }

        var dividers = new ArrayList<Segment>(laneCount - 1);

        for (var divider = 1; divider < laneCount; divider++) {

            // Stepped from the right-hand edge (minus half the girth) across in whole lanes.
            var offset = -thickness / 2 + thickness * divider / laneCount;

            dividers.add(offsetAlongNormal(normal, offset));
        }

        return dividers;
    }

    /**
     * The unit vector perpendicular to the segment, on the left of its direction of travel, as
     * {@link Segments#computeLeftUnitNormal} states it.
     *
     * @return the normal as {@code {x, y}}; null when the segment is too short to take a direction
     *         from ({@link Limits#MIN_EDGE_LENGTH})
     */
    public double[] computeLeftUnitNormal() {
        return Segments.computeLeftUnitNormal(readStart(), readEnd());
    }

    /**
     * The segment moved sideways by {@code distance}, parallel to this one, as long and running
     * the same way.
     *
     * <p>Positive distances move it to the left of the direction of travel and negative ones to
     * the right, along {@link #computeLeftUnitNormal}.
     *
     * @param distance how far to move it, signed by side
     * @return the moved segment; null when the segment is too short to take a direction from
     */
    public Segment computeOffsetSegment(double distance) {

        var normal = computeLeftUnitNormal();

        return normal == null
            ? null
            : offsetAlongNormal(normal, distance);
    }

    /**
     * The end as a point.
     *
     * @return {@code {endX, endY}}, freshly allocated so a caller may keep it
     */
    public double[] readEnd() {
        return new double[] {endX, endY};
    }

    /**
     * The start as a point.
     *
     * @return {@code {startX, startY}}, freshly allocated so a caller may keep it
     */
    public double[] readStart() {
        return new double[] {startX, startY};
    }

    // Whether there is a band to describe: a direction to spend the girth across, and a girth that
    // encloses area. Asked once for the corners and the dividers, so the two never disagree on it.
    private static boolean canHoldBand(double[] normal, double thickness) {

        return normal != null
            && thickness >= Limits.MIN_EDGE_LENGTH;
    }

    // This segment moved along a normal already worked out, so a band offsetting it more than once
    // takes the normal once.
    private Segment offsetAlongNormal(double[] normal, double distance) {

        var shiftX = normal[0] * distance;
        var shiftY = normal[1] * distance;

        return new Segment(
            startX + shiftX,
            startY + shiftY,
            endX + shiftX,
            endY + shiftY);
    }
}
