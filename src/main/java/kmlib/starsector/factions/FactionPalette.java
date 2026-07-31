package kmlib.starsector.factions;

import java.awt.Color;

/**
 * A faction's two authored UI palette shades, paired the same way a political-map owner
 * pairs its colors: the bright color as primary, the dark color as secondary.
 *
 * @param primaryColor   the faction's bright UI color
 * @param secondaryColor the faction's dark UI color
 */
public record FactionPalette(
    Color primaryColor,
    Color secondaryColor) {
}
