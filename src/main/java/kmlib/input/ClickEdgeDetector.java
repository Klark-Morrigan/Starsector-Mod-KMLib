package kmlib.input;

/**
 * Turns a per-frame "is the button down" sample into a single press event, so a held button
 * fires once rather than every frame it stays down. Made for polled input with no discrete
 * event to consume - a control that reads the raw mouse each render frame treats the rising
 * edge (up last sample, down this one) as the click.
 */
public final class ClickEdgeDetector {
    private boolean wasButtonDown;

    /**
     * Feeds this frame's button state and reports whether it just went down.
     *
     * @param isButtonDown whether the polled button is down this frame
     * @return true only on the frame the button transitions from up to down
     */
    public boolean detectPress(boolean isButtonDown) {
        var isPress = isButtonDown && !wasButtonDown;
        wasButtonDown = isButtonDown;
        return isPress;
    }
}
