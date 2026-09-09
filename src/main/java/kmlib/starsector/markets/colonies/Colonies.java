package kmlib.starsector.markets.colonies;

import java.util.List;

/**
 * A set of owned colonies, selected by one rule, one entry per place and owner.
 *
 * <p>The set exists because "what colonies are here" was being answered independently by every
 * reader that asked it - one walking the economy, one walking the entities, one doing both and
 * concatenating - so two surfaces drawn from the same place could disagree about who is present
 * in it. Selecting once and handing the result down means a disagreement is no longer
 * expressible.
 *
 * <p>Says nothing about where its colonies were found. {@link SystemColonies} and
 * {@link HyperspaceColonies} each select one, and a reader handed one already knows which it
 * asked - so carrying the place would only be a second copy of what the caller has.
 *
 * <p>Economy order is preserved ahead of the rest, because a caller mirroring vanilla's claim
 * mechanic settles a tied contest on whichever market the economy reaches first, and an order
 * imposed here would resolve a different winner.
 *
 * <p>The set is unfogged: it holds every colony present, found or not. That is deliberate,
 * because a mechanic mirrored from vanilla has to see what vanilla sees - claim scoring weighs
 * colonies the player has never found, and a fogged input would resolve a different claimant.
 *
 * <p>What may be <em>shown</em> of the set is nobody's answer here. Withholding a colony is a
 * judgement made for a purpose - a map paints under one rule and a dev listing under another -
 * and it needs facts the sector does not hold, such as what somebody has seen standing where.
 * A consumer states its own projection over this set and is handed {@link KnownColonyReader}
 * to state it through where a library reader has to spend one.
 *
 * @param colonies the colonies present, in the order they were selected
 */
public record Colonies(List<Colony> colonies) {

    /** A place with nobody in it, and the answer for one that cannot be read at all. */
    public static final Colonies NONE = new Colonies(List.of());

    /**
     * Takes an immutable copy of the colonies, and reads a null list as an empty one, so a set
     * handed around a render pass cannot change under its readers.
     */
    public Colonies {
        colonies = colonies == null ? List.of() : List.copyOf(colonies);
    }
}
