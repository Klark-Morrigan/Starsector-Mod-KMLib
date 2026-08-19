package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.markets.Markets;

/**
 * What kind of place a colony is: somewhere people live, or a derelict nobody ever lived on.
 *
 * <p>The sector holds both under one market shape. An abandoned station is owned by the faction
 * whose station it was, is not condition-only, and is registered like any other market, so every
 * ownership read admits it as a colony - while nobody is aboard it, it supports nothing, and a
 * place holding only derelicts is empty space with hulks in it. A reader that cannot tell the two
 * apart says something false about the sector rather than merely drawing it oddly.
 *
 * <p>A kind rather than a boolean because the distinction is already known not to be binary: a
 * decivilised world is a third kind - somewhere people did live and no longer do - and it needs
 * its own answer rather than being folded into either of these. An enum admits it without any
 * reader changing shape, where a boolean would have to be replaced.
 *
 * <p>Resolved once, where a colony is selected, and carried on the colony from there. Every
 * reader downstream routes on it, and each re-deriving it from the market would be that many
 * independent statements of what an abandoned station is.
 */
public enum ColonyKind {

    /**
     * Somebody lives here. The ordinary case, and the only kind that makes its location
     * inhabited.
     */
    COLONY,

    /**
     * A derelict station with a storage submarket bolted on. Nobody has ever lived aboard it,
     * so it settles nothing and supports nothing - it is a thing present in a place, which the
     * player may or may not be entitled to be told about.
     */
    ABANDONED_STATION;

    /**
     * Reads which kind of place a market stands for.
     *
     * <p>Positive identification only: a market is an abandoned station because it says so
     * ({@link Markets#isAbandonedStation}), and everything else - including a market that reads
     * as nothing in particular, and a null one - is an ordinary colony.
     *
     * <p>That default is the safe direction rather than the tidy one. Misfiling a derelict as a
     * colony overstates a place by one hulk; misfiling a colony as a derelict erases a
     * settlement that is really there, taking its people with it.
     *
     * @param market the market to classify; null yields {@link #COLONY}
     * @return the kind of place the market stands for
     */
    public static ColonyKind resolveKind(MarketAPI market) {
        return Markets.isAbandonedStation(market) ? ABANDONED_STATION : COLONY;
    }
}
