package kmlib.starsector.markets.colonies;

import java.util.List;

/**
 * Source port for which of a place's colonies the player may be told about.
 *
 * <p>A port rather than a rule stated here, because the answer is not a fact the sector holds. It
 * turns on what somebody has seen standing where, on which shapes of colony a caller means to hold
 * back, and on how much a caller means to reveal - all of them judgements made for a purpose, and
 * all of them the consumer's. A library reader that had to invent one would be answering a
 * question it was never asked.
 *
 * <p>Asked of a whole place at once rather than colony by colony. Whether one colony may be named
 * can turn on what else stands beside it - a hulk in orbit over an inhabited world is common
 * knowledge there - so a per-colony question would have every implementation fold the place again
 * for each of its colonies.
 *
 * <p>Spent on display and never on a mechanic. A claim mirrored from vanilla has to see what
 * vanilla sees, so what this admits decides only what an explanation of that claim may name.
 */
@FunctionalInterface
public interface KnownColonyReader {

    /**
     * Nothing may be named. What a reader given no port at all answers under, an absent rule
     * being no grounds for naming a colony the caller never said could be named.
     */
    KnownColonyReader NOTHING_KNOWN = colonies -> List.of();

    /**
     * Selects the colonies of one place that the player may be told about.
     *
     * @param colonies the place's colonies, as one walk of it reported; a null set holds nothing
     *                 to admit
     * @return the colonies that may be named, in the set's own order; never null
     */
    List<Colony> readKnownColonies(Colonies colonies);
}
