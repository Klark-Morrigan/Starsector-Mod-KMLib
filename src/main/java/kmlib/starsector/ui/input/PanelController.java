package kmlib.starsector.ui.input;

import com.fs.starfarer.api.input.InputEventAPI;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.ReselectBehaviour;
import kmlib.starsector.ui.widgets.PanelPlacement;
import kmlib.starsector.ui.widgets.RadioRow;
import kmlib.starsector.ui.widgets.scroll.PanelScrollbars;
import kmlib.starsector.ui.widgets.scroll.ScrollState;

/**
 * Drives one headerless panel's pointer input, owning the runtime state a panel's input needs across
 * frames: its {@link ScrollState} (read by the layout to place the scrolling list, written by the wheel
 * and by a drag) and the in-progress scrollbar-thumb drag. It stays agnostic to what a body control
 * means: a press on a control fires the control's own {@link kmlib.starsector.ui.controls.ControlAction},
 * so the controller dispatches a checkbox toggle and a radio pick the same way without learning either.
 *
 * <p>One controller per panel, since it holds that panel's scroll and drag state; a host creates it, reads
 * its {@link #getScrollState()} when it lays the panel out, and feeds it pointer events. The pointer
 * mechanics are all here - drag (grab, follow, release), wheel scroll of the flex list, control hit-and-
 * fire, and consuming every event over the panel so the surface behind it does not also act. A {@link
 * kmlib.starsector.ui.input.TabPanelController} reuses this for the body and routes the header tabs
 * separately, so a tab switch stays with that controller.
 */
public final class PanelController {

    // Pixels one wheel notch scrolls the flex list. Only the wheel's sign is read (like the vanilla
    // scroll lists), so each notch moves this fixed step regardless of the raw wheel magnitude - about two
    // list rows, a comfortable step without overshooting a short list.
    private static final float SCROLL_STEP_PX = 40f;

    // No cell: what a hit-test reports when the point missed every cell or landed on chrome that is not a
    // hit target, and what the firing step reports when the cell it was handed turned out to be inert. Null
    // rather than an index sentinel, because the answer is "which cell, if any": an out-of-range index reads
    // as a cell like any other to a caller keying anything by it, while a null cannot be keyed by at all.
    //
    // Shared with the tab panel rather than restated there, this being the answer its header resolver hands
    // straight back - a second name for one null is a second place to explain why it is not an index.
    static final Integer NO_CELL_RESOLVED = null;

    // This panel's scroll position, read by the layout and written by the wheel and by a drag.
    private final ScrollState scrollState = new ScrollState();

    // A scrollbar-thumb drag in progress, and the pointer's offset from the thumb centre when grabbed. The
    // drag spans frames (press, moves, release), so it lives as state between events: while set, every
    // mouse move maps the pointer to a scroll position; the grab offset holds the thumb under the cursor
    // so it does not jump when grabbed off-centre.
    private boolean isDraggingThumb;
    private float thumbGrabOffsetY;

    /**
     * @return this panel's scroll position, for the layout to read (the requested offset) and settle
     *         (clamp to the overflow) each frame
     */
    public ScrollState getScrollState() {
        return scrollState;
    }

    /**
     * Ends any in-progress thumb drag, for the host to call when the panel stops showing so a drag left
     * dangling cannot hijack the next session.
     */
    public void cancelDrag() {
        isDraggingThumb = false;
    }

    /**
     * Handles one pointer event over the panel: continues a thumb drag wherever the pointer is, else -
     * over the panel box - scrolls the flex list on a wheel, starts a drag on a press in the scrollbar
     * grab column, or fires the control under a left press. Every event over the panel is consumed, so the
     * surface behind it does not also act on it.
     *
     * @param event     the pointer event
     * @param placement the laid-out panel the renderer drew this frame
     */
    public void handlePointer(InputEventAPI event, PanelPlacement placement) {
        // A thumb drag in progress owns the event wherever the pointer is - even past the panel edge - so
        // the list keeps following the cursor until the release, rather than dropping the drag the moment
        // the pointer leaves the narrow scrollbar column.
        if (isDraggingThumb) {
            continueThumbDrag(event, placement);
            return;
        }
        if (!placement.box().containsPoint(event.getX(), event.getY())) {
            return;
        }
        // A wheel over the panel scrolls its flex list rather than acting on the surface behind it; a press
        // on the scrollbar's grab column starts a drag; any other left press fires the control under it.
        // Either way the event is consumed below.
        if (event.isMouseScrollEvent()) {
            scrollListUnderPointer(event, placement);
        } else if (event.isLMBDownEvent()) {
            if (!beginThumbDragIfPressed(event, placement)) {
                actOnLeftPress(
                    placement,
                    event.getX(),
                    event.getY());
            }
        }
        event.consume();
    }

    /**
     * Fires the action of the cell a press lands on, and reports which cell that was. Resolves the cell
     * through {@link #resolveHitCell(Control, Rectangle, float, float)} and offers it to {@link
     * #activateCellIfActionable}, so the geometry a press acts on is the geometry that resolver answers and
     * the narrowing that decides whether it acts is stated once, beside the action it gates.
     *
     * <p>The cell comes back rather than a bare yes/no because the hit-test is the only thing that resolved
     * it: a caller wanting to mark the cell it just fired would otherwise walk the same segments a second
     * time to recover a number this already had. The action's meaning stays with whoever supplied the spec -
     * this only maps the click to a cell.
     *
     * @param control      the laid-out control to hit-test
     * @param flexViewport the scrolling control's viewport; a scrolling control only counts inside it
     * @param pointX       the press x, in UI coordinates
     * @param pointY       the press y, in UI coordinates
     * @return the cell that fired, or {@code null} when the press acted on nothing
     */
    static Integer activateControlIfHit(
            Control control,
            Rectangle flexViewport,
            float pointX,
            float pointY) {
        return activateCellIfActionable(
            control,
            resolveHitCell(control, flexViewport, pointX, pointY));
    }

    /**
     * Fires the action of an already-resolved cell when the control's {@link ReselectBehaviour} says that
     * cell is worth acting on, and reports the cell that fired. A segmented control's lit segment is inert
     * unless its reselect fires on a re-pick - a plain option pair and a tabs row swallow it, a deselectable
     * picker and a re-firing selector do not - and every other cell acts.
     *
     * <p>This is the whole of the press's narrowing, kept out of the resolver so a reader can ask which cell
     * is under a point without also being told whether pressing it would do anything. Those are different
     * questions: a tabs row lights the tab it is already showing and fires nothing there, so a hover that
     * took the press's answer would leave the lit tab dark.
     *
     * @param control      the laid-out control the cell belongs to
     * @param resolvedCell the cell a resolver reported under the point, or {@code null} for none
     * @return the cell that fired, or {@code null} when nothing acted
     */
    static Integer activateCellIfActionable(Control control, Integer resolvedCell) {
        if (resolvedCell == NO_CELL_RESOLVED) {
            return NO_CELL_RESOLVED;
        }
        // Only an Interactive spec ever resolves to a cell, so this cannot fail once one came back; it is
        // how the action is reached without a cast.
        if (!(control.spec() instanceof ControlSpec.Interactive interactive)
                || !isActionableCell(interactive, resolvedCell)) {
            return NO_CELL_RESOLVED;
        }
        interactive.action().activateCell(resolvedCell);
        return resolvedCell;
    }

    /**
     * Resolves which cell of a control in a scrollable strip a point lands on: a control marked {@link
     * ControlSpec.VerticalTable#scrolls()} counts only inside {@code flexViewport}, and otherwise resolves
     * as {@link #resolveHitCell(Control, float, float)}. The scrolling list clips because a row scrolled up
     * under a pinned header (or down under a footer) is drawn away, so its segment - still laid out at its
     * scrolled position - must not stay hittable through the control that hides it. Every non-scrolling
     * control ignores the viewport, so the clip bites only the one flex list.
     *
     * @param control      the laid-out control to hit-test
     * @param flexViewport the scrolling control's viewport; a scrolling control only counts inside it
     * @param pointX       the point's x, in UI coordinates
     * @param pointY       the point's y, in UI coordinates
     * @return the cell under the point, or {@code null} when it lands on none
     */
    static Integer resolveHitCell(
            Control control,
            Rectangle flexViewport,
            float pointX,
            float pointY) {
                
        if (control.spec() instanceof ControlSpec.VerticalTable table
                && table.scrolls()
                && !flexViewport.containsPoint(pointX, pointY)) {
            return NO_CELL_RESOLVED;
        }
        return resolveHitCell(control, pointX, pointY);
    }

    /**
     * Resolves which cell of a control a point lands on, without firing anything. A radio or a tabs row hits
     * by segment over the segments the layout laid; a single-cell checkbox or toggle hits anywhere on its
     * row, reported as {@link ControlSpec#SINGLE_CELL}. A caption label or a divider is not a hit target and
     * resolves to no cell, so a press falls through to a control below rather than being swallowed on an
     * inert action.
     *
     * <p>Geometry and nothing else: the lit segment of an inert radio resolves to itself here, even though a
     * press on it fires nothing. Whether a cell would act is {@link #activateCellIfActionable}'s question,
     * which is what lets a hover and a press share this one answer - a control that lights the cell it is
     * already showing is the common case, not the exception.
     *
     * @param control the laid-out control to hit-test
     * @param pointX  the point's x, in UI coordinates
     * @param pointY  the point's y, in UI coordinates
     * @return the cell under the point, or {@code null} when it lands on none
     */
    static Integer resolveHitCell(Control control, float pointX, float pointY) {
        // A caption row and a divider are drawn but not clickable - they are not Interactive - so a press
        // over either hits nothing and falls through to let the loop try the controls below, never
        // consuming a click as if it acted. The divider matters here because it spans the whole body width.
        if (!(control.spec() instanceof ControlSpec.Interactive interactive)) {
            return NO_CELL_RESOLVED;
        }
        // A radio or a tabs row hits by segment over the segments the layout laid - a radio's equal cells
        // or a tabs row's per-tab boxes.
        if (isSegmented(interactive)) {

            var segmentIndex = RadioRow.findSegmentIndexAt(control.segments(), pointX, pointY);

            // Branched rather than a ternary: a conditional mixing the boxed no-cell answer with the int
            // index unboxes both arms, so the miss case would throw on the null instead of reporting it.
            if (segmentIndex == RadioRow.NO_SEGMENT) {
                return NO_CELL_RESOLVED;
            }
            return segmentIndex;
        }
        if (!control.bounds().containsPoint(pointX, pointY)) {
            return NO_CELL_RESOLVED;
        }
        return ControlSpec.SINGLE_CELL;
    }

    // Starts a scrollbar drag when a left press lands on the grab column, reporting whether it did. The
    // grab column is the gutter right of the list, wider than the thin track so it need not be hit exactly;
    // a press on the thumb records its offset from the thumb centre so the thumb stays under the cursor,
    // while a press on the bare track jumps the thumb to the pointer at once. Only fires while the list
    // overflows - there is no scrollbar otherwise.
    private boolean beginThumbDragIfPressed(InputEventAPI event, PanelPlacement placement) {
        if (!placement.isScrollbarNeeded()) {
            return false;
        }
        if (!PanelScrollbars.computeGrabColumn(placement).containsPoint(event.getX(), event.getY())) {
            return false;
        }
        isDraggingThumb = true;
        var thumb = PanelScrollbars.computeThumb(placement);
        thumbGrabOffsetY = thumb.containsPoint(event.getX(), event.getY())
            ? event.getY() - thumb.computeCenterY()
            : 0f;
        updateDragOffset(placement, event.getY());
        return true;
    }

    // Follows an in-progress drag: the release ends it, and until then every move maps the pointer to a
    // scroll position. Consumes the event so the surface behind neither pans nor acts while the thumb is
    // held.
    private void continueThumbDrag(InputEventAPI event, PanelPlacement placement) {
        if (event.isLMBUpEvent()) {
            isDraggingThumb = false;
            event.consume();
            return;
        }
        if (placement.isScrollbarNeeded()) {
            updateDragOffset(placement, event.getY());
        }
        event.consume();
    }

    // Maps the dragged pointer to an absolute scroll offset along the track and stores it, holding the
    // thumb the grab offset below the cursor so it tracks the drag rather than snapping its centre to the
    // pointer.
    private void updateDragOffset(PanelPlacement placement, float pointerY) {
        scrollState.setOffset(
            PanelScrollbars.resolveOffsetForPointer(placement, pointerY - thumbGrabOffsetY));
    }

    // Scrolls the flex list when the wheel turns over its scroll region and it has somewhere to scroll.
    // Only the wheel's sign is read (like the vanilla scroll lists): a wheel up scrolls toward the list
    // top, so it decreases the offset, and a wheel down increases it, each by one fixed step. Off the
    // scroll region (over a pinned control, or a list that fits) the wheel does nothing, though the caller
    // still consumes it so the surface behind does not act.
    private void scrollListUnderPointer(InputEventAPI event, PanelPlacement placement) {
        if (!placement.isScrollbarNeeded()
                || !placement.flexViewport().containsPoint(event.getX(), event.getY())) {
            return;
        }
        scrollState.scrollBy(-Math.signum((float) event.getEventValue()) * SCROLL_STEP_PX);
    }

    // Routes a left press inside the box to the body control under it: a control fires its action. A press
    // on the border or blank body falls through to no control and only consumes (handled by the caller),
    // so empty chrome swallows the click without acting. Which cell fired is immaterial here - the action
    // carries it - so only "did one" is read; a header tab is what needs the cell itself.
    private static void actOnLeftPress(PanelPlacement placement, float pointX, float pointY) {
        for (var control : placement.bodyControls()) {
            if (activateControlIfHit(control, placement.flexViewport(), pointX, pointY) != null) {
                return;
            }
        }
    }

    // Whether a press on an already-resolved cell reaches the control's action. Only a segmented control
    // narrows: its lit segment is inert unless the reselect it carries fires on a re-pick, which is the
    // standard radio rule and what makes re-clicking a vanilla tab strip's active tab do nothing. A
    // single-cell checkbox or toggle has no lit segment to re-pick, so every hit on it acts.
    private static boolean isActionableCell(ControlSpec.Interactive control, int resolvedCell) {
        if (!isSegmented(control)) {
            return true;
        }
        return reselectBehaviourOf(control).firesOnReselect()
            || resolvedCell != control.selectedIndex();
    }

    // The reselect the control carries, or INERT for a variant that has none. A vertical table and a
    // horizontal radio each name what a re-pick of their lit segment does; a tabs row is always inert on
    // its lit tab, so it is read as INERT here rather than carrying its own field.
    private static ReselectBehaviour reselectBehaviourOf(ControlSpec.Interactive control) {
        if (control instanceof ControlSpec.VerticalTable table) {
            return table.reselect();
        }
        if (control instanceof ControlSpec.HorizontalRadio radio) {
            return radio.reselect();
        }
        return ReselectBehaviour.INERT;
    }

    // A horizontal radio, a vertical table, and a tabs row all resolve a click to one of their laid-out
    // segments, so the three hit-test through one path; a single-cell checkbox or toggle is hit anywhere
    // on its bounds instead.
    private static boolean isSegmented(ControlSpec.Interactive control) {
        return control instanceof ControlSpec.HorizontalRadio
            || control instanceof ControlSpec.VerticalTable
            || control instanceof ControlSpec.Tabs;
    }
}
