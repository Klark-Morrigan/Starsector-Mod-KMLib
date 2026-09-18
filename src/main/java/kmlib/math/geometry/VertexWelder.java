package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Assigns each distinct corner a small integer ID, treating two points within a tolerance as
 * the same corner.
 *
 * <p>What decides whether two edges meet. Every edge arriving at a shared corner reports it
 * from its own arithmetic, and two reports of it differ by a rounding whisker - so taken
 * literally the edges end near each other and never touch. Welding turns near into the same,
 * and the tolerance is how near counts. A caller that welds first can then compare corners as
 * exact integer IDs rather than by tolerance at every hop.
 *
 * <p><b>The first report of a corner is the one kept.</b> Averaging as more edges arrive would
 * move a corner after edges had already been welded to it, so an edge welded early would end
 * somewhere its own endpoint no longer is.
 *
 * <p>Points are bucketed by a grid cell of the tolerance's size, so a lookup scans only the
 * query point's cell and its eight neighbours - a straddling point still finds its match -
 * never the whole set.
 */
public final class VertexWelder {

    // Packs two signed 32-bit grid-cell indices into one 64-bit key: the x index in the high
    // half, the y index masked into the low half. Cell indices stay well within 32 bits for any
    // real map coordinate at a sub-unit tolerance, so the two halves never collide.
    private static final int CELL_KEY_X_SHIFT = 32;

    private static final long CELL_KEY_LOW_MASK = 0xffffffffL;

    private final List<double[]> canonicalPoints = new ArrayList<>();

    private final Map<Long, List<Integer>> pointsByCell = new HashMap<>();

    private final double tolerance;
    private final double toleranceSquared;

    /**
     * @param tolerance the largest gap between two reports still treated as one corner. Raised
     *                  to {@link Limits#MIN_EDGE_LENGTH} when smaller: a grid of no width has no
     *                  cell to look in, and two reports of one corner are never bit-identical
     *                  anyway, so an exact match is not a tolerance a caller can usefully ask for
     */
    public VertexWelder(double tolerance) {
        this.tolerance = Math.max(tolerance, Limits.MIN_EDGE_LENGTH);
        this.toleranceSquared = this.tolerance * this.tolerance;
    }

    /**
     * The ID of the canonical corner within tolerance of a point, registering a new one when
     * none exists yet.
     *
     * @param x the point's x
     * @param y the point's y
     * @return its ID, whether it joined a corner or started one
     */
    public int weld(double x, double y) {

        var cellX = (long) Math.floor(x / tolerance);
        var cellY = (long) Math.floor(y / tolerance);

        for (var dx = -1; dx <= 1; dx++) {
            for (var dy = -1; dy <= 1; dy++) {

                var bucket = pointsByCell.get(packCell(cellX + dx, cellY + dy));

                if (bucket == null) {
                    continue;
                }

                for (var id : bucket) {

                    var point = canonicalPoints.get(id);
                    var offsetX = point[0] - x;
                    var offsetY = point[1] - y;

                    if (offsetX * offsetX + offsetY * offsetY <= toleranceSquared) {
                        return id;
                    }
                }
            }
        }

        var newId = canonicalPoints.size();

        canonicalPoints
            .add(new double[] {x, y});
            
        pointsByCell
            .computeIfAbsent(packCell(cellX, cellY), cell -> new ArrayList<>())
            .add(newId);

        return newId;
    }

    /**
     * Every corner held, in ID order.
     *
     * @return the canonical points, so that a caller holding IDs from {@link #weld} can read
     *         back where each corner stands
     */
    public List<double[]> collectPoints() {
        return List.copyOf(canonicalPoints);
    }

    private static long packCell(long cellX, long cellY) {
        return (cellX & CELL_KEY_LOW_MASK) << CELL_KEY_X_SHIFT | cellY & CELL_KEY_LOW_MASK;
    }
}
