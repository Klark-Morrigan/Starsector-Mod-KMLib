package kmlib.starsector.ui.input;

import com.fs.starfarer.api.input.InputEventAPI;

import kmlib.animation.PulseEnvelopes;
import kmlib.animation.TraverseDurations;
import kmlib.starsector.ui.controls.BodyHoverSource;
import kmlib.starsector.ui.controls.BodyPressSource;
import kmlib.starsector.ui.sound.UiSoundPlayer;
import kmlib.starsector.ui.sound.UiSoundScheme;
import kmlib.starsector.ui.sound.VanillaUiSoundPlayer;
import kmlib.starsector.ui.widgets.PanelPlacement;
import kmlib.starsector.ui.widgets.scroll.PanelScrollbars;
import kmlib.starsector.ui.widgets.scroll.ScrollState;

/**
 * Drives one headerless panel's pointer input, owning the runtime state a panel's input needs across
 * frames: its {@link ScrollState} (read by the layout to place the scrolling list, written by the wheel
 * and by a drag), the in-progress scrollbar-thumb drag, and the lift each body cell carries in answer to a
 * press landing on it. It stays agnostic to what a body control
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
 *
 * <p>A press is also the one of those the panel goes on showing after the moment has passed, so this end
 * holds its lift as well as sounding it. Both hang off the same resolved cell, which is what keeps the cell
 * that sounds and the cell that lights from ever parting. What the lift is made of stays the widget's own
 * paint and the pace stays the caller's, handed in with the frame: this end times a lift and draws nothing.
 *
 * <p>Everything else one body cell is currently doing is held here for the same reason, off the reading a
 * caller resolves each frame and hands in: how far it has travelled onto its hovered look, whether the
 * pointer just reached it, and - out to the host that supplied the control - which of its cells that
 * pointer is on. The hover and the press are one subject, so a panel with no header holds them both; and
 * the arrival is one this end can answer honestly, being the end that knows whether the list moved rather
 * than the pointer. Which sound an arrival makes is left to the caller, a body inside a tab panel answering
 * at the level that panel's chrome leaves it.
 */
public final class PanelController {

    // Pixels one wheel notch scrolls the flex list. Only the wheel's sign is read (like the vanilla
    // scroll lists), so each notch moves this fixed step regardless of the raw wheel magnitude - about two
    // list rows, a comfortable step without overshooting a short list.
    private static final float SCROLL_STEP_PX = 40f;

    // The lift each body cell is carrying in answer to a press that landed on it, keyed by the slot that
    // cell occupies in the strip. Held on this end rather than beside a header's own click lifts because
    // this is where a body press is detected - the same reason the press sounds from here. Driving it is
    // whatever pumps the panel's frame, which is a tab panel today; a holder that never steps these has
    // no lift to read and none to drop, since only the advance spends one.
    //
    // Keyed by the slot for the reason the body's fades are: a host rebuilds its strip every frame, so a
    // press belongs to the place under the pointer rather than to the widget standing in it. Unheld, unlike
    // a tab's, because a body control acts on the way down and has nothing left to hold by the time the
    // button comes up.
    private final PulseEnvelopes<BodyCellSlot> bodyPressPulses = new PulseEnvelopes<>();

    // How far each body cell has travelled onto its hovered look, keyed by the slot it occupies in the
    // strip. Beside the lifts above because both are what one cell of this body is currently showing, and
    // keyed alike for the same reason: a host rebuilds its strip every frame, so either belongs to the
    // place under the pointer rather than to the widget standing in it.
    private final HoverFades<BodyCellSlot> bodyHoverFades = new HoverFades<>();

    // When the pointer reaches a body cell, keyed by the slot its fade is held against. One latch for the
    // whole strip rather than one per control, only one cell of a body being under the pointer at a time -
    // so crossing from one segment of a row to the next replaces the key and reads as the arrival it is.
    //
    // Keyed by the slot alone, which is why a widget swapped into a slot under a still pointer announces
    // nothing: an arrival is the player reaching something, and a strip rebuilt beneath a parked cursor was
    // reached by nobody.
    private final KeyedHoverArrival<BodyCellSlot> bodyHoverArrival = new KeyedHoverArrival<>();

    // Where the body's reading goes back out to whoever built the control under the pointer, once per
    // change. Beside the latch above because both turn one per-frame reading into a moment, and apart from
    // it on a scroll: rows carried under a parked cursor were reached by nobody, and are still a different
    // row for the host to answer.
    private final BodyHoverReporter bodyHoverReporter = new BodyHoverReporter();

    // This panel's scroll position, read by the layout and written by the wheel and by a drag.
    private final ScrollState scrollState = new ScrollState();

    // What this panel sounds like in answer to the moments it detects. This end owns the moments and none of
    // the choices, which is why the pair arrives whole rather than being named here.
    private final PanelSounds sounds;

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
     * look of its own, or one that wants to observe which moments sound.
     *
     * @param soundPlayer where this panel's interface sounds go
     * @param soundScheme what each moment this panel answers sounds like, the audible half of the look the
     *                    host paints the panel from
     */
    public PanelController(UiSoundPlayer soundPlayer, UiSoundScheme soundScheme) {
        this(new PanelSounds(soundPlayer, soundScheme));
    }

    /**
     * A panel sounding as an enclosing panel already does, for a tab panel building the body beneath its own
     * header: handing the one value down is what makes the two halves answer alike, rather than two ends
     * assembled from the same parts and trusted to match.
     *
     * @param sounds what the panel sounds like in answer to the moments it detects
     */
    PanelController(PanelSounds sounds) {
        this.sounds = sounds;
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
     * claimed, so the surface behind it does not also act on it - a press or a wheel by being consumed,
     * and a move by {@linkplain PointerParking parking the pointer} instead, the surface having to hear
     * that the pointer left the control it had lit.
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
        // on the scrollbar's grab column starts a drag; any other left press is answered by the control
        // under it. Every other event is only claimed (below), which is why nothing but these two sounds.
        if (event.isMouseScrollEvent()) {
            scrollListUnderPointer(event, placement);
        } else if (event.isLMBDownEvent()) {
            if (!beginThumbDragIfPressed(event, placement)) {

                // A press on the border or on blank body resolves to no control and is only claimed (below),
                // so empty chrome swallows the click silently and without acting. What fired is immaterial
                // here - the action carries its own cell, and the press has already been answered where the
                // cell resolved - so the answer is dropped; a header tab is what needs it.
                pressBodyControlAtPoint(placement, event.getX(), event.getY());
            }
        }
        // Claimed rather than consumed outright, which for a move means the pointer is parked instead: the
        // screen underneath has to hear that the pointer left the control it lit, and a consumed event
        // tells it nothing. Last, so everything above reads the pointer where the player actually put it.
        PointerParking.claimEvent(event);
    }

    /**
     * Reports whether the list has moved since this was last asked, and forgets it - read by the arrival
     * this class detects, which has to tell rows carried under a still pointer from a pointer moving over
     * rows. Cleared by the reading, so one movement is answered by the first frame after it and by that
     * frame alone; a movement made while nothing is drawing waits for the next frame rather than being
     * dropped.
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
     * Drops a movement no frame has read yet, spent with the body's other motions when a panel stops
     * showing - so the next session's first frame answers the pointer where it is rather than silently
     * taking whatever is under it on the strength of a scroll from a session the player has since left.
     */
    void resetListScrolled() {
        hasListScrolledSinceLastFrame = false;
    }

    /**
     * What the body's controls are currently showing, for the render pass to lift them by: asked for a
     * control's place in the drawn strip, it answers that control's own cells. The strip walk binds the
     * position and the widget below passes only the cell it is painting, so the two halves of a slot are
     * never both loose in one call - a crossed pair would light a cell of the wrong control, which is a
     * flicker nobody can reproduce rather than a failure anything reports.
     *
     * <p>The seam a paint pass takes, over the fraction read below: a control is drawn cell by cell, so what
     * it needs is something to ask, not a fraction fetched per cell by a caller that would have to spell the
     * slot out itself.
     *
     * @return the body's live hover channel
     */
    BodyHoverSource getBodyHoverSource() {
        return controlIndex -> cell -> resolveBodyHoverFractionAt(new BodyCellSlot(controlIndex, cell));
    }

    /**
     * What the body's controls are showing for the presses they answered, bound the same two steps the hover
     * channel above is: the strip walk binds a control's place and the widget below passes only the cell it
     * is painting, so the two halves of a slot are never both loose in one call.
     *
     * <p>A channel beside that one rather than folded into it, because the two say different things about
     * one cell: where the pointer is standing, and what it just did there.
     *
     * @return the body's live press channel
     */
    BodyPressSource getBodyPressSource() {
        return controlIndex -> cell -> resolveBodyPressFractionAt(new BodyCellSlot(controlIndex, cell));
    }

    /**
     * Steps every motion the body makes in answer to input - the hover fades of its controls and the press
     * lifts running on its cells - and reports the hovered cell onward to the host that built the control,
     * for the pass that pumps the panel's frame to call once it has resolved what is under the pointer.
     *
     * <p>One call rather than one per motion, so a body's parts cannot be advanced against different
     * readings or charged different slices of the same frame. What is under the pointer is resolved against
     * the placement being drawn rather than latched from the last pointer event, which is what keeps a fade
     * honest - and a report current - when the panel moves under a still cursor.
     *
     * <p>The arrival that reading is also owed is detected separately ({@link #detectBodyCellArrivalAt}),
     * because who sounds it is the caller's: a body inside a tab panel answers at the level that panel's
     * chrome leaves it, and only the caller holds the rest of the frame's reading to weigh it against.
     *
     * @param hoveredCell    the body cell the pointer is on this frame, or null when it is on none
     * @param elapsedSeconds real time since the last frame the host drew
     * @param durations      how long a traverse takes each way; a non-positive one snaps that way
     */
    void advanceBodyInputMotionsForFrame(
            HoveredBodyCell hoveredCell,
            float elapsedSeconds,
            TraverseDurations durations) {

        bodyHoverFades.advanceTowardHoveredKey(
            HoveredBodyCell.resolveSlotOf(hoveredCell),
            elapsedSeconds,
            durations);

        bodyHoverReporter.reportHoverChangeTo(hoveredCell);
        advanceBodyPressPulses(elapsedSeconds, durations);
    }

    /**
     * Whether the pointer reached a body cell this frame - which a frame the list moved on answers no to,
     * however the reading changed. An arrival is the player reaching something, and rows carried under a
     * parked cursor were reached by nobody; a wheel spun down a long list would otherwise tick once for every
     * row it swept past, where the scroll answers for the whole movement in one sound. One act, one sound,
     * which is also the honest reading - the player turned the wheel once.
     *
     * <p>The latch still takes what is now under the cursor rather than being skipped, so the frame after a
     * scroll is an ordinary frame again: the pointer moving onto that same cell later is an arrival like any
     * other, and the cell it was on before the list moved cannot announce itself as the list settles.
     *
     * <p>Whether the list moved is this end's own to know - it is the end that moved it - so no caller hands
     * that in, and none can forget to.
     *
     * @param hoveredSlot the slot the pointer is on this frame, or null when it is on no body cell
     * @return true on the frame the pointer arrives on a cell, by its own movement
     */
    boolean detectBodyCellArrivalAt(BodyCellSlot hoveredSlot) {

        if (takeHasListScrolledSinceLastFrame()) {
            bodyHoverArrival.adoptArrivalAt(hoveredSlot);
            return false;
        }
        return bodyHoverArrival.detectArrivalAt(hoveredSlot);
    }

    /**
     * How far onto its hovered look the body cell at a given slot currently stands. A bare fraction, so this
     * end holds no colour: what the lift is made of - a blend, a wash, a brightened frame - is the widget's
     * own paint, resolved where its style is.
     *
     * <p>The fraction {@link #getBodyHoverSource()} is bound over, and the terms the fades are actually keyed
     * in - which is what makes it the reachable end for pinning that a slot's two halves are not crossed.
     *
     * @param slot the body cell being asked about
     * @return its hover fraction, 0 fully at rest and 1 fully on its hovered look
     */
    float resolveBodyHoverFractionAt(BodyCellSlot slot) {
        return bodyHoverFades.resolveHoverFractionAt(slot);
    }

    /**
     * Drops every motion the body holds and reports the pointer's leave, for a panel that stops showing.
     *
     * <p>A fade or a lift left part-way would otherwise be the first thing the next session paints and then
     * wind down, showing the player the tail of an interaction they never saw begin. What was announced is
     * forgotten with them, so a panel re-opening under a still pointer answers that cell afresh - it is an
     * arrival to the player, the strip not having been there a moment ago. And the host answering the hover
     * hears the leave, since no further frame will resolve a reading to tell it with.
     */
    void resetBodyInputMotions() {
        bodyHoverFades.resetFades();
        resetBodyPressPulses();
        bodyHoverArrival.resetArrival();
        bodyHoverReporter.reportHoverCleared();

        // And the scroll the latch above would otherwise have adopted on. A movement left unread would make
        // the next session's first frame take its cell in silence, which is the one thing the resets just
        // above exist to prevent.
        resetListScrolled();
    }

    /**
     * Steps every press lift the body is carrying by a frame's worth of time, spent with the body's other
     * motions by the frame pass above. Ungated, unlike a fade: a press is an event already seen, so its
     * cycle runs out wherever the pointer went afterwards and whatever the panel did next.
     *
     * <p>The pace arrives with the frame rather than being named here, so a body's presses run at whatever
     * rhythm the rest of the panel does - a panel answering input at two speeds reads as two panels.
     *
     * <p>A lift is spent only by being advanced, so a holder that never calls this keeps every lift its
     * presses started. That is the obligation this end cannot check for itself, and the reason a panel
     * whose presses are never stepped shows none of them rather than showing them stuck.
     *
     * @param elapsedSeconds real time since the last frame the host drew
     * @param durations      how long the rise and the fall each take; a non-positive one snaps that way
     */
    void advanceBodyPressPulses(float elapsedSeconds, TraverseDurations durations) {
        bodyPressPulses.advanceByElapsedTime(elapsedSeconds, durations);
    }

    /**
     * How far through its press lift the body cell at a given slot currently stands. A bare fraction, so
     * this end holds no colour: what the lift is made of - a light added over whatever the cell already
     * shows - is the widget's own paint, resolved where its style is.
     *
     * @param slot the body cell being asked about
     * @return its press fraction, 0 with no lift running on it and 1 at a lift's peak
     */
    float resolveBodyPressFractionAt(BodyCellSlot slot) {
        return bodyPressPulses.resolvePulseFractionAt(slot);
    }

    /**
     * Drops every press lift the body is carrying, dropped with the body's other motions when a panel stops
     * showing - so a lift left part-way through its cycle cannot be the first thing the next session paints,
     * decaying from a peak the player never saw rise.
     */
    void resetBodyPressPulses() {
        bodyPressPulses.resetPulses();
    }

    /**
     * Answers a left press on the body: sounds and lifts it where it reached a control, fires that control's
     * action when the cell is one worth acting on, and reports which cell fired. Resolves the press through
     * {@link ControlHitResolver#resolveHitBodyCell} and offers what comes back to {@link
     * ControlActivation#activateCellIfActionable}, so the geometry a press acts on is the geometry every
     * reader of that resolver answers, and the narrowing that decides whether it acts is stated once, apart
     * from the geometry it narrows.
     *
     * <p>Both answers hang off the resolve and not off the firing, so a press that lands on an inert cell
     * sounds and lifts like the press it was. That case - a re-press on a lit segment - is the one press with
     * nothing else to show for it, the screen answering it with no change at all, so hanging either answer on
     * the action would leave the panel's only unexplained press as its only unanswered one. Chrome stays
     * quiet by the same rule rather than by a second one: the resolver reports no cell on a border, on blank
     * body, on a caption or on a divider, and a press that reached nothing has nothing to answer for.
     *
     * <p>On the way down, the body's controls acting on the way down - a box is ticked and a fold is already
     * moving by the time the button comes up. A tab's lift is held until the release and so sounds there;
     * this end holds nothing, so there is no release to plumb and the lift times its own fall.
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

        var hitCell = ControlHitResolver.resolveHitBodyCell(placement, pointX, pointY);
        if (hitCell == null) {
            return null;
        }
        sounds.soundPress();

        // The seen half of the same answer, off the same cell and beside the heard one. Started rather than
        // held: the control has already acted, so there is nothing for a release to end.
        bodyPressPulses.startPulseAt(hitCell.slot());

        var firedCell = ControlActivation.activateCellIfActionable(
            hitCell.control(),
            hitCell.slot().cell());

        if (firedCell == ControlHitResolver.NO_CELL_RESOLVED) {
            return null;
        }
        return hitCell;
    }

    // Starts a scrollbar drag when a left press lands on the grab column, reporting whether it did. The
    // grab column is the gutter right of the list, wider than the thin track so it need not be hit exactly;
    // a press on the thumb records its offset from the thumb centre so the thumb stays under the cursor,
    // while a press on the bare track jumps the thumb to the pointer at once. Only fires while there is a
    // bar on screen: a list that fits has none, and neither has a panel whose host set the bar to no width
    // at all - so the column claims nothing there and the press goes on to whatever control is under it.
    private boolean beginThumbDragIfPressed(InputEventAPI event, PanelPlacement placement) {
        if (!placement.isScrollbarDrawn()) {
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
    // held. A frame where the bar has gone - the list stopped overrunning, or its host took the thickness
    // away - carries the drag without moving anything, so the release still ends it where the player let go
    // rather than leaving a held thumb behind. The same reading that began the drag, so a bar cannot be
    // grabbable by one question and draggable by another.
    private void continueThumbDrag(InputEventAPI event, PanelPlacement placement) {
        if (event.isLMBUpEvent()) {
            isDraggingThumb = false;
            event.consume();
            return;
        }
        if (placement.isScrollbarDrawn()) {
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
    //
    // Gated on the list overrunning rather than on there being a bar drawn, deliberately: the wheel is how a
    // player who has set the bar away moves the list at all, so taking it with the bar would leave that list
    // unreachable.
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
            sounds.soundListScroll();
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

}
