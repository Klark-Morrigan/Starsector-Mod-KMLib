package kmlib.starsector.factions;

import java.awt.Color;

/**
 * A faction's two authored UI palette shades, paired the same way a political-map owner
 * pairs its colours: the bright colour as primary, the dark colour as secondary.
 *
 * @param primaryColour   the faction's bright UI colour
 * @param secondaryColour the faction's dark UI colour
 */
public record FactionPalette(
    Color primaryColour,
    Color secondaryColour) {
}
