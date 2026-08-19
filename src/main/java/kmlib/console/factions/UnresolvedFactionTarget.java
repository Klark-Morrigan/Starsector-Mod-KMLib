package kmlib.console.factions;

/**
 * No faction to act for, and what to tell the player about why - the half of
 * {@link FactionTargetResolution} that found none.
 *
 * <p>Carries the reason as finished copy rather than as a code the caller words itself, for the
 * reason the market side gives: every command naming an owner refuses one for the same handful
 * of reasons, and a code would have each writing that wording again and drifting from the others.
 *
 * @param failureMessage the reason, phrased as a whole sentence for the console
 */
public record UnresolvedFactionTarget(
    String failureMessage) implements FactionTargetResolution {
}
