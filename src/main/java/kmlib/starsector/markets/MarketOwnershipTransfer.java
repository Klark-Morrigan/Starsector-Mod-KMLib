package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

/**
 * Moving a colony that already exists from the owner holding it to another.
 *
 * <p>The other half of how a faction comes to hold a place, beside {@link MarketColoniser}:
 * one makes a colony where there was none, this one changes whose it is. Kept apart because
 * the two share no step - founding has no previous owner to detach - and folding them into
 * one place would leave every caller having to say which of the two it meant.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state, and
 * null-defensive like the rest of the library.
 */
public final class MarketOwnershipTransfer {

    private MarketOwnershipTransfer() {
        // utility class, no instances.
    }

    /**
     * Whether this market is a colony whose owner there is something to change.
     *
     * <p>The counterpart of {@link MarketColoniser#isReadyForColonisation}, and in practice
     * its complement: a body still carrying only survey data has no owner to move, while a
     * place a faction holds does. The rule is {@link Markets#isOwnedColony}'s, named here as
     * the transfer's own requirement so a caller asks by what it needs of a market rather
     * than by which read happens to state it.
     *
     * <p>Registration with the economy is deliberately not an arm. Vanilla builds Galatia
     * Academy as a real colony under a real faction that the economy never lists, and moving
     * that one to another owner is as much a transfer as moving any other.
     *
     * @param market the market to test; null yields false
     * @return true when a faction holds the market as a colony
     */
    public static boolean isReadyForTransfer(MarketAPI market) {
        return Markets.isOwnedColony(market);
    }
}
