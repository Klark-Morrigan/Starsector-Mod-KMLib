package kmlib.profiling.recording;

import kmlib.profiling.ProfileCounter;
import kmlib.profiling.snapshot.CountTotals;
import kmlib.profiling.snapshot.ProfileCount;

/**
 * The mutable tally one node keeps for one counter, folding each ended call's
 * amounts into the totals and the per-call maximum a snapshot reports.
 *
 * <p>Kept apart from the {@link ProfileCount} a snapshot hands out for the same
 * reason a node is: what a reader is given must not change while it is being
 * read.
 */
final class ProfileCountAccumulator {

    private final ProfileCounter counter;

    private long total;
    private long selfTotal;
    private long maxPerCall;

    /**
     * @param counter what is being counted
     */
    ProfileCountAccumulator(ProfileCounter counter) {
        this.counter = counter;
    }

    ProfileCounter getCounter() {
        return counter;
    }

    /**
     * Folds in one ended call of the row this tally belongs to.
     *
     * @param selfAmount  what the call counted itself
     * @param totalAmount what the call counted including everything opened
     *                    inside it
     */
    void addCall(long selfAmount, long totalAmount) {

        // No first-call case to carry: an amount is never negative, so a zero
        // start is the floor rather than a sentinel a snapshot would have to
        // undo, and a call that counted none of something cannot be the largest.
        maxPerCall = Math.max(maxPerCall, totalAmount);
        total += totalAmount;
        selfTotal += selfAmount;
    }

    /**
     * @return this tally copied into the immutable form a snapshot is read from
     */
    ProfileCount buildCount() {
        return new ProfileCount(counter, new CountTotals(total, selfTotal), maxPerCall);
    }
}
