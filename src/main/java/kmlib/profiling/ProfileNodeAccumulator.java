package kmlib.profiling;

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
    private final long[] callsPerBucket = new long[DurationBuckets.countBuckets()];

    private long count;
    private long totalNanos;
    private long minNanos;
    private long maxNanos;

    private long worstNanos;
    private String worstTag = WorstCall.NO_TAG;
    private List<CallCount> worstCounts = List.of();

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

        // An indexed identity scan rather than a map lookup: a node holds a
        // handful of children, a section is a registered value, and this runs on
        // every open - so no name is hashed and no iterator allocated.
        for (var index = 0; index < siblings.size(); index++) {
            var sibling = siblings.get(index);
            if (sibling.section == section) {
                return sibling;
            }
        }
        var opened = new ProfileNodeAccumulator(section);
        siblings.add(opened);
        return opened;
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
     * @param tag          what the caller named the call, {@link WorstCall#NO_TAG}
     *                     when it named nothing
     */
    void addSpan(long elapsedNanos, List<ScopeCount> callCounts, String tag) {

        // Before the call is counted, since a counter first added to in this
        // call has to know how many calls preceded it without it - and since
        // the worst call is told from the others by there being none before it.
        recordCallCounts(callCounts);
        recordWorstCall(elapsedNanos, callCounts, tag);

        // The first span sets both bounds rather than being folded into
        // sentinels the snapshot would then have to undo. A node with no span -
        // opened while the snapshot was taken, or left open by a caller - then
        // reads as the zeroes it holds, with no rule about what a row means.
        if (count == 0) {
            minNanos = elapsedNanos;
            maxNanos = elapsedNanos;
        } else {
            minNanos = Math.min(minNanos, elapsedNanos);
            maxNanos = Math.max(maxNanos, elapsedNanos);
        }
        count++;
        totalNanos += elapsedNanos;
        callsPerBucket[DurationBuckets.resolveBucketIndex(elapsedNanos)]++;
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
            new ProfileTiming(count, totalNanos, minNanos, maxNanos, buildBuckets()),
            count == 0 ? WorstCall.NO_CALL : new WorstCall(worstNanos, worstTag, worstCounts),
            counts,
            childNodes);
    }

    // The shared empty spread where no call has finished, so a tree full of
    // rows a snapshot caught mid-call copies no arrays of zeroes.
    private DurationBuckets buildBuckets() {
        return count == 0 ? DurationBuckets.NO_CALLS : new DurationBuckets(callsPerBucket);
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

    // Every counter this node has ever seen takes a value for the call that has
    // just ended, zero included: a call that counted none of something is what
    // makes a minimum zero, and a spread that only saw the calls which counted
    // would read as a floor no call ever went under.
    private void recordCallCounts(List<ScopeCount> callCounts) {

        for (var countAccumulator : countAccumulators) {

            var callCount = CounterLookup.findByCounter(
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

            var alreadyOpened = CounterLookup.findByCounter(
                countAccumulators, ProfileCountAccumulator::getCounter, callCount.getCounter());

            if (alreadyOpened != null) {
                continue;
            }
            var opened = new ProfileCountAccumulator(callCount.getCounter(), count);

            opened.addCall(callCount.getSelfAmount(), callCount.getTotalAmount());
            countAccumulators.add(opened);
        }
    }

    // Kept only while nothing slower has closed, so what is held is always the
    // call the row's maximum reports - and so what that call was doing is read
    // beside the duration rather than looked for in a log afterwards. The first
    // call takes the record by the same rule the bounds are set by: there is
    // nothing before it to be slower than.
    private void recordWorstCall(long elapsedNanos, List<ScopeCount> callCounts, String tag) {

        if (count > 0 && elapsedNanos <= worstNanos) {
            return;
        }
        worstNanos = elapsedNanos;
        worstTag = tag;
        worstCounts = copyCallCounts(callCounts);
    }
}
