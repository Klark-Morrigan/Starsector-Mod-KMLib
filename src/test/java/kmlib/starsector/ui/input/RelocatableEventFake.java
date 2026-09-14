package kmlib.starsector.ui.input;

import com.fs.starfarer.api.input.InputEventAPI;
import com.fs.starfarer.api.input.InputEventClass;
import com.fs.starfarer.api.input.InputEventType;

/**
 * An input event shaped like the game's own: it carries a position and, unlike the interface every mod
 * compiles against, exposes the setters that move it.
 *
 * <p>Stands in for the game's event because the real one cannot be loaded outside the game at all - its
 * members carry names that are not legal Java identifiers, so a verifying JVM refuses it. What is being
 * tested is a reach by method name, so what it has to reproduce is the shape that reach looks for:
 * public {@code setX} and {@code setY} taking an int. A mock reproduces the interface and none of that,
 * which is why one is used here for the failing case and this for the succeeding one.
 *
 * <p>Public, and that is part of what it reproduces rather than an oversight: a public method on a
 * non-public class is not reachable by reflection from another package, so a package-private stand-in
 * would fail the reach for a reason the game's own event - a public class - never would.
 *
 * <p>Which kind of event each factory below builds is spelt the way {@link PointerEventMocks} spells it,
 * that being one fact about the input API rather than one per fixture. This exists beside those mocks
 * only because a mock carries the interface and nothing else, and what is under test here is a reach for
 * methods the interface does not declare.
 */
public final class RelocatableEventFake implements InputEventAPI {

    private final boolean isMouseMove;
    private final boolean isLeftPress;

    private boolean isConsumed;
    private int x;
    private int y;

    private RelocatableEventFake(int x, int y, boolean isMouseMove, boolean isLeftPress) {
        this.x = x;
        this.y = y;
        this.isMouseMove = isMouseMove;
        this.isLeftPress = isLeftPress;
    }

    /**
     * A pointer move at a point - the one kind of event a panel claims by moving rather than consuming.
     *
     * @param x the pointer's x, in UI coordinates
     * @param y the pointer's y, in UI coordinates
     * @return the event
     */
    public static RelocatableEventFake createMoveAt(int x, int y) {
        return new RelocatableEventFake(x, y, true, false);
    }

    /**
     * A left-button press at a point, which a panel claims by consuming.
     *
     * @param x the press x, in UI coordinates
     * @param y the press y, in UI coordinates
     * @return the event
     */
    public static RelocatableEventFake createLeftPressAt(int x, int y) {
        return new RelocatableEventFake(x, y, false, true);
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    @Override
    public boolean isMouseMoveEvent() {
        return isMouseMove;
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
    public int getEventValue() {
        return 0;
    }

    @Override
    public int getDX() {
        return 0;
    }

    @Override
    public int getDY() {
        return 0;
    }

    @Override
    public InputEventClass getEventClass() {
        return InputEventClass.MOUSE_EVENT;
    }

    @Override
    public void logEvent() {
    }

    @Override
    public boolean isRepeat() {
        return false;
    }

    @Override
    public InputEventType getEventType() {
        return InputEventType.MOUSE_MOVE;
    }

    @Override
    public boolean isMouseEvent() {
        return true;
    }

    @Override
    public boolean isKeyboardEvent() {
        return false;
    }

    @Override
    public boolean isKeyUpEvent() {
        return false;
    }

    @Override
    public boolean isKeyDownEvent() {
        return false;
    }

    @Override
    public boolean isMouseUpEvent() {
        return false;
    }

    @Override
    public boolean isMouseDownEvent() {
        return false;
    }

    @Override
    public boolean isLMBDownEvent() {
        return isLeftPress;
    }

    @Override
    public boolean isLMBEvent() {
        return false;
    }

    @Override
    public boolean isRMBEvent() {
        return false;
    }

    @Override
    public boolean isLMBUpEvent() {
        return false;
    }

    @Override
    public boolean isRMBDownEvent() {
        return false;
    }

    @Override
    public boolean isRMBUpEvent() {
        return false;
    }

    @Override
    public boolean isMouseScrollEvent() {
        return false;
    }

    @Override
    public char getEventChar() {
        return ' ';
    }

    @Override
    public boolean isAltDown() {
        return false;
    }

    @Override
    public boolean isCtrlDown() {
        return false;
    }

    @Override
    public boolean isShiftDown() {
        return false;
    }

    @Override
    public boolean isUnmodified() {
        return true;
    }

    @Override
    public boolean isDoubleClick() {
        return false;
    }

    @Override
    public boolean isModifierKey() {
        return false;
    }

    @Override
    public boolean isControlDownEvent(String controlId) {
        return false;
    }

    @Override
    public boolean isControlUpEvent(String controlId) {
        return false;
    }

    @Override
    public boolean isControlActivated(String controlId) {
        return false;
    }
}
