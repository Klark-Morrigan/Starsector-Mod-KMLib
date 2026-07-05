package kmlib.starsector.memory;

import com.fs.starfarer.api.Global;

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
     * @return the stored value, or the default when the key is absent (never written, or
     *         read before the sector exists)
     */
    public boolean isSet() {
        var sector = Global.getSector();
        if (sector == null) {
            return defaultValue;
        }
        var memory = sector.getMemoryWithoutUpdate();
        if (memory == null || !memory.contains(key)) {
            return defaultValue;
        }
        return memory.getBoolean(key);
    }

    /**
     * Records the value in sector memory. A no-op before the sector exists, since there is
     * no save to write into yet.
     *
     * @param isSet the value to store
     */
    public void set(boolean isSet) {
        var sector = Global.getSector();
        if (sector == null) {
            return;
        }
        sector.getMemoryWithoutUpdate().set(key, isSet);
    }

    /** Flips the stored value between true and false. */
    public void toggle() {
        set(!isSet());
    }
}
