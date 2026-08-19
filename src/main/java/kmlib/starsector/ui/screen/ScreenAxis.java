package kmlib.starsector.ui.screen;

/**
 * One axis of the screen measured in both of the spaces a UI pass straddles at once: its length in
 * the UI units a widget is laid out in, and its length in the framebuffer pixels the mouse and a GL
 * clip are stated in.
 *
 * <p>The two travel bound together because neither converts without the other, and because a
 * conversion handed one axis's UI length with the other axis's pixel length is wrong by the ratio
 * between the axes - which is nothing at all on a square pixel scale, so the mistake survives every
 * test run on the machine that made it. Loose, the pair is two same-typed floats that transpose
 * silently; bound, there is no arrangement of them left to get wrong.
 *
 * <p>Pure arithmetic, holding no notion of where its numbers came from, so the conversions are
 * verifiable without a display. {@link VanillaScreen} is what measures a real one.
 *
 * @param uiLength    the axis length in UI units
 * @param pixelLength the axis length in framebuffer pixels
 */
public record ScreenAxis(
    float uiLength,
    float pixelLength) {

    // What a conversion answers when the axis it was asked about has no extent - no display, or a
    // window collapsed to nothing. Negative rather than zero so a converted cursor position parks
    // off every region a hit-test could ask about, instead of landing on whatever sits at the
    // origin.
    private static final float NO_EXTENT_COORDINATE = -1f;

    /**
     * Rescales a raw pixel coordinate on this axis into UI units.
     *
     * @param rawPixel the coordinate in framebuffer pixels
     * @return the coordinate in UI units, or a negative value when the axis has no pixel extent
     */
    public float convertPixelToUi(float rawPixel) {

        if (pixelLength <= 0f) {
            return NO_EXTENT_COORDINATE;
        }
        return rawPixel * uiLength / pixelLength;
    }

    /**
     * Rescales a UI-unit coordinate on this axis back into framebuffer pixels - the inverse of
     * {@link #convertPixelToUi}. A pass drawing in UI coordinates needs this to state a rectangle
     * to a pixel-space GL call, which operates in raw framebuffer pixels rather than the UI
     * projection the layout works in.
     *
     * @param uiCoordinate the coordinate in UI units
     * @return the coordinate in framebuffer pixels, or a negative value when the axis has no UI
     *         extent
     */
    public float convertUiToPixel(float uiCoordinate) {
        
        if (uiLength <= 0f) {
            return NO_EXTENT_COORDINATE;
        }
        return uiCoordinate * pixelLength / uiLength;
    }
}
