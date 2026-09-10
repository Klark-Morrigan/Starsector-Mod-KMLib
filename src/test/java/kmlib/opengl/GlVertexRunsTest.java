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
        void flattenVerticesPacksEachPointInOrderAsAnXYPair() {
            var flat = GlVertexRuns.flattenVertices(List.of(
                new double[] {1, 2},
                new double[] {3, 4},
                new double[] {5, 6}));

            assertThat(flat)
                .containsExactly(1f, 2f, 3f, 4f, 5f, 6f);
        }

        @Test
        void flattenVerticesYieldsAnEmptyRunForNoPoints() {
            assertThat(GlVertexRuns.flattenVertices(List.of()))
                .isEmpty();
        }
    }

    @Nested
    class FlattenLoops {
        @Test
        void flattenLoopsPacksOneRunPerLoopInOrder() {
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
        void flattenLoopsPacksEachLoopExactlyAsFlattenVerticesWould() {
            // The plural is the singular applied down the list; a divergence here would show
            // as geometry that draws correctly alone and wrongly in company.
            var loop = List.of(
                new double[] {1, 2},
                new double[] {3, 4});

            assertThat(GlVertexRuns.flattenLoops(List.of(loop)).get(0))
                .containsExactly(GlVertexRuns.flattenVertices(loop));
        }

        @Test
        void flattenLoopsYieldsNoRunsForNoLoops() {
            assertThat(GlVertexRuns.flattenLoops(List.of()))
                .isEmpty();
        }

        @Test
        void flattenLoopsKeepsAnEmptyLoopAsAnEmptyRun() {
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
        void unflattenVerticesReadsEachXYPairBackOutInOrder() {
            var vertices = GlVertexRuns.unflattenVertices(new float[] {1, 2, 3, 4, 5, 6});

            assertThat(vertices)
                .containsExactly(
                    new double[] {1, 2},
                    new double[] {3, 4},
                    new double[] {5, 6});
        }

        @Test
        void unflattenVerticesRoundTripsWhatFlattenVerticesPacked() {
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
        void unflattenVerticesYieldsNoPointsForAnEmptyRun() {
            assertThat(GlVertexRuns.unflattenVertices(GlVertexRuns.NO_VERTICES))
                .isEmpty();
        }
    }

    @Nested
    class PackFloats {
        @Test
        void packFloatsWritesEachFloatIntoTheRunInOrder() {
            var run = GlVertexRuns.packFloats(List.of(1f, 2f, 3f, 4f, 5f));

            assertThat(run)
                .containsExactly(1f, 2f, 3f, 4f, 5f);
        }

        @Test
        void packFloatsYieldsAnEmptyRunForNoFloats() {
            assertThat(GlVertexRuns.packFloats(List.of()))
                .isEmpty();
        }
    }

    @Nested
    class FlattenClosedLoopAsSegments {
        @Test
        void flattenClosedLoopEmitsOneSegmentPerEdgeIncludingTheWrap() {
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
        void flattenClosedLoopYieldsAnEmptyRunForAnEmptyRing() {
            assertThat(GlVertexRuns.flattenClosedLoopAsSegments(List.of()))
                .isEmpty();
        }
    }
}
