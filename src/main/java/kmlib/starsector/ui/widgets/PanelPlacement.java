package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.widgets.scroll.ScrollRegion;

import java.util.List;

/**
 * One laid-out headerless panel: a bordered {@code box} framing an inset control-strip {@code body},
 * with the {@link Control}s laid inside that body. All rectangles are in UI coordinates, so a renderer
 * draws them and an input listener hit-tests them without conversion. It is the generic placement of a
 * bordered box over a control-strip body - a panel anywhere on screen, not tied to a side or a bar - so
 * a host reaches the frame through {@link #box()} and the controls through {@link #bodyControls()}. An
 * empty {@code bodyControls} and a zero-size {@code body} reserve no dead click zone.
 *
 * <p>A {@link kmlib.starsector.ui.widgets.tabs.TabPanelPlacement} builds on this: it overlays a tabs
 * header on the top band of the same
 * box, so its nested {@code box} spans the whole footprint (header band included) while its {@code body}
 * is the strip region beneath the header. The record stays neutral to that - it only carries rectangles.
 *
 * <p>When the body is capped (its natural height would run past a bottom limit), its one scrolling
 * control gives up the difference and scrolls within {@link #flexViewport()}: a renderer clips that
 * control's draw to the viewport and an input listener confines its clicks to it, while {@link
 * #scrollOffset()} (already baked into the scrolling control's laid-out bounds) and {@link
 * #scrollOverflow()} drive the scrollbar. An uncapped body carries a zero viewport and zero overflow, so
 * the whole strip pins and no scrollbar shows.
 *
 * @param box            the panel's full footprint, border included
 * @param body           the inset control-strip region framed by the box, zero-size when there is no body
 * @param bodyControls   the controls laid inside the body, top to bottom (empty for no body)
 * @param flexViewport   the clip rectangle for the scrolling control, zero-size when nothing scrolls
 * @param scrollOffset   the applied scroll offset in pixels, baked into the scrolling control's bounds
 * @param scrollOverflow how far the scrolling control overruns its viewport, zero when it fits
 */
public record PanelPlacement(
    Rectangle box,
    Rectangle body,
    List<Control> bodyControls,
    Rectangle flexViewport,
    float scrollOffset,
    float scrollOverflow) {

    /**
     * @return whether the scrolling control overruns its viewport, so the renderer draws a scrollbar and
     *         the input listener scrolls on a wheel event
     */
    public boolean isScrollbarNeeded() {
        return scrollOverflow > 0f;
    }

    /**
     * Projects this panel's scrolling control into a {@link ScrollRegion} - the body as the container,
     * the flex viewport, and the scroll offset/overflow - so a {@link
     * kmlib.starsector.ui.widgets.scroll.Scrollbar} sizes and hit-tests
     * itself from the placement without the panel owning any scrollbar geometry itself.
     *
     * @return the scroll region for this panel's scrolling control
     */
    public ScrollRegion toScrollRegion() {
        return new ScrollRegion(
            body(),
            flexViewport(),
            scrollOffset(),
            scrollOverflow());
    }
}
