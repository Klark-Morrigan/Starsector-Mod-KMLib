package kmlib.starsector.ui.widgets.tabs.style;

import kmlib.colour.Colours;

import java.awt.Color;

/**
 * The shades a vanilla tab settles on, worked out from the engine's own colours by the engine's own
 * rule: a tab is its dark button fill laid over what is behind it, with a glow added on top - the glow
 * being the tab's label colour half-way to white, at half strength, tempered by how solid the fill under
 * it is, and scaled by how brightly the tab is currently lit.
 *
 * <p>Fills only. A tab's label is not a lit form of one colour but one of two roles the engine picks
 * between - the button blue while untouched, the standard text grey once lit - so it is named by the
 * palette rather than computed here. The glow is still measured off the resting label colour, that being
 * what the engine tints its light with whatever the text above it is doing.
 *
 * <p>How brightly is the whole of what parts a tab's states. Vanilla has no separate shade for a shown
 * tab and a pointed-at one: it has one glow amount, driven up to {@code SELECTED_GLOW} while a tab is the
 * one being shown and to {@code POINTED_GLOW} while the pointer is on it, whichever is greater. That the
 * pointer's amount is the higher of the two is what keeps a pointed-at tab from being mistaken for the
 * shown one when nothing but the fill marks the shown one.
 *
 * <p>Vanilla arrives at each shade by drawing two passes, the second additively, with the glow animated
 * between them; a strip that paints one fill per tab cannot do that, so the same arithmetic is run once
 * here and each state's result handed over as a settled colour for the fade between them to travel
 * across.
 *
 * <p>Computed rather than sampled, so a restyled install reaches these tabs exactly as it reaches the
 * engine's own: every shade is a function of two settings colours, and nothing here is a measurement
 * that would go stale the moment either changed. That the fills answer to settings and not to the player
 * faction is the engine's choice, not a simplification - a vanilla tab takes no faction colour at all.
 *
 * <p>All come back opaque. A tab fill is a surface: left translucent it would be the colour of whatever
 * the strip happens to be drawn over, so a row would change shade with its surroundings.
 */
public final class VanillaTabFills {

    /**
     * How brightly the engine lights the tab it is showing - well short of the full glow, so the tab under
     * the pointer stands clearly above it and a press above that again. Measured against the engine's own
     * Sector/System tabs, where a shown tab reads as a moderate lift off the resting fill rather than as
     * the near-full one it takes under a held press.
     */
    public static final float SELECTED_GLOW = 0.45f;

    /**
     * How brightly the engine lights the tab under the pointer - above the shown tab, so the two never read
     * alike, and below the lift a press adds on top.
     *
     * <p>Short of the full glow rather than at it. The two-pass additive draw this is worked out from is an
     * approximation of the engine's, so the amounts are measured against its own tabs rather than declared:
     * at the full glow a pointed-at tab came out plainly brighter than the Sector/System tabs beside it,
     * which is the comparison that decides this number.
     */
    public static final float POINTED_GLOW = 0.65f;

    /** No glow at all, which is where an untouched tab rests. */
    public static final float NO_GLOW = 0f;

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

    private VanillaTabFills() {
    }

    /**
     * The shade a resting tab settles on: its fill laid over the backdrop behind it, with no glow.
     *
     * @param paint the three colours a vanilla tab is painted from
     * @return the opaque resting shade
     */
    public static Color resolveRestingFill(VanillaTabPaint paint) {
        return Colours.flattenOnto(paint.fill(), paint.backdrop());
    }

    /**
     * The shade a tab settles on at a given glow: the resting shade with that much of the glow added on
     * top. One method for every lit state, since the states differ in nothing but the amount - a second
     * shade named beside this one would be a second thing to keep in step with the engine's.
     *
     * @param paint      the three colours a vanilla tab is painted from
     * @param glowAmount how brightly the tab is lit: {@link #NO_GLOW}, {@link #SELECTED_GLOW}, or
     *                   {@link #POINTED_GLOW}
     * @return the opaque shade at that glow
     */
    public static Color resolveFillAtGlow(VanillaTabPaint paint, float glowAmount) {
        return addGlowOnto(resolveRestingFill(paint), paint, glowAmount);
    }

    /**
     * The glow's own colour: the label colour part-way to white, so a lit tab brightens toward its own text
     * rather than toward a shade named nowhere. Exposed because it is the axis every brightening of a
     * vanilla tab travels along, a momentary lift over a settled fill included - a lift aimed anywhere else
     * parts from the fills at the moment it is most visible, whatever depth it is given.
     *
     * @param labelColour the tab's own label colour, the shade its glow is measured off
     * @return the colour a lit tab brightens toward
     */
    public static Color resolveGlowColour(Color labelColour) {
        return Colours.blendRgbTowards(labelColour, Color.WHITE, GLOW_WHITE_MIX);
    }

    // One colour of a tab with the glow added over it. The whole of what the engine's single glow pass
    // does, held once rather than spelt out per surface it lands on: the fill and the label take the same
    // light at the same amount, so a caller choosing which of them to light picks the base and nothing
    // else. Two call sites each naming the colour and the weight themselves would be two places for that
    // pass to drift apart, which is exactly the drift that had labels answering to a state instead of to
    // the glow.
    private static Color addGlowOnto(Color base, VanillaTabPaint paint, float glowAmount) {
        return Colours.addOverlay(
            base,
            resolveGlowColour(paint.labelColour()),
            resolveGlowWeight(paint.fill(), glowAmount));
    }

    // How much glow a tab takes at that amount: half strength scaled by it, and scaled down again when
    // the fill beneath is see-through enough to show through the light.
    private static float resolveGlowWeight(Color fill, float glowAmount) {
        return glowAmount
            * GLOW_STRENGTH
            * Math.min(1f, (fill.getAlpha() + GLOW_ALPHA_HEADROOM) / OPAQUE_ALPHA);
    }
}
