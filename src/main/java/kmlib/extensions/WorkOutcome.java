package kmlib.extensions;

/**
 * What an installed implementation did with the work it was offered: performed it, or handed it
 * back with the reason it could not.
 *
 * <p>Sealed over the two, so an implementation cannot hand work back without saying why. A bare
 * "no" is what makes a misbehaving install impossible to diagnose: the colony comes out the plainer
 * way, everything looks like it worked, and nothing anywhere names the body, the faction or the
 * missing mod that caused it. The reason has to be produced at the moment it is known, because the
 * moment afterwards nothing has it.
 *
 * <p>Stated here rather than left to each integration's own logging. A mod that logs on its own
 * side reports it under its own logger, in its own words, at whatever level it chose, and a mod
 * that forgets reports nothing - none of which can be relied on by the operation that has to
 * explain itself. A contract can be relied on: what comes back either carries a reason or is not a
 * decline.
 */
public sealed interface WorkOutcome permits ExecutedWork, DeclinedWork {

    /**
     * @return true where the implementation performed the work, so nothing else should do it
     */
    boolean wasExecuted();
}
