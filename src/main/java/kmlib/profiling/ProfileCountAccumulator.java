package kmlib.profiling;

/**
 * The mutable tally one node keeps for one counter, folding each ended call's
 * amounts into the totals and the per-call spread a snapshot reports.
 *
 * <p>Kept apart from the {@link ProfileCount} a snapshot hands out for the same
 * reason a node is: what a reader is given must not change while it is being
 * read.
 */
final class ProfileCountAccumulator {

    private final ProfileCounter counter;

    private boolean hasObservedCall;
    private long total;
    private long selfTotal;
    private long minPerCall;
    private long maxPerCall;

    /**
     * @param counter               what is being counted
     * @param callsBeforeFirstCount how many calls of the row had already ended
     *                              when this counter was first added to - each
     *                              of them counted none of it, which is a zero
     *                              per call and not a gap, so the minimum is
     *                              settled before the first amount arrives
     */
    ProfileCountAccumulator(ProfileCounter counter, long callsBeforeFirstCount) {
        this.counter = counter;
        this.hasObservedCall = callsBeforeFirstCount > 0;
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

        // The first call sets both bounds rather than being folded into
        // sentinels a snapshot would then have to undo - the same rule a span
        // is bounded by.
        if (hasObservedCall) {
            minPerCall = Math.min(minPerCall, totalAmount);
            maxPerCall = Math.max(maxPerCall, totalAmount);
        } else {
            minPerCall = totalAmount;
            maxPerCall = totalAmount;
            hasObservedCall = true;
        }
        total += totalAmount;
        selfTotal += selfAmount;
    }

    /**
     * @return this tally copied into the immutable form a snapshot is read from
     */
    ProfileCount buildCount() {
        return new ProfileCount(counter, total, selfTotal, minPerCall, maxPerCall);
    }
}
