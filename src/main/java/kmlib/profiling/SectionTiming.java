package kmlib.profiling;

/**
 * An immutable snapshot of one profiled section's accumulated timings: how many
 * times it ran, and the total, fastest, and slowest nanoseconds observed.
 *
 * <p>Produced by {@link Profiler#snapshot()} for reporting. Holds raw
 * nanoseconds (the clock's unit) so the reader decides how to present them;
 * {@link TimingReport} converts to milliseconds for display.
 */
public final class SectionTiming {
    private final String section;
    private final long count;
    private final long totalNanos;
    private final long minNanos;
    private final long maxNanos;

    public SectionTiming(String section, long count, long totalNanos,
            long minNanos, long maxNanos) {
        this.section = section;
        this.count = count;
        this.totalNanos = totalNanos;
        this.minNanos = minNanos;
        this.maxNanos = maxNanos;
    }

    public String getSection() {
        return section;
    }

    public long getCount() {
        return count;
    }

    public long getTotalNanos() {
        return totalNanos;
    }

    public long getMinNanos() {
        return minNanos;
    }

    public long getMaxNanos() {
        return maxNanos;
    }

    /**
     * @return the mean nanoseconds per run, or 0 when nothing was recorded
     *         (guards the divide so an empty snapshot is still printable)
     */
    public long getAverageNanos() {
        return count == 0 ? 0 : totalNanos / count;
    }
}
