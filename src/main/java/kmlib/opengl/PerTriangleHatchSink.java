package kmlib.opengl;

import java.util.ArrayList;
import java.util.List;

/**
 * Emits one segment per span it is offered, in the order the clip walk finds them, so a line
 * crossing several triangles comes back as a chain of shorter segments touching end to end.
 *
 * <p>The joining that decides nothing: it merges no span with any other, so it holds no state
 * beyond the run it is filling and costs the walk nothing over emitting inline.
 *
 * <p>Endpoints are still rebuilt from the axes rather than taken from the clip. The two ends of
 * one chain then agree exactly where they meet, whichever triangle each came out of, so a chain
 * is collinear by construction rather than to within the edge arithmetic's rounding.
 */
final class PerTriangleHatchSink implements HatchSegmentSink {

    private final HatchAxes axes;
    private final List<Float> segments = new ArrayList<>();

    PerTriangleHatchSink(HatchAxes axes) {
        this.axes = axes;
    }

    @Override
    public void acceptClippedSpan(int lineIndex, double minAlong, double maxAlong) {
        addPointOnLine(lineIndex, minAlong);
        addPointOnLine(lineIndex, maxAlong);
    }

    @Override
    public float[] packSegments() {
        return GlVertexRuns.packFloats(segments);
    }

    // Appends one endpoint as its packed x then y.
    private void addPointOnLine(int lineIndex, double distanceAlong) {
        segments.add((float) axes.computeXOnLine(lineIndex, distanceAlong));
        segments.add((float) axes.computeYOnLine(lineIndex, distanceAlong));
    }
}
