package kmlib.math.geometry;

import java.util.Collection;
import java.util.List;

/**
 * A {@link DirectedLine} evaluated against a bounded region: the infinite line together
 * with the region it is measured inside - the boundary rings it must stay within and the
 * keep-out points it must clear - plus the projection of a parameter span along the line
 * back to a concrete {@link Segment}.
 *
 * <p>Bundles the region with the line so a search that sweeps many candidate chords across
 * one region passes a single value rather than the same rings and keep-outs down every
 * call. The rings and keep-out points are constant across a region while the line varies
 * per candidate, so a chord is cheap to mint per candidate. Anything grown or placed along
 * a chord within a region - a label band, a marker strip - reads its geometry from here.
 *
 * @param rings    the region's boundary rings (outer ring plus any holes) a placement along
 *                 the chord must stay inside
 * @param keepOuts the points a placement along the chord must keep its clearance from
 * @param line     the infinite line the chord runs along (unit direction, so a parameter
 *                 reads as a world distance), measured inside the region
 */
public record RegionChord(List<List<double[]>> rings, Collection<double[]> keepOuts,
        DirectedLine line) {

    /**
     * The world segment spanning {@code {tStart, tEnd}} along this chord's line - the step
     * that turns a fitted or measured parameter span into a concrete line segment.
     *
     * @param span the parameter interval as {@code {tStart, tEnd}}
     * @return the span's endpoints in world coordinates
     */
    public Segment toSegment(double[] span) {
        return new Segment(
                line.originX() + line.directionX() * span[0],
                line.originY() + line.directionY() * span[0],
                line.originX() + line.directionX() * span[1],
                line.originY() + line.directionY() * span[1]);
    }
}
