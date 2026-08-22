package kmlib.starsector.colonies;

/**
 * Where each colony was last observed standing, asked by the colony's own id.
 *
 * <p>Says nothing about who did the observing. The player standing in a place and the place's own
 * inhabitants are both observations, and one register holds them alike - which is what keeps a
 * colony known once the neighbours who could see it are gone.
 *
 * <p>Vanilla records only that a system has been entered, which answers a different question: a
 * colony founded after the visit, or moved in since, was never seen there however many times the
 * player has crossed the place. So what is kept is the place a colony was seen standing in, and a
 * reader compares that against where it stands now.
 *
 * <p>No clock is involved, and that is deliberate. The fact is "this colony was seen here", not
 * "this place was visited on such a day", so there is no recency window to fall out of and no
 * state that expires. A colony that appears after the player has gone is unseen on that day and
 * every day after it, until the player returns - rather than shown for the rest of the day and
 * then taken away again, which is the one behaviour a visibility rule must never have.
 *
 * <p>Stated as a port rather than as a value because what answers it is save state a mod keeps,
 * while the rule read over it is not. A caller holding no register of its own reads {@link #NONE},
 * under which nothing has ever been seen - the conservative answer, since a register that
 * invented sightings would show colonies nobody has met.
 */
@FunctionalInterface
public interface ColonySightings {

    /**
     * Nothing has ever been seen anywhere. What an unstated register reads as, since an absent
     * record of what was observed is not a reason to suppose anything was.
     */
    ColonySightings NONE = colonyId -> null;

    /**
     * Where this colony was last seen standing.
     *
     * @param colonyId the colony's market id, as {@code MarketAPI#getId} reports it; an id the
     *                 register has never held reads as never seen
     * @return the id of the location the colony was last observed in, or null when nobody has
     *         seen it anywhere
     */
    String readSightedLocationId(String colonyId);
}
