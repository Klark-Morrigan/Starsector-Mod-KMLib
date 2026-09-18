package kmlib.testfixtures.starsector.ui.coreui;

/**
 * A modal dialog raised over the core UI, answering the two contracts that identify one: the
 * backdrop-dim accessor a modal alone carries, and the {@code getFader} state saying whether it is
 * still on screen. Published as a fixture variant so both KMLib's and consuming mods' tests stand a dialog over
 * the same shape of tree the live reads walk.
 *
 * <p>Carries a dim amount because a modal does, not because anything reads it - what identifies one
 * is that the accessor is there at all. The value is the engine's own resting default, so a fixture
 * asserted against it says what a real dialog would.
 *
 * <p>The dismissed mode is the frames between the player answering a dialog and its fade finishing,
 * during which it is still a child of the core UI. Modelled because those frames are the only thing
 * separating a reader that asks whether a dialog is still up from one that asks only whether one is
 * there. The mid-fade mode is the frames either side of that, where a reader following the dialog's
 * own fade parts company with one that only asks whether it is there.
 */
public final class ModalDialogFake {

    // What the engine dims the screen behind a modal by, at rest.
    private static final float DEFAULT_BACKDROP_DIM = 0.66f;

    private final FaderFake faderFake;

    /** A modal standing over the core UI. */
    public ModalDialogFake() {
        this(new FaderFake(false));
    }

    private ModalDialogFake(FaderFake faderFake) {
        this.faderFake = faderFake;
    }

    /**
     * @return a modal the player has answered and whose fade has finished, still held as a child
     */
    public static ModalDialogFake createDismissed() {
        return new ModalDialogFake(new FaderFake(true));
    }

    /**
     * A modal part way through arriving or leaving, which is the state anything painting against its
     * backdrop has to follow rather than snap through.
     *
     * @param brightness how far through its fade it stands, 0..1
     * @return a modal showing at that brightness
     */
    public static ModalDialogFake createMidFade(float brightness) {
        return new ModalDialogFake(FaderFake.createMidFade(brightness));
    }

    public float getBackgroundDimAmount() {
        return DEFAULT_BACKDROP_DIM;
    }

    public FaderFake getFader() {
        return faderFake;
    }
}
