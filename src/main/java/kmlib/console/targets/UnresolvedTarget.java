package kmlib.console.targets;

/**
 * Nothing to act on, and what to tell the player about why - the half of
 * {@link TargetResolution} that found none.
 *
 * <p>Carries the reason as finished copy rather than as a code the caller words itself. Every
 * command refusing a target refuses it for the same handful of reasons, and a code would have
 * each writing that wording again and drifting from the others.
 *
 * <p>Type parameter without a component that uses it, which is the one seam the generic pair
 * leaves showing: a refusal holds nothing of the kind that was looked for, but it has to be
 * usable where that kind was expected. It is inferred from the return type at every site that
 * builds one, so it costs a reader nothing.
 *
 * @param failureMessage the reason, phrased as a whole sentence for the console
 * @param <T>            what the search that failed was looking for
 */
public record UnresolvedTarget<T>(
    String failureMessage) implements TargetResolution<T> {
}
