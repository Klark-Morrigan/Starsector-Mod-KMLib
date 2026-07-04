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
 * <p>Cost is O(n^2) clips per site, O(n^3) overall, which is
 * sub-millisecond at Starsector's system counts (low hundreds) and is run
 * once and cached by the caller.
 */
public final class VoronoiCellBuilder {
    /**
     * Edge label for a cell edge that came from the max-radius bound rather than
     * a neighbour's bisector - a frontier into empty space, with no system on the
     * far side. Distinguishable from any real site index, which are non-negative.
     */
    public static final int BOUND_EDGE = -1;

    /**
     * Default sides of the regular polygon that approximates each cell's max-radius
     * bound, used by the builders that take no explicit count. High enough that the
     * rounded frontier reads as a smooth curve rather than a visible polygon, and
     * that two neighbouring cells' bound chords meet their shared bisector within a
     * hair of the same point - so a consumer chaining adjacent cells' frontier edges
     * into one outline finds them effectively coincident rather than separated by a
     * visible chord-vs-arc gap.
     *
     * <p>Every segment is a vertex on each frontier cell, so a caller that needs to
     * trade a slightly faceted frontier for fewer vertices (a rendering cost) passes
     * its own lower count to the builders that accept one.
     */
    public static final int DEFAULT_CELL_BOUND_SEGMENTS = 48;

    private VoronoiCellBuilder() {
    }

    /**
     * A site's convex cell together with, per edge, the neighbouring site that
     * produced it - the raw material for a cell-adjacency graph.
     *
     * <p>{@code vertices} are the cell corners in winding order, identical to
     * {@link #buildCell}'s output. {@code edgeNeighbourSiteIndices} runs parallel
     * to the edges: entry {@code i} is the index (into the {@code sites} list the
     * cell was built from) of the site whose perpendicular bisector cut edge
     * {@code i} - the segment from vertex {@code i} to vertex {@code (i + 1)}
     * modulo the vertex count - or {@link #BOUND_EDGE} when that edge came from
     * the max-radius bound rather than a neighbour. Two sites are adjacent
     * exactly when each lists the other here, so this is the adjacency graph
     * region merging is built on.
     */
    public record LabelledCell(List<double[]> vertices, int[] edgeNeighbourSiteIndices) {
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
        return buildCells(sites, maxCellRadius, DEFAULT_CELL_BOUND_SEGMENTS);
    }

    /**
     * As {@link #buildCells(List, double)}, but with the frontier resolution under
     * caller control: {@code boundSegments} sets the sides of the seed polygon each
     * cell's max-radius bound is approximated by, and so the vertex count of every
     * frontier cell. Lower is coarser but cheaper to render; see
     * {@link #DEFAULT_CELL_BOUND_SEGMENTS}.
     *
     * @param sites         site positions as {x, y} pairs; order is preserved,
     *                      so the returned cell at index i belongs to site i
     * @param maxCellRadius the farthest a cell may extend from its site
     * @param boundSegments sides of the regular polygon approximating each cell's
     *                      max-radius bound; higher is smoother, lower has fewer
     *                      vertices
     * @return one convex polygon per site, each a list of {x, y} vertices in
     *         winding order; an empty list when {@code sites} is empty
     */
    public static List<List<double[]>> buildCells(List<double[]> sites,
            double maxCellRadius, int boundSegments) {
        var cells = new ArrayList<List<double[]>>();
        if (sites.isEmpty()) {
            return cells;
        }

        for (var site : sites) {
            cells.add(buildCell(site, sites, maxCellRadius, boundSegments));
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
        return buildCell(site, sites, maxCellRadius, DEFAULT_CELL_BOUND_SEGMENTS);
    }

    /**
     * As {@link #buildCell(double[], List, double)}, but with the frontier
     * resolution under caller control - see {@link #buildCells(List, double, int)}.
     *
     * @param site          the site to build the cell for; an element of
     *                      {@code sites}
     * @param sites         all sites in the partition
     * @param maxCellRadius the farthest the cell may extend from its site
     * @param boundSegments sides of the regular polygon approximating the cell's
     *                      max-radius bound; higher is smoother, lower has fewer
     *                      vertices
     * @return the site's convex cell as {x, y} vertices in winding order
     */
    public static List<double[]> buildCell(double[] site, List<double[]> sites,
            double maxCellRadius, int boundSegments) {
        return buildLabelledCell(indexOf(sites, site), sites, maxCellRadius, boundSegments)
                .vertices();
    }

    /**
     * Builds {@code site}'s cell and tags each of its edges with the neighbouring
     * site that produced it - {@link #buildCell} plus the adjacency information
     * the unlabelled form discards.
     *
     * <p>The clip step already knows the answer: every edge of the finished cell
     * lies either on the perpendicular bisector against one specific other site
     * (that site is the neighbour across the edge) or on the max-radius seed (a
     * frontier into empty space). This carries that identity out, so a caller can
     * build the cell-adjacency graph without re-deriving which cells touch.
     *
     * @param siteIndex     index of the site to build the cell for, into
     *                      {@code sites}
     * @param sites         all sites in the partition
     * @param maxCellRadius the farthest the cell may extend from its site
     * @return the site's cell with a neighbour-site index per edge; an empty cell
     *         (no vertices, no edges) when the site is fully clipped away
     */
    public static LabelledCell buildLabelledCell(int siteIndex, List<double[]> sites,
            double maxCellRadius) {
        return buildLabelledCell(siteIndex, sites, maxCellRadius, DEFAULT_CELL_BOUND_SEGMENTS);
    }

    /**
     * As {@link #buildLabelledCell(int, List, double)}, but with the frontier
     * resolution under caller control - see {@link #buildCells(List, double, int)}.
     * This is the seam a consumer tunes the frontier vertex count through, since a
     * lone or edge cell keeps every seed vertex the neighbours do not clip away.
     *
     * @param siteIndex     index of the site to build the cell for, into
     *                      {@code sites}
     * @param sites         all sites in the partition
     * @param maxCellRadius the farthest the cell may extend from its site
     * @param boundSegments sides of the regular polygon approximating the cell's
     *                      max-radius bound; higher is smoother, lower has fewer
     *                      vertices
     * @return the site's cell with a neighbour-site index per edge; an empty cell
     *         (no vertices, no edges) when the site is fully clipped away
     */
    public static LabelledCell buildLabelledCell(int siteIndex, List<double[]> sites,
            double maxCellRadius, int boundSegments) {
        var site = sites.get(siteIndex);
        // Seed the cell with a bounded polygon whose every edge is a frontier
        // (BOUND_EDGE), then let each neighbour's bisector clip it, stamping the
        // cut edge with that neighbour's index. What survives labels each edge
        // with the site across it, or BOUND_EDGE where the seed was never cut.
        var cell = LabelledPolygon.createRegularPolygon(
                site, maxCellRadius, boundSegments, BOUND_EDGE);
        for (var other = 0; other < sites.size(); other++) {
            if (other == siteIndex) {
                continue;
            }
            cell = clipToBisector(cell, site, sites.get(other), other);
            if (cell.isEmpty()) {
                break;
            }
        }
        return new LabelledCell(cell.getVertices(), cell.getEdgeLabels());
    }

    // Clips a cell to the half-plane of points at least as close to {@code keep}
    // as to {@code drop} - the keep side of the perpendicular bisector of the two
    // sites - tagging the newly cut edge with {@code dropIndex}, the site on the
    // far side of it. The normal points toward the kept site, so a non-negative
    // half-plane test is the side to retain; the bisector passes through the
    // midpoint of the two sites.
    private static LabelledPolygon clipToBisector(LabelledPolygon cell, double[] keep,
            double[] drop, int dropIndex) {
        return cell.clipToHalfPlane(
                (keep[0] + drop[0]) * 0.5, (keep[1] + drop[1]) * 0.5,
                keep[0] - drop[0], keep[1] - drop[1], dropIndex);
    }

    // Locates {@code site} in {@code sites} by reference, the identity the
    // unlabelled buildCell skips its own site by; an element of the list, so the
    // search always hits.
    private static int indexOf(List<double[]> sites, double[] site) {
        for (var i = 0; i < sites.size(); i++) {
            if (sites.get(i) == site) {
                return i;
            }
        }
        throw new IllegalArgumentException("site must be an element of sites");
    }
}
