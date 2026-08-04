package kmlib.starsector.ui.widgets.tabs;

import kmlib.colour.Colours;
import kmlib.math.ranges.Ranges;

import java.awt.Color;

/**
 * The whole settled look of a tab in one {@link TabLookState}: the fill its surface takes and the colour
 * its label reads in. Absolute rather than relative, so a look is a shade a tab arrives at rather than a
 * distance it travels - which is what lets the resting and the selected tab meet at one hovered shade.
 *
 * <p>The two travel together rather than as separate palette roles, so a state cannot be given a fill
 * without the label meant to sit legibly on it.
 *
 * @param fill  the solid fill covering the tab; the tab's own surface, not a tint over a backdrop
 * @param label the colour the tab's label text reads in against that fill
 */
public record TabLook(
    Color fill,
    Color label) {

    /**
     * This look part of the way to another one - fill and label moved alike, so a tab travelling between two
     * looks reads as one piece rather than as a fill sliding out from under its text. Both ends are absolute
     * looks, so a fraction places the tab between two named shades rather than lifting it by a depth its own
     * starting colour would scale differently.
     *
     * <p>Held here rather than at each consumer because the ends of a fade are looks whichever animation
     * drives it, so a hover, a blink, and any later look-to-look travel move a tab the same way.
     *
     * @param targetLook the look being travelled toward
     * @param fraction   how far along the way, 0 (this look) to 1 (the target); confined to that range so a
     *                   composed value that overshoots settles on the target rather than blending past it
     * @return the look at that point between the two
     */
    public TabLook computeBlendedLook(TabLook targetLook, float fraction) {

        var travelled = Ranges.clampToUnit(fraction);

        return new TabLook(
            Colours.blendRgbTowards(fill, targetLook.fill(), travelled),
            Colours.blendRgbTowards(label, targetLook.label(), travelled));
    }

    /**
     * This look brightened by a momentary {@link TabWash} - fill and label moved alike, so a lifted tab
     * reads as one piece rather than as a fill sliding out from under its text. A wash of no strength
     * returns the same shades, so a tab with nothing happening to it needs no separate path.
     *
     * @param wash the lift the tab is currently carrying
     * @return the look as painted under that lift
     */
    public TabLook computeWashedLook(TabWash wash) {
        return new TabLook(
            wash.computeWashedColour(fill),
            wash.computeWashedColour(label));
    }
}
