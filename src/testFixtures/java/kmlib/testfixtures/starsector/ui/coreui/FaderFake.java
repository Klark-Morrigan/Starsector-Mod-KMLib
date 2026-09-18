package kmlib.testfixtures.starsector.ui.coreui;

/**
 * A component's fade state, answering the two contracts a by-name reach takes off it: {@code
 * isFadedOut}, for whether the component is still on screen at all, and {@code getBrightness}, for how
 * far through its own fade it stands. Published as a fixture variant so both KMLib's and consuming mods' tests can
 * stand up a widget the game has taken down, or one still on its way.
 *
 * <p>The two resting states pair the answers the way a real fader does - gone reads as no brightness,
 * present as full - so a test that cares about only one of them says nothing about the other by
 * accident. A fade in progress is its own state rather than a setting of those two, since it is the
 * one case where the component is emphatically not gone and emphatically not fully there.
 */
public final class FaderFake {

    // What a component that has finished fading out and one at rest on screen report.
    private static final float FADED_OUT = 0f;
    private static final float FULLY_SHOWN = 1f;

    private final boolean isFadedOut;
    private final float brightness;

    /**
     * A fader at rest, either gone or fully shown.
     *
     * @param isFadedOut whether the component has finished fading out
     */
    public FaderFake(boolean isFadedOut) {
        this(isFadedOut, isFadedOut ? FADED_OUT : FULLY_SHOWN);
    }

    private FaderFake(boolean isFadedOut, float brightness) {
        this.isFadedOut = isFadedOut;
        this.brightness = brightness;
    }

    /**
     * A fade still running: on screen, and part way through arriving or leaving.
     *
     * @param brightness how far through the fade it stands, 0..1
     * @return a fader reporting it as still showing at that brightness
     */
    public static FaderFake createMidFade(float brightness) {
        return new FaderFake(false, brightness);
    }

    public float getBrightness() {
        return brightness;
    }

    public boolean isFadedOut() {
        return isFadedOut;
    }
}
