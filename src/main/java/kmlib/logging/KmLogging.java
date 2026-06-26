package kmlib.logging;

import lunalib.lunaSettings.LunaSettings;
import lunalib.lunaSettings.LunaSettingsListener;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Binds a mod's log verbosity to its LunaLib setting, live.
 *
 * <p>A mod ships a LunaLib level-name dropdown (OFF/ERROR/WARN/INFO/DEBUG/
 * ALL) and calls {@link #bindToLunaSetting} once at load. After that the
 * player retunes the mod's logging from LunaLib's in-game settings screen
 * with no reload: the registered listener re-reads and re-applies the level
 * whenever that mod's settings change.
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
 * <p>Precondition: the calling mod must depend on LunaLib - these methods
 * touch {@code lunalib.*} types. KMLib itself declares no LunaLib dependency,
 * so a consumer without LunaLib simply must not call in here.
 */
public final class KmLogging {
    /**
     * Library-wide fallback verbosity, used when a caller does not specify one
     * and the LunaLib value is unavailable. WARN keeps warnings and errors
     * while dropping routine INFO and diagnostic DEBUG lines as normal-play
     * noise. Defined once here so no mod has to restate its default.
     */
    public static final Level DEFAULT_LEVEL = Level.WARN;

    private KmLogging() {
    }

    /**
     * Registers a live binding using the library default level
     * ({@link #DEFAULT_LEVEL}) as the fallback. Preferred entry point: a mod
     * states only its own id, package, and field, never a default level.
     *
     * @param modId      the mod's LunaLib settings id; also the filter that
     *                   restricts the binding to this mod's own changes
     * @param loggerRoot the mod's top package (e.g. {@code "kmu"})
     * @param fieldId    the LunaSettings field holding the level name
     */
    public static void bindToLunaSetting(String modId, String loggerRoot, String fieldId) {
        bindToLunaSetting(modId, loggerRoot, fieldId, DEFAULT_LEVEL);
    }

    /**
     * Registers a live binding from a LunaLib level-name field to the
     * {@code loggerRoot} logger subtree, and applies the current value once.
     * Takes an explicit fallback for the rare mod that wants a default other
     * than {@link #DEFAULT_LEVEL}.
     *
     * @param modId      the mod's LunaLib settings id; also the filter that
     *                   restricts the binding to this mod's own changes
     * @param loggerRoot the mod's top package (e.g. {@code "kmu"}); its logger
     *                   and, by inheritance, every logger beneath it take the
     *                   level, and nothing outside it does
     * @param fieldId    the LunaSettings field holding the level name
     * @param fallback   level used when the field is null, blank, or invalid
     */
    public static void bindToLunaSetting(String modId, String loggerRoot,
            String fieldId, Level fallback) {
        var binding = new LunaLogBinding(modId, loggerRoot, fieldId, fallback);
        LunaSettings.addSettingsListener(binding);
        binding.applyConfiguredLevel();
    }

    // Sets the resolved level on the loggerRoot subtree. Package-private so the
    // log4j inheritance behaviour can be pinned without the LunaLib boundary.
    static void applyLevel(String loggerRoot, String levelName, Level fallback) {
        Logger.getLogger(loggerRoot).setLevel(resolveLevel(levelName, fallback));
    }

    // Null-safe, whitespace-tolerant log4j level-name parse, so a value read
    // from a settings dropdown survives any padding around the stored entry.
    private static Level resolveLevel(String levelName, Level fallback) {
        if (levelName == null) {
            return fallback;
        }
        return Level.toLevel(levelName.trim(), fallback);
    }

    // Live binding: re-reads and re-applies the level whenever the player
    // changes this mod's LunaLib settings.
    static final class LunaLogBinding implements LunaSettingsListener {
        private final String modId;
        private final String loggerRoot;
        private final String fieldId;
        private final Level fallback;

        LunaLogBinding(String modId, String loggerRoot, String fieldId, Level fallback) {
            this.modId = modId;
            this.loggerRoot = loggerRoot;
            this.fieldId = fieldId;
            this.fallback = fallback;
        }

        @Override
        public void settingsChanged(String changedModId) {
            // LunaLib notifies every listener for every mod's change; retune
            // only when this mod's own settings changed.
            if (modId.equals(changedModId)) {
                applyConfiguredLevel();
            }
        }

        void applyConfiguredLevel() {
            applyLevel(loggerRoot, LunaSettings.getString(modId, fieldId), fallback);
        }
    }
}
