package kmlib.profiling.snapshot;

import kmlib.profiling.ProfileCounter;

/**
 * What one counter stood at on one ended call: the counter, and the amount that
 * call reached.
 *
 * <p>The amount is inclusive of everything opened inside the call, by the same
 * rule a row's total is, so it is the work the call's duration bought - a
 * rebuild's slowest call is read against the systems walked beneath it, whoever
 * walked them - and so it can be held against the per-call maximum the row
 * reports for the same counter.
 *
 * <p>One call's worth and immutable, because it is kept as part of the record
 * of the call it belonged to: a later call must not be able to rewrite what an
 * earlier one counted.
 */
public final class CallCount {

    private final ProfileCounter counter;
    private final long amount;

    public CallCount(ProfileCounter counter, long amount) {
        this.counter = counter;
        this.amount = amount;
    }

    public ProfileCounter getCounter() {
        return counter;
    }

    /**
     * @return what the call counted of it, everything opened inside the call
     *         included
     */
    public long getAmount() {
        return amount;
    }
}
