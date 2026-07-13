package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;

import java.util.List;

/**
 * One laid-out panel: the reusable {@link TabPanelPlacement} carrying the panel's outer box, tab row,
 * and framed body rectangle, paired with the {@link Control}s laid inside that body. All rectangles are
 * in UI coordinates, so a renderer draws them and an input listener hit-tests them without conversion. It
 * is the generic placement of a framed tab strip over a control-strip body - a panel anywhere on screen,
 * not tied to a side or a bar - so a host reaches the frame through {@link #panel()} and the controls
 * through {@link #bodyControls()}. The {@link #box()}, {@link #tabs()}, and {@link #body()} shortcuts read
 * straight off the panel, so a tabless or bodyless panel (empty tabs, an empty {@code bodyControls} and a
 * zero-size body) reserves no dead click zone.
 *
 * <p>When the body is capped (its natural height would run past a bottom limit), its one scrolling
 * control gives up the difference and scrolls within {@link #flexViewport()}: a renderer clips that
 * control's draw to the viewport and an input listener confines its clicks to it, while {@link
 * #scrollOffset()} (already baked into the scrolling control's laid-out bounds) and {@link
 * #scrollOverflow()} drive the scrollbar. An uncapped body carries a zero viewport and zero overflow, so
 * the whole strip pins and no scrollbar shows.
 *
 * @param panel          the frame, tab row, and body rectangle from the reusable panel
 * @param bodyControls   the controls laid inside the body, top to bottom (empty for no body)
 * @param flexViewport   the clip rectangle for the scrolling control, zero-size when nothing scrolls
 * @param scrollOffset   the applied scroll offset in pixels, baked into the scrolling control's bounds
 * @param scrollOverflow how far the scrolling control overruns its viewport, zero when it fits
 */
public record PanelPlacement(TabPanelPlacement panel, List<Control> bodyControls,
        Rectangle flexViewport, float scrollOffset, float scrollOverflow) {
    /**
     * @return the panel's full footprint, border included
     */
    public Rectangle box() {
        return panel.box();
    }

    /**
     * @return the laid-out tabs, in row order, so a hit index maps back to its tab
     */
    public List<VanillaTab> tabs() {
        return panel.tabs();
    }

    /**
     * @return the framed body rectangle, zero-size when the active tab opens no body
     */
    public Rectangle body() {
        return panel.body();
    }

    /**
     * @return whether the scrolling control overruns its viewport, so the renderer draws a scrollbar and
     *         the input listener scrolls on a wheel event
     */
    public boolean isScrollbarNeeded() {
        return scrollOverflow > 0f;
    }

    /**
     * Projects this panel's scrolling control into a {@link ScrollRegion} - the body as the container,
     * the flex viewport, and the scroll offset/overflow - so a {@link Scrollbar} sizes and hit-tests
     * itself from the placement without the panel owning any scrollbar geometry itself.
     *
     * @return the scroll region for this panel's scrolling control
     */
    public ScrollRegion toScrollRegion() {
        return new ScrollRegion(body(), flexViewport(), scrollOffset(), scrollOverflow());
    }
}
