package kmlib.starsector.ui.widgets;

/**
 * The footprint a consumer needs the {@link TabPanel} to frame beneath its tab row, or
 * {@link #NONE} for a tab that opens no body. The panel owns the frame and the header but not
 * the body's contents, so the consumer measures how large its body must be and hands that size in;
 * the panel places a body rectangle of this size and the consumer fills it. Keeping the size an
 * input (rather than the panel measuring the body itself) is what lets the panel stay agnostic to
 * what a body holds.
 *
 * @param width  the body's width, already including whatever internal padding the consumer wants
 * @param height the body's height
 */
public record TabPanelBodySize(float width, float height) {
    /** The absent body: a tab with no body reserves no framed region beneath its tab row. */
    public static final TabPanelBodySize NONE = new TabPanelBodySize(0f, 0f);

    /**
     * @return whether this size reserves no body, so the panel frames the tab row alone
     */
    public boolean isEmpty() {
        return width <= 0f || height <= 0f;
    }
}
