package kmlib.opengl;

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
 * <p>Endpoints are rebuilt from the axes rather than taken from whatever crossing produced them,
 * so every emitted point lies exactly on its own line and the two ends of a chain agree exactly
 * where they meet.
 */
final class HatchSegmentWriter {

    private final HatchAxes axes;
    private final List<Float> segments = new ArrayList<>();

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
    void addSegmentOnLine(int lineIndex, double start, double end) {
        addPointOnLine(lineIndex, start);
        addPointOnLine(lineIndex, end);
    }

    /**
     * @return everything appended so far as a flat {@code [x1, y1, x2, y2, ...]} run
     */
    float[] packSegments() {
        return GlVertexRuns.packFloats(segments);
    }

    // Appends one endpoint as its packed x then y.
    private void addPointOnLine(int lineIndex, double distanceAlong) {
        segments.add((float) axes.computeXOnLine(lineIndex, distanceAlong));
        segments.add((float) axes.computeYOnLine(lineIndex, distanceAlong));
    }
}
