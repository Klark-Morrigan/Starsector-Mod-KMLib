package kmlib.profiling;

/**
 * How a row's calls were spread over duration bands: one tally per band, each
 * band twice as long as the one before it.
 *
 * <p>Kept beside a mean and a maximum because those two cannot tell a section
 * that is always this slow from one that ran fast until it stalled once, and
 * the two want different fixes. Where the calls sit says which of the two a row
 * is.
 *
 * <p>The bands double rather than divide the range evenly, because a duration
 * is read in orders of magnitude: the difference worth seeing is between a
 * hundred microseconds and ten milliseconds, not between one hundred and two
 * hundred microseconds. Twenty-two of them reach from a call too short to
 * matter up to one that has already cost the frame.
 *
 * <p>Immutable, and holding raw nanoseconds (the clock's unit) so the reader
 * decides how to present them.
 */
public final class DurationBuckets {

    // Everything under a microsecond is one band: a call that short is free at
    // frame scale, and how free it was is not worth a column of its own.
    private static final long FIRST_BUCKET_FLOOR_NANOS = 1_000L;

    // The band under that floor, then one per doubling, so the last band starts
    // at 2^20 microseconds - just over a second, long past the point the frame
    // is gone. Anything slower lands in it rather than off the end.
    private static final int BUCKET_COUNT = 22;

    // Where a long's top bit sits, which is what turns a count of leading zeroes
    // into the power of two a value has reached.
    private static final int HIGHEST_BIT_INDEX = 63;

    /**
     * The spread of a row no call has finished on. Shared rather than an array
     * of zeroes per row, since every such row says the same nothing.
     */
    public static final DurationBuckets NO_CALLS = new DurationBuckets(new long[BUCKET_COUNT]);

    private final long[] callsPerBucket;

    /**
     * @param callsPerBucket how many calls landed in each band, indexed the way
     *                       {@link #resolveBucketIndex} indexes them; copied, so
     *                       the array a profiler accumulates into stays its own
     */
    public DurationBuckets(long[] callsPerBucket) {
        this.callsPerBucket = callsPerBucket.clone();
    }

    /**
     * @return how many bands there are - what an array accumulating them is
     *         sized by, and how wide a spread reads
     */
    public static int countBuckets() {
        return BUCKET_COUNT;
    }

    /**
     * @param nanos how long one call took
     * @return the band it belongs to: 0 below a microsecond, then one per
     *         doubling of it, and the last band for everything past its floor -
     *         a duration off the end is a slow call, not an unplaceable one
     */
    public static int resolveBucketIndex(long nanos) {

        if (nanos < FIRST_BUCKET_FLOOR_NANOS) {
            return 0;
        }

        // How many times the floor doubles into the duration, read off the
        // position of its top bit rather than through a logarithm - this runs
        // on every close.
        var doublings = HIGHEST_BIT_INDEX
            - Long.numberOfLeadingZeros(nanos / FIRST_BUCKET_FLOOR_NANOS);

        return Math.min(1 + doublings, BUCKET_COUNT - 1);
    }

    /**
     * @param bucketIndex the band, as {@link #resolveBucketIndex} numbers them
     * @return how many calls landed in it
     */
    public long getCallsInBucket(int bucketIndex) {
        return callsPerBucket[bucketIndex];
    }

    /**
     * @return whether any call has been placed at all, which is what tells a
     *         row with a spread worth showing from one a snapshot caught before
     *         its first call ended
     */
    public boolean hasAnyCalls() {
        for (var calls : callsPerBucket) {
            if (calls > 0) {
                return true;
            }
        }
        return false;
    }
}
