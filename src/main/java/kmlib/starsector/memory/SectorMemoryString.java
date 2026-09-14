package kmlib.starsector.memory;

import com.fs.starfarer.api.campaign.rules.MemoryAPI;

/**
 * A single string stored in sector memory, which serialises into the save so a player's choice
 * survives reload - the string sibling of {@link SectorMemoryFlag}. Wraps the raw
 * {@code getMemoryWithoutUpdate} read/write behind a key: absent from memory (never written, or read
 * before the sector exists) reads back as null, which the caller treats as "nothing stored, use the
 * default", rather than dereferencing a null sector.
 *
 * <p>Instance per key: construct once with the memory key, then read and write through it. The key is
 * the save-serialised identity, so it must stay stable once shipped - renaming it silently drops
 * every existing save's stored value.
 *
 * <p>{@link #set} and {@link #clear} report whether they actually touched memory, so a caller that
 * must fire a side effect only on a real change - a repaint request, say - can gate on the return
 * rather than re-deriving whether the sector exists or the key was set.
 */
public final class SectorMemoryString {
    private final String key;

    /**
     * @param key the sector-memory key the value is stored under; stable once shipped, since it is
     *            what the save serialises
     */
    public SectorMemoryString(String key) {
        this.key = key;
    }

    /**
     * @return the stored string, or null when the key is absent (never written, or read before the
     *         sector exists)
     */
    public String get() {
        MemoryAPI memory = SectorMemoryAccess.readSectorMemory();
        if (memory == null || !memory.contains(key)) {
            return null;
        }
        return memory.getString(key);
    }

    /**
     * @return whether a value is stored under the key - false before the sector exists or when the
     *         key was never written
     */
    public boolean isSet() {
        MemoryAPI memory = SectorMemoryAccess.readSectorMemory();
        return memory != null && memory.contains(key);
    }

    /**
     * Records the value in sector memory. A no-op before the sector exists, since there is no save to
     * write into yet.
     *
     * @param value the string to store
     * @return whether the write landed - false only before the sector exists, so a caller can gate a
     *         follow-on side effect on a real write
     */
    public boolean set(String value) {
        MemoryAPI memory = SectorMemoryAccess.readSectorMemory();
        if (memory == null) {
            return false;
        }
        memory.set(key, value);
        return true;
    }

    /**
     * Removes the stored value. A no-op before the sector exists or when the key holds nothing.
     *
     * @return whether a stored value was removed - false before the sector exists or when the key was
     *         already absent, so a caller can gate a follow-on side effect on a real removal
     */
    public boolean clear() {
        MemoryAPI memory = SectorMemoryAccess.readSectorMemory();
        if (memory == null || !memory.contains(key)) {
            return false;
        }
        memory.unset(key);
        return true;
    }
}
