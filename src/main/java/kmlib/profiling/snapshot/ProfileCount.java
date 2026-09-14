package kmlib.profiling.snapshot;

import kmlib.profiling.ProfileCounter;

/**
 * What one counter accumulated at one place in the tree: which counter it was,
 * how much of it the row holds, and the most any one call of it reached.
 *
 * <p>The totals are held as their own value rather than as two more longs on
 * this one, for the reason {@link ProfileTiming} groups the durations it
 * reports: numbers of the same type that only mean anything in pairs are
 * numbers a call site can transpose, and a row claiming it counted everything
 * itself reads as a fact rather than as a mistake. The maximum stands alone
 * because it pairs with nothing - there is no per-call minimum beside it to be
 * handed over the wrong way round.
 */
public final class ProfileCount {

    private final ProfileCounter counter;
    private final CountTotals totals;
    private final long maxPerCall;

    /**
     * @param counter    what was counted
     * @param totals     how much of it the row reached, and how much it reached
     *                   itself
     * @param maxPerCall the most any one call of the row counted, everything
     *                   opened inside that call included
     */
    public ProfileCount(ProfileCounter counter, CountTotals totals, long maxPerCall) {
        this.counter = counter;
        this.totals = totals;
        this.maxPerCall = maxPerCall;
    }

    public ProfileCounter getCounter() {
        return counter;
    }

    /**
     * @return how much of this counter the row holds, inclusive and its own
     */
    public CountTotals getTotals() {
        return totals;
    }

    /**
     * The most any one call of the row counted.
     *
     * <p>Per call rather than in total, because that is what a bound is broken by
     * and what a reading of one counter ranks on: the row that walked the sector
     * three times in a single call is the one to look at, not the row that walked
     * it once on each of a thousand frames.
     *
     * @return the largest amount one call reached, everything opened inside that
     *         call included
     */
    public long getMaxPerCall() {
        return maxPerCall;
    }
}
