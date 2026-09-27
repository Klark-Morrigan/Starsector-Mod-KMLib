package kmlib.settings;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Binds a mod's log verbosity to its LunaLib setting, live, scoped to that mod and nothing else.
 *
 * <p>A mod ships a LunaLib level-name dropdown (OFF/ERROR/WARN/INFO/DEBUG/ALL) and calls
 * {@link #bindLogLevel} once at load. After that the player retunes the mod's logging from LunaLib's
 * in-game settings screen with no reload: the level is read and set again whenever that mod's
 * settings change.
 *
 * <p>What this hides is the non-obvious part. Starsector names every logger after its class's fully
 * qualified name ({@code Global.getLogger(c)} is {@code Logger.getLogger(c.getName())}), so setting
 * the level on the logger named after a mod's TOP PACKAGE governs every logger beneath it through
 * log4j inheritance - and nothing else: not the root logger, not the engine's {@code com.fs} loggers,
 * not other mods. A reader who does not know that rule would scope it by enumerating every class (as
 * LazyLib does); keeping it here means every KM mod scopes its logging the same correct way.
 *
 * <p>Scoped to the mod and no further. A level set here does not reach KMLib's own {@code kmlib}
 * loggers, which sit under no mod's package - deliberately, so that one mod's verbosity cannot retune
 * a shared library for every other mod in the same game. The library binds its own level like any
 * other mod, from its own setting; and library output written <em>for</em> one mod is handed back for
 * that mod to log as its own rather than logged here at all, so a player chasing their own mod's
 * behaviour does not have to know which library the code sits in. See {@link KmlibLunaSettings} and
 * {@code MapTabWidgetTrace} for the two halves of that.
 *
 * <p>Built on {@link LunaSettingsReader}'s read and change relay rather than on LunaLib directly,
 * so a read taken before the game is up answers the default level, and a change that fails to apply
 * is logged the way every other settings callback's is.
 */
public final class LunaLogLevelBinding {

    /**
     * Library-wide fallback verbosity, applied where a mod's field is unset or holds no level name.
     * WARN keeps warnings and errors while dropping routine INFO and diagnostic DEBUG lines as
     * normal-play noise. Defined once here so no mod has to restate its default.
     */
    public static final Level DEFAULT_LEVEL = Level.WARN;

    private LunaLogLevelBinding() {
    }

    /**
     * Registers a live binding from a LunaLib level-name field to the {@code loggerRoot} logger
     * subtree, and applies the current value once.
     *
     * @param modId      the mod's LunaLib settings ID; also the filter that restricts the binding to
     *                   this mod's own changes
     * @param loggerRoot the mod's top package (e.g. {@code "kmu"}); its logger and, by inheritance,
     *                   every logger beneath it take the level, and nothing outside it does
     * @param fieldId    the LunaSettings field holding the level name
     */
    public static void bindLogLevel(String modId, String loggerRoot, String fieldId) {

        Runnable applyConfiguredLevel = () -> applyLevel(
            loggerRoot,
            LunaSettingsReader.getString(modId, fieldId, null));

        LunaSettingsReader.runOnSettingsChange(modId, applyConfiguredLevel);
        applyConfiguredLevel.run();
    }

    private static void applyLevel(String loggerRoot, String levelName) {

        Logger
            .getLogger(loggerRoot)
            .setLevel(resolveLevel(levelName));
    }

    // Null-safe, whitespace-tolerant log4j level-name parse, so a value read from a settings dropdown
    // survives any padding around the stored entry.
    private static Level resolveLevel(String levelName) {

        if (levelName == null) {
            return DEFAULT_LEVEL;
        }
        return Level.toLevel(
            levelName.trim(),
            DEFAULT_LEVEL);
    }
}
