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
        List<List<double[]>> cells = new ArrayList<>();
        if (sites.isEmpty()) {
            return cells;
        }

        for (double[] site : sites) {
            cells.add(buildCell(site, sites, maxCellRadius));
        }
        return cells;
    }

    // One cell: the site's max-radius polygon clipped by the bisector against
    // every other site. Stops early if clipping ever empties the polygon.
    private static List<double[]> buildCell(double[] site, List<double[]> sites,
            double maxCellRadius) {
        List<double[]> cell = regularPolygon(site, maxCellRadius);
        for (double[] other : sites) {
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

    // Sutherland-Hodgman clip of a convex polygon against the half-plane of
    // points at least as close to {@code keep} as to {@code drop} - the keep
    // side of the perpendicular bisector of the two sites.
    private static List<double[]> clipToBisector(List<double[]> polygon,
            double[] keep, double[] drop) {
        // Half-plane test: dot(p - midpoint, keep - drop) >= 0. The normal
        // points toward the kept site, so a non-negative value is the side
        // to retain.
        double normalX = keep[0] - drop[0];
        double normalY = keep[1] - drop[1];
        double midX = (keep[0] + drop[0]) * 0.5;
        double midY = (keep[1] + drop[1]) * 0.5;

        List<double[]> result = new ArrayList<>();
        int count = polygon.size();
        for (int i = 0; i < count; i++) {
            double[] current = polygon.get(i);
            double[] next = polygon.get((i + 1) % count);
            double currentDist = (current[0] - midX) * normalX
                    + (current[1] - midY) * normalY;
            double nextDist = (next[0] - midX) * normalX
                    + (next[1] - midY) * normalY;

            if (currentDist >= 0) {
                result.add(current);
            }
            // Edge straddles the bisector: insert the crossing point so the
            // clipped polygon stays closed.
            if ((currentDist >= 0) != (nextDist >= 0)) {
                double crossFraction = currentDist / (currentDist - nextDist);
                result.add(new double[] {
                        current[0] + crossFraction * (next[0] - current[0]),
                        current[1] + crossFraction * (next[1] - current[1]),
                });
            }
        }
        return result;
    }

    // Regular polygon of {@code radius} about {@code center}, the seed each
    // cell is carved out of. It both caps the cell's reach (the zone-of-
    // control bound) and rounds any frontier edge that the bisectors do not
    // cut. Counter-clockwise winding.
    private static List<double[]> regularPolygon(double[] center, double radius) {
        List<double[]> polygon = new ArrayList<>(CELL_BOUND_SEGMENTS);
        for (int i = 0; i < CELL_BOUND_SEGMENTS; i++) {
            double angle = 2.0 * Math.PI * i / CELL_BOUND_SEGMENTS;
            polygon.add(new double[] {
                    center[0] + radius * Math.cos(angle),
                    center[1] + radius * Math.sin(angle),
            });
        }
        return polygon;
    }
}
