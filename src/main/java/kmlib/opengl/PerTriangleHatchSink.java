package kmlib.opengl;

/**
 * Emits one segment per span it is offered, in the order the clip walk finds them, so a line
 * crossing several triangles comes back as a chain of shorter segments touching end to end.
 *
 * <p>The joining that decides nothing: it merges no span with any other, so it holds no state
 * beyond the run it is filling and costs the walk nothing over emitting inline. What is left once
 * the deciding is taken away is the writer below, which is why almost nothing remains here.
 */
final class PerTriangleHatchSink implements HatchSegmentSink {

    private final HatchSegmentWriter writer;

    PerTriangleHatchSink(HatchAxes axes) {
        this.writer = new HatchSegmentWriter(axes);
    }

    @Override
    public void acceptClippedSpan(int lineIndex, double minAlong, double maxAlong) {
        writer.addSegmentOnLine(lineIndex, minAlong, maxAlong);
    }

    @Override
    public HatchRun packHatchRun() {
        // No join is ever attempted, so the tally has nothing to report either way - not zero
        // because both kinds happened to come out empty on this region.
        return new HatchRun(writer.packSegments(), HatchJoinTally.NO_JOINS);
    }
}
