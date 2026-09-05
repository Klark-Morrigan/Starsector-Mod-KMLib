package kmlib.profiling;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * The {@link Profiler} that keeps what it measures: per section name, the call
 * count and the total, fastest, and slowest durations.
 *
 * <p>The clock is injected (defaulting to {@link System#nanoTime()}) so the
 * accumulation reads whatever time source its caller names rather than the
 * system clock.
 */
public final class RecordingProfiler implements Profiler {
    private final Map<String, MutableStat> statsBySection = new LinkedHashMap<>();
    private final LongSupplier clockNanos;

    /**
     * Creates a profiler timing against the system nanosecond clock.
     */
    public RecordingProfiler() {
        this(System::nanoTime);
    }

    /**
     * Creates a profiler timing against {@code clockNanos}, for a caller that
     * supplies its own time source rather than reading the system clock.
     *
     * @param clockNanos source of the current time in nanoseconds
     */
    public RecordingProfiler(LongSupplier clockNanos) {
        this.clockNanos = clockNanos;
    }

    @Override
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

    @Override
    public <T> T measure(String section, Supplier<T> work) {
        var start = clockNanos.getAsLong();
        try {
            return work.get();
        } finally {
            record(section, clockNanos.getAsLong() - start);
        }
    }

    @Override
    public void record(String section, long elapsedNanos) {
        statsBySection
            .computeIfAbsent(section, key -> new MutableStat())
            .add(elapsedNanos);
    }

    @Override
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

    @Override
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
