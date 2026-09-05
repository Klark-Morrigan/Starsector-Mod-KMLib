package kmlib.profiling;

import java.util.ArrayList;
import java.util.List;

/**
 * One entry on {@link RecordingProfiler}'s open stack: the node the span will
 * land on, when it started, and what it has counted so far.
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
        node.addSpan(endNanos - startNanos, counts == null ? List.of() : counts);
    }
}
