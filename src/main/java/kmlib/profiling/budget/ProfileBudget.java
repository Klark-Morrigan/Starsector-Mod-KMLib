package kmlib.profiling.budget;

import kmlib.profiling.ProfileCounter;
import kmlib.profiling.snapshot.BudgetBreach;

import java.util.function.LongSupplier;
import java.util.function.ToLongFunction;

/**
 * What one call of a section is allowed: how much of a counter it may reach, how
 * long it may run, or both.
 *
 * <p>Checked as a call closes, against that call alone. A row's totals are not
 * what a bound is about: a rebuild that walks the sector once per call is
 * correct however many rebuilds a session ran. What each bound is worth stating
 * in advance, and why one is fixed while the other is read late, are in this
 * package's README.
 *
 * <p>Its own package because a bound is a rule rather than a name: the sections
 * and counters a caller spells are vocabulary, while what may be done with them
 * is a judgement made in one place and reached only from the accumulation that
 * applies it. Nothing here knows how a span is tallied or how a row is printed.
 */
public interface ProfileBudget {

    /**
     * What a section that stated no budget carries, breached by nothing.
     *
     * <p>Shared and compared by reference, so the close of a section with no
     * budget - which is nearly every close on a per-frame path - costs one
     * comparison and allocates nothing.
     */
    ProfileBudget NO_BUDGET = (elapsedNanos, readCallAmount) -> BudgetBreach.NO_BREACH;

    /**
     * Allows one call {@code maxAmountPerCall} of {@code counter}, inclusive of
     * whatever ran inside it - which is what makes the bound checkable at the
     * top of a pass, whoever underneath it did the counting.
     *
     * @param counter          what is bounded
     * @param maxAmountPerCall the most one call may reach without breaching
     * @return the budget a section is registered with
     */
    static ProfileBudget allowingCountPerCall(ProfileCounter counter, long maxAmountPerCall) {
        return new CountPerCallBudget(counter, maxAmountPerCall);
    }

    /**
     * Allows one call the duration {@code maxNanosPerCall} states when the call
     * closes.
     *
     * <p>A bound of zero or less states no bound at all, so the knob behind one
     * carries its own off position rather than needing a switch beside it.
     *
     * @param maxNanosPerCall the most one call may take without breaching
     * @return the budget a section is registered with
     */
    static ProfileBudget allowingDurationPerCall(LongSupplier maxNanosPerCall) {
        return new DurationPerCallBudget(maxNanosPerCall);
    }

    /**
     * Judges one ended call.
     *
     * @param elapsedNanos   how long the call took
     * @param readCallAmount what the call counted of a given counter, inclusive
     *                       of everything opened inside it
     * @return what the call broke, or {@link BudgetBreach#NO_BREACH} where it
     *         stayed inside every bound
     */
    BudgetBreach findBreachInCall(long elapsedNanos, ToLongFunction<ProfileCounter> readCallAmount);

    /**
     * Binds this budget and {@code other} to one section, breached by whichever
     * of the two a call breaks first.
     *
     * @param other the further bound the same call is held to
     * @return the budget a section is registered with
     */
    default ProfileBudget combineWith(ProfileBudget other) {
        return new CombinedBudget(this, other);
    }
}
