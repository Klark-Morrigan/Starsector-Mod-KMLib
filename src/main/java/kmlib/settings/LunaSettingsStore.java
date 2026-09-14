package kmlib.settings;

/**
 * Where a mod's LunaLib settings are kept: an in-memory JSON object the readers read from, backed
 * by a file on disk.
 *
 * <p>A port rather than a static reach, for the reason every port here is one - the store is found
 * through LunaLib's own static loader, so a caller naming that reach directly could only ever run
 * inside a running game with LunaLib installed. Named as a role, so what keeps the settings is the
 * caller's to choose.
 *
 * <p>Keyed by mod ID rather than handed out as a per-mod handle, because a handle is a thing a
 * caller can hold past the moment the store behind it is replaced. LunaLib reloads a mod's store
 * when its settings are re-read, and a stale handle would then write into an object nothing reads.
 *
 * <p>Every operation fails soft and says so in its return, since a settings write is never worth
 * taking down the pass that made it: the store is absent until LunaLib has loaded the mod's
 * settings, and the disk beneath it can refuse a save at any time.
 */
public interface LunaSettingsStore {

    /**
     * @param modId the mod's LunaLib settings ID
     * @return whether the mod's settings have been loaded, so there is a store to write into at all
     */
    boolean hasStoreFor(String modId);

    /**
     * @param modId   the mod's LunaLib settings ID
     * @param fieldId the field's ID
     * @return whether the mod's store carries a value under that field
     */
    boolean hasValue(String modId, String fieldId);

    /**
     * Puts a value into the mod's store, where the readers see it at once.
     *
     * @param modId   the mod's LunaLib settings ID
     * @param fieldId the field's ID
     * @param value   what to store; a string or a boolean, being what LunaLib's fields hold
     * @return whether the value landed, so a caller can leave the store unmarked for a save that
     *         would have nothing new to write
     */
    boolean putValue(String modId, String fieldId, Object value);

    /**
     * Takes a field out of the mod's store altogether, which LunaLib itself never does.
     *
     * @param modId   the mod's LunaLib settings ID
     * @param fieldId the field's ID
     * @return whether a value was there to remove
     */
    boolean removeValue(String modId, String fieldId);

    /**
     * Writes the mod's whole store to disk, so what it holds survives a restart.
     *
     * @param modId the mod's LunaLib settings ID
     * @return whether the file was written; false leaves the values live in memory and unsaved
     */
    boolean saveStore(String modId);

    /**
     * Tells LunaLib's listeners that a mod's settings changed, so the settings screen and any
     * change-driven state rebuild against what was just written.
     *
     * @param modId the mod's LunaLib settings ID
     */
    void announceSettingsChanged(String modId);
}
