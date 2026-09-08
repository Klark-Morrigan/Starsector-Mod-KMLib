package kmlib.profiling.snapshot;

import kmlib.profiling.ProfileCounter;
import kmlib.time.Timings;

import java.util.Locale;

/**
 * What one call broke of what its section allows, stated in the terms a reader
 * acts on: what the call did, and what it was allowed.
 *
 * <p>Carried rather than recomputed, because a bound may be a knob a player
 * moves: a row read after the knob moved would otherwise be judged against a
 * budget it was never measured against, and would stop reading as a breach at
 * all.
 *
 * <p>The one value here that composes its own sentence. Every other row value is
 * numbers a view formats, but a breach is read in two places that cannot share a
 * formatter - the log, as the call closes, and the table afterwards - so the one
 * spelling of it lives with the fact rather than in whichever of the two a
 * reader happened to look at.
 *
 * <p>Made from what was measured rather than from prose, so a breach can only
 * say something a bound could actually have produced. The observed value is
 * stated only where a table does not already carry it: a call that counted three
 * walks against a budget of one says so here, while one that ran too long says
 * only what it was allowed, its duration already being the row's own column.
 */
public final class BudgetBreach {

    /**
     * What a call inside its budget - and every call of a section that stated
     * none - comes back with. Shared, since every such call says the same
     * nothing.
     */
    public static final BudgetBreach NO_BREACH = new BudgetBreach("");

    private static final String COUNT_BREACH_FORMAT = "%d %s, %d allowed per call";
    private static final String DURATION_BREACH_FORMAT = "%s allowed per call";

    private final String statement;

    private BudgetBreach(String statement) {
        this.statement = statement;
    }

    /**
     * Reports a call that reached more of a counter than it was allowed.
     *
     * @param counter         what was bounded
     * @param amount          what the call reached of it
     * @param allowedPerCall  the most a call was allowed to reach
     * @return the breach a row is marked with
     */
    public static BudgetBreach reportCountBreach(
            ProfileCounter counter,
            long amount,
            long allowedPerCall) {

        return new BudgetBreach(String.format(
            Locale.ROOT, COUNT_BREACH_FORMAT, amount, counter.getName(), allowedPerCall));
    }

    /**
     * Reports a call that ran longer than it was allowed.
     *
     * @param allowedNanosPerCall the longest a call was allowed to run
     * @return the breach a row is marked with
     */
    public static BudgetBreach reportDurationBreach(long allowedNanosPerCall) {
        return new BudgetBreach(String.format(
            Locale.ROOT, DURATION_BREACH_FORMAT, Timings.formatMillis(allowedNanosPerCall)));
    }

    /**
     * @return whether a bound was actually broken, which is what marks the row
     *         and what decides whether anything is said about it
     */
    public boolean hasBreached() {
        return !statement.isEmpty();
    }

    /**
     * @return what the call did against what it was allowed, e.g.
     *         {@code "3 walks, 1 allowed per call"}; empty where nothing was
     *         broken
     */
    public String describeBreach() {
        return statement;
    }

    @Override
    public String toString() {
        return statement;
    }
}
