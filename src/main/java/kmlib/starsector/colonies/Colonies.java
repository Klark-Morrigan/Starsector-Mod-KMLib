package kmlib.starsector.colonies;

import kmlib.starsector.markets.MarketVisibility;

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
 * <p>The set is unfogged: it holds every owned colony present, found or not. That is
 * deliberate, because a mechanic mirrored from vanilla has to see what vanilla sees - claim
 * scoring weighs colonies the player has never found, and a fogged input would resolve a
 * different claimant. What the player may be shown is {@link #readKnownColonies}, and what
 * amounts to people living here is {@link #readInhabitingColonies} - named projections over the
 * set rather than filters each display reader applies for itself.
 *
 * <p>The sighting register travels with the set rather than being handed to each projection,
 * because it is a fact about the world the set was read out of - as the colonies themselves are -
 * and every projection asks it the same question. A register given per read is one a later reader
 * can state differently, which would have two surfaces drawn from one place disagreeing about
 * where the player has been.
 *
 * @param colonies  the colonies present, in the order they were selected
 * @param sightings what the player has seen of them and where; an unstated register reads as
 *                  {@link ColonySightings#NONE}
 */
public record Colonies(
    List<Colony> colonies,
    ColonySightings sightings) {

    /** A place with nobody in it, and the answer for one that cannot be read at all. */
    public static final Colonies NONE = new Colonies(List.of());

    // The kind test of a projection that turns nothing away for what it is - the known reading,
    // where the visibility rule is the whole of the filter.
    private static final Predicate<Colony> EVERY_KIND = colony -> true;

    /**
     * Takes an immutable copy of the colonies, and reads a null list as an empty one, so a set
     * handed around a render pass cannot change under its readers. An absent register reads as
     * nothing seen, which withholds a gated colony rather than leaking one.
     */
    public Colonies {
        colonies = colonies == null ? List.of() : List.copyOf(colonies);
        sightings = sightings == null ? ColonySightings.NONE : sightings;
    }

    /**
     * A set nothing is known to have been seen in - what a caller holding no register of the
     * player's travels reads, and what every place answers before one is opened.
     *
     * @param colonies the colonies present; null reads as an empty set
     */
    public Colonies(List<Colony> colonies) {
        this(colonies, ColonySightings.NONE);
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
        return readColoniesPassing(rule, EVERY_KIND);
    }

    /**
     * Whether anyone the player knows of is here - the emptiness of
     * {@link #readKnownColonies} asked without materialising it.
     *
     * <p>Offered because "does anyone live in this system" is asked of every system in the
     * sector on a scan, and per cell while the map is drawn, where the projection's contents
     * are never wanted - only whether it has any.
     *
     * <p>Stops at the first colony that passes, the place's settled reading having been taken
     * for the whole set beforehand - so the answer cannot differ from the listing's own
     * emptiness, whichever way a gate falls.
     *
     * @param rule what the player may be shown of the set; null reads as
     *             {@link ColonyVisibility#BASE_FOG}
     * @return true when at least one colony passes the projection
     */
    public boolean hasKnownColony(ColonyVisibility rule) {
        return hasColonyPassing(rule, EVERY_KIND);
    }

    /**
     * The colonies that amount to people living here - the known projection minus the derelicts
     * nobody was ever aboard, and the one every reader answering a question about habitation is
     * meant to take.
     *
     * <p>A second named projection rather than a flag on the first, because what may be
     * <em>said</em> about a place and what constitutes <em>habitation</em> of it are different
     * questions with different answers. A derelict is named in a listing once somebody has seen
     * it and settles nothing whatever, so a reader handed the wrong one of these makes a claim
     * about the sector rather than a formatting mistake.
     *
     * <p>Resolved by the same walk under the same rule, differing from the known listing in its
     * kind test alone, so the two cannot disagree about what has been found or revealed. A
     * derelict a settled place reveals is therefore admitted to the listing only, and goes on
     * settling nothing - which is what keeps one derelict from vouching for another.
     *
     * @param rule what the player may be shown of the set; null reads as
     *             {@link ColonyVisibility#BASE_FOG}
     * @return the known colonies somebody lives on, in the set's own order
     */
    public List<Colony> readInhabitingColonies(ColonyVisibility rule) {
        return readColoniesPassing(rule, Colonies::isInhabitingColony);
    }

    /**
     * Whether anybody the player knows of lives here - the emptiness of
     * {@link #readInhabitingColonies} asked without materialising it.
     *
     * <p>Offered for the same reason its known counterpart is: the cell that paints a place and
     * the scan that decides whether to draw it at all ask only whether the projection has any,
     * per system and per frame.
     *
     * <p>Stops at the first colony that passes, on the same terms its known counterpart does -
     * one walk, the settled reading taken for the whole set beforehand, and a derelict passed
     * over however plainly the player can see it.
     *
     * @param rule what the player may be shown of the set; null reads as
     *             {@link ColonyVisibility#BASE_FOG}
     * @return true when at least one colony passes the habitation projection
     */
    public boolean hasInhabitingColony(ColonyVisibility rule) {
        return hasColonyPassing(rule, Colonies::isInhabitingColony);
    }

    // Materialises one projection: the place's settled reading taken once for the whole set,
    // then every colony the rule admits whose kind this projection wants.
    //
    // The two projections are one walk with two kind tests rather than a walk each, so the only
    // thing that can differ between them is the kind - a second statement of the visibility rule
    // is what would let the listing and habitation disagree about what has been found.
    //
    // Walked in the set's own order rather than gated colonies after ungated ones, since a
    // caller mirroring vanilla's tie rules reads that order and would resolve differently.
    private List<Colony> readColoniesPassing(
            ColonyVisibility rule,
            Predicate<Colony> isWantedColony) {

        var resolvedRule = resolveRule(rule);
        var isSettledPlace = hasSettlingColony(resolvedRule);
        var passingColonies = new ArrayList<Colony>();

        for (var colony : colonies) {

            if (isWantedColony.test(colony)
                    && isKnownColony(colony, resolvedRule, isSettledPlace)) {
                passingColonies.add(colony);
            }
        }
        return List.copyOf(passingColonies);
    }

    // The emptiness of one projection, asked without materialising it - the same settled reading
    // and the same per-colony rule as the listing, stopped at the first colony that passes.
    //
    // Its own walk rather than the listing's isEmpty, because the cell that paints a place and
    // the scan that decides whether to draw it at all ask this per system and per frame, and
    // never want the contents.
    private boolean hasColonyPassing(
            ColonyVisibility rule,
            Predicate<Colony> isWantedColony) {

        var resolvedRule = resolveRule(rule);
        var isSettledPlace = hasSettlingColony(resolvedRule);

        for (var colony : colonies) {

            if (isWantedColony.test(colony)
                    && isKnownColony(colony, resolvedRule, isSettledPlace)) {
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
    private boolean hasSettlingColony(ColonyVisibility rule) {

        for (var colony : colonies) {

            if (isSettlingColony(colony, rule)) {
                return true;
            }
        }
        return false;
    }

    // A colony that makes its place settled: somewhere people are, held in the open, and shown
    // by the fog. Its kind and its openness are exactly what keeps it out of every gate, which
    // is why the pass over these can be read before any gate is decided.
    //
    // A dead colony would not qualify, having nobody left to talk; a derelict never had anybody;
    // and a concealed colony is not the sector's town crier. Which kinds have somebody to talk is
    // the kind's own answer rather than a comparison written here, so a kind added later is not
    // left out of it by omission.
    private static boolean isSettlingColony(Colony colony, ColonyVisibility rule) {

        return colony.kind().isSettlingLocation()
            && !colony.isHidden()
            && isAdmittedByFog(colony, rule);
    }

    // The projection's rule for one colony, stated once so the read that materialises the
    // projection and the emptiness question about it cannot answer under different filters -
    // which is the very drift naming the projection here exists to prevent.
    //
    // Written over the kind, the two gates and the place's settled reading rather than as a
    // branch per surface, so a fourth kind or a third gate has one place to be added.
    private boolean isKnownColony(
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

    // Whether a colony amounts to people living where it stands - the one thing that separates
    // the habitation projection from the known one.
    //
    // Stated as the exclusion of the kind nobody was ever aboard rather than as an admission of
    // the ordinary one, so a kind added later inhabits its place unless it says otherwise.
    // Overstating a place by one hulk is the cheaper mistake; erasing a settlement that is really
    // there takes its people with it.
    private static boolean isInhabitingColony(Colony colony) {
        return colony.kind() != ColonyKind.SPACE_DERELICT;
    }

    // The base fog, plus the ownership arm the composed filter carries with it.
    //
    // Ownership is re-asked although the set is already selected on it: the composition is what
    // the rule is, and unpicking it here to save the second read would leave a narrower statement
    // of "counts as a known colony" living in this class.
    private static boolean isAdmittedByFog(Colony colony, ColonyVisibility rule) {

        return MarketVisibility.isCountedAsColony(
            colony.market(),
            rule.shouldIncludeUndiscoveredMarkets());
    }

    // Whether any gate the rule carries is about this colony. Asked of the gates themselves
    // rather than branched on here, so what a gate covers is stated where the gate is named and
    // a third one needs no edit in this class.
    private static boolean isGatedOnRevelation(Colony colony, ColonyVisibility rule) {

        for (var gate : rule.revelationGates()) {

            if (gate.coversColony(colony)) {
                return true;
            }
        }
        return false;
    }

    // Somebody has seen this colony where it now stands, and word of it has reached the player.
    //
    // A disjunction because the two routes are two ways one piece of word travels rather than
    // two separate requirements: demanding both would put every derelict in the Core behind a
    // visit the place's own population makes unnecessary.
    //
    // The two are tightened separately for the same reason they are stated separately: the
    // settled route asks who else could have seen this and reads the place as it stands, while
    // the sighting route asks whether the player did and is the only one of the two needing a
    // memory to answer with.
    private boolean isRevealedToPlayer(Colony colony, boolean isSettledPlace) {
        return isSettledPlace || colony.isSightedByPlayer(sightings);
    }

    // An unstated rule is the fog alone. Absent settings are not a reason to hold anything back
    // beyond it, and they are certainly not a reason to reveal what has not been found.
    private static ColonyVisibility resolveRule(ColonyVisibility rule) {
        return rule == null ? ColonyVisibility.BASE_FOG : rule;
    }

}
