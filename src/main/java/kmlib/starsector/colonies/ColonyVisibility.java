package kmlib.starsector.colonies;

/**
 * The rule a colony set is shown under: the reveal that lifts the fog outright, and the two
 * gates that hold back the kinds of colony the fog alone would leak.
 *
 * <p>The fog itself - the player has found the market's entity - is not a knob and is not
 * carried here. What is carried is everything that <em>changes</em> that answer: a reveal that
 * admits what has not been found, and, for the two kinds a bare fog shows before the player
 * could plausibly have heard of them, a requirement that somebody have seen them where they
 * stand.
 *
 * <p>Passed as one value rather than as loose flags because all three are read together
 * wherever the rule is applied. A surface handed one gate and not the other would show a
 * derelict it had been told to hold back, and a signature taking three booleans in a row would
 * say nothing about it either way.
 *
 * <p>A gate narrows and never widens. Turning one off drops that kind back to the fog alone
 * rather than admitting anything the fog refuses, so no setting here can put a colony on the
 * map that the player has not found.
 *
 * @param shouldIncludeUndiscoveredMarkets whether an undiscovered colony still counts - the
 *                                         "show all factions" reveal, which admits everything
 *                                         and overrides both gates below
 * @param shouldGateAbandonedStations      whether an abandoned station must have been revealed
 *                                         as well as found. On, a derelict is shown only where
 *                                         the player has been or where somebody lives; off,
 *                                         finding it is enough
 * @param shouldGateHiddenColonies         whether a concealed colony must have been revealed as
 *                                         well as found. The same gate over the other leaking
 *                                         kind: a colony hiding itself on an entity that was
 *                                         never discoverable, which the fog admits outright
 */
public record ColonyVisibility(
    boolean shouldIncludeUndiscoveredMarkets,
    boolean shouldGateAbandonedStations,
    boolean shouldGateHiddenColonies) {

    /**
     * The fog alone: nothing admitted that has not been found, and neither kind held back
     * beyond it. What a caller stating no rule of its own is read as, since a gate nobody asked
     * for must not appear out of an unstated argument.
     */
    public static final ColonyVisibility BASE_FOG =
        new ColonyVisibility(false, false, false);
}
