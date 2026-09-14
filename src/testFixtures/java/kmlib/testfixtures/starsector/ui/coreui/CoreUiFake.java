package kmlib.testfixtures.starsector.ui.coreui;

/**
 * A core UI that answers the {@code getCurrentTab} and {@code getFader} contracts, so a walk from
 * the campaign down to whichever screen is up can be driven without a running game. Shipped from
 * KMLib so both KMLib's and consuming mods' tests stand up the same shape of core.
 *
 * <p>Holds the tab as a plain object rather than a widget, because a walk that reaches it goes on
 * by name from there; a test supplies whatever it needs the tab to answer next.
 *
 * <p>The dismissed mode is the shape a dialog leaves behind when the player closes a screen it
 * opened: the panel is faded out and taken off the screen, while the core goes on naming the tab it
 * was last showing. Modelled because that pairing is what tells a reader of one from a reader of
 * both - a core with a stale tab is only misleading to a caller that does not ask whether it is
 * still up.
 */
public final class CoreUiFake {
    private final FaderFake faderFake;
    private final Object currentTab;

    /** A core UI on screen, showing this tab. */
    public CoreUiFake(Object currentTab) {
        this(currentTab, new FaderFake(false));
    }

    private CoreUiFake(Object currentTab, FaderFake faderFake) {
        this.currentTab = currentTab;
        this.faderFake = faderFake;
    }

    /**
     * @param currentTab the tab it goes on naming after being taken off the screen
     * @return a core UI the screen it belonged to has taken down
     */
    public static CoreUiFake createDismissed(Object currentTab) {
        return new CoreUiFake(currentTab, new FaderFake(true));
    }

    public Object getCurrentTab() {
        return currentTab;
    }

    public FaderFake getFader() {
        return faderFake;
    }
}
