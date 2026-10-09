package kmlib.colour;

import java.awt.Color;

/**
 * Two shades something is painted in, paired the way a faction's own UI colours pair: the brighter
 * colour as primary, the darker one as secondary.
 *
 * <p>Often a faction's authored UI pair. A pair derived rather than authored - a relation ramp shade
 * against a darkened form of itself, say - fills the same two slots, so anything painting in two
 * shades takes it without knowing where it came from.
 *
 * @param primaryColour   the bright shade, as a faction's bright UI colour is
 * @param secondaryColour the dark shade, as a faction's dark UI colour is
 */
public record ColourPair(
    Color primaryColour,
    Color secondaryColour) {
}
