package kmlib.opengl;

import kmlib.math.geometry.PolygonRegions;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the contract of {@link PolygonTessellator#tessellateToTriangles}:
 *  - a concave polygon triangulates to triangles whose areas sum to its own area
 *    (so the fill covers the shape with no gaps or overlaps),
 *  - a hole ring wound opposite its outer ring is subtracted from the fill,
 *  - degenerate and empty contours produce no triangles.
 *
 * <p>Area is the robust check here: how the tessellator groups faces (triangles,
 * fans, or strips) is its own business, but the total covered area must equal the
 * region's area whatever the grouping.
 */
final class PolygonTessellatorTest {

    // Triangle-area tolerance: the tessellation is exact for straight-edged input,
    // so only float rounding separates the sum from the true area.
    private static final double AREA_TOLERANCE = 1e-3;

    @Nested
    class TessellateToTriangles {
        @Test
        void concave_polygon_triangulates_to_its_own_area() {
            // CCW L-shape: a 30x30 square with a 20x20 bite out of the top-right,
            // area 900 - 400 = 500.
            var lShape = Arrays.asList(
                    new double[] {0, 0}, new double[] {30, 0}, new double[] {30, 10},
                    new double[] {10, 10}, new double[] {10, 30}, new double[] {0, 30});

            var triangles = PolygonTessellator.tessellateToTriangles(List.of(lShape));

            assertThat(totalTriangleArea(triangles)).isCloseTo(500.0, within(AREA_TOLERANCE));
        }

        @Test
        void an_opposite_wound_hole_is_subtracted_from_the_fill() {
            // CCW 40x40 outer (area 1600) with a CW 20x20 hole (area 400): the filled
            // region is 1600 - 400 = 1200.
            var outer = Arrays.asList(
                    new double[] {0, 0}, new double[] {40, 0},
                    new double[] {40, 40}, new double[] {0, 40});
            var hole = Arrays.asList(
                    new double[] {10, 10}, new double[] {10, 30},
                    new double[] {30, 30}, new double[] {30, 10});

            var triangles = PolygonTessellator.tessellateToTriangles(List.of(outer, hole));

            assertThat(totalTriangleArea(triangles)).isCloseTo(1200.0, within(AREA_TOLERANCE));
        }

        @Test
        void a_degenerate_contour_produces_no_triangles() {
            var twoPoints = Arrays.asList(new double[] {0, 0}, new double[] {10, 0});

            assertThat(PolygonTessellator.tessellateToTriangles(List.of(twoPoints))).isEmpty();
        }

        @Test
        void empty_input_produces_no_triangles() {
            assertThat(PolygonTessellator.tessellateToTriangles(List.of())).isEmpty();
        }
    }

    @Nested
    class TessellateToBoundaryLoops {
        @Test
        void a_simple_square_returns_one_loop_of_its_area() {
            var square = Arrays.asList(
                    new double[] {0, 0}, new double[] {10, 0},
                    new double[] {10, 10}, new double[] {0, 10});

            var loops = PolygonTessellator.tessellateToBoundaryLoops(List.of(square));

            assertThat(loops).hasSize(1);
            assertThat(loopArea(loops.get(0))).isCloseTo(100.0, within(AREA_TOLERANCE));
        }

        @Test
        void a_self_intersecting_bowtie_keeps_only_its_positive_lobe() {
            // A figure-eight whose waist crosses itself: one lobe winds
            // counter-clockwise (winding +1), the other clockwise (-1). Under the
            // positive rule only the +1 lobe survives - a clean loop with no
            // self-crossing edge - so the reversed lobe (which would read as a stray
            // ear) is dropped. Each lobe is a 10x10 triangle of area 25.
            var bowtie = Arrays.asList(
                    new double[] {0, 0}, new double[] {10, 10},
                    new double[] {0, 10}, new double[] {10, 0});

            var loops = PolygonTessellator.tessellateToBoundaryLoops(List.of(bowtie));

            assertThat(loops).isNotEmpty();
            var total = loops.stream().mapToDouble(PolygonTessellatorTest::loopArea).sum();
            assertThat(total).isCloseTo(25.0, within(AREA_TOLERANCE));
        }

        @Test
        void empty_input_produces_no_loops() {
            assertThat(PolygonTessellator.tessellateToBoundaryLoops(List.of())).isEmpty();
        }
    }

    // The unsigned area a single closed loop encloses, for asserting a resolved
    // boundary covers the region the fill would. Uses the production shoelace so the
    // test does not restate it.
    private static double loopArea(List<double[]> loop) {
        return Math.abs(PolygonRegions.computeSignedArea(loop));
    }

    // Sums the unsigned area of every triangle in a flat [x, y, x, y, ...] soup, six
    // floats per triangle - the area the fill actually covers.
    private static double totalTriangleArea(float[] triangles) {
        var floatsPerTriangle = 6;
        var total = 0.0;
        for (var i = 0; i + floatsPerTriangle <= triangles.length; i += floatsPerTriangle) {
            var ax = triangles[i];
            var ay = triangles[i + 1];
            var bx = triangles[i + 2];
            var by = triangles[i + 3];
            var cx = triangles[i + 4];
            var cy = triangles[i + 5];
            total += Math.abs((bx - ax) * (cy - ay) - (cx - ax) * (by - ay)) / 2.0;
        }
        return total;
    }
}
