package kmlib.profiling;

import java.util.List;
import java.util.function.Supplier;

/**
 * Where code reports how long its named sections took, for on-demand profiling.
 *
 * <p>A caller wraps a block in {@link #measure}, or hands a raw duration to
 * {@link #record}, then reads {@link #snapshot()} and formats it with
 * {@link TimingReport}. What comes back is one row per section rather than one
 * per call, which is the shape per-frame work needs: thousands of calls collapse
 * into a count and a spread instead of thousands of log lines.
 *
 * <p>A seam rather than a class, so what profiling costs is a binding rather
 * than a rebuild. Library code measures through {@link ActiveProfiler} without
 * knowing whether anything is listening; a mod that wants a readout binds a
 * {@link RecordingProfiler}, and everything else runs against the
 * {@link SilentProfiler}, which keeps nothing and allocates nothing.
 *
 * <p>Not synchronised: intended for the single game thread that drives campaign
 * advance and rendering; sharing one instance across threads would need
 * external synchronisation.
 */
public interface Profiler {

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
     * @return an immutable snapshot of every section's stats, in the order the
     *         sections were first recorded
     */
    List<SectionTiming> snapshot();

    /**
     * Clears all accumulated timings, so the next measurements start fresh.
     */
    void reset();
}
