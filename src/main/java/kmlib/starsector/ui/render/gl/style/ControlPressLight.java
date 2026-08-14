package kmlib.starsector.ui.render.gl.style;

import kmlib.starsector.ui.colour.AccentColours;
import kmlib.starsector.ui.render.gl.UiElementPaint;

import java.awt.Color;

/**
 * What a body control shows for the press it just answered, as the light its cell takes: a colour and the
 * alpha that colour reaches at a lift's peak. It sits here beside the {@link ControlHoverWash} for the same
 * reason that does - the body widgets carry no paint description of their own, so the treatments laid over
 * their chrome are named where the accents they lift over are, rather than as floats inside a renderer
 * where retuning one would mean hunting through draw calls.
 *
 * <p>A channel of its own, added over the wash rather than blended toward it. A press is always made on a
 * cell the pointer is already holding fully lit, so a lift that only travelled as far as the hovered shade
 * would show nothing on every press a player actually makes - which is the difference from a bound key's
 * blink, struck while the pointer is somewhere else entirely and so free to share the hover's channel. The
 * cost of a press being visible at all is the second per-cell paint.
 *
 * <p>Drawn over the cell's washes and under its chrome marks - the frame, the tick, the icons, the label -
 * so a press reads as the cell lifting rather than as its marks changing. That order is what a press
 * dimming a control instead of lifting it would be the signal of.
 *
 * <p>The pace is not here, as it is not on the wash: every motion a panel makes travels on one shared pair
 * of durations, and what the travel arrives at is the widget's own.
 *
 * @param colour         the shade a pressed cell is lit in
 * @param fullPressAlpha the alpha that light reaches at the lift's peak, before the panel's own opacity
 *                       fades it - the one number to retune if a press flashes too hard or reads too
 *                       faintly over the wash beneath it
 */
public record ControlPressLight(
    Color colour,
    float fullPressAlpha) {

    // How strongly a press lights a cell at its peak, as a fraction of the panel's opacity. Above the
    // hover wash's own strength because it has to read over it: the cell it lands on is by definition
    // already washed, so a press at or below that amount would be a lift into what the player is already
    // looking at. It is the depth the strip above lifts a pressed tab by, which is what keeps one panel's
    // two halves answering a click at one apparent strength.
    private static final float ACCENT_PRESS_ALPHA = 0.25f;

    /**
     * The light a panel's own controls take when pressed: the accent's bright step, at the amount a press
     * is worth.
     *
     * <p>The bright step rather than the base one the wash uses, because this is the one cell treatment
     * that must read <em>against</em> a surface the panel has already laid - the hovered wash under it -
     * rather than against bare chrome. Same accent either way, so a press is still more of what the control
     * already wears.
     *
     * @param accentColours the accent the panel's controls are ruled in
     * @return the pressed-cell light for that accent
     */
    public static ControlPressLight createAccentPressLight(AccentColours accentColours) {
        return new ControlPressLight(accentColours.bright(), ACCENT_PRESS_ALPHA);
    }

    /**
     * The paint a cell lights with at a given point of its press lift, faded by the panel's opacity. Zero at
     * either end - a cell with no press running on it or a fully faded panel - is a hidden paint, which the
     * fill primitives skip, so a strip nobody is pressing emits nothing rather than compositing a run the
     * blend would discard.
     *
     * @param pressFraction how far through its lift the press stands, 0 with none running and 1 at the peak
     * @param opacity       the panel's overall alpha, 0..1
     * @return the light to fill that cell with
     */
    public UiElementPaint resolvePaintAtPressFraction(float pressFraction, float opacity) {
        return new UiElementPaint(colour, opacity * fullPressAlpha * pressFraction);
    }
}
