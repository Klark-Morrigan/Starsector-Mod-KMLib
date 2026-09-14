package kmlib.settings;

import kmlib.logging.KmLogging;

/**
 * KMLib's own LunaLib settings: at present only the verbosity of the library's own output.
 *
 * <p>The library needs a switch of its own because log4j scopes a level to a package subtree, and
 * {@code kmlib} sits under no mod's package. A consuming mod turning its verbosity up therefore
 * cannot reach the library code it calls, and a mod binding {@code kmlib} to <em>its</em> dropdown
 * would retune the library for every other mod sharing the game - last writer wins. One library,
 * one level, set in one place is the only arrangement without that conflict.
 *
 * <p>What it costs is worth naming: a player wanting the library's diagnostics has to know the code
 * they are chasing lives in the library rather than in the mod they were using. That is why library
 * output written <em>for</em> one mod is handed back for that mod to log as its own instead - see
 * {@code MapTabWidgetTrace}. This switch is for the lines the library owns outright, chiefly the
 * reflection probes explaining why they could not read the game's UI.
 */
public final class KmlibLunaSettings {

    private static final String MOD_ID = "kmlib";
    private static final String LOGGER_ROOT = "kmlib";
    private static final String LOG_LEVEL_FIELD = "kmlib_logLevel";

    private KmlibLunaSettings() {
    }

    /**
     * Registers KMLib's LunaLib bindings and applies their current values. Call once at application
     * load, by which point LunaLib - a declared dependency - has loaded.
     */
    public static void installBindings() {
        KmLogging.bindToLunaSetting(MOD_ID, LOGGER_ROOT, LOG_LEVEL_FIELD);
    }
}
