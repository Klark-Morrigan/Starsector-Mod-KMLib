package kmlib.profiling.recording;

import kmlib.profiling.PhasedSection;
import kmlib.profiling.ProfilePhase;
import kmlib.profiling.snapshot.WorstCall;

/**
 * The loop of one open scope: the turn in hand, and the tally the turns end
 * into.
 *
 * <p>Held on the scope and folded into the node when the scope closes, the way
 * a scope's counts are, so what a reader is handed never includes a call's loop
 * before that call has finished.
 *
 * <p>A turn costs one clock read per step boundary and an add into a slot -
 * nothing allocated, nothing looked up. The reading arrives from the scope
 * rather than being taken here, so every span of one call is read off the one
 * clock the profiler was built with.
 */
final class ScopeIterations {

    private final IterationTally tally;

    // The turn in hand: what it was named, when it began, and when the step
    // being timed began. Reset by each begin, so nothing of one turn reaches the
    // next.
    private boolean isIterationOpen;
    private String tag = WorstCall.NO_TAG;
    private long startNanos;
    private long lastMarkNanos;

    ScopeIterations(PhasedSection section) {
        this.tally = new IterationTally(section);
    }

    IterationTally getTally() {
        return tally;
    }

    /**
     * Starts one turn.
     *
     * @param nowNanos the clock as the turn begins
     * @param tag      what the caller named it, blank where it named nothing
     */
    void beginIteration(long nowNanos, String tag) {
        this.tag = WorstCall.normaliseTag(tag);
        startNanos = nowNanos;
        lastMarkNanos = nowNanos;
        isIterationOpen = true;
    }

    /**
     * Charges the time since the turn began, or since its previous step was
     * marked, to {@code phase}.
     *
     * @param nowNanos the clock as the step ends
     * @param phase    the step that has just finished
     */
    void markPhase(long nowNanos, ProfilePhase phase) {

        // A mark outside a turn has no boundary to measure from, and a step of
        // another section's loop numbers a slot that means something else here.
        // Both are dropped rather than charged: a phase total quietly holding
        // somebody else's work misleads about the very loop it was opened for.
        if (!isIterationOpen || phase.getSection() != tally.getSection()) {
            return;
        }
        tally.addPhaseNanos(phase.getSlotIndex(), nowNanos - lastMarkNanos);
        lastMarkNanos = nowNanos;
    }

    /**
     * Ends the turn in hand.
     *
     * @param nowNanos the clock as the turn ends
     */
    void endIteration(long nowNanos) {

        if (!isIterationOpen) {
            return;
        }
        isIterationOpen = false;
        tally.addTurn(nowNanos - startNanos, tag);
    }
}
