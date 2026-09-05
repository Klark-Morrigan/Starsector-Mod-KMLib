package kmlib.profiling;

import kmlib.profiling.snapshot.ProfileNode;

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
 * <p>A seam rather than a class, so what profiling costs is a binding rather
 * than a rebuild. Library code measures through {@link ActiveProfiler} without
 * knowing whether anything is listening; a mod that wants a readout binds a
 * recording implementation, and everything else runs against the
 * {@link SilentProfiler}, which keeps nothing and allocates nothing.
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
     * @return an immutable snapshot of the section tree - the sections opened
     *         with nothing else open, each holding what ran inside it, in the
     *         order they were first recorded
     */
    List<ProfileNode> snapshot();

    /**
     * Clears all accumulated timings, so the next measurements start fresh.
     */
    void reset();
}
