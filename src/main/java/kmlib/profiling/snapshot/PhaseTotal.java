package kmlib.profiling.snapshot;

import kmlib.profiling.ProfilePhase;

/**
 * What one step of a loop came to across every turn a row ran: the step, and the
 * nanoseconds charged to it.
 *
 * <p>A total rather than a per-turn cost, so the reader divides by whichever
 * turn count it is reporting over - and so the step's share of the row is
 * readable against the row's own total, which is a sum of the same spans.
 */
public final class PhaseTotal {

    private final ProfilePhase phase;
    private final long totalNanos;

    public PhaseTotal(ProfilePhase phase, long totalNanos) {
        this.phase = phase;
        this.totalNanos = totalNanos;
    }

    public ProfilePhase getPhase() {
        return phase;
    }

    public long getTotalNanos() {
        return totalNanos;
    }
}
