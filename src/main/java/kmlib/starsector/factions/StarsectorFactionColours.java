package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import java.awt.Color;

/**
 * Reads a named faction's authored UI palette from the sector, confining the
 * {@code FactionAPI} colour lookups to one place.
 *
 * <p>Distinct from {@link kmlib.starsector.ui.colour.StarsectorUiColour}, which
 * re-exposes the engine's global palette <em>slots</em> ({@code Misc} shades):
 * this reads a specific faction's own colour. Both the sector and the faction can
 * be absent until the engine has finished building them, so each read comes in a
 * defaulting form that answers grey and a nullable form that answers nothing.
 */
public final class StarsectorFactionColours {
    // Fallback when the neutral faction cannot be read (it always exists in
    // vanilla); a mid grey so whatever is painted in it still reads as unowned.
    private static final Color NEUTRAL_FALLBACK_COLOUR = Color.GRAY;

    private StarsectorFactionColours() {
    }

    /**
     * A named faction's two authored UI shades with no fallback, for a caller whose answer to an
     * absent faction is to draw nothing rather than to draw it grey - a faction gone from the sector
     * is one such a caller cannot name, and a stand-in shade would put colour on the screen for it.
     *
     * @param sector    the sector to read; null yields null
     * @param factionId the faction to read
     * @return the faction's (primary, secondary) shade pair, or null when the sector or the faction
     *         is absent
     */
    public static FactionPalette findPalette(SectorAPI sector, String factionId) {

        var faction = SectorFactions.findFaction(sector, factionId);

        return faction == null
            ? null
            : new FactionPalette(faction.getBrightUIColor(), faction.getDarkUIColor());
    }

    /**
     * Resolves the neutral faction's base UI colour - the shade unowned space
     * (uninhabited and decivilised) is drawn in, so it reads consistently.
     *
     * @param sector the sector to read; null falls back to a mid grey
     * @return the neutral faction's base UI colour, or a grey fallback when the
     *         sector or its neutral faction is absent
     */
    public static Color resolveNeutralColour(SectorAPI sector) {

        var neutral = SectorFactions.findFaction(sector, Factions.NEUTRAL);

        return neutral == null
            ? NEUTRAL_FALLBACK_COLOUR
            : neutral.getBaseUIColor();
    }

    /**
     * Resolves a named faction's own two authored UI palette shades - its bright colour as
     * primary and its dark colour as secondary - so a caller painting in "faction X's colours"
     * gets the exact shades the game paints that faction in.
     *
     * @param sector    the sector to read; null falls back to a grey pair
     * @param factionId the faction to read
     * @return the faction's (primary, secondary) shade pair, or a grey pair when the
     *         sector or the faction is absent
     */
    public static FactionPalette resolvePalette(SectorAPI sector, String factionId) {

        var palette = findPalette(sector, factionId);
        return palette == null
            ? new FactionPalette(NEUTRAL_FALLBACK_COLOUR, NEUTRAL_FALLBACK_COLOUR)
            : palette;
    }
}
