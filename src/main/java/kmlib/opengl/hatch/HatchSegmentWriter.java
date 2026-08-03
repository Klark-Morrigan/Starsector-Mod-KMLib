package kmlib.opengl.hatch;

import kmlib.opengl.GlVertexRuns;

import java.util.ArrayList;
import java.util.List;

/**
 * Accumulates a hatch's emitted segments as the flat {@code GL_LINES} run it will be packed into,
 * turning each one from a (line, distance, distance) triple into the four floats that draw it.
 *
 * <p>Every sink ends the same way whatever it decided along the route - some number of stretches
 * of some numbered lines, packed in the order it emitted them - so that ending is written once
 * here and the sinks hold one of these rather than a coordinate list each. What a sink is for is
 * the rule that produces the stretches, and this is what is left when that rule is taken away.
 *
 * <p>It is also the whole of what a sink needs in order to emit, which is why a sink is handed one
 * of these rather than the axes behind it. Turning a distance along a numbered line back into a
 * point is projection arithmetic a sink has no business knowing: deciding what one primitive is
 * can be done entirely in the (line, distance) terms the clip offers. So only this class crosses
 * back into coordinates, and only this class is built from what that takes - which is what lets
 * the axes stay inside the package that works them out.
 *
 * <p>Endpoints are rebuilt from those axes rather than taken from whatever crossing produced them,
 * so every emitted point lies exactly on its own line and the two ends of a chain agree exactly
 * where they meet.
 */
public final class HatchSegmentWriter {

    private final HatchAxes axes;
    private final List<Float> segments = new ArrayList<>();

    // Package-private, so a writer can only be raised where the line family was worked out. A sink
    // is handed one already built.
    HatchSegmentWriter(HatchAxes axes) {
        this.axes = axes;
    }

    /**
     * Appends one segment of the numbered line, as its two endpoints each packed x then y.
     *
     * @param lineIndex which line of the family the segment lies on
     * @param start     where it starts, as a distance along the line direction
     * @param end       where it ends
     */
    public void addSegmentOnLine(int lineIndex, double start, double end) {
        addPointOnLine(lineIndex, start);
        addPointOnLine(lineIndex, end);
    }

    /**
     * @return everything appended so far as a flat {@code [x1, y1, x2, y2, ...]} run
     */
    public float[] packSegments() {
        return GlVertexRuns.packFloats(segments);
    }

    // Appends one endpoint as its packed x then y.
    private void addPointOnLine(int lineIndex, double distanceAlong) {
        segments.add((float) axes.computeXOnLine(lineIndex, distanceAlong));
        segments.add((float) axes.computeYOnLine(lineIndex, distanceAlong));
    }
}
