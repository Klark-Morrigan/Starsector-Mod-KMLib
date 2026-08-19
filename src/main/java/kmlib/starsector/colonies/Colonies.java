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
     * The colonies the player may be shown - the known projection of the set, and the one every
     * display reader is meant to take rather than filtering the whole set itself.
     *
     * <p>Named once here so "what counts on the map" cannot drift between the cell that paints
     * a system, the ribbon that counts in it and the box that names its factions. The rule is a
     * parameter rather than a settings read, so the projection stays free of any one mod's
     * knobs and a pass resolves its rule once.
     *
     * <p>Knowledge is the composition of two facts, not a choice between them: the fog must
     * admit the colony, and the kinds a bare fog would leak must additionally have been
     * revealed. So a widened gate can never show what has not been found.
     *
     * <p>Resolved in two passes, because revelation has a route that runs through the place
     * itself. The first settles what the ungated colonies show; the second judges the gated ones
     * against it. That is an ordering rather than a cycle - an ordinary open colony is never
     * gated, so what settles a place is settled without consulting anything a gate decided.
     *
     * @param rule what the player may be shown of the set; null reads as
     *             {@link ColonyVisibility#BASE_FOG}
     * @return the colonies the rule admits, in the set's own order
     */
    public List<Colony> readKnownColonies(ColonyVisibility rule) {

        var resolvedRule = resolveRule(rule);
        var isSettledPlace = isSettledPlace(resolvedRule);
        var knownColonies = new ArrayList<Colony>();

        // Walked in the set's own order rather than gated colonies after ungated ones, since a
        // caller mirroring vanilla's tie rules reads that order and would resolve differently.
        for (var colony : colonies) {

            if (isKnownColony(colony, resolvedRule, isSettledPlace)) {
                knownColonies.add(colony);
            }
        }
        return List.copyOf(knownColonies);
    }

    /**
     * Whether anyone the player knows of is here - the emptiness of
     * {@link #readKnownColonies} asked without materialising it.
     *
     * <p>Offered because "does anyone live in this system" is asked of every system in the
     * sector on a scan, and per cell while the map is drawn, where the projection's contents
     * are never wanted - only whether it has any.
     *
     * <p>Short-circuits on the first colony that settles the place rather than on the first
     * that passes at all. A settling colony is known on its own account and is also what would
     * reveal anything gated, so no second pass could change a yes it has already given; a set
     * holding none is resolved in full, with the place unsettled.
     *
     * @param rule what the player may be shown of the set; null reads as
     *             {@link ColonyVisibility#BASE_FOG}
     * @return true when at least one colony passes the projection
     */
    public boolean hasKnownColony(ColonyVisibility rule) {

        var resolvedRule = resolveRule(rule);

        for (var colony : colonies) {

            if (isSettlingColony(colony, resolvedRule)) {
                return true;
            }
        }
        // Nothing settles the place, so nothing gated can be revealed by it: the rest resolves
        // exactly as the projection does, which is what keeps the two reads in agreement.
        for (var colony : colonies) {

            if (isKnownColony(colony, resolvedRule, false)) {
                return true;
            }
        }
        return false;
    }

    // The first pass: whether the ungated colonies the fog admits amount to somewhere people
    // live, and so to somewhere with inhabitants who would have seen whatever else is here.
    //
    // Read off what the projection shows rather than off what is present, because word reaches
    // the player through colonies the player knows are inhabited. A place whose only ordinary
    // colony is itself undiscovered has no grapevine the player is party to, and letting it
    // reveal anything would have the map act on a fact the player has no means of holding.
    private boolean isSettledPlace(ColonyVisibility rule) {

        for (var colony : colonies) {

            if (isSettlingColony(colony, rule)) {
                return true;
            }
        }
        return false;
    }

    // A colony that makes its place settled: somewhere people live, held in the open, and shown
    // by the fog. Its kind and its openness are exactly what keeps it out of every gate, which
    // is why the pass over these can be read before any gate is decided.
    //
    // A dead colony would not qualify, having nobody left to talk; an abandoned station never
    // had anybody; and a concealed colony is not the sector's town crier.
    private static boolean isSettlingColony(Colony colony, ColonyVisibility rule) {

        return colony.kind() == ColonyKind.COLONY
            && !colony.isHidden()
            && isAdmittedByFog(colony, rule);
    }

    // The projection's rule for one colony, stated once so the read that materialises the
    // projection and the emptiness question about it cannot answer under different filters -
    // which is the very drift naming the projection here exists to prevent.
    //
    // Written over the kind, the two gates and the place's settled reading rather than as a
    // branch per surface, so a fourth kind or a third gate has one place to be added.
    private static boolean isKnownColony(
            Colony colony,
            ColonyVisibility rule,
            boolean isSettledPlace) {

        if (!isAdmittedByFog(colony, rule)) {
            return false;
        }
        // The reveal overrides the gates outright. A listing that showed everything the fog
        // hides and then withheld a derelict would answer half the question it was asked.
        return rule.shouldIncludeUndiscoveredMarkets()
            || !isGatedOnRevelation(colony, rule)
            || isRevealedToPlayer(colony, isSettledPlace);
    }

    // The base fog, plus the ownership arm the composed filter carries with it.
    //
    // Ownership is re-asked although the set is already selected on it: the composition is what
    // the rule is, and unpicking it here to save a test would leave a second, narrower statement
    // of "counts as a known colony" living in this class.
    private static boolean isAdmittedByFog(Colony colony, ColonyVisibility rule) {

        return MarketVisibility.isCountedAsColony(
            colony.market(),
            rule.shouldIncludeUndiscoveredMarkets());
    }

    // The two kinds of colony a bare fog leaks, each behind its own gate. A derelict is admitted
    // by the fog the moment its entity is found - and most modded ones are never discoverable at
    // all - while a colony hiding itself is admitted on that same technicality.
    private static boolean isGatedOnRevelation(Colony colony, ColonyVisibility rule) {

        return (rule.shouldGateAbandonedStations()
                && colony.kind() == ColonyKind.ABANDONED_STATION)
            || (rule.shouldGateHiddenColonies() && colony.isHidden());
    }

    // Somebody has seen this colony where it now stands, and word of it has reached the player.
    //
    // A disjunction because the two routes are two ways one piece of word travels rather than
    // two separate requirements: demanding both would put every derelict in the Core behind a
    // visit the place's own population makes unnecessary.
    private static boolean isRevealedToPlayer(Colony colony, boolean isSettledPlace) {
        return isSettledPlace || colony.isSightedByPlayer();
    }

    // An unstated rule is the fog alone. Absent settings are not a reason to hold anything back
    // beyond it, and they are certainly not a reason to reveal what has not been found.
    private static ColonyVisibility resolveRule(ColonyVisibility rule) {
        return rule == null ? ColonyVisibility.BASE_FOG : rule;
    }

}
