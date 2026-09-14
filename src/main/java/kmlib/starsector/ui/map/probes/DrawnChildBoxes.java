package kmlib.starsector.ui.map.probes;

import kmlib.math.geometry.Rectangle;

import java.util.List;

/**
 * A drawn direct child of a map tab, read one level deeper than its own box: where the layout put
 * it, and where it put the things it draws inside it.
 *
 * <p>Both halves are carried because a widget's box is not always what the player sees of it. A
 * strip laid across the map to hold buttons is as wide as the map, while what is drawn on it - and
 * what the cursor is ever aimed at - is the buttons. A rule handed only boxes cannot tell the two
 * apart, and a rule handed only the children would lose every chrome piece that draws itself and
 * holds nothing.
 *
 * <p>One level down and no further. A control's own parts are the control: descending past a button
 * would replace it with its label and reopen the same gap one level in, around the padding the
 * player is still aiming at.
 *
 * <p>A plain carrier for one measure, not a value held across frames - {@link MapSurfaceArea} is
 * what outlives the walk, and it takes its own copy of whatever it is built from.
 *
 * @param box             the child's own box in UI coordinates
 * @param drawnChildBoxes the boxes of the drawn children it holds, empty when it holds none
 */
record DrawnChildBoxes(
    Rectangle box,
    List<Rectangle> drawnChildBoxes) {
}
