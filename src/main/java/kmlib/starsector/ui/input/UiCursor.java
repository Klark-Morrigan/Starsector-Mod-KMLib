package kmlib.starsector.ui.input;

import com.fs.starfarer.api.Global;

import org.lwjgl.input.Mouse;

/**
 * Reports the mouse position in UI coordinates. LWJGL's {@link Mouse} gives the cursor in raw
 * screen pixels from the bottom-left; UI layout works in UI units off the same origin, so the
 * two only line up after rescaling pixels by the UI-to-pixel ratio. A control that draws in UI
 * coordinates and hit-tests the raw mouse needs this so the region it draws is the region it
 * tests.
 *
 * <p>{@link #getUiX()} / {@link #getUiY()} touch the LWJGL and settings statics and are
 * exercised in-engine; the scaling itself is {@link #convertPixelToUi}, kept pure so the
 * conversion (including the no-display guard) is verifiable on its own.
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
        var settings = Global.getSettings();
        return convertPixelToUi(Mouse.getX(), settings.getScreenWidth(),
                settings.getScreenWidthPixels());
    }

    /**
     * @return the cursor's y in UI coordinates, or a negative value when there is no display
     *         (zero pixel height)
     */
    public static float getUiY() {
        var settings = Global.getSettings();
        return convertPixelToUi(Mouse.getY(), settings.getScreenHeight(),
                settings.getScreenHeightPixels());
    }

    /**
     * Rescales a raw pixel coordinate into UI units along one axis.
     *
     * @param rawPixel  the coordinate in screen pixels
     * @param uiSize    the axis length in UI units
     * @param pixelSize the axis length in screen pixels
     * @return the coordinate in UI units, or {@code -1} when {@code pixelSize} is non-positive
     */
    public static float convertPixelToUi(float rawPixel, float uiSize, float pixelSize) {
        if (pixelSize <= 0f) {
            return -1f;
        }
        return rawPixel * uiSize / pixelSize;
    }
}
