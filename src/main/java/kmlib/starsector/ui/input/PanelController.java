package kmlib.starsector.ui.input;

import com.fs.starfarer.api.input.InputEventAPI;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.Control;
import kmlib.starsector.ui.controls.ControlSpec;
import kmlib.starsector.ui.controls.ReselectBehaviour;
import kmlib.starsector.ui.sound.UiSoundPlayer;
import kmlib.starsector.ui.sound.UiSoundScheme;
import kmlib.starsector.ui.sound.VanillaUiSoundPlayer;
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
 *
 * <p>Two moments this end answers audibly, both because it is where they happen: a press landing on a body
 * control, and the wheel moving the list. A headerless panel therefore presses and scrolls with sounds of
 * its own rather than only a tab panel's body. What either sounds like stays the look's, handed in with the
 * scheme - this end knows only that a press reached a control and that the list actually moved, which is the
 * part neither the event nor the scheme can say.
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

    // Where this panel's interface sounds go, held as a seam because a sound leaves no trace in the panel's
    // state: every other answer to an input can be read back off the scroll offset, and this one can only
    // be asserted by recording that it was asked for.
    private final UiSoundPlayer soundPlayer;

    // What each moment this panel answers sounds like, taken from the host with the rest of its look rather
    // than named here - how a wheel sounds is a property of how the panel presents itself, and this end owns
    // the moment and none of the choices.
    private final UiSoundScheme soundScheme;

    // A scrollbar-thumb drag in progress, and the pointer's offset from the thumb centre when grabbed. The
    // drag spans frames (press, moves, release), so it lives as state between events: while set, every
    // mouse move maps the pointer to a scroll position; the grab offset holds the thumb under the cursor
    // so it does not jump when grabbed off-centre.
    private boolean isDraggingThumb;
    private float thumbGrabOffsetY;

    // Whether the list has moved since a frame last asked. Latched rather than worked out from the offset,
    // because the offset a frame reads says where the list is and never how it got there - and the only end
    // that knows a move happened at all is the one that made it.
    private boolean hasListScrolledSinceLastFrame;

    /**
     * A panel that answers like a vanilla control - the scheme a panel scrolls by unless a host asks
     * otherwise.
     */
    public PanelController() {
        this(new VanillaUiSoundPlayer(), UiSoundScheme.createVanillaSoundScheme());
    }

    /**
     * A panel that sounds through the given player and answers by the given scheme - for a host wearing a
     * look of its own, or a test asserting which moments sound.
     *
     * @param soundPlayer where this panel's interface sounds go
     * @param soundScheme what each moment this panel answers sounds like, the audible half of the look the
     *                    host paints the panel from
     */
    public PanelController(UiSoundPlayer soundPlayer, UiSoundScheme soundScheme) {

        this.soundPlayer = soundPlayer;
        this.soundScheme = soundScheme;
    }

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
     * grab column, or sounds and fires the control under a left press. Every event over the panel is
     * consumed, so the surface behind it does not also act on it.
     *
     * <p>The order the branches are tried in is what keeps the scrollbar quiet: a press in the grab column
     * is taken as a drag before a control is ever offered it, so the gutter answers with the list moving and
     * never with a control's press.
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
        // The panel's claim on the event rather than a visibility gate on its controls: what it decides is
        // whether the wheel, a drag, and a press are this panel's to answer at all. The gate that keeps a
        // folded-away control from being hit is stated once inside the resolver, so a reader that never
        // reaches here - a hover - inherits it.
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

                // A press on the border or on blank body resolves to no control and only consumes (below),
                // so empty chrome swallows the click silently and without acting. What fired is immaterial
                // here - the action carries its own cell, and the press has already been answered where the
                // cell resolved - so the answer is dropped; a header tab is what needs it.
                pressBodyControlAtPoint(placement, event.getX(), event.getY());
            }
        }
        event.consume();
    }

    /**
     * Reports whether the list has moved since this was last asked, and forgets it - for a per-frame pass
     * that has to tell rows carried under a still pointer from a pointer moving over rows. Cleared by the
     * reading, so one movement is answered by the first frame after it and by that frame alone; a movement
     * made while nothing is drawing waits for the next frame rather than being dropped.
     *
     * <p>The wheel and a scrollbar drag both report through it, because what it answers is that content
     * moved and not what moved it - rows sliding past a parked cursor were reached by nobody either way.
     * Only the wheel sounds, that being the one act with a moment to it.
     *
     * @return whether the list's offset changed since the last frame read this
     */
    boolean takeHasListScrolledSinceLastFrame() {

        var hasScrolled = hasListScrolledSinceLastFrame;
        hasListScrolledSinceLastFrame = false;
        return hasScrolled;
    }

    /**
     * Drops a movement no frame has read yet, for a panel that stops showing - so the next session's first
     * frame answers the pointer where it is rather than silently taking whatever is under it on the
     * strength of a scroll from a session the player has since left.
     */
    void resetListScrolled() {
        hasListScrolledSinceLastFrame = false;
    }

    /**
     * Answers a left press on the body: sounds it where it reached a control, fires that control's action
     * when the cell is one worth acting on, and reports which cell fired. Resolves the press through {@link
     * #resolveHitBodyCell} and offers what comes back to {@link #activateCellIfActionable}, so the geometry
     * a press acts on is the geometry the panel's one body resolver answers, and the narrowing that decides
     * whether it acts is stated once, beside the action it gates.
     *
     * <p>The sound hangs off the resolve and not off the firing, so a press that lands on an inert cell
     * sounds like the press it was. That case - a re-press on a lit segment - is the one press with nothing
     * else to show for it, the screen answering it with no change at all, so hanging the sound on the action
     * would leave the panel's only unexplained press as its only silent one. Chrome stays quiet by the same
     * rule rather than by a second one: the resolver reports no cell on a border, on blank body, on a
     * caption or on a divider, and a press that reached nothing has nothing to answer for.
     *
     * <p>On the way down, the body's controls acting on the way down - a box is ticked and a fold is already
     * moving by the time the button comes up. A tab's lift is held until the release and so sounds there;
     * this end holds nothing, so there is no release to plumb.
     *
     * <p>The walk stops at the control the point is over rather than at the first control willing to act. A
     * press on an inert cell has landed on that cell, and looking past it for something further down the
     * strip would fire a control the player never aimed at. Nothing in a vertical strip overlaps today, so
     * the two orders agree - which is why the one that matches what the player pressed is the one written
     * down, rather than the one that happens to fall out of a loop over firings.
     *
     * <p>Split from the pointer event above, and reporting what fired rather than nothing, so the pairing
     * this seam exists for - the cell that fires is the cell that resolved - can be checked without an
     * engine input event to raise. The event handler drops the answer: it has nothing to mark, the action
     * carrying its own cell to whoever supplied the spec.
     *
     * @param placement the laid-out panel the renderer drew this frame
     * @param pointX    the press x, in UI coordinates
     * @param pointY    the press y, in UI coordinates
     * @return the control and cell that fired, or {@code null} when the press acted on nothing
     */
    ResolvedBodyCell pressBodyControlAtPoint(
            PanelPlacement placement,
            float pointX,
            float pointY) {

        var hitCell = resolveHitBodyCell(placement, pointX, pointY);
        if (hitCell == null) {
            return null;
        }
        soundPlayer.playCueIfPresent(soundScheme.pressCue());

        if (activateCellIfActionable(hitCell.control(), hitCell.slot().cell()) == NO_CELL_RESOLVED) {
            return null;
        }
        return hitCell;
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
     * Whether a laid-out control is one of the segmented kinds - a radio, a table, or a tabs row - as
     * opposed to a whole-row control hit anywhere on its bounds. The same rule the hit-tests below turn on,
     * asked of the control rather than of its spec, for a reader holding one and nothing to narrow.
     *
     * <p>Chrome answers no. A caption or a divider has no cells at all, so nothing about it is one of many
     * alike - and nothing about it ever resolves to a cell to ask this of in the first place.
     *
     * @param control the laid-out control
     * @return whether its cells are segments laid side by side
     */
    static boolean isSegmentedControl(Control control) {
        return control.spec() instanceof ControlSpec.Interactive interactive
            && isSegmented(interactive);
    }

    /**
     * Resolves which cell of which body control a point lands on - the one hit-test the panel answers its
     * body with, read by the press that fires a control and by whatever lights one under the pointer. Two
     * readers of one walk rather than two walks that happen to agree, so the control that lights and the
     * control a press lands on are the same control because they are the same answer.
     *
     * <p>Taking the placement rather than a control is the whole point of it. Both of the things that decide
     * whether a laid-out control is on screen live on it - the box the body is drawn inside and {@link
     * PanelPlacement#flexViewport()} the scrolling list is clipped to - so each reaches the hit-test without
     * any caller having to remember to hand it over. A row scrolled up under a pinned control (or down under
     * a footer) keeps its segment exactly where the layout put it, and a folding panel narrows its box over
     * controls that keep their laid-out places: a caller walking the strip for itself would find both
     * hittable, and lightable, straight through whatever is drawn over them.
     *
     * <p>Geometry and visibility and nothing else, like every resolver here: whether pressing the cell it
     * reports would <em>do</em> anything is {@link #activateCellIfActionable}'s question, which is what lets
     * a hover read what a press reads.
     *
     * @param placement the laid-out panel the renderer drew this frame
     * @param pointX    the point's x, in UI coordinates
     * @param pointY    the point's y, in UI coordinates
     * @return the control under the point and the slot it sits at, or {@code null} when it is over none
     */
    static ResolvedBodyCell resolveHitBodyCell(
            PanelPlacement placement,
            float pointX,
            float pointY) {

        // The body is drawn within its box and wiped with it, so a point outside the box is on none of the
        // controls laid inside: a collapsing panel narrows the box while its controls keep their laid-out
        // positions, leaving a strip of them behind the rail that is on screen nowhere.
        if (!placement.box().containsPoint(pointX, pointY)) {
            return null;
        }
        var bodyControls = placement.bodyControls();

        // Walked by index rather than over the list, the index being half of where the hit is: a fade is
        // held against the slot a control occupies, so the walk that finds the control reports the slot too
        // rather than leaving a reader to search the strip again for the position it just passed through.
        for (var controlIndex = 0; controlIndex < bodyControls.size(); controlIndex++) {

            var control = bodyControls.get(controlIndex);
            var resolvedCell = resolveHitCell(control, placement.flexViewport(), pointX, pointY);

            // A caption, a divider, and a scrolled-away row all report no cell, so the walk carries on past
            // them to the controls below rather than stopping on the first thing whose row the point is in.
            if (resolvedCell != NO_CELL_RESOLVED) {
                return new ResolvedBodyCell(control, new BodyCellSlot(controlIndex, resolvedCell));
            }
        }
        return null;
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

        var offsetBeforeDrag = scrollState.getOffset();
        scrollState.setOffset(
            PanelScrollbars.resolveOffsetForPointer(placement, pointerY - thumbGrabOffsetY));

        // Noted and not sounded. A drag is one held act carrying the list continuously, with the pointer
        // off on the scrollbar - so the rows it sweeps past the cursor must reach whatever answers
        // arrivals, while the sound belongs to the wheel alone; a drag ticking per frame would be the
        // chatter the wheel's single sound exists to avoid.
        recordListMovedFrom(offsetBeforeDrag);
    }

    // Scrolls the flex list when the wheel turns over its scroll region and it has somewhere to scroll, and
    // sounds the movement. Only the wheel's sign is read (like the vanilla scroll lists): a wheel up scrolls
    // toward the list top, so it decreases the offset, and a wheel down increases it, each by one fixed
    // step. Off the scroll region (over a pinned control, or a list that fits) the wheel does nothing,
    // though the caller still consumes it so the surface behind does not act.
    private void scrollListUnderPointer(InputEventAPI event, PanelPlacement placement) {
        if (!placement.isScrollbarNeeded()
                || !placement.flexViewport().containsPoint(event.getX(), event.getY())) {
            return;
        }
        var offsetBeforeWheel = scrollState.getOffset();
        scrollState.scrollBy(-Math.signum((float) event.getEventValue()) * SCROLL_STEP_PX);

        // Settled here against the overflow the drawn frame resolved, rather than left to the next layout's
        // clamp as the stored request otherwise is. It is what makes the question below "did the list move"
        // instead of "did a request change": a wheel at the end of a list pushes the raw offset past
        // anything that can be shown, and would sound for a list that never budged.
        scrollState.clampTo(placement.scrollOverflow());

        if (recordListMovedFrom(offsetBeforeWheel)) {
            soundPlayer.playCueIfPresent(soundScheme.listScrollCue());
        }
    }

    // Notes whether the list actually went anywhere, having just been moved from the given offset, and
    // holds that for the next frame to read. One place the movement is judged, so the wheel and the drag
    // cannot come to disagree about what counts as the list having moved - and one place the frame-facing
    // latch is written, so neither can move the list without the arrivals hearing of it.
    private boolean recordListMovedFrom(float offsetBeforeMove) {

        var hasMoved = Float.compare(scrollState.getOffset(), offsetBeforeMove) != 0;
        hasListScrolledSinceLastFrame |= hasMoved;
        return hasMoved;
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
