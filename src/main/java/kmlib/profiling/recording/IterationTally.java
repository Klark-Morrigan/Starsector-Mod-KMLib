package kmlib.profiling.recording;

import kmlib.profiling.PhasedSection;
import kmlib.profiling.snapshot.PhaseTotal;
import kmlib.profiling.snapshot.ProfileIterations;
import kmlib.profiling.snapshot.WorstCall;

import java.util.ArrayList;

/**
 * What a loop has run so far: how many turns ended, what each declared step of
 * them came to, and the slowest turn with the name its caller gave it.
 *
 * <p>One tally for both places a loop is summed - the scope adding turns as they
 * end, and the node adding whole scopes as they close - so the rule that makes a
 * turn the slowest is stated once, and a scope's tally folds into its node's by
 * addition rather than by a second reading of the same fields.
 */
final class IterationTally {

    private final PhasedSection section;
    private final long[] phaseNanos;

    private long count;
    private long slowestNanos;
    private String slowestTag = WorstCall.NO_TAG;

    IterationTally(PhasedSection section) {
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
     * Charges part of a turn to one step.
     *
     * @param slotIndex which step, by the slot its phase holds
     * @param nanos     what to add to it
     */
    void addPhaseNanos(int slotIndex, long nanos) {
        phaseNanos[slotIndex] += nanos;
    }

    /**
     * Counts one ended turn, keeping it where it is the slowest so far.
     *
     * @param elapsedNanos how long the turn took
     * @param tag          what its caller named it, {@link WorstCall#NO_TAG}
     *                     where it named nothing
     */
    void addTurn(long elapsedNanos, String tag) {
        keepIfSlowest(elapsedNanos, tag);
        count++;
    }

    /**
     * Folds a whole tally in - a closed scope's loop into its row's.
     *
     * @param other the tally to add, on the same section as this one
     */
    void addTally(IterationTally other) {

        // A loop that ran no turn has no slowest to offer and no phase that
        // cost anything: folding it in would only move nothing.
        if (other.count == 0) {
            return;
        }
        for (var slot = 0; slot < phaseNanos.length; slot++) {
            phaseNanos[slot] += other.phaseNanos[slot];
        }
        keepIfSlowest(other.slowestNanos, other.slowestTag);
        count += other.count;
    }

    /**
     * @return these turns copied into the immutable form a snapshot is read
     *         from, or {@link ProfileIterations#NO_ITERATIONS} where none ended
     */
    ProfileIterations buildIterations() {

        if (count == 0) {
            return ProfileIterations.NO_ITERATIONS;
        }
        var phaseTotals = new ArrayList<PhaseTotal>(phaseNanos.length);

        for (var slot = 0; slot < phaseNanos.length; slot++) {
            phaseTotals.add(new PhaseTotal(section.getPhases().get(slot), phaseNanos[slot]));
        }
        return new ProfileIterations(count, phaseTotals, slowestNanos, slowestTag);
    }

    // The first turn sets the bound rather than being compared against a zero
    // it cannot beat, so a loop whose turns all round to nothing still names
    // one of them.
    private void keepIfSlowest(long elapsedNanos, String tag) {
        if (count == 0 || elapsedNanos > slowestNanos) {
            slowestNanos = elapsedNanos;
            slowestTag = tag;
        }
    }
}
