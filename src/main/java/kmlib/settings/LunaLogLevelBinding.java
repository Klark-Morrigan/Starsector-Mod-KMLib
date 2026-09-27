package kmlib.settings;

import kmlib.logging.KmLogging;

/**
 * Binds a mod's log verbosity to its LunaLib setting, live.
 *
 * <p>A mod ships a LunaLib level-name dropdown (OFF/ERROR/WARN/INFO/DEBUG/ALL) and calls
 * {@link #bindLogLevel} once at load. After that the player retunes the mod's logging from LunaLib's
 * in-game settings screen with no reload: the level is read and set again whenever that mod's
 * settings change. What setting a level does, and why it stops at the mod's own package, is
 * {@link KmLogging}'s.
 *
 * <p>Built on {@link LunaSettingsReader}'s read and change relay rather than on LunaLib directly,
 * so a read taken before the game is up answers the default level, and a change that fails to apply
 * is logged the way every other settings callback's is.
 */
public final class LunaLogLevelBinding {

    private LunaLogLevelBinding() {
    }

    /**
     * Registers a live binding from a LunaLib level-name field to the {@code loggerRoot} logger
     * subtree, and applies the current value once. A field that is unset or holds no level name
     * applies {@link KmLogging#DEFAULT_LEVEL}.
     *
     * @param modId      the mod's LunaLib settings ID; also the filter that restricts the binding to
     *                   this mod's own changes
     * @param loggerRoot the mod's top package (e.g. {@code "kmu"})
     * @param fieldId    the LunaSettings field holding the level name
     */
    public static void bindLogLevel(String modId, String loggerRoot, String fieldId) {

        Runnable applyConfiguredLevel = () -> KmLogging.applyLevel(
            loggerRoot,
            LunaSettingsReader.getString(modId, fieldId, null));

        LunaSettingsReader.runOnSettingsChange(modId, applyConfiguredLevel);
        applyConfiguredLevel.run();
    }
}
