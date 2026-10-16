package kmlib.starsector.ui.modal;

import com.fs.starfarer.api.ui.CustomPanelAPI;

/**
 * The widgets standing in a {@link ModalPanel}, and the fills painted beneath them.
 *
 * <p>The panel holds the surface and the body holds what stands on it, so a body is replaced whole
 * on every change without the panel publishing what it keeps. The fills are the body's because the
 * game publishes a rectangle component that strokes and none that fills: the dim behind the box and
 * the box's own surface are painted through the panel's render hook rather than built as widgets.
 */
public interface ModalPanelBody {

    /**
     * Takes this body back off the panel it was built into.
     *
     * @param modalPanel the panel it was built into
     */
    void removeFromPanel(CustomPanelAPI modalPanel);

    /**
     * Paints the fills beneath every widget the panel holds.
     *
     * @param alphaMult how far through its own fade the panel stands, which the fills honour so the
     *                  panel arrives and leaves as one piece
     */
    void renderFills(float alphaMult);
}
