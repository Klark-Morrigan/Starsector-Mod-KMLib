package kmlib.profiling;

/**
 * What one call broke, stated in the terms a reader acts on: what it did, and
 * what its section was allowed.
 *
 * <p>Carried rather than recomputed, because a bound may be a knob a player
 * moves: a row read after the knob moved would otherwise be judged against a
 * budget it was never measured against, and would stop reading as a breach at
 * all.
 *
 * <p>The observed value is stated only where the report does not already carry
 * it - a call that counted three walks against a budget of one says so here,
 * while one that ran too long says only what it was allowed, its duration
 * already being the row's own column.
 */
public final class BudgetBreach {

    /**
     * What a call inside its budget - and every call of a section that stated
     * none - comes back with. Shared, since every such call says the same
     * nothing.
     */
    public static final BudgetBreach NO_BREACH = new BudgetBreach("");

    private final String statement;

    private BudgetBreach(String statement) {
        this.statement = statement;
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

    /**
     * Records a broken bound.
     *
     * <p>Package private: a breach is what a {@link ProfileBudget} answers with
     * and never something a caller states about itself.
     *
     * @param statement what the call did against what it was allowed
     * @return the breach a row is marked with
     */
    static BudgetBreach reportBreach(String statement) {
        return new BudgetBreach(statement);
    }
}
