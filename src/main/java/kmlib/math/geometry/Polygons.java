package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Polygon operations for 2D geometry.
 *
 * <p>Pure 2D math: no rendering and no Starsector types, so it can be
 * reasoned about and verified on its own.
 */
public final class Polygons {
    // Edges shorter than this have no well-defined direction (and so no
    // normal); they are skipped rather than dividing by ~zero length.
    private static final double MIN_EDGE_LENGTH = 1e-6;
    // A polygon needs at least three vertices to enclose any area; fewer
    // collapses to a point or segment and insets to nothing.
    private static final int MIN_POLYGON_VERTICES = 3;

    private Polygons() {
    }

    /**
     * Insets a counter-clockwise convex polygon by {@code distance}, returning
     * the smaller closed polygon whose every edge sits {@code distance} inside
     * the matching original edge.
     *
     * <p>Computed by clipping the polygon against each edge's inward-shifted
     * line (a half-plane intersection), not by mitring adjacent offset edges.
     * That makes it robust on the shapes a Voronoi partition throws up: a sharp
     * or near-parallel corner gets cleanly bevelled instead of shooting out a
     * long miter spike, and over-insetting a thin cell shrinks it to nothing
     * rather than inverting it. Drawing the result as a line loop (and filling
     * it) renders one tidy province outline, with neighbours left a
     * {@code 2 * distance} channel apart.
     *
     * @param polygon  CCW convex polygon vertices as {x, y} pairs
     * @param distance inward inset applied to every edge
     * @return the inset polygon's vertices as {x, y} pairs in the same winding;
     *         empty when the polygon has fewer than three distinct vertices, or
     *         when {@code distance} is large enough to consume the whole cell
     */
    public static List<double[]> insetConvexPolygon(List<double[]> polygon, double distance) {
        var vertices = removeConsecutiveDuplicates(polygon);
        var count = vertices.size();
        if (count < MIN_POLYGON_VERTICES) {
            return new ArrayList<>();
        }

        // Clip the polygon by each edge's inward-offset line in turn. The
        // surviving region is the set of points at least {@code distance} inside
        // every edge - the inset cell.
        var inset = vertices;
        for (var i = 0; i < count && !inset.isEmpty(); i++) {
            var start = vertices.get(i);
            var end = vertices.get((i + 1) % count);
            var normal = computeInwardUnitNormal(start, end);
            if (normal == null) {
                continue;
            }
            // Shift a point on the edge inward by distance along that normal to
            // get a point on the clip line.
            var clipX = start[0] + normal[0] * distance;
            var clipY = start[1] + normal[1] * distance;
            inset = clipToHalfPlane(inset, clipX, clipY, normal[0], normal[1]);
        }
        return inset;
    }

    /**
     * Rounds a closed polygon's corners with a fixed radius, leaving the
     * straight edges between corners intact, and chamfers corners sharper than
     * {@code bevelBelowAngleRadians} with a flat cut instead.
     *
     * <p>At each vertex it steps back {@code radius} along both adjacent edges
     * and replaces the sharp corner with a short quadratic-bezier arc (the
     * vertex is the control point), sampled into {@code segmentsPerCorner}
     * segments. Because the cut is a fixed distance, not a fraction of the edge,
     * long edges stay long and only the corners soften - so a big cell does not
     * round off into a blob. The radius is clamped to half of each adjacent edge
     * so neighbouring corners never overlap, which also keeps the result convex
     * for a convex input.
     *
     * <p>A bezier arc still pinches to a near-point at an acute corner: with the
     * vertex as control point the curve barely pulls in from the apex, so a
     * sharp spike stays a spike. Any corner whose interior angle falls below
     * {@code bevelBelowAngleRadians} is therefore cut straight across (a chamfer
     * between the two step-back points), removing the spike outright while the
     * obtuse corners keep the smoother arc. A non-positive threshold disables
     * the chamfer and rounds every corner. Vertex cost is at most {@code corners
     * * (segmentsPerCorner + 1)}.
     *
     * @param polygon                closed polygon vertices as {x, y} pairs
     * @param radius                 corner radius in the polygon's units
     * @param segmentsPerCorner      arc segments per rounded corner; higher is
     *                               smoother
     * @param bevelBelowAngleRadians corners with an interior angle below this
     *                               are chamfered flat rather than rounded;
     *                               non-positive rounds every corner
     * @return the rounded polygon; a copy of the input (deduplicated) when it
     *         has fewer than three vertices, or when radius/segments are
     *         non-positive (nothing to round)
     */
    public static List<double[]> roundCorners(List<double[]> polygon, double radius,
            int segmentsPerCorner, double bevelBelowAngleRadians) {
        var vertices = removeConsecutiveDuplicates(polygon);
        var count = vertices.size();
        if (count < MIN_POLYGON_VERTICES || radius <= 0 || segmentsPerCorner < 1) {
            return vertices;
        }

        var rounded = new ArrayList<double[]>(count * (segmentsPerCorner + 1));
        for (var i = 0; i < count; i++) {
            var previous = vertices.get((i - 1 + count) % count);
            var corner = vertices.get(i);
            var next = vertices.get((i + 1) % count);
            // Clamp the cut to half of the shorter adjacent edge so two corners
            // sharing an edge cannot eat into each other.
            var cut = Math.min(radius,
                    0.5 * Math.min(Points.computeDistance(corner, previous),
                            Points.computeDistance(corner, next)));
            var arcStart = computePointToward(corner, previous, cut);
            var arcEnd = computePointToward(corner, next, cut);
            // Below the threshold a rounded arc would still read as a spike, so
            // cut straight across the corner: the two step-back points alone.
            if (bevelBelowAngleRadians > 0
                    && computeInteriorAngle(previous, corner, next) < bevelBelowAngleRadians) {
                rounded.add(arcStart);
                rounded.add(arcEnd);
                continue;
            }
            for (var step = 0; step <= segmentsPerCorner; step++) {
                var t = (double) step / segmentsPerCorner;
                rounded.add(computeQuadraticBezier(arcStart, corner, arcEnd, t));
            }
        }
        return rounded;
    }

    // A point {@code distance} from {@code from} toward {@code to}; returns from
    // itself when the two coincide (no direction).
    private static double[] computePointToward(double[] from, double[] to, double distance) {
        var deltaX = to[0] - from[0];
        var deltaY = to[1] - from[1];
        var length = Points.computeVectorLength(deltaX, deltaY);
        if (length < MIN_EDGE_LENGTH) {
            return new double[] {from[0], from[1]};
        }
        return new double[] {
                from[0] + deltaX / length * distance,
                from[1] + deltaY / length * distance,
        };
    }

    // Interior angle at {@code corner} in radians [0, PI], measured between the
    // edges to its two neighbours: PI is a straight pass-through and small
    // values are sharp spikes. Returns PI for a degenerate (zero-length) edge,
    // so such a corner is treated as straight and never chamfered.
    private static double computeInteriorAngle(double[] previous, double[] corner,
            double[] next) {
        var toPreviousX = previous[0] - corner[0];
        var toPreviousY = previous[1] - corner[1];
        var toNextX = next[0] - corner[0];
        var toNextY = next[1] - corner[1];
        var previousLength = Points.computeVectorLength(toPreviousX, toPreviousY);
        var nextLength = Points.computeVectorLength(toNextX, toNextY);
        if (previousLength < MIN_EDGE_LENGTH || nextLength < MIN_EDGE_LENGTH) {
            return Math.PI;
        }
        var cosine = (toPreviousX * toNextX + toPreviousY * toNextY)
                / (previousLength * nextLength);
        // Clamp against rounding drift just outside [-1, 1] before acos.
        return Math.acos(Math.max(-1.0, Math.min(1.0, cosine)));
    }

    // Quadratic bezier point at parameter t in [0, 1] from start to end, bending
    // toward control.
    private static double[] computeQuadraticBezier(double[] start, double[] control,
            double[] end, double t) {
        var oneMinusT = 1.0 - t;
        var a = oneMinusT * oneMinusT;
        var b = 2.0 * oneMinusT * t;
        var c = t * t;
        return new double[] {
                a * start[0] + b * control[0] + c * end[0],
                a * start[1] + b * control[1] + c * end[1],
        };
    }

    /**
     * Offsets every edge of a counter-clockwise convex polygon inward by
     * {@code distance} and returns the offset edges as independent segments.
     *
     * <p>Unlike shrinking the whole polygon, this never collapses: each edge
     * is shifted along its own inward normal and emitted as its own segment,
     * so a thin polygon yields near-overlapping segments rather than
     * vanishing. Drawing a cell's edges this way sits its border a uniform
     * {@code distance} inside its true outline - so two neighbours leave a
     * {@code 2 * distance} channel between them - at the cost of a small open
     * notch at each corner, where adjacent offset edges no longer meet.
     *
     * @param polygon  CCW convex polygon vertices as {x, y} pairs
     * @param distance inward offset applied to each edge
     * @return one segment per edge as {x1, y1, x2, y2}; empty for fewer than
     *         two vertices
     */
    public static List<double[]> offsetEdgesInward(List<double[]> polygon, double distance) {
        var segments = new ArrayList<double[]>();
        var count = polygon.size();
        if (count < 2) {
            return segments;
        }

        for (var i = 0; i < count; i++) {
            var a = polygon.get(i);
            var b = polygon.get((i + 1) % count);
            var normal = computeInwardUnitNormal(a, b);
            if (normal == null) {
                continue;
            }
            segments.add(new double[] {
                    a[0] + normal[0] * distance, a[1] + normal[1] * distance,
                    b[0] + normal[0] * distance, b[1] + normal[1] * distance,
            });
        }
        return segments;
    }

    /**
     * Clips a convex polygon to one half-plane: the points on the {@code normal}
     * side of the line through {@code (lineX, lineY)}. Sutherland-Hodgman against
     * a single edge, so it both keeps inside vertices and inserts the crossing
     * points where edges straddle the line, leaving the result closed.
     *
     * <p>The clip {@link #insetConvexPolygon} is built on (clip by an offset
     * edge); {@link LabelledPolygon#clipToHalfPlane} is the label-carrying
     * counterpart, and the two share their geometry through
     * {@link Points#computeSignedOffsetFromLine} and
     * {@link Points#computeCrossingPoint}. The {@code normal} need not be unit
     * length, since only the sign of the half-plane test matters.
     *
     * @param polygon the convex polygon to clip, as {x, y} pairs
     * @param lineX   x of a point on the clip line
     * @param lineY   y of a point on the clip line
     * @param normalX x of the normal pointing to the kept side
     * @param normalY y of the normal pointing to the kept side
     * @return the clipped polygon; empty when nothing lies on the kept side
     */
    static List<double[]> clipToHalfPlane(List<double[]> polygon,
            double lineX, double lineY, double normalX, double normalY) {
        var result = new ArrayList<double[]>();
        var count = polygon.size();
        // Sutherland-Hodgman edge walk; crossing math shared with
        // LabelledPolygon.clipToHalfPlane via Points.
        for (var i = 0; i < count; i++) {
            var current = polygon.get(i);
            var next = polygon.get((i + 1) % count);
            var currentOffset = Points.computeSignedOffsetFromLine(
                    current[0], current[1], lineX, lineY, normalX, normalY);
            var nextOffset = Points.computeSignedOffsetFromLine(
                    next[0], next[1], lineX, lineY, normalX, normalY);

            if (currentOffset >= 0) {
                result.add(current);
            }
            // Edge straddles the line: insert the crossing so the result stays
            // closed.
            if ((currentOffset >= 0) != (nextOffset >= 0)) {
                result.add(Points.computeCrossingPoint(current, next, currentOffset, nextOffset));
            }
        }
        return result;
    }

    // The inward unit normal of directed edge {@code a -> b} for a CCW polygon:
    // {@code (-edgeY, edgeX)} normalized, pointing to the polygon interior (the
    // left of the directed edge). Null when the edge is shorter than
    // {@link #MIN_EDGE_LENGTH} and so has no defined direction, so the caller
    // skips it rather than dividing by ~zero. Shared by the inset and edge-offset
    // passes, which both step inward along this normal.
    private static double[] computeInwardUnitNormal(double[] a, double[] b) {
        var edgeX = b[0] - a[0];
        var edgeY = b[1] - a[1];
        var length = Points.computeVectorLength(edgeX, edgeY);
        if (length < MIN_EDGE_LENGTH) {
            return null;
        }
        return new double[] {-edgeY / length, edgeX / length};
    }

    // Drops vertices that coincide with their predecessor (within the minimum
    // edge length), including the wrap from last back to first, so the inset
    // math never sees a zero-length edge with an undefined direction.
    private static List<double[]> removeConsecutiveDuplicates(List<double[]> polygon) {
        var cleaned = new ArrayList<double[]>();
        for (var vertex : polygon) {
            if (cleaned.isEmpty() || !isSamePoint(cleaned.get(cleaned.size() - 1), vertex)) {
                cleaned.add(vertex);
            }
        }
        var size = cleaned.size();
        if (size > 1 && isSamePoint(cleaned.get(0), cleaned.get(size - 1))) {
            cleaned.remove(size - 1);
        }
        return cleaned;
    }

    private static boolean isSamePoint(double[] a, double[] b) {
        return Points.computeDistance(a, b) < MIN_EDGE_LENGTH;
    }
}
