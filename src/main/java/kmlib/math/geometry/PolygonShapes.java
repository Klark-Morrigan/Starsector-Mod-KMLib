package kmlib.math.geometry;

import org.lwjgl.util.vector.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds a ring from a description of its shape, where the other polygon classes take a ring that
 * already exists and offset, smooth or interrogate it.
 *
 * <p>A regular polygon - equal sides, every vertex the same distance from the centre - is built
 * through the one walk that steps a vertex round the centre, so a shape seeded as a regular polygon
 * and one drawn as one cannot place their vertices by two derivations of the same angles. The start
 * angle is what tells two otherwise identical polygons apart: a hexagon starting at zero lies with a
 * flat top, one starting a twelfth of a turn round stands on a point.
 */
public final class PolygonShapes {

    private PolygonShapes() {
    }

    /**
     * The vertices of a regular polygon, counter-clockwise from {@code startAngle}.
     *
     * @param centreX    x of the centre
     * @param centreY    y of the centre
     * @param radius     distance from the centre to each vertex
     * @param sides      how many sides, and so how many vertices
     * @param startAngle the first vertex's bearing from the centre, in radians counter-clockwise
     *                   from the positive x-axis
     * @return the vertices as {x, y} pairs, in counter-clockwise order
     * @throws IllegalArgumentException when {@code sides} is too few to enclose an area
     */
    public static List<double[]> computeRegularVertices(
            double centreX,
            double centreY,
            double radius,
            int sides,
            double startAngle) {

        if (sides < Limits.MIN_VERTICES_TO_ENCLOSE_AREA) {
            throw new IllegalArgumentException(
                "sides must be at least " + Limits.MIN_VERTICES_TO_ENCLOSE_AREA
                    + " to enclose an area: "
                    + sides);
        }
        var vertices = new ArrayList<double[]>(sides);

        for (var i = 0; i < sides; i++) {

            var angle = startAngle + Angles.FULL_TURN * i / sides;

            vertices.add(new double[] {
                centreX + radius * Math.cos(angle),
                centreY + radius * Math.sin(angle),
            });
        }
        return vertices;
    }

    /**
     * The vertices of a regular polygon as vectors, counter-clockwise from {@code startAngle} -
     * {@link #computeRegularVertices(double, double, double, int, double)} for a polygon laid out in the
     * float coordinates the game's UI works in.
     *
     * @param centre     the centre
     * @param radius     distance from the centre to each vertex
     * @param sides      how many sides, and so how many vertices
     * @param startAngle the first vertex's bearing from the centre, in radians counter-clockwise
     *                   from the positive x-axis
     * @return the vertices, in counter-clockwise order
     * @throws IllegalArgumentException when {@code sides} is too few to enclose an area
     */
    public static List<Vector2f> computeRegularVertices(
            Vector2f centre,
            float radius,
            int sides,
            double startAngle) {

        // Worked in double and narrowed once per coordinate, so the vertices carry one rounding
        // each rather than one per step of the trigonometry.
        var vertices = computeRegularVertices(centre.x, centre.y, radius, sides, startAngle);
        var vectors = new ArrayList<Vector2f>(vertices.size());

        for (var vertex : vertices) {
            vectors.add(new Vector2f((float) vertex[0], (float) vertex[1]));
        }
        return vectors;
    }
}
