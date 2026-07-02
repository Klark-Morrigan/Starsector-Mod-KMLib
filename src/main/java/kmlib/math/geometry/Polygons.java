package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Polygon operations for 2D geometry.
 */
public final class Polygons {

    private Polygons() {
    }

    // One edge's inward-offset clip line: a point on the line and the unit normal
    // pointing to the kept (interior) side - exactly the four values a half-plane
    // clip takes. A named, immutable tuple so a clip call reads by role rather than
    // by array index.
    private record OffsetLine(double pointX, double pointY, double normalX, double normalY) {
    }

    /**
     * The result of {@link #insetSelectedEdges}: the inset polygon and, per edge,
     * whether that edge lies on an inward-offset line (a pulled-in border) rather
     * than on an original, un-inset edge.
     *
     * @param vertices    the inset polygon's vertices as {x, y} pairs, in winding
     *                    order
     * @param edgeIsInset parallel to {@code vertices}: entry {@code i} is true when
     *                    the edge from vertex {@code i} to vertex {@code (i + 1)}
     *                    modulo the count lies on an inset line
     */
    public record SelectiveInset(List<double[]> vertices, boolean[] edgeIsInset) {
    }

    /**
     * Insets a counter-clockwise convex polygon along only the edges flagged in
     * {@code insetEdge}, leaving the rest on their original lines, and reports
     * which edges of the result lie on an inset line.
     *
     * <p>Where {@link #insetConvexPolygon} pulls every edge inward by
     * {@code distance}, this pulls in only the selected ones. Two polygons that
     * share an un-inset edge therefore still meet exactly along it - so their
     * fills fuse with no seam - while their selected edges pull back to leave the
     * usual {@code 2 * distance} channel against everything else. A kept edge that
     * runs into a pulled-in edge is truncated at the offset line, so its end stays
     * within the inset region instead of poking out to the original corner.
     *
     * <p>Built on the same half-plane clipping as {@link #insetConvexPolygon},
     * carried through {@link LabelledPolygon} so each output edge's origin (inset
     * line versus kept edge) survives the clips. A selected edge's original
     * position lies fully outside its own inward-offset line, so clipping replaces
     * it with the pulled-in edge; an un-selected edge is never used as a clip, so
     * it stays on its line, cut only where another edge's clip crosses it.
     *
     * @param polygon   CCW convex polygon vertices as {x, y} pairs
     * @param insetEdge parallel to {@code polygon}: entry {@code i} is true to
     *                  inset the edge from vertex {@code i} to vertex
     *                  {@code (i + 1)}, false to leave it on its original line
     * @param distance  inward inset applied to each selected edge
     * @return the inset polygon with a per-edge inset-line flag. Either empty (both
     *         lists) or a real polygon with area - never a degenerate sliver: it
     *         empties when the input has fewer than three vertices, the inset
     *         consumes it, or the clip collapses it to fewer than three distinct
     *         vertices (a point or line), so a caller can treat any non-empty
     *         result as directly drawable
     * @throws IllegalArgumentException when {@code insetEdge} is not parallel to
     *         the polygon's edges
     */
    public static SelectiveInset insetSelectedEdges(List<double[]> polygon, boolean[] insetEdge,
            double distance) {
        var count = polygon.size();
        if (insetEdge.length != count) {
            throw new IllegalArgumentException("insetEdge must be parallel to the polygon edges: "
                    + insetEdge.length + " vs " + count);
        }
        if (count < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            return new SelectiveInset(new ArrayList<>(), new boolean[0]);
        }

        // Labels that let the clips tell an edge pulled onto an inward-offset line
        // from one left on the original outline. Only their distinctness matters,
        // so any two unequal ints serve, and they stay local to the one method that
        // borrows the labelled-clip machinery.
        final var insetEdgeLabel = 1;
        final var keptEdgeLabel = 0;

        // Seed a labelled copy so the clips can tell a pulled-in edge from a kept
        // one, then clip only by the selected edges' inward-offset lines. Offset
        // lines are taken from the original geometry, so a later clip does not
        // shift an earlier one.
        var labels = new int[count];
        for (var i = 0; i < count; i++) {
            labels[i] = insetEdge[i] ? insetEdgeLabel : keptEdgeLabel;
        }
        var working = LabelledPolygon.fromLabelledEdges(polygon, labels);
        for (var i = 0; i < count && !working.isEmpty(); i++) {
            if (!insetEdge[i]) {
                continue;
            }
            var line = computeInwardOffsetLine(polygon.get(i), polygon.get((i + 1) % count),
                    distance);
            if (line == null) {
                continue;
            }
            working = working.clipToHalfPlane(line.pointX(), line.pointY(),
                    line.normalX(), line.normalY(), insetEdgeLabel);
        }

        // Normalise a clipped-away or collapsed result to empty, so a non-empty
        // return is always a real polygon with area a caller can fill and stroke
        // without re-checking. A clip can pull every border through the cell,
        // collapsing it to a point or line - fewer than three distinct vertices
        // even when the raw vertex count is not (coincident corners). Deduplicating
        // before the count test catches that; the vertices themselves are returned
        // as clipped so they stay parallel to the edge flags.
        var vertices = working.getVertices();
        if (removeConsecutiveDuplicates(vertices).size() < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            return new SelectiveInset(new ArrayList<>(), new boolean[0]);
        }
        var edgeLabels = working.getEdgeLabels();
        var edgeIsInset = new boolean[edgeLabels.length];
        for (var i = 0; i < edgeLabels.length; i++) {
            edgeIsInset[i] = edgeLabels[i] == insetEdgeLabel;
        }
        return new SelectiveInset(vertices, edgeIsInset);
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
        if (count < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            return new ArrayList<>();
        }

        // Clip the polygon by each edge's inward-offset line in turn. The
        // surviving region is the set of points at least {@code distance} inside
        // every edge - the inset cell. Routed through LabelledPolygon so the one
        // Sutherland-Hodgman walk lives there; a whole-polygon inset draws no
        // per-edge distinction, so every edge takes the same throwaway label and
        // the labels are dropped on the way out.
        var working = LabelledPolygon.fromLabelledEdges(vertices, new int[count]);
        for (var i = 0; i < count && !working.isEmpty(); i++) {
            var line = computeInwardOffsetLine(vertices.get(i), vertices.get((i + 1) % count),
                    distance);
            if (line == null) {
                continue;
            }
            working = working.clipToHalfPlane(
                    line.pointX(), line.pointY(), line.normalX(), line.normalY(), 0);
        }
        return working.getVertices();
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
        if (count < Limits.MIN_VERTICES_TO_ENCLOSE_AREA || radius <= 0 || segmentsPerCorner < 1) {
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
        if (length < Limits.MIN_EDGE_LENGTH) {
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
        if (previousLength < Limits.MIN_EDGE_LENGTH || nextLength < Limits.MIN_EDGE_LENGTH) {
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

    // The inward-offset clip line of directed edge {@code a -> b}, pulled
    // {@code distance} into a CCW polygon's interior - the point and normal a
    // half-plane clip takes to keep only the points at least {@code distance}
    // inside that edge. Null when the edge is too short to have a direction, so a
    // caller skips it. The single source of the inset's "edge -> clip line" step,
    // shared by the whole-polygon {@link #insetConvexPolygon} and the per-edge
    // {@link #insetSelectedEdges}, which differ only in which edges they feed it
    // and whether they track the cut edge's label.
    private static OffsetLine computeInwardOffsetLine(double[] a, double[] b, double distance) {
        var normal = computeInwardUnitNormal(a, b);
        if (normal == null) {
            return null;
        }
        return new OffsetLine(
                a[0] + normal[0] * distance, a[1] + normal[1] * distance, normal[0], normal[1]);
    }

    // The inward unit normal of directed edge {@code a -> b} for a CCW polygon:
    // {@code (-edgeY, edgeX)} normalized, pointing to the polygon interior (the
    // left of the directed edge). Null when the edge is shorter than
    // {@link Limits#MIN_EDGE_LENGTH} and so has no defined direction, so the caller
    // skips it rather than dividing by ~zero. Shared by the inset and edge-offset
    // passes, which both step inward along this normal.
    private static double[] computeInwardUnitNormal(double[] a, double[] b) {
        var edgeX = b[0] - a[0];
        var edgeY = b[1] - a[1];
        var length = Points.computeVectorLength(edgeX, edgeY);
        if (length < Limits.MIN_EDGE_LENGTH) {
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
        return Points.computeDistance(a, b) < Limits.MIN_EDGE_LENGTH;
    }
}
