package kmlib.math.geometry;

import java.util.ArrayList;
import java.util.List;

/**
 * One filled region bounded by rings: the outer ring enclosing it, and the hole rings cut
 * out of its interior.
 *
 * <p>The shape the region passes here already work in - each documents its rings as an outer
 * plus its holes - given a name, so several disjoint regions can be held apart from one
 * another. In a flat ring soup they are not: which ring bounds which body, and which hole
 * belongs to which of them, survives only as winding and containment, and every consumer that
 * needs the answer has to recover it the same way.
 *
 * @param outerRing the ring enclosing the region, wound counter-clockwise
 * @param holeRings the rings cut out of its interior, wound clockwise; empty for a region
 *                  with nothing cut out of it
 */
public record RingRegion(
    List<double[]> outerRing,
    List<List<double[]>> holeRings) {

    /**
     * This region's rings as one flat list, outer first - the form every pass that takes "an
     * outer ring plus its holes" reads them in.
     *
     * <p>Kept here rather than at each call site so a region built by grouping can be handed
     * straight back to those passes, without the ordering convention being restated by whoever
     * unpacks it.
     *
     * @return the outer ring followed by each hole ring
     */
    public List<List<double[]>> toRings() {
        var rings = new ArrayList<List<double[]>>(1 + holeRings.size());
        rings.add(outerRing);
        rings.addAll(holeRings);
        return rings;
    }
}
