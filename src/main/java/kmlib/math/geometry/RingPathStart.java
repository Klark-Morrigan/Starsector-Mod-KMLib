package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Where a ring is traced from, and which way round: the winding a path is walked in, the point it
 * begins at, and the re-listing that puts that point first.
 *
 * <p>A ring arrives with neither settled. Its winding is whatever the pass that built it produced,
 * and its first vertex is wherever that pass began, so two rings describing the same shape can
 * disagree about both. Deciding them is a step of its own, taken before anything is measured along
 * the result, and it is the only part of tracing that reads a ring as a shape rather than as a path.
 *
 * <p>Split from {@link RingPath} because nothing here is about a path at all: these answer what the
 * ring looks like before there is one, and none of them touches an arc length.
 */
final class RingPathStart {

    private RingPathStart() {
    }

    // The ring re-listed to begin at its top centre, in the same order it arrived in: the
    // start point first, then every corner from the far end of the edge it split, round to
    // that edge's near end.
    static List<double[]> rotateToTopCentre(List<double[]> ring, double[] topAnchor) {

        var start = findTopCentre(ring, topAnchor);
        var count = ring.size();
        var rotated = new ArrayList<double[]>(count + 1);

        rotated.add(start.point());

        for (var step = 1; step <= count; step++) {
            rotated.add(ring.get((start.edgeIndex() + step) % count));
        }

        // The walk ends back at the corner the start's edge leaves, and the start itself
        // may sit exactly on a corner at either end of that edge. Both leave a zero-length
        // edge the dedup takes out - one of them, never both, since that would need the
        // edge's two ends to coincide, which a deduplicated ring has none of. So the
        // rotation hands back at least as many corners as it was given, and a ring that
        // enclosed area still does.
        return Rings.removeConsecutiveDuplicates(rotated);
    }

    // Where the vertical line through the anchor last crosses the ring on the way up - the
    // ring's top at the anchor's x, and the edge that crossing splits.
    //
    // Taking the highest crossing rather than the first one above the anchor keeps the
    // answer defined wherever the anchor sits: a ring is not always convex and an anchor
    // is not always within the shape traced from it (an inset ring can pull away past it),
    // so "the first crossing going up" has cases with no answer where "the topmost
    // crossing" has one. Where the anchor does sit inside a convex ring the two agree.
    static TopCentre findTopCentre(List<double[]> ring, double[] topAnchor) {

        TopCentre highest = null;
        var count = ring.size();

        for (var i = 0; i < count; i++) {

            var from = ring.get(i);
            var to = ring.get((i + 1) % count);

            // Half-open side test: an edge crosses the line when its ends sit on opposite
            // sides of it, a corner exactly on the line counting to one fixed side. So a
            // corner the line passes through is a crossing of one of its two edges rather
            // than of both or of neither.
            if ((from[0] <= topAnchor[0]) == (to[0] <= topAnchor[0])) {
                continue;
            }

            var alongEdge = (topAnchor[0] - from[0]) / (to[0] - from[0]);
            var crossing = new double[] {
                topAnchor[0],
                from[1] + alongEdge * (to[1] - from[1])};

            if (highest == null || crossing[1] > highest.point()[1]) {
                highest = new TopCentre(crossing, i);
            }
        }

        // The line misses the ring entirely when the anchor sits beyond it to the left or
        // right, which an anchor away from the shape's own centre can. The ring's own
        // topmost corner is the nearest thing to a top centre then, and it keeps every
        // ring starting somewhere at its top rather than dropping the path over an anchor
        // that only says where to look.
        return highest == null ? findTopCorner(ring) : highest;
    }

    // The ring's highest corner, as a start splitting the edge that leaves it - so the
    // rotation begins at that corner and carries on forward from there.
    static TopCentre findTopCorner(List<double[]> ring) {

        var top = 0;

        for (var i = 1; i < ring.size(); i++) {
            if (ring.get(i)[1] > ring.get(top)[1]) {
                top = i;
            }
        }
        return new TopCentre(ring.get(top), top);
    }

    // The ring wound counter-clockwise, reversed only when it arrived the other way.
    static List<double[]> orientCounterClockwise(List<double[]> ring) {
        return PolygonRegions.computeSignedArea(ring) < 0 ? reverseRing(ring) : ring;
    }

    // The same ring traced the other way round, as a fresh list - the corners are shared,
    // since nothing here moves one.
    static List<double[]> reverseRing(List<double[]> ring) {

        var reversed = new ArrayList<>(ring);

        Collections.reverse(reversed);

        return reversed;
    }

    // Where a path starts, as the point itself plus the edge of the ring it splits - the
    // pair a rotation needs, since the point alone does not say which corner comes next.
    private record TopCentre(
        double[] point,
        int edgeIndex) {
    }
}
