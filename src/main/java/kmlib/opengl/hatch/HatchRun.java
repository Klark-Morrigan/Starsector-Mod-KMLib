package kmlib.opengl.hatch;

import kmlib.opengl.GlVertexRuns;

/**
 * One hatch as the clip hands it back: the {@code GL_LINES} run to draw, and how the joining that
 * packed it closed its joins.
 *
 * <p>The tally rides with the run rather than being reported some other way because it describes
 * this one packing - the same ground hatched under another joining, or another tolerance, closes
 * different joins - and numbers arriving apart from the run they came from cannot be tied back to
 * it.
 *
 * <p>The run is a plain float array, so two of these are equal only when they hold the very same
 * array. Compare the runs themselves rather than the records.
 *
 * @param segments the clipped hatch as a flat {@code [x1, y1, x2, y2, ...]} {@code GL_LINES} run
 * @param joins    how the joining that packed those segments closed its joins
 */
public record HatchRun(
    float[] segments,
    HatchJoinTally joins) {

    /** Nothing hatched at all, and so no joins of either kind. */
    public static final HatchRun NOTHING_HATCHED =
        new HatchRun(GlVertexRuns.NO_VERTICES, HatchJoinTally.NO_JOINS);
}
