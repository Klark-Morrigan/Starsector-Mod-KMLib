package kmlib.starsector.ui.widgets.tabs;

import java.awt.Color;

/**
 * The whole resting look of a tab in one {@link TabBaseState}: the fill its surface takes and the colour
 * its label reads in. Both absolute, because a base state is what a tab is - it has nothing beneath it to
 * lift from, unlike the momentary {@link TabWash} that lifts it.
 *
 * <p>The two travel together rather than as separate palette roles, so a state cannot be given a fill
 * without the label meant to sit legibly on it.
 *
 * @param fill  the solid fill covering the tab; the tab's own surface, not a tint over a backdrop
 * @param label the colour the tab's label text reads in against that fill
 */
public record TabBaseLook(
    Color fill,
    Color label) {
}
