package kmlib.math.geometry;

import java.util.Collection;
import java.util.List;

/**
 * An oriented line evaluated against a bounded region: an infinite line through
 * {@code (throughX, throughY)} along {@code direction}, together with the region it is
 * measured inside - the boundary rings it must stay within and the keep-out points it
 * must clear - plus the projection of a parameter span along the line back to a concrete
 * {@link Segment}.
 *
 * <p>Bundles the region with the line so a search that sweeps many candidate chords across
 * one region passes a single value rather than the same rings and keep-outs down every
 * call. The rings and keep-out points are constant across a region while the through-point
 * and direction vary per candidate, so a chord is cheap to mint per candidate. Anything
 * grown or placed along a chord within a region - a label band, a marker strip - reads its
 * geometry from here.
 *
 * @param rings     the region's boundary rings (outer ring plus any holes) a placement
 *                  along the chord must stay inside
 * @param keepOuts  the points a placement along the chord must keep its clearance from
 * @param throughX  x of a point the line passes through
 * @param throughY  y of a point the line passes through
 * @param direction the line's unit direction as {@code {x, y}}
 */
public record RegionChord(List<List<double[]>> rings, Collection<double[]> keepOuts, double throughX,
        double throughY, double[] direction) {

    /**
     * The world segment spanning {@code {tStart, tEnd}} along this chord's direction - the
     * step that turns a fitted or measured parameter span into a concrete line segment.
     *
     * @param span the parameter interval as {@code {tStart, tEnd}}
     * @return the span's endpoints in world coordinates
     */
    public Segment toSegment(double[] span) {
        return new Segment(
                throughX + direction[0] * span[0],
                throughY + direction[1] * span[0],
                throughX + direction[0] * span[1],
                throughY + direction[1] * span[1]);
    }
}
