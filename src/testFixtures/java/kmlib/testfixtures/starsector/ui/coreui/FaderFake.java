package kmlib.testfixtures.starsector.ui.coreui;

/**
 * A component's fade state, answering the {@code isFadedOut} contract a by-name reach takes to find
 * out whether the component is still on screen. Shipped from KMLib so both KMLib's and consuming
 * mods' tests can stand up a widget the game has taken down.
 *
 * <p>Holds the resting answer only. A fade in progress is not modelled: every read built on this
 * asks whether the component is gone, and the frames while it is still going are ones on which it
 * is not.
 */
public final class FaderFake {

    private final boolean isFadedOut;

    public FaderFake(boolean isFadedOut) {
        this.isFadedOut = isFadedOut;
    }

    public boolean isFadedOut() {
        return isFadedOut;
    }
}
