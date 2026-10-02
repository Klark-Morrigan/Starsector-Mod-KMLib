package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.text.KmlibStrings;

import java.util.LinkedHashSet;
import java.util.List;

/**
 * Reads a faction's authored names, so the choice between its two forms and what an absent faction
 * or a blank name reads as are stated once rather than at every surface that names a faction.
 *
 * <p>A faction declares a short display name and a long one, and either may be blank: plenty of
 * modded factions declare no long name, or the same text twice. Which form a surface wants is its
 * own choice; what this settles is how the two are read.
 */
public final class FactionNames {

    // Reads only; never instantiated.
    private FactionNames() {
    }

    /**
     * One of a faction's two names, as authored.
     *
     * @param faction the faction to name; null yields null
     * @param form    which of its names to read
     * @return the name in that form, exactly as authored and possibly blank; null for a null faction
     */
    public static String resolveName(FactionAPI faction, FactionNameForm form) {

        if (faction == null) {
            return null;
        }

        return form == FactionNameForm.SHORT
            ? faction.getDisplayName()
            : faction.getDisplayNameLong();
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
    public static List<String> listEveryName(SectorAPI sector) {

        if (sector == null) {
            return List.of();
        }

        var factions = sector.getAllFactions();
        var names = new LinkedHashSet<String>();

        for (var form : FactionNameForm.values()) {
            names.addAll(KmlibStrings.collectTexts(factions, faction -> resolveName(faction, form)));
        }

        return List.copyOf(names);
    }

    /**
     * The fullest name a faction carries: its long name, or its short one where the long is blank,
     * trimmed either way - for a surface that wants a faction named as completely as it can be and
     * has no use for a blank.
     *
     * @param faction the faction to name; null yields null
     * @return the trimmed long name, else the trimmed short name, else null when neither has text
     */
    public static String resolveFullestName(FactionAPI faction) {

        var longName = resolveName(faction, FactionNameForm.LONG);
        if (KmlibStrings.hasText(longName)) {
            return longName.trim();
        }

        var shortName = resolveName(faction, FactionNameForm.SHORT);
        return KmlibStrings.hasText(shortName)
            ? shortName.trim()
            : null;
    }
}
