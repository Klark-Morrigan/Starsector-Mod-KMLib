package kmlib.starsector.markets.colonies;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

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

    /**
     * Whether any colony here passes a test, stopped at the first that does.
     *
     * <p>Offered beside the selection rather than left to {@code selectColonies(...).isEmpty()},
     * because "is anybody here" is asked of every place in the sector on a scan and per frame while
     * a map is drawn, where the contents are never wanted - only whether there are any.
     *
     * @param isPassingColony the test to apply; null passes nothing, an unstated test being no
     *                        grounds for answering that somebody is here
     * @return true when at least one colony passes
     */
    public boolean hasAnyColony(Predicate<Colony> isPassingColony) {

        if (isPassingColony == null) {
            return false;
        }
        for (var colony : colonies) {

            if (isPassingColony.test(colony)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The colonies here that pass a test, in the set's own order.
     *
     * <p>The order is the set's rather than the test's, a caller mirroring vanilla's tie rules
     * reading it to settle a contest - so grouping the passing colonies any other way would resolve
     * a different winner.
     *
     * @param isPassingColony the test to apply; null selects nothing, for the reason above
     * @return the passing colonies; never null
     */
    public List<Colony> selectColonies(Predicate<Colony> isPassingColony) {

        if (isPassingColony == null) {
            return List.of();
        }
        var passingColonies = new ArrayList<Colony>();

        for (var colony : colonies) {

            if (isPassingColony.test(colony)) {
                passingColonies.add(colony);
            }
        }
        return List.copyOf(passingColonies);
    }
}
