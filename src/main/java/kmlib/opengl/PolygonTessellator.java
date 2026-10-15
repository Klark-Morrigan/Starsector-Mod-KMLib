package kmlib.opengl;

import kmlib.math.geometry.Limits;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;
import org.lwjgl.util.glu.GLUtessellatorCallbackAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * Triangulates arbitrary polygon contours - concave, and with holes - into a flat
 * triangle soup, so a concave region can be filled with a single {@code
 * GL_TRIANGLES} run instead of the convex-only {@code GL_TRIANGLE_FAN}.
 *
 * <p>A fan or a lone convex inset fills only a convex shape; a rounded or dented
 * outline is concave, and one with an enclave has a hole. To fill such a region so
 * the fill matches its stroked outline exactly, the outline's rings are triangulated
 * here once (at cache-build time, not per frame) and the triangles replayed. The GLU
 * tessellator does the heavy lifting, and this wrapper only flattens whatever
 * primitive kind it emits (triangles, fans, or strips) into one uniform triangle list
 * a caller can hand straight to {@code glVertex}.
 *
 * <p>Winding decides solid from hole under the positive rule: a counter-clockwise
 * outer ring fills, a clockwise hole ring inside it cuts back to winding zero (so an
 * enclave stays empty), and where a ring crosses itself the reversed sub-loop is
 * dropped rather than left as a stray loop - so a self-touching outline resolves to
 * one clean envelope. Disjoint outer rings may share one call; each fills
 * independently.
 */
public final class PolygonTessellator {

    private PolygonTessellator() {
    }

    /**
     * Triangulates the given contours into a flat {@code [x, y, x, y, ...]} triangle
     * soup - every {@link GlVertexRuns#FLOATS_PER_TRIANGLE} floats is one triangle -
     * under the positive winding rule.
     *
     * @param contours the polygon's rings as {@code {x, y}} vertex lists; a
     *                 counter-clockwise outer ring fills, a clockwise hole ring cuts
     *                 back, and both may be mixed freely.
     *                 Rings of fewer than three vertices are ignored
     * @return the triangles as {@code [x1, y1, x2, y2, x3, y3, ...]}; empty when the
     *         contours enclose no area
     */
    public static float[] tessellateToTriangles(List<List<double[]>> contours) {

        var collector = new TriangleCollector();

        runTessellation(contours, collector, GLU.GLU_TESS_WINDING_POSITIVE);

        return collector.packTriangles();
    }

    /**
     * Triangulates the intersection of two regions - the area both cover - into a flat
     * {@code [x, y, x, y, ...]} triangle soup, so a fill can be clamped to lie inside a
     * second boundary rather than spilling past it.
     *
     * <p>Each region counts as the area {@link #tessellateToTriangles} would fill from it:
     * a hole in either is cut from the overlap, and a contour overlapping itself counts once.
     *
     * @param regionA one region's rings as {@code {x, y}} vertex lists; rings of fewer
     *                than three vertices are ignored
     * @param regionB the other region's rings, in the same convention
     * @return the triangles covering the two regions' overlap as {@code [x1, y1, x2,
     *         y2, x3, y3, ...]}; empty when the regions do not overlap
     */
    public static float[] tessellateIntersectionToTriangles(
            List<List<double[]>> regionA,
            List<List<double[]>> regionB) {

        var collector = new TriangleCollector();

        runTessellation(
            combineResolvedBoundaries(regionA, regionB),
            collector,
            GLU.GLU_TESS_WINDING_ABS_GEQ_TWO);

        return collector.packTriangles();
    }

    /**
     * Outlines the intersection of two regions - the boundary of the exact overlap
     * {@link #tessellateIntersectionToTriangles} fills from the same two regions - so a
     * stroke can be clamped to match that fill.
     *
     * <p>The overlap can come back as more than one loop where the second region bites the
     * first into disjoint pieces.
     *
     * @param regionA one region's rings as {@code {x, y}} vertex lists; rings of fewer
     *                than three vertices are ignored
     * @param regionB the other region's rings, in the same convention
     * @return one closed loop per boundary contour of the two regions' overlap, each a
     *         list of {@code {x, y}} vertices; empty when the regions do not overlap
     */
    public static List<List<double[]>> tessellateIntersectionToBoundaryLoops(
            List<List<double[]>> regionA,
            List<List<double[]>> regionB) {

        var collector = new BoundaryCollector();

        runTessellation(
            combineResolvedBoundaries(regionA, regionB),
            collector,
            GLU.GLU_TESS_WINDING_ABS_GEQ_TWO);

        return collector.getLoops();
    }

    /**
     * Resolves the given contours into their clean, non-self-intersecting boundary
     * loops under the positive winding rule - the outline of the exact region {@link
     * #tessellateToTriangles} would fill from the same contours.
     *
     * <p>Where a contour crosses itself (a shape inset past a narrow neck folds its
     * two sides through each other), stroking it raw would draw the crossing as a
     * stray loop. Running it through the tessellator in boundary-only mode instead
     * returns the region's true outline, so the stroke matches the fill and shows no
     * crossing. Holes come back as their own loops.
     *
     * @param contours the polygon's rings as {@code {x, y}} vertex lists; rings of
     *                 fewer than three vertices are ignored
     * @return one closed loop per boundary contour of the resolved region, each a
     *         list of {@code {x, y}} vertices; empty when the contours enclose no
     *         area
     */
    public static List<List<double[]>> tessellateToBoundaryLoops(List<List<double[]>> contours) {

        var collector = new BoundaryCollector();

        runTessellation(contours, collector, GLU.GLU_TESS_WINDING_POSITIVE);

        return collector.getLoops();
    }

    /**
     * Outlines the region a set of extents covers, clamped to lie within one enclosing loop -
     * so an outline drawn from the result never spills past a boundary that was already drawn.
     *
     * <p>The extents resolve together rather than one at a time: abutting extents wind into one
     * region under the positive rule, so the edge two neighbours share is dropped and the set
     * comes back as the outline of its combined reach.
     *
     * <p>The loop is taken in the flat {@code [x, y, x, y, ...]} run form a baked outline is
     * kept in. A null loop means there is nothing to clamp to, and the extents resolve to their
     * own combined boundary as {@link #tessellateToBoundaryLoops} would. Nothing here rounds or
     * reshapes a corner: the clamp only follows the loop it is given.
     *
     * @param extents       the regions to outline, as {@code {x, y}} vertex lists; rings of
     *                      fewer than three vertices are ignored
     * @param enclosingLoop the loop to clamp them to, as a {@code [x, y, x, y, ...]} run, or
     *                      null to leave them unclamped
     * @return one closed loop per boundary contour of what is left, each a list of
     *         {@code {x, y}} vertices; the clamp can bite the extents into more than one loop
     */
    public static List<List<double[]>> tessellateToBoundaryLoopsWithinLoop(
            List<List<double[]>> extents,
            float[] enclosingLoop) {

        if (enclosingLoop == null) {
            return tessellateToBoundaryLoops(extents);
        }
        return tessellateIntersectionToBoundaryLoops(
            extents,
            List.of(GlVertexRuns.unflattenVertices(enclosingLoop)));
    }

    // Resolves both operands to their own clean positive-winding boundary and concatenates
    // them, ready to run under the "absolute winding >= 2" rule: a point both regions cover
    // winds +2 and survives, a point only one covers winds +1 and is dropped, and a point in
    // either region's hole falls below the threshold. Resolving each operand first is what
    // makes ">= 2" read as "inside both" - a single self-overlapping contour could otherwise
    // reach +2 on its own and survive where the other region does not reach.
    private static List<List<double[]>> combineResolvedBoundaries(
            List<List<double[]>> regionA,
            List<List<double[]>> regionB) {

        var boundaryA = tessellateToBoundaryLoops(regionA);
        var boundaryB = tessellateToBoundaryLoops(regionB);
        var combined = new ArrayList<List<double[]>>(boundaryA.size() + boundaryB.size());

        combined.addAll(boundaryA);
        combined.addAll(boundaryB);

        return combined;
    }

    // Feeds every contour through a fresh GLU tessellator under windingRule. The collector
    // receives the output and also decides its form - triangles or boundary loops - so the two
    // cannot be paired wrongly.
    private static void runTessellation(
            List<List<double[]>> contours,
            RunCollector collector,
            int windingRule) {

        var tessellator = GLU.gluNewTess();

        tessellator.gluTessCallback(GLU.GLU_TESS_BEGIN, collector);
        tessellator.gluTessCallback(GLU.GLU_TESS_VERTEX, collector);
        tessellator.gluTessCallback(GLU.GLU_TESS_END, collector);
        tessellator.gluTessCallback(GLU.GLU_TESS_COMBINE, collector);
        tessellator.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, windingRule);

        if (collector.isBoundaryOnly()) {
            tessellator.gluTessProperty(GLU.GLU_TESS_BOUNDARY_ONLY, GL11.GL_TRUE);
        }

        tessellator.gluTessBeginPolygon(null);

        for (var contour : contours) {

            if (contour.size() < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
                continue;
            }

            tessellator.gluTessBeginContour();

            for (var vertex : contour) {

                // The tessellator both consumes the coordinates (3D, z = 0) and hands
                // back the vertex-data object in the vertex callback; the 2D point is
                // passed as that data so it re-emerges without a lookup.
                tessellator.gluTessVertex(
                    new double[] {vertex[0], vertex[1], 0.0},
                    0,
                    new double[] {vertex[0], vertex[1]});
            }

            tessellator.gluTessEndContour();
        }

        tessellator.gluTessEndPolygon();
        tessellator.gluDeleteTess();
    }

    // Gathers the tessellator's boundary-only output: one GL_LINE_LOOP run per
    // boundary contour of the resolved region. Each run's vertices are one closed
    // loop; loops shorter than a triangle enclose no area and are dropped.
    private static final class BoundaryCollector
            extends RunCollector {

        private final List<List<double[]>> loops = new ArrayList<>();

        @Override
        protected void collectRun(int kind, List<double[]> vertices) {

            if (vertices.size() >= Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
                loops.add(new ArrayList<>(vertices));
            }
        }

        @Override
        protected boolean isBoundaryOnly() {
            return true;
        }

        private List<List<double[]>> getLoops() {
            return loops;
        }
    }

    // Buffers the tessellator's output one run at a time and hands each finished run to the
    // subclass, which decides both the output's form and what to keep of each run.
    private abstract static class RunCollector
            extends GLUtessellatorCallbackAdapter {

        private final List<double[]> runVertices = new ArrayList<>();

        private int runKind;

        @Override
        public void begin(int kind) {

            runKind = kind;
            runVertices.clear();
        }

        // The tessellator may synthesise a vertex where contours cross; hand back a
        // 2D point at the interpolated location so it flows through like any other.
        @Override
        public void combine(double[] coords, Object[] data, float[] weight, Object[] outData) {
            outData[0] = new double[] {coords[0], coords[1]};
        }

        @Override
        public void end() {
            collectRun(runKind, runVertices);
        }

        @Override
        public void vertex(Object vertexData) {
            runVertices.add((double[]) vertexData);
        }

        // Receives one finished run. The vertex list is reused for the next run, so a subclass
        // that keeps it must copy it.
        protected abstract void collectRun(int kind, List<double[]> vertices);

        // Whether the tessellator should emit the resolved region's boundary loops rather than
        // its triangles.
        protected abstract boolean isBoundaryOnly();
    }

    // Gathers the tessellator's output. It emits faces as GL_TRIANGLES,
    // GL_TRIANGLE_FAN, or GL_TRIANGLE_STRIP runs; this expands each run into
    // individual triangles so every consumer sees one uniform triangle list
    // regardless of how the tessellator grouped the faces.
    private static final class TriangleCollector
            extends RunCollector {

        private final List<Float> triangles = new ArrayList<>();

        @Override
        protected void collectRun(int kind, List<double[]> vertices) {

            if (kind == GL11.GL_TRIANGLES) {

                for (var i = 0; i + 2 < vertices.size(); i += GlVertexRuns.VERTICES_PER_TRIANGLE) {
                    addTriangle(vertices, i, i + 1, i + 2);
                }
            } else if (kind == GL11.GL_TRIANGLE_FAN) {

                for (var i = 1; i + 1 < vertices.size(); i++) {
                    addTriangle(vertices, 0, i, i + 1);
                }
            } else if (kind == GL11.GL_TRIANGLE_STRIP) {

                for (var i = 0; i + 2 < vertices.size(); i++) {

                    // A strip alternates winding every other triangle; swap the first
                    // two indices on odd steps so all output triangles wind alike.
                    if (i % 2 == 0) {
                        addTriangle(vertices, i, i + 1, i + 2);
                    } else {
                        addTriangle(vertices, i + 1, i, i + 2);
                    }
                }
            }
        }

        @Override
        protected boolean isBoundaryOnly() {
            return false;
        }

        private void addTriangle(List<double[]> vertices, int a, int b, int c) {

            addVertex(vertices.get(a));
            addVertex(vertices.get(b));
            addVertex(vertices.get(c));
        }

        private void addVertex(double[] vertex) {

            triangles.add((float) vertex[0]);
            triangles.add((float) vertex[1]);
        }

        private float[] packTriangles() {
            return GlVertexRuns.packFloats(triangles);
        }
    }
}
