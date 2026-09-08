package kmlib.profiling.recording;

import kmlib.profiling.snapshot.BudgetBreach;
import kmlib.profiling.snapshot.CallCount;
import kmlib.profiling.snapshot.WorstCall;

import java.util.ArrayList;
import java.util.List;

/**
 * The worst call one node has seen so far, kept with what it was doing: the
 * slowest, until one breaks what the section allows.
 *
 * <p>Holds whether it has seen a call itself rather than reading the node's
 * tally of them: what makes a call the worst is that nothing slower has closed,
 * which has nothing to do with how a span or a counter is folded in, and a rule
 * sharing another's state is a rule that breaks when that other one moves.
 *
 * <p>The breach it is holding is what says the rule has changed, rather than a
 * flag beside it: once one has been kept the record is the breach's and duration
 * decides nothing, and a row's finding and the call that produced it can never
 * come apart because they are one answer.
 */
final class WorstCallAccumulator {

    private boolean hasObservedCall;
    private long durationNanos;
    private String tag = WorstCall.NO_TAG;
    private List<CallCount> counts = List.of();
    private BudgetBreach budgetBreach = BudgetBreach.NO_BREACH;

    /**
     * Offers one ended call, kept while nothing slower has closed - so what is
     * held is the call the row's maximum reports, and what it was doing is read
     * beside that duration rather than looked for in a log afterwards - and kept
     * unconditionally where it broke its budget, since a fault can be over
     * faster than the calls that behaved.
     *
     * @param elapsedNanos how long the call took
     * @param callCounts   what the call counted, empty when it counted nothing
     * @param tag          what the caller named the call, {@link WorstCall#NO_TAG}
     *                     when it named nothing
     * @param breach       what the call broke of its section's budget,
     *                     {@link BudgetBreach#NO_BREACH} when it broke nothing
     */
    void addCall(
            long elapsedNanos,
            List<ScopeCount> callCounts,
            String tag,
            BudgetBreach breach) {

        if (breach.hasBreached()) {
            budgetBreach = breach;
        } else if (!isWorseThanTheRecord(elapsedNanos)) {
            return;
        }
        hasObservedCall = true;
        durationNanos = elapsedNanos;
        this.tag = tag;
        counts = copyCallCounts(callCounts);
    }

    /**
     * @return the record a snapshot is read from, or {@link WorstCall#NO_CALL}
     *         where no call has finished here yet
     */
    WorstCall buildWorstCall() {
        return hasObservedCall ? new WorstCall(durationNanos, tag, counts) : WorstCall.NO_CALL;
    }

    /**
     * @return what the kept call broke, which is what marks the row a snapshot
     *         is built from, or {@link BudgetBreach#NO_BREACH} where no call
     *         here has broken anything
     */
    BudgetBreach getBudgetBreach() {
        return budgetBreach;
    }

    // A call takes the record while nothing slower has closed - and never once a
    // breach has been kept, a call that stayed inside the budget not being what
    // a flagged row is read for.
    private boolean isWorseThanTheRecord(long elapsedNanos) {
        return !budgetBreach.hasBreached() && (!hasObservedCall || elapsedNanos > durationNanos);
    }

    // What one call's counters stood at, frozen out of the tallies the ended
    // scope was still adding to. Copied only when a call turns out to be the
    // worst so far, which after the first few calls of a row is rare.
    private static List<CallCount> copyCallCounts(List<ScopeCount> callCounts) {

        if (callCounts.isEmpty()) {
            return List.of();
        }
        var counts = new ArrayList<CallCount>(callCounts.size());

        for (var callCount : callCounts) {
            counts.add(new CallCount(callCount.getCounter(), callCount.getTotalAmount()));
        }
        return List.copyOf(counts);
    }
}
