package kmlib.starsector.ui.input;

import com.fs.starfarer.api.input.InputEventAPI;

import kmlib.starsector.ui.widgets.PanelPlacement;
import kmlib.starsector.ui.widgets.scroll.PanelScrollbars;
import kmlib.starsector.ui.widgets.scroll.ScrollState;

/**
 * Moves one panel's scrolling list in answer to the pointer, and holds where it stands: the wheel over the
 * list, the thumb drag (grab, follow, release), the {@link ScrollState} both of them write and the layout
 * reads, and whether either moved the list since a frame last asked.
 *
 * <p>The list's movement is its own end because nothing else on a panel shares its state. What a body cell
 * is doing - its fade, its lift, the pointer arriving on it - is keyed by the place under the pointer and
 * answers to where the pointer is; this answers to where the <em>list</em> is, and the one thing the two
 * exchange is the latch below, which a cell's arrival reads so rows carried under a still cursor announce
 * nobody reaching them. Held apart, a reader of either half meets only that half's state, and a reader of
 * the latch finds it beside the two acts that set it rather than among motions that never touch it.
 *
 * <p>One of these per panel, owned by the panel's controller: it is the piece of that controller the wheel
 * and a drag reach, not a second controller a host wires. The two acts it answers are gated on different
 * readings of the same placement, and deliberately so. A drag needs a bar to grab, so it asks whether one is
 * drawn; the wheel needs only somewhere to scroll to, so it asks whether the list overruns - the wheel being
 * how a list whose host drew no bar is moved at all, gating it on the bar would strand that list.
 *
 * <p>Two moments this end answers audibly and one it does not: the wheel moving the list sounds, and a
 * drag moving it stays silent. A drag is one held act carrying the list continuously with the pointer off
 * on the scrollbar, so per-frame ticks would be exactly the chatter the wheel's single sound exists to
 * avoid. Both report through the latch, because what the latch answers is that content moved and not what
 * moved it.
 */
final class PanelScrollController {

    // Pixels one wheel notch scrolls the flex list. Only the wheel's sign is read (like the vanilla scroll
    // lists), so each notch moves this fixed step regardless of the raw wheel magnitude - about two list
    // rows, a comfortable step without overshooting a short list.
    private static final float SCROLL_STEP_PX = 40f;

    // This panel's scroll position, read by the layout and written by the wheel and by a drag.
    private final ScrollState scrollState = new ScrollState();

    // What this panel sounds like in answer to the list moving under the wheel. The one moment here with a
    // sound to it, and the choice of sound stays the look's - this end names the moment alone.
    private final PanelSounds sounds;

    // A scrollbar-thumb drag in progress, and the pointer's offset from the thumb centre when grabbed. The
    // drag spans frames (press, moves, release), so it lives as state between events: while set, every
    // mouse move maps the pointer to a scroll position; the grab offset holds the thumb under the cursor so
    // it does not jump when grabbed off-centre.
    private boolean isDraggingThumb;
    private float thumbGrabOffsetY;

    // Whether the list has moved since a frame last asked. Latched rather than worked out from the offset,
    // because the offset a frame reads says where the list is and never how it got there - and the only end
    // that knows a move happened at all is the one that made it.
    private boolean hasListScrolledSinceLastFrame;

    /**
     * @param sounds what the panel sounds like in answer to the moments it detects - handed in whole rather
     *               than assembled here, so the list sounds by the same look as the controls beside it
     */
    PanelScrollController(PanelSounds sounds) {
        this.sounds = sounds;
    }

    /**
     * @return this panel's scroll position, for the layout to read (the requested offset) and settle
     *         (clamp to the overflow) each frame
     */
    ScrollState getScrollState() {
        return scrollState;
    }

    /**
     * Ends any in-progress thumb drag, for a panel that stops showing so a drag left dangling cannot hijack
     * the next session.
     */
    void cancelDrag() {
        isDraggingThumb = false;
    }

    /**
     * Follows a thumb drag already in progress, reporting whether there was one to follow. A held drag owns
     * the event wherever the pointer is - even past the panel edge - so the list keeps following the cursor
     * until the release rather than dropping the drag the moment the pointer leaves the narrow scrollbar
     * column; the caller therefore asks this before it asks whether the event is over the panel at all.
     *
     * <p>The release ends the drag, and until then every move maps the pointer to a scroll position. Either
     * way the event is consumed, so the surface behind neither pans nor acts while the thumb is held. A
     * frame where the bar has gone - the list stopped overrunning, or its host took the thickness away -
     * carries the drag without moving anything, so the release still ends it where the player let go rather
     * than leaving a held thumb behind. That is the same reading that began the drag, so a bar cannot be
     * grabbable by one question and draggable by another.
     *
     * @param event     the pointer event
     * @param placement the laid-out panel the renderer drew this frame
     * @return whether a drag was in progress and took the event
     */
    boolean continueDragIfHeld(InputEventAPI event, PanelPlacement placement) {
        if (!isDraggingThumb) {
            return false;
        }
        if (event.isLMBUpEvent()) {
            isDraggingThumb = false;
            event.consume();
            return true;
        }
        if (placement.isScrollbarDrawn()) {
            updateDragOffset(placement, event.getY());
        }
        event.consume();
        return true;
    }

    /**
     * Starts a thumb drag when a left press lands on the grab column, reporting whether it did. The grab
     * column is the gutter right of the list, wider than the thin track so it need not be hit exactly; a
     * press on the thumb records its offset from the thumb centre so the thumb stays under the cursor, while
     * a press on the bare track jumps the thumb to the pointer at once.
     *
     * <p>Only fires while there is a bar on screen: a list that fits has none, and neither has a panel whose
     * host set the bar to no width at all - so the column claims nothing there and the press is left to
     * whatever control is under it.
     *
     * @param event     the left press
     * @param placement the laid-out panel the renderer drew this frame
     * @return whether the press grabbed the scrollbar
     */
    boolean beginThumbDragIfPressed(InputEventAPI event, PanelPlacement placement) {
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

    /**
     * Scrolls the flex list when the wheel turns over its scroll region and it has somewhere to scroll, and
     * sounds the movement. Only the wheel's sign is read (like the vanilla scroll lists): a wheel up scrolls
     * toward the list top, so it decreases the offset, and a wheel down increases it, each by one fixed
     * step. Off the scroll region - over a pinned control, or a list that fits - the wheel does nothing,
     * though the caller still consumes it so the surface behind does not act.
     *
     * <p>Gated on the list overrunning rather than on there being a bar drawn, deliberately: the wheel is
     * how a player whose bar has been set away moves the list at all, so taking it with the bar would leave
     * that list unreachable.
     *
     * @param event     the wheel event
     * @param placement the laid-out panel the renderer drew this frame
     */
    void scrollListUnderPointer(InputEventAPI event, PanelPlacement placement) {
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

    /**
     * Reports whether the list has moved since this was last asked, and forgets it - read by the arrival a
     * body cell detects, which has to tell rows carried under a still pointer from a pointer moving over
     * rows. Cleared by the reading, so one movement is answered by the first frame after it and by that
     * frame alone; a movement made while nothing is drawing waits for the next frame rather than being
     * dropped.
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
     * frame answers the pointer where it is rather than silently taking whatever is under it on the strength
     * of a scroll from a session the player has since left.
     */
    void resetListScrolled() {
        hasListScrolledSinceLastFrame = false;
    }

    // Maps the dragged pointer to an absolute scroll offset along the track and stores it, holding the
    // thumb the grab offset below the cursor so it tracks the drag rather than snapping its centre to the
    // pointer. Noted and not sounded: the sound belongs to the wheel alone, while the rows a drag sweeps
    // past the cursor still have to reach whatever answers arrivals.
    private void updateDragOffset(PanelPlacement placement, float pointerY) {

        var offsetBeforeDrag = scrollState.getOffset();
        scrollState.setOffset(
            PanelScrollbars.resolveOffsetForPointer(placement, pointerY - thumbGrabOffsetY));
        recordListMovedFrom(offsetBeforeDrag);
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
