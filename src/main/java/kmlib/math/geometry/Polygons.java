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
     * Insets a simple closed polygon inward by {@code distance}, mitring convex
     * corners and bevelling concave ones, and unlike {@link #insetConvexPolygon}
     * keeps the concavities.
     *
     * <p>Each edge is shifted {@code distance} toward the interior (the left of the
     * directed edge, as for a counter-clockwise ring). Where two shifted edges meet
     * at a convex corner they simply cross, and that miter point is the inset corner
     * - so the whole ring moves inward as one, keeping concavities that {@link
     * #insetConvexPolygon}'s half-plane clip would shear off. A concave (reflex)
     * corner is different: its two shifted edges diverge, and joining them by
     * extending to their crossing would shoot a long spike into the interior, which
     * reads as a stray loop. Such a corner is bevelled instead - the two shifted edge
     * ends are joined directly - which is the erosion's true corner. A convex corner
     * so sharp that its miter would spike past {@code miterSpikeLimit * distance} is
     * bevelled on the same grounds.
     *
     * <p>The winding drives the direction: a counter-clockwise ring shrinks, and a
     * clockwise ring (a hole traced with the solid on its outside) grows away from
     * the hole - both moving into the solid, so a hole keeps the same channel a
     * border does. No global self-intersection cleanup is done, so a ring narrower
     * than {@code 2 * distance} folds inside out (its winding flips); a caller
     * insetting past a shape's own scale should discard a result whose winding
     * flipped.
     *
     * @param polygon         simple closed polygon vertices as {x, y} pairs
     * @param distance        inward inset applied to every edge
     * @param miterSpikeLimit a convex miter whose point sits farther than this
     *                        multiple of {@code distance} from its corner is a spike
     *                        from a near-parallel corner and is bevelled instead;
     *                        larger keeps crisper points, smaller bevels sooner
     * @return the inset polygon's vertices in the same winding (a bevelled corner
     *         contributes two, a mitred corner one); empty when fewer than three
     *         distinct vertices remain
     */
    public static List<double[]> insetPolygonByMiter(List<double[]> polygon, double distance,
            double miterSpikeLimit) {
        var vertices = removeConsecutiveDuplicates(polygon);
        var count = vertices.size();
        if (count < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            return new ArrayList<>();
        }

        var inset = new ArrayList<double[]>(count);
        for (var i = 0; i < count; i++) {
            appendInsetCorner(inset, vertices.get((i - 1 + count) % count), vertices.get(i),
                    vertices.get((i + 1) % count), distance, miterSpikeLimit);
        }
        return inset;
    }

    // Appends the inset of one corner: the miter point for a convex corner within
    // the spike limit, otherwise the bevel (the two shifted edge ends). A reflex
    // corner always bevels, since its miter would spike into the interior; a
    // degenerate (zero-length) edge falls back to the one good offset, or the corner
    // itself when neither edge has a direction.
    private static void appendInsetCorner(List<double[]> inset, double[] previous,
            double[] corner, double[] next, double distance, double miterSpikeLimit) {
        var inboundNormal = computeInwardUnitNormal(previous, corner);
        var outboundNormal = computeInwardUnitNormal(corner, next);
        if (inboundNormal == null || outboundNormal == null) {
            var normal = inboundNormal == null ? outboundNormal : inboundNormal;
            inset.add(normal == null
                    ? new double[] {corner[0], corner[1]}
                    : new double[] {corner[0] + normal[0] * distance, corner[1] + normal[1] * distance});
            return;
        }

        var inboundPoint = new double[] {
                corner[0] + inboundNormal[0] * distance, corner[1] + inboundNormal[1] * distance};
        var outboundPoint = new double[] {
                corner[0] + outboundNormal[0] * distance, corner[1] + outboundNormal[1] * distance};
        // Left turn (positive cross) is convex for a CCW ring; a right turn is the
        // reflex corner whose miter would spike, so it bevels.
        var turn = (corner[0] - previous[0]) * (next[1] - corner[1])
                - (corner[1] - previous[1]) * (next[0] - corner[0]);
        if (turn > 0) {
            var miter = computeMiterVertex(corner, inboundNormal, outboundNormal,
                    inboundPoint, outboundPoint);
            if (Points.computeDistance(miter, corner) <= miterSpikeLimit * distance) {
                inset.add(miter);
                return;
            }
        }
        inset.add(inboundPoint);
        inset.add(outboundPoint);
    }

    // Where the inbound and outbound shifted edges cross - the miter point for a
    // convex corner. The lines run through the two shifted points along each edge's
    // direction (the inward normal rotated back to the edge); falls back to the
    // outbound shifted point when the edges are collinear and never cross.
    private static double[] computeMiterVertex(double[] corner, double[] inboundNormal,
            double[] outboundNormal, double[] inboundPoint, double[] outboundPoint) {
        // Each edge's direction is its inward normal turned 90 degrees, so a line
        // through the shifted point along it is the shifted edge; where the two
        // shifted edges cross is the miter. Collinear edges never cross - fall back
        // to the outbound shifted point.
        var crossing = Lines.intersectLines(inboundPoint, inboundNormal[1], -inboundNormal[0],
                outboundPoint, outboundNormal[1], -outboundNormal[0]);
        return crossing == null ? outboundPoint : crossing;
    }

    /**
     * Rounds a closed polygon's corners with a fixed radius, leaving the
     * straight edges between corners intact, and chamfers corners sharper than
     * {@code bevelBelowAngleRadians} with a flat cut instead.
     *
     * <p>At each vertex it steps back {@code radius} along both adjacent edges
     * and replaces the sharp corner with a true circular arc tangent to both
     * edges at those step-back points, sampled into {@code segmentsPerCorner}
     * segments. Because the cut is a fixed distance, not a fraction of the edge,
     * long edges stay long and only the corners soften - so a big cell does not
     * round off into a blob. The radius is clamped to half of each adjacent edge
     * so neighbouring corners never overlap, which also keeps the result convex
     * for a convex input. A circular arc (rather than a bezier with the vertex as
     * control point) rounds sharp corners uniformly instead of pinching back to a
     * near-point, so a spur is genuinely sanded off rather than left a spike.
     *
     * <p>A corner still sharper than {@code bevelBelowAngleRadians} is cut straight
     * across (a chamfer between the two step-back points) rather than arced, since
     * an arc that tight reads as a nick; a non-positive threshold disables the
     * chamfer and arcs every corner. Vertex cost is at most {@code corners *
     * (segmentsPerCorner + 1)}.
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
            appendCircularArc(rounded, previous, corner, next, arcStart, arcEnd,
                    segmentsPerCorner);
        }
        return rounded;
    }

    /**
     * Removes needle spikes and cusps from a closed ring: a vertex whose interior
     * angle is sharper than {@code maxCornerAngleRadians} and whose perpendicular
     * offset from the straight chord between its two neighbours is under
     * {@code maxSpikeHeight} is dropped, and its neighbours are spliced together.
     * Such a vertex is a thin protrusion (an outward needle) or nick (an inward
     * cusp), not real shape - the sliver a Voronoi bloc's inset throws up at a
     * pinched neck or a same-owner disc waist, which downstream rounding cannot sand
     * off because the spike's own edges are too short to step back along.
     *
     * <p>Both conditions must hold, which is what protects real geometry. A sharp
     * corner that juts far - a genuine peninsula tip - clears the height bar and
     * stays (rounding chamfers it instead); a gently curved run - a smooth arc
     * facet, whose corners are nearly straight - clears the angle bar and stays. Only
     * a corner that is both sharp and shallow, a sliver, is removed. Because the
     * interior angle is unsigned, an outward spike and an inward notch of equal
     * sharpness are treated alike, so the pass sands off both. Removal iterates to a
     * fixed point: splicing one vertex reshapes its neighbours' corners and can
     * expose a spike that hid behind it. It never drops below three vertices, so a
     * ring that is all sliver survives as a triangle for the caller's area check to
     * discard.
     *
     * @param polygon               closed polygon vertices as {x, y} pairs
     * @param maxSpikeHeight        a candidate corner is removed only if it sits
     *                              nearer than this to its neighbour chord; larger
     *                              sands off deeper protrusions. Non-positive
     *                              disables the pass
     * @param maxCornerAngleRadians only corners with an interior angle below this are
     *                              candidates; larger treats gentler corners as
     *                              spikes. Non-positive disables the pass
     * @return the cleaned ring; a copy of the input (deduplicated) when it has fewer
     *         than three vertices or either threshold is non-positive
     */
    public static List<double[]> removeSpikes(List<double[]> polygon, double maxSpikeHeight,
            double maxCornerAngleRadians) {
        var vertices = removeConsecutiveDuplicates(polygon);
        if (vertices.size() < Limits.MIN_VERTICES_TO_ENCLOSE_AREA
                || maxSpikeHeight <= 0 || maxCornerAngleRadians <= 0) {
            return vertices;
        }

        // Splicing out one spike reshapes the corners on either side, which can turn
        // a neighbour into a new spike, so re-scan until a full pass removes none.
        // Each removal drops a vertex and the loop stops at three, so it terminates.
        var removedAny = true;
        while (removedAny && vertices.size() > Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            removedAny = false;
            var count = vertices.size();
            for (var i = 0; i < count; i++) {
                var previous = vertices.get((i - 1 + count) % count);
                var corner = vertices.get(i);
                var next = vertices.get((i + 1) % count);
                if (computeInteriorAngle(previous, corner, next) < maxCornerAngleRadians
                        && Lines.computePerpendicularDistance(corner, previous, next)
                                < maxSpikeHeight) {
                    vertices.remove(i);
                    removedAny = true;
                    break;
                }
            }
        }
        return vertices;
    }

    /**
     * The signed area a closed ring encloses, by the shoelace sum: positive when the
     * ring winds counter-clockwise, negative when clockwise, and its magnitude the
     * enclosed area. So its sign reports winding and comparing two rings' signs
     * detects a fold.
     *
     * @param ring closed polygon vertices as {x, y} pairs
     * @return the signed area; zero for fewer than three vertices
     */
    public static double computeSignedArea(List<double[]> ring) {
        var twiceArea = 0.0;
        var count = ring.size();
        for (var i = 0; i < count; i++) {
            var current = ring.get(i);
            var next = ring.get((i + 1) % count);
            twiceArea += current[0] * next[1] - next[0] * current[1];
        }
        return twiceArea / 2.0;
    }

    // Appends the circular arc that rounds one corner: the arc tangent to both edges
    // at {@code arcStart} and {@code arcEnd}, sampled into {@code segments} steps.
    // Its centre is where the two edge-perpendiculars through those points meet; the
    // arc sweeps the short way between them, so it bulges toward the corner. Falls
    // back to the two step-back points alone when the edges are collinear (no corner
    // to round, so no centre).
    private static void appendCircularArc(List<double[]> out, double[] previous,
            double[] corner, double[] next, double[] arcStart, double[] arcEnd, int segments) {
        var center = computeArcCenter(previous, corner, next, arcStart, arcEnd);
        if (center == null) {
            out.add(arcStart);
            out.add(arcEnd);
            return;
        }
        var radius = Points.computeDistance(center, arcStart);
        var startAngle = Math.atan2(arcStart[1] - center[1], arcStart[0] - center[0]);
        var endAngle = Math.atan2(arcEnd[1] - center[1], arcEnd[0] - center[0]);
        // Sweep the short way (normalise to (-PI, PI]); that arc is the one on the
        // corner's side, so the rounded corner bulges toward the original vertex.
        var sweep = endAngle - startAngle;
        while (sweep <= -Math.PI) {
            sweep += 2.0 * Math.PI;
        }
        while (sweep > Math.PI) {
            sweep -= 2.0 * Math.PI;
        }
        for (var step = 0; step <= segments; step++) {
            var angle = startAngle + sweep * step / segments;
            out.add(new double[] {
                    center[0] + radius * Math.cos(angle),
                    center[1] + radius * Math.sin(angle),
            });
        }
    }

    // The centre of the arc rounding {@code corner}: the point equidistant from both
    // edges at the step-back points, found as the intersection of the perpendicular
    // to the inbound edge through {@code arcStart} and that to the outbound edge
    // through {@code arcEnd}. Null when those perpendiculars are parallel (the edges
    // are collinear, so there is no corner to round).
    private static double[] computeArcCenter(double[] previous, double[] corner, double[] next,
            double[] arcStart, double[] arcEnd) {
        // The centre lies on the perpendicular to each edge (the radius direction,
        // (-dy, dx)) through that edge's step-back point; where those two
        // perpendiculars cross is the centre. Parallel means collinear edges - no
        // corner - so there is no centre.
        return Lines.intersectLines(
                arcStart, -(corner[1] - previous[1]), corner[0] - previous[0],
                arcEnd, -(next[1] - corner[1]), next[0] - corner[0]);
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
     * @return one {@link Segment} per edge; empty for fewer than two vertices
     */
    public static List<Segment> offsetEdgesInward(List<double[]> polygon, double distance) {
        var segments = new ArrayList<Segment>();
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
            segments.add(new Segment(
                    a[0] + normal[0] * distance, a[1] + normal[1] * distance,
                    b[0] + normal[0] * distance, b[1] + normal[1] * distance));
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
