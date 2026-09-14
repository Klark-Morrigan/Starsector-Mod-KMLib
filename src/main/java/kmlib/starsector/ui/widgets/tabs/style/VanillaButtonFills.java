package kmlib.starsector.ui.widgets.tabs.style;

import kmlib.colour.Colours;

import java.awt.Color;

/**
 * The interior shades a vanilla raised button settles on, worked out from the colours the engine builds
 * one with: the dark accent step laid over the backing behind it. The engine hands a button a
 * three-colour set and draws its interior from the dark member, so both shades here are that member -
 * once laid down, once not.
 *
 * <p>A separate rule from {@link VanillaTabFills} rather than that one with a knob. A tab's light is its
 * own label colour taken half-way to white and tempered by how solid its fill is; a button's is the base
 * accent it was built with, added as it comes. The two land in different places for the same shade, which
 * is what says these are two rules and not one with a knob.
 *
 * <p>Interiors only. What frames a button and what backs it are constants of the chrome, drawn the same
 * whatever the button is doing - which is the whole difference from a tab, where the fill and the rule
 * around it move together.
 *
 * <p>The shown shade comes back opaque, having been composited onto the backing; the unpainted one is
 * that same shade at zero alpha, so the travel between them is an interior fading in over an unchanged
 * backing rather than two unrelated colours crossing.
 *
 * <p>Settled shades only. What the pointer adds is not among them - {@link #POINTED_GLOW} is a weight the
 * hover rule lays over whichever shade a button has settled on, so it is stated here as the vanilla fact
 * it is and applied where every other momentary lift is.
 */
public final class VanillaButtonFills {

    /**
     * How much of its base accent a button under the pointer adds. The colour is the accent itself rather
     * than white, and the weight is fitted rather than read - the engine's own constant lives behind an
     * obfuscated widget - but it is fitted twice over, from two pairs sampled within one frame each:
     *
     * <ul>
     *   <li>an unshown button over a flat map fill, #1b1d1b to #364144, which is +27, +36, +41;</li>
     *   <li>the shown button, #17424f to #346a7c, which is +29, +40, +45.</li>
     * </ul>
     *
     * <p>Both normalise to the base accent's own proportions rather than to equal channels, which is what
     * says the light is the button's colour and not plain white; the weights they give are 0.161 and
     * 0.176, and this sits between them. A pair sampled across two frames cannot settle either question -
     * the backdrop moves between the two reads, and the difference then measures the backdrop.
     *
     * <p>The same light lands on the glyphs: the bound key's gold moves by that identical +27, +36, +41,
     * which is the confirmation that this is one light over the whole button and not a fill rule.
     */
    public static final float POINTED_GLOW = 0.17f;

    // What an unpainted interior is: the shown shade with nothing of it laid down. Naming zero here is
    // what lets the resting state be a value rather than a quad the paint pass learns to skip.
    private static final float UNPAINTED_ALPHA = 0f;

    private VanillaButtonFills() {
    }

    /**
     * The interior of a button that is not being shown: the shown shade, unpainted - so the backing behind
     * it is what the eye sees, and a fade onto the shown state is that shade arriving rather than a second
     * colour travelling in from somewhere.
     *
     * @param paint the colours a vanilla button's interior is worked out from
     * @return the shown shade at zero alpha
     */
    public static Color resolveUnpaintedFill(VanillaButtonPaint paint) {
        return Colours.scaleAlpha(resolveShownFill(paint), UNPAINTED_ALPHA);
    }

    /**
     * The interior the button being shown settles on: its dark step composited onto the backing, and
     * nothing else. There is no second lit shade to compute - what the pointer adds is added over
     * whichever of these two a button has settled on.
     *
     * @param paint the colours a vanilla button's interior is worked out from
     * @return the opaque interior shade
     */
    public static Color resolveShownFill(VanillaButtonPaint paint) {
        return Colours.flattenOnto(paint.fill(), paint.backdrop());
    }
}
