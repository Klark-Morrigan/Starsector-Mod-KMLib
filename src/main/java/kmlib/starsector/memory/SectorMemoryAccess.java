package kmlib.starsector.memory;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

/**
 * The single null-safe entry point to sector memory, so the "no sector yet" guard lives in one
 * place instead of being repeated at every read and write site. Sector memory only exists once a
 * game is loaded, so any code that touches it must no-op cleanly before a save exists (the main
 * menu, early load); routing every access through here is how that guard stays in one spot.
 */
public final class SectorMemoryAccess {

    private SectorMemoryAccess() {
    }

    /**
     * @return the sector's memory, or null before the sector exists or while it carries no
     *         memory yet - the caller treats null as "no persisted state, use the default"
     */
    public static MemoryAPI readSectorMemory() {
        return readSectorMemory(Global.getSector());
    }

    /**
     * The same guard over a sector the caller already holds, for code handed its sector rather than
     * reading the running one.
     *
     * @param sector the sector whose memory to read, or null where there is none yet
     * @return that sector's memory, or null when there is no sector or it carries no memory yet
     */
    public static MemoryAPI readSectorMemory(SectorAPI sector) {
        if (sector == null) {
            return null;
        }
        return sector.getMemoryWithoutUpdate();
    }
}
