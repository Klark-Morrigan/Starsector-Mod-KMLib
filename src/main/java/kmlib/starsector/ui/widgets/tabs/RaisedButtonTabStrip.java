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
 * leading tab gives up nothing - so the row matches the engine's own: its leftmost button is flush with the
 * row's left edge, its last ends where the row ends, and each pair is parted by one channel rather than by
 * two half-channels. A button fills its band top to bottom for the same reason: a vanilla button is sized to
 * the row it sits in, so the height a host tuned for its header is the height its buttons wear.
 */
public final class RaisedButtonTabStrip {

    // The gap between two neighbouring buttons, matching the 3px the engine's own intel-screen button row
    // keeps between its buttons (com.fs.starfarer.coreui.A.R). Taken from inside the laid tab because the
    // row's total width is the layout's to decide - a button row and a strip occupy the same band either
    // way.
    private static final float NEIGHBOUR_CHANNEL = 3f;

    // The row's leading tab, which has no neighbour on its left to be parted from and so gives up nothing.
    private static final int LEADING_TAB_INDEX = 0;

    private RaisedButtonTabStrip() {
    }

    /**
     * The button's footprint inside one laid-out tab: the tab's full band height, and its width less the
     * channel parting it from the button to its left. Width floors at zero, so a tab too narrow to give up
     * its own channel collapses to nothing rather than inverting into a button drawn back-to-front across
     * its neighbour.
     *
     * @param tabBounds the tab's laid-out box, in UI coordinates (origin bottom-left)
     * @param rowIndex  the tab's position in the row; the leading tab takes no channel, so its button stands
     *                  flush with the row's left edge
     * @return the button's box, in the same coordinates
     */
    public static Rectangle computeButtonBox(Rectangle tabBounds, int rowIndex) {

        var channel = rowIndex == LEADING_TAB_INDEX ? 0f : NEIGHBOUR_CHANNEL;
        var width = Math.max(0f, tabBounds.width() - channel);

        // Placed from the tab's right edge rather than its left, so the collapsed case keeps the whole
        // narrowed tab as channel instead of pushing a zero-width button past the neighbour it parts from.
        return new Rectangle(
            tabBounds.x() + tabBounds.width() - width,
            tabBounds.y(),
            width,
            tabBounds.height());
    }
}
