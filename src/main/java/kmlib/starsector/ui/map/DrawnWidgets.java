package kmlib.starsector.ui.map;

import com.fs.starfarer.api.ui.UIComponentAPI;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.layout.VanillaPositions;

/**
 * What a core-UI component occupies on screen, and whether it is drawn there at all.
 *
 * <p>Held apart from the probes that walk the tree because more than one of them asks these two
 * questions, and they have to give the same answer. A probe that only describes the tree and a rule
 * that suppresses an overlay from it must agree on which components count as present: were their
 * opacity thresholds to drift apart, the description would name chrome the rule stands aside for -
 * or worse, not name chrome the rule does - and the discrepancy would only be visible in play.
 */
final class DrawnWidgets {

    // Below this a component is drawn to nothing, so the cursor is not meaningfully "over" it and
    // treating it as present would suggest chrome where the player sees none. A tab the player has
    // switched away from keeps its box and its place in the tree while fading out, which is the
    // case this threshold exists for.
    static final float MIN_VISIBLE_OPACITY = 0.01f;

    private DrawnWidgets() {
    }

    /**
     * @param widget the component to test
     * @return whether the component is drawn solidly enough to count as present on screen
     */
    static boolean isWidgetDrawn(UIComponentAPI widget) {
        return widget.getOpacity() >= MIN_VISIBLE_OPACITY;
    }

    /**
     * A component's drawn box in UI coordinates.
     *
     * <p>Read through the published {@link UIComponentAPI}, so the box is the game's own answer
     * rather than anything inferred from the walk that reached the component.
     *
     * @param widget the component to measure
     * @return its box, or null when the layout never positioned it - it then occupies nothing
     */
    static Rectangle resolveBoxOf(UIComponentAPI widget) {
        var position = widget.getPosition();
        return position == null ? null : VanillaPositions.toRectangle(position);
    }

    /**
     * A component's box, but only while it is something the player can see there.
     *
     * <p>The two questions above are always asked together wherever a walk sifts a raw children
     * list, so they are answered together here. An entry that is not a component at all, one faded
     * to nothing and one the layout never positioned all mean the same thing to a caller sizing up
     * what is on screen - nothing to measure - and a caller that had to distinguish them would be
     * writing the same three-way test at every level it descends.
     *
     * @param component an entry off a children list, which the list does not promise is a component
     * @return its drawn box, or null when it is not a drawn, positioned component
     */
    static Rectangle resolveDrawnBoxOf(Object component) {
        if (!(component instanceof UIComponentAPI widget) || !isWidgetDrawn(widget)) {
            return null;
        }
        return resolveBoxOf(widget);
    }
}
