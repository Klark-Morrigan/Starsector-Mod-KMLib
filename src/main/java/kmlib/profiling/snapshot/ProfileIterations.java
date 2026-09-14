package kmlib.profiling.snapshot;

import java.util.List;

/**
 * What the loops of one row ran: how many turns they took in all, what each step
 * of a turn came to, and the slowest single turn with the name its caller gave
 * it.
 *
 * <p>Across every call of the row, the way its timing is: a bake is one call and
 * a cell is one turn, so the phase totals answer "what does a cell cost" while
 * the row's own timing answers "what does a bake cost". The two are different
 * questions and the row now carries both.
 *
 * <p>The slowest turn is kept because an average over thousands of turns cannot
 * say whether one item was pathological - which is the item worth looking at,
 * and the only one a name would help with.
 *
 * <p>Immutable, and holding raw nanoseconds (the clock's unit) so the reader
 * decides how to present them.
 */
public final class ProfileIterations {

    /**
     * The record of a row whose calls ran no loop at all, which is most of them.
     * Shared, since every such row says the same nothing.
     */
    public static final ProfileIterations NO_ITERATIONS =
        new ProfileIterations(0, List.of(), 0, WorstCall.NO_TAG);

    private final long count;
    private final List<PhaseTotal> phaseTotals;
    private final long slowestNanos;
    private final String slowestTag;

    /**
     * @param count        how many turns ended across every call of the row
     * @param phaseTotals  what each declared step came to, in declaration order
     * @param slowestNanos how long the slowest single turn took
     * @param slowestTag   what its caller named it, or {@link WorstCall#NO_TAG}
     *                     where it named nothing
     */
    public ProfileIterations(
            long count,
            List<PhaseTotal> phaseTotals,
            long slowestNanos,
            String slowestTag) {

        this.count = count;
        this.phaseTotals = List.copyOf(phaseTotals);
        this.slowestNanos = slowestNanos;
        this.slowestTag = slowestTag;
    }

    public long getCount() {
        return count;
    }

    /**
     * @return what each step of a turn came to across all of them, in the order
     *         the section declared its steps - which is the order a turn pays
     *         them
     */
    public List<PhaseTotal> getPhaseTotals() {
        return phaseTotals;
    }

    public long getSlowestNanos() {
        return slowestNanos;
    }

    /**
     * @return what the caller named the slowest turn, or {@link WorstCall#NO_TAG}
     *         where it named nothing
     */
    public String getSlowestTag() {
        return slowestTag;
    }

    /**
     * @return whether any turn ended here, which is what says a row has a loop
     *         to report on at all
     */
    public boolean hasAnyIterations() {
        return count > 0;
    }
}
