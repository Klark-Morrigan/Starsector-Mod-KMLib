package kmlib.profiling;

import java.util.List;

/**
 * One section's accumulated timings at one place in the tree: how often it ran
 * under this parent, its total, fastest and slowest span, how much of that
 * total it spent itself rather than beneath it, what it counted while it ran,
 * and the sections opened inside it.
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
 * decides how to present them; {@link TimingReport} converts to milliseconds.
 */
public final class ProfileNode {

    private final ProfileSection section;
    private final long count;
    private final long totalNanos;
    private final long minNanos;
    private final long maxNanos;
    private final long selfNanos;
    private final List<ProfileCount> counts;
    private final List<ProfileNode> children;

    public ProfileNode(
            ProfileSection section,
            long count,
            long totalNanos,
            long minNanos,
            long maxNanos,
            List<ProfileCount> counts,
            List<ProfileNode> children) {

        this.section = section;
        this.count = count;
        this.totalNanos = totalNanos;
        this.minNanos = minNanos;
        this.maxNanos = maxNanos;
        this.counts = List.copyOf(counts);
        this.children = List.copyOf(children);
        this.selfNanos = subtractChildNanos(totalNanos, this.children);
    }

    public ProfileSection getSection() {
        return section;
    }

    public long getCount() {
        return count;
    }

    public long getTotalNanos() {
        return totalNanos;
    }

    public long getMinNanos() {
        return minNanos;
    }

    public long getMaxNanos() {
        return maxNanos;
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
     * @return the mean nanoseconds per run, or 0 when nothing was recorded
     *         (guards the divide so an empty snapshot is still printable)
     */
    public long getAverageNanos() {
        return count == 0 ? 0 : totalNanos / count;
    }

    // Derived rather than accumulated, so self time cannot drift from the two
    // totals it is the difference of. Clamped at zero because a scope left open
    // never received the time of the children that closed under it, and a
    // negative duration in a report is worse than a missing one.
    private static long subtractChildNanos(long totalNanos, List<ProfileNode> children) {
        var childNanos = 0L;
        for (var child : children) {
            childNanos += child.totalNanos;
        }
        return Math.max(0, totalNanos - childNanos);
    }
}
