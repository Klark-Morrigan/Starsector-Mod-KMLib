package kmlib.opengl;

import kmlib.math.geometry.Limits;
import kmlib.math.geometry.Points;

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
    public static float[] computeHatchSegments(
            float[] triangleSoup,
            double angleRadians,
            double spacing) {
        if (spacing <= 0 || triangleSoup.length < FLOATS_PER_TRIANGLE) {
            return GlVertexRuns.NO_VERTICES;
        }
        var axes = HatchAxes.computeAxesFromAngle(angleRadians, spacing);
        var segments = new ArrayList<Float>();
        for (var i = 0; i + FLOATS_PER_TRIANGLE <= triangleSoup.length; i += FLOATS_PER_TRIANGLE) {
            hatchTriangle(readLevelledTriangle(triangleSoup, i, axes), axes, segments);
        }
        return GlVertexRuns.packFloats(segments);
    }

    // Clips every hatch line crossing {@code triangle} into {@code segments}. The lines that
    // can touch a triangle are those whose level falls within its own level span, so only that
    // integer range of lines is walked rather than the whole plane.
    private static void hatchTriangle(
            LevelledTriangle triangle,
            HatchAxes axes,
            List<Float> segments) {
        // A multiple landing exactly on the min or max level touches only that extreme corner;
        // it collapses to a point and drops out in the length guard downstream.
        var firstLine = (int) Math.ceil(triangle.computeMinLevel() / axes.spacing());
        var lastLine = (int) Math.floor(triangle.computeMaxLevel() / axes.spacing());
        for (var line = firstLine; line <= lastLine; line++) {
            clipLineToTriangle(line * axes.spacing(), triangle, axes, segments);
        }
    }

    // Adds the segment where the constant-level line {@code level} crosses the triangle. The
    // line enters and leaves through two of the three edges, so the crossing points on the
    // edges are gathered and the two farthest apart along the line direction become the
    // segment's ends. Fewer than two distinct crossings (the line only touches a corner) adds
    // nothing.
    private static void clipLineToTriangle(
            double level,
            LevelledTriangle triangle,
            HatchAxes axes,
            List<Float> segments) {
        var crossings = new ArrayList<double[]>();
        addEdgeCrossing(crossings, level, triangle.vertexA(), triangle.vertexB());
        addEdgeCrossing(crossings, level, triangle.vertexB(), triangle.vertexC());
        addEdgeCrossing(crossings, level, triangle.vertexC(), triangle.vertexA());
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
            var along = axes.computeDistanceAlong(crossing[0], crossing[1]);
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

    // Adds the point where the constant-level line crosses the edge from {@code start} to
    // {@code end}, if it does. An edge lying flat on the line (both ends at the level) is
    // skipped: its direction along the line is undefined, and its endpoints re-enter through
    // the two adjacent edges anyway, so nothing is lost.
    private static void addEdgeCrossing(
            List<double[]> crossings,
            double level,
            LevelledVertex start,
            LevelledVertex end) {
        var levelSpan = end.level() - start.level();
        if (Math.abs(levelSpan) < Limits.MIN_EDGE_LENGTH) {
            return;
        }
        var fraction = (level - start.level()) / levelSpan;
        if (fraction < 0 || fraction > 1) {
            return;
        }
        crossings.add(new double[] {
            start.x() + fraction * (end.x() - start.x()),
            start.y() + fraction * (end.y() - start.y())});
    }

    // Reads the triangle starting at {@code base} in the soup. Its three vertices are packed
    // one after another, so each starts a full vertex stride past the last.
    private static LevelledTriangle readLevelledTriangle(
            float[] soup,
            int base,
            HatchAxes axes) {
        return new LevelledTriangle(
            readLevelledVertex(soup, base, axes),
            readLevelledVertex(soup, base + GlVertexRuns.FLOATS_PER_VERTEX, axes),
            readLevelledVertex(soup, base + 2 * GlVertexRuns.FLOATS_PER_VERTEX, axes));
    }

    // Reads the vertex starting at {@code offset} - its y sits one float past its x - and
    // levels it as it is read, so the clip walk that visits each corner once per crossing
    // line never recomputes the same level.
    private static LevelledVertex readLevelledVertex(float[] soup, int offset, HatchAxes axes) {
        var x = soup[offset];
        var y = soup[offset + 1];
        return new LevelledVertex(x, y, axes.computeLevel(x, y));
    }

    // The hatch line family as the two axes it is defined on, so the direction, its normal and
    // the spacing travel as one value instead of five loose doubles threaded down the clip.
    // Owning the two projections as well keeps the "which line is this point on" and "how far
    // along the line is it" arithmetic in one place rather than restated at each use site.
    private record HatchAxes(
            double directionX,
            double directionY,
            double normalX,
            double normalY,
            double spacing) {

        // The lines run along the direction; the level axis is that direction turned 90
        // degrees, so the hatch lines are the loci where the level is an integer multiple of
        // the spacing.
        private static HatchAxes computeAxesFromAngle(double angleRadians, double spacing) {
            var directionX = Math.cos(angleRadians);
            var directionY = Math.sin(angleRadians);
            return new HatchAxes(directionX, directionY, -directionY, directionX, spacing);
        }

        // How far along the line direction the point sits - the ordering a clipped segment's
        // two ends are picked by.
        private double computeDistanceAlong(double x, double y) {
            return Points.projectPointOnto(x, y, directionX, directionY);
        }

        // The point's perpendicular offset, which names the hatch line through it once
        // divided by the spacing.
        private double computeLevel(double x, double y) {
            return Points.projectPointOnto(x, y, normalX, normalY);
        }
    }

    // One triangle of the soup with each corner already levelled, the unit a hatch clip works
    // on. Named corners rather than nine loose doubles, so an edge is passed as its two
    // endpoints and no call site can transpose a coordinate for a level.
    private record LevelledTriangle(
            LevelledVertex vertexA,
            LevelledVertex vertexB,
            LevelledVertex vertexC) {

        // The far end of the triangle's level span; with the min, the range of hatch lines
        // that can cross it at all.
        private double computeMaxLevel() {
            return Math.max(vertexA.level(), Math.max(vertexB.level(), vertexC.level()));
        }

        // The near end of the triangle's level span.
        private double computeMinLevel() {
            return Math.min(vertexA.level(), Math.min(vertexB.level(), vertexC.level()));
        }
    }

    // A triangle corner paired with its level on the hatch axes - the perpendicular offset
    // that decides which hatch lines reach it and where along each edge they cross.
    private record LevelledVertex(double x, double y, double level) {
    }
}
