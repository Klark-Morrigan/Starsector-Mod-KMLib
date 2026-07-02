package kmlib.math.geometry;

/**
 * Shared degenerate-shape thresholds for 2D geometry: the minimums below which a
 * shape stops enclosing real area and an operation must bail out or skip. The
 * single home for the thresholds that {@link Polygons}, {@link EdgeRings}, the
 * {@code kmlib.opengl} tessellator, and their neighbours would otherwise each
 * restate. Two routines asking "is this shape degenerate?" against the same fact
 * must agree, so the fact lives once here rather than as a per-class literal.
 */
public final class Limits {
    // A closed 2D loop needs at least three distinct corners to bound any area; a
    // shorter loop collapses to a point or a back-and-forth segment and encloses
    // nothing.
    public static final int MIN_VERTICES_TO_ENCLOSE_AREA = 3;

    // Edges shorter than this have no well-defined direction (and so no normal);
    // callers skip them rather than dividing by a near-zero length.
    public static final double MIN_EDGE_LENGTH = 1e-6;

    private Limits() {
    }
}
