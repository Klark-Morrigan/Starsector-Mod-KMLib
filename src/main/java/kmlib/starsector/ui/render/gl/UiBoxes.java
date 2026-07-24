package kmlib.starsector.ui.render.gl;

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
     * Strokes the {@code border}'s edges around the rectangle at {@code (x, y)} with the given size, each
     * edge inset so it falls inside the footprint (the top and right edges offset by the border width,
     * matching the bottom and left). An omitted edge leaves that side open, so a box drawn flush against
     * another's edge can drop the border there rather than doubling it.
     *
     * @param x      left edge, in UI coordinates
     * @param y      bottom edge, in UI coordinates (UI origin is bottom-left)
     * @param width  rectangle width
     * @param height rectangle height
     * @param border the border width and which of the four edges to stroke
     * @param color  edge colour
     * @param alpha  edge alpha, 0..1
     */
    public static void renderBorder(
            float x,
            float y,
            float width,
            float height,
            BoxBorder border,
            Color color,
            float alpha) {

        // Every edge is the same quad differing only in placement and size, so bind the shared
        // colour and alpha once and let each edge supply its own rectangle.
        var thickness = border.width();
        var edges = border.edges();
        QuadPlacer strokeEdge =
                (edgeX, edgeY, edgeWidth, edgeHeight) ->
                        UiFill.renderQuad(edgeX, edgeY, edgeWidth, edgeHeight, color, alpha);

        if (edges.contains(BoxEdge.BOTTOM)) {
            strokeEdge.placeQuad(x, y, width, thickness);
        }
        if (edges.contains(BoxEdge.TOP)) {
            strokeEdge.placeQuad(x, y + height - thickness, width, thickness);
        }
        if (edges.contains(BoxEdge.LEFT)) {
            strokeEdge.placeQuad(x, y, thickness, height);
        }
        if (edges.contains(BoxEdge.RIGHT)) {
            strokeEdge.placeQuad(x + width - thickness, y, thickness, height);
        }
    }

    /**
     * Places one quad from its bottom-left corner and size. Lets {@link #renderBorder} bind the
     * shared colour and alpha once and vary only each edge's rectangle.
     */
    @FunctionalInterface
    private interface QuadPlacer {
        void placeQuad(float x, float y, float width, float height);
    }
}
