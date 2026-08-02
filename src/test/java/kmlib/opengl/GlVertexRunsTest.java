package kmlib.opengl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the flat-run packing contract: vertices flatten in order as {@code x, y}
 * pairs and unflatten back to the points they came from, a multi-loop shape flattens to one
 * run per loop with the correspondence between the two kept exact, a closed ring flattens to
 * one {@code GL_LINES} segment per edge with the wrap edge back to the first vertex included,
 * and empty input yields an empty run.
 */
final class GlVertexRunsTest {

    @Nested
    class FlattenVertices {
        @Test
        void flatten_vertices_packs_each_point_in_order_as_an_x_y_pair() {
            var flat = GlVertexRuns.flattenVertices(List.of(
                new double[] {1, 2},
                new double[] {3, 4},
                new double[] {5, 6}));

            assertThat(flat)
                .containsExactly(1f, 2f, 3f, 4f, 5f, 6f);
        }

        @Test
        void flatten_vertices_yields_an_empty_run_for_no_points() {
            assertThat(GlVertexRuns.flattenVertices(List.of()))
                .isEmpty();
        }
    }

    @Nested
    class FlattenLoops {
        @Test
        void flatten_loops_packs_one_run_per_loop_in_order() {
            var runs = GlVertexRuns.flattenLoops(List.of(
                List.of(
                    new double[] {0, 0},
                    new double[] {4, 0}),
                List.of(
                    new double[] {7, 8},
                    new double[] {9, 10},
                    new double[] {11, 12})));

            // Run per loop, not one concatenated buffer: a consumer strokes each loop as its
            // own primitive, so a joined run would close the shape across the gap between two.
            assertThat(runs)
                .hasSize(2);
            assertThat(runs.get(0))
                .containsExactly(0f, 0f, 4f, 0f);
            assertThat(runs.get(1))
                .containsExactly(7f, 8f, 9f, 10f, 11f, 12f);
        }

        @Test
        void flatten_loops_packs_each_loop_exactly_as_flatten_vertices_would() {
            // The plural is the singular applied down the list; a divergence here would show
            // as geometry that draws correctly alone and wrongly in company.
            var loop = List.of(
                new double[] {1, 2},
                new double[] {3, 4});

            assertThat(GlVertexRuns.flattenLoops(List.of(loop)).get(0))
                .containsExactly(GlVertexRuns.flattenVertices(loop));
        }

        @Test
        void flatten_loops_yields_no_runs_for_no_loops() {
            assertThat(GlVertexRuns.flattenLoops(List.of()))
                .isEmpty();
        }

        @Test
        void flatten_loops_keeps_an_empty_loop_as_an_empty_run() {
            // Dropping it would silently renumber the runs against the loops they came from,
            // which a caller pairing them up by index would never see.
            var runs = GlVertexRuns.flattenLoops(List.of(
                List.of(),
                List.of(new double[] {1, 1})));

            assertThat(runs)
                .hasSize(2);
            assertThat(runs.get(0))
                .isEmpty();
        }
    }

    @Nested
    class UnflattenVertices {
        @Test
        void unflatten_vertices_reads_each_x_y_pair_back_out_in_order() {
            var vertices = GlVertexRuns.unflattenVertices(new float[] {1, 2, 3, 4, 5, 6});

            assertThat(vertices)
                .containsExactly(
                    new double[] {1, 2},
                    new double[] {3, 4},
                    new double[] {5, 6});
        }

        @Test
        void unflatten_vertices_round_trips_what_flatten_vertices_packed() {
            // The two halves of the packing must agree; a stride that drifted apart would
            // survive either test alone.
            var polygon = List.of(
                new double[] {0, 0},
                new double[] {4, 0},
                new double[] {0, 3});

            var roundTripped = GlVertexRuns.unflattenVertices(
                GlVertexRuns.flattenVertices(polygon));

            assertThat(roundTripped)
                .containsExactlyElementsOf(polygon);
        }

        @Test
        void unflatten_vertices_yields_no_points_for_an_empty_run() {
            assertThat(GlVertexRuns.unflattenVertices(GlVertexRuns.NO_VERTICES))
                .isEmpty();
        }
    }

    @Nested
    class PackFloats {
        @Test
        void pack_floats_writes_each_float_into_the_run_in_order() {
            var run = GlVertexRuns.packFloats(List.of(1f, 2f, 3f, 4f, 5f));

            assertThat(run)
                .containsExactly(1f, 2f, 3f, 4f, 5f);
        }

        @Test
        void pack_floats_yields_an_empty_run_for_no_floats() {
            assertThat(GlVertexRuns.packFloats(List.of()))
                .isEmpty();
        }
    }

    @Nested
    class FlattenClosedLoopAsSegments {
        @Test
        void flatten_closed_loop_emits_one_segment_per_edge_including_the_wrap() {
            // A triangle yields three segments: 0->1, 1->2, and the wrap 2->0.
            var flat = GlVertexRuns.flattenClosedLoopAsSegments(List.of(
                new double[] {0, 0},
                new double[] {4, 0},
                new double[] {0, 3}));

            assertThat(flat)
                .containsExactly(
                    0f, 0f, 4f, 0f,
                    4f, 0f, 0f, 3f,
                    0f, 3f, 0f, 0f);
        }

        @Test
        void flatten_closed_loop_yields_an_empty_run_for_an_empty_ring() {
            assertThat(GlVertexRuns.flattenClosedLoopAsSegments(List.of()))
                .isEmpty();
        }
    }
}
