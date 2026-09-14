package kmlib.starsector.ui.input;

import org.lwjgl.input.Mouse;

/**
 * The {@link PointerButtonHold} a running game has: LWJGL's mouse, polled directly.
 *
 * <p>Answers "not held" wherever there is no mouse to ask. LWJGL refuses a button query outright
 * before the game has created one, and this is read per frame, so the refusal would surface as a
 * throw inside whatever poll asked it. Nothing is being held on such a frame either, so the quiet
 * answer costs nothing it could have reported.
 */
public final class VanillaPointerButtonHold implements PointerButtonHold {

    // LWJGL numbers the buttons from the left. The game's own map widgets poll the same pair of
    // indices, 0 against a left-button mode and 1 against a right-button one.
    private static final int LEFT_BUTTON_INDEX = 0;

    @Override
    public boolean isLeftButtonHeld() {
        return Mouse.isCreated() && Mouse.isButtonDown(LEFT_BUTTON_INDEX);
    }
}
