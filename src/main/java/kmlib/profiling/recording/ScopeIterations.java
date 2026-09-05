package kmlib.profiling.recording;

import kmlib.profiling.PhasedSection;
import kmlib.profiling.ProfilePhase;
import kmlib.profiling.snapshot.WorstCall;
import kmlib.text.KmlibStrings;

/**
 * What the loop of one open scope has run so far: how many turns ended, what
 * each declared step of them came to, and the slowest turn with the name its
 * caller gave it.
 *
 * <p>Held on the scope and folded into the node when the scope closes, the way
 * a scope's counts are, so what a reader is handed never includes a call's loop
 * before that call has finished.
 *
 * <p>A turn costs one clock read per step boundary and an add into a slot -
 * nothing allocated, nothing looked up - which is what makes measuring a
 * per-item loop affordable. The clock reading arrives from the scope rather
 * than being taken here, so every span of one call is read off the one clock the
 * profiler was built with.
 */
final class ScopeIterations {

    private final PhasedSection section;
    private final long[] phaseNanos;

    private long count;
    private long slowestNanos;
    private String slowestTag = WorstCall.NO_TAG;

    // The turn in hand: what it was named, when it began, and when the step
    // being timed began. Reset by each begin, so nothing of one turn reaches the
    // next.
    private boolean isIterationOpen;
    private String tag = WorstCall.NO_TAG;
    private long startNanos;
    private long lastMarkNanos;

    ScopeIterations(PhasedSection section) {
        this.section = section;
        this.phaseNanos = new long[section.getPhases().size()];
    }

    PhasedSection getSection() {
        return section;
    }

    /**
     * @return how many turns have ended, which is what the phase totals are
     *         spread over
     */
    long getCount() {
        return count;
    }

    /**
     * @return what each step came to, by the slot its phase holds
     */
    long[] getPhaseNanos() {
        return phaseNanos;
    }

    long getSlowestNanos() {
        return slowestNanos;
    }

    String getSlowestTag() {
        return slowestTag;
    }

    /**
     * Starts one turn.
     *
     * @param nowNanos the clock as the turn begins
     * @param tag      what the caller named it, blank where it named nothing
     */
    void beginIteration(long nowNanos, String tag) {

        // A blank name is no name, for the reason a call's tag normalises the
        // same way: a name composed out of what a caller happens to hold can
        // come out empty, and a slowest turn named a run of spaces reads as one
        // whose name was lost.
        this.tag = KmlibStrings.hasText(tag) ? tag : WorstCall.NO_TAG;
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

        // A mark outside a turn has no boundary behind it to measure from, and a
        // step of another section's loop numbers a slot of this one that means
        // something else. Both are caller bugs, and both are dropped rather than
        // charged: a phase total that silently holds somebody else's work is a
        // diagnostic that misleads about the very loop it was opened for.
        if (!isIterationOpen || phase.getSection() != section) {
            return;
        }
        phaseNanos[phase.getSlotIndex()] += nowNanos - lastMarkNanos;
        lastMarkNanos = nowNanos;
    }

    /**
     * Ends the turn in hand, keeping it where it is the slowest run here.
     *
     * @param nowNanos the clock as the turn ends
     */
    void endIteration(long nowNanos) {

        if (!isIterationOpen) {
            return;
        }
        isIterationOpen = false;

        var elapsedNanos = nowNanos - startNanos;

        // The first turn sets the bound rather than being compared against a
        // zero it cannot beat, so a loop whose turns all round to nothing still
        // names one of them.
        if (count == 0 || elapsedNanos > slowestNanos) {
            slowestNanos = elapsedNanos;
            slowestTag = tag;
        }
        count++;
    }
}
