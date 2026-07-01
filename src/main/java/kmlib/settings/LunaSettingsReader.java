package kmlib.settings;

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
 * <p>Precondition: the calling mod must depend on LunaLib - these methods touch
 * {@code lunalib.*} types. KMLib declares no LunaLib mod dependency, so a
 * consumer without LunaLib simply must not call in here.
 *
 * <p>Thin passthrough to {@code LunaSettings}, exercised in-engine rather than
 * in tests (like {@link kmlib.opengl.GlColor}'s GL passthrough): there is no
 * logic here beyond the null fallback and the mod-id filtering.
 */
public final class LunaSettingsReader {

    private LunaSettingsReader() {
    }

    /**
     * Reads a boolean LunaLib setting, falling back when it is unavailable.
     *
     * @param modId    the mod's LunaLib settings id
     * @param fieldId  the boolean field's id
     * @param fallback value returned when the setting is null (unset, or read
     *                 before LunaLib has loaded the mod's settings)
     * @return the stored boolean, or {@code fallback} when it is unavailable
     */
    public static boolean getBoolean(String modId, String fieldId, boolean fallback) {
        var value = LunaSettings.getBoolean(modId, fieldId);
        return value != null ? value : fallback;
    }

    /**
     * Reads a double LunaLib setting, falling back when it is unavailable.
     *
     * @param modId    the mod's LunaLib settings id
     * @param fieldId  the double field's id
     * @param fallback value returned when the setting is null (unset, or read
     *                 before LunaLib has loaded the mod's settings)
     * @return the stored double, or {@code fallback} when it is unavailable
     */
    public static double getDouble(String modId, String fieldId, double fallback) {
        var value = LunaSettings.getDouble(modId, fieldId);
        return value != null ? value : fallback;
    }

    /**
     * Reads a string LunaLib setting, falling back when it is unavailable. Serves
     * the Radio field type, whose stored value is the selected option's label.
     *
     * @param modId    the mod's LunaLib settings id
     * @param fieldId  the string (or Radio) field's id
     * @param fallback value returned when the setting is null (unset, or read
     *                 before LunaLib has loaded the mod's settings)
     * @return the stored string, or {@code fallback} when it is unavailable
     */
    public static String getString(String modId, String fieldId, String fallback) {
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
