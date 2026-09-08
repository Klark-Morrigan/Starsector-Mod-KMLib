package kmlib.profiling.snapshot;

import kmlib.profiling.BudgetBreach;
import kmlib.profiling.ProfileCounter;
import kmlib.profiling.ProfileSection;

import java.util.List;

/**
 * One section at one place in the tree: what its calls cost, how much of that
 * it spent itself rather than beneath it, what it counted while it ran, what
 * its worst call was doing, whether any call went over what the section allows,
 * what the loops inside those calls ran, and the sections opened inside it.
 *
 * <p>A node is inclusive by construction - its total is everything that ran
 * under it - so self time is what the section costs on its own, which is the
 * number saying whether to look here or one level down.
 *
 * <p>The same section under two different parents is two nodes, because what a
 * call cost depends on what it was doing: a walk under a rebuild and a walk
 * under a hover are two facts, and one row averaging them is neither.
 *
 * <p>Immutable, and holding raw nanoseconds (the clock's unit) so the reader
 * decides how to present them.
 */
public final class ProfileNode {

    private final ProfileSection section;
    private final ProfileTiming timing;
    private final WorstCall worstCall;
    private final BudgetBreach budgetBreach;
    private final ProfileIterations iterations;
    private final long selfNanos;
    private final List<ProfileCount> counts;
    private final List<ProfileNode> children;

    /**
     * @param worstCall    what the worst call of this row was doing - the
     *                     slowest, or the one that broke its budget where one
     *                     did - or {@link WorstCall#NO_CALL} where none has
     *                     finished here
     * @param budgetBreach what a call of this row broke of its section's budget,
     *                     or {@link BudgetBreach#NO_BREACH} where none did
     * @param iterations   what the loops of this row ran, or
     *                     {@link ProfileIterations#NO_ITERATIONS} where its
     *                     calls ran none
     */
    public ProfileNode(
            ProfileSection section,
            ProfileTiming timing,
            WorstCall worstCall,
            BudgetBreach budgetBreach,
            ProfileIterations iterations,
            List<ProfileCount> counts,
            List<ProfileNode> children) {

        this.section = section;
        this.timing = timing;
        this.worstCall = worstCall;
        this.budgetBreach = budgetBreach;
        this.iterations = iterations;
        this.counts = List.copyOf(counts);
        this.children = List.copyOf(children);
        this.selfNanos = subtractChildNanos(timing.getTotalNanos(), this.children);
    }

    public ProfileSection getSection() {
        return section;
    }

    /**
     * @return how often this section ran here and what those calls cost,
     *         inclusive of everything opened inside it
     */
    public ProfileTiming getTiming() {
        return timing;
    }

    /**
     * @return what the worst call of this row was doing - its duration, its
     *         counters and its tag - or {@link WorstCall#NO_CALL} where a
     *         snapshot caught the row before any call of it had finished. The
     *         slowest call, until one breaks the section's budget: from then on
     *         it is the breach, which is the call a flagged row is read for
     */
    public WorstCall getWorstCall() {
        return worstCall;
    }

    /**
     * @return what a call of this row broke of its section's budget, or
     *         {@link BudgetBreach#NO_BREACH} where every call stayed inside it -
     *         which is every row of a capture nobody stated a bound in. The row
     *         is read as a finding rather than as a number wherever this says
     *         something, and the call that broke it is the one
     *         {@link #getWorstCall()} holds
     */
    public BudgetBreach getBudgetBreach() {
        return budgetBreach;
    }

    /**
     * @return what the loops of this row ran - how many turns, what each step of
     *         a turn came to, and the slowest turn - or
     *         {@link ProfileIterations#NO_ITERATIONS} where its calls ran no
     *         loop, which is most rows
     */
    public ProfileIterations getIterations() {
        return iterations;
    }

    /**
     * @return the nanoseconds spent in this section itself - its total less
     *         what the sections opened inside it took
     */
    public long getSelfNanos() {
        return selfNanos;
    }

    /**
     * @return what this section counted while it ran, in the order the counters
     *         were first added to; a counter nothing under this section ever
     *         touched is absent rather than present at zero, since "counted
     *         none" and "does not count this" are different facts
     */
    public List<ProfileCount> getCounts() {
        return counts;
    }

    /**
     * @return the sections opened inside this one, in the order they were
     *         first opened
     */
    public List<ProfileNode> getChildren() {
        return children;
    }

    /**
     * @param counter what is being looked for
     * @return what this row counted of it, or {@code null} where nothing under
     *         this row counted it at all - which a reader states as a blank
     *         rather than treats as an error
     */
    public ProfileCount findCount(ProfileCounter counter) {

        // An indexed identity scan: a counter is a registered value, so a match
        // is a reference comparison, and a row holds a handful of counts. Asked
        // of the row itself, so a reader is not left scanning the list it was
        // handed.
        for (var index = 0; index < counts.size(); index++) {
            var count = counts.get(index);
            if (count.getCounter() == counter) {
                return count;
            }
        }
        return null;
    }

    // Derived rather than accumulated, so self time cannot drift from the two
    // totals it is the difference of. Clamped at zero because a scope left open
    // never received the time of the children that closed under it, and a
    // negative duration in a report is worse than a missing one.
    private static long subtractChildNanos(long totalNanos, List<ProfileNode> children) {
        var childNanos = 0L;
        for (var child : children) {
            childNanos += child.timing.getTotalNanos();
        }
        return Math.max(0, totalNanos - childNanos);
    }
}
