package kmlib.math.geometry;

/**
 * Where a thing has to be placed to land on whole pixels. A surface rasterised at one resolution draws
 * pixel-for-pixel only where its edges fall on the grid; half a unit out, every row or column of it is
 * resampled across two of the screen's, which costs it both its edges and its weight - worst on a bitmap
 * face or a hairline, neither of which has any antialiasing of its own to hide the smear behind.
 *
 * <p>Arithmetic over positions and nothing else - no drawing surface, no font, no element - so it sits
 * beside the rest of {@code kmlib.math} rather than inside the UI tree, and so the one rule with a trap
 * in it can be checked without a GL context.
 */
public final class PixelGrid {

    private PixelGrid() {
    }

    /**
     * The nearest whole-pixel position to {@code edge} - what an element placed by its own corner is drawn
     * at.
     *
     * @param edge where the element would otherwise be drawn
     * @return that position on the grid
     */
    public static float computeSnappedEdge(float edge) {
        return Math.round(edge);
    }

    /**
     * The centre to place a span of {@code extent} about so that its own two edges land on whole pixels.
     *
     * <p>Not the nearest whole-pixel centre, which is the trap: a span of odd extent centred on a whole
     * pixel has both its edges on half ones, so snapping the centre would place exactly the spans it was
     * meant to fix half a pixel out. The span's near edge is what is snapped, and the centre follows from
     * it.
     *
     * @param centre where the span would otherwise be centred
     * @param extent how long the span is, along the same axis
     * @return the centre that puts the span's edges on the grid
     */
    public static float computeSnappedCentre(float centre, float extent) {
        return computeSnappedEdge(centre - extent / 2f) + extent / 2f;
    }
}
