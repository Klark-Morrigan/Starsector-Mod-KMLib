package kmlib.profiling;

/**
 * How often a section ran at one place in the tree and what its calls cost: the
 * call count, and the total, fastest and slowest span across them.
 *
 * <p>Grouped rather than carried as four loose numbers, because they only mean
 * anything together - a total says nothing without the count it is spread over,
 * a maximum nothing without the total it is part of - and because four adjacent
 * longs at a call site are four chances to hand one over in another's place.
 *
 * <p>Self time is not here. What a section spent outside the sections opened
 * inside it is a fact about where the node sits in the tree rather than about
 * its own calls, and only the node knows what those children took.
 *
 * <p>Immutable, and holding raw nanoseconds (the clock's unit) so the reader
 * decides how to present them.
 */
public final class ProfileTiming {

    private final long count;
    private final long totalNanos;
    private final long minNanos;
    private final long maxNanos;

    public ProfileTiming(long count, long totalNanos, long minNanos, long maxNanos) {
        this.count = count;
        this.totalNanos = totalNanos;
        this.minNanos = minNanos;
        this.maxNanos = maxNanos;
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
