package kmlib.starsector.ui.input;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventClass;
import com.fs.starfarer.api.input.InputEventType;

/**
 * One real pointer event, answering every question about itself truthfully except where the pointer is:
 * it reports a position no widget covers. For handing to a screen that must hear the pointer moved but
 * must not find it over anything of its own.
 *
 * <p>The problem it exists for: a mod panel drawn over a vanilla screen consumes the events that land on
 * it, so the screen underneath neither acts on them nor learns of them - and a vanilla control hovered a
 * moment before the pointer crossed onto the panel goes on being drawn lit, because nothing has told it
 * the pointer is elsewhere. Consuming is a claim on what an event *does*; it was never meant to be a claim
 * on what the screen may *know*.
 *
 * <p>Parking rather than passing the event through is what keeps that from trading one wrong hover for
 * another. Passed through, the position is real and lands on whatever sits behind the panel, which then
 * lights up beneath it; parked, no widget contains it, so every control drops its hover and none takes
 * one. The position is the whole of the difference - every other answer is the event's own, so a caller
 * reading a modifier, a button, or an event class off it reads the truth.
 *
 * <p>It reports itself unconsumed whatever the event it wraps has answered, because it is handed on in
 * place of an event that was claimed: the claim was on the original, and this is what the screen behind is
 * being given instead of nothing.
 */
public final class ParkedPointerEvent implements InputEventAPI {

    // Off every widget on the screen: UI coordinates start at the bottom-left corner, so a negative pair
    // is contained by nothing whatever is on screen and needs no knowledge of what is.
    private static final int PARKED_POSITION = -1;

    // The event this stands in for, and the source of every answer but the position.
    private final InputEventAPI event;

    // Whether this stand-in has itself been claimed. Held apart from the wrapped event's own flag: the
    // original was claimed by the panel, and that claim is what this exists to hand on past.
    private boolean isConsumed;

    /**
     * @param event the real event this stands in for
     */
    public ParkedPointerEvent(InputEventAPI event) {
        this.event = event;
    }

    /**
     * @return a position no widget contains, which is the whole of what this type changes
     */
    @Override
    public int getX() {
        return PARKED_POSITION;
    }

    /**
     * @return a position no widget contains, which is the whole of what this type changes
     */
    @Override
    public int getY() {
        return PARKED_POSITION;
    }

    @Override
    public int getEventValue() {
        return event.getEventValue();
    }

    @Override
    public int getDX() {
        return event.getDX();
    }

    @Override
    public int getDY() {
        return event.getDY();
    }

    @Override
    public InputEventClass getEventClass() {
        return event.getEventClass();
    }

    @Override
    public void logEvent() {
        event.logEvent();
    }

    @Override
    public boolean isConsumed() {
        return isConsumed;
    }

    @Override
    public void consume() {
        isConsumed = true;
    }

    @Override
    public boolean isRepeat() {
        return event.isRepeat();
    }

    @Override
    public InputEventType getEventType() {
        return event.getEventType();
    }

    @Override
    public boolean isMouseEvent() {
        return event.isMouseEvent();
    }

    @Override
    public boolean isKeyboardEvent() {
        return event.isKeyboardEvent();
    }

    @Override
    public boolean isKeyUpEvent() {
        return event.isKeyUpEvent();
    }

    @Override
    public boolean isKeyDownEvent() {
        return event.isKeyDownEvent();
    }

    @Override
    public boolean isMouseUpEvent() {
        return event.isMouseUpEvent();
    }

    @Override
    public boolean isMouseDownEvent() {
        return event.isMouseDownEvent();
    }

    @Override
    public boolean isLMBDownEvent() {
        return event.isLMBDownEvent();
    }

    @Override
    public boolean isLMBEvent() {
        return event.isLMBEvent();
    }

    @Override
    public boolean isRMBEvent() {
        return event.isRMBEvent();
    }

    @Override
    public boolean isLMBUpEvent() {
        return event.isLMBUpEvent();
    }

    @Override
    public boolean isRMBDownEvent() {
        return event.isRMBDownEvent();
    }

    @Override
    public boolean isRMBUpEvent() {
        return event.isRMBUpEvent();
    }

    @Override
    public boolean isMouseMoveEvent() {
        return event.isMouseMoveEvent();
    }

    @Override
    public boolean isMouseScrollEvent() {
        return event.isMouseScrollEvent();
    }

    @Override
    public char getEventChar() {
        return event.getEventChar();
    }

    @Override
    public boolean isAltDown() {
        return event.isAltDown();
    }

    @Override
    public boolean isCtrlDown() {
        return event.isCtrlDown();
    }

    @Override
    public boolean isShiftDown() {
        return event.isShiftDown();
    }

    @Override
    public boolean isUnmodified() {
        return event.isUnmodified();
    }

    @Override
    public boolean isDoubleClick() {
        return event.isDoubleClick();
    }

    @Override
    public boolean isModifierKey() {
        return event.isModifierKey();
    }

    @Override
    public boolean isControlDownEvent(String controlId) {
        return event.isControlDownEvent(controlId);
    }

    @Override
    public boolean isControlUpEvent(String controlId) {
        return event.isControlUpEvent(controlId);
    }

    @Override
    public boolean isControlActivated(String controlId) {
        return event.isControlActivated(controlId);
    }
}
