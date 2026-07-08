package kmlib.starsector.ui.render;

import java.awt.Color;

/**
 * Draws the frame of a rectangular UI panel in screen/UI coordinates. A one-liner in intent
 * but not in code - a border is four thin filled quads, one per edge - so hand-rolling it at
 * each panel invites the edges to drift out of step. This is the shared outline every KM UI
 * box strokes.
 *
 * <p>Builds on {@link UiFill#renderQuad}, so the edges composite over what is behind them and
 * fade to nothing at zero alpha. This only places the quads. Like the rest of the raw-draw
 * helpers it touches the GL surface and is exercised in-engine rather than in unit tests.
 */
public final class UiBoxes {
    private UiBoxes() {
    }

    /**
     * Strokes the outline of the rectangle at {@code (x, y)} with the given size, inset so the
     * edges fall inside the rectangle's footprint (the top and right edges are offset by
     * {@code thickness}, matching the bottom and left).
     *
     * @param x         left edge, in UI coordinates
     * @param y         bottom edge, in UI coordinates (UI origin is bottom-left)
     * @param width     rectangle width
     * @param height    rectangle height
     * @param thickness edge thickness
     * @param color     edge colour
     * @param alpha     edge alpha, 0..1
     */
    public static void renderBorder(float x, float y, float width, float height, float thickness,
            Color color, float alpha) {
        UiFill.renderQuad(x, y, width, thickness, color, alpha);
        UiFill.renderQuad(x, y + height - thickness, width, thickness, color, alpha);
        UiFill.renderQuad(x, y, thickness, height, color, alpha);
        UiFill.renderQuad(x + width - thickness, y, thickness, height, color, alpha);
    }
}
