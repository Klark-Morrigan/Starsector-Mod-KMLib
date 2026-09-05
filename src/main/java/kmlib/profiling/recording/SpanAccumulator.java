package kmlib.profiling.recording;

import kmlib.profiling.snapshot.DurationBuckets;
import kmlib.profiling.snapshot.ProfileTiming;

/**
 * What one node's ended calls cost, folded in as they close: how many there
 * were, what they came to in all, the fastest and the slowest, and which
 * duration band each fell in.
 *
 * <p>Its own accumulator rather than fields on the node, so the rule about the
 * first call - that it sets both bounds rather than being folded into sentinels
 * a snapshot would have to undo - is stated once, beside the only state it
 * reads. The node then keeps no counter of calls for anything else to depend on
 * the order of.
 */
final class SpanAccumulator {

    private final long[] callsPerBucket = new long[DurationBuckets.countBuckets()];

    private long count;
    private long totalNanos;
    private long minNanos;
    private long maxNanos;

    /**
     * @return how many calls have ended here, which is how many preceded the
     *         one ending now
     */
    long getCallCount() {
        return count;
    }

    /**
     * Folds one ended call in.
     *
     * @param elapsedNanos how long the call took
     */
    void addSpan(long elapsedNanos) {

        // The first span sets both bounds rather than being folded into
        // sentinels the snapshot would then have to undo. A node with no span -
        // opened while the snapshot was taken, or left open by a caller - then
        // reads as the zeroes it holds, with no rule about what a row means.
        if (count == 0) {
            minNanos = elapsedNanos;
            maxNanos = elapsedNanos;
        } else {
            minNanos = Math.min(minNanos, elapsedNanos);
            maxNanos = Math.max(maxNanos, elapsedNanos);
        }
        count++;
        totalNanos += elapsedNanos;
        callsPerBucket[DurationBuckets.resolveBucketIndex(elapsedNanos)]++;
    }

    /**
     * @return these calls copied into the immutable form a snapshot is read
     *         from
     */
    ProfileTiming buildTiming() {
        return new ProfileTiming(count, totalNanos, minNanos, maxNanos, buildBuckets());
    }

    // The shared empty spread where no call has finished, so a tree full of
    // rows a snapshot caught mid-call copies no arrays of zeroes.
    private DurationBuckets buildBuckets() {
        return count == 0 ? DurationBuckets.NO_CALLS : new DurationBuckets(callsPerBucket);
    }
}
