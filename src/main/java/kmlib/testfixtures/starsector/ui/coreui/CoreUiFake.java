package kmlib.testfixtures.starsector.ui.coreui;

/**
 * A core UI that answers the {@code getCurrentTab} contract, so a walk from the campaign down to
 * whichever screen is up can be driven without a running game. Shipped from KMLib so both KMLib's
 * and consuming mods' tests stand up the same shape of core.
 *
 * <p>Holds the tab as a plain object rather than a widget, because a walk that reaches it goes on
 * by name from there; a test supplies whatever it needs the tab to answer next.
 */
public final class CoreUiFake {
    private final Object currentTab;

    public CoreUiFake(Object currentTab) {
        this.currentTab = currentTab;
    }

    public Object getCurrentTab() {
        return currentTab;
    }
}
