package kmlib.profiling.snapshot;

import kmlib.profiling.ProfileCounter;

/**
 * What one counter accumulated at one place in the tree: which counter it was,
 * how much of it the row holds, and how far one call's worth spread.
 *
 * <p>The two halves are held as their own values rather than as four longs on
 * this one, for the reason {@link ProfileTiming} groups the durations it
 * reports: numbers of the same type that only mean anything in pairs are
 * numbers a call site can transpose, and a row claiming it counted everything
 * itself reads as a fact rather than as a mistake.
 */
public final class ProfileCount {

    private final ProfileCounter counter;
    private final CountTotals totals;
    private final CountSpread spread;

    public ProfileCount(ProfileCounter counter, CountTotals totals, CountSpread spread) {
        this.counter = counter;
        this.totals = totals;
        this.spread = spread;
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
     * @return how far one call's worth of it spread across the row's calls
     */
    public CountSpread getSpread() {
        return spread;
    }
}
