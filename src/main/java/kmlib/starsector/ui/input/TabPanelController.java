package kmlib.starsector.ui.input;

import com.fs.starfarer.api.input.InputEventAPI;

import kmlib.starsector.ui.widgets.scroll.ScrollState;
import kmlib.starsector.ui.widgets.tabs.TabPanelPlacement;

/**
 * Drives one tab panel's pointer input: it routes a left press on a header tab to that tab's own action,
 * and delegates everything else - the body control hits, the scrollbar drag, the wheel scroll - to a
 * {@link PanelController} for the body. A tab panel is a panel plus a header, so its input is the panel's
 * input plus one control hit-test on top; the tab's action is baked into its spec (the host wires it), so
 * this controller stays agnostic to what selecting a tab does.
 *
 * <p>One controller per panel, since the body controller holds that panel's scroll and drag state across
 * frames; a host creates it, reads its {@link #getScrollState()} when it lays the panel out, and feeds it
 * pointer events. The header never scrolls, so it is never clipped; only the body carries the scroll and
 * drag state, which is exactly what the delegated {@link PanelController} owns.
 */
public final class TabPanelController {
    // The body's controller, owning the scroll and drag state; this routes everything but a header-tab
    // press to it, so the panel's scroll and drag behaviour is the plain panel's, unchanged.
    private final PanelController bodyController = new PanelController();

    /**
     * @return the body's scroll position, for the layout to read (the requested offset) and settle
     *         (clamp to the overflow) each frame
     */
    public ScrollState getScrollState() {
        return bodyController.getScrollState();
    }

    /**
     * Ends any in-progress body scrollbar drag, for the host to call when the panel stops showing so a
     * drag left dangling cannot hijack the next session.
     */
    public void cancelDrag() {
        bodyController.cancelDrag();
    }

    /**
     * Handles one pointer event over the tab panel: a left press on a header tab fires that tab's own
     * action (and is consumed); every other event - body control hits, the scrollbar drag, the wheel -
     * is the body's, delegated to its {@link PanelController}.
     *
     * @param event     the pointer event
     * @param placement the laid-out tab panel the renderer drew this frame
     */
    public void handlePointer(InputEventAPI event, TabPanelPlacement placement) {
        // A left press on a header tab fires that tab's action; the header never scrolls, so it is not
        // clipped. Only a left press hits a tab - a wheel or an in-progress drag over the header falls
        // through to the body, which simply finds nothing there and consumes it, the same as any chrome.
        if (event.isLMBDownEvent() && PanelController.activateControlIfHit(
                placement.tabsHeader(),
                event.getX(),
                event.getY())) {
            event.consume();
            return;
        }
        bodyController.handlePointer(event, placement.body());
    }
}
