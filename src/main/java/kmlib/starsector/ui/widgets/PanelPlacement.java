package kmlib.starsector.ui.widgets;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.widgets.scroll.ScrollRegion;
import kmlib.starsector.ui.widgets.scroll.ScrollbarThickness;

import java.util.List;

/**
 * One laid-out headerless panel: a bordered {@code box} framing an inset control-strip {@code body},
 * with the {@link Control}s laid inside that body. All rectangles are in UI coordinates, so a renderer
 * draws them and an input listener hit-tests them without conversion. It is the generic placement of a
 * bordered box over a control-strip body - a panel anywhere on screen, not tied to a side or a bar - so
 * a host reaches the frame through {@link #box()} and the controls through {@link #bodyControls()}. An
 * empty {@code bodyControls} and a zero-size {@code body} reserve no dead click zone.
 *
 * <p>A {@link kmlib.starsector.ui.widgets.tabs.TabPanelPlacement} builds on this: it stands a tabs header
 * on top of the same box, so this box frames the body the row selects and the row's own footprint is the
 * tab panel's to carry. The record stays neutral to that - it only carries rectangles.
 *
 * <p>When the body is capped (its natural height would run past a bottom limit), its one scrolling
 * control gives up the difference and scrolls within {@link #flexViewport()}: a renderer clips that
 * control's draw to the viewport and an input listener confines its clicks to it, while {@link
 * #scrollOffset()} (already baked into the scrolling control's laid-out bounds) and {@link
 * #scrollOverflow()} drive the scrollbar. An uncapped body carries a zero viewport and zero overflow, so
 * the whole strip pins and no scrollbar shows.
 *
 * <p>How thick that bar draws rides here beside the scroll geometry rather than reaching the paint pass as
 * a render parameter, because two passes spend it: one draws the track and thumb, the other hit-tests the
 * thumb to tell a grab from a press on bare track. A thickness known to only one of them would leave a fat
 * drawn thumb that grabs along a thin strip of itself.
 *
 * @param box                the panel's full footprint, border included
 * @param body               the inset control-strip region framed by the box, zero-size when there is no
 *                           body
 * @param bodyControls       the controls laid inside the body, top to bottom (empty for no body)
 * @param flexViewport       the clip rectangle for the scrolling control, zero-size when nothing scrolls
 * @param scrollOffset       the applied scroll offset in pixels, baked into the scrolling control's bounds
 * @param scrollOverflow     how far the scrolling control overruns its viewport, zero when it fits
 * @param scrollbarThickness how wide the scrollbar's track and thumb draw over this panel's body
 */
public record PanelPlacement(
    Rectangle box,
    Rectangle body,
    List<Control> bodyControls,
    Rectangle flexViewport,
    float scrollOffset,
    float scrollOverflow,
    ScrollbarThickness scrollbarThickness) {

    /**
     * Whether there is a bar on screen: the list has somewhere to scroll AND the host asked for a bar wide
     * enough to draw. A thickness of nothing takes the track and thumb away rather than drawing them at no
     * width, so nothing is painted there and nothing there is grabbable.
     *
     * <p>Stated here rather than at each pass, because the two that spend it - the one drawing the bar and
     * the one hit-testing the thumb - must agree: a bar drawn by one reading and grabbed by another is a
     * thumb that does not answer the press landing on it, or a column claiming presses over nothing.
     *
     * @return whether the bar is both due and drawn
     */
    public boolean isScrollbarDrawn() {
        return isScrollbarNeeded() && scrollbarThickness.isTrackDrawn();
    }

    /**
     * Whether the scrolling control overruns its viewport, and so has somewhere to be scrolled to. It is
     * the wheel's question: a wheel is how a player with no bar moves the list, so it answers to the list
     * overrunning alone and never to how the bar is drawn.
     *
     * @return whether the scrolling control overruns its viewport
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
