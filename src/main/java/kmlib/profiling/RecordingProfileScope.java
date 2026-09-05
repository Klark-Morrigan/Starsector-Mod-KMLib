package kmlib.profiling;

/**
 * One entry on {@link RecordingProfiler}'s open stack: the node the span will
 * land on, and when it started.
 *
 * <p>Closing is handed back to the profiler rather than settled here, because
 * what a close means depends on the whole stack - a scope closed while a
 * younger one is still open ends that one too.
 */
final class RecordingProfileScope implements ProfileScope {

    private final RecordingProfiler profiler;
    private final ProfileNodeAccumulator node;
    private final long startNanos;

    RecordingProfileScope(RecordingProfiler profiler, ProfileNodeAccumulator node, long startNanos) {
        this.profiler = profiler;
        this.node = node;
        this.startNanos = startNanos;
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

    void recordSpan(long endNanos) {
        node.addSpan(endNanos - startNanos);
    }
}
