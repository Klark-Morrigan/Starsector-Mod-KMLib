package kmlib.profiling.recording;

import kmlib.profiling.snapshot.CallCount;
import kmlib.profiling.snapshot.WorstCall;

import java.util.ArrayList;
import java.util.List;

/**
 * The slowest call one node has seen so far, kept with what it was doing.
 *
 * <p>Holds whether it has seen a call itself rather than reading the node's
 * tally of them: what makes a call the worst is that nothing slower has closed,
 * which has nothing to do with how a span or a counter is folded in, and a rule
 * sharing another's state is a rule that breaks when that other one moves.
 *
 * <p>A call that broke the section's budget displaces that rule. Once one has
 * been kept, the record is the breach's and duration decides nothing: what a
 * reader wants beside a flagged row is the call that broke it, and a fault can
 * be over faster than the calls that behaved.
 */
final class WorstCallAccumulator {

    private boolean hasObservedCall;
    private boolean hasKeptBreachingCall;
    private long durationNanos;
    private String tag = WorstCall.NO_TAG;
    private List<CallCount> counts = List.of();

    /**
     * Offers one ended call, which is kept only while nothing slower has
     * closed - so what is held is always the call the row's maximum reports,
     * and what it was doing is read beside that duration rather than looked for
     * in a log afterwards.
     *
     * @param elapsedNanos how long the call took
     * @param callCounts   what the call counted, empty when it counted nothing
     * @param tag          what the caller named the call, {@link WorstCall#NO_TAG}
     *                     when it named nothing
     */
    void addCall(long elapsedNanos, List<ScopeCount> callCounts, String tag) {

        if (hasKeptBreachingCall || (hasObservedCall && elapsedNanos <= durationNanos)) {
            return;
        }
        keepCall(elapsedNanos, callCounts, tag);
    }

    /**
     * Takes one ended call that broke its section's budget, which becomes the
     * record whatever it took and whatever has closed before it.
     *
     * @param elapsedNanos how long the call took
     * @param callCounts   what the call counted, empty when it counted nothing
     * @param tag          what the caller named the call, {@link WorstCall#NO_TAG}
     *                     when it named nothing
     */
    void keepBreachingCall(long elapsedNanos, List<ScopeCount> callCounts, String tag) {

        hasKeptBreachingCall = true;
        keepCall(elapsedNanos, callCounts, tag);
    }

    /**
     * @return the record a snapshot is read from, or {@link WorstCall#NO_CALL}
     *         where no call has finished here yet
     */
    WorstCall buildWorstCall() {
        return hasObservedCall ? new WorstCall(durationNanos, tag, counts) : WorstCall.NO_CALL;
    }

    private void keepCall(long elapsedNanos, List<ScopeCount> callCounts, String tag) {

        hasObservedCall = true;
        durationNanos = elapsedNanos;
        this.tag = tag;
        counts = copyCallCounts(callCounts);
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
