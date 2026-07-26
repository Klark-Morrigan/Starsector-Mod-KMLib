package kmlib.starsector.ui.render.gl;

import kmlib.math.geometry.BoxEdge;
import kmlib.math.geometry.Rectangle;

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
     * Strokes the {@code border}'s edges around {@code bounds}, each edge inset so it falls inside the
     * footprint (the top and right edges offset by the border width, matching the bottom and left). An
     * omitted edge leaves that side open, so a box drawn flush against another's edge can drop the
     * border there rather than doubling it.
     *
     * @param bounds the footprint to frame, in UI coordinates (UI origin is bottom-left)
     * @param border the border width and which of the four edges to stroke
     * @param paint  the edge colour and alpha
     */
    public static void renderBorder(Rectangle bounds, BoxBorder border, UiElementPaint paint) {
        // Every edge is the same quad differing only in placement and size, so bind the shared
        // paint once and let each edge supply its own rectangle.
        var thickness = border.width();
        var edges = border.edges();
        var x = bounds.x();
        var y = bounds.y();
        var width = bounds.width();
        var height = bounds.height();
        QuadPlacer strokeEdge = edgeBounds -> UiFill.renderQuad(edgeBounds, paint);

        if (edges.contains(BoxEdge.BOTTOM)) {
            strokeEdge.placeQuad(new Rectangle(
                    x,
                    y,
                    width,
                    thickness));
        }
        if (edges.contains(BoxEdge.TOP)) {
            strokeEdge.placeQuad(new Rectangle(
                    x,
                    y + height - thickness,
                    width,
                    thickness));
        }
        if (edges.contains(BoxEdge.LEFT)) {
            strokeEdge.placeQuad(new Rectangle(
                    x,
                    y,
                    thickness,
                    height));
        }
        if (edges.contains(BoxEdge.RIGHT)) {
            strokeEdge.placeQuad(new Rectangle(
                    x + width - thickness,
                    y,
                    thickness,
                    height));
        }
    }

    /**
     * Places one edge quad. Lets {@link #renderBorder} bind the shared paint once and vary only each
     * edge's rectangle.
     */
    @FunctionalInterface
    private interface QuadPlacer {
        void placeQuad(Rectangle edgeBounds);
    }
}
