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
}
