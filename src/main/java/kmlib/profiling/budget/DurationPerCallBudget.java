package kmlib.profiling.budget;

import kmlib.profiling.ProfileCounter;
import kmlib.profiling.snapshot.BudgetBreach;

import java.util.function.LongSupplier;
import java.util.function.ToLongFunction;

/**
 * A bound on how long a single call may run.
 *
 * <p>What a frame can afford is a property of the machine and of what else it is
 * drawing, so the bound is asked for as the call closes rather than fixed when
 * the section was registered: a knob moved mid-session then holds the next call
 * to what it now says.
 */
final class DurationPerCallBudget implements ProfileBudget {

    // A bound of nothing is no bound. Stated this way so the knob behind one
    // carries its own off position, rather than needing a switch beside it that
    // could disagree with it.
    private static final long NO_BOUND_STATED = 0L;

    private final LongSupplier maxNanosPerCall;

    DurationPerCallBudget(LongSupplier maxNanosPerCall) {
        this.maxNanosPerCall = maxNanosPerCall;
    }

    @Override
    public BudgetBreach findBreachInCall(
            long elapsedNanos,
            ToLongFunction<ProfileCounter> readCallAmount) {

        var maxNanos = maxNanosPerCall.getAsLong();

        if (maxNanos <= NO_BOUND_STATED || elapsedNanos <= maxNanos) {
            return BudgetBreach.NO_BREACH;
        }
        return BudgetBreach.reportDurationBreach(maxNanos);
    }
}
