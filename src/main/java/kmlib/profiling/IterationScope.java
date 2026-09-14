package kmlib.profiling;

/**
 * An open section that is running a loop: one scope over the whole loop, which
 * counts the turns it takes and what each step of a turn cost, rather than a
 * scope per turn.
 *
 * <p>A scope per turn is what a per-item loop cannot afford - the pair of clock
 * reads and the row lookup would come to about what a cell's own work costs -
 * and a loop measured as a single span cannot say which of its steps grew. This
 * says both: the row keeps the whole loop's span, and beneath it the per-turn
 * cost of each step and the turn that took longest.
 *
 * <p>Everything a turn adds is an add into a slot resolved when the section
 * declared its phases, so what measuring costs is one clock read per step
 * boundary. That is affordable per cell; it is not affordable per vertex.
 */
public interface IterationScope extends ProfileScope {

    /**
     * Starts one turn of the loop, naming it so the slowest turn can say which
     * one it was - the cell it was baking, the system it was walking.
     *
     * <p>Named at the start rather than at the end, unlike a call's own tag,
     * because a turn is over the item it was handed and knows it from the
     * outset. A blank name leaves the turn unnamed, for the reason
     * {@link #tagCall} does.
     *
     * @param tag what to call this one turn where it turns out to be the slowest
     */
    void beginIteration(String tag);

    /**
     * Charges to {@code phase} the time since the turn began, or since the
     * previous step of it was marked - so a caller marks a step as it finishes
     * it.
     *
     * <p>Every moment of a turn between two marks is charged to the step marked
     * second, the gaps between the steps included: a turn's steps then account
     * for the turn rather than for as much of it as somebody remembered to
     * bracket. What follows the last mark of a turn is charged to no step.
     *
     * <p>A step of some other section's loop is ignored rather than charged to
     * whatever slot it happens to number, since a diagnostic that reports the
     * wrong step is worse than one that reports none.
     *
     * @param phase the step of this section's loop that has just finished
     */
    void markPhase(ProfilePhase phase);

    /**
     * Ends one turn of the loop, counting it and keeping it where it is the
     * slowest turn this scope has run.
     */
    void endIteration();
}
