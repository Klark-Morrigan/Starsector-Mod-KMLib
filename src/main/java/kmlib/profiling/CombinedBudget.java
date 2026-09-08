package kmlib.profiling;

import java.util.function.ToLongFunction;

/**
 * Two bounds held over one section's calls.
 *
 * <p>The first one broken is what the call is reported as breaking. A call that
 * walked the sector twice and took too long doing it has one fault, and the
 * bound stated first is the one its section was written to hold.
 */
final class CombinedBudget implements ProfileBudget {

    private final ProfileBudget firstBudget;
    private final ProfileBudget secondBudget;

    CombinedBudget(ProfileBudget firstBudget, ProfileBudget secondBudget) {
        this.firstBudget = firstBudget;
        this.secondBudget = secondBudget;
    }

    @Override
    public BudgetBreach findBreachInCall(
            long elapsedNanos,
            ToLongFunction<ProfileCounter> readCallAmount) {

        var firstBreach = firstBudget.findBreachInCall(elapsedNanos, readCallAmount);

        return firstBreach.hasBreached()
            ? firstBreach
            : secondBudget.findBreachInCall(elapsedNanos, readCallAmount);
    }
}
