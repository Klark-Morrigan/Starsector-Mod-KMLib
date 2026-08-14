package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Offsets a polygon's edges inward or outward - the erosion and dilation family
 * behind province borders and territory channels.
 *
 * <p>Two offset strategies live here because a Voronoi partition needs both. The
 * half-plane clip ({@link #insetConvexPolygon}, {@link #insetSelectedEdges})
 * only ever removes area, so it is robust on sharp cells but cannot push an edge
 * outward. The miter path ({@link #insetPolygonByMiter}) shifts each edge then
 * joins the shifted edges, so it keeps concavities and, with a negative distance,
 * can carry an edge <em>outside</em> the original outline - the reach a frontier
 * border needs. {@link #offsetEdgesInward} sits between them, emitting one shifted
 * segment per edge without joining, so a thin cell yields segments rather than
 * vanishing.
 */
public final class PolygonOffsets {

    private PolygonOffsets() {
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
    public record SelectiveInset(
        List<double[]> vertices,
        boolean[] edgeIsInset) {

        /**
         * The result when nothing is left to draw - the inset consumed the polygon, or it
         * never had an interior to begin with.
         *
         * <p>Named because the pair of empties is a statement, not a construction: a caller
         * reading {@code SelectiveInset.nothingLeftToDraw()} learns the outcome, where a
         * caller reading {@code new SelectiveInset(new ArrayList<>(), new boolean[0])} has to
         * work out that two empty containers mean the shape is gone rather than that the
         * method gave up part way.
         *
         * @return the empty result
         */
        public static SelectiveInset nothingLeftToDraw() {
            return new SelectiveInset(new ArrayList<>(), new boolean[0]);
        }
    }

    /**
     * Insets a counter-clockwise convex polygon along only the edges flagged in
     * {@code insetEdge}, each by the one {@code distance}, leaving the rest on
     * their original lines - the uniform-distance convenience over the per-edge
     * {@link #insetSelectedEdges(List, double[])}.
     *
     * <p>Where {@link #insetConvexPolygon} pulls every edge inward by
     * {@code distance}, this pulls in only the selected ones. Two polygons that
     * share an un-inset edge therefore still meet exactly along it - so their
     * fills fuse with no seam - while their selected edges pull back to leave the
     * usual {@code 2 * distance} channel against everything else. A kept edge that
     * runs into a pulled-in edge is truncated at the offset line, so its end stays
     * within the inset region instead of poking out to the original corner.
     *
     * @param polygon   CCW convex polygon vertices as {x, y} pairs
     * @param insetEdge parallel to {@code polygon}: entry {@code i} is true to
     *                  inset the edge from vertex {@code i} to vertex
     *                  {@code (i + 1)}, false to leave it on its original line
     * @param distance  inward inset applied to each selected edge
     * @return the inset polygon with a per-edge inset-line flag, empty-or-drawable
     *         as {@link #insetSelectedEdges(List, double[])} describes
     * @throws IllegalArgumentException when {@code insetEdge} is not parallel to
     *         the polygon's edges
     */
    public static SelectiveInset insetSelectedEdges(
            List<double[]> polygon,
            boolean[] insetEdge,
            double distance) {

        if (insetEdge.length != polygon.size()) {
            throw new IllegalArgumentException(
                "insetEdge must be parallel to the polygon edges: "
                    + insetEdge.length
                    + " vs "
                    + polygon.size());
        }

        // Fold the mask-plus-one-scalar form into the per-edge form the primitive
        // takes: a flagged edge carries the scalar distance, an unflagged edge
        // carries 0, which the primitive reads as "leave this edge on its line".
        var edgeDistances = new double[insetEdge.length];

        for (var i = 0; i < insetEdge.length; i++) {
            edgeDistances[i] = insetEdge[i] ? distance : 0.0;
        }

        return insetSelectedEdges(polygon, edgeDistances);
    }

    /**
     * Insets a counter-clockwise convex polygon along each edge by its own inward
     * distance in {@code edgeDistances}, an entry of {@code 0} leaving that edge on
     * its original line, and reports which edges of the result lie on an inset line.
     *
     * <p>The per-edge generalisation of {@link #insetSelectedEdges(List, boolean[],
     * double)}: where the boolean form pulls every flagged edge in by one shared
     * scalar, this gives each edge its own inward distance, so a single cell can
     * pull one border to a near neighbour and another to a far one in the same pass.
     * Two polygons that share an edge left at {@code 0} still meet exactly along it -
     * so their fills fuse with no seam - while inset edges pull back to leave a
     * channel against everything else. A kept edge that runs into a pulled-in edge
     * is truncated at the offset line, so its end stays within the inset region
     * instead of poking out to the original corner.
     *
     * <p>Every distance is inward (a keep-out pull-in is always a larger inset, never
     * an outward push): the half-plane clip only ever removes area, so it stays valid
     * here where the miter path's outward reach is not needed. Built on the same
     * clipping as {@link #insetConvexPolygon}, carried through {@link LabelledPolygon}
     * so each output edge's origin (inset line versus kept edge) survives the clips.
     * An inset edge's original position lies fully outside its own inward-offset line,
     * so clipping replaces it with the pulled-in edge; a {@code 0}-distance edge is
     * never used as a clip, so it stays on its line, cut only where another edge's
     * clip crosses it.
     *
     * @param polygon       CCW convex polygon vertices as {x, y} pairs
     * @param edgeDistances parallel to {@code polygon}: entry {@code i} is the inward
     *                      inset for the edge from vertex {@code i} to vertex
     *                      {@code (i + 1)}; {@code 0} leaves that edge on its line.
     *                      Distances are inward (non-negative); a non-positive entry
     *                      leaves the edge on its line
     * @return the inset polygon with a per-edge inset-line flag. Either empty (both
     *         lists) or a real polygon with area - never a degenerate sliver: it
     *         empties when the input has fewer than three vertices, the inset
     *         consumes it, or the clip collapses it to fewer than three distinct
     *         vertices (a point or line), so a caller can treat any non-empty
     *         result as directly drawable
     * @throws IllegalArgumentException when {@code edgeDistances} is not parallel to
     *         the polygon's edges
     */
    public static SelectiveInset insetSelectedEdges(
            List<double[]> polygon,
            double[] edgeDistances) {

        var count = polygon.size();

        if (edgeDistances.length != count) {
            throw new IllegalArgumentException(
                "edgeDistances must be parallel to the polygon edges: "
                    + edgeDistances.length
                    + " vs "
                    + count);
        }

        if (count < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            return SelectiveInset.nothingLeftToDraw();
        }

        // Labels that let the clips tell an edge pulled onto an inward-offset line
        // from one left on the original outline. Only their distinctness matters,
        // so any two unequal ints serve, and they stay local to the one method that
        // borrows the labelled-clip machinery.
        final var insetEdgeLabel = 1;
        final var keptEdgeLabel = 0;

        // Seed a labelled copy so the clips can tell a pulled-in edge from a kept
        // one, then clip only by the inset edges' own inward-offset lines. An edge
        // with a non-positive distance is left on its line (kept). Offset lines are
        // taken from the original geometry, so a later clip does not shift an
        // earlier one.
        var labels = new int[count];

        for (var i = 0; i < count; i++) {
            labels[i] = edgeDistances[i] > 0 ? insetEdgeLabel : keptEdgeLabel;
        }

        var working = LabelledPolygon.fromLabelledEdges(polygon, labels);

        for (var i = 0; i < count && !working.isEmpty(); i++) {

            if (edgeDistances[i] <= 0) {
                continue;
            }

            var line = computeInwardOffsetLine(
                polygon.get(i),
                polygon.get((i + 1) % count),
                edgeDistances[i]);

            if (line == null) {
                continue;
            }

            working = working.clipToHalfPlane(line, insetEdgeLabel);
        }

        // Normalise a clipped-away or collapsed result to empty, so a non-empty
        // return is always a real polygon with area a caller can fill and stroke
        // without re-checking. A clip can pull every border through the cell,
        // collapsing it to a point or line - fewer than three distinct vertices
        // even when the raw vertex count is not (coincident corners). Deduplicating
        // before the count test catches that; the vertices themselves are returned
        // as clipped so they stay parallel to the edge flags.
        var vertices = working.getVertices();

        if (Rings.removeConsecutiveDuplicates(vertices).size()
                < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {

            return SelectiveInset.nothingLeftToDraw();
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
    public static List<double[]> insetConvexPolygon(
            List<double[]> polygon,
            double distance) {

        var vertices = Rings.removeConsecutiveDuplicates(polygon);
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

            var line = computeInwardOffsetLine(
                vertices.get(i),
                vertices.get((i + 1) % count),
                distance);

            if (line == null) {
                continue;
            }

            working = working.clipToHalfPlane(line, 0);
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
     * bevelled for the same reason.
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
    public static List<double[]> insetPolygonByMiter(
            List<double[]> polygon,
            double distance,
            double miterSpikeLimit) {

        var vertices = Rings.removeConsecutiveDuplicates(polygon);
        var count = vertices.size();

        if (count < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            return new ArrayList<>();
        }

        var inset = new ArrayList<double[]>(count);

        for (var i = 0; i < count; i++) {

            // The whole-polygon inset shifts every edge the same distance, so both
            // of a corner's edges take the one scalar.
            appendInsetCorner(
                inset,
                vertices.get((i - 1 + count) % count),
                vertices.get(i),
                vertices.get((i + 1) % count),
                distance,
                distance,
                miterSpikeLimit);
        }
        return inset;
    }

    /**
     * Insets a simple closed polygon by a per-edge signed distance, mitring convex
     * corners and bevelling concave ones - the per-edge, sign-aware counterpart of
     * {@link #insetPolygonByMiter(List, double, double)}.
     *
     * <p>Each edge is shifted by its own entry in {@code edgeDistances} along the
     * inward normal of a counter-clockwise ring: a positive distance moves the edge
     * inward (as the scalar inset does), a negative distance moves it <em>outward</em>,
     * past the original outline. That outward push is the reason the miter path,
     * rather than the half-plane clip of {@link #insetConvexPolygon}, is used for
     * frontier borders: only shifting-then-mitring can carry an edge outside the
     * polygon, which a clip - it only ever removes area - cannot express.
     *
     * <p>Corner handling is the scalar method's: two shifted edges cross at a convex
     * corner's miter, a reflex corner bevels (its miter would spike inward), and a
     * convex miter that spikes past {@code miterSpikeLimit} times the larger of the
     * corner's two edge distances bevels too. Taking the larger magnitude as the
     * spike scale makes this reduce exactly to the scalar inset when every entry is
     * equal, and, because it uses the magnitude, catches an <em>outward</em> bulge
     * that spikes at a shared convex corner - two adjacent frontier edges both pushed
     * out - just as it catches an inward one, so such a corner bevels instead of
     * shooting a self-intersecting point far outside the ring.
     *
     * <p>Winding, hole growth, and the missing global self-intersection cleanup are
     * as for the scalar method: a caller insetting past a shape's own scale should
     * discard a result whose winding flipped.
     *
     * @param edgeDistances   parallel to {@code polygon}: entry {@code i} is the
     *                        signed distance for the edge from vertex {@code i} to
     *                        vertex {@code (i + 1)} - positive inward, negative
     *                        outward
     * @param polygon         simple closed polygon vertices as {x, y} pairs
     * @param miterSpikeLimit a convex miter farther than this multiple of the corner's
     *                        larger edge-distance magnitude is a spike and is bevelled
     * @return the inset polygon's vertices in the same winding (a bevelled corner
     *         contributes two, a mitred corner one); empty when fewer than three
     *         distinct vertices remain
     * @throws IllegalArgumentException when {@code edgeDistances} is not parallel to
     *         the polygon's edges
     */
    public static List<double[]> insetPolygonByMiter(
            List<double[]> polygon,
            double[] edgeDistances,
            double miterSpikeLimit) {

        if (edgeDistances.length != polygon.size()) {
            throw new IllegalArgumentException(
                "edgeDistances must be parallel to the polygon edges: "
                    + edgeDistances.length
                    + " vs "
                    + polygon.size());
        }

        // Dedup while carrying each surviving edge's distance with it, so dropping a
        // zero-length edge does not slide the per-edge distances out of step with
        // the vertices they offset (a mismatch the scalar path never risks).
        var survivors = Rings.findSurvivingVertices(polygon);
        var vertices = Rings.collectPointsAt(polygon, survivors.pointIndices());
        var distances = collectDistancesAt(edgeDistances, survivors.outgoingEdgeIndices());
        var count = vertices.size();

        if (count < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            return new ArrayList<>();
        }

        var inset = new ArrayList<double[]>(count);

        for (var i = 0; i < count; i++) {
            // The inbound edge (previous -> corner) is edge (i - 1); the outbound
            // edge (corner -> next) is edge i. Each carries its own signed distance.
            appendInsetCorner(
                inset,
                vertices.get((i - 1 + count) % count),
                vertices.get(i),
                vertices.get((i + 1) % count),
                distances[(i - 1 + count) % count],
                distances[i],
                miterSpikeLimit);
        }
        return inset;
    }

    /**
     * Removes the loops a polygon folds over itself into, keeping only what is wound the
     * same way as the polygon as a whole - the self-intersection cleanup the miter path
     * leaves to its caller.
     *
     * <p>Offsetting inward past a shape's own local width makes the two sides of a corner
     * cross, and the run of boundary between the crossings comes back wound against the
     * rest. {@link #insetPolygonByMiter} produces those rather than preventing them, because
     * preventing them is a global question and it works corner by corner: it can bevel a
     * miter that would spike, but it cannot see that two edges nowhere near each other in
     * the ring have swapped sides. The fold draws as a spur poking out of the shape, and it
     * survives every local check - the ring still tiles against its neighbours, still
     * encloses about the right area, and every corner of it is individually sound.
     *
     * <p>Offered separately rather than folded into the inset so that callers pinning the
     * existing behaviour keep it. Splicing a fold out shortens the ring, so this terminates.
     *
     * <p>Only crossings within {@code windowVertices} of each other along the ring are
     * considered. A fold is two sides of ONE corner, so its segments sit close together;
     * scanning every pair costs quadratically more to find the same folds. A caller who has
     * offset by more than the shape's own scale, where distant parts of a ring can cross,
     * should pass a window covering the ring.
     *
     * @param polygon        closed polygon vertices as {x, y} pairs
     * @param windowVertices how far apart along the ring two segments may be and still be
     *                       compared; non-positive compares every pair
     * @return the polygon with its reversed loops spliced out; the input when it has fewer
     *         than three vertices or nothing folds
     */
    public static List<double[]> removeReversedLoops(
            List<double[]> polygon,
            int windowVertices) {

        if (polygon.size() < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            return polygon;
        }

        var cleaned = new ArrayList<>(polygon);
        var windsPositive = PolygonRegions.computeSignedArea(cleaned) >= 0;

        while (spliceOneReversedLoop(cleaned, windsPositive, windowVertices)) {

            if (cleaned.size() < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
                return polygon;
            }
        }
        return cleaned;
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

    // Appends the inset of one corner from its two edges' signed distances (equal
    // for the scalar inset): the miter point for a convex corner within the spike
    // limit, otherwise the bevel (the two shifted edge ends). A reflex corner always
    // bevels, since its miter would spike into the interior; a degenerate
    // (zero-length) edge falls back to the one good offset, or the corner itself when
    // neither edge has a direction.
    // One splice per call, so the caller's loop stops when a whole pass finds nothing.
    private static boolean spliceOneReversedLoop(
            List<double[]> ring,
            boolean windsPositive,
            int windowVertices) {

        for (var first = 0; first < ring.size(); first++) {

            var last = windowVertices > 0
                ? Math.min(first + windowVertices, ring.size() - 1)
                : ring.size() - 1;

            for (var second = first + 2; second <= last; second++) {

                // The edge from the last vertex back to the first is an edge like any
                // other, so it is indexed and wrapped rather than left off the end - and
                // it is adjacent to edge zero, which share a vertex and cannot fold.
                if (first == 0 && second == ring.size() - 1) {
                    continue;
                }

                var crossing = Segment.intersectSegments(
                    ring.get(first),
                    ring.get((first + 1) % ring.size()),
                    ring.get(second),
                    ring.get((second + 1) % ring.size()));

                if (crossing == null) {
                    continue;
                }

                var loop = new ArrayList<double[]>();

                loop.add(crossing);
                loop.addAll(ring.subList(first + 1, second + 1));

                if (PolygonRegions.computeSignedArea(loop) >= 0 == windsPositive) {
                    continue;
                }

                // The fold collapses to the point its two sides met at, which is where a
                // miter would have put the corner had one been takeable.
                ring.subList(first + 1, second + 1).clear();
                ring.add(first + 1, crossing);

                return true;
            }
        }
        return false;
    }

    private static void appendInsetCorner(
            List<double[]> inset,
            double[] previous,
            double[] corner,
            double[] next,
            double inboundDistance,
            double outboundDistance,
            double miterSpikeLimit) {

        var inboundNormal = computeInwardUnitNormal(previous, corner);
        var outboundNormal = computeInwardUnitNormal(corner, next);

        if (inboundNormal == null || outboundNormal == null) {

            var normal = inboundNormal == null ? outboundNormal : inboundNormal;
            var distance = inboundNormal == null ? outboundDistance : inboundDistance;

            inset.add(normal == null
                ? new double[] {
                    corner[0],
                    corner[1]}
                : new double[] {
                    corner[0] + normal[0] * distance,
                    corner[1] + normal[1] * distance});

            return;
        }

        var inboundPoint = new double[] {
            corner[0] + inboundNormal[0] * inboundDistance,
            corner[1] + inboundNormal[1] * inboundDistance};

        var outboundPoint = new double[] {
            corner[0] + outboundNormal[0] * outboundDistance,
            corner[1] + outboundNormal[1] * outboundDistance};

        // Left turn (positive cross) is convex for a CCW ring; a right turn is the
        // reflex corner whose miter would spike, so it bevels.
        var turn = (corner[0] - previous[0]) * (next[1] - corner[1])
            - (corner[1] - previous[1]) * (next[0] - corner[0]);

        if (turn > 0) {
            var miter = computeMiterVertex(
                corner,
                inboundNormal,
                outboundNormal,
                inboundPoint,
                outboundPoint);

            // The spike scale is the larger of the two edges' offset magnitudes: it
            // reduces to the scalar case when they match, and, being a magnitude,
            // flags an outward (negative-distance) bulge that spikes past the corner
            // the same way it flags an inward one.
            var spikeScale = Math.max(
                Math.abs(inboundDistance),
                Math.abs(outboundDistance));

            if (Points.computeDistance(miter, corner) <= miterSpikeLimit * spikeScale) {

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
    private static double[] computeMiterVertex(
            double[] corner,
            double[] inboundNormal,
            double[] outboundNormal,
            double[] inboundPoint,
            double[] outboundPoint) {

        // Each edge's direction is its inward normal turned 90 degrees, so a line
        // through the shifted point along it is the shifted edge; where the two
        // shifted edges cross is the miter. Collinear edges never cross - fall back
        // to the outbound shifted point.
        var crossing = Lines.intersectLines(
            inboundPoint,
            inboundNormal[1],
            -inboundNormal[0],
            outboundPoint,
            outboundNormal[1],
            -outboundNormal[0]);

        return crossing == null ? outboundPoint : crossing;
    }

    // The distances of the edges at {@code indices}, in that order - the per-edge half
    // of a dedup, gathered through the surviving vertices' outgoing edges so a distance
    // never parts company with the edge it offsets.
    private static double[] collectDistancesAt(double[] edgeDistances, int[] indices) {

        var distances = new double[indices.length];

        for (var i = 0; i < indices.length; i++) {
            distances[i] = edgeDistances[indices[i]];
        }
        return distances;
    }

    // The inward-offset clip line of directed edge {@code a -> b}, pulled
    // {@code distance} into a CCW polygon's interior - the point and normal a
    // half-plane clip takes to keep only the points at least {@code distance}
    // inside that edge. Null when the edge is too short to have a direction, so a
    // caller skips it. The single source of the inset's "edge -> clip line" step,
    // shared by the whole-polygon {@link #insetConvexPolygon} and the per-edge
    // {@link #insetSelectedEdges}, which differ only in which edges they feed it
    // and whether they track the cut edge's label.
    private static HalfPlane computeInwardOffsetLine(double[] a, double[] b, double distance) {

        var normal = computeInwardUnitNormal(a, b);

        if (normal == null) {
            return null;
        }

        return new HalfPlane(
            a[0] + normal[0] * distance,
            a[1] + normal[1] * distance,
            normal[0],
            normal[1]);
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
        var unitEdge = Points.computeUnitVector(edgeX, edgeY, Limits.MIN_EDGE_LENGTH);

        if (unitEdge == null) {
            return null;
        }
        
        // Rotate the unit edge 90 degrees left (x, y) -> (-y, x) to face the CCW
        // polygon's interior, which lies to the left of the directed edge.
        return new double[] {-unitEdge[1], unitEdge[0]};
    }
}
