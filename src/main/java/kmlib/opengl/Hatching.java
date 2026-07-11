package kmlib.opengl;

import kmlib.math.geometry.Limits;

import java.util.ArrayList;
import java.util.List;

/**
 * Fills a tessellated region with parallel diagonal hatch lines, clipping the lines to
 * the region's triangles so the hatch stops exactly at its outline. Consumes the flat
 * triangle soup {@link PolygonTessellator#tessellateToTriangles} produces and returns a
 * {@code GL_LINES} run in the same coordinate space, so a caller hatches a fill region
 * with the same geometry it would have filled solid.
 *
 * <p>Generated line geometry, not {@code glLineStipple}: a hatch drawn as real world-space
 * segments pans and zooms with the map (its spacing is in world units), where stipple is a
 * screen-space pattern some Starsector GL bridges do not implement and would not track the
 * map anyway. Clipping per triangle - rather than stroking the whole plane and masking -
 * means a concave or holed region hatches correctly with no stencil pass, since each
 * triangle already carries the region's shape.
 */
public final class Hatching {
    // A triangle is three {x, y} vertices, so its packed width is three vertices - the
    // stride this steps the soup by to read one triangle at a time.
    private static final int FLOATS_PER_TRIANGLE = 3 * GlVertexRuns.FLOATS_PER_VERTEX;

    // Generates only; never instantiated.
    private Hatching() {
    }

    /**
     * Clips a family of parallel hatch lines to {@code triangleSoup} and returns the clipped
     * segments as a flat {@code [x1, y1, x2, y2, ...]} {@code GL_LINES} run in the soup's own
     * coordinate space.
     *
     * <p>The lines run in the {@code angleRadians} direction and are spaced {@code spacing}
     * apart measured perpendicular to that direction; line zero passes through the origin, so
     * the pattern is stable frame to frame rather than jittering with the region's position.
     *
     * @param triangleSoup the region to hatch, as {@code [x, y, x, y, ...]} with every six
     *                     floats one triangle - exactly what {@link
     *                     PolygonTessellator#tessellateToTriangles} emits
     * @param angleRadians the direction the hatch lines run in
     * @param spacing      the perpendicular distance between adjacent hatch lines, in the
     *                     soup's own (world) units; a non-positive value hatches nothing,
     *                     since it defines no line family
     * @return the clipped hatch segments as a {@code GL_LINES} run; empty when the spacing is
     *         non-positive or the soup encloses no area for a line to cross
     */
    public static float[] computeHatchSegments(float[] triangleSoup, double angleRadians,
            double spacing) {
        if (spacing <= 0 || triangleSoup.length < FLOATS_PER_TRIANGLE) {
            return GlVertexRuns.NO_VERTICES;
        }
        // The lines run along (dirX, dirY); the level axis (normalX, normalY) is that direction
        // turned 90 degrees, so a point's level is its perpendicular offset and the hatch lines
        // are the loci where level is an integer multiple of the spacing.
        var dirX = Math.cos(angleRadians);
        var dirY = Math.sin(angleRadians);
        var normalX = -dirY;
        var normalY = dirX;
        var segments = new ArrayList<Float>();
        for (var i = 0; i + FLOATS_PER_TRIANGLE <= triangleSoup.length; i += FLOATS_PER_TRIANGLE) {
            hatchTriangle(triangleSoup, i, dirX, dirY, normalX, normalY, spacing, segments);
        }
        return GlVertexRuns.packFloats(segments);
    }

    // Clips every hatch line crossing the triangle at {@code base} into {@code segments}. The
    // lines that can touch this triangle are those whose level falls within the triangle's own
    // level span, so only that integer range of lines is walked rather than the whole plane.
    private static void hatchTriangle(float[] soup, int base, double dirX, double dirY,
            double normalX, double normalY, double spacing, List<Float> segments) {
        // The three vertices are packed one after another, so each starts a full vertex stride
        // past the last and its y sits one float past its x.
        var aX = soup[base];
        var aY = soup[base + 1];
        var bX = soup[base + GlVertexRuns.FLOATS_PER_VERTEX];
        var bY = soup[base + GlVertexRuns.FLOATS_PER_VERTEX + 1];
        var cX = soup[base + 2 * GlVertexRuns.FLOATS_PER_VERTEX];
        var cY = soup[base + 2 * GlVertexRuns.FLOATS_PER_VERTEX + 1];
        var levelA = aX * normalX + aY * normalY;
        var levelB = bX * normalX + bY * normalY;
        var levelC = cX * normalX + cY * normalY;
        var minLevel = Math.min(levelA, Math.min(levelB, levelC));
        var maxLevel = Math.max(levelA, Math.max(levelB, levelC));
        // Only the integer multiples within the triangle's own level span carry a line across
        // it, so a triangle is walked over just its own range rather than the whole plane. A
        // multiple landing exactly on the min or max level touches only that extreme corner;
        // it collapses to a point and drops out in the length guard downstream.
        var firstLine = (int) Math.ceil(minLevel / spacing);
        var lastLine = (int) Math.floor(maxLevel / spacing);
        for (var line = firstLine; line <= lastLine; line++) {
            clipLineToTriangle(line * spacing, dirX, dirY,
                    aX, aY, levelA, bX, bY, levelB, cX, cY, levelC, segments);
        }
    }

    // Adds the segment where the constant-level line {@code level} crosses the triangle. The
    // line enters and leaves through two of the three edges, so the crossing points on the
    // edges are gathered and the two farthest apart along the line direction become the
    // segment's ends. Fewer than two distinct crossings (the line only touches a corner) adds
    // nothing.
    private static void clipLineToTriangle(double level, double dirX, double dirY,
            double aX, double aY, double levelA, double bX, double bY, double levelB,
            double cX, double cY, double levelC, List<Float> segments) {
        var crossings = new ArrayList<double[]>();
        addEdgeCrossing(crossings, level, aX, aY, levelA, bX, bY, levelB);
        addEdgeCrossing(crossings, level, bX, bY, levelB, cX, cY, levelC);
        addEdgeCrossing(crossings, level, cX, cY, levelC, aX, aY, levelA);
        if (crossings.size() < 2) {
            return;
        }
        // Rank the crossings by their position along the line and keep the two extremes, so a
        // line clipping a corner (three crossings, two coincident) still yields the one true
        // spanning segment rather than a stray zero-length pair.
        var minAlong = Double.POSITIVE_INFINITY;
        var maxAlong = Double.NEGATIVE_INFINITY;
        double[] startPoint = null;
        double[] endPoint = null;
        for (var crossing : crossings) {
            var along = crossing[0] * dirX + crossing[1] * dirY;
            if (along < minAlong) {
                minAlong = along;
                startPoint = crossing;
            }
            if (along > maxAlong) {
                maxAlong = along;
                endPoint = crossing;
            }
        }
        if (maxAlong - minAlong < Limits.MIN_EDGE_LENGTH) {
            return;
        }
        segments.add((float) startPoint[0]);
        segments.add((float) startPoint[1]);
        segments.add((float) endPoint[0]);
        segments.add((float) endPoint[1]);
    }

    // Adds the point where the constant-level line crosses the edge from (startX, startY) to
    // (endX, endY), if it does. An edge lying flat on the line (both ends at the level) is
    // skipped: its direction along the line is undefined, and its endpoints re-enter through
    // the two adjacent edges anyway, so nothing is lost.
    private static void addEdgeCrossing(List<double[]> crossings, double level,
            double startX, double startY, double startLevel,
            double endX, double endY, double endLevel) {
        var levelSpan = endLevel - startLevel;
        if (Math.abs(levelSpan) < Limits.MIN_EDGE_LENGTH) {
            return;
        }
        var fraction = (level - startLevel) / levelSpan;
        if (fraction < 0 || fraction > 1) {
            return;
        }
        crossings.add(new double[] {
                startX + fraction * (endX - startX),
                startY + fraction * (endY - startY)});
    }
}
