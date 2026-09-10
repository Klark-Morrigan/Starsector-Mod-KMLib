package kmlib.mods.console;

/**
 * The bare read of a console's own state: is its overlay live right now? No gate, no failure
 * handling, no caching - just the one question, asked of whatever mod owns the console.
 *
 * <p>Separate from {@link ConsoleCommandsOverlay} so that the class naming a
 * {@code org.lazywizard.console} type is a class of its own, which the classloader resolves only
 * when the gate above it has already found the mod installed. A direct reference held anywhere on
 * that gate's own class would be resolved with it, and an install without Console Commands would
 * then take a missing-class error on a per-frame path.
 *
 * <p>Public because this is where the console read is stood in: a caller settling what its own
 * code does while a console is up supplies one of these to {@link ConsoleCommandsOverlay} rather
 * than replacing the gate, whose fail-open handling it inherits rather than restates.
 */
public interface ConsoleOverlayPresence {

    /**
     * @return whether this console's overlay is live right now
     */
    boolean isOverlayUp();
}
