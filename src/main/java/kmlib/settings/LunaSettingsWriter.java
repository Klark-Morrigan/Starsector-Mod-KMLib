package kmlib.settings;

import org.apache.log4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Writes a value back into a mod's LunaLib settings and announces the change, so a control drawn
 * outside the settings screen (an in-map toggle, say) can drive the same stored value the screen
 * edits and both stay in sync.
 *
 * <p>The write sibling of {@link LunaSettingsReader}: LunaLib publishes readers but no setter, so
 * the write path reaches through a {@link LunaSettingsStore} to the mod's backing JSON store and
 * saves it, then has the store announce the change so every listener (the settings screen and the
 * mod's own change-driven state) rebuilds. Keeping this here holds all {@code lunalib.*} and
 * LazyLib JSON coupling in KMLib, the same as the reader does.
 *
 * <p>Two write paths, since persistence and liveness have different costs. A LunaLib store is an
 * in-memory JSON object the readers already read from, backed by a file that saving rewrites whole.
 * Putting a value updates the object the readers see immediately; the file write only matters
 * across restarts. So a control interacted with rapidly (a slider, or a burst of toggles) writes
 * through the deferred path, which puts and announces the change on the spot - live value, live
 * redraw - and leaves the store marked for a later {@link #flushPendingWrites} that collapses a
 * session's edits into one disk write. The immediate path puts and saves in one call, for a write
 * that must be durable the moment it is made.
 *
 * <p>Shedding a key is the third path, and it is a write in the same sense the others are - the
 * store is edited and the file rewritten. It is here rather than beside the reads because LunaLib
 * offers no removal at all: the loader seeds defaults and never prunes, so a field the shipped
 * table stopped declaring keeps its stored value forever unless something takes it out.
 *
 * <p>Which mods carry an unsaved deferred write is this writer's own state, so a flush is asked of
 * the writer that made the writes rather than of a tally shared across the game. The set is
 * thread-safe because KMLib is shared and makes no promise about the caller's thread, though in
 * practice writes and the flush both ride the UI thread.
 *
 * <p>Precondition: the store is only present once LunaLib has loaded the mod's settings - which
 * happens the first time any setting is read. Before then a write is a logged no-op rather than an
 * error.
 */
public final class LunaSettingsWriter {

    private static final Logger LOG = Logger.getLogger(LunaSettingsWriter.class);

    // Mod IDs whose in-memory store carries an unsaved deferred write, awaiting a flush to disk. A
    // mod ID lingers here only until its next flush; a failed save keeps it so a later flush
    // retries.
    private final Set<String> modsWithUnsavedWrites = ConcurrentHashMap.newKeySet();

    private final LunaSettingsStore store;

    /**
     * @param store where the settings being written are kept
     */
    public LunaSettingsWriter(LunaSettingsStore store) {
        this.store = store;
    }

    /**
     * Writes a string setting and saves it to disk at once - the stored form of a Radio field,
     * whose value is the selected option's label.
     *
     * @param modId   the mod's LunaLib settings ID
     * @param fieldId the field's ID
     * @param value   the string (or Radio label) to store
     */
    public void putString(String modId, String fieldId, String value) {
        writeSetting(modId, fieldId, value, true);
    }

    /**
     * Writes a boolean setting and saves it to disk at once.
     *
     * @param modId   the mod's LunaLib settings ID
     * @param fieldId the field's ID
     * @param value   the boolean to store
     */
    public void putBoolean(String modId, String fieldId, boolean value) {
        writeSetting(modId, fieldId, value, true);
    }

    /**
     * Writes a string setting live but defers the disk write to the next
     * {@link #flushPendingWrites}. Use for a control interacted with rapidly, so the value and any
     * redraw update on the spot while the file write batches. The stored value survives in memory
     * (readers see it at once); it reaches disk only when the mod is flushed.
     *
     * @param modId   the mod's LunaLib settings ID
     * @param fieldId the field's ID
     * @param value   the string (or Radio label) to store
     */
    public void putStringDeferred(String modId, String fieldId, String value) {
        writeSetting(modId, fieldId, value, false);
    }

    /**
     * Writes a boolean setting live but defers the disk write to the next
     * {@link #flushPendingWrites}, the boolean counterpart of {@link #putStringDeferred}.
     *
     * @param modId   the mod's LunaLib settings ID
     * @param fieldId the field's ID
     * @param value   the boolean to store
     */
    public void putBooleanDeferred(String modId, String fieldId, boolean value) {
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
    public void flushPendingWrites(String modId) {

        if (!modsWithUnsavedWrites.contains(modId)) {
            return;
        }
        if (!store.hasStoreFor(modId)) {
            // The store vanished (settings never loaded, or reloaded away) - there is nothing to
            // save, so drop the mark rather than retry a write that can never land.
            modsWithUnsavedWrites.remove(modId);
            return;
        }
        if (store.saveStore(modId)) {
            modsWithUnsavedWrites.remove(modId);
        }
        // A refused save leaves the mark set so a later flush retries; the value is already live in
        // memory, so only cross-restart persistence is at stake here.
    }

    /**
     * Flushes every mod with deferred writes pending, the mod-agnostic form of
     * {@link #flushPendingWrites}.
     */
    public void flushPendingWrites() {
        for (var modId : modsWithUnsavedWrites) {
            flushPendingWrites(modId);
        }
    }

    /**
     * Drops a retired field's stored value and saves at once, so a key whose CSV row and reader
     * are both gone stops travelling in the player's settings file.
     *
     * <p>LunaLib only ever adds: on load it seeds a default for each row the shipped table
     * declares and never prunes a key the table stopped declaring. A retired field's value
     * therefore outlives the feature it belonged to, and would be handed straight back to any
     * later field that reused the id. Shedding it is the settings-side counterpart of unsetting a
     * dead save key.
     *
     * <p>No listener is told. A retired field is by definition one nothing reads, so announcing
     * its removal would only make every consumer rebuild over a value that changed for nobody.
     *
     * <p>A no-op where the key is absent, which is a fresh install and every load after the sweep
     * has run once - so the common case pays no disk write.
     *
     * @param modId   the mod's LunaLib settings ID
     * @param fieldId the retired field's ID
     */
    public void removeSetting(String modId, String fieldId) {

        if (!store.removeValue(modId, fieldId)) {
            return;
        }
        if (store.saveStore(modId)) {
            // The save wrote the whole store, deferred edits included, so any pending mark it was
            // carrying is now settled - left set, the next flush would rewrite an unchanged file.
            modsWithUnsavedWrites.remove(modId);
        }
    }

    // Puts the value into the mod's backing JSON store, then either saves it now or marks the store
    // for a later flush, and reports the change so listeners rebuild. The put alone makes the value
    // live - readers read the same store - so the deferred path is correct in-session and only
    // postpones the disk write. The store is absent until LunaLib has loaded the mod's settings (no
    // setting read yet this session), in which case there is nothing to write to. A refused put or
    // save is said rather than raised: a settings write must never take down the UI pass that
    // triggered it, and the change is simply not persisted. value is Object so one path serves both
    // String and boolean (autoboxed) writes.
    private void writeSetting(String modId, String fieldId, Object value, boolean shouldSaveNow) {

        if (!store.hasStoreFor(modId)) {
            LOG.warn("No LunaLib settings store for mod '"
                + modId
                + "'; skipping write of '"
                + fieldId
                + "' (settings screen not opened yet this session)");
            return;
        }
        if (!store.putValue(modId, fieldId, value)) {
            return;
        }
        if (shouldSaveNow && store.saveStore(modId)) {
            modsWithUnsavedWrites.remove(modId);
        } else if (!shouldSaveNow) {
            modsWithUnsavedWrites.add(modId);
        }
        store.announceSettingsChanged(modId);
    }
}
