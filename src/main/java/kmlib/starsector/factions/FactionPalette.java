package kmlib.starsector.factions;

import java.awt.Color;

/**
 * The two shades something is painted in on a faction's behalf, paired the way a faction's own UI
 * colours pair: the brighter colour as primary, the darker one as secondary.
 *
 * <p>Usually a faction's own authored UI pair, which is what the name says. A pair derived rather
 * than authored - a relation ramp shade against a darkened form of itself, say - fills the same two
 * slots, so anything painting in a faction's two shades takes it without knowing where it came from.
 *
 * @param primaryColour   the bright shade, as a faction's bright UI colour is
 * @param secondaryColour the dark shade, as a faction's dark UI colour is
 */
public record FactionPalette(
    Color primaryColour,
    Color secondaryColour) {
}
