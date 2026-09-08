package kmlib.profiling;

import java.util.Locale;
import java.util.function.ToLongFunction;

/**
 * A bound on how much of one counter a single call may reach.
 *
 * <p>The bound a rule about the work is stated as: one sector walk per rebuild,
 * one pass over the markets per refresh. What breaks it is a second traversal
 * nobody meant to make, which no duration column can be read as.
 */
final class CountPerCallBudget implements ProfileBudget {

    // What a call reached against what it was allowed. The amount is stated
    // because no column of the report carries one call's worth of a counter, so
    // the breach is the only place it is said.
    private static final String BREACH_FORMAT = "%d %s, %d allowed per call";

    private final ProfileCounter counter;
    private final long maxAmountPerCall;

    CountPerCallBudget(ProfileCounter counter, long maxAmountPerCall) {
        this.counter = counter;
        this.maxAmountPerCall = maxAmountPerCall;
    }

    @Override
    public BudgetBreach findBreachInCall(
            long elapsedNanos,
            ToLongFunction<ProfileCounter> readCallAmount) {

        var amount = readCallAmount.applyAsLong(counter);

        if (amount <= maxAmountPerCall) {
            return BudgetBreach.NO_BREACH;
        }
        return BudgetBreach.reportBreach(String.format(
            Locale.ROOT, BREACH_FORMAT, amount, counter.getName(), maxAmountPerCall));
    }
}
