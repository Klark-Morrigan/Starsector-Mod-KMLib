package kmlib.starsector.ui.render.gl.controls;

import kmlib.starsector.ui.font.TextFace;
import kmlib.starsector.ui.layout.ControlStripLayout;
import kmlib.starsector.ui.render.gl.style.WidgetStyle;

/**
 * The look bundle plus the frame's alpha and the control's live cell paints, threaded together through
 * every draw so a helper takes one paint rather than unpacking the accents, body font, opacity, and
 * resolved washes and lights into loose arguments each time.
 *
 * <p>Package-private and shared by the passes that draw a control: nothing outside builds one, since it
 * is assembled from a style and a frame's live state at the top of the draw and spent within it.
 *
 * @param style      the look bundle - accents, cell treatments, tab chrome, and the body font
 * @param opacity    the alpha the whole control and its text fade by
 * @param cellPaints the hovered wash and press light resolved per cell for this frame
 */
record ControlPaint(
    WidgetStyle style,
    float opacity,
    CellPaintSources cellPaints) {

    /**
     * The face every control's text is measured and drawn at. Asked of the paint rather than built where
     * it is wanted, so the measurement and the draw cannot end up naming a different pair - text measured
     * on one face and painted on another sizes a row it then overflows.
     *
     * @return the body face at the strip's own body size
     */
    TextFace resolveBodyFace() {
        return new TextFace(
            style.bodyFont(),
            ControlStripLayout.BODY_FONT_SIZE);
    }
}
