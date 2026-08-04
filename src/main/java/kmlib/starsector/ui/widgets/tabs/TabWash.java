package kmlib.starsector.ui.widgets.tabs;

import kmlib.colour.Colours;

import java.awt.Color;

/**
 * A lift over whatever a tab already wears: a colour to move toward and how far to move. Relative rather
 * than absolute, so one wash serves every {@link TabBaseState} - a hovered selected tab and a hovered
 * unselected tab are each their own base brightened, which is what a single "hovered" fill could not
 * express.
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
     * A tab with nothing happening to it: no movement toward anything, so the base look stands as it is.
     * The target is arbitrary and never read, since a strength of zero blends nowhere - a plain white
     * stands in rather than a live palette colour, so the constant costs no engine call to hold.
     */
    public static final TabWash NONE = new TabWash(Color.WHITE, 0f);

    /**
     * Clamps the strength into the unit range, so a composited value that overshoots settles at the
     * target rather than blending past it into a colour neither side named.
     */
    public TabWash {
        strength = Math.max(0f, Math.min(1f, strength));
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
