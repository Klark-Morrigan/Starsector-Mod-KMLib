package kmlib.starsector.ui.widgets.tabs;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.widgets.tabs.style.TabChrome;

/**
 * The geometry of a raised-button tab row: where each button stands inside the tab box the row was laid
 * out with. Substrate-independent - it computes boxes and renders nothing - so the raw-GL paint in
 * {@link kmlib.starsector.ui.render.gl.tabs.RaisedButtonTabStripRenderer} and any other surface place a button
 * the same way.
 *
 * <p>It lays no tabs of its own. A row is laid out once, by {@link VanillaTabStrip}, and that layout is
 * what the panel hit-tests against; this chrome only stands its buttons inside those boxes. Deriving a
 * second set of tab widths here is exactly the drift the arrangement exists to prevent - a button drawn
 * where nothing is clickable, or a click landing beside the button that answered it.
 *
 * <p>Buttons stand apart where a {@link TabChrome#STRIP} row's tabs abut, which is the whole visible
 * difference between the two chromes at rest. The channel comes out of the laid tab rather than being added
 * to it, so the row spans the width the layout measured and a chrome swap moves no tab.
 *
 * <p>The channel is taken from one side only - a tab gives up the room for the channel on its left, and the
 * leading tab gives up nothing - so the row matches the engine's own: its leftmost button stands at the
 * row's left edge, its last ends where the row ends, and each pair is parted by one channel rather than by
 * two half-channels. A button fills its band top to bottom for the same reason: a vanilla button is sized to
 * the row it sits in, so the height a host tuned for its header is the height its buttons wear.
 *
 * <p>Where a button meets a line the panel already draws - its frame down the left of the row, the body's
 * own border under it - the button reaches one hairline past the tab and lays its border on that line
 * instead of beside it. Two hairlines meeting edge to edge read as a two-pixel rule, which is the one place
 * a row copied from vanilla's would come out heavier than the row it was copied from.
 */
public final class RaisedButtonTabStrip {

    /**
     * The weight of a button's own outline, matching the hairline the engine's own buttons are framed in.
     *
     * <p>Public because it is a fact about the button's box and not only about its paint: how far a button
     * reaches past its tab to lay a border on the panel's own line is exactly this width, so a chrome
     * stroking a heavier outline than the geometry reached for would sit its border half on that line.
     */
    public static final float FRAME_THICKNESS = 1f;

    // The gap between two neighbouring buttons, matching the 3px the engine's own intel-screen button row
    // keeps between its buttons (com.fs.starfarer.coreui.A.R). Taken from inside the laid tab because the
    // row's total width is the layout's to decide - a button row and a strip occupy the same band either
    // way.
    private static final float NEIGHBOUR_CHANNEL = 3f;

    // The row's leading tab, which has no neighbour on its left to be parted from and so gives up nothing.
    private static final int LEADING_TAB_INDEX = 0;

    // How far above the button's own middle its label sits. The pixel face this chrome is lettered in draws
    // no descenders and leaves the room for them empty at the foot of its line box, so text centred by that
    // box reads low by about the space it is not using. A whole pixel, never a fraction of one: a face of
    // hard-edged pixels drawn on a half-pixel row is split across two rows of screen, which costs it both
    // its crispness and its weight.
    private static final float LABEL_LIFT = 1f;

    private RaisedButtonTabStrip() {
    }

    /**
     * The screen the whole row covers, given the band it was laid into: the band, plus the hairline its
     * buttons reach past it with on the left and below. A caller clipping the row to its band alone would
     * crop those two borders away, which reads as buttons open on two sides rather than as anything having
     * gone wrong.
     *
     * @param band the row's laid-out band, in UI coordinates (origin bottom-left)
     * @return the region this chrome paints into, in the same coordinates
     */
    public static Rectangle computeRowFootprint(Rectangle band) {
        return new Rectangle(
            band.x() - FRAME_THICKNESS,
            band.y() - FRAME_THICKNESS,
            band.width() + FRAME_THICKNESS,
            band.height() + FRAME_THICKNESS);
    }

    /**
     * The box a button's label centres in: its own, raised so the text sits on the button's optical middle
     * rather than on the middle of the line box its face reserves. The label is placed from this and the
     * chrome from the button box itself, so a face wanting a different correction moves the text and
     * nothing else.
     *
     * @param buttonBox the button's box, as {@link #computeButtonBox} gives it
     * @return the box to centre the label in, in the same coordinates
     */
    public static Rectangle computeLabelBox(Rectangle buttonBox) {
        return new Rectangle(
            buttonBox.x(),
            buttonBox.y() + LABEL_LIFT,
            buttonBox.width(),
            buttonBox.height());
    }

    /**
     * The button's footprint about one laid-out tab: the tab's band height, and its width less the channel
     * parting it from the button to its left, then reached one hairline outward on each edge that meets a
     * line the panel draws - the bottom always, the left for the leading button alone. Width floors at zero,
     * so a tab too narrow to give up its own channel collapses to nothing rather than inverting into a
     * button drawn back-to-front across its neighbour.
     *
     * @param tabBounds the tab's laid-out box, in UI coordinates (origin bottom-left)
     * @param rowIndex  the tab's position in the row; the leading tab takes no channel and is the one that
     *                  reaches out over the panel's left frame
     * @return the button's box, in the same coordinates
     */
    public static Rectangle computeButtonBox(Rectangle tabBounds, int rowIndex) {

        var isLeadingTab = rowIndex == LEADING_TAB_INDEX;
        var channel = isLeadingTab ? 0f : NEIGHBOUR_CHANNEL;
        var leftReach = isLeadingTab ? FRAME_THICKNESS : 0f;
        var width = Math.max(0f, tabBounds.width() - channel + leftReach);

        // Placed from the tab's right edge rather than its left, so the collapsed case keeps the whole
        // narrowed tab as channel instead of pushing a zero-width button past the neighbour it parts from.
        return new Rectangle(
            tabBounds.x() + tabBounds.width() - width,
            tabBounds.y() - FRAME_THICKNESS,
            width,
            tabBounds.height() + FRAME_THICKNESS);
    }
}
