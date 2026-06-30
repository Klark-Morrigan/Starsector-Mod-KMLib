package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds Voronoi cells for a set of 2D sites by half-plane clipping.
 *
 * <p>Each site owns the region of the plane closer to it than to any other
 * site. A cell is produced by starting from a regular polygon of a given
 * radius around the site and clipping it with the perpendicular bisector
 * between the site and each other site, keeping the half closer to the site.
 * The intersection of those half-planes (within the radius bound) is the
 * site's convex cell.
 *
 * <p>Geometry only: no rendering and no Starsector types, so the partition
 * can be reasoned about and verified on its own, independently of how it is
 * drawn. Cost is O(n^2) clips per site, O(n^3) overall, which is
 * sub-millisecond at Starsector's system counts (low hundreds) and is run
 * once and cached by the caller.
 */
public final class VoronoiCellBuilder {
    // Sides of the regular polygon that approximates each cell's max-radius
    // bound. High enough that the rounded frontier reads as a smooth curve
    // rather than a visible polygon.
    private static final int CELL_BOUND_SEGMENTS = 48;

    private VoronoiCellBuilder() {
    }

    /**
     * Partitions the plane around {@code sites} into one convex cell each,
     * with every cell bounded to {@code maxCellRadius} from its site.
     *
     * <p>The radius bound is a site's zone of control: where sites are close
     * the cells still meet along their perpendicular bisectors (a hard
     * border), but a site's reach into empty space is capped, so an isolated
     * site gets a rounded disc-like cell instead of claiming the void, and
     * frontier borders curve along the bound.
     *
     * @param sites         site positions as {x, y} pairs; order is preserved,
     *                      so the returned cell at index i belongs to site i
     * @param maxCellRadius the farthest a cell may extend from its site
     * @return one convex polygon per site, each a list of {x, y} vertices in
     *         winding order; an empty list when {@code sites} is empty
     */
    public static List<List<double[]>> buildCells(List<double[]> sites,
            double maxCellRadius) {
        var cells = new ArrayList<List<double[]>>();
        if (sites.isEmpty()) {
            return cells;
        }

        for (var site : sites) {
            cells.add(buildCell(site, sites, maxCellRadius));
        }
        return cells;
    }

    /**
     * Builds the single cell for {@code site}: its max-radius polygon clipped by
     * the perpendicular bisector against every other site. Lets a caller
     * recompute one site's cell without rebuilding the whole partition - the
     * basis for an incremental update, where only the cells near a changed site
     * are redone.
     *
     * <p>{@code site} must be one of the elements of {@code sites} (compared by
     * reference, so it is skipped as its own neighbour).
     *
     * @param site          the site to build the cell for; an element of
     *                      {@code sites}
     * @param sites         all sites in the partition
     * @param maxCellRadius the farthest the cell may extend from its site
     * @return the site's convex cell as {x, y} vertices in winding order
     */
    public static List<double[]> buildCell(double[] site, List<double[]> sites,
            double maxCellRadius) {
        var cell = regularPolygon(site, maxCellRadius);
        for (var other : sites) {
            if (other == site) {
                continue;
            }
            cell = clipToBisector(cell, site, other);
            if (cell.isEmpty()) {
                break;
            }
        }
        return cell;
    }

    // Clips a convex polygon to the half-plane of points at least as close to
    // {@code keep} as to {@code drop} - the keep side of the perpendicular
    // bisector of the two sites. The normal points toward the kept site, so a
    // non-negative half-plane test is the side to retain; the bisector passes
    // through the midpoint of the two sites.
    private static List<double[]> clipToBisector(List<double[]> polygon,
            double[] keep, double[] drop) {
        return Polygons.clipToHalfPlane(polygon,
                (keep[0] + drop[0]) * 0.5, (keep[1] + drop[1]) * 0.5,
                keep[0] - drop[0], keep[1] - drop[1]);
    }

    // Regular polygon of {@code radius} about {@code center}, the seed each
    // cell is carved out of. It both caps the cell's reach (the zone-of-
    // control bound) and rounds any frontier edge that the bisectors do not
    // cut. Counter-clockwise winding.
    private static List<double[]> regularPolygon(double[] center, double radius) {
        var polygon = new ArrayList<double[]>(CELL_BOUND_SEGMENTS);
        for (var i = 0; i < CELL_BOUND_SEGMENTS; i++) {
            var angle = 2.0 * Math.PI * i / CELL_BOUND_SEGMENTS;
            polygon.add(new double[] {
                    center[0] + radius * Math.cos(angle),
                    center[1] + radius * Math.sin(angle),
            });
        }
        return polygon;
    }
}
