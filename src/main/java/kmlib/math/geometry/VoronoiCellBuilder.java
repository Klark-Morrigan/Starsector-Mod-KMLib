package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.Comparator;
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
 * <p>The same clipping answers the local question too: {@link #splitPolygonAmongSites}
 * divides one given region among the sites around it, with the region taking the seed's
 * place as the bound.
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
     * rounded frontier reads as a smooth curve rather than a visible polygon.
     *
     * <p>It decides smoothness and nothing else. Where a frontier starts and stops -
     * the corner two neighbours share - is placed on the true bound rather than on
     * this polygon, so adjacent cells meet exactly at any count, and a consumer
     * chaining their frontier edges into one outline welds at rounding rather than at
     * a chord-versus-arc gap that grows as the count falls.
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
    public record LabelledCell(
        List<double[]> vertices,
        int[] edgeNeighbourSiteIndices) {
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
    public static List<List<double[]>> buildCells(
            List<double[]> sites,
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
    public static List<List<double[]>> buildCells(
            List<double[]> sites,
            double maxCellRadius,
            int boundSegments) {

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
    public static List<double[]> buildCell(
            double[] site,
            List<double[]> sites,
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
    public static List<double[]> buildCell(
            double[] site,
            List<double[]> sites,
            double maxCellRadius,
            int boundSegments) {

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
    public static LabelledCell buildLabelledCell(
            int siteIndex,
            List<double[]> sites,
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
    public static LabelledCell buildLabelledCell(
            int siteIndex,
            List<double[]> sites,
            double maxCellRadius,
            int boundSegments) {

        var site = sites.get(siteIndex);

        // Seed the cell with a bounded polygon whose every edge is a frontier (BOUND_EDGE),
        // then let each neighbour's bisector clip it, stamping the cut edge with that
        // neighbour's index. What survives labels each edge with the site across it, or
        // BOUND_EDGE where the seed was never cut.
        //
        // The seed is inscribed in the bound, so it falls short of it between its vertices
        // and the corners in that band come out moved or missing. Laying those corners back
        // on the bound is the pass that follows, and it is what keeps the bound's segment
        // count a question about how smooth an arc looks rather than about which corners the
        // map has.
        var cell = LabelledPolygon.createRegularPolygon(
            new Disk(site, maxCellRadius, boundSegments),
            BOUND_EDGE);

        for (var other = 0; other < sites.size(); other++) {

            if (other == siteIndex) {
                continue;
            }
            cell = clipToBisector(cell, site, sites.get(other), other);

            if (cell.isEmpty()) {
                break;
            }
        }
        return layBoundCorners(
            new LabelledCell(cell.getVertices(), cell.getEdgeLabels()),
            siteIndex,
            sites,
            maxCellRadius);
    }

    /**
     * Splits {@code polygon} among {@code sites}: the Voronoi partition of those
     * sites restricted to the polygon, so each site takes the part of the polygon
     * nearer to it than to any other site.
     *
     * <p>The same bisector clipping the whole-plane partition runs on, with the
     * polygon standing in for the max-radius seed as the bound the cells are carved
     * out of. Where {@link #buildCells} answers "which site owns each point of the
     * plane", this answers "how does one bounded region divide among the sites
     * around it" - a local question, asked of a region whose own site is not among
     * the ones dividing it.
     *
     * <p>The pieces are disjoint and cover the polygon by construction: a point
     * lands in exactly the piece of the site it is nearest, so the split can never
     * hand overlapping area to two sites. Coincident sites are the one exception
     * the nearest-site rule cannot decide, and each of them takes the full shared
     * region.
     *
     * @param polygon the convex region to divide, as {x, y} vertices in winding
     *                order
     * @param sites   the sites dividing it as {x, y} pairs; order is preserved, so
     *                the returned piece at index i belongs to site i
     * @return one convex piece per site, each a list of {x, y} vertices in the
     *         polygon's winding; empty for a site with no nearest region inside the
     *         polygon, and empty for every site when {@code polygon} encloses no
     *         area
     */
    public static List<List<double[]>> splitPolygonAmongSites(
            List<double[]> polygon,
            List<double[]> sites) {

        var pieces = new ArrayList<List<double[]>>(sites.size());
        var ring = Rings.removeConsecutiveDuplicates(polygon);
        var enclosesArea = ring.size() >= Limits.MIN_VERTICES_TO_ENCLOSE_AREA;

        for (var siteIndex = 0; siteIndex < sites.size(); siteIndex++) {

            pieces.add(enclosesArea
                ? computeNearestRegion(ring, sites, siteIndex)
                : new ArrayList<>());
        }
        return pieces;
    }

    // The part of {@code ring} nearer to site {@code siteIndex} than to any other:
    // the ring clipped by the perpendicular bisector against each of the others.
    // Mirrors buildLabelledCell with the ring as the seed in place of the
    // max-radius polygon - the bisector clip does label each cut edge with the site
    // across it, but a split's caller wants the piece's outline alone, so the
    // labels are dropped on the way out.
    private static List<double[]> computeNearestRegion(
            List<double[]> ring,
            List<double[]> sites,
            int siteIndex) {

        var site = sites.get(siteIndex);
        var region = LabelledPolygon.fromLabelledEdges(ring, new int[ring.size()]);

        for (var other = 0; other < sites.size(); other++) {

            if (other == siteIndex) {
                continue;
            }
            region = clipToBisector(region, site, sites.get(other), other);

            if (region.isEmpty()) {
                break;
            }
        }
        // A region clipped down to a sliver of one or two vertices bounds nothing;
        // report it as no region rather than as a degenerate polygon a consumer
        // would have to re-test before drawing.
        var vertices = region.getVertices();

        return vertices.size() < Limits.MIN_VERTICES_TO_ENCLOSE_AREA
            ? new ArrayList<>()
            : vertices;
    }

    // Every corner the bound gives this cell, put where it belongs and put back where the
    // seed cut it away.
    //
    // The seed is a polygon inscribed in the bound, so its flat sides fall short of the arcs
    // they stand for, by up to a chord's sagitta at the middle of each. Two things go wrong in
    // that band, and both concern the corner where a neighbour's border meets the bound - the
    // one point the two cells share there, and so the one point deciding whether their
    // frontiers meet or leave a gap between them.
    //
    // A corner the seed merely moved is moved back. A corner the seed cut away entirely is put
    // back, by breaking the span it should have stood on and running the boundary through it.
    // The second is the one that bites at a coarse bound: the band is widest there, a corner
    // falling inside it is gone from the ring altogether, and a corner that is not in the ring
    // cannot be moved onto anything.
    //
    // The cost is a dozen or so candidates per cell, each weighed only against the sites near
    // enough to have a claim - a fraction of the clipping already done, and it does not grow
    // with the bound's segment count.
    private static LabelledCell layBoundCorners(
            LabelledCell cell,
            int siteIndex,
            List<double[]> sites,
            double maxCellRadius) {

        var site = sites.get(siteIndex);
        var laid = placeBoundCornersExactly(cell, site, sites, maxCellRadius);

        if (laid.vertices().size() < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            return laid;
        }

        // Weighed against every site near enough to have a claim, NOT against the neighbours
        // the ring still names. A stretch of border the seed cut away took its label with it,
        // so the very corners worth putting back are the ones whose neighbour the ring no
        // longer mentions - and asking only the survivors would propose every corner except
        // those.
        var claimants = gatherClaimants(siteIndex, sites, maxCellRadius);
        var corners = new ArrayList<>(laid.vertices());
        var labels = readLabelList(laid.edgeNeighbourSiteIndices());

        for (var neighbour : claimants) {

            var across = sites.get(neighbour);

            for (var corner : crossBoundOnBorderWith(site, across, maxCellRadius)) {

                if (!isThisCells(corner, sites, claimants, maxCellRadius)
                        || isAlreadyInTheRing(corners, corner)) {
                    continue;
                }
                runBoundaryThrough(corners, labels, corner, neighbour, across, site);
            }
        }
        dropEmptySpans(corners, labels);

        return new LabelledCell(corners, readLabels(labels));
    }

    // Drops spans with no length, which are two corners standing in one place.
    //
    // Where three cells meet INSIDE the bound, the borders this cell shares with the other two
    // both stop at that one corner - so both are placed there, and the stretch of bound the
    // seed reported between them closes to nothing. Left in, that empty stretch says the cell
    // faces the void at a point where in truth its two borders simply meet, which is a corner
    // invented by how coarsely the bound was drawn. Dropped, the two borders meet at the one
    // corner, exactly as they do when the bound is drawn finely enough never to have split
    // them.
    //
    // The span that goes is the empty one, so the corner keeps what lies across the span
    // LEAVING it - the border it gives way to, rather than the nothing in between.
    private static void dropEmptySpans(List<double[]> corners, List<Integer> labels) {

        for (var index = corners.size() - 1; index >= 0 && corners.size() > 1; index--) {

            var next = (index + 1) % corners.size();

            if (Points.computeDistance(corners.get(index), corners.get(next))
                    >= Limits.MIN_EDGE_LENGTH) {
                continue;
            }
            labels.set(index, labels.get(next));
            corners.remove(next);
            labels.remove(next);
        }
    }

    // Breaks the span the corner should have stood on and runs the boundary through it.
    //
    // The span to break is the one the corner stands nearest: the corner was cut from the ring
    // by a seed side, so it lies just beyond the very span that replaced it. Each half of the
    // broken span then takes the label of whatever line it actually runs along - the border
    // shared with the neighbour, or whatever the span ran along before - read back off the
    // half's own middle rather than guessed from which half it is.
    private static void runBoundaryThrough(
            List<double[]> corners,
            List<Integer> labels,
            double[] corner,
            int neighbour,
            double[] across,
            double[] site) {

        var breaks = findNearestSpan(corners, corner);
        var opens = corners.get(breaks);
        var closes = corners.get((breaks + 1) % corners.size());
        var stood = labels.get(breaks);

        corners.add(breaks + 1, corner);
        labels.add(breaks + 1, readSpanLabel(corner, closes, site, across, neighbour, stood));
        labels.set(breaks, readSpanLabel(opens, corner, site, across, neighbour, stood));
    }

    // Which line a span runs along: the border shared with the neighbour, when its middle is
    // the same distance from both sites, and otherwise whatever the span it was broken out of
    // ran along.
    private static int readSpanLabel(
            double[] opens,
            double[] closes,
            double[] site,
            double[] across,
            int neighbour,
            int stood) {

        var middle = new double[] {(opens[0] + closes[0]) / 2, (opens[1] + closes[1]) / 2};

        var evenly = Math.abs(
            Points.computeDistance(middle, site) - Points.computeDistance(middle, across));

        return evenly < Limits.MIN_EDGE_LENGTH ? neighbour : stood;
    }

    // The span whose own length the corner stands nearest to.
    private static int findNearestSpan(List<double[]> corners, double[] corner) {

        var nearest = 0;
        var gap = Double.MAX_VALUE;

        for (var index = 0; index < corners.size(); index++) {

            var to = Segments.computeDistanceToPoint(
                corners.get(index), corners.get((index + 1) % corners.size()), corner);

            if (to < gap) {
                gap = to;
                nearest = index;
            }
        }
        return nearest;
    }

    // Whether the ring already stands on this corner.
    private static boolean isAlreadyInTheRing(List<double[]> corners, double[] corner) {

        for (var stood : corners) {
            if (Points.computeDistance(stood, corner) < Limits.MIN_EDGE_LENGTH) {
                return true;
            }
        }
        return false;
    }

    // Whether a corner on the bound is this cell's at all: it stands at the reach from its own
    // site by construction, so any site it stands nearer to has the better claim, and the
    // border never reaches the bound there.
    private static boolean isThisCells(
            double[] corner,
            List<double[]> sites,
            List<Integer> claimants,
            double maxCellRadius) {

        for (var other : claimants) {

            if (Points.computeDistance(corner, sites.get(other))
                    < maxCellRadius - Limits.MIN_EDGE_LENGTH) {
                return false;
            }
        }
        return true;
    }

    // The sites near enough to take a corner off this one. A site more than two reaches away
    // is further from every point of this bound than this site is, so it can claim nothing
    // here and need not be asked - which is what keeps the pass off the whole map.
    private static List<Integer> gatherClaimants(
            int siteIndex, List<double[]> sites, double maxCellRadius) {

        var site = sites.get(siteIndex);
        var claimants = new ArrayList<Integer>();

        for (var other = 0; other < sites.size(); other++) {

            if (other != siteIndex
                    && Points.computeDistance(site, sites.get(other)) <= 2 * maxCellRadius) {
                claimants.add(other);
            }
        }
        return claimants;
    }

    // The two points a border between two sites shares with the bound: on their bisector, so
    // the same distance from each, and at the reach from both at once - which is what makes it
    // one corner belonging to two cells rather than two corners that nearly agree. Worked out
    // from the two sites and the reach, so both cells arrive at the same doubles.
    private static List<double[]> crossBoundOnBorderWith(
            double[] site, double[] neighbour, double maxCellRadius) {

        var border = measureBorderWith(site, neighbour, maxCellRadius);

        return border == null
            ? List.of()
            : List.of(border.strideFromMiddle(border.toBound()),
                border.strideFromMiddle(-border.toBound()));
    }

    // The border between two sites, as the line it runs along and how far along it the bound
    // stands - the one reading of that line, so the corner it ends at and the walk along it to
    // find where a third site cuts in cannot drift apart.
    //
    // Null where the border never reaches the bound at all, which needs the two sites more
    // than two reaches apart, and leaves every point of each bound nearer its own site.
    private static Border measureBorderWith(
            double[] site, double[] neighbour, double maxCellRadius) {

        var half = Points.computeDistance(site, neighbour) * 0.5;
        var squared = maxCellRadius * maxCellRadius - half * half;

        // At right angles to the line of centres, which is that line turned a quarter turn.
        var along = Points.computeUnitVector(
            neighbour[1] - site[1], site[0] - neighbour[0], Limits.MIN_EDGE_LENGTH);

        return squared <= 0 || along == null
            ? null
            : new Border(
                (site[0] + neighbour[0]) * 0.5,
                (site[1] + neighbour[1]) * 0.5,
                along[0],
                along[1],
                Math.sqrt(squared));
    }

    // One border between two sites: the middle of it, the way it runs, and how far along it
    // the bound stands either side of that middle.
    private record Border(
        double middleX,
        double middleY,
        double alongX,
        double alongY,
        double toBound) {

        // The point a given distance along the border from its middle, taken negative for the
        // other way.
        private double[] strideFromMiddle(double stride) {
            return new double[] {middleX + alongX * stride, middleY + alongY * stride};
        }
    }

    private static List<Integer> readLabelList(int[] labels) {

        var read = new ArrayList<Integer>(labels.length);

        for (var label : labels) {
            read.add(label);
        }
        return read;
    }

    private static int[] readLabels(List<Integer> labels) {

        var read = new int[labels.size()];

        for (var index = 0; index < labels.size(); index++) {
            read[index] = labels.get(index);
        }
        return read;
    }

    // Moves each corner where a neighbour's border meets the bound onto the bound itself, in
    // place of the one the clip produced against the seed.
    //
    // The clip crosses a CHORD of the bound rather than the bound, so the corner lands short
    // of it. Two neighbours inscribe their own seeds about their own sites, so each puts the
    // shared corner somewhere different and the two cells do not quite meet - and a consumer
    // chaining their frontier edges into one outline finds a gap at every such corner.
    //
    // The exact corner needs no seed at all, and both neighbours work it out from the same
    // midpoint, separation and radius, so they agree bit for bit rather than to a tolerance.
    //
    // A corner between two border edges is untouched: that one is where two neighbours cut
    // each other short of the bound, and the bound had no part in it.
    private static LabelledCell placeBoundCornersExactly(
            LabelledCell cell,
            double[] site,
            List<double[]> sites,
            double maxCellRadius) {

        var vertices = cell.vertices();
        var labels = cell.edgeNeighbourSiteIndices();
        var count = vertices.size();

        if (count < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            return cell;
        }

        var placed = new ArrayList<double[]>(count);

        for (var index = 0; index < count; index++) {

            var arriving = labels[(index + count - 1) % count];
            var leaving = labels[index];

            // Exactly one side on the bound: the corner where the frontier starts or stops,
            // and the only kind this has anything to say about. Both sides on it is a sample
            // partway along the frontier, neither is two neighbours meeting - and in both
            // cases the bound's own circle decides nothing.
            var startsOrStopsTheFrontier =
                (arriving == BOUND_EDGE) != (leaving == BOUND_EDGE);

            var neighbour = arriving == BOUND_EDGE ? leaving : arriving;

            placed.add(startsOrStopsTheFrontier
                ? placeCornerWhereTheBorderEnds(
                    vertices.get(index), site, sites, neighbour, maxCellRadius)
                : vertices.get(index));
        }
        return new LabelledCell(placed, labels);
    }

    // Where this cell's border against one neighbour ends: on the bound, or sooner if a third
    // site's claim starts first.
    //
    // Where a third site does cut in, the border never reaches the bound at all - it ends at
    // the corner the three cells share, and sliding to the bound would hand this cell a wedge
    // nearer that third site, which is the one property the partition cannot give up.
    private static double[] placeCornerWhereTheBorderEnds(
            double[] corner,
            double[] site,
            List<double[]> sites,
            int neighbour,
            double maxCellRadius) {

        var far = sites.get(neighbour);
        var border = measureBorderWith(site, far, maxCellRadius);

        // The clip had nothing to cut here, so there is no such corner to place.
        if (border == null) {
            return corner;
        }

        // Walked from the middle, in whichever direction the clip already put the corner, so
        // the two ends of one border are told apart by where each already is. Both neighbours
        // read the same direction: each has the border pointing the other way and its own
        // corner on the other side, and the two reversals cancel.
        var towards = Math.signum(
            (corner[0] - border.middleX()) * border.alongX()
                + (corner[1] - border.middleY()) * border.alongY());

        var stepX = border.alongX() * (towards < 0 ? -1 : 1);
        var stepY = border.alongY() * (towards < 0 ? -1 : 1);

        var ends = border.toBound();

        for (var other : sites) {

            if (other == site || other == far) {
                continue;
            }
            ends = Math.min(ends, measureClaimStart(
                site, other, border.middleX(), border.middleY(), stepX, stepY));
        }

        var reaches = Math.max(0, ends);

        return new double[] {
            border.middleX() + stepX * reaches, border.middleY() + stepY * reaches};
    }

    // How far along a border this cell's claim survives against one other site, as a distance
    // from the midpoint - or no limit at all where that site never takes over along this
    // direction.
    //
    // A point is this cell's as long as it is at least as near its own site as the other's,
    // which is the half-plane through the midpoint of those two. Walked along a straight line
    // that reads as one inequality in the distance travelled, so where the other site takes
    // over is one division rather than a search.
    private static double measureClaimStart(
            double[] site,
            double[] other,
            double midX,
            double midY,
            double stepX,
            double stepY) {

        var towardsSite = new double[] {site[0] - other[0], site[1] - other[1]};

        var fromMidpoint =
            ((site[0] + other[0]) * 0.5 - midX) * towardsSite[0]
                + ((site[1] + other[1]) * 0.5 - midY) * towardsSite[1];

        var closing = stepX * towardsSite[0] + stepY * towardsSite[1];

        // Going this way never crosses into the other site's half-plane, so it sets no limit.
        return closing < 0 ? fromMidpoint / closing : Double.MAX_VALUE;
    }

    // Clips a cell to the half-plane of points at least as close to {@code keep}
    // as to {@code drop} - the keep side of the perpendicular bisector of the two
    // sites - tagging the newly cut edge with {@code dropIndex}, the site on the
    // far side of it. The normal points toward the kept site, so a non-negative
    // half-plane test is the side to retain; the bisector passes through the
    // midpoint of the two sites.
    private static LabelledPolygon clipToBisector(
            LabelledPolygon cell,
            double[] keep,
            double[] drop,
            int dropIndex) {
        return cell.clipToHalfPlane(
            new HalfPlane(
                (keep[0] + drop[0]) * 0.5, (keep[1] + drop[1]) * 0.5,
                keep[0] - drop[0], keep[1] - drop[1]),
            dropIndex);
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
