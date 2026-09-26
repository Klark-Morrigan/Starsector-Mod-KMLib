package kmlib.settings;

import kmlib.KmlibMod;
import kmlib.logging.KmLogging;
import kmlib.starsector.compatibility.CompatibilityConsumer;
import kmlib.starsector.compatibility.ModIntegration;
import kmlib.starsector.strings.KmlibStringKeys;

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

    /**
     * LunaLib's own mod ID, as the key a compatibility record about this binding latches under and
     * what its installed version is read by.
     *
     * <p>Here rather than under {@code kmlib.mods}, which is the home for the mods a consumer may
     * run without. LunaLib is a declared dependency: the library is compiled against it and cannot
     * run without it, so its identity belongs with the code that binds to it, which is this.
     */
    public static final String LUNALIB_MOD_ID = "lunalib";

    /** LunaLib as a report about a binding to it is headed. */
    public static final String LUNALIB_MOD_NAME = "LunaLib";

    private static final String LOGGER_ROOT = "kmlib";
    private static final String LOG_LEVEL_FIELD = "kmlib_logLevel";

    // Which of the library's features a failure of these bindings costs, as the half of a latch key
    // the mod ID does not cover.
    private static final String LUNALIB_SETTINGS_FEATURE_KEY = "lunalib-settings";

    private KmlibLunaSettings() {
    }

    /**
     * These bindings as the compatibility channel states them: LunaLib as the third party, and the
     * library as the mod that loses something by it.
     *
     * <p>Composed only once the install has failed, so the wording read out of strings.json and the
     * mod manager read behind the installed version stay off every load where it worked.
     *
     * @return the integration a failure to install these bindings is reported under
     */
    public static ModIntegration describeLunaLibIntegration() {

        return new ModIntegration(
            LUNALIB_MOD_ID,
            LUNALIB_MOD_NAME,
            new CompatibilityConsumer(
                KmlibMod.MOD_ID,
                LUNALIB_SETTINGS_FEATURE_KEY,
                KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_LOST_LUNALIB_SETTINGS),
                KmlibStringKeys.get(KmlibStringKeys.COMPATIBILITY_UNAFFECTED_LUNALIB_SETTINGS)));
    }

    /**
     * Registers KMLib's LunaLib bindings and applies their current values. Call once at application
     * load, by which point LunaLib - a declared dependency - has loaded.
     */
    public static void installBindings() {
        KmLogging.bindToLunaSetting(KmlibMod.MOD_ID, LOGGER_ROOT, LOG_LEVEL_FIELD);
    }
}
