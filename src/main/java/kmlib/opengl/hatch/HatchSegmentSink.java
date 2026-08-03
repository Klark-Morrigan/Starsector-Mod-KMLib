package kmlib.opengl.hatch;

/**
 * Collects the spans a hatch clip walk finds and packs them into the {@code GL_LINES} run the
 * hatch hands back.
 *
 * <p>The seam between finding the geometry and deciding what one emitted primitive is. The clip
 * walk is written once and offers every crossing it finds to a sink; how many segments those
 * crossings become is the sink's alone. A second joining is therefore a second sink over the same
 * geometry rather than a second walk, which is the failure this seam rules out: two walks drift,
 * and the drift shows as a hatch that is subtly wrong under one joining only.
 *
 * <p>A span is offered as the line it lies on and the two distances along that line it runs
 * between, rather than as two points. That is the form a sink can compare and merge in, and it
 * leaves each emitted point to be rebuilt from the line family's own axes, where a point carried
 * through from an edge intersection would land only near its line.
 */
public interface HatchSegmentSink {

    /**
     * Records one hatch line's crossing of one triangle.
     *
     * @param lineIndex which line of the family the span lies on
     * @param minAlong  where the span starts, as a distance along the line direction
     * @param maxAlong  where it ends, always at or past {@code minAlong}
     */
    void acceptClippedSpan(int lineIndex, double minAlong, double maxAlong);

    /**
     * @return everything accepted so far as a drawable run, paired with how this sink's own rule
     *         for what one primitive is closed the joins it made
     */
    HatchRun packHatchRun();
}
