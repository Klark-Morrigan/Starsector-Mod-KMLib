package kmlib.profiling.recording;

import kmlib.profiling.ProfileCounter;
import kmlib.profiling.ProfileScope;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.snapshot.WorstCall;
import kmlib.text.KmlibStrings;

import java.util.ArrayList;
import java.util.List;

/**
 * One entry on {@link RecordingProfiler}'s open stack: the node the span will
 * land on, when it started, what it has counted so far, and what the caller
 * has named it.
 *
 * <p>Closing is handed back to the profiler rather than settled here, because
 * what a close means depends on the whole stack - a scope closed while a
 * younger one is still open ends that one too, and a scope's counts are handed
 * to whatever it was open inside.
 */
final class RecordingProfileScope implements ProfileScope {

    private final RecordingProfiler profiler;
    private final ProfileNodeAccumulator node;
    private final long startNanos;

    // Created on the first count rather than with the scope, so a section that
    // counts nothing - which most on a per-frame path do - allocates only itself.
    private List<ScopeCount> counts;

    private String tag = WorstCall.NO_TAG;

    RecordingProfileScope(RecordingProfiler profiler, ProfileNodeAccumulator node, long startNanos) {
        this.profiler = profiler;
        this.node = node;
        this.startNanos = startNanos;
    }

    @Override
    public void addCount(ProfileCounter counter, long amount) {
        if (counts == null) {
            counts = new ArrayList<>();
        }
        ScopeCount.resolveCountIn(counts, counter).addSelfAmount(amount);
    }

    @Override
    public void tagCall(String tag) {
        // A blank name is no name: a caller composing one out of what it
        // happens to hold may end up with an empty string, and a record whose
        // name is a run of spaces reads as a name that was lost rather than as
        // one that was never given.
        this.tag = KmlibStrings.hasText(tag) ? tag : WorstCall.NO_TAG;
    }

    @Override
    public void close() {
        profiler.closeScope(this);
    }

    ProfileNodeAccumulator resolveChildNode(ProfileSection childSection) {
        return node.resolveChildNode(childSection);
    }

    ProfileSection getSection() {
        return node.getSection();
    }

    /**
     * Takes on what a scope opened inside this one counted, so this scope's row
     * is inclusive of it while its self total stays what this scope counted.
     *
     * @param child the scope that has just ended inside this one
     */
    void receiveChildCounts(RecordingProfileScope child) {

        if (child.counts == null) {
            return;
        }
        if (counts == null) {
            counts = new ArrayList<>();
        }
        for (var childCount : child.counts) {
            ScopeCount.resolveCountIn(counts, childCount.getCounter())
                .addChildAmount(childCount.getTotalAmount());
        }
    }

    void recordSpan(long endNanos) {
        node.addSpan(endNanos - startNanos, counts == null ? List.of() : counts, tag);
    }
}
