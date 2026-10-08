package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.text.KmlibStrings;

/**
 * Looks a faction up by its ID, so the guards every faction read needs before it reaches the sector are
 * stated once - the faction counterpart of {@link kmlib.starsector.markets.SectorMarkets}.
 *
 * <p>The sector is absent until the engine has built it, and an ID can name a faction a mod removed or
 * nothing at all. Each of these is no faction rather than a fault, so a caller holding a faction ID has
 * one null to handle instead of three.
 */
public final class SectorFactions {

    // Reads only; never instantiated.
    private SectorFactions() {
    }

    /**
     * The faction an ID names.
     *
     * @param sector    the sector to look in; null yields null
     * @param factionId the ID to look up; null or blank yields null without asking the sector
     * @return the faction, or null when the sector is absent or knows no faction by that ID
     */
    public static FactionAPI findFaction(SectorAPI sector, String factionId) {

        // An ID naming nobody is looked up as nobody rather than handed to the sector, which is free
        // to fault on it.
        if (sector == null || !KmlibStrings.hasText(factionId)) {
            return null;
        }

        return sector.getFaction(factionId);
    }
}
