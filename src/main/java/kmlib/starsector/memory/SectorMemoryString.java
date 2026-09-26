package kmlib.starsector.memory;

import com.fs.starfarer.api.campaign.SectorAPI;
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
 * <p>Every operation comes in two forms. The one naming a sector acts on that sector, for code handed
 * its sector by whoever chose it; the bare one acts on the running sector. Both answer alike, so the
 * difference is only which save the value lands in.
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
     * Removes the stored value from the running sector. See {@link #clear(SectorAPI)}.
     *
     * @return whether a stored value was removed
     */
    public boolean clear() {
        return clearIn(SectorMemoryAccess.readSectorMemory());
    }

    /**
     * Removes the stored value. A no-op before the sector exists or when the key holds nothing.
     *
     * @param sector the sector whose memory to clear
     * @return whether a stored value was removed - false before the sector exists or when the key was
     *         already absent, so a caller can gate a follow-on side effect on a real removal
     */
    public boolean clear(SectorAPI sector) {
        return clearIn(SectorMemoryAccess.readSectorMemory(sector));
    }

    /**
     * Reads the running sector's stored string. See {@link #get(SectorAPI)}.
     *
     * @return the stored string, or null when the key is absent
     */
    public String get() {
        return readFrom(SectorMemoryAccess.readSectorMemory());
    }

    /**
     * @param sector the sector whose memory to read
     * @return the stored string, or null when the key is absent (never written, or read before the
     *         sector exists)
     */
    public String get(SectorAPI sector) {
        return readFrom(SectorMemoryAccess.readSectorMemory(sector));
    }

    /**
     * Whether the running sector holds a value under the key. See {@link #isSet(SectorAPI)}.
     *
     * @return whether a value is stored
     */
    public boolean isSet() {
        return isSetIn(SectorMemoryAccess.readSectorMemory());
    }

    /**
     * @param sector the sector whose memory to read
     * @return whether a value is stored under the key - false before the sector exists or when the
     *         key was never written
     */
    public boolean isSet(SectorAPI sector) {
        return isSetIn(SectorMemoryAccess.readSectorMemory(sector));
    }

    /**
     * Records the value in the running sector's memory. See {@link #set(SectorAPI, String)}.
     *
     * @param value the string to store
     * @return whether the write landed
     */
    public boolean set(String value) {
        return writeTo(SectorMemoryAccess.readSectorMemory(), value);
    }

    /**
     * Records the value in sector memory. A no-op before the sector exists, since there is no save to
     * write into yet.
     *
     * @param sector the sector whose memory to write
     * @param value  the string to store
     * @return whether the write landed - false only before the sector exists, so a caller can gate a
     *         follow-on side effect on a real write
     */
    public boolean set(SectorAPI sector, String value) {
        return writeTo(SectorMemoryAccess.readSectorMemory(sector), value);
    }

    // Each operation once, over whichever memory the public form resolved; null stands for "no sector
    // yet" whichever way it was reached.
    private boolean clearIn(MemoryAPI memory) {
        if (memory == null || !memory.contains(key)) {
            return false;
        }
        memory.unset(key);
        return true;
    }

    private boolean isSetIn(MemoryAPI memory) {
        return memory != null && memory.contains(key);
    }

    private String readFrom(MemoryAPI memory) {
        if (memory == null || !memory.contains(key)) {
            return null;
        }
        return memory.getString(key);
    }

    private boolean writeTo(MemoryAPI memory, String value) {
        if (memory == null) {
            return false;
        }
        memory.set(key, value);
        return true;
    }
}
