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
 * <p>A fan or a lone convex inset fills only a convex shape; a fused faction bloc
 * or any rounded, dented outline is concave, and one with an enclave has a hole. To
 * fill such a region so the fill matches its stroked outline exactly, the outline's
 * rings are triangulated here once (at cache-build time, not per frame) and the
 * triangles replayed. The GLU tessellator does the heavy lifting - it handles
 * self-touching contours and holes via the positive winding rule - and this wrapper
 * only flattens whatever primitive kind it emits (triangles, fans, or strips) into
 * one uniform triangle list a caller can hand straight to {@code glVertex}.
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
     * soup - every six floats is one triangle - under the positive winding rule.
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
        runTessellation(contours, collector, false, GLU.GLU_TESS_WINDING_POSITIVE);
        return collector.toTriangleArray();
    }

    /**
     * Triangulates the intersection of two regions - the area both cover, clipped to
     * their overlap - into a flat {@code [x, y, x, y, ...]} triangle soup, so a fill
     * can be clamped to lie inside a second boundary rather than spilling past it.
     *
     * <p>Each operand is first resolved to its own clean, positive-winding boundary
     * (interior winding exactly +1, self-crossings dropped, holes cut back to 0), and
     * the two boundaries are then tessellated together under the "absolute winding
     * &gt;= 2" rule: a point both regions cover winds +2 and survives, a point only one
     * covers winds +1 and is dropped, and a point inside either region's hole is cut
     * below the threshold. Resolving each operand first is what makes the &gt;= 2 test
     * read as "inside both" - a single self-overlapping contour could otherwise reach
     * +2 on its own and survive where the other region does not reach.
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
            false,
            GLU.GLU_TESS_WINDING_ABS_GEQ_TWO);
        return collector.toTriangleArray();
    }

    /**
     * Outlines the intersection of two regions - the boundary loops of the area both
     * cover - so a stroke can be clamped to lie inside a second boundary rather than
     * spilling past it, matching the fill {@link #tessellateIntersectionToTriangles}
     * clips from the same two regions.
     *
     * <p>Both operands are resolved to their own clean positive-winding boundary first
     * and then run together under the "absolute winding &gt;= 2" rule, exactly as the
     * triangle form does; the tessellator emits the overlap's boundary loops instead of
     * its triangles. The overlap can come back as more than one loop where the clipping
     * boundary bites the region into disjoint pieces.
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
            true,
            GLU.GLU_TESS_WINDING_ABS_GEQ_TWO);
        return collector.loops();
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
        runTessellation(contours, collector, true, GLU.GLU_TESS_WINDING_POSITIVE);
        return collector.loops();
    }

    // Resolves both operands to their own clean positive-winding boundary and concatenates
    // them - the shared front half of both intersection entry points. The combined loops are
    // then run under the abs>=2 rule, where a point both regions cover winds twice and
    // survives; resolving each operand first is what makes that test read as "inside both",
    // since a single self-overlapping contour could otherwise reach +2 on its own.
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

    // Feeds every contour through a fresh GLU tessellator under {@code windingRule},
    // routing its callbacks to {@code collector}. When {@code boundaryOnly}, the
    // tessellator emits the resolved region's boundary loops rather than its triangles.
    //
    // The positive rule keeps only the region wound counter-clockwise overall (the
    // outer envelope): a counter-clockwise outer contour fills, a clockwise hole
    // contour cuts back to winding zero (so enclaves stay empty), and a ring that
    // crosses itself has its reversed sub-loop dropped rather than split off as a
    // stray loop with a matching notch in the survivor. So a self-touching outline
    // resolves to one clean envelope instead of an ear-plus-notch pair. The
    // absolute-winding >= 2 rule instead keeps only where two positive regions
    // overlap - their intersection - since a point both cover winds twice.
    private static void runTessellation(
            List<List<double[]>> contours,
            GLUtessellatorCallbackAdapter collector,
            boolean boundaryOnly,
            int windingRule) {

        var tessellator = GLU.gluNewTess();
        tessellator.gluTessCallback(GLU.GLU_TESS_BEGIN, collector);
        tessellator.gluTessCallback(GLU.GLU_TESS_VERTEX, collector);
        tessellator.gluTessCallback(GLU.GLU_TESS_END, collector);
        tessellator.gluTessCallback(GLU.GLU_TESS_COMBINE, collector);
        tessellator.gluTessProperty(GLU.GLU_TESS_WINDING_RULE, windingRule);
        if (boundaryOnly) {
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

    // Gathers the tessellator's output. It emits faces as GL_TRIANGLES,
    // GL_TRIANGLE_FAN, or GL_TRIANGLE_STRIP runs; this records the current run's kind
    // and, at its end, expands it into individual triangles so every consumer sees
    // one uniform triangle list regardless of how the tessellator grouped the faces.
    private static final class TriangleCollector extends GLUtessellatorCallbackAdapter {
        private final List<Float> triangles = new ArrayList<>();
        private final List<double[]> runVertices = new ArrayList<>();
        private int runKind;

        @Override
        public void begin(int kind) {
            runKind = kind;
            runVertices.clear();
        }

        @Override
        public void vertex(Object vertexData) {
            runVertices.add((double[]) vertexData);
        }

        @Override
        public void end() {
            if (runKind == GL11.GL_TRIANGLES) {
                for (var i = 0; i + 2 < runVertices.size(); i += TESS_TRIANGLE_STEP) {
                    addTriangle(i, i + 1, i + 2);
                }
            } else if (runKind == GL11.GL_TRIANGLE_FAN) {
                for (var i = 1; i + 1 < runVertices.size(); i++) {
                    addTriangle(0, i, i + 1);
                }
            } else if (runKind == GL11.GL_TRIANGLE_STRIP) {
                for (var i = 0; i + 2 < runVertices.size(); i++) {
                    // A strip alternates winding every other triangle; swap the first
                    // two indices on odd steps so all output triangles wind alike.
                    if (i % 2 == 0) {
                        addTriangle(i, i + 1, i + 2);
                    } else {
                        addTriangle(i + 1, i, i + 2);
                    }
                }
            }
        }

        // The tessellator may synthesise a vertex where contours cross; hand back a
        // 2D point at the interpolated location so it flows through like any other.
        @Override
        public void combine(double[] coords, Object[] data, float[] weight, Object[] outData) {
            outData[0] = new double[] {coords[0], coords[1]};
        }

        private void addTriangle(int a, int b, int c) {
            addVertex(runVertices.get(a));
            addVertex(runVertices.get(b));
            addVertex(runVertices.get(c));
        }

        private void addVertex(double[] vertex) {
            triangles.add((float) vertex[0]);
            triangles.add((float) vertex[1]);
        }

        private float[] toTriangleArray() {
            return GlVertexRuns.packFloats(triangles);
        }

        // A GL_TRIANGLES run is consumed three vertices at a time.
        private static final int TESS_TRIANGLE_STEP = 3;
    }

    // Gathers the tessellator's boundary-only output: one GL_LINE_LOOP run per
    // boundary contour of the resolved region. Each run's vertices are one closed
    // loop; loops shorter than a triangle enclose no area and are dropped.
    private static final class BoundaryCollector extends GLUtessellatorCallbackAdapter {
        private final List<List<double[]>> loops = new ArrayList<>();
        private List<double[]> currentLoop = new ArrayList<>();

        @Override
        public void begin(int kind) {
            currentLoop = new ArrayList<>();
        }

        @Override
        public void vertex(Object vertexData) {
            currentLoop.add((double[]) vertexData);
        }

        @Override
        public void end() {
            if (currentLoop.size() >= Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
                loops.add(currentLoop);
            }
        }

        // A crossing may synthesise a vertex; hand back a 2D point at the
        // interpolated location so it flows through like any other.
        @Override
        public void combine(double[] coords, Object[] data, float[] weight, Object[] outData) {
            outData[0] = new double[] {coords[0], coords[1]};
        }

        private List<List<double[]>> loops() {
            return loops;
        }
    }
}
