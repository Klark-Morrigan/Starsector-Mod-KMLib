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
 * <p>{@link #getUiX()} / {@link #getUiY()} touch the LWJGL mouse and the live screen
 * ({@link VanillaScreen}, which states which of its two axis pairs is which) and are exercised
 * in-engine; the scaling itself is {@link #convertPixelToUi}, kept pure so the conversion
 * (including the no-display guard) is verifiable on its own.
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
        return convertPixelToUi(
            Mouse.getX(),
            VanillaScreen.resolveUiWidth(),
            VanillaScreen.resolvePixelWidth());
    }

    /**
     * @return the cursor's y in UI coordinates, or a negative value when there is no display
     *         (zero pixel height)
     */
    public static float getUiY() {
        return convertPixelToUi(
            Mouse.getY(),
            VanillaScreen.resolveUiHeight(),
            VanillaScreen.resolvePixelHeight());
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

    /**
     * Rescales a UI-unit coordinate back into screen pixels along one axis - the inverse of {@link
     * #convertPixelToUi}. A pass drawing in UI coordinates needs this to hand a rectangle to a
     * pixel-space GL call (a scissor clip), which operates in raw framebuffer pixels rather than the UI
     * projection the layout works in.
     *
     * @param uiCoordinate the coordinate in UI units
     * @param uiSize       the axis length in UI units
     * @param pixelSize    the axis length in screen pixels
     * @return the coordinate in screen pixels, or {@code -1} when {@code uiSize} is non-positive
     */
    public static float convertUiToPixel(float uiCoordinate, float uiSize, float pixelSize) {
        if (uiSize <= 0f) {
            return -1f;
        }
        return uiCoordinate * pixelSize / uiSize;
    }
}
