package kmlib.math.geometry;

/**
 * Shared degenerate-shape thresholds for 2D geometry: the minimums below which a
 * shape stops enclosing real area and an operation must bail out or skip. The
 * single home for the thresholds that the polygon passes ({@link PolygonOffsets},
 * {@link PolygonSmoothing}, {@link PolygonRegions}), {@link EdgeRings}, the
 * {@code kmlib.opengl} tessellator, and their neighbours would otherwise each
 * restate. Two routines asking "is this shape degenerate?" against the same fact
 * must agree, so the fact lives once here rather than as a per-class literal.
 */
public final class Limits {
    // A closed 2D loop needs at least three distinct corners to bound any area; a
    // shorter loop collapses to a point or a back-and-forth segment and encloses
    // nothing.
    public static final int MIN_VERTICES_TO_ENCLOSE_AREA = 3;

    // A run needs three vertices before any of them is a corner: with two it is one
    // straight step, and with one it is a place. The same number as the loop threshold
    // above and a different fact - a closed ring turns at every vertex it has, an open
    // run turns only between its ends - so a pass over corners states which it means.
    public static final int MIN_VERTICES_TO_TURN = 3;

    // Edges shorter than this have no well-defined direction (and so no normal);
    // callers skip them rather than dividing by a near-zero length.
    public static final double MIN_EDGE_LENGTH = 1e-6;

    // Below this the chosen eigenvector is treated as the zero vector: the point
    // cloud has no direction of greater spread (a single point, coincident points,
    // or a perfectly isotropic spread), so a principal-axis fit falls back to the
    // x-axis. Far tighter than MIN_EDGE_LENGTH: it gates a covariance-derived
    // vector, not a raw edge, so it must not trip on merely small (but real) spread.
    public static final double MIN_AXIS_VECTOR_LENGTH = 1e-9;

    private Limits() {
    }
}
