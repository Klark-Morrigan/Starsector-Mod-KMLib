package kmlib.settings;

import com.fs.starfarer.api.Global;

import lunalib.backend.ui.settings.LunaSettingsLoader;
import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Logger;
import org.json.JSONException;

import java.io.IOException;

/**
 * Writes a value back into a mod's LunaLib settings and announces the change, so a control
 * drawn outside the settings screen (an in-map toggle, say) can drive the same stored value the
 * screen edits and both stay in sync.
 *
 * <p>The write sibling of {@link LunaSettingsReader}: LunaLib publishes readers but no setter,
 * so the write path reaches through {@code LunaSettingsLoader} to the mod's backing JSON store
 * and saves it, then fires {@link LunaSettings#reportSettingsChanged} so every listener (the
 * settings screen and the mod's own change-driven state) rebuilds. Keeping this here holds all
 * {@code lunalib.*} and LazyLib JSON coupling in KMLib, the same as the reader does.
 *
 * <p>Precondition: the calling mod must depend on LunaLib (these touch {@code lunalib.*} types)
 * and the store is only present once LunaLib has loaded the mod's settings - which happens when
 * the player opens the settings screen. Before then the store is absent and a write is a logged
 * no-op rather than an error.
 *
 * <p>Thin passthrough to the LunaLib/LazyLib write path, exercised in-engine rather than in
 * tests (like {@link LunaSettingsReader} and {@link kmlib.opengl.GlColor}): the only logic here
 * is the null-store guard and the write-failure handling.
 */
public final class LunaSettingsWriter {
    private static final Logger LOG = Global.getLogger(LunaSettingsWriter.class);

    private LunaSettingsWriter() {
    }

    /**
     * Writes a string setting - the stored form of a Radio field, whose value is the selected
     * option's label.
     *
     * @param modId   the mod's LunaLib settings id
     * @param fieldId the field's id
     * @param value   the string (or Radio label) to store
     */
    public static void putString(String modId, String fieldId, String value) {
        writeSetting(modId, fieldId, value);
    }

    /**
     * Writes a boolean setting.
     *
     * @param modId   the mod's LunaLib settings id
     * @param fieldId the field's id
     * @param value   the boolean to store
     */
    public static void putBoolean(String modId, String fieldId, boolean value) {
        writeSetting(modId, fieldId, value);
    }

    // Puts the value into the mod's backing JSON store and saves it, then reports the change so
    // listeners rebuild. The store is null until LunaLib has loaded the mod's settings (settings
    // screen never opened this session), in which case there is nothing to write to. A put or
    // save failure is logged, not propagated: a settings write must never take down the UI pass
    // that triggered it, and the change is simply not persisted. value is Object so one path
    // serves both String and boolean (autoboxed) writes.
    private static void writeSetting(String modId, String fieldId, Object value) {
        var store = LunaSettingsLoader.getSettings().get(modId);
        if (store == null) {
            LOG.warn("No LunaLib settings store for mod '" + modId + "'; skipping write of '"
                    + fieldId + "' (settings screen not opened yet this session)");
            return;
        }
        try {
            store.put(fieldId, value);
            store.save();
            LunaSettings.reportSettingsChanged(modId);
        } catch (JSONException | IOException exception) {
            LOG.error("Failed to write LunaLib setting '" + fieldId + "' for mod '" + modId + "'",
                    exception);
        }
    }
}
