package kmlib.starsector.ui.input;

import com.fs.starfarer.api.input.InputEventAPI;

import kmlib.starsector.ui.widgets.RadioRow;
import kmlib.starsector.ui.widgets.scroll.ScrollState;
import kmlib.starsector.ui.widgets.tabs.TabInteractionSources;
import kmlib.starsector.ui.widgets.tabs.TabPanelCollapse;
import kmlib.starsector.ui.widgets.tabs.TabPanelPlacement;
import kmlib.starsector.ui.widgets.tabs.TabWashSource;

/**
 * Drives one tab panel's pointer input: it routes a left press on a header tab to that tab's own action,
 * flips the collapse handle on a press of the notch, and delegates everything else - the body control hits,
 * the scrollbar drag, the wheel scroll - to a {@link PanelController} for the body. A tab panel is a panel
 * plus a header plus a collapse handle, so its input is the panel's input plus a header hit-test and a notch
 * hit-test on top; the tab's action is baked into its spec (the host wires it), so this controller stays
 * agnostic to what selecting a tab does.
 *
 * <p>One controller per panel, since it holds that panel's runtime state across frames: the body's scroll
 * and drag state, the collapse animation, and the hover fades of the parts that light under the pointer -
 * the header tabs and the collapse handle. A host creates it, reads its {@link #getScrollState()} and
 * {@link #getCollapseFraction()} when it lays the panel out, advances the collapse and the hover fades each
 * frame it draws, reads {@link #getTabInteractionSources()} and {@link #getNotchHoverFraction()} to paint
 * with, and feeds it pointer events. That state lives here beside the scroll offset because all of it
 * is the panel's own transient per-session UI state, not the host's; a consumer that lays out a placement and
 * pumps this controller inherits the collapse handle and the live tabs without wiring either animation
 * itself. The panel opens
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

    // How far each header tab has travelled onto the hovered shade, keyed by its index in the row - stable
    // for as long as the row is, which is all a key has to be. Held here with the panel's other transient
    // state rather than on the placement, which is an immutable value the layout computes: a fade is where
    // the panel currently stands, not where its parts sit.
    private final HoverFades<Integer> tabHoverFades = new HoverFades<>();

    // How far the collapse handle has travelled onto its lit look. A lone fade rather than a keyed set,
    // there being one handle per panel, and a fraction rather than a flag so the notch lights and dims at
    // the pace the tabs do rather than switching on the frame the pointer arrives.
    private final HoverFade notchHoverFade = new HoverFade();

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
     * @return how far the collapse handle has faded onto its lit look, for the render pass to light the
     *         handle by; 0 fully at rest, 1 fully lit
     */
    public float getNotchHoverFraction() {
        return notchHoverFade.getHoverFraction();
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
     * What the header's tabs are currently showing, for the render pass to paint them at: how far each has
     * faded onto the hovered shade, and what momentary lift each carries. The paint pass therefore reads no
     * cursor and holds no timing - it is handed both channels already resolved.
     *
     * @return the panel's live tab interaction channels
     */
    public TabInteractionSources getTabInteractionSources() {
        // No pulse animator exists yet, so the lift channel rests; the hover channel is live.
        return new TabInteractionSources(
            tabHoverFades::resolveHoverFractionAt,
            TabWashSource.createRestingWashSource());
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
     * Steps every hover fade the panel holds - its header tabs' and its collapse handle's - by a frame's
     * worth of time, for the host to call each frame it draws, after it has resolved the placement. One call
     * rather than one per hovered part, so the panel's elements cannot be advanced against different
     * placements or charged different slices of the same frame.
     *
     * <p>What is under the pointer is resolved against the very placement being drawn rather than latched
     * from the last pointer event. That is what keeps a fade honest when the panel moves under a still
     * cursor: a scroll, a fold, or a relayout otherwise leaves an element lit that the pointer is no longer
     * over.
     *
     * <p>A panel that is not fully expanded hovers no tab. Its header is being wiped toward the docked rail
     * (or is already gone behind it), so a tab still laid out under the pointer is not a tab the player can
     * see, let alone one they are pointing at. The handle is not gated that way - it draws past the frame and
     * outlives the fold, being what brings a docked panel back.
     *
     * @param placement       the laid-out tab panel this frame is drawing
     * @param elapsedSeconds  real time since the last frame the host drew
     * @param durationSeconds how long a full fade onto a hovered look should take; zero or less snaps
     */
    public void advanceHoverFades(
            TabPanelPlacement placement,
            float elapsedSeconds,
            float durationSeconds) {

        // One cursor read spent on both hit-tests, so the tab and the handle answer the same pointer.
        var cursorX = UiCursor.getUiX();
        var cursorY = UiCursor.getUiY();

        advanceHoverFadesTowardHovered(
            resolveHoveredTabIndex(placement, cursorX, cursorY),
            placement.containsPointInNotch(cursorX, cursorY),
            elapsedSeconds,
            durationSeconds);
    }

    /**
     * Ends any in-progress body scrollbar drag, for the host to call when the panel stops showing so a
     * drag left dangling cannot hijack the next session.
     */
    public void cancelDrag() {
        bodyController.cancelDrag();
    }

    /**
     * Drops every hover fade the panel holds, for the host to call when the panel stops showing. A fade left
     * part-way up would otherwise be the first thing the next session paints and then wind down, showing the
     * player the tail of a hover they never saw begin - the same reason a host drops its frame clock there.
     */
    public void resetHoverFades() {
        tabHoverFades.resetFades();
        notchHoverFade.resetFade();
    }

    /**
     * Handles one pointer event over the tab panel: a left press on the collapse notch flips the fold and a
     * left press on a header tab fires that tab's own action (each consumed); every other event - body
     * control hits, the scrollbar drag, the wheel - is the body's, delegated to its {@link PanelController}.
     * Hover is none of its business: the fades are resolved per frame against the drawn placement by {@link
     * #advanceHoverFades}, so nothing here has to be latched for the render pass.
     *
     * @param event     the pointer event
     * @param placement the laid-out tab panel the renderer drew this frame
     */
    public void handlePointer(InputEventAPI event, TabPanelPlacement placement) {

        // A left press on the notch flips the body between expanded and docked. Tested before the header and
        // body because the notch sits outside the box (and stays reachable when docked), so it can never
        // collide with a tab or a body control for the same press.
        if (event.isLMBDownEvent()
                && placement.containsPointInNotch(event.getX(), event.getY())) {
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

    /**
     * Steps the fades toward the named elements, once the cursor read and the hit-tests above have settled
     * which those are, and applies the docked gate the tabs answer to. Split off for the same reason {@link
     * UiCursor} keeps its scaling separable from its LWJGL read: what is left here is the whole of what the
     * hover channel does per frame, and it is verifiable without a display to point at.
     *
     * @param hoveredTabIndex the tab the pointer is on this frame, or null when it is on none
     * @param isNotchHovered  whether the pointer is on the collapse handle this frame
     * @param elapsedSeconds  real time since the last frame the host drew
     * @param durationSeconds how long a full fade onto a hovered look should take; zero or less snaps
     */
    void advanceHoverFadesTowardHovered(
            Integer hoveredTabIndex,
            boolean isNotchHovered,
            float elapsedSeconds,
            float durationSeconds) {

        // A panel that is not fully expanded hovers no tab, whatever is laid out under the pointer: its
        // header is being wiped toward the docked rail, or is already gone behind it. The handle takes no
        // such gate - it draws past the frame and outlives the fold, being what brings a docked panel back.
        tabHoverFades.advanceTowardHoveredKey(
            isFullyExpanded()
                ? hoveredTabIndex
                : null,
            elapsedSeconds,
            durationSeconds);

        notchHoverFade.advanceTowardHover(isNotchHovered, elapsedSeconds, durationSeconds);
    }

    /**
     * Which header tab a point falls on, as the key a hover fade is held under. Resolved over the header
     * control's laid segments - the same rectangles a press is hit-tested against - so the tab that lights
     * and the tab that would fire are always the same one.
     *
     * @param placement the laid-out tab panel to test against
     * @param pointX    the point's x in UI coordinates, the coordinates the placement is laid out in
     * @param pointY    the point's y in UI coordinates
     * @return the tab's index, or null when the point is on no tab
     */
    static Integer resolveHoveredTabIndex(
            TabPanelPlacement placement,
            float pointX,
            float pointY) {

        var segmentIndex = RadioRow.findSegmentIndexAt(
            placement.tabsHeader().segments(),
            pointX,
            pointY);

        // Null rather than the row-miss sentinel, because a keyed fade set is asked "which element, if any"
        // and an out-of-row index would be a key like another - one fade per place the pointer has missed.
        return segmentIndex == RadioRow.NO_SEGMENT
            ? null
            : segmentIndex;
    }
}
