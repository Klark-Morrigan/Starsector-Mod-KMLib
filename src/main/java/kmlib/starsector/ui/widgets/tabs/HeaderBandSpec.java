package kmlib.starsector.ui.widgets.tabs;

import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.widgets.tabs.style.TabStyle;

/**
 * The band a tab panel flies, as a caller asks for one: how the row looks, the tabs standing in it, and
 * the panel's own button after them.
 *
 * <p>They travel as one value because a layout cannot use any of them alone. The style states how tall
 * the band stands and how wide a tab is, the tabs are what is measured against it, and the button stands
 * in the same band at the same height - so a caller handing over two of the three has described nothing
 * layable, and three loose arguments in a row are three a caller can reorder into a row that still
 * compiles.
 *
 * <p>The two styles here are not a mistake. The row's is this record's; the button carries its own inside
 * {@link BandButtonSpec}, because a button is as wide as the one thing it shows while a tab row is as wide
 * as the longest label it will ever carry. What they must share is the band, and that is not either of
 * theirs to state - the layout pins both to the band the panel was given.
 *
 * @param style      the chrome, box, palette and face the tabs are measured and painted at, and the band
 *                   height the whole row stands in
 * @param tabs       the tabs control - the labels, their per-tab shortcuts, the lit tab and what a click
 *                   on one does
 * @param bandButton the panel's own button standing after the last tab, or null for a band of tabs alone
 */
public record HeaderBandSpec(
    TabStyle style,
    ControlSpec.Tabs tabs,
    BandButtonSpec bandButton) {
}
