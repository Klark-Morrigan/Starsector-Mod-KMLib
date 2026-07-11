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
 */
final class HatchingTest {

    @Nested
    class ComputeHatchSegments {
        // A right triangle with legs on the axes: the level axis for a horizontal hatch is
        // its y extent (0..4), so unit spacing lays lines at y = 0, 1, 2, 3, with the apex
        // line at y = 4 collapsing to the corner and dropping out.
        private static final float[] RIGHT_TRIANGLE = {
                0f, 0f, 4f, 0f, 0f, 4f};

        @Test
        void compute_hatch_segments_yields_an_empty_run_for_zero_spacing() {
            assertThat(Hatching.computeHatchSegments(RIGHT_TRIANGLE, 0, 0)).isEmpty();
        }

        @Test
        void compute_hatch_segments_yields_an_empty_run_for_negative_spacing() {
            assertThat(Hatching.computeHatchSegments(RIGHT_TRIANGLE, 0, -1)).isEmpty();
        }

        @Test
        void compute_hatch_segments_yields_an_empty_run_for_a_soup_smaller_than_a_triangle() {
            // Two vertices cannot form a triangle, so there is no area to hatch.
            assertThat(Hatching.computeHatchSegments(new float[] {0f, 0f, 1f, 0f}, 0, 1))
                    .isEmpty();
        }

        @Test
        void compute_hatch_segments_clips_horizontal_lines_to_a_right_triangle() {
            // Horizontal lines (angle 0) at y = 0, 1, 2, 3; each clipped to the triangle's
            // narrowing width, and the apex line at y = 4 dropped as a corner-only touch.
            var run = Hatching.computeHatchSegments(RIGHT_TRIANGLE, 0, 1);

            assertThat(run).containsExactly(
                    0f, 0f, 4f, 0f,
                    0f, 1f, 3f, 1f,
                    0f, 2f, 2f, 2f,
                    0f, 3f, 1f, 3f);
        }

        @Test
        void compute_hatch_segments_lays_fewer_lines_as_the_spacing_widens() {
            // Doubling the spacing halves how many lines fall within the triangle's y span.
            var tight = Hatching.computeHatchSegments(RIGHT_TRIANGLE, 0, 1);
            var loose = Hatching.computeHatchSegments(RIGHT_TRIANGLE, 0, 2);

            assertThat(segmentCount(tight)).isEqualTo(4);
            assertThat(segmentCount(loose)).isEqualTo(2);
        }

        @Test
        void compute_hatch_segments_runs_every_segment_along_the_given_angle() {
            // A 45-degree hatch: every clipped segment must lie parallel to that direction,
            // so its direction vector's cross product with (cos, sin) is zero.
            var angle = Math.PI / 4;
            var run = Hatching.computeHatchSegments(RIGHT_TRIANGLE, angle, 1);

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

            assertThat(Hatching.computeHatchSegments(collinear, 0, 1)).isEmpty();
        }

        // A GL_LINES run packs four floats per segment (two endpoints).
        private static int segmentCount(float[] run) {
            return run.length / 4;
        }
    }
}
