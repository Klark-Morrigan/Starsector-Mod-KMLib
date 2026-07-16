package kmlib.starsector.ui.render.gl;

import kmlib.math.geometry.Rectangle;
import kmlib.starsector.ui.controls.TriangleDirection;

import java.awt.Color;

/**
 * Raw-GL paint for a small filled triangle sized to a box: an up triangle (apex on the box's top edge)
 * or a down triangle (apex on the bottom edge), filling {@code box} in one colour faded by one opacity
 * - the shape the fixed-function pipeline draws where a font has no up/down glyph. A passthrough over
 * {@link UiFill#renderTriangle} that turns a box plus a {@link TriangleDirection} into the three fill
 * vertices; runs under the caller's current GL state.
 */
public final class TriangleRenderer {
    private TriangleRenderer() {
    }

    /**
     * Fills the triangle inside {@code box}, pointing the way {@code direction} names, in
     * {@code color} faded by {@code opacity}. The triangle spans the box's full width at its base and
     * meets at the apex on the opposite edge, so it sits centred in {@code box}. Must run with a
     * current GL context, like any immediate-mode GL call.
     *
     * @param box       the box the triangle fills, in UI coordinates
     * @param direction which way the triangle points, up or down
     * @param color     the fill colour
     * @param opacity   overall alpha, 0..1
     */
    public static void render(
            Rectangle box,
            TriangleDirection direction,
            Color color,
            float opacity) {
        var vertices = direction == TriangleDirection.UP
                ? computeUpTriangle(box)
                : computeDownTriangle(box);
        UiFill.renderTriangle(vertices, color, opacity);
    }

    // An up triangle: its base along the box's bottom edge and its apex centred on the top edge, so it
    // reads as pointing up out of the slot.
    private static float[] computeUpTriangle(Rectangle box) {
        var top = box.y() + box.height();
        return new float[] {
                box.x(),
                box.y(),
                box.x() + box.width(),
                box.y(),
                box.computeCenterX(),
                top,
        };
    }

    // A down triangle: its base along the box's top edge and its apex centred on the bottom edge, the
    // vertical mirror of the up triangle.
    private static float[] computeDownTriangle(Rectangle box) {
        var top = box.y() + box.height();
        return new float[] {
                box.x(),
                top,
                box.x() + box.width(),
                top,
                box.computeCenterX(),
                box.y(),
        };
    }
}
