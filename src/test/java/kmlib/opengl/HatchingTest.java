package kmlib.opengl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins the hatch-clipping contract: a family of parallel lines spaced by the given
 * distance, clipped to each triangle of the soup and returned as a {@code GL_LINES} run
 * in the soup's own coordinate space. The right triangle {@code (0,0)-(4,0)-(0,4)} with
 * unit-spaced horizontal lines is the worked example the exact-value test reads against;
 * the rest fix the guards, the spacing response, and the line orientation.
 *
 * <p>The joining is pinned by the same worked examples rather than by a suite of its own,
 * since a joining changes nothing about which ground is hatched - only how many primitives
 * the crossings of it are packed into. The two-triangle strip is where that shows: one
 * unbroken line comes back split at the strip's shared edge.
 */
final class HatchingTest {

    @Nested
    class ComputeHatchSegments {
        // A right triangle with legs on the axes: the level axis for a horizontal hatch is
        // its y extent (0..4), so unit spacing lays lines at y = 0, 1, 2, 3, with the apex
        // line at y = 4 collapsing to the corner and dropping out.
        private static final float[] RIGHT_TRIANGLE = {
            0f, 0f, 4f, 0f, 0f, 4f};

        // The same square split along its (0,0)-(4,4) diagonal into two triangles, so a
        // horizontal hatch line crosses both and each triangle answers for its own half.
        private static final float[] SPLIT_SQUARE = {
            0f, 0f, 4f, 0f, 4f, 4f,
            0f, 0f, 4f, 4f, 0f, 4f};

        @Test
        void compute_hatch_segments_yields_an_empty_run_for_zero_spacing() {
            assertThat(hatch(RIGHT_TRIANGLE, 0, 0)).isEmpty();
        }

        @Test
        void compute_hatch_segments_yields_an_empty_run_for_negative_spacing() {
            assertThat(hatch(RIGHT_TRIANGLE, 0, -1)).isEmpty();
        }

        @Test
        void compute_hatch_segments_yields_an_empty_run_for_a_soup_smaller_than_a_triangle() {
            // Two vertices cannot form a triangle, so there is no area to hatch.
            assertThat(hatch(new float[] {0f, 0f, 1f, 0f}, 0, 1)).isEmpty();
        }

        @Test
        void compute_hatch_segments_clips_horizontal_lines_to_a_right_triangle() {
            // Horizontal lines (angle 0) at y = 0, 1, 2, 3; each clipped to the triangle's
            // narrowing width, and the apex line at y = 4 dropped as a corner-only touch.
            var run = hatch(RIGHT_TRIANGLE, 0, 1);

            assertThat(run).containsExactly(
                0f, 0f, 4f, 0f,
                0f, 1f, 3f, 1f,
                0f, 2f, 2f, 2f,
                0f, 3f, 1f, 3f);
        }

        @Test
        void compute_hatch_segments_breaks_one_line_at_every_triangle_it_crosses() {
            // The y = 1 line spans the whole square, but the strip's shared diagonal splits
            // it: the first triangle answers for x = 1..4 and the second for x = 0..1, so
            // the run holds two touching segments where the map shows one stroke. Same for
            // y = 2 and y = 3; y = 0 and y = 4 each lie along one triangle's own edge and so
            // come back whole.
            var run = hatch(SPLIT_SQUARE, 0, 1);

            assertThat(run).containsExactly(
                0f, 0f, 4f, 0f,
                1f, 1f, 4f, 1f,
                2f, 2f, 4f, 2f,
                3f, 3f, 4f, 3f,
                0f, 1f, 1f, 1f,
                0f, 2f, 2f, 2f,
                0f, 3f, 3f, 3f,
                0f, 4f, 4f, 4f);
        }

        @Test
        void compute_hatch_segments_lays_fewer_lines_as_the_spacing_widens() {
            // Doubling the spacing halves how many lines fall within the triangle's y span.
            var tight = hatch(RIGHT_TRIANGLE, 0, 1);
            var loose = hatch(RIGHT_TRIANGLE, 0, 2);

            assertThat(segmentCount(tight)).isEqualTo(4);
            assertThat(segmentCount(loose)).isEqualTo(2);
        }

        @Test
        void compute_hatch_segments_runs_every_segment_along_the_given_angle() {
            // A 45-degree hatch: every clipped segment must lie parallel to that direction,
            // so its direction vector's cross product with (cos, sin) is zero.
            var angle = Math.PI / 4;
            var run = hatch(RIGHT_TRIANGLE, angle, 1);

            assertThat(segmentCount(run)).isPositive();
            for (var segment = 0; segment < run.length; segment += 4) {
                var deltaX = run[segment + 2] - run[segment];
                var deltaY = run[segment + 3] - run[segment + 1];
                var cross = deltaX * Math.sin(angle) - deltaY * Math.cos(angle);
                assertThat(cross).isCloseTo(0, within(1e-3));
            }
        }

        @Test
        void compute_hatch_segments_ignores_a_zero_area_triangle() {
            // Three collinear points enclose no area, so no line crosses them.
            var collinear = new float[] {0f, 0f, 2f, 0f, 4f, 0f};

            assertThat(hatch(collinear, 0, 1)).isEmpty();
        }

        // The joining every case here reads against, named once so a case says only what it
        // varies. It is the emission the whole suite's expectations are written to: another
        // joining hatches the same ground but packs it into a different number of segments.
        private static float[] hatch(float[] triangleSoup, double angleRadians, double spacing) {
            return Hatching.computeHatchSegments(
                triangleSoup,
                angleRadians,
                spacing,
                HatchJoining.PER_TRIANGLE);
        }

        // A GL_LINES run packs four floats per segment (two endpoints).
        private static int segmentCount(float[] run) {
            return run.length / 4;
        }
    }
}
