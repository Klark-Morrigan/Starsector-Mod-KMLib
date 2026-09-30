package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;

import kmlib.text.KmlibStrings;

/**
 * Reads a faction's authored names, so the choice between its two forms and what an absent faction
 * or a blank name reads as are stated once rather than at every surface that names a faction.
 *
 * <p>A faction declares a short display name and a long one, and either may be blank: plenty of
 * modded factions declare no long name, or the same text twice. Which form a surface wants is its
 * own choice; what this settles is how the two are read.
 */
public final class FactionNames {

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
