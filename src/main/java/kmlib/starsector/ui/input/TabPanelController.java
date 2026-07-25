package kmlib.starsector.ui.input;

import com.fs.starfarer.api.input.InputEventAPI;

import kmlib.starsector.ui.widgets.scroll.ScrollState;
import kmlib.starsector.ui.widgets.tabs.TabPanelCollapse;
import kmlib.starsector.ui.widgets.tabs.TabPanelPlacement;

/**
 * Drives one tab panel's pointer input: it routes a left press on a header tab to that tab's own action,
 * flips the collapse handle on a press of the notch, and delegates everything else - the body control hits,
 * the scrollbar drag, the wheel scroll - to a {@link PanelController} for the body. A tab panel is a panel
 * plus a header plus a collapse handle, so its input is the panel's input plus a header hit-test and a notch
 * hit-test on top; the tab's action is baked into its spec (the host wires it), so this controller stays
 * agnostic to what selecting a tab does.
 *
 * <p>One controller per panel, since it holds that panel's runtime state across frames: the body's scroll
 * and drag state, and the collapse animation. A host creates it, reads its {@link #getScrollState()} and
 * {@link #getCollapseFraction()} when it lays the panel out, advances the collapse each frame it draws, and
 * feeds it pointer events. The collapse state lives here beside the scroll offset because both are the
 * panel's own transient per-session UI state, not the host's; a consumer that lays out a placement and
 * pumps this controller inherits the collapse handle without wiring the animation itself. The panel opens
 * expanded by default, or collapsed to its docked rail via {@link #createStartingDocked()}, so a host picks
 * the initial fold at construction rather than driving the animation to reach it.
 */
public final class TabPanelController {
    // The body's controller, owning the scroll and drag state; this routes everything but a header-tab or
    // notch press to it, so the panel's scroll and drag behaviour is the plain panel's, unchanged.
    private final PanelController bodyController = new PanelController();

    // The collapse animation - how far the body is folded to its docked rail and which way it is heading.
    // Held beside the scroll offset so any tab-panel consumer inherits the handle by pumping this controller.
    private final TabPanelCollapse collapse;

    // Whether the pointer sat over the collapse notch as of the last pointer event, so the render pass can
    // light the handle. Only the input pass sees the pointer, so it is latched here; hover changes only when
    // the pointer moves, so the last-seen value stays correct on the frames between moves.
    private boolean isNotchHovered;

    /** A controller whose panel opens expanded - the fold a tab panel starts at unless a host asks otherwise. */
    public TabPanelController() {
        this(new TabPanelCollapse());
    }

    // Shared construction taking the collapse seed, so the expanded default and the docked start differ only
    // in that seed and neither construction path learns a second one.
    private TabPanelController(TabPanelCollapse collapse) {
        this.collapse = collapse;
    }

    /**
     * A controller whose panel opens collapsed to its docked rail rather than expanded, for a host that
     * wants the body out of the way until the player expands it. The handle then animates it open exactly as
     * an expanded panel animates shut.
     *
     * @return a controller seeded at the docked end
     */
    public static TabPanelController createStartingDocked() {
        return new TabPanelController(TabPanelCollapse.createDocked());
    }

    /**
     * @return the body's scroll position, for the layout to read (the requested offset) and settle
     *         (clamp to the overflow) each frame
     */
    public ScrollState getScrollState() {
        return bodyController.getScrollState();
    }

    /**
     * @return how far the body is collapsed toward its docked rail, eased for the render pass to lay the
     *         panel out at and to orient the notch chevron; 0 fully expanded, 1 fully docked
     */
    public float getCollapseFraction() {
        return collapse.getCollapseFraction();
    }

    /**
     * @return whether the pointer was over the collapse notch as of the last pointer event, for the render
     *         pass to light the handle
     */
    public boolean isNotchHovered() {
        return isNotchHovered;
    }

    /**
     * @return true only when the panel is fully expanded and idle - not docked, docking, or undocking - so
     *         a host can gate expanded-only input such as tab hotkeys, which should not switch tabs while
     *         the body is folded or in motion
     */
    public boolean isFullyExpanded() {
        return collapse.isFullyExpanded();
    }

    /**
     * Steps the collapse animation toward its current direction's end by a frame's worth of time, for the
     * host to call each frame it draws so the fold accelerates and settles under the eased curve. The
     * duration is the host's to supply, so it can expose the pace as a setting; a settled panel is left
     * unchanged, so an unconditional per-frame call rests once the animation ends.
     *
     * @param elapsedSeconds  real time since the last frame the host drew
     * @param durationSeconds how long a full collapse or expand should take; zero or less snaps instantly
     */
    public void advanceCollapse(float elapsedSeconds, float durationSeconds) {
        collapse.advanceByElapsedTime(elapsedSeconds, durationSeconds);
    }

    /**
     * Ends any in-progress body scrollbar drag, for the host to call when the panel stops showing so a
     * drag left dangling cannot hijack the next session.
     */
    public void cancelDrag() {
        bodyController.cancelDrag();
    }

    /**
     * Handles one pointer event over the tab panel: a left press on the collapse notch flips the fold and a
     * left press on a header tab fires that tab's own action (each consumed); every other event - body
     * control hits, the scrollbar drag, the wheel - is the body's, delegated to its {@link PanelController}.
     * Also latches whether the pointer is over the notch, so the render pass can light the handle.
     *
     * @param event     the pointer event
     * @param placement the laid-out tab panel the renderer drew this frame
     */
    public void handlePointer(InputEventAPI event, TabPanelPlacement placement) {
        // A null notch marks a bodyless, non-collapsible panel: there is no handle to hover or press, so
        // the hover latch clears and the toggle is skipped, and the event falls through to the header/body.
        var notch = placement.notch();
        // Latch hover off every pointer event so the render pass can light the handle; the notch draws past
        // the frame's right edge, so this is tested against the notch rect, not the body box.
        isNotchHovered = notch != null && notch.containsPoint(event.getX(), event.getY());
        // A left press on the notch flips the body between expanded and docked. Tested before the header and
        // body because the notch sits outside the box (and stays reachable when docked), so it can never
        // collide with a tab or a body control for the same press.
        if (notch != null && event.isLMBDownEvent() && notch.containsPoint(event.getX(), event.getY())) {
            collapse.toggleCollapse();
            event.consume();
            return;
        }
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
