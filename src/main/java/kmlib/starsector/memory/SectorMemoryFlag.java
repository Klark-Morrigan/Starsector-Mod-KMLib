package kmlib.starsector.memory;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

/**
 * A single boolean stored in sector memory, which serialises into the save, so a player's
 * choice survives reload. Wraps the raw {@code getMemoryWithoutUpdate} read/write behind a
 * key and a default: absent from memory (never written, or read before the sector exists)
 * resolves to the default rather than {@code false}, the distinction the bare
 * {@code getBoolean} cannot make.
 *
 * <p>Instance per flag: construct once with the memory key and default, then read and write
 * through it. The key is the save-serialised identity, so it must stay stable once shipped -
 * renaming it silently resets every existing save to the default.
 *
 * <p>Every operation comes in two forms, as on {@link SectorMemoryString}. The one naming a
 * sector acts on that sector, for code handed its sector by whoever chose it; the bare one acts
 * on the running sector.
 */
public final class SectorMemoryFlag {
    private final String key;
    private final boolean defaultValue;

    /**
     * @param key          the sector-memory key the value is stored under; stable once
     *                     shipped, since it is what the save serialises
     * @param defaultValue the value returned when the key is absent from memory
     */
    public SectorMemoryFlag(String key, boolean defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    /**
     * Reads the running sector's stored value. See {@link #isSet(SectorAPI)}.
     *
     * @return the stored value, or the default when the key is absent
     */
    public boolean isSet() {
        return readFrom(SectorMemoryAccess.readSectorMemory());
    }

    /**
     * @param sector the sector whose memory to read
     * @return the stored value, or the default when the key is absent (never written, or
     *         read before the sector exists)
     */
    public boolean isSet(SectorAPI sector) {
        return readFrom(SectorMemoryAccess.readSectorMemory(sector));
    }

    /**
     * Records the value in the running sector's memory. See {@link #set(SectorAPI, boolean)}.
     *
     * @param isSet the value to store
     * @return whether the write landed
     */
    public boolean set(boolean isSet) {
        return writeTo(SectorMemoryAccess.readSectorMemory(), isSet);
    }

    /**
     * Records the value in sector memory. A no-op before the sector exists, since there is
     * no save to write into yet.
     *
     * @param sector the sector whose memory to write
     * @param isSet  the value to store
     * @return whether the write landed - false only before the sector exists, so a caller can
     *         gate a follow-on side effect (a repaint request) on a real write
     */
    public boolean set(SectorAPI sector, boolean isSet) {
        return writeTo(SectorMemoryAccess.readSectorMemory(sector), isSet);
    }

    /** Flips the running sector's stored value between true and false. */
    public void toggle() {
        toggleIn(SectorMemoryAccess.readSectorMemory());
    }

    /**
     * Flips the stored value between true and false; an absent key flips from the default.
     *
     * @param sector the sector whose memory to flip the value in
     */
    public void toggle(SectorAPI sector) {
        toggleIn(SectorMemoryAccess.readSectorMemory(sector));
    }

    // Each operation once, over whichever memory the public form resolved; null stands for "no
    // sector yet" whichever way it was reached.
    private boolean readFrom(MemoryAPI memory) {
        if (memory == null || !memory.contains(key)) {
            return defaultValue;
        }
        return memory.getBoolean(key);
    }

    private void toggleIn(MemoryAPI memory) {
        writeTo(memory, !readFrom(memory));
    }

    private boolean writeTo(MemoryAPI memory, boolean isSet) {
        if (memory == null) {
            return false;
        }
        memory.set(key, isSet);
        return true;
    }
}
