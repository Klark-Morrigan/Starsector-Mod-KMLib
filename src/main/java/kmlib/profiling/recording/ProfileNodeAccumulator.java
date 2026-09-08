package kmlib.profiling.recording;

import kmlib.profiling.BudgetBreach;
import kmlib.profiling.ProfileBudget;
import kmlib.profiling.ProfileCounter;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.snapshot.ProfileCount;
import kmlib.profiling.snapshot.ProfileIterations;
import kmlib.profiling.snapshot.ProfileNode;

import java.util.ArrayList;
import java.util.List;

/**
 * The mutable node {@link RecordingProfiler} adds to - one section under one
 * parent - together with what its calls counted and the nodes opened inside it,
 * both in first-seen order.
 *
 * <p>Kept apart from the {@link ProfileNode} a snapshot hands out: what a
 * reader is given must not change while it is being read, and what the profiler
 * adds to must stay cheap enough for a path that runs every frame.
 */
final class ProfileNodeAccumulator {

    private final ProfileSection section;
    private final List<ProfileNodeAccumulator> children = new ArrayList<>();
    private final List<ProfileCountAccumulator> countAccumulators = new ArrayList<>();
    private final SpanAccumulator spans = new SpanAccumulator();
    private final WorstCallAccumulator worstCall = new WorstCallAccumulator();

    // Created when the first loop closes here rather than with the node, since
    // its slots are sized by the section that loop was opened on - and since
    // most rows never run one.
    private IterationTally iterations;

    // What the last call to break this section's budget did. Kept once broken:
    // a finding is about the pass that produced it, and a later call staying
    // inside the bound does not undo the one that did not.
    private BudgetBreach budgetBreach = BudgetBreach.NO_BREACH;

    ProfileNodeAccumulator(ProfileSection section) {
        this.section = section;
    }

    /**
     * Finds the node {@code section} holds among {@code siblings}, appending
     * one the first time that section is opened there.
     *
     * <p>Static and taking the list, because the roots of the tree are siblings
     * with no node above them and are resolved by the same rule as a node's
     * children.
     *
     * @param siblings the nodes at one level, in first-opened order
     * @param section  the section being opened
     * @return the node that level keeps for the section
     */
    static ProfileNodeAccumulator resolveNodeIn(
            List<ProfileNodeAccumulator> siblings,
            ProfileSection section) {

        return IdentityLookup.resolveByKey(
            siblings, ProfileNodeAccumulator::getSection, section, ProfileNodeAccumulator::new);
    }

    ProfileSection getSection() {
        return section;
    }

    ProfileNodeAccumulator resolveChildNode(ProfileSection childSection) {
        return resolveNodeIn(children, childSection);
    }

    /**
     * Folds one ended call into this node.
     *
     * @param elapsedNanos how long the call took
     * @param callCounts   what the call counted, empty when it counted nothing
     * @param tag          what the caller named the call, empty when it named
     *                     nothing
     * @return what this call broke of its section's budget, or
     *         {@link BudgetBreach#NO_BREACH} where it broke nothing - handed
     *         back rather than logged here, since saying it once per section is
     *         a fact about the whole capture and not about one row
     */
    BudgetBreach addSpan(long elapsedNanos, List<ScopeCount> callCounts, String tag) {

        // Before the span is folded in, since a counter first added to in this
        // call has to know how many calls preceded it without it, and the spans
        // are what remember that.
        recordCallCounts(callCounts);

        var breach = findBreachInCall(elapsedNanos, callCounts);

        // A breaching call is what the row is read for, so it takes the record
        // whether or not it was the slowest: a walk too many can be over in
        // microseconds and still be the only call worth looking at.
        if (breach.hasBreached()) {
            budgetBreach = breach;
            worstCall.keepBreachingCall(elapsedNanos, callCounts, tag);
        } else {
            worstCall.addCall(elapsedNanos, callCounts, tag);
        }
        spans.addSpan(elapsedNanos);
        return breach;
    }

    /**
     * Folds one ended call's loop into this node.
     *
     * <p>Apart from the span, since a turn and a call are different things to
     * average over: what a bake costs is the span, and what a cell costs is the
     * turn.
     *
     * @param callIterations what the call's turns came to
     */
    void addIterations(IterationTally callIterations) {

        if (iterations == null) {
            iterations = new IterationTally(callIterations.getSection());
        }
        iterations.addTally(callIterations);
    }

    /**
     * @return this node and everything under it, copied into the immutable form
     *         a snapshot is read from
     */
    ProfileNode buildNode() {

        var childNodes = new ArrayList<ProfileNode>(children.size());
        var counts = new ArrayList<ProfileCount>(countAccumulators.size());

        for (var child : children) {
            childNodes.add(child.buildNode());
        }
        for (var countAccumulator : countAccumulators) {
            counts.add(countAccumulator.buildCount());
        }
        return new ProfileNode(
            section,
            spans.buildTiming(),
            worstCall.buildWorstCall(),
            budgetBreach,
            iterations == null ? ProfileIterations.NO_ITERATIONS : iterations.buildIterations(),
            counts,
            childNodes);
    }

    // What one ended call broke. The section's budget is compared by reference
    // against the shared nothing first, because most sections state none and a
    // close on a per-frame path must not allocate the lookup a check needs.
    private BudgetBreach findBreachInCall(long elapsedNanos, List<ScopeCount> callCounts) {

        var budget = section.getBudget();

        if (budget == ProfileBudget.NO_BUDGET) {
            return BudgetBreach.NO_BREACH;
        }
        return budget.findBreachInCall(
            elapsedNanos, counter -> readCallAmount(callCounts, counter));
    }

    // What the call counted of one counter, inclusive of everything opened
    // inside it - the same quantity the row's per-call maximum is taken over, so
    // a bound and the column a reader checks it against are one number. Nothing
    // counted is zero, a call that touched a counter not at all having reached
    // none of it.
    private static long readCallAmount(List<ScopeCount> callCounts, ProfileCounter counter) {

        var callCount = IdentityLookup.findByKey(callCounts, ScopeCount::getCounter, counter);

        return callCount == null ? 0 : callCount.getTotalAmount();
    }

    // Every counter this node has ever seen takes a value for the call that has
    // just ended, zero included: a call that counted none of something is what
    // makes a minimum zero, and a spread that only saw the calls which counted
    // would read as a floor no call ever went under.
    private void recordCallCounts(List<ScopeCount> callCounts) {

        for (var countAccumulator : countAccumulators) {

            var callCount = IdentityLookup.findByKey(
                callCounts, ScopeCount::getCounter, countAccumulator.getCounter());

            if (callCount == null) {
                countAccumulator.addCall(0, 0);
            } else {
                countAccumulator.addCall(callCount.getSelfAmount(), callCount.getTotalAmount());
            }
        }

        // Whatever the loop above did not already hold: the counters this call
        // is the first of this row's to touch.
        for (var callCount : callCounts) {

            var alreadyOpened = IdentityLookup.findByKey(
                countAccumulators, ProfileCountAccumulator::getCounter, callCount.getCounter());

            if (alreadyOpened != null) {
                continue;
            }
            var opened =
                new ProfileCountAccumulator(callCount.getCounter(), spans.getCallCount());

            opened.addCall(callCount.getSelfAmount(), callCount.getTotalAmount());
            countAccumulators.add(opened);
        }
    }
}
