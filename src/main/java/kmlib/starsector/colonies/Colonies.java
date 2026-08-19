package kmlib.starsector.colonies;

import kmlib.starsector.markets.MarketVisibility;

import java.util.ArrayList;
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
 * <p>The set is unfogged: it holds every owned colony present, found or not. That is
 * deliberate, because a mechanic mirrored from vanilla has to see what vanilla sees - claim
 * scoring weighs colonies the player has never found, and a fogged input would resolve a
 * different claimant. What the player may be shown is {@link #readKnownColonies}, one named
 * projection over the set rather than a filter each display reader applies for itself.
 */
public record Colonies(
    List<Colony> colonies) {

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
     * The colonies the player may be shown - the fogged projection of the set, and the one
     * every display reader is meant to take rather than filtering the whole set itself.
     *
     * <p>Named once here so "what counts on the map" cannot drift between the cell that paints
     * a system, the ribbon that counts in it and the box that names its factions. The dev
     * reveal is a parameter rather than a settings read, so the projection stays free of any
     * one mod's knobs and a pass resolves its reveal once.
     *
     * @param shouldIncludeUndiscoveredMarkets whether an undiscovered colony still counts (the
     *                                         "show all factions" dev reveal); false applies
     *                                         the normal known-to-player filter
     * @return the colonies passing {@link MarketVisibility#isCountedAsColony}, in the set's own
     *         order
     */
    public List<Colony> readKnownColonies(boolean shouldIncludeUndiscoveredMarkets) {

        var knownColonies = new ArrayList<Colony>();

        for (var colony : colonies) {

            if (isKnownColony(colony, shouldIncludeUndiscoveredMarkets)) {
                knownColonies.add(colony);
            }
        }
        return List.copyOf(knownColonies);
    }

    /**
     * Whether anyone the player knows of lives here - the emptiness of
     * {@link #readKnownColonies} asked without materialising it.
     *
     * <p>Offered because "does anyone live in this system" is asked of every system in the
     * sector on a scan, and per cell while the map is drawn, where the projection's contents
     * are never wanted - only whether it has any. Short-circuiting on the first colony that
     * passes is what keeps that ask the cost of a test rather than of a list.
     *
     * @param shouldIncludeUndiscoveredMarkets whether an undiscovered colony still counts (the
     *                                         "show all factions" dev reveal); false applies the
     *                                         normal known-to-player filter
     * @return true when at least one colony passes the projection
     */
    public boolean hasKnownColony(boolean shouldIncludeUndiscoveredMarkets) {

        for (var colony : colonies) {

            if (isKnownColony(colony, shouldIncludeUndiscoveredMarkets)) {
                return true;
            }
        }
        return false;
    }

    // The projection's rule for one colony, stated once so the read that materialises the
    // projection and the emptiness question about it cannot answer under different filters -
    // which is the very drift naming the projection here exists to prevent.
    //
    // Ownership is re-asked through the composed filter although the set is already selected on
    // it: the composition is what the rule is, and unpicking it here to save a test would leave a
    // second, narrower statement of "counts as a known colony" living in this class.
    private static boolean isKnownColony(
            Colony colony,
            boolean shouldIncludeUndiscoveredMarkets) {

        return MarketVisibility.isCountedAsColony(
            colony.market(),
            shouldIncludeUndiscoveredMarkets);
    }

}
