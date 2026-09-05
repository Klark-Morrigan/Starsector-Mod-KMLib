package kmlib.profiling;

import java.util.List;
import java.util.function.Supplier;

/**
 * Accumulates how long named sections of code take, for on-demand profiling.
 *
 * <p>A caller wraps a block in {@link #measure} (or hands a raw duration to
 * {@link #record}); the profiler keeps, per section name, the call count and
 * the total, fastest, and slowest durations. This shape suits per-frame work:
 * thousands of calls collapse into one row of stats instead of one log line
 * each. Read the result with {@link #snapshot()} and format it with
 * {@link TimingReport}.
 *
 * <p>A seam rather than a class so the cost of profiling is a binding: library
 * code measures through {@link ActiveProfiler} without knowing whether anything
 * is listening, and a mod that wants a readout binds a {@link RecordingProfiler}
 * while everything else runs against the {@link SilentProfiler}, which keeps
 * nothing and allocates nothing.
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
