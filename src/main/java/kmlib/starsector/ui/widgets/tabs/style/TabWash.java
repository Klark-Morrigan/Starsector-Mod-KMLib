package kmlib.starsector.ui.widgets.tabs.style;

import kmlib.colour.Colours;

import java.awt.Color;

/**
 * A momentary lift over whatever a tab already wears: a colour to move toward and how far to move.
 * Relative rather than absolute, so one wash serves every {@link TabLookState} - a click on a resting tab
 * and a click on the one under the pointer each brighten from where that tab already was, which is what a
 * single pulse colour could not express.
 *
 * <p>A wash lifts the tab's whole look, fill and label alike, so a lifted tab brightens as one piece
 * rather than as a fill sliding out from under its text. The bound key is the exception: its colour is
 * {@link HotkeyStyle}'s and reads the same whatever is happening to its tab, so it is left where it is
 * while the delimiters around it, being label text, move with the label.
 *
 * <p>Strength is a fraction of the way to the target, clamped into the unit range: the value is composed
 * from live interaction (a held hover, a decaying pulse) and an out-of-range one would otherwise drive
 * the blend past the target and back out the far side.
 *
 * @param target   the colour the tab's fill and label move toward
 * @param strength how far they move, 0 (untouched) to 1 (fully the target); clamped into that range
 */
public record TabWash(
    Color target,
    float strength) {

    /**
     * Clamps the strength into the unit range, so a composited value that overshoots settles at the
     * target rather than blending past it into a colour neither side named.
     */
    public TabWash {
        strength = Math.max(0f, Math.min(1f, strength));
    }

    /**
     * This wash part of the way to its full depth - the same target, reached by that fraction of the
     * strength. It is what turns a palette's peak lift into the lift a tab is carrying right now: the peak
     * says how bright a click reads at its brightest, the fraction says how far through the click's cycle
     * the tab has got, and only the two together describe a frame.
     *
     * <p>The target is untouched, so a decaying pulse fades back along the one colour it lifted toward
     * rather than sliding through shades no palette named.
     *
     * @param fraction how far up the wash's own depth to go, 0 (no lift) to 1 (the full depth); an
     *                 out-of-range value is confined by the strength clamp above
     * @return the wash at that depth
     */
    public TabWash computeScaledWash(float fraction) {
        return new TabWash(target, strength * fraction);
    }

    /**
     * Moves one of the tab's colours toward this wash's target by its strength, keeping the colour's own
     * alpha - the rule the fill and the label are both lifted by, held here so every renderer drawing a
     * washed tab lifts them the same way.
     *
     * @param baseColour the colour to lift - the base look's fill or its label
     * @return that colour moved toward the target, or the colour itself at zero strength
     */
    public Color computeWashedColour(Color baseColour) {
        return Colours.blendRgbTowards(baseColour, target, strength);
    }
}
