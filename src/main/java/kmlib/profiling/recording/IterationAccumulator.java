package kmlib.profiling.recording;

import kmlib.profiling.PhasedSection;
import kmlib.profiling.snapshot.PhaseTotal;
import kmlib.profiling.snapshot.ProfileIterations;
import kmlib.profiling.snapshot.WorstCall;

import java.util.ArrayList;

/**
 * What the loops of one node have run across every call of it: the turns, each
 * step's total, and the slowest turn any of those calls saw.
 *
 * <p>Its own accumulator beside the span and the worst call, since a loop is a
 * third thing said about a row and is not derived from either: a row's calls and
 * a row's turns are different denominators, and mixing them would price a bake
 * as though it were a cell.
 *
 * <p>The steps arrive from the section the loop was opened on, so the totals
 * are self-describing by the time a reader is handed them - no view has to go
 * back to a section to find out what a slot means.
 */
final class IterationAccumulator {

    private PhasedSection section;
    private long[] phaseNanos;
    private long count;
    private long slowestNanos;
    private String slowestTag = WorstCall.NO_TAG;

    /**
     * Folds one ended call's loop in.
     *
     * @param callIterations what that call's turns came to
     */
    void addIterations(ScopeIterations callIterations) {

        // A call that opened over a loop and ran no turn of it says nothing
        // about steps, and folding it in would raise a row of zeroed phases on a
        // section whose loop simply had nothing to iterate over.
        if (callIterations.getCount() == 0) {
            return;
        }
        if (section == null) {
            section = callIterations.getSection();
            phaseNanos = new long[section.getPhases().size()];
        }
        for (var slot = 0; slot < phaseNanos.length; slot++) {
            phaseNanos[slot] += callIterations.getPhaseNanos()[slot];
        }
        if (count == 0 || callIterations.getSlowestNanos() > slowestNanos) {
            slowestNanos = callIterations.getSlowestNanos();
            slowestTag = callIterations.getSlowestTag();
        }
        count += callIterations.getCount();
    }

    /**
     * @return these turns copied into the immutable form a snapshot is read
     *         from, or {@link ProfileIterations#NO_ITERATIONS} where no call of
     *         this node ran a loop
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
}
