package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * Lays the corners a Voronoi cell shares with its neighbours onto the bound it stops at.
 *
 * <p>The second half of building such a cell, and a separate subject from the first. Carving one
 * out of the plane is a matter of half-planes and knows nothing of arcs; this knows only about
 * the places a border gives way to the bound, and is the whole of what the two cells meeting
 * there have to agree on.
 *
 * <p>Named for the cells it serves rather than for corners in general, because that is all it
 * serves: every step of it reads sites, the borders between them and which of them has the
 * better claim to a point. There is nothing here a shape that is not a Voronoi cell could use.
 *
 * <p><b>What makes those corners worth their own pass.</b> A cell is carved from a seed polygon
 * inscribed in its bound, so the seed falls short of the bound between its vertices - by up to
 * a chord's sagitta at the middle of each side. A shared corner landing in that band comes back
 * moved, or at a coarse segment count does not come back at all, having been clipped away with
 * the rest of the band. Two neighbours inscribe their own seeds about their own sites, so each
 * gets it wrong differently, and the frontier between cell and void then has a gap at every such
 * corner - wide enough, at a low count, to leak one enclosed piece of void into the next.
 *
 * <p>So the corner is worked out from the two sites and the reach and from nothing either cell
 * drew, which makes both arrive at the same doubles; a corner merely moved is moved back, and
 * one cut away is put back by breaking the span it should have stood on. The segment count then
 * decides how smoothly the arc BETWEEN corners is drawn and nothing else, which is what lets a
 * caller trade smoothness for vertices without changing where a neighbour can be reached.
 *
 * <p>Package-private because it is how {@link VoronoiCellBuilder} finishes a cell rather than
 * something to run over one - there is no cell to lay corners on that the builder did not carve.
 */
final class VoronoiCellCorners {

    /**
     * How many reaches away a site can still hold a corner of this cell.
     *
     * <p>Two, because a site further than that is further from every point of this bound than
     * this site is. Named and package-visible because it is what this pass's cost is made of:
     * a benchmark reporting how many claimants the average cell weighs has to count them by
     * the same distance the pass uses, and the two spelled apart would drift the day the
     * reach changed.
     */
    static final double CLAIM_REACH_MULTIPLE = 2;

    private VoronoiCellCorners() {
    }

    // Two passes, because the seed gets a corner wrong in two ways and only one of them can
    // be corrected in place: a corner it merely moved is moved back, and one it cut away
    // entirely has to be put back, there being nothing left in the ring to move.
    //
    // The cost is a dozen or so candidates per cell, each weighed only against the sites near
    // enough to have a claim - a fraction of the clipping already done, and it does not grow
    // with the bound's segment count.
    static VoronoiCellBuilder.LabelledCell layCornersInto(
            VoronoiCellBuilder.LabelledCell cell,
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

        return new VoronoiCellBuilder.LabelledCell(corners, readLabels(labels));
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
                    && Points.computeDistance(site, sites.get(other))
                        <= CLAIM_REACH_MULTIPLE * maxCellRadius) {
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
    private static VoronoiCellBuilder.LabelledCell placeBoundCornersExactly(
            VoronoiCellBuilder.LabelledCell cell,
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
                (arriving == VoronoiCellBuilder.BOUND_EDGE) != (leaving == VoronoiCellBuilder.BOUND_EDGE);

            var neighbour = arriving == VoronoiCellBuilder.BOUND_EDGE ? leaving : arriving;

            placed.add(startsOrStopsTheFrontier
                ? placeCornerWhereTheBorderEnds(
                    vertices.get(index), site, sites, neighbour, maxCellRadius)
                : vertices.get(index));
        }
        return new VoronoiCellBuilder.LabelledCell(placed, labels);
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

}
