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
 *
 * <p>And of {@link Polygons#insetConvexPolygon}: a convex polygon shrinks to a
 * concentric closed polygon, duplicate vertices are dropped first, an over-inset
 * cell empties rather than inverting, and fewer than three distinct vertices
 * yields nothing.
 *
 * <p>And of {@link Polygons#roundCorners}: a corner becomes an arc of
 * {@code segmentsPerCorner + 1} points while straight edges are preserved, the
 * radius is clamped so it never spikes, a zero radius leaves the polygon
 * untouched, and a corner sharper than the threshold is chamfered flat instead
 * of rounded.
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

    @Nested
    class InsetConvexPolygon {
        @Test
        void inset_shrinks_a_square_to_a_concentric_square() {
            // The side-10 square inset by 2 is the side-6 square (2,2)..(8,8): each
            // corner is where two adjacent offset edges meet, so the shape stays
            // closed with clean corners rather than overshooting segments.
            var inset = Polygons.insetConvexPolygon(square(), 2.0);

            assertThat(inset).hasSize(4);
            assertThat(inset.get(0)).containsExactly(new double[] {2, 2}, within());
            assertThat(inset.get(1)).containsExactly(new double[] {8, 2}, within());
            assertThat(inset.get(2)).containsExactly(new double[] {8, 8}, within());
            assertThat(inset.get(3)).containsExactly(new double[] {2, 8}, within());
        }

        @Test
        void inset_drops_duplicate_vertices_before_insetting() {
            // A repeated vertex would make a zero-length edge with no direction;
            // the inset must dedupe and still return the side-6 square's 4 corners.
            var withDuplicate = Arrays.asList(
                    new double[] {0, 0},
                    new double[] {0, 0},
                    new double[] {10, 0},
                    new double[] {10, 10},
                    new double[] {0, 10});

            assertThat(Polygons.insetConvexPolygon(withDuplicate, 2.0)).hasSize(4);
        }

        @Test
        void inset_empties_when_distance_consumes_the_polygon() {
            // A height-1 quad inset by 2 has no interior left: clipping empties it
            // rather than producing an inverted/spiking shape.
            var thin = Arrays.asList(
                    new double[] {0, 0},
                    new double[] {10, 0},
                    new double[] {10, 1},
                    new double[] {0, 1});

            assertThat(Polygons.insetConvexPolygon(thin, 2.0)).isEmpty();
        }

        @Test
        void inset_returns_empty_for_fewer_than_three_distinct_vertices() {
            assertThat(Polygons.insetConvexPolygon(
                    Arrays.asList(new double[] {0, 0}, new double[] {10, 0}), 1.0)).isEmpty();
        }
    }

    @Nested
    class RoundCorners {
        @Test
        void round_corners_arcs_each_corner_and_keeps_straight_edges() {
            // Side-100 square, radius 10, 3 segments/corner -> 4 corners x 4 points
            // = 16. The cut is a fixed 10 units, so the bottom edge stays straight
            // through its middle: the arc endpoints (10,0) and (90,0) lie on y = 0.
            var rounded = Polygons.roundCorners(bigSquare(100), 10.0, 3, 0.0);

            assertThat(rounded).hasSize(16);
            assertThat(rounded).allMatch(vertex ->
                    vertex[0] >= 0 && vertex[0] <= 100 && vertex[1] >= 0 && vertex[1] <= 100);
            // A straight-edge point survives untouched between the rounded corners.
            assertThat(rounded).anyMatch(vertex ->
                    Math.abs(vertex[1]) < 1e-6 && vertex[0] > 0 && vertex[0] < 100);
        }

        @Test
        void round_corners_clamps_an_oversized_radius_without_spiking() {
            // Radius far larger than the side: clamped to half the edge, so the
            // result still stays inside the square instead of overshooting.
            var rounded = Polygons.roundCorners(bigSquare(100), 10_000.0, 3, 0.0);

            assertThat(rounded).allMatch(vertex ->
                    vertex[0] >= 0 && vertex[0] <= 100 && vertex[1] >= 0 && vertex[1] <= 100);
        }

        @Test
        void round_corners_leaves_the_polygon_unchanged_for_zero_radius() {
            assertThat(Polygons.roundCorners(square(), 0.0, 3, 0.0)).hasSize(4);
        }

        @Test
        void round_corners_chamfers_a_corner_sharper_than_the_threshold() {
            // Thin CCW triangle: the apex at the origin spans ~5.7 deg, well below
            // the 45 deg threshold, so it is chamfered flat (its two step-back
            // points only) while the two near-90 deg corners stay rounded into
            // 3 + 1 points each. 2 + 4 + 4 = 10.
            var triangle = Arrays.asList(
                    new double[] {0, 0}, new double[] {100, 0}, new double[] {100, 10});

            var rounded = Polygons.roundCorners(triangle, 5.0, 3, Math.toRadians(45));

            assertThat(rounded).hasSize(10);
            // The spike is gone: no vertex pinches back toward the apex the way a
            // bezier arc would. Both chamfer points sit ~5 units (the cut) out.
            assertThat(rounded).allMatch(vertex -> Math.hypot(vertex[0], vertex[1]) >= 4.0);
        }

        // CCW square of the given side, anchored at the origin.
        private static List<double[]> bigSquare(double side) {
            return Arrays.asList(
                    new double[] {0, 0},
                    new double[] {side, 0},
                    new double[] {side, side},
                    new double[] {0, side});
        }
    }
}
