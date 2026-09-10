package kmlib.mods.console.commands.targets;

/**
 * What a command may act on - the half of {@link TargetResolution} that found something.
 *
 * @param target the thing the command was pointed at
 * @param <T>    what kind of thing that is
 */
public record ResolvedTarget<T>(
    T target) implements TargetResolution<T> {
}
