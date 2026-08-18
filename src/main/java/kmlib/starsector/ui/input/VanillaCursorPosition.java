package kmlib.starsector.ui.input;

/**
 * The {@link CursorPosition} a running game has: LWJGL's mouse, rescaled into UI units.
 *
 * <p>Nothing but the binding. The rescaling itself stays on {@link UiCursor}, where it is reachable
 * without an instance for the callers that predate this port and are hit-testing boxes they laid
 * out themselves.
 */
public final class VanillaCursorPosition implements CursorPosition {

    @Override
    public float getUiX() {
        return UiCursor.getUiX();
    }

    @Override
    public float getUiY() {
        return UiCursor.getUiY();
    }
}
