package kmlib.starsector.ui.widgets.tabs.style;

import kmlib.colour.Colours;
import kmlib.starsector.ui.colour.StarsectorUiColour;

import java.awt.Color;

/**
 * The interior shades a vanilla raised button settles on, worked out from the colours the engine builds
 * one with: a button is its dark accent step laid over the backing behind it, with the pointer adding its
 * base accent on top. The engine hands a button a three-colour set and draws its interior from the dark
 * member, so both shades here are that member - once as it comes, once brightened.
 *
 * <p>A separate rule from {@link VanillaTabFills} rather than that one with a knob. A tab's light is its
 * own label colour taken half-way to white and tempered by how solid its fill is - so a strip's glow
 * carries the row's own colour - where a button takes plain white at a weight of its own. The two land in
 * different places for the same lit shade, which is what says these are two rules and not one with a
 * knob.
 *
 * <p>Interiors only. What frames a button and what backs it are constants of the chrome, drawn the same
 * whatever the button is doing - which is the whole difference from a tab, where the fill and the rule
 * around it move together.
 *
 * <p>The lit shades come back opaque, having been composited onto the backing; the unpainted one is that
 * same shade at zero alpha, so the travel between them is an interior fading in over an unchanged backing
 * rather than two unrelated colours crossing.
 */
public final class VanillaButtonFills {

    /**
     * The light a button under the pointer takes: white, at {@link #POINTED_GLOW}. Fitted rather than
     * read, the engine's own constant living behind an obfuscated widget - but fitted against a
     * side-by-side sample of the two rows, where a vanilla button lit at #17424f reads #3f6d78 under the
     * pointer. That is +42, +44, +43 - one weight on every channel against white, and three different
     * weights against the accent the button is built from, which is what says the light is white and not
     * more of the button's own colour.
     */
    public static final Color POINTED_LIGHT = StarsectorUiColour.WHITE.resolve();

    /**
     * How much {@link #POINTED_LIGHT} a button under the pointer takes. Lands within a channel value of
     * the sampled shade on all three channels.
     */
    public static final float POINTED_GLOW = 0.17f;

    /** No glow at all, which is where the button the panel is showing sits. */
    public static final float NO_GLOW = 0f;

    // What an unpainted interior is: the lit shade with nothing of it laid down. Naming zero here is what
    // lets the resting state be a value rather than a quad the paint pass learns to skip.
    private static final float UNPAINTED_ALPHA = 0f;

    private VanillaButtonFills() {
    }

    /**
     * The interior of a button that is not being shown: its lit shade, unpainted - so the backing behind
     * it is what the eye sees, and a fade onto any lit state is that shade arriving rather than a second
     * colour travelling in from somewhere.
     *
     * @param paint the three colours a vanilla button is painted from
     * @return the lit shade at zero alpha
     */
    public static Color resolveUnpaintedFill(VanillaButtonPaint paint) {
        return Colours.scaleAlpha(resolveFillAtGlow(paint, NO_GLOW), UNPAINTED_ALPHA);
    }

    /**
     * The interior a lit button settles on at a given glow: its dark step composited onto the backing,
     * with that much of its base accent added on top. One method for both lit states, since they differ
     * in the amount alone.
     *
     * @param paint      the three colours a vanilla button is painted from
     * @param glowAmount how brightly the button is lit: {@link #NO_GLOW} for the one being shown,
     *                   {@link #POINTED_GLOW} for the one under the pointer
     * @return the opaque interior shade at that glow
     */
    public static Color resolveFillAtGlow(VanillaButtonPaint paint, float glowAmount) {
        return Colours.addOverlay(
            Colours.flattenOnto(paint.fill(), paint.backdrop()),
            paint.glowColour(),
            glowAmount);
    }
}
