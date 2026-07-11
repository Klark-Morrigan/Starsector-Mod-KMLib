package kmlib.opengl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the flat-run packing contract: vertices flatten in order as {@code x, y}
 * pairs, a closed ring flattens to one {@code GL_LINES} segment per edge with the
 * wrap edge back to the first vertex included, and empty input yields an empty run.
 */
final class GlVertexRunsTest {

    @Nested
    class FlattenVertices {
        @Test
        void flatten_vertices_packs_each_point_in_order_as_an_x_y_pair() {
            var flat = GlVertexRuns.flattenVertices(List.of(
                    new double[] {1, 2}, new double[] {3, 4}, new double[] {5, 6}));

            assertThat(flat).containsExactly(1f, 2f, 3f, 4f, 5f, 6f);
        }

        @Test
        void flatten_vertices_yields_an_empty_run_for_no_points() {
            assertThat(GlVertexRuns.flattenVertices(List.of())).isEmpty();
        }
    }

    @Nested
    class PackFloats {
        @Test
        void pack_floats_writes_each_float_into_the_run_in_order() {
            var run = GlVertexRuns.packFloats(List.of(1f, 2f, 3f, 4f, 5f));

            assertThat(run).containsExactly(1f, 2f, 3f, 4f, 5f);
        }

        @Test
        void pack_floats_yields_an_empty_run_for_no_floats() {
            assertThat(GlVertexRuns.packFloats(List.of())).isEmpty();
        }
    }

    @Nested
    class FlattenClosedLoopAsSegments {
        @Test
        void flatten_closed_loop_emits_one_segment_per_edge_including_the_wrap() {
            // A triangle yields three segments: 0->1, 1->2, and the wrap 2->0.
            var flat = GlVertexRuns.flattenClosedLoopAsSegments(List.of(
                    new double[] {0, 0}, new double[] {4, 0}, new double[] {0, 3}));

            assertThat(flat).containsExactly(
                    0f, 0f, 4f, 0f,
                    4f, 0f, 0f, 3f,
                    0f, 3f, 0f, 0f);
        }

        @Test
        void flatten_closed_loop_yields_an_empty_run_for_an_empty_ring() {
            assertThat(GlVertexRuns.flattenClosedLoopAsSegments(List.of())).isEmpty();
        }
    }
}
