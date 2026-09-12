package kmlib.testfixtures.starsector.settings;

import kmlib.settings.LunaSettingsStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A LunaLib settings store that really holds what is written into it, for a subject that writes a
 * mod's settings and decides when they reach disk.
 *
 * <p>Backed by maps rather than by stubbed answers, for the reason the common-data fake is: a suite
 * exercising a writer has to give the write somewhere to land and then read it back the way the
 * game would, so a value written under one field and read under another fails here rather than
 * passing on two stubs that agree.
 *
 * <p>Saves are counted rather than merely recorded, because what separates the two write paths is
 * how many disk writes a burst of edits costs - a path that saved every time and one that batched
 * would leave the same values behind and differ only in that count.
 *
 * <p>Both refusals the live store answers with are posable, since failing soft is the contract
 * rather than an accident of it: {@link #loseStoreFor} poses settings LunaLib has not loaded, and
 * {@link #refuseSaves} a disk that will not take the file. Neither empties what is stored, what is
 * posed being a store that cannot be reached rather than one that has been wiped.
 */
public final class LunaSettingsStoreFake implements LunaSettingsStore {

    // What each mod's store holds. Insertion ordered so a case reading a store back sees the fields
    // in the order they were written.
    private final Map<String, Map<String, Object>> valuesByMod = new LinkedHashMap<>();
    private final Map<String, Integer> saveCounts = new HashMap<>();
    private final List<String> announcedMods = new ArrayList<>();

    private boolean isSavable = true;

    @Override
    public boolean hasStoreFor(String modId) {
        return valuesByMod.containsKey(modId);
    }

    @Override
    public boolean hasValue(String modId, String fieldId) {
        return valuesByMod.containsKey(modId) && valuesByMod.get(modId).containsKey(fieldId);
    }

    @Override
    public boolean putValue(String modId, String fieldId, Object value) {

        if (!valuesByMod.containsKey(modId)) {
            return false;
        }
        valuesByMod.get(modId).put(fieldId, value);

        return true;
    }

    @Override
    public boolean removeValue(String modId, String fieldId) {
        return hasValue(modId, fieldId) && valuesByMod.get(modId).remove(fieldId) != null;
    }

    @Override
    public boolean saveStore(String modId) {

        if (!isSavable || !valuesByMod.containsKey(modId)) {
            return false;
        }
        saveCounts.merge(modId, 1, Integer::sum);

        return true;
    }

    @Override
    public void announceSettingsChanged(String modId) {
        announcedMods.add(modId);
    }

    /**
     * Poses a mod whose settings LunaLib has loaded, which is the state every write needs and the
     * one a session reaches the first time any of that mod's settings is read.
     *
     * @param modId the mod's LunaLib settings id
     */
    public void openStoreFor(String modId) {
        valuesByMod.put(modId, new LinkedHashMap<>());
    }

    /**
     * Poses settings LunaLib has not loaded, or has reloaded away, so a write has nowhere to land.
     *
     * @param modId the mod's LunaLib settings id
     */
    public void loseStoreFor(String modId) {
        valuesByMod.remove(modId);
    }

    /**
     * Seeds a field as though an earlier session had stored it. Counted as no save, standing for
     * what the file held before the case began.
     *
     * @param modId   the mod's LunaLib settings id
     * @param fieldId the field's id
     * @param value   what the store holds under it
     */
    public void storeValue(String modId, String fieldId, Object value) {
        valuesByMod.computeIfAbsent(modId, mod -> new LinkedHashMap<>()).put(fieldId, value);
    }

    /**
     * @param modId   the mod's LunaLib settings id
     * @param fieldId the field's id
     * @return what the store holds under it, or null where it holds nothing
     */
    public Object readStoredValue(String modId, String fieldId) {
        return hasValue(modId, fieldId) ? valuesByMod.get(modId).get(fieldId) : null;
    }

    /**
     * @param modId the mod's LunaLib settings id
     * @return how many times the mod's store has been written to disk, so a case can show a save
     *         was deferred rather than only that the value ended up stored
     */
    public int countSavesOf(String modId) {
        return saveCounts.getOrDefault(modId, 0);
    }

    /**
     * @return the mods whose listeners were told their settings changed, in the order they were
     *         told, so a case can show a write that stayed silent
     */
    public List<String> readAnnouncedMods() {
        return List.copyOf(announcedMods);
    }

    /** Poses a disk that will not take the file, which the live store reports rather than throws. */
    public void refuseSaves() {
        isSavable = false;
    }

    /**
     * Poses the disk taking files again, for a case about what a subject does at the settling point
     * after a refusal - a refused save is a moment rather than a state the store stays in.
     */
    public void allowSaves() {
        isSavable = true;
    }
}
