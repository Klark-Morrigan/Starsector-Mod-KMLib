package kmlib.logging;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Sets a mod's log verbosity, scoped to that mod and nothing else.
 *
 * <p>What this hides is the non-obvious part. Starsector names every logger
 * after its class's fully qualified name ({@code Global.getLogger(c)} is
 * {@code Logger.getLogger(c.getName())}), so setting the level on the logger
 * named after a mod's TOP PACKAGE governs every logger beneath it through
 * log4j inheritance - and nothing else: not the root logger, not the engine's
 * {@code com.fs} loggers, not other mods. A reader who does not know that rule
 * would scope it by enumerating every class (as LazyLib does); keeping it here
 * means every KM mod scopes its logging the same correct way.
 *
 * <p>Scoped to the mod and no further. A level set here does not reach KMLib's
 * own {@code kmlib} loggers, which sit under no mod's package - deliberately, so
 * that one mod's verbosity cannot retune a shared library for every other mod in
 * the same game. The library sets its own level like any other mod, from its own
 * setting; and library output written <em>for</em> one mod is handed back for
 * that mod to log as its own rather than logged here at all, so a player chasing
 * their own mod's behaviour does not have to know which library the code sits in.
 * See {@code KmlibLunaSettings} and {@code MapTabWidgetTrace} for the two halves
 * of that.
 *
 * <p>Which level to set is the caller's: {@code LunaLogLevelBinding} reads it
 * from a mod's LunaLib dropdown and sets it again on every change.
 */
public final class KmLogging {
    /**
     * Library-wide fallback verbosity, used where no level name is given or the
     * one given is not a level. WARN keeps warnings and errors while dropping
     * routine INFO and diagnostic DEBUG lines as normal-play noise. Defined once
     * here so no mod has to restate its default.
     */
    public static final Level DEFAULT_LEVEL = Level.WARN;

    private KmLogging() {
    }

    /**
     * Sets the named level on the {@code loggerRoot} subtree.
     *
     * @param loggerRoot the mod's top package (e.g. {@code "kmu"}); its logger
     *                   and, by inheritance, every logger beneath it take the
     *                   level, and nothing outside it does
     * @param levelName  a log4j level name, surrounding whitespace allowed;
     *                   {@link #DEFAULT_LEVEL} where it is null or not a level
     */
    public static void applyLevel(String loggerRoot, String levelName) {

        Logger
            .getLogger(loggerRoot)
            .setLevel(resolveLevel(levelName));
    }

    // Null-safe, whitespace-tolerant log4j level-name parse, so a value read
    // from a settings dropdown survives any padding around the stored entry.
    private static Level resolveLevel(String levelName) {

        if (levelName == null) {
            return DEFAULT_LEVEL;
        }
        return Level.toLevel(
            levelName.trim(),
            DEFAULT_LEVEL);
    }
}
