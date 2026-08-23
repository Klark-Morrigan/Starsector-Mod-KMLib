package kmlib.starsector.colonies;

/**
 * A half of the fog a caller may ask to be dropped, so a colony the player could not have known
 * about is shown anyway.
 *
 * <p>Named rather than carried as a boolean apiece for the reason {@link RevelationGate} is: two
 * adjacent booleans are transposable without failing, and a rule built with the discovery reveal
 * and the survey reveal the wrong way round means something quite different, compiles, passes and
 * ships. A set of these cannot be got wrong that way, and a third reveal becomes one constant here
 * rather than a fourth flag every construction of a rule has to be corrected for.
 *
 * <p>Each reveal answers exactly one fog arm and no other. A colony held back by two of them needs
 * both, which is what keeps a player who turned one thing on from being shown a second thing they
 * never asked about - the failure they would have no way to diagnose.
 *
 * <p>A reveal widens where a gate narrows, and the two are stated apart for that reason. Neither
 * reaches the other: a reveal cannot admit a shape a gate is holding back, and a gate cannot
 * withhold what no fog arm covers.
 */
public enum VisibilityReveal {

    /**
     * A colony on an entity the player has not found is shown anyway - the "show all factions"
     * reveal, which drops the discovery arm of the fog for every kind of colony at once.
     */
    UNDISCOVERED_MARKETS,

    /**
     * A dead world nobody has surveyed closely enough to see is dead is shown anyway.
     *
     * <p>Its own reveal rather than a case of the one above, because the two axes are independent:
     * a planet the player has flown past is discovered whatever its survey level says, and that is
     * exactly the world whose ruins are still unread. Folded together, one knob would answer for
     * two facts and leave a player no way to ask for the one they meant.
     */
    UNSURVEYED_DEAD_WORLDS,
}
