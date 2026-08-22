package kmlib.starsector.ui.render.gl;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.widgets.BoxBorder;

/**
 * Draws the frame of a rectangular UI panel in screen/UI coordinates. A one-liner in intent
 * but not in code - a border is four thin filled quads, one per edge - so hand-rolling it at
 * each panel invites the edges to drift out of step. This is the shared outline every KM UI
 * box strokes.
 *
 * <p>Builds on {@link UiFill#renderQuad}, so the edges composite over what is behind them and
 * fade to nothing at zero alpha. This only places the quads. Like the rest of the raw-draw
 * helpers it touches the GL surface and is run only in-engine.
 */
public final class UiBoxes {
    private UiBoxes() {
    }

    /**
     * Strokes the {@code border}'s edges around {@code bounds}, each edge inset so it falls inside the
     * footprint. An omitted edge leaves that side open, so a box drawn flush against another's edge can
     * drop the border there rather than doubling it.
     *
     * <p>Where the quads go is {@link BoxBorder#computeStrokeBoxes}'s - it cuts them so no two overlap,
     * which is what keeps a translucent frame from compositing twice at its corners - and this pass fills
     * what it is handed.
     *
     * @param bounds the footprint to frame, in UI coordinates (UI origin is bottom-left)
     * @param border the border width and which of the four edges to stroke
     * @param paint  the edge colour and alpha
     */
    public static void renderBorder(Rectangle bounds, BoxBorder border, UiElementPaint paint) {
        for (var stroke : border.computeStrokeBoxes(bounds)) {
            UiFill.renderQuad(stroke, paint);
        }
    }
}
