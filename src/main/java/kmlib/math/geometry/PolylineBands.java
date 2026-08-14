package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Gives an open polyline girth: a centreline and a width become a filled band of
 * triangles that turns at each corner rather than cutting it.
 *
 * <p>Triangles rather than a wide line primitive, because a wide line has no join
 * handling - each segment is stroked on its own, so a corner comes out as a notch on
 * the outside of the turn and a stack of overdrawn ends on the inside - and its width
 * is a screen quantity, where a band laid out along world geometry is a world one.
 * Triangles carry the joins the primitive does not, and a band of them is built once
 * from geometry that does not move rather than re-emitted every frame.
 *
 * <p>A corner is mitred: the band's two rails are carried on to where they cross, so
 * the outside of the turn closes to a point, the inside meets at a single vertex, and
 * the band turns as one piece. A miter fails in two ways, and each falls back to a
 * bevel - the rails cut straight across the corner instead:
 *
 * <ul>
 *   <li>A sharp turn throws the outer miter far past the corner. That is a spike, and
 *       it reads as a stray point rather than as a band turning, so
 *       {@code miterSpikeLimit} says where a corner stops being one.
 *   <li>A miter also reaches back along both of the segments it joins, and a segment
 *       short enough for the reaches at its two ends to meet has its rails cross -
 *       the band folds into a bowtie. So a miter is given up when it outruns the room
 *       its segments leave it, whatever the angle.
 * </ul>
 *
 * <p>The band comes out gap-free, and free of self-overlap wherever the polyline's own
 * turns leave room for the width asked of them - a centreline doubling back within its
 * own band width has nowhere to put the band but over itself. That matters because a
 * translucent band draws every overlap as a brighter patch, so overlap is visible in a
 * way it is not for an opaque fill.
 *
 * <p>Joins are made within one stroke. Two bands stroked separately butt at their shared
 * end rather than joining, which is exact where that end sits along a straight stretch and
 * leaves a small wedge open where it lands on a corner. So a band that changes colour along
 * its length is stroked once and cut into its pieces afterwards
 * ({@link #strokeSpansToTriangles}) rather than stroked a piece at a time - otherwise every
 * colour change landing on a corner opens that wedge, and on a centreline traced round a
 * rounded shape most of them do.
 */
public final class PolylineBands {

    private PolylineBands() {
    }

    /**
     * Strokes {@code polyline} into a band of {@code width} centred on it, as triangles.
     *
     * <p>Every corner of the polyline is a corner of the band: the stretch between two
     * corners is one quad, and the corner between two stretches is a miter or a bevel.
     * A caller wanting a band along part of a longer path therefore has to hand over
     * that part's corners, not only its two ends - the ends alone describe a straight
     * band that cuts across everything the path turns at in between.
     *
     * @param polyline        the band's centreline as {x, y} points in order, open (no
     *                        closing point back to the first). Points repeated within
     *                        {@link Limits#MIN_EDGE_LENGTH} of their predecessor are one
     *                        point, since the step between them names no direction
     * @param width           the band's full width, half of it either side of the
     *                        centreline
     * @param miterSpikeLimit a corner whose miter would stand farther than this multiple
     *                        of the half-width from the corner is bevelled instead, as
     *                        {@link PolygonOffsets#insetPolygonByMiter} bevels one;
     *                        larger keeps crisper points, smaller bevels sooner
     * @return the band's triangles as {x, y} points, every three consecutive points one
     *         triangle; empty when the polyline has fewer than two distinct points or
     *         the width is too small to enclose area
     */
    public static List<double[]> strokeToTriangles(
            List<double[]> polyline,
            double width,
            double miterSpikeLimit) {

        return strokeSpansToTriangles(List.of(polyline), width, miterSpikeLimit).get(0);
    }

    /**
     * Strokes the consecutive spans of one centreline into a single band of {@code width},
     * handing back each span's own share of it.
     *
     * <p>For a band drawn in more than one piece - different colours along its length, say.
     * Stroking each piece on its own would leave every boundary between two of them a butt
     * rather than a join, so a boundary landing on a corner of the centreline opens a wedge
     * there; stroking once and cutting the result up makes each boundary a point the one
     * band turns at like any other. Only the band's two outer ends stay open, having nothing
     * to join to.
     *
     * <p>The spans are consecutive stretches of one centreline, so each begins where its
     * predecessor ended. That shared point may be given twice, once per span, or once in
     * either of them - a point landing on its predecessor is one point, exactly as within a
     * single span.
     *
     * @param spans           the centreline's stretches in order, each as {x, y} points and
     *                        each carrying every corner the centreline turns at within it -
     *                        two ends alone describe a straight stretch cutting across
     *                        whatever lies between them
     * @param width           the band's full width, half of it either side of the centreline
     * @param miterSpikeLimit a corner whose miter would stand farther than this multiple of
     *                        the half-width from the corner is bevelled instead, as
     *                        {@link PolygonOffsets#insetPolygonByMiter} bevels one
     * @return one triangle list per span, in the order the spans were given, every three
     *         consecutive {x, y} points one triangle. A span holding no distinct step of its
     *         own comes back empty rather than being dropped, so a caller can read its spans
     *         off by position
     */
    public static List<List<double[]>> strokeSpansToTriangles(
            List<List<double[]>> spans,
            double width,
            double miterSpikeLimit) {

        var centreline = joinSpans(spans);
        var points = centreline.points();
        var trianglesPerSpan = new ArrayList<List<double[]>>(spans.size());

        for (var span = 0; span < spans.size(); span++) {
            trianglesPerSpan.add(new ArrayList<double[]>());
        }

        // A band needs two distinct points to have a direction and a width to have area.
        // The width is measured against the length below which two points are one, since
        // a band thinner than that is a line the rails of which have collapsed together.
        if (points.size() < 2 || width < Limits.MIN_EDGE_LENGTH) {
            return trianglesPerSpan;
        }

        var joins = buildJoins(points, width / 2, miterSpikeLimit);

        for (var segment = 0; segment + 1 < points.size(); segment++) {

            // The joins were built over the whole centreline, so a segment ending at a span
            // boundary closes on the miter that boundary's corner takes rather than on a
            // square end - which is the whole of what stroking once buys.
            var triangles = trianglesPerSpan.get(centreline.spanOfSegment()[segment]);
            var from = joins.get(segment);
            var to = joins.get(segment + 1);

            // The quad runs down one rail and back up the other, so its two triangles
            // are wound the same way; then the corner it arrives at contributes the
            // wedge holding its bevel open, which is empty where that corner mitred.
            appendQuad(
                triangles,
                from.leavingLeft(),
                from.leavingRight(),
                to.arrivingRight(),
                to.arrivingLeft());

            triangles.addAll(to.bevelTriangle());
        }
        return trianglesPerSpan;
    }

    // One join per point of the centreline: a straight cap at each end, where the band
    // simply stops, and a miter or bevel at every corner in between.
    private static List<BandJoin> buildJoins(
            List<double[]> points,
            double halfWidth,
            double miterSpikeLimit) {

        var last = points.size() - 1;
        var joins = new ArrayList<BandJoin>(points.size());

        joins.add(buildCap(
            points.get(0),
            computeLeftUnitNormal(points.get(0), points.get(1)),
            halfWidth));

        for (var corner = 1; corner < last; corner++) {
            joins.add(buildCorner(points, corner, halfWidth, miterSpikeLimit));
        }

        joins.add(buildCap(
            points.get(last),
            computeLeftUnitNormal(points.get(last - 1), points.get(last)),
            halfWidth));

        return joins;
    }

    // The join where the band stops: the two rail points square across the end, on the
    // normal of the one segment reaching it.
    private static BandJoin buildCap(double[] end, double[] normal, double halfWidth) {

        return BandJoin.ofSharedRails(
            offsetPoint(end, normal, halfWidth),
            offsetPoint(end, normal, -halfWidth));
    }

    // The join where the band turns: the miter both rails cross at, or the bevel that
    // replaces it where the miter would spike or outrun the segments it joins.
    private static BandJoin buildCorner(
            List<double[]> points,
            int corner,
            double halfWidth,
            double miterSpikeLimit) {

        var previous = points.get(corner - 1);
        var at = points.get(corner);
        var next = points.get(corner + 1);

        var arrivingNormal = computeLeftUnitNormal(previous, at);
        var leavingNormal = computeLeftUnitNormal(at, next);

        var arrivingLeft = offsetPoint(at, arrivingNormal, halfWidth);
        var arrivingRight = offsetPoint(at, arrivingNormal, -halfWidth);
        var leavingLeft = offsetPoint(at, leavingNormal, halfWidth);
        var leavingRight = offsetPoint(at, leavingNormal, -halfWidth);

        var leftMiter = intersectRails(arrivingLeft, arrivingNormal, leavingLeft, leavingNormal);
        var rightMiter = intersectRails(arrivingRight, arrivingNormal, leavingRight, leavingNormal);

        // Parallel rails have no crossing to miter at. Either the polyline runs straight
        // through this point - where the two pairs of rail points already coincide, so
        // butting them is seamless - or it doubles back on itself, where there is no
        // outside of the turn to close and the band is bound to lie over itself anyway.
        //
        // One rail decides for both: the two rails on a side are the segments' own
        // directions, offset, so whether they converge is a fact about the corner rather
        // than about which side of it the offset went.
        if (leftMiter == null) {
            return new BandJoin(arrivingLeft, arrivingRight, leavingLeft, leavingRight, List.of());
        }

        // A left turn puts the inside of the turn on the left. The normals turn with the
        // segments, so their cross product carries the same sign as the segments' own.
        var turnsLeft = arrivingNormal[0] * leavingNormal[1]
            - arrivingNormal[1] * leavingNormal[0] > 0;

        var innerMiter = turnsLeft ? leftMiter : rightMiter;
        var outerMiter = turnsLeft ? rightMiter : leftMiter;
        var overshoot = Points.computeDistance(outerMiter, at);

        var arrivesFromCorner = corner - 1 > 0;
        var leavesToCorner = corner + 1 < points.size() - 1;

        var fitsOnItsSegments = overshoot <= Math.min(
            computeAvailableReach(previous, at, arrivesFromCorner),
            computeAvailableReach(at, next, leavesToCorner));

        // The two failures are asked separately because they answer different halves of
        // the join. A miter that cannot fit is dropped on both rails, since either would
        // cross; one that fits but spikes is dropped only on the outer rail, where the
        // spike is - the inner rail still meets at a point and needs no bevel.
        var inner = fitsOnItsSegments ? innerMiter : at;
        var isOuterMitred = fitsOnItsSegments && overshoot <= miterSpikeLimit * halfWidth;

        var outerArriving = isOuterMitred
            ? outerMiter
            : (turnsLeft ? arrivingRight : arrivingLeft);

        var outerLeaving = isOuterMitred
            ? outerMiter
            : (turnsLeft ? leavingRight : leavingLeft);

        // The bevel leaves the two segments' outer rail ends apart, and the wedge back to
        // the shared inner vertex is exactly the gap between the quads either side.
        var bevelTriangle = isOuterMitred
            ? List.<double[]>of()
            : List.of(inner, outerArriving, outerLeaving);

        return turnsLeft
            ? new BandJoin(inner, outerArriving, inner, outerLeaving, bevelTriangle)
            : new BandJoin(outerArriving, inner, outerLeaving, inner, bevelTriangle);
    }

    // Where the two rails on one side of the band cross - the miter point that corner
    // takes. Each rail runs through its own offset point along its segment's direction,
    // which is that segment's left normal turned back the way it came. Null where the two
    // are parallel and never cross.
    private static double[] intersectRails(
            double[] arrivingPoint,
            double[] arrivingNormal,
            double[] leavingPoint,
            double[] leavingNormal) {

        return Lines.intersectLines(
            arrivingPoint,
            arrivingNormal[1],
            -arrivingNormal[0],
            leavingPoint,
            leavingNormal[1],
            -leavingNormal[0]);
    }

    // How far back along a segment a join at one of its ends may reach before its rails
    // cross what is at the other end: half the segment where that other end is a corner,
    // so two miters split it between them, and the whole of it where the segment ends the
    // polyline, since a straight cap reaches back none of it.
    private static double computeAvailableReach(
            double[] from,
            double[] to,
            boolean otherEndIsCorner) {

        return Points.computeDistance(from, to) * (otherEndIsCorner ? 0.5 : 1.0);
    }

    // The spans laid end to end as the one centreline they are stretches of, plus the span
    // each of its segments belongs to - which is what lets one set of joins be cut back up
    // into the pieces it was asked for.
    //
    // Points landing on their predecessor are dropped throughout, which covers the shared
    // ends the spans meet at as much as a repeat within one of them: such a step has no
    // direction, and so no rails to offset along it, and dropping it leaves the same
    // centreline stated in steps that all have one. A span left with no step of its own
    // therefore owns no segment, and strokes to nothing.
    private static SpannedCentreline joinSpans(List<List<double[]>> spans) {

        var totalPoints = 0;

        for (var span : spans) {
            totalPoints += span.size();
        }

        var points = new ArrayList<double[]>(totalPoints);
        var spanOfSegment = new int[totalPoints];

        for (var span = 0; span < spans.size(); span++) {
            for (var point : spans.get(span)) {

                if (points.isEmpty()) {
                    points.add(point);
                    continue;
                }
                if (Rings.isSamePoint(points.get(points.size() - 1), point)) {
                    continue;
                }

                // The point about to be added closes the segment leaving the one before it,
                // and that segment is walked by the span the new point came from.
                spanOfSegment[points.size() - 1] = span;
                points.add(point);
            }
        }
        return new SpannedCentreline(points, spanOfSegment);
    }

    // The left-hand unit normal of the directed segment a -> b: its direction turned 90
    // degrees left, so the band's left rail sits at a positive offset along it.
    //
    // No zero-length guard: the centreline was deduplicated at Limits.MIN_EDGE_LENGTH,
    // which is the length below which a segment has no direction, so nothing here divides
    // by ~zero.
    private static double[] computeLeftUnitNormal(double[] a, double[] b) {

        var segmentX = b[0] - a[0];
        var segmentY = b[1] - a[1];
        var length = Points.computeVectorLength(segmentX, segmentY);

        return new double[] {-segmentY / length, segmentX / length};
    }

    private static double[] offsetPoint(double[] point, double[] normal, double distance) {

        return new double[] {
            point[0] + normal[0] * distance,
            point[1] + normal[1] * distance};
    }

    // Splits a quad given in winding order into the two triangles across its diagonal.
    private static void appendQuad(
            List<double[]> triangles,
            double[] first,
            double[] second,
            double[] third,
            double[] fourth) {

        triangles.add(first);
        triangles.add(second);
        triangles.add(third);
        triangles.add(first);
        triangles.add(third);
        triangles.add(fourth);
    }

    /**
     * Where one point of the centreline puts the band's rails: the pair closing the
     * segment arriving at it, the pair opening the one leaving it, and the wedge between
     * the two pairs where they differ.
     *
     * <p>The pairs are separate because a bevelled corner is exactly the case where they
     * part company - the two segments meet the corner square rather than at a shared
     * miter, and the wedge is what holds the gap that leaves closed.
     *
     * @param arrivingLeft  the left rail's point closing the arriving segment
     * @param arrivingRight the right rail's point closing the arriving segment
     * @param leavingLeft   the left rail's point opening the leaving segment
     * @param leavingRight  the right rail's point opening the leaving segment
     * @param bevelTriangle the three corners of the wedge filling the outside of a
     *                      bevelled corner, or empty where the corner mitred and the two
     *                      pairs meet
     */
    private record BandJoin(
        double[] arrivingLeft,
        double[] arrivingRight,
        double[] leavingLeft,
        double[] leavingRight,
        List<double[]> bevelTriangle) {

        /**
         * The join whose two segments meet at one pair of rail points - a corner that
         * mitred, or an end of the band, where only one segment reads the pair at all.
         *
         * @param left  the left rail's point
         * @param right the right rail's point
         * @return the join sharing that pair between its arriving and leaving segments
         */
        static BandJoin ofSharedRails(double[] left, double[] right) {
            return new BandJoin(left, right, left, right, List.of());
        }
    }

    /**
     * Several spans read as the one centreline they make up, without losing which span each
     * step of it came from.
     *
     * @param points        the spans' points laid end to end, each distinct from the one
     *                      before it
     * @param spanOfSegment the span walking the segment from {@code points[i]} to
     *                      {@code points[i + 1]}, at index {@code i}. Entries past the last
     *                      segment are unwritten, the list being sized before the repeated
     *                      points were dropped
     */
    private record SpannedCentreline(
        List<double[]> points,
        int[] spanOfSegment) {
    }
}
