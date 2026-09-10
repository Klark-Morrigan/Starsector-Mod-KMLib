package kmlib.mods.console.commands.targets;

/**
 * What came of looking for the thing a command was pointed at: the thing, or the reason there is
 * none, worded for the player.
 *
 * <p>Sealed over the two, so neither can be read without the other having been dealt with. A
 * single value carrying the thing and a message beside it would let a caller act on something
 * that was never found, or report nothing when nothing was.
 *
 * <p>Generic because a command resolves more than one kind of argument - the place it acts on,
 * the faction it acts for - and the shape of the answer is the same for all of them: found, or
 * refused with a reason. Stated once, a new kind of argument brings only its own resolver.
 *
 * @param <T> what a run of this resolution finds when it succeeds
 */
public sealed interface TargetResolution<T> permits ResolvedTarget, UnresolvedTarget {
}
