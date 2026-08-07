package kmlib.starsector.ui.widgets.tabs;

import kmlib.math.geometry.Rectangle;

/**
 * The geometry of a raised-button tab row: where each button stands inside the tab box the row was laid
 * out with. Substrate-independent - it computes boxes and renders nothing - so the raw-GL paint in
 * {@link kmlib.starsector.ui.render.gl.RaisedButtonTabStripRenderer} and any other surface place a button
 * the same way.
 *
 * <p>It lays no tabs of its own. A row is laid out once, by {@link VanillaTabStrip}, and that layout is
 * what the panel hit-tests against; this chrome only stands its buttons inside those boxes. Deriving a
 * second set of tab widths here is exactly the drift the arrangement exists to prevent - a button drawn
 * where nothing is clickable, or a click landing beside the button that answered it.
 *
 * <p>Buttons stand apart where a {@link TabChrome#STRIP} row's tabs abut, which is the whole visible
 * difference between the two chromes at rest. The gap comes out of the laid tab rather than being added
 * to it, so the row spans the width the layout measured and a chrome swap moves no tab.
 */
public final class RaisedButtonTabStrip {

    // Half the gap between neighbouring buttons: each of the two gives up this much, so the visible
    // channel between them is twice it. Taken from inside the laid tab because the row's total width is
    // the layout's to decide - a button row and a strip occupy the same band either way.
    private static final float SIDE_GUTTER = 2f;

    // The clearance above and below a button inside its band, so a button reads as standing in the header
    // rather than as filling it. Both eyeballed against the intel screen's own buttons rather than
    // derived - nothing in the laid geometry knows what a raised button should look like.
    private static final float VERTICAL_INSET = 1f;

    private RaisedButtonTabStrip() {
    }

    /**
     * The button's footprint inside one laid-out tab: the tab's box less a gutter to either side and a
     * clearance above and below, held on the tab's own centre. Extents floor at zero, so a tab too narrow
     * or too short to hold its own insets collapses to nothing at its centre rather than inverting into a
     * button drawn back-to-front across its neighbours.
     *
     * @param tabBounds the tab's laid-out box, in UI coordinates (origin bottom-left)
     * @return the button's box, in the same coordinates
     */
    public static Rectangle computeButtonBox(Rectangle tabBounds) {

        var width = Math.max(0f, tabBounds.width() - 2f * SIDE_GUTTER);
        var height = Math.max(0f, tabBounds.height() - 2f * VERTICAL_INSET);

        return new Rectangle(
            tabBounds.computeCenterX() - width / 2f,
            tabBounds.computeCenterY() - height / 2f,
            width,
            height);
    }
}
