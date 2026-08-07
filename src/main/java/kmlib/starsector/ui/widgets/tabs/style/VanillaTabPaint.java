package kmlib.starsector.ui.widgets.tabs.style;

import java.awt.Color;

/**
 * The three colours a vanilla tab is painted from, which every shade it can settle on is worked out of:
 * the engine's dark button fill, the label colour its glow is a whitened form of, and the surface it all
 * stands over. Bundled because they only ever travel together - a shade asked for from two of them and a
 * third borrowed from somewhere else would not be a shade the engine shows - and because the states of one
 * strip then differ in the glow amount alone rather than in three colours re-threaded per state.
 *
 * <p>Substrate-independent, like the rest of this package: colour values with nothing GL about them.
 *
 * @param fill        the engine's dark button fill, alpha and all - it sets both the shade beneath the
 *                    glow and, through its alpha, how much glow lands on it
 * @param labelColour the tab's label colour, which the glow is a whitened form of
 * @param backdrop    the surface the tab is drawn over, taken as opaque
 */
public record VanillaTabPaint(
    Color fill,
    Color labelColour,
    Color backdrop) {
}
