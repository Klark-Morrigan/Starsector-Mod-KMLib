package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

/**
 * Founding a colony on a body that so far carries only survey data.
 *
 * <p>Every uninhabited world already carries a market object - the placeholder its hazard,
 * atmosphere and resource conditions hang on - and colonisation is that placeholder becoming
 * a colony rather than a second market being built beside it. Which is why whether a place
 * can be colonised is a read of a market's state and not of a planet's.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state, and
 * null-defensive like the rest of the library.
 */
public final class MarketColoniser {

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
}
