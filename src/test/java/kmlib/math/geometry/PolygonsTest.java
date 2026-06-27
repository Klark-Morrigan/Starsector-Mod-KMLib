package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link Polygons#offsetEdgesInward}:
 *  - one segment per edge,
 *  - edges move toward the interior by the offset,
 *  - a thin polygon never collapses (a whole-polygon inset would),
 *  - fewer than two vertices yields nothing.
 */
final class PolygonsTest {

    // CCW square with side 10, used as the offset reference shape.
    private static List<double[]> square() {
        return Arrays.asList(
                new double[] {0, 0},
                new double[] {10, 0},
                new double[] {10, 10},
                new double[] {0, 10});
    }

    private static org.assertj.core.data.Offset<Double> within() {
        return org.assertj.core.data.Offset.offset(1e-6);
    }

    @Nested
    class OffsetEdgesInward {
        @Test
        void offset_returns_one_segment_per_edge() {
            assertThat(Polygons.offsetEdgesInward(square(), 2.0)).hasSize(4);
        }

        @Test
        void offset_moves_edges_toward_the_interior() {
            // The first edge (0,0)->(10,0) is the bottom edge; offsetting inward by
            // 2 lifts it from y = 0 to y = 2, spanning the same x extent.
            var segments = Polygons.offsetEdgesInward(square(), 2.0);

            var bottom = segments.get(0);
            assertThat(bottom[1]).isCloseTo(2.0, within());
            assertThat(bottom[3]).isCloseTo(2.0, within());
            assertThat(bottom[0]).isCloseTo(0.0, within());
            assertThat(bottom[2]).isCloseTo(10.0, within());
        }

        @Test
        void offset_does_not_collapse_a_thin_polygon() {
            // A whole-polygon inset by 2 would empty this height-1 quad; per-edge
            // offset still yields a segment per edge - nothing vanishes.
            var thin = Arrays.asList(
                    new double[] {0, 0},
                    new double[] {10, 0},
                    new double[] {10, 1},
                    new double[] {0, 1});

            assertThat(Polygons.offsetEdgesInward(thin, 2.0)).hasSize(4);
        }

        @Test
        void offset_returns_empty_for_fewer_than_two_vertices() {
            assertThat(Polygons.offsetEdgesInward(
                    List.of(new double[] {0, 0}), 1.0)).isEmpty();
        }
    }
}
