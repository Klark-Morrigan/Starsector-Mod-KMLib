package kmlib.starsector.ui.debug;

/**
 * One of the four screen corners a {@link DebugHud} stacks its lines into. A caller pushes an
 * output to the corner it wants it read from, keeping unrelated debug streams apart rather than
 * interleaving into one column.
 *
 * <p>The two halves each corner belongs to drive its layout: the top halves stack downward from the
 * top edge, the bottom halves upward from the bottom, and the left halves right-align their text
 * toward the screen centre (the right halves left-align), so the two sides read as mirrored columns
 * flanking the centreline.
 */
public enum DebugQuadrant {

    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_RIGHT,
    BOTTOM_LEFT;

    /**
     * @return whether this corner is on the left half, where text is right-aligned toward the centre
     */
    public boolean isLeftHalf() {
        return this == TOP_LEFT || this == BOTTOM_LEFT;
    }

    /**
     * @return whether this corner is on the top half, where lines stack downward from the top edge
     */
    public boolean isTopHalf() {
        return this == TOP_LEFT || this == TOP_RIGHT;
    }
}
