package kmlib.starsector.ui.widgets.tabs;

import kmlib.colour.Colours;

import java.awt.Color;

/**
 * The two shades a vanilla tab settles on, worked out from the engine's own colours by the engine's own
 * rule: a tab at rest is its dark button fill laid over what is behind it, and a lit tab is that same
 * fill with a glow added on top - the glow being the tab's label colour half-way to white, at half
 * strength, tempered by how solid the fill under it is.
 *
 * <p>Vanilla arrives at the lit shade by drawing two passes, the second additively, with the glow
 * animated between them; a strip that paints one fill per tab cannot do that, so the same arithmetic is
 * run once here and the result handed over as a settled colour. The lit shade is therefore the glow at
 * full, which is where a lit tab rests.
 *
 * <p>Computed rather than sampled, so a restyled install reaches these tabs exactly as it reaches the
 * engine's own: both shades are functions of two settings colours, and nothing here is a measurement
 * that would go stale the moment either changed. That the fills answer to settings and not to the player
 * faction is the engine's choice, not a simplification - a vanilla tab takes no faction colour at all.
 *
 * <p>Both come back opaque. A tab fill is a surface: left translucent it would be the colour of whatever
 * the strip happens to be drawn over, so a row would change shade with its surroundings.
 */
public final class VanillaTabFills {

    // How far the glow's colour stands from the tab's label colour toward white.
    private static final float GLOW_WHITE_MIX = 0.5f;

    // How much of that glow a fully lit tab carries.
    private static final float GLOW_STRENGTH = 0.5f;

    // The glow is tempered by the fill's own alpha, but not from nothing: a fill this much short of
    // opaque still takes the glow in full, so a see-through fill dims its tab's lit state without
    // extinguishing it.
    private static final float GLOW_ALPHA_HEADROOM = 50f;

    // The full alpha channel the headroom is measured against.
    private static final float OPAQUE_ALPHA = 255f;

    // The glow at full, which is where a lit tab settles once its fade-in has run.
    private static final float FULL_GLOW = 1f;

    private VanillaTabFills() {
    }

    /**
     * The shade a resting tab settles on: its fill laid over the backdrop behind it.
     *
     * @param fill     the engine's dark button fill, alpha and all
     * @param backdrop the surface the tab is drawn over
     * @return the opaque resting shade
     */
    public static Color resolveRestingFill(Color fill, Color backdrop) {
        return Colours.flattenOnto(fill, backdrop);
    }

    /**
     * The shade a lit tab settles on: the resting shade with the glow added on top at full strength.
     *
     * @param fill        the engine's dark button fill, alpha and all - it sets both the shade beneath the
     *                    glow and, through its alpha, how much glow lands on it
     * @param labelColour the tab's label colour, which the glow is a whitened form of
     * @param backdrop    the surface the tab is drawn over
     * @return the opaque lit shade
     */
    public static Color resolveLitFill(Color fill, Color labelColour, Color backdrop) {
        return Colours.addOverlay(
            resolveRestingFill(fill, backdrop),
            resolveGlowColour(labelColour),
            resolveGlowWeight(fill));
    }

    // The glow's own colour: the label colour part-way to white, so a lit tab brightens toward its own
    // text rather than toward a shade named nowhere.
    private static Color resolveGlowColour(Color labelColour) {
        return Colours.blendRgbTowards(labelColour, Color.WHITE, GLOW_WHITE_MIX);
    }

    // How much glow a fully lit tab takes: half strength, scaled down when the fill beneath it is
    // see-through enough to show through the light.
    private static float resolveGlowWeight(Color fill) {
        return FULL_GLOW
            * GLOW_STRENGTH
            * Math.min(1f, (fill.getAlpha() + GLOW_ALPHA_HEADROOM) / OPAQUE_ALPHA);
    }
}
