package kmlib.console.markets;

/**
 * No market to act on, and what to tell the player about why - the half of
 * {@link MarketTargetResolution} that found none.
 *
 * <p>Carries the reason as finished copy rather than as a code the caller words itself. Every
 * command refusing a target refuses it for the same handful of reasons, and a code would have
 * each writing that wording again and drifting from the others.
 *
 * @param failureMessage the reason, phrased as a whole sentence for the console
 */
public record UnresolvedMarketTarget(
    String failureMessage) implements MarketTargetResolution {
}
