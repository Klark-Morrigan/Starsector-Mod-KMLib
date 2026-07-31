package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;

/**
 * Reads the switches a faction is configured with, confining those lookups to one place.
 *
 * <p>These are fields of the faction's {@code getCustom()} data - the authored configuration
 * shipped in its {@code .faction} file - not memory flags. Nothing here touches a faction's
 * memory, so a read answers the same way whatever the campaign has done since.
 *
 * <p>Grouped rather than split one class per switch, because they share a substrate: each is a
 * nested lookup into the same JSON, each has to decide what absent or malformed data means, and
 * that decision belongs in one class rather than restated per field. Sibling to
 * {@link FactionCrests} and {@link StarsectorFactionColors}, which confine the crest and colour
 * lookups.
 *
 * <p>Absent or malformed data reads as the off position throughout, matching the base game, so
 * every caller agrees on what missing data means rather than each re-deciding.
 */
public final class FactionFlags {

    private static final String PUNITIVE_EXPEDITION_DATA = "punitiveExpeditionData";
    private static final String TERRITORIAL_FLAG = "territorial";

    private FactionFlags() {
    }

    /**
     * Whether the faction treats the space around its holdings as its own - the disposition a
     * system claim turns on.
     *
     * <p>Territoriality has no field of its own, but lives inside the data configuring punitive
     * expeditions: mounting expeditions over one's space and claiming that space are the same
     * disposition, so the base game stores them together.
     *
     * @param faction the faction to read, or null
     * @return true when the faction's punitive-expedition data marks it territorial; false for a
     *         null faction, absent custom data, or data carrying no territorial flag
     */
    public static boolean isTerritorial(FactionAPI faction) {
        if (faction == null) {
            return false;
        }
        var custom = faction.getCustom();
        if (custom == null) {
            return false;
        }
        var punitiveExpeditionData = custom.optJSONObject(PUNITIVE_EXPEDITION_DATA);
        return punitiveExpeditionData != null
            && punitiveExpeditionData.optBoolean(TERRITORIAL_FLAG);
    }
}
