package kmlib.math.geometry;

import java.util.List;

/**
 * The keep-out intervals a set of point obstacles carves from one line, as
 * {@code {tStart, tEnd}} parameter pairs along that line's unit direction - the
 * obstacle projection {@link Spans#findLongestClearSubsegment} otherwise redoes on
 * every call.
 *
 * <p>Reified because the projection depends on the line, the obstacle set and the
 * clearance alone, while the spans subtracted from it vary: a caller testing many
 * span lists against one line pays the projection and the sort once rather than per
 * test, which is the difference between an obstacle scan per test and one for the
 * whole sweep.
 *
 * <p>The intervals mean nothing against any other line, since a parameter is a
 * distance measured from that line's origin along its direction - a value of this
 * type travels with the line it was computed for.
 *
 * @param intervals the blocked intervals as {@code {tStart, tEnd}} pairs, ascending
 *                  by start so a span can be walked against them with one advancing
 *                  cursor rather than a re-scan per gap
 */
public record LineBlockers(
    List<double[]> intervals) {
}
