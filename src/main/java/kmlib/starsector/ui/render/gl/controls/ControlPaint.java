package kmlib.starsector.ui.render.gl.controls;

import kmlib.starsector.ui.font.TextFace;
import kmlib.starsector.ui.layout.ControlStripLayout;
import kmlib.starsector.ui.render.gl.style.WidgetStyle;
import kmlib.starsector.ui.widgets.PanelAlpha;

/**
 * The look bundle plus the frame's alpha and the control's live cell paints, threaded together through
 * every draw so a helper takes one paint rather than unpacking the accents, body font, opacity, and
 * resolved washes and lights into loose arguments each time.
 *
 * <p>Package-private and shared by the passes that draw a control: nothing outside builds one, since it
 * is assembled from a style and a frame's live state at the top of the draw and spent within it.
 *
 * @param style      the look bundle - accents, cell treatments, tab chrome, and the body font
 * @param alpha      how see-through the panel's body is meant to be and how much of the panel is on
 *                   screen, kept as the pair so the chrome and the text can take different channels
 *                   of it
 * @param cellPaints the hovered wash and press light resolved per cell for this frame
 */
record ControlPaint(
    WidgetStyle style,
    PanelAlpha alpha,
    CellPaintSources cellPaints) {

    /**
     * The alpha a control's chrome draws at - its fills, frames, washes and markers - which honours both
     * the look's translucency and the moment.
     *
     * <p>Named for its channel rather than left as the bare "opacity", so neither it nor
     * {@link #textOpacity()} reads as the one a caller means by default. They are peers: a control has
     * two, and which of them a draw takes is a decision, not a fallback.
     *
     * @return the chrome's alpha, 0..1
     */
    float chromeOpacity() {
        return alpha.resolveBodyAlpha();
    }

    /**
     * The alpha a control's text draws at, which honours the moment alone.
     *
     * <p>Words opt out of the body's translucency for the reason a tab row does: a see-through panel
     * exists so the map shows through the pane, not so the reading is half-composited with whatever
     * happens to be behind it. The cost falls hardest on text that carries a colour of its own - a
     * value picked out in a relation's shade loses that shade toward the backdrop, while the greys it
     * is meant to be told apart from barely move - so a faded reading does not merely dim, it flattens
     * the very distinctions the colour was for. The fade is still honoured: a panel leaving takes its
     * words with it.
     *
     * @return the text's alpha, 0..1
     */
    float textOpacity() {
        return alpha.resolveChromeAlpha();
    }

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
