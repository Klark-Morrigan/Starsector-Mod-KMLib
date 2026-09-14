package kmlib.settings;

import kmlib.starsector.settings.modmanager.ModPresence;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;

/**
 * Reads a mod's LunaLib settings values, null-safely, and relays change events.
 *
 * <p>Centralises the {@code lunalib.*} coupling the same way
 * {@link kmlib.logging.KmLogging} does for the log-level binding: a KM mod asks
 * here for a typed setting, or to be told when its settings change, and never
 * imports LunaLib itself, so the mod's own classpath stays free of LunaLib and
 * the dependency lives in one place.
 *
 * <p>LunaLib is a declared KMLib dependency, so these methods are safe to call
 * from any mod that depends on KMLib - it is present whenever KMLib is.
 *
 * <p>Thin passthrough to {@code LunaSettings}, which answers only in-engine
 * (like {@link kmlib.opengl.GlColour}'s GL passthrough): there is no logic here
 * beyond the fallbacks and the mod-id filtering.
 *
 * <p>Two fallbacks rather than one, because there are two ways a read can find
 * nothing. LunaLib answers null for a field it has no value for, which is the
 * ordinary case this has always covered - and it throws for a read taken before
 * the game has stood its settings up, its loader reaching the mod set on the
 * first read of any mod's settings. The second is not a null it could return,
 * so it is asked about beforehand: a read outside a running game answers the
 * caller's fallback rather than dying inside LunaLib. That reaches every caller
 * whose value is read on a path a test drives - a per-frame budget, a colour, a
 * cadence - and none of them is asking to be told the game is not up.
 */
public final class LunaSettingsReader {

    // LunaLib's own mod ID, asked of the game's mod set to establish that both it and the settings
    // behind it are up. Its own, rather than the calling mod's: what a read needs is the library
    // that answers it, and a mod may legitimately read a setting belonging to another.
    private static final String LUNALIB_MOD_ID = "lunalib";

    private LunaSettingsReader() {
    }

    /**
     * Reads a boolean LunaLib setting, falling back when it is unavailable.
     *
     * @param modId    the mod's LunaLib settings ID
     * @param fieldId  the boolean field's ID
     * @param fallback value returned when the setting is null (unset, or read
     *                 before LunaLib has loaded the mod's settings)
     * @return the stored boolean, or {@code fallback} when it is unavailable
     */
    public static boolean getBoolean(String modId, String fieldId, boolean fallback) {

        if (!isSettingsLibraryUp()) {
            return fallback;
        }
        var value = LunaSettings.getBoolean(modId, fieldId);

        return value != null ? value : fallback;
    }

    /**
     * Reads a double LunaLib setting, falling back when it is unavailable.
     *
     * @param modId    the mod's LunaLib settings ID
     * @param fieldId  the double field's ID
     * @param fallback value returned when the setting is null (unset, or read
     *                 before LunaLib has loaded the mod's settings)
     * @return the stored double, or {@code fallback} when it is unavailable
     */
    public static double getDouble(String modId, String fieldId, double fallback) {

        if (!isSettingsLibraryUp()) {
            return fallback;
        }
        var value = LunaSettings.getDouble(modId, fieldId);

        return value != null ? value : fallback;
    }

    /**
     * Reads an integer LunaLib setting, falling back when it is unavailable. Serves
     * the Int field type (an integer slider).
     *
     * @param modId    the mod's LunaLib settings ID
     * @param fieldId  the int field's ID
     * @param fallback value returned when the setting is null (unset, or read
     *                 before LunaLib has loaded the mod's settings)
     * @return the stored int, or {@code fallback} when it is unavailable
     */
    public static int getInt(String modId, String fieldId, int fallback) {

        if (!isSettingsLibraryUp()) {
            return fallback;
        }
        var value = LunaSettings.getInt(modId, fieldId);

        return value != null ? value : fallback;
    }

    /**
     * Reads a string LunaLib setting, falling back when it is unavailable. Serves
     * the Radio field type, whose stored value is the selected option's label.
     *
     * @param modId    the mod's LunaLib settings ID
     * @param fieldId  the string (or Radio) field's ID
     * @param fallback value returned when the setting is null (unset, or read
     *                 before LunaLib has loaded the mod's settings)
     * @return the stored string, or {@code fallback} when it is unavailable
     */
    public static String getString(String modId, String fieldId, String fallback) {

        if (!isSettingsLibraryUp()) {
            return fallback;
        }
        var value = LunaSettings.getString(modId, fieldId);

        return value != null ? value : fallback;
    }

    /**
     * Runs {@code onChange} whenever the player applies a change to
     * {@code modId}'s settings, so callers react to settings live instead of
     * polling. LunaLib notifies for every mod's change; this filters to the
     * one mod before invoking the callback.
     *
     * <p>Register once (e.g. at application load): every call adds another
     * listener.
     *
     * @param modId    the mod whose settings changes to listen for
     * @param onChange run on each change to that mod's settings
     */
    public static void runOnSettingsChange(String modId, Runnable onChange) {
        LunaSettings.addSettingsListener(new ChangeRelay(modId, onChange));
    }

    // Whether LunaLib can answer a read at all, which is the game's mod set being up and carrying
    // it. Asked before every read rather than caught after one: what LunaLib does outside a running
    // game is throw from inside its own loader, and a caught throwable there would be
    // indistinguishable from a real fault in it.
    //
    // Deliberately not asked of the listener registration below, which only puts a callback on a
    // list: refusing to register outside a running game would drop a subscription taken at
    // application load, before the settings it waits on exist.
    private static boolean isSettingsLibraryUp() {
        return ModPresence.isModEnabled(LUNALIB_MOD_ID);
    }

    // Relays LunaLib's change event to a plain Runnable, filtered to one mod so
    // a caller never sees other mods' settings changes.
    private static final class ChangeRelay implements LunaSettingsListener {
        private final String modId;
        private final Runnable onChange;

        private ChangeRelay(String modId, Runnable onChange) {
            this.modId = modId;
            this.onChange = onChange;
        }

        @Override
        public void settingsChanged(String changedModId) {
            if (modId.equals(changedModId)) {
                onChange.run();
            }
        }
    }
}
