package kmlib.console.markets;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

/**
 * The market a command may act on - the half of {@link MarketTargetResolution} that found one.
 *
 * @param market the market the command was pointed at
 */
public record ResolvedMarketTarget(
    MarketAPI market) implements MarketTargetResolution {
}
