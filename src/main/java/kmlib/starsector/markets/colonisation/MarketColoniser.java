package kmlib.starsector.markets.colonisation;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.ListenerUtil;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;
import com.fs.starfarer.api.impl.campaign.ids.Factions;
import com.fs.starfarer.api.impl.campaign.ids.Industries;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

import kmlib.starsector.markets.ownership.MarketOwnershipRule;

/**
 * Founding a colony on a body that so far carries only survey data.
 *
 * <p>Every uninhabited world already carries a market object - the placeholder its hazard,
 * atmosphere and resource conditions hang on - and colonisation is that placeholder becoming
 * a colony rather than a second market being built beside it. Which is why whether a place
 * can be colonised is a read of a market's state and not of a planet's.
 *
 * <p>Split in two along the one axis that separates the game's own two colonisation routines:
 * the survey panel's, which founds a colony for the player, and the one story rulecmd that
 * founds a colony for a faction. Read side by side those two agree on everything except who
 * ends up holding the place, so what they share is {@link #foundColony} and what they differ
 * over is {@link MarketOwnershipRule#applyOwnership}. Neither owner's sequence is written out in
 * full here, which is what keeps the two from drifting apart.
 *
 * <p>All of which is what founding a colony means where nothing else has an opinion about it. An
 * install running a mod with a colonisation of its own hands that mod the whole founding instead,
 * since a colony there has to be the shape the rest of that mod expects - see
 * {@link #establishColony}.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state, and
 * null-defensive like the rest of the library.
 */
public final class MarketColoniser {

    // What a colony starts at: three, the smallest size the game's own two colonisation
    // routines both found at, and the size the population_3 condition names. Stated once for both
    // paths - a routine an install supplies is founded at this size too rather than at one of its
    // own, so a colony is the same size however it came to be.
    private static final int BASELINE_COLONY_SIZE = 3;

    // What has been spent so far towards the queued spaceport, which on a colony founded a
    // moment ago is nothing - the item is queued rather than delivered.
    private static final int NO_CREDITS_SPENT_ON_CONSTRUCTION = 0;

    // Whether registering the colony also scatters orbital junk around the body and starts the
    // radio chatter its traffic is heard as. Both belong to a place that has just become
    // inhabited, which is why the survey panel asks for them. The story rulecmd declines them, but
    // it is dressing one particular world the story has already staged; a colony founded anywhere
    // else has nothing around it until this puts it there.
    private static final boolean WITH_ORBITAL_JUNK_AND_CHATTER = true;

    // Whatever colonisation this install supplies, offered every founding before the sequence below
    // is composed. Asked of this package's own register rather than by naming a mod here: which
    // mods are present is a fact about the install and is settled where the install is composed,
    // not by the operation that happens to have a colony to found.
    private static final ColonisationRoutine INSTALLED_COLONISATION_ROUTINES =
        ColonisationRoutines::offerColonisation;

    private MarketColoniser() {
        // utility class, no instances.
    }

    /**
     * Whether a colony can still be founded on the body this market stands for.
     *
     * <p>The first arm is the engine's own definition rather than one derived here: a market
     * flagged condition-only <em>is</em> survey data, and vanilla's own colonisation turns one
     * into a colony by clearing exactly that flag. A market without it is already a colony,
     * whoever holds it and however small.
     *
     * <p>The second arm is registration, and it is not implied by the first. Vanilla builds a
     * condition-only market unregistered - procgen hangs it on the planet, and decivilisation
     * hands one back after removing the colony from the economy - so a condition-only market
     * the economy does list is a shape nothing vanilla builds, and one that founding would
     * register a second time.
     *
     * @param market the market to test; null yields false
     * @return true when the market is survey data on a body no one has colonised
     */
    public static boolean isReadyForColonisation(MarketAPI market) {
        return market != null
            && market.isPlanetConditionMarketOnly()
            && !market.isInEconomy();
    }

    /**
     * Turns a body's survey data into a colony no one holds yet: settled, sized, attached to the
     * body and registered with the economy.
     *
     * <p>Owner-neutral by construction - nothing here names a faction, sets the player-owned
     * mark, opens a trading counter or charges a tariff. Those are what the game's two
     * colonisation routines disagree over, and they are {@link MarketOwnershipRule#applyOwnership}'s
     * to state once for both directions rather than this method's to state twice.
     *
     * <p>Everything the body carries is surveyed on arrival. The survey panel can leave that out
     * because the player cannot reach its button before surveying the place, and this cannot: a
     * caller founding a colony from outside that flow is free to found one on a body nobody has
     * looked at, and a colony whose own conditions read as unknown to its owner is not a shape
     * the game produces. The one routine that founds on an unsurveyed body does the same.
     *
     * <p>What it deliberately leaves out is the free-port mark. The survey panel clears it and
     * the story rulecmd does not, because clearing it is about undoing an arrangement a previous
     * owner made - and survey data has had no owner to make one.
     *
     * <p>A market that is not survey data is left exactly as it is rather than founded a second
     * time: registering a colony the economy already lists would enter it twice.
     *
     * @param sector the sector whose economy the colony is registered with; null, or a sector
     *               with no economy, leaves the market alone - an unregistered colony is worse
     *               than none, being a place the economy never feeds
     * @param market the survey data to found on; null, or a market that is already a colony,
     *               is left alone
     */
    public static void foundColony(SectorAPI sector, MarketAPI market) {

        if (!canFoundColony(sector, market)) {
            return;
        }

        markEveryConditionSurveyed(market);
        adoptBodyName(market);

        // The two marks that are colonisation itself: an age starting now, and the flag whose
        // clearing turns survey data into a colony. Every read that tells the two apart - this
        // class's own eligibility read included - keys on that flag rather than on the
        // population that follows it.
        market.setDaysInExistence(0f);
        market.setPlanetConditionMarketOnly(false);

        settleBaselinePopulation(market);
        addStorageSubmarket(market);
        bindMarketToBody(market);
        registerMarketWithEconomy(sector, market);
        queueFirstSpaceport(market);
    }

    /**
     * Founds a colony and hands it to an owner - the whole of what colonising a body means.
     *
     * <p>The owner lands before the economy hears about the place, which is the order both of
     * the game's colonisation routines take: a colony reaches the economy already flying its
     * flag, trading over its owner's counters and taxed at its owner's rate, so the economy
     * steps the game runs on registration are run against the colony as it will actually be.
     * Applying ownership afterwards would register an ownerless colony and then re-flag it, with
     * the trading counters arriving after the economy had already settled without them.
     *
     * <p>The colonisation report is the player's alone. The game's own listener call is named
     * for player colonisation and its listeners record it as the player's doing, so firing it
     * for a faction's colony would file that colony under the player's history. It reaches only
     * a colony founded on a planet, that being what the report carries - a colony founded on
     * anything else is founded silently rather than refused.
     *
     * <p>Where the install runs a mod with a colonisation of its own, that mod founds the colony
     * and none of the above happens: a colony on such an install has to be the shape that mod
     * builds, which nothing can arrange after the fact. What a caller is promised either way is a
     * colony founded under the owner it named, not the particular sequence that produced it.
     *
     * @param sector    the sector whose economy the colony is registered with; null, or a sector
     *                  with no economy, leaves the market alone
     * @param market    the survey data to found on; null, or a market that is already a colony,
     *                  is left alone
     * @param factionId the owner the colony is founded under, {@link Factions#PLAYER} for the
     *                  player; null leaves the market alone rather than founding an ownerless
     *                  colony
     */
    public static void establishColony(SectorAPI sector, MarketAPI market, String factionId) {
        establishColony(sector, market, factionId, INSTALLED_COLONISATION_ROUTINES);
    }

    // The same founding against a stated routine rather than the installed one, which is what lets
    // both branches be posed on a machine that has whichever mods it happens to have.
    static void establishColony(
            SectorAPI sector,
            MarketAPI market,
            String factionId,
            ColonisationRoutine colonisationRoutine) {

        // The founding conditions are asked before the owner is applied, not only inside the
        // founding that follows: an owner applied to a market that then cannot be founded would
        // leave survey data flying a flag and trading over counters, holding an ownership change
        // that no colony was ever built under. They are asked before the routine below is offered
        // the founding too, so a market this library would refuse is not one a mod is handed.
        if (factionId == null || !canFoundColony(sector, market)) {
            return;
        }

        // A mod that founds its own colonies takes the whole founding rather than having this
        // sequence run under it, so a colony on that install is the shape the rest of that mod
        // expects to find. A routine that declines leaves the market untouched for the sequence
        // below - which is the answer on every install without such a mod, and on a body its
        // routine cannot found on.
        var outcome =
            colonisationRoutine.establishColony(sector, market, factionId, BASELINE_COLONY_SIZE);

        if (outcome != null && outcome.wasExecuted()) {
            return;
        }

        MarketOwnershipRule.applyOwnership(market, factionId);

        foundColony(sector, market);

        if (Factions.PLAYER.equals(factionId)) {
            reportPlayerColonisation(market);
        }
    }

    // Whether there is a colony to found here at all: survey data to found on, and an economy to
    // register the result with. Stated once because both entry points need the same answer, and
    // an entry point that asked only half of it would mutate a market it could not finish.
    private static boolean canFoundColony(SectorAPI sector, MarketAPI market) {
        return sector != null
            && sector.getEconomy() != null
            && isReadyForColonisation(market);
    }

    // The people the colony starts with and the room they take up: the population every colony
    // runs as a condition and an industry alike, the ruins of a previous colony swapped for their
    // descendants where the body carries any, and the size that population amounts to.
    private static void settleBaselinePopulation(MarketAPI market) {

        market.addCondition(Conditions.POPULATION_3);
        market.addIndustry(Industries.POPULATION);

        replaceDecivilisationWithSubpopulation(market);

        market.setSize(BASELINE_COLONY_SIZE);
    }

    // The hold a colony keeps whoever owns it. Both of the game's colonisation routines add one -
    // the survey panel for the player, the story rulecmd for a faction - and the game's own market
    // setup lists it for every faction market alongside the trading counters, so a colony founded
    // without it is a place nothing can be stored at. Owner-neutral for that reason: what an owner
    // changes about it is only whether the player has already paid to use it, which is the
    // ownership rule's to say.
    private static void addStorageSubmarket(MarketAPI market) {

        if (!market.hasSubmarket(Submarkets.SUBMARKET_STORAGE)) {
            market.addSubmarket(Submarkets.SUBMARKET_STORAGE);
        }
    }

    // Every condition the body carries, marked as looked at, and the body itself as fully
    // surveyed. A condition survives colonisation - the hazard and the ore deposits are the
    // colony's own facts of life - so what is unsurveyed here would stay unreadable on a colony
    // its owner runs.
    private static void markEveryConditionSurveyed(MarketAPI market) {

        market.setSurveyLevel(MarketAPI.SurveyLevel.FULL);

        var conditions = market.getConditions();

        if (conditions == null) {
            return;
        }

        for (var condition : conditions) {

            if (condition != null) {
                condition.setSurveyed(true);
            }
        }
    }

    // The colony is named after the body it stands on. Survey data carries the body's own name
    // only by coincidence of how it was built, so the name is taken from the body rather than
    // left to whatever the placeholder happened to hold.
    private static void adoptBodyName(MarketAPI market) {

        var body = market.getPrimaryEntity();

        if (body == null || body.getName() == null) {
            return;
        }

        market.setName(body.getName());
    }

    // A world that was settled once and lost carries a condition saying so, and a colony founded
    // on the ruins carries the descendants of who was left instead. Leaving the original in
    // place would have the new colony reading as uninhabited ruins it is standing on.
    private static void replaceDecivilisationWithSubpopulation(MarketAPI market) {

        if (!market.hasCondition(Conditions.DECIVILIZED)) {
            return;
        }

        market.removeCondition(Conditions.DECIVILIZED);
        market.addCondition(Conditions.DECIVILIZED_SUBPOP);
    }

    // The colony and the body it stands on, each pointed at the other. Survey data is already
    // hung on its body, so this is stating a link rather than making one - and a market that
    // stands for a place with no body has none to state.
    private static void bindMarketToBody(MarketAPI market) {

        var body = market.getPrimaryEntity();

        if (body == null) {
            return;
        }

        body.setMarket(market);
        market.setPrimaryEntity(body);
    }

    // What turns a colony into a place the economy trades with. The three steps go together:
    // registering it, stepping the economy so the colony's own supply and demand exist rather
    // than arriving at the next monthly tick, and advancing the colony so it reads as settled
    // from the moment it is founded rather than mid-update.
    private static void registerMarketWithEconomy(SectorAPI sector, MarketAPI market) {

        var economy = sector.getEconomy();

        economy.addMarket(market, WITH_ORBITAL_JUNK_AND_CHATTER);
        economy.tripleStep();
        market.advance(0f);
    }

    // A colony's first building, queued rather than built. Both of the game's colonisation
    // routines leave the same one waiting, a colony with no spaceport being cut off from
    // everything that reaches it by ship.
    private static void queueFirstSpaceport(MarketAPI market) {

        var constructionQueue = market.getConstructionQueue();

        if (constructionQueue == null) {
            return;
        }

        constructionQueue.addToEnd(Industries.SPACEPORT, NO_CREDITS_SPENT_ON_CONSTRUCTION);
    }

    // Tells whatever is listening that the player has colonised a planet. Guarded on the body
    // being a planet because that is what the report carries - a colony founded on a station or
    // any other body has nothing to report with.
    private static void reportPlayerColonisation(MarketAPI market) {

        if (market.getPrimaryEntity() instanceof PlanetAPI planet) {
            ListenerUtil.reportPlayerColonizedPlanet(planet);
        }
    }
}
