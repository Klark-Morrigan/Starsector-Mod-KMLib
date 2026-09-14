package kmlib.starsector.ui.input;

import kmlib.starsector.ui.screen.VanillaScreen;

import org.lwjgl.input.Mouse;

/**
 * Reports the mouse position in UI coordinates. LWJGL's {@link Mouse} gives the cursor in raw
 * screen pixels from the bottom-left; UI layout works in UI units off the same origin, so the
 * two only line up after rescaling pixels by the UI-to-pixel ratio. A control that draws in UI
 * coordinates and hit-tests the raw mouse needs this so the region it draws is the region it
 * tests.
 *
 * <p>The rescale itself belongs to the axis being rescaled along, not here: each read asks
 * {@link kmlib.starsector.ui.screen.ScreenAxis} to convert on the axis it took the pixel from, so
 * an x cannot be rescaled by the screen's height. What is left here is the pair of live reads, the
 * LWJGL mouse and the screen behind them being why this runs only in-engine.
 */
public final class UiCursor {
    private UiCursor() {
    }

    /**
     * @return the cursor's x in UI coordinates, or a negative value when there is no display
     *         (zero pixel width) so a hit-test parks off every region rather than dividing by
     *         zero
     */
    public static float getUiX() {
        return VanillaScreen.resolveXAxis().convertPixelToUi(Mouse.getX());
    }

    /**
     * @return the cursor's y in UI coordinates, or a negative value when there is no display
     *         (zero pixel height)
     */
    public static float getUiY() {
        return VanillaScreen.resolveYAxis().convertPixelToUi(Mouse.getY());
    }
}
