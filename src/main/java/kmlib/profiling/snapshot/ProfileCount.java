package kmlib.profiling.snapshot;

import kmlib.profiling.ProfileCounter;

/**
 * What one counter accumulated at one place in the tree: the total across every
 * call of that row, how much of it the row counted itself, and the spread of
 * one call's worth.
 *
 * <p>The total is inclusive, by the same rule a row's time is: it holds what
 * the row counted plus what everything opened inside it counted, so a rebuild's
 * row says how many sector walks happened beneath it whoever performed them.
 *
 * <p>The self total is the divisor of a per-item cost. Only the items a row
 * handled with its own hands can be priced against its self time; the ones its
 * children handled are their rows' to price.
 *
 * <p>The per-call minimum and maximum are over every call of the row, calls
 * that counted none of it included - so a row that usually walks once and
 * occasionally not at all reads as a minimum of zero rather than as one.
 */
public final class ProfileCount {

    private final ProfileCounter counter;
    private final long total;
    private final long selfTotal;
    private final long minPerCall;
    private final long maxPerCall;

    public ProfileCount(
            ProfileCounter counter,
            long total,
            long selfTotal,
            long minPerCall,
            long maxPerCall) {

        this.counter = counter;
        this.total = total;
        this.selfTotal = selfTotal;
        this.minPerCall = minPerCall;
        this.maxPerCall = maxPerCall;
    }

    public ProfileCounter getCounter() {
        return counter;
    }

    /**
     * @return everything counted under this row, its own adds and its
     *         children's alike
     */
    public long getTotal() {
        return total;
    }

    /**
     * @return what this row counted itself, which is what its self time is
     *         spread over
     */
    public long getSelfTotal() {
        return selfTotal;
    }

    /**
     * @return the least this row counted in any one call
     */
    public long getMinPerCall() {
        return minPerCall;
    }

    /**
     * @return the most this row counted in any one call
     */
    public long getMaxPerCall() {
        return maxPerCall;
    }
}
