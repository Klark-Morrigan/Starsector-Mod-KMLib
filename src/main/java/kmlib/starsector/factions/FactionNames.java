package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;

import kmlib.text.KmlibStrings;

import java.util.Objects;

/**
 * Reads a faction's authored names, so the choice between its two forms and what an absent faction
 * or a blank name reads as are stated once rather than at every surface that names a faction.
 *
 * <p>A faction declares a short display name and a long one, and either may be blank: plenty of
 * modded factions declare no long name, or the same text twice. Which form a surface wants is its
 * own choice; what this settles is how the two are read.
 *
 * <p>The player faction's short name is the one read that depends on the sector: until the player sets
 * the faction up, it is a spec's placeholder that reads as a real name in prose. The display-name reads
 * hold it back until then, by the rule {@link StarsectorPlayerFactionResolver} states. The sector-wide
 * listing of names is {@link SectorFactions#listFactionNames}.
 */
public final class FactionNames {

    // Reads only; never instantiated.
    private FactionNames() {
    }

    /**
     * The short name a faction is shown by in prose: its display name, held back for the player faction
     * until the sector records it as set up.
     *
     * <p>Any faction's name is read the same way, so a host, an occupier and the player are named by one
     * rule wherever a surface substitutes a faction's name into a sentence.
     *
     * @param sector   the sector the faction belongs to, asked whether its player faction is set up
     * @param faction  the faction to name; null yields {@code fallback}
     * @param fallback what to show where there is no name to show; never null
     * @return the faction's display name, or {@code fallback} when the faction is absent, its name is
     *         blank, or it is the player faction and the sector has not recorded it as set up
     */
    public static String resolveDisplayName(SectorAPI sector, FactionAPI faction, String fallback) {

        Objects.requireNonNull(fallback, "fallback");

        if (faction == null) {
            return fallback;
        }

        if (faction.isPlayerFaction() && !StarsectorPlayerFactionResolver.isPlayerFactionEstablished(sector)) {
            return fallback;
        }

        var displayName = resolveName(faction, FactionNameForm.SHORT);

        return KmlibStrings.hasText(displayName)
            ? displayName
            : fallback;
    }

    /**
     * {@link #resolveDisplayName} with the faction's own ID as the fallback - for a surface addressed by
     * ID, where the ID is what a reader typed to name the faction in the first place.
     *
     * @param sector  the sector the faction belongs to
     * @param faction the faction to name; null yields null
     * @return the faction's display name, or its ID where {@link #resolveDisplayName} would fall back
     */
    public static String resolveDisplayNameOrId(SectorAPI sector, FactionAPI faction) {

        return faction == null
            ? null
            : resolveDisplayName(sector, faction, faction.getId());
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

        var longName = findTrimmedName(faction, FactionNameForm.LONG);

        return longName == null
            ? findTrimmedName(faction, FactionNameForm.SHORT)
            : longName;
    }

    /**
     * What a row naming one faction shows: its long name, or the ID it was asked about where it carries
     * none - for a surface that must name something on every row, even for an ID the sector has lost.
     * A bare ID reads better there than a blank where the name belongs.
     *
     * @param faction   the faction to name; null where the ID would not resolve
     * @param factionId the ID the row was asked to name, shown where the faction has no long name
     * @return the trimmed long name, else {@code factionId}
     */
    public static String resolveLabel(FactionAPI faction, String factionId) {

        var longName = findTrimmedName(faction, FactionNameForm.LONG);

        return longName == null
            ? factionId
            : longName;
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

    // One name in one form, trimmed, or null where there is none to show. The single statement of
    // what a blank name reads as, so the readers built on it cannot disagree about one.
    private static String findTrimmedName(FactionAPI faction, FactionNameForm form) {

        var name = resolveName(faction, form);

        return KmlibStrings.hasText(name)
            ? name.trim()
            : null;
    }
}
