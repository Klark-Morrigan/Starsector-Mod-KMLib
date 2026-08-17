package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * A convex polygon whose every edge carries an integer label, closed under
 * half-plane clipping.
 *
 * <p>Each vertex stores the label of the edge leaving it toward the next vertex
 * in the ring, so an edge's identity survives the Sutherland-Hodgman clips that
 * reshape the polygon. Clipping against a half-plane keeps the label on every
 * surviving original edge and stamps the freshly cut edge with the clip's own
 * label - so a caller can learn, per output edge, which clip produced it.
 * {@link VoronoiCellBuilder} uses that to tag each cell edge with the
 * neighbouring site whose bisector cut it.
 *
 * <p>Immutable - {@link #clipToHalfPlane} returns a new polygon rather than
 * mutating this one.
 */
public final class LabelledPolygon {

    private final List<LabelledVertex> vertices;

    private LabelledPolygon(List<LabelledVertex> vertices) {
        this.vertices = vertices;
    }

    /**
     * Builds the regular polygon approximating {@code disk} with every edge labelled
     * {@code seedLabel} - the seed a clipped shape is carved out of. Counter-clockwise
     * winding.
     *
     * @param disk      the disk to approximate, supplying the centre, the distance from
     *                  it to each vertex, and the number of sides
     * @param seedLabel the label every edge starts with, until a clip cuts it
     * @return the seed polygon
     */
    public static LabelledPolygon createRegularPolygon(Disk disk, int seedLabel) {

        var segments = disk.segments();
        var vertices = new ArrayList<LabelledVertex>(segments);

        for (var i = 0; i < segments; i++) {

            var angle = Angles.FULL_TURN * i / segments;
            
            vertices.add(new LabelledVertex(
                new double[] {
                    disk.centreX() + disk.radius() * Math.cos(angle),
                    disk.centreY() + disk.radius() * Math.sin(angle),
                },
                seedLabel));
        }
        return new LabelledPolygon(vertices);
    }

    /**
     * Builds a polygon from explicit vertices, each carrying the label of the
     * edge leaving it toward the next vertex in the ring - the inverse of
     * {@link #getVertices()} paired with {@link #getEdgeLabels()}.
     *
     * <p>Where {@link #createRegularPolygon} can only seed a uniform regular
     * polygon, this seeds a clip from an arbitrary convex ring (a Voronoi cell,
     * say) with a per-edge label chosen up front, so a later clip can preserve
     * which edges were which. Winding is taken as given; the caller supplies a
     * counter-clockwise ring when the clip normals are to point inward.
     *
     * @param vertices   the ring's vertices as {x, y} pairs, in winding order
     * @param edgeLabels the label of each edge, parallel to {@code vertices}:
     *                   entry {@code i} labels the edge from vertex {@code i} to
     *                   vertex {@code (i + 1)} modulo the count
     * @return the labelled polygon
     * @throws IllegalArgumentException when the two arrays are not parallel
     */
    public static LabelledPolygon fromLabelledEdges(List<double[]> vertices, int[] edgeLabels) {

        if (vertices.size() != edgeLabels.length) {
            throw new IllegalArgumentException(
                "vertices and edgeLabels must be parallel: "
                    + vertices.size()
                    + " vs "
                    + edgeLabels.length);
        }
        var labelled = new ArrayList<LabelledVertex>(vertices.size());

        for (var i = 0; i < vertices.size(); i++) {
            labelled.add(new LabelledVertex(vertices.get(i), edgeLabels[i]));
        }
        return new LabelledPolygon(labelled);
    }

    /**
     * Clips this polygon to one half-plane: the points on the {@code normal} side
     * of the line through {@code (lineX, lineY)}, stamping the newly cut edge with
     * {@code clipLabel}.
     *
     * <p>Sutherland-Hodgman against a single edge - the one half-plane clip walk,
     * with its point geometry in {@link Lines#computeSignedOffsetFromLine} and
     * {@link Segments#computeCrossingPoint} and the label bookkeeping threaded
     * through on top. A whole-polygon inset that needs no per-edge labels drives
     * this with a single throwaway label ({@link PolygonOffsets#insetConvexPolygon}). A
     * surviving inside vertex keeps its
     * outgoing-edge label; a crossing made while leaving the kept side starts the
     * new clip-line edge and so takes {@code clipLabel}, while a crossing made
     * while re-entering resumes the original edge and keeps that edge's label.
     * The boundary's normal need not be unit length, since only the sign of the
     * half-plane test matters.
     *
     * @param boundary  the clip line and the normal pointing to its kept side
     * @param clipLabel the label stamped on the edge cut along the clip line
     * @return the clipped polygon; empty when nothing lies on the kept side
     */
    public LabelledPolygon clipToHalfPlane(HalfPlane boundary, int clipLabel) {

        var result = new ArrayList<LabelledVertex>();
        var count = vertices.size();

        // Sutherland-Hodgman edge walk; the half-plane side test lives in Lines and
        // the crossing point in Segment, so this method owns only the label
        // bookkeeping on top.
        for (var i = 0; i < count; i++) {

            var current = vertices.get(i);
            var next = vertices.get((i + 1) % count);
            var currentOffset = Lines.computeSignedOffsetFromLine(current.point(), boundary);
            var nextOffset = Lines.computeSignedOffsetFromLine(next.point(), boundary);

            if (currentOffset >= 0) {
                result.add(current);
            }
            if ((currentOffset >= 0) != (nextOffset >= 0)) {

                var crossing = Segments.computeCrossingPoint(
                    current.point(),
                    next.point(),
                    currentOffset,
                    nextOffset);

                var crossingLabel = currentOffset >= 0
                    ? clipLabel
                    : current.outgoingEdgeLabel();

                result.add(new LabelledVertex(crossing, crossingLabel));
            }
        }
        return new LabelledPolygon(result);
    }

    /**
     * @return true when clipping has consumed the whole polygon (no vertices left)
     */
    public boolean isEmpty() {
        return vertices.isEmpty();
    }

    /**
     * @return the polygon's vertices as {x, y} pairs in winding order; a fresh
     *         list, though the point arrays themselves are shared
     */
    public List<double[]> getVertices() {

        var points = new ArrayList<double[]>(vertices.size());
        for (var vertex : vertices) {
            points.add(vertex.point());
        }
        return points;
    }

    /**
     * @return each edge's label, parallel to {@link #getVertices()}: entry
     *         {@code i} is the label of the edge from vertex {@code i} to vertex
     *         {@code (i + 1)} modulo the vertex count
     */
    public int[] getEdgeLabels() {

        var labels = new int[vertices.size()];
        for (var i = 0; i < vertices.size(); i++) {
            labels[i] = vertices.get(i).outgoingEdgeLabel();
        }
        return labels;
    }

    // A polygon vertex paired with the label of the edge leaving it toward the
    // next vertex in the ring - how a clip threads each surviving edge's label
    // through the half-plane intersections.
    private record LabelledVertex(
        double[] point,
        int outgoingEdgeLabel) {
    }
}
