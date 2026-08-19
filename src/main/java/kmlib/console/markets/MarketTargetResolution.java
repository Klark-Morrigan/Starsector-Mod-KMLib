package kmlib.console.markets;

/**
 * What came of looking for the market a command should act on: the market, or the reason there
 * is none, worded for the player.
 *
 * <p>Sealed over the two, so neither can be read without the other having been dealt with. A
 * single value carrying a market and a message beside it would let a caller act on a market
 * that was never found, or report nothing when nothing was.
 */
public sealed interface MarketTargetResolution
    permits ResolvedMarketTarget, UnresolvedMarketTarget {
}
