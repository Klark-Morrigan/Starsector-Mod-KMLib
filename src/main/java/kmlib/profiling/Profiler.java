package kmlib.profiling;

import kmlib.profiling.snapshot.ProfileOriginTree;

import java.util.List;
import java.util.function.Supplier;

/**
 * Where code reports how long its named sections took, for on-demand profiling.
 *
 * <p>A caller opens a section with {@link #open}, wraps a block in
 * {@link #measure}, or hands a raw duration to {@link #record}, then reads
 * {@link #snapshot()}. What comes back is one row per section per parent rather
 * than one per call, the shape per-frame work needs: thousands of calls collapse
 * into a count and a spread instead of thousands of log lines.
 *
 * <p>Rows are a tree, not a list, because a duration on its own says little: a
 * section that ran under a rebuild and the same section under a hover are two
 * facts, and a row averaging them is neither. A section opened inside another
 * is that one's child, its time counts towards the parent's total as well as
 * its own, and the parent's self time is what is left when its children are
 * taken out - which is what says whether to look at a row or below it.
 *
 * <p>A section that runs a per-item loop is opened through
 * {@link #openIterations} instead, which measures the loop's turns inside the
 * one span.
 *
 * <p>Rows group by origin above that, so a capture taken across two games says
 * which one each tree came from: {@link #openRoot} names the origin a beat and
 * everything beneath it belongs to.
 *
 * <p>A seam rather than a class, so what profiling costs is a binding rather
 * than a rebuild. Library code measures through {@link ActiveProfiler} without
 * knowing whether anything is listening; a mod that wants a readout binds a
 * recording implementation, and everything else runs against the
 * {@link SilentProfiler}, which keeps nothing and allocates nothing.
 *
 * <p>How much a bound profiler keeps is a level rather than a switch: a section
 * on a per-item path states the detail it is only worth timing at, and a capture
 * taken to read whole frames opens it silently. See {@link #getRecordedLevel()}.
 *
 * <p>Not synchronised: intended for the single game thread that drives campaign
 * advance and rendering; sharing one instance across threads would need
 * external synchronisation.
 */
public interface Profiler {

    /**
     * Opens {@code section} under whatever scope is already open, timing until
     * the returned scope is closed.
     *
     * <p>For try-with-resources, so the scope cannot outlive the call that
     * opened it - which is the whole basis of the nesting: what is open is what
     * is still running.
     *
     * @param section the section this span belongs to
     * @return the open scope, closed to record the span
     */
    ProfileScope open(ProfileSection section);

    /**
     * Opens {@code section} as a root of {@code origin} - a span with no parent,
     * whatever else happens to be open - timing until the returned scope is
     * closed.
     *
     * <p>How a caller states which game a beat was measured in. Everything
     * opened inside the returned scope is that origin's too, so a caller names
     * the origin once per beat rather than at every section beneath it.
     *
     * <p>What a section opened with no root open lands under is
     * {@link ProfileOrigin#UNSCOPED}, so a walk from a path nobody profiled is
     * seen rather than dropped.
     *
     * @param origin  the game this span and everything under it was measured in
     * @param section the section this span belongs to
     * @return the open scope, closed to record the span
     */
    ProfileScope openRoot(ProfileOrigin origin, ProfileSection section);

    /**
     * Opens {@code section} the way {@link #open} does, over a loop whose turns
     * and steps the returned scope also measures.
     *
     * <p>The row is the same one a plain open of that section lands on: what
     * differs is that the scope takes the turns of a loop as well as a span, so
     * a per-item loop is one row that states what one item cost rather than
     * thousands of rows or one number nothing can be read out of.
     *
     * @param section the section this span belongs to, and the steps one turn of
     *                its loop is split into
     * @return the open scope, closed to record the span and the turns beneath it
     */
    IterationScope openIterations(PhasedSection section);

    /**
     * Adds {@code amount} to what the innermost open scope has counted of
     * {@code counter}, for the shared reads that count what they traverse
     * without owning a scope of their own.
     *
     * <p>What lets the places the sector is actually walked report their size on
     * whichever row asked for the walk. The count is added once, where the
     * traversal happens, and rolls up like any other: a caller that never wrote
     * a profiling line still says how much it touched, and a second traversal
     * nobody meant to make shows on the row that made it.
     *
     * <p>A count arriving with nothing open lands on
     * {@link ProfileSection#UNSCOPED_COUNTS} under
     * {@link ProfileOrigin#UNSCOPED} rather than being dropped.
     *
     * @param counter what is being counted
     * @param amount  how many to add to what the open scope has counted already
     */
    void addCountToOpenScope(ProfileCounter counter, long amount);

    /**
     * Times {@code work} and records its duration under {@code section}.
     *
     * @param section the name to accumulate the duration under
     * @param work    the block to time
     */
    void measure(String section, Runnable work);

    /**
     * Times {@code work}, records its duration under {@code section}, and
     * returns its result - the value-returning form of {@link #measure}.
     *
     * @param section the name to accumulate the duration under
     * @param work    the block to time
     * @param <T>     the result type
     * @return whatever {@code work} returns
     */
    <T> T measure(String section, Supplier<T> work);

    /**
     * Records a pre-measured duration under {@code section}, for callers that
     * time a span themselves rather than wrapping a block.
     *
     * @param section      the name to accumulate the duration under
     * @param elapsedNanos the duration to add
     */
    void record(String section, long elapsedNanos);

    /**
     * @return the finest detail this profiler keeps, which is what decides
     *         whether a section registered at a level is timed or opened
     *         silently; {@link ProfileLevel#OFF} where nothing is kept at all
     */
    ProfileLevel getRecordedLevel();

    /**
     * @return an immutable snapshot of the capture - one tree per origin, each
     *         holding the sections opened with nothing else open under it and
     *         what ran inside those, both in the order they were first recorded
     */
    List<ProfileOriginTree> snapshot();

    /**
     * Clears all accumulated timings, so the next measurements start fresh.
     */
    void reset();
}
