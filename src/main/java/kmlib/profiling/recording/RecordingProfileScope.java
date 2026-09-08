package kmlib.profiling.recording;

import kmlib.profiling.BudgetBreach;
import kmlib.profiling.IterationScope;
import kmlib.profiling.ProfileCounter;
import kmlib.profiling.ProfilePhase;
import kmlib.profiling.ProfileSection;
import kmlib.profiling.snapshot.WorstCall;

import java.util.ArrayList;
import java.util.List;

/**
 * One entry on {@link RecordingProfiler}'s open stack: the node the span will
 * land on, the scope it was opened inside, when it started, what it has counted
 * so far, and what the caller has named it.
 *
 * <p>Closing is handed back to the profiler rather than settled here, because
 * what a close means depends on the whole stack - a scope closed while a
 * younger one is still open ends that one too, and a scope's counts are handed
 * to whatever it was open inside.
 *
 * <p>One class for both kinds of open, since a loop's turns are one more thing
 * a span may carry: which of the two a caller is holding is settled by the
 * interface it was handed back, so the turns of a loop cannot be marked on a
 * scope that was not opened over one.
 */
final class RecordingProfileScope implements IterationScope {

    private final RecordingProfiler profiler;
    private final ProfileNodeAccumulator node;
    private final long startNanos;

    // The scope this one was opened inside, and so the one its counts roll into
    // when it ends. Null on a root, whose node hangs off an origin rather than
    // off a row, and whose counts therefore stop with it.
    private final RecordingProfileScope parentScope;

    // What this scope's loop has run, or null on a scope opened over no loop -
    // which is nearly all of them, and which is why the state is not carried
    // where it would never be filled in.
    private final ScopeIterations iterations;

    // Created on the first count rather than with the scope, so a section that
    // counts nothing - which most on a per-frame path do - allocates only itself.
    private List<ScopeCount> counts;

    private String tag = WorstCall.NO_TAG;

    RecordingProfileScope(
            RecordingProfiler profiler,
            ProfileNodeAccumulator node,
            long startNanos,
            ScopeIterations iterations,
            RecordingProfileScope parentScope) {

        this.profiler = profiler;
        this.node = node;
        this.startNanos = startNanos;
        this.iterations = iterations;
        this.parentScope = parentScope;
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
        this.tag = WorstCall.normaliseTag(tag);
    }

    @Override
    public void beginIteration(String tag) {
        // Null only where no caller holds this as an iteration scope, which is
        // what an open over no loop hands back.
        if (iterations != null) {
            iterations.beginIteration(profiler.readClockNanos(), tag);
        }
    }

    @Override
    public void markPhase(ProfilePhase phase) {
        if (iterations != null) {
            iterations.markPhase(profiler.readClockNanos(), phase);
        }
    }

    @Override
    public void endIteration() {
        if (iterations != null) {
            iterations.endIteration(profiler.readClockNanos());
        }
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
     * @return what the caller named this call, or {@link WorstCall#NO_TAG} where
     *         it named nothing - which is what says which call a budget breach
     *         was reported for
     */
    String getTag() {
        return tag;
    }

    /**
     * Rolls what this scope counted into the scope it ran inside, so that row is
     * inclusive of it while its self total stays what it counted itself.
     *
     * <p>Nothing to do on a root: its node hangs off an origin rather than off a
     * row, so whatever happened to be open when it was opened is not a call that
     * can answer for what it counted.
     */
    void handCountsToParentScope() {

        if (parentScope != null) {
            parentScope.receiveChildCounts(this);
        }
    }

    /**
     * @return what this call broke of its section's budget, or
     *         {@link BudgetBreach#NO_BREACH} where it broke nothing
     */
    BudgetBreach recordSpan(long endNanos) {

        var breach =
            node.addSpan(endNanos - startNanos, counts == null ? List.of() : counts, tag);

        // The turns go in with the span rather than as they run: a row is read
        // as one thing, and a loop half way through its cells is not a fact
        // about what a call of that row costs.
        if (iterations != null) {
            node.addIterations(iterations.getTally());
        }
        return breach;
    }

    // Takes on what a scope opened inside this one counted. Its totals rather
    // than its self amounts, so a count added three levels down reaches every
    // row above it exactly once.
    private void receiveChildCounts(RecordingProfileScope child) {

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
}
