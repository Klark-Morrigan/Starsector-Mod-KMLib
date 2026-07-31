package kmlib.settings;

import com.fs.starfarer.api.Global;

import lunalib.backend.ui.settings.LunaSettingsLoader;
import lunalib.lunaSettings.LunaSettings;
import org.apache.log4j.Logger;
import org.json.JSONException;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
 * <p>Two write paths, since persistence and liveness have different costs. A LunaLib store is an
 * in-memory JSON object the readers already read from, backed by a file that {@code save()}
 * rewrites whole. Putting a value updates the object the readers see immediately; the file write
 * only matters across restarts. So a control interacted with rapidly (a slider, or a burst of
 * toggles) writes through the deferred path, which puts and announces the change on the spot -
 * live value, live redraw - and leaves the store marked for a later {@link #flush} that collapses
 * a session's edits into one disk write. The immediate path puts and saves in one call, for a
 * write that must be durable the moment it is made.
 *
 * <p>Precondition: the calling mod must depend on LunaLib (these touch {@code lunalib.*} types)
 * and the store is only present once LunaLib has loaded the mod's settings - which happens the
 * first time any setting is read. Before then the store is absent and a write is a logged no-op
 * rather than an error.
 */
public final class LunaSettingsWriter {
    private static final Logger LOG = Global.getLogger(LunaSettingsWriter.class);

    // Mod ids whose in-memory store carries an unsaved deferred write, awaiting a flush to disk.
    // Thread-safe because KMLib is shared and makes no promise about the caller's thread, though in
    // practice writes and the flush both ride the UI thread. A mod id lingers here only until its
    // next flush; a failed save keeps it so a later flush retries.
    private static final Set<String> modsWithUnsavedWrites = ConcurrentHashMap.newKeySet();

    private LunaSettingsWriter() {
    }

    /**
     * Writes a string setting and saves it to disk at once - the stored form of a Radio field,
     * whose value is the selected option's label.
     *
     * @param modId   the mod's LunaLib settings id
     * @param fieldId the field's id
     * @param value   the string (or Radio label) to store
     */
    public static void putString(String modId, String fieldId, String value) {
        writeSetting(modId, fieldId, value, true);
    }

    /**
     * Writes a boolean setting and saves it to disk at once.
     *
     * @param modId   the mod's LunaLib settings id
     * @param fieldId the field's id
     * @param value   the boolean to store
     */
    public static void putBoolean(String modId, String fieldId, boolean value) {
        writeSetting(modId, fieldId, value, true);
    }

    /**
     * Writes a string setting live but defers the disk write to the next {@link #flush}. Use for a
     * control interacted with rapidly, so the value and any redraw update on the spot while the
     * file write batches. The stored value survives in memory (readers see it at once); it reaches
     * disk only when the mod is flushed.
     *
     * @param modId   the mod's LunaLib settings id
     * @param fieldId the field's id
     * @param value   the string (or Radio label) to store
     */
    public static void putStringDeferred(String modId, String fieldId, String value) {
        writeSetting(modId, fieldId, value, false);
    }

    /**
     * Writes a boolean setting live but defers the disk write to the next {@link #flush}, the
     * boolean counterpart of {@link #putStringDeferred}.
     *
     * @param modId   the mod's LunaLib settings id
     * @param fieldId the field's id
     * @param value   the boolean to store
     */
    public static void putBooleanDeferred(String modId, String fieldId, boolean value) {
        writeSetting(modId, fieldId, value, false);
    }

    /**
     * Saves a mod's store to disk if it carries deferred writes, then clears its pending mark; a
     * no-op when nothing is pending. Call at a natural settling point for the deferred path (say,
     * when the player leaves the screen the controls live on) so a session's edits land in one
     * disk write.
     *
     * @param modId the mod whose deferred writes to persist
     */
    public static void flush(String modId) {
        if (!modsWithUnsavedWrites.contains(modId)) {
            return;
        }
        var store = LunaSettingsLoader.getSettings().get(modId);
        if (store == null) {
            // The store vanished (settings never loaded, or reloaded away) - there is nothing to
            // save, so drop the mark rather than retry a write that can never land.
            modsWithUnsavedWrites.remove(modId);
            return;
        }
        try {
            store.save();
            modsWithUnsavedWrites.remove(modId);
        } catch (JSONException | IOException exception) {
            // Leave the mark set so a later flush retries; the value is already live in memory, so
            // only cross-restart persistence is at stake here.
            LOG.error("Failed to flush LunaLib settings for mod '" + modId + "'", exception);
        }
    }

    /**
     * Flushes every mod with deferred writes pending, the mod-agnostic form of {@link #flush}.
     */
    public static void flush() {
        for (var modId : modsWithUnsavedWrites) {
            flush(modId);
        }
    }

    // Puts the value into the mod's backing JSON store, then either saves it now or marks the store
    // for a later flush, and reports the change so listeners rebuild. The put alone makes the value
    // live - readers read the same store - so the deferred path is correct in-session and only
    // postpones the disk write. The store is null until LunaLib has loaded the mod's settings (no
    // setting read yet this session), in which case there is nothing to write to. A put or save
    // failure is logged, not propagated: a settings write must never take down the UI pass that
    // triggered it, and the change is simply not persisted. value is Object so one path serves both
    // String and boolean (autoboxed) writes.
    private static void writeSetting(String modId, String fieldId, Object value, boolean shouldSaveNow) {
        var store = LunaSettingsLoader.getSettings().get(modId);
        if (store == null) {
            LOG.warn("No LunaLib settings store for mod '"
                + modId
                + "'; skipping write of '"
                + fieldId
                + "' (settings screen not opened yet this session)");
            return;
        }
        try {
            store.put(fieldId, value);
            if (shouldSaveNow) {
                store.save();
                modsWithUnsavedWrites.remove(modId);
            } else {
                modsWithUnsavedWrites.add(modId);
            }
            LunaSettings.reportSettingsChanged(modId);
        } catch (JSONException | IOException exception) {
            LOG.error("Failed to write LunaLib setting '"
                + fieldId
                + "' for mod '"
                + modId
                + "'",
                exception);
        }
    }
}
