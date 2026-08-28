package kmlib.testfixtures.starsector.ui.input;

import kmlib.starsector.ui.input.PointerButtonHold;

/**
 * A {@link PointerButtonHold} left in whatever state a test last put it, standing in for the live
 * mouse wherever the question is only what the pointer is doing this frame.
 *
 * <p>Published as a fixture variant so a consuming mod's own tests can drive their rules through
 * this seam without rebuilding the fake, which is the point of the role being here at all: what a
 * held button decides is decided in the mods that read one.
 *
 * <p>Starts released, which is what the live read answers with no mouse to ask, so a rule driven by
 * a fixture no case has touched is asked about a pointer at rest rather than one already pressing.
 */
public final class PointerButtonHoldFake implements PointerButtonHold {

    private boolean isLeftButtonHeld;

    @Override
    public boolean isLeftButtonHeld() {
        return isLeftButtonHeld;
    }

    /** Presses the left button and leaves it down. */
    public void holdLeftButton() {
        isLeftButtonHeld = true;
    }

    /** Lets the left button back up. */
    public void releaseLeftButton() {
        isLeftButtonHeld = false;
    }
}
