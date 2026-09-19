package kmlib.testfixtures.starsector.ui.coreui;

/**
 * An interaction dialog that stands up its own core UI, answering the {@code getCoreUI} contract a
 * by-name reach takes to get at it. Published as a fixture variant so both KMLib's and consuming mods' tests
 * drive the dialog-hosted core the same way.
 *
 * <p>Models only the hosting side of that contract. A dialog exposing no such method is what the
 * reach reads as hosting no core UI, and any object at all already stands for one - giving this
 * fixture a "hosts nothing" mode would model it as a host holding null, which the reach cannot tell
 * apart from a host it never asked.
 */
public final class CoreUiHostFake {
    private final Object coreUi;

    public CoreUiHostFake(Object coreUi) {
        this.coreUi = coreUi;
    }

    public Object getCoreUI() {
        return coreUi;
    }
}
