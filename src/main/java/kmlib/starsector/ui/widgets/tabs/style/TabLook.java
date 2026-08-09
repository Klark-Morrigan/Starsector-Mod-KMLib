package kmlib.starsector.ui.widgets.tabs.style;

import kmlib.colour.Colours;
import kmlib.math.ranges.Ranges;

import java.awt.Color;
import java.util.function.UnaryOperator;

/**
 * The whole settled look of a tab in one {@link TabLookState}: the fill its surface takes and the colour
 * its label reads in. Absolute rather than relative, so a look is a shade a tab arrives at rather than a
 * distance it travels - which is what lets the resting and the selected tab meet at one hovered shade.
 *
 * <p>The two travel together rather than as separate palette roles, so a state cannot be given a fill
 * without the label meant to sit legibly on it.
 *
 * @param fill  the fill covering the tab; how solid it is is the fill's own, so a chrome standing its
 *              tabs on a backing states an unpainted interior as a fill at zero alpha rather than as a
 *              state its paint pass skips
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
     * <p>How solid each shade is travels with it. A chrome whose resting state is an unpainted interior
     * names that as a fill at zero alpha, so a fade that carried the starting alpha across would leave the
     * whole travel invisible; a chrome whose looks are all opaque surfaces interpolates 255 to 255 and is
     * untouched by the same rule.
     *
     * @param targetLook the look being travelled toward
     * @param fraction   how far along the way, 0 (this look) to 1 (the target); confined to that range so a
     *                   composed value that overshoots settles on the target rather than blending past it
     * @return the look at that point between the two
     */
    public TabLook computeBlendedLook(TabLook targetLook, float fraction) {

        var travelled = Ranges.clampToUnit(fraction);

        // The one case where the two surfaces do not take the same operation: each travels toward its own
        // counterpart in the target, so the pair is built here rather than through the mapping below.
        return new TabLook(
            Colours.blendTowards(fill, targetLook.fill(), travelled),
            Colours.blendTowards(label, targetLook.label(), travelled));
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
        return computeMappedLook(wash::computeWashedColour);
    }

    /**
     * This look with light added to it - fill and label alike, so a lit tab reads as one piece. Light
     * added rather than a shade travelled toward, which is a different motion and not a differently
     * weighted one: adding reproduces a glow pass drawn over a surface, where travelling walks the
     * surface's own colour toward another and can only ever arrive at it.
     *
     * <p>The light is a surface of its own, so a look naming an unpainted fill is painted by whatever
     * light lands on it and goes back to unpainted as that light comes off. A chrome resting on an
     * unpainted interior would otherwise answer with its label alone - the surface brightening in channels
     * nobody can see, because it is still drawn at nothing.
     *
     * @param glowColour the light being added; its own alpha scales what it contributes and is how solid
     *                   it makes what it lands on
     * @param glowAmount how much of it to add, 0 adding nothing and 1 adding it in full
     * @return the look with that much light on it
     */
    public TabLook computeGlowingLook(Color glowColour, float glowAmount) {

        var added = Ranges.clampToUnit(glowAmount);

        return computeMappedLook(colour -> Colours.addLight(colour, glowColour, added));
    }

    // Both of a look's surfaces put through one operation. The invariant every transform here holds -
    // fill and label move alike, so a lifted tab reads as one piece rather than as a fill sliding out
    // from under its text - written once, so a new transform inherits it rather than restating it.
    private TabLook computeMappedLook(UnaryOperator<Color> operation) {
        return new TabLook(
            operation.apply(fill),
            operation.apply(label));
    }
}
