package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.FactionAPI;

import kmlib.colour.ColourPair;

import java.awt.Color;
import java.util.Objects;

/**
 * Reads a faction's authored UI palette, confining the {@code FactionAPI} colour lookups to one place.
 *
 * <p>Distinct from {@link kmlib.starsector.ui.colour.StarsectorUiColour}, which
 * re-exposes the engine's global palette <em>slots</em> ({@code Misc} shades):
 * this reads a specific faction's own colour. A faction can be absent - an ID the sector no longer
 * knows looks up as null through {@link SectorFactions#findFaction} - so every read says what an
 * absent faction answers. The pair read comes in two forms: a defaulting one that answers grey, and a
 * nullable one that answers nothing.
 */
public final class FactionColours {

    // What an absent faction is painted in: a mid grey, so whatever is drawn in it still reads as unowned.
    private static final Color ABSENT_FACTION_COLOUR = Color.GRAY;

    private static final ColourPair ABSENT_FACTION_PALETTE = new ColourPair(
        ABSENT_FACTION_COLOUR,
        ABSENT_FACTION_COLOUR);

    private FactionColours() {
    }

    /**
     * A faction's two authored UI shades with no fallback, for a caller whose answer to an absent
     * faction is to draw nothing rather than to draw it grey - a faction gone from the sector is one
     * such a caller cannot name, and a stand-in shade would put colour on the screen for it.
     *
     * @param faction the faction to read; null yields null
     * @return the faction's (primary, secondary) shade pair, or null when the faction is absent
     */
    public static ColourPair findPalette(FactionAPI faction) {

        return faction == null
            ? null
            : new ColourPair(faction.getBrightUIColor(), faction.getDarkUIColor());
    }

    /**
     * Whether a faction still paints in a pair {@link #findPalette} read off it earlier - for a caller
     * asking every frame whether shades it painted in have moved.
     *
     * @param faction the faction to read; null paints in no pair
     * @param palette the pair to compare against, or null for the absent faction's answer
     * @return whether {@link #findPalette} would answer an equal pair; true when both are null
     */
    public static boolean hasPalette(FactionAPI faction, ColourPair palette) {

        return Objects.equals(findPalette(faction), palette);
    }

    /**
     * Resolves a faction's base UI colour - the one shade that stands for the faction where a single
     * colour is drawn. The neutral faction's is the shade unowned space is drawn in.
     *
     * @param faction the faction to read; null falls back to a mid grey
     * @return the faction's base UI colour, or a grey fallback when the faction is absent
     */
    public static Color resolveBaseColour(FactionAPI faction) {

        return faction == null
            ? ABSENT_FACTION_COLOUR
            : faction.getBaseUIColor();
    }

    /**
     * Resolves a faction's own two authored UI palette shades - its bright colour as
     * primary and its dark colour as secondary - so a caller painting in "faction X's colours"
     * gets the exact shades the game paints that faction in.
     *
     * @param faction the faction to read; null falls back to a grey pair
     * @return the faction's (primary, secondary) shade pair, or a grey pair when the faction is
     *         absent
     */
    public static ColourPair resolvePalette(FactionAPI faction) {

        var palette = findPalette(faction);
        return palette == null
            ? ABSENT_FACTION_PALETTE
            : palette;
    }
}
