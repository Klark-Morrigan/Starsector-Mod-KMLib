package kmlib.opengl;

import java.util.List;

/**
 * The single source of truth for the flat {@code [x, y, x, y, ...]} float runs the
 * immediate-mode GL helpers consume, and the conversions that pack {@code {x, y}}
 * point geometry into them.
 *
 * <p>A producer flattens polygon or ring geometry here; a consumer such as
 * {@link GlLines#drawDashedSegments} strides through the result at
 * {@link #FLOATS_PER_VERTEX} per vertex. Owning the stride and the flatten
 * conversions in one place means both ends agree on the packing by construction
 * rather than by two matching magic numbers on opposite sides of the buffer.
 */
public final class GlVertexRuns {
    // A vertex is a 2D point packed as x then y, so every run is a
    // [x, y, x, y, ...] array. This is the stride from one vertex to the next and
    // the multiplier that sizes a run from its vertex count.
    public static final int FLOATS_PER_VERTEX = 2;

    // A GL_LINES segment is two endpoints, so its packed width is two vertices -
    // the stride a consumer steps by to read one segment at a time.
    public static final int FLOATS_PER_SEGMENT = 2 * FLOATS_PER_VERTEX;

    // The empty run shared by every draw element that packs nothing, so an omitted
    // element draws nothing without allocating a fresh empty array each time.
    public static final float[] NO_VERTICES = new float[0];

    // Conversions only; never instantiated.
    private GlVertexRuns() {
    }

    /**
     * Packs a polygon's {@code {x, y}} vertices into a flat
     * {@code [x, y, x, y, ...]} run, one vertex per point in order.
     *
     * @param polygon the vertices to pack, each a {@code {x, y}} pair
     * @return the packed run, {@code polygon.size() * FLOATS_PER_VERTEX} floats long
     */
    public static float[] flattenVertices(List<double[]> polygon) {
        var flat = new float[polygon.size() * FLOATS_PER_VERTEX];
        var index = 0;
        for (var vertex : polygon) {
            flat[index++] = (float) vertex[0];
            flat[index++] = (float) vertex[1];
        }
        return flat;
    }

    /**
     * Packs a list of loose floats into a run of exactly those floats in order - the
     * unbox step a producer that accumulates coordinates one at a time (the tessellator's
     * triangle collector, the hatch clipper) ends with before handing the run to GL.
     *
     * @param floats the run's floats in order
     * @return the packed run, {@code floats.size()} floats long
     */
    public static float[] packFloats(List<Float> floats) {
        var run = new float[floats.size()];
        for (var i = 0; i < run.length; i++) {
            run[i] = floats.get(i);
        }
        return run;
    }

    /**
     * Packs a closed ring into {@code GL_LINES} segment pairs - one segment per
     * edge, including the wrap from the last vertex back to the first - so the ring
     * strokes as a closed loop under a single {@code GL_LINES} pass.
     *
     * @param ring the ring's {@code {x, y}} vertices in order; the closing edge back
     *             to the first vertex is added implicitly
     * @return the packed run, {@code ring.size() * FLOATS_PER_SEGMENT} floats long
     */
    public static float[] flattenClosedLoopAsSegments(List<double[]> ring) {
        var count = ring.size();
        var flat = new float[count * FLOATS_PER_SEGMENT];
        var index = 0;
        for (var i = 0; i < count; i++) {
            var start = ring.get(i);
            var end = ring.get((i + 1) % count);
            flat[index++] = (float) start[0];
            flat[index++] = (float) start[1];
            flat[index++] = (float) end[0];
            flat[index++] = (float) end[1];
        }
        return flat;
    }
}
