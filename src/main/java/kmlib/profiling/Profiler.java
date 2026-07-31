package kmlib.profiling;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
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
 * <p>The clock is injected (defaulting to {@link System#nanoTime()}) so the
 * accumulation logic can be unit tested against a scripted clock, with no real
 * time involved. Not synchronised: intended for the single game thread that
 * drives campaign advance and rendering; sharing one instance across threads
 * would need external synchronisation.
 */
public final class Profiler {
    private final Map<String, MutableStat> statsBySection = new LinkedHashMap<>();
    private final LongSupplier clockNanos;

    /**
     * Creates a profiler timing against the system nanosecond clock.
     */
    public Profiler() {
        this(System::nanoTime);
    }

    /**
     * Creates a profiler timing against {@code clockNanos}, for tests that need
     * a deterministic clock.
     *
     * @param clockNanos source of the current time in nanoseconds
     */
    public Profiler(LongSupplier clockNanos) {
        this.clockNanos = clockNanos;
    }

    /**
     * Times {@code work} and records its duration under {@code section}.
     *
     * @param section the name to accumulate the duration under
     * @param work    the block to time
     */
    public void measure(String section, Runnable work) {
        var start = clockNanos.getAsLong();
        try {
            work.run();
        } finally {
            // Record in finally so a throwing block still contributes its
            // (partial) duration rather than vanishing from the stats.
            record(section, clockNanos.getAsLong() - start);
        }
    }

    /**
     * Times {@code work}, records its duration under {@code section}, and
     * returns its result - the value-returning form of {@link #measure}.
     *
     * @param section the name to accumulate the duration under
     * @param work    the block to time
     * @param <T>     the result type
     * @return whatever {@code work} returns
     */
    public <T> T measure(String section, Supplier<T> work) {
        var start = clockNanos.getAsLong();
        try {
            return work.get();
        } finally {
            record(section, clockNanos.getAsLong() - start);
        }
    }

    /**
     * Records a pre-measured duration under {@code section}, for callers that
     * time a span themselves rather than wrapping a block.
     *
     * @param section     the name to accumulate the duration under
     * @param elapsedNanos the duration to add
     */
    public void record(String section, long elapsedNanos) {
        statsBySection
            .computeIfAbsent(section, key -> new MutableStat())
            .add(elapsedNanos);
    }

    /**
     * @return an immutable snapshot of every section's stats, in the order the
     *         sections were first recorded
     */
    public List<SectionTiming> snapshot() {
        var timings = new ArrayList<SectionTiming>();
        for (var entry : statsBySection.entrySet()) {
            var stat = entry.getValue();
            timings.add(new SectionTiming(
                entry.getKey(),
                stat.count,
                stat.totalNanos,
                stat.minNanos,
                stat.maxNanos));
        }
        return timings;
    }

    /**
     * Clears all accumulated timings, so the next measurements start fresh.
     */
    public void reset() {
        statsBySection.clear();
    }

    // Running totals for one section. Mutable and package-free on purpose: it is
    // an internal accumulator, never handed out (snapshot() copies into the
    // immutable SectionTiming instead).
    private static final class MutableStat {
        private long count;
        private long totalNanos;
        private long minNanos = Long.MAX_VALUE;
        private long maxNanos = Long.MIN_VALUE;

        private void add(long elapsedNanos) {
            count++;
            totalNanos += elapsedNanos;
            minNanos = Math.min(minNanos, elapsedNanos);
            maxNanos = Math.max(maxNanos, elapsedNanos);
        }
    }
}
