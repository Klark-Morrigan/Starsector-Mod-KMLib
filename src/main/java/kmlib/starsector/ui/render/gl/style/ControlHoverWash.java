package kmlib.starsector.ui.render.gl.style;

import kmlib.starsector.ui.colour.AccentColours;
import kmlib.starsector.ui.render.gl.UiElementPaint;

import java.awt.Color;

/**
 * How far a body control lifts under the pointer, as the wash its hovered cell takes: a colour and the
 * alpha that colour reaches at a full hover. The body widgets carry no paint description of their own -
 * they draw from a panel's {@link AccentColours} and the lit state their spec names - so the treatment the
 * pointer adds sits here beside the accents it is a lift over, rather than as a float inside a renderer
 * where retuning it would mean hunting through draw calls.
 *
 * <p>The pace is not here and never will be. Every element of a panel travels onto its hovered look on one
 * shared pair of durations, because a panel answering the pointer at two speeds reads as two panels; what
 * that travel *arrives at* is the widget's own, which is what this names. A tab meets a shade, the collapse
 * handle takes an accent wash over its whole face, and a body cell washes - three treatments, one clock.
 *
 * <p>A wash rather than a brightened chrome because a body control's chrome is a hairline frame and a small
 * mark, neither of which can carry a lift the eye finds without also changing what the control is saying: a
 * tick box brightened toward its tick colour reads as half-ticked. A wash lands on the cell rather than on
 * the marks in it, so it says "the pointer is here" and nothing about the control's state.
 *
 * @param colour         the shade a hovered cell is washed in
 * @param fullHoverAlpha the alpha that wash reaches once the cell is fully hovered, before the panel's own
 *                       opacity fades it - the one number to retune if the body lights too hard or too
 *                       faintly against the chrome around it
 */
public record ControlHoverWash(
    Color colour,
    float fullHoverAlpha) {

    // How strongly a fully hovered cell washes, as a fraction of the panel's opacity. Below the selected
    // wash's own strength on purpose: the two are one colour at two amounts, so a hovered cell reads as
    // lit-without-being-picked and cannot be mistaken for the selection, while a hovered selected cell
    // lifts past both. That ordering is the engine's own - its tabs mark a pointer by amount along the
    // axis their selection already brightens on, not by a second colour.
    private static final float ACCENT_HOVER_ALPHA = 0.15f;

    /**
     * The wash a panel's own controls take under the pointer: the accent's base step, the shade every
     * control on that panel already washes and strokes with, at the lesser amount a hover is worth.
     *
     * <p>The base step rather than the bright one because this is a surface and not a mark: bright is
     * reserved for what must read <em>against</em> the base - a tick, a lit label - and a wash laid under a
     * control's chrome is the one thing on it that is neither.
     *
     * @param accentColours the accent the panel's controls are ruled in
     * @return the hovered-cell wash for that accent
     */
    public static ControlHoverWash createAccentHoverWash(AccentColours accentColours) {
        return new ControlHoverWash(accentColours.base(), ACCENT_HOVER_ALPHA);
    }

    /**
     * The paint a cell washes with at a given point of its hover, faded by the panel's opacity. Zero at
     * either end - an untouched cell or a fully faded panel - is a hidden paint, which the fill primitives
     * skip, so a resting strip emits nothing rather than compositing a run the blend would discard.
     *
     * @param hoverFraction how far the cell has travelled onto its hovered look, 0 fully off and 1 fully on
     * @param opacity       the panel's overall alpha, 0..1
     * @return the wash to fill that cell with
     */
    public UiElementPaint resolvePaintAtHoverFraction(float hoverFraction, float opacity) {
        return new UiElementPaint(colour, opacity * fullHoverAlpha * hoverFraction);
    }
}
