package kmlib.starsector.ui.render.gl;

import java.awt.Color;

/**
 * The collapse handle's own two shades: the colour its chevron draws in at rest, and the colour it takes
 * while the pointer is over the handle. Grouped into its own record rather than spread across {@link
 * WidgetStyle} for the same reason the tab look travels as its own
 * {@link kmlib.starsector.ui.widgets.tabs.TabStyle} - the handle is the one piece of chrome whose
 * emphasis a consumer may want pitched apart from the panel's accents, since a direction cue can be
 * asked either to stand out from the frame carrying it or to read as part of it - so its shades travel
 * as one unit that is swapped wholesale.
 *
 * <p>Both shades are given rather than derived from one another, so a look that holds a single colour
 * across both states passes that colour twice, and one that brightens under the pointer passes a
 * brighter second shade.
 *
 * @param chevron        the chevron's colour at rest
 * @param chevronHovered the chevron's colour while the handle is hovered
 */
public record NotchColours(
    Color chevron,
    Color chevronHovered) {

    /**
     * Picks which of the two shades the chevron draws in: the hovered shade while the pointer is over
     * the handle, the resting one otherwise. A look that does not distinguish the two supplies the same
     * colour for both, so the pick stays one rule rather than a flag the caller also has to set.
     *
     * @param isHovered true while the pointer is over the handle
     * @return the colour to stroke the chevron with
     */
    public Color resolveChevronColour(boolean isHovered) {
        return isHovered
            ? chevronHovered
            : chevron;
    }
}
