package kmlib.starsector.factions;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import java.awt.Color;

/**
 * Reads a named faction's authored UI palette from the sector, confining the
 * {@code FactionAPI} colour lookups the KM mods share to one place.
 *
 * <p>Distinct from {@link kmlib.starsector.ui.color.StarsectorUiColor}, which
 * re-exposes the engine's global palette <em>slots</em> ({@code Misc} shades):
 * this reads a specific faction's own colour, with a fallback that keeps the
 * caller from having to null-check the vanilla lifecycle - both the sector and
 * the faction can be absent during early engine boot or in a bare test double.
 */
public final class StarsectorFactionColors {
    // Fallback when the neutral faction cannot be read (it always exists in
    // vanilla); a mid grey so an uninhabited outline still reads as unowned.
    private static final Color NEUTRAL_FALLBACK_COLOR = Color.GRAY;

    private StarsectorFactionColors() {
    }

    /**
     * Resolves the neutral faction's base UI colour - the shade unowned space
     * (uninhabited and decivilised) is drawn in, so it reads consistently.
     *
     * @param sector the sector to read; null falls back to a mid grey
     * @return the neutral faction's base UI colour, or a grey fallback when the
     *         sector or its neutral faction is absent
     */
    public static Color resolveNeutralColor(SectorAPI sector) {
        if (sector == null) {
            return NEUTRAL_FALLBACK_COLOR;
        }
        var neutral = sector.getFaction(Factions.NEUTRAL);
        if (neutral == null) {
            return NEUTRAL_FALLBACK_COLOR;
        }
        return neutral.getBaseUIColor();
    }
}
