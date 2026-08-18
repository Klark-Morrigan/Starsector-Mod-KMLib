package kmlib.testfixtures.starsector.ui.input;

import kmlib.starsector.ui.input.CursorPosition;

/**
 * A {@link CursorPosition} resting wherever a test put it, standing in for the live mouse and screen
 * metrics wherever the question is only which side of a box the pointer is on.
 *
 * <p>Ships in the main jar rather than a test source set, so a consuming mod's own tests can drive
 * their hit-testing through this seam without rebuilding the fixture - which is the point of the
 * role being here at all, since what a cursor position decides is decided in the mods that read one.
 *
 * <p>Starts off screen at negative coordinates, which is what the live read answers with no display,
 * so a rule driven by a fixture no case has moved is asked about a pointer that is nowhere rather
 * than one resting on the origin.
 */
public final class CursorPositionFake implements CursorPosition {

    // The live read's own answer when there is no display to measure against, so an unmoved fixture
    // stands for a pointer that is nowhere rather than for one at the bottom-left corner.
    private static final float OFF_SCREEN = -1f;

    private float uiX = OFF_SCREEN;
    private float uiY = OFF_SCREEN;

    @Override
    public float getUiX() {
        return uiX;
    }

    @Override
    public float getUiY() {
        return uiY;
    }

    /**
     * Puts the pointer somewhere, in the same UI units a layout is measured in.
     *
     * @param uiX the cursor's x, from the left edge
     * @param uiY the cursor's y, from the bottom edge
     */
    public void restCursorAt(float uiX, float uiY) {
        this.uiX = uiX;
        this.uiY = uiY;
    }
}
