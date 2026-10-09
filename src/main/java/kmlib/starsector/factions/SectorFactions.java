package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.text.KmlibStrings;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * The sector-wide faction reads: looking a faction up by its ID, and listing what the sector's factions
 * are called. What one faction is called is {@link FactionNames}' read.
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

    /**
     * Every name the sector's factions are authored with, both forms, blanks left out and each name once
     * - for a caller that has to know what a faction may be called before it names one, such as settling
     * the face a faction's name is drawn in. A faction authoring one text as both its names is read once,
     * a second copy saying nothing the first did not.
     *
     * <p>Counts no sector walk: the faction list is a few dozen entries, whatever the size of the sector,
     * so it is not what a read's duration is judged against.
     *
     * @param sector the sector whose factions are read; null yields an empty list
     * @return every short name, then every long name not already read, in the sector's faction order
     */
    public static List<String> listFactionNames(SectorAPI sector) {

        if (sector == null) {
            return List.of();
        }

        var factions = sector.getAllFactions();
        var names = new LinkedHashSet<String>();

        for (var form : FactionNameForm.values()) {
            names.addAll(KmlibStrings.collectTexts(factions, faction -> FactionNames.resolveName(faction, form)));
        }

        return List.copyOf(names);
    }
}
