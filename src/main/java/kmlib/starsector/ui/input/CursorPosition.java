package kmlib.starsector.ui.input;

/**
 * Where the pointer is, in the UI units a layout is measured in.
 *
 * <p>A role rather than a static call so a rule that compares the cursor against a box can be
 * driven without a display. {@link UiCursor} reads LWJGL's mouse and the game's screen metrics,
 * neither of which exists outside a running game, and a rule that reached for it directly would
 * work only inside one - which is where a containment rule is hardest to see failing.
 *
 * <p>Two reads rather than one point, matching what the live source has to offer: the axes are
 * rescaled independently from raw pixels, so a value type here would be assembled from these two
 * either way and would only add a shape for a caller to unpack again.
 */
public interface CursorPosition {

    /**
     * @return the cursor's x in UI units, measured from the left edge
     */
    float getUiX();

    /**
     * @return the cursor's y in UI units, measured from the bottom edge
     */
    float getUiY();
}
