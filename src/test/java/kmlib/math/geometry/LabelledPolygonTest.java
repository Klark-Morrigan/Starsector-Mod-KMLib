package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link LabelledPolygon}: a seed labels every edge alike; an arbitrary ring
 * can be seeded from explicit vertices and parallel per-edge labels (rejecting a
 * mismatched pair); a half-plane clip keeps only the kept side, stamps the freshly
 * cut edge with the clip label while every surviving edge keeps its own, leaves the
 * source polygon untouched, and empties out when the whole polygon is clipped away.
 */
final class LabelledPolygonTest {

    private static final int SEED_LABEL = 7;
    private static final int CLIP_LABEL = 42;

    @Nested
    class CreateRegularPolygon {

        @Test
        void createRegularPolygonHasOneEdgePerSideAllSeedLabelled() {
            var polygon = LabelledPolygon.createRegularPolygon(new double[] {0, 0}, 100, 4, SEED_LABEL);

            assertThat(polygon.isEmpty()).isFalse();
            assertThat(polygon.getVertices()).hasSize(4);
            assertThat(polygon.getEdgeLabels()).containsExactly(SEED_LABEL, SEED_LABEL, SEED_LABEL,
                SEED_LABEL);
        }

        @Test
        void createRegularPolygonVerticesLieOnTheRadiusAboutTheCentre() {
            double[] centre = {10, -5};
            var polygon = LabelledPolygon.createRegularPolygon(centre, 200, 8, SEED_LABEL);

            assertThat(polygon.getVertices()).allSatisfy(vertex -> {
                var dx = vertex[0] - centre[0];
                var dy = vertex[1] - centre[1];
                assertThat(Math.sqrt(dx * dx + dy * dy)).isCloseTo(200, within(1e-9));
            });
        }
    }

    @Nested
    class FromLabelledEdges {

        @Test
        void fromLabelledEdgesKeepsTheVerticesAndPerEdgeLabels() {
            var vertices = java.util.List.of(
                new double[] {0, 0}, new double[] {10, 0}, new double[] {10, 10});

            var polygon = LabelledPolygon.fromLabelledEdges(vertices, new int[] {1, 2, 3});

            assertThat(polygon.getVertices()).hasSize(3);
            assertThat(polygon.getEdgeLabels()).containsExactly(1, 2, 3);
        }

        @Test
        void fromLabelledEdgesRejectsMismatchedArrayLengths() {
            var vertices = java.util.List.of(new double[] {0, 0}, new double[] {10, 0});

            assertThatThrownBy(() -> LabelledPolygon.fromLabelledEdges(vertices, new int[] {1}))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    class ClipToHalfPlane {

        @Test
        void clipKeepsOnlyTheKeptSide() {
            var diamond = LabelledPolygon.createRegularPolygon(new double[] {0, 0}, 100, 4, SEED_LABEL);

            // Keep x >= 50 (normal points +x, line through (50, 0)); the clip line
            // misses every vertex, so no degenerate zero-length edges arise.
            var clipped = diamond.clipToHalfPlane(new HalfPlane(50, 0, 1, 0), CLIP_LABEL);

            assertThat(clipped.getVertices()).isNotEmpty();
            assertThat(clipped.getVertices())
                .allSatisfy(vertex -> assertThat(vertex[0]).isGreaterThanOrEqualTo(50 - 1e-9));
        }

        @Test
        void clipStampsTheCutEdgeAndKeepsTheRest() {
            var diamond = LabelledPolygon.createRegularPolygon(new double[] {0, 0}, 100, 4, SEED_LABEL);

            var clipped = diamond.clipToHalfPlane(new HalfPlane(50, 0, 1, 0), CLIP_LABEL);

            // Exactly one edge - the one lying on the clip line - takes the clip
            // label; every surviving original edge keeps its seed label.
            var labels = clipped.getEdgeLabels();
            assertThat(count(labels, CLIP_LABEL)).isEqualTo(1);
            assertThat(count(labels, SEED_LABEL)).isEqualTo(labels.length - 1);
        }

        @Test
        void clipLeavesTheSourcePolygonUntouched() {
            var diamond = LabelledPolygon.createRegularPolygon(new double[] {0, 0}, 100, 4, SEED_LABEL);

            diamond.clipToHalfPlane(new HalfPlane(50, 0, 1, 0), CLIP_LABEL);

            // Immutable: clipping returns a new polygon, so the original still has
            // all four seed-labelled edges.
            assertThat(diamond.getVertices()).hasSize(4);
            assertThat(diamond.getEdgeLabels()).containsOnly(SEED_LABEL);
        }

        @Test
        void clipEmptiesWhenNothingIsKept() {
            var diamond = LabelledPolygon.createRegularPolygon(new double[] {0, 0}, 100, 4, SEED_LABEL);

            // The whole polygon lies left of x = 1000, so the kept side is empty.
            var clipped = diamond.clipToHalfPlane(new HalfPlane(1000, 0, 1, 0), CLIP_LABEL);

            assertThat(clipped.isEmpty()).isTrue();
            assertThat(clipped.getVertices()).isEmpty();
            assertThat(clipped.getEdgeLabels()).isEmpty();
        }
    }

    private static long count(int[] values, int target) {
        return Arrays.stream(values).filter(value -> value == target).count();
    }
}
