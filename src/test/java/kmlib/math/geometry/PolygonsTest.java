package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
 * <p>And of {@link Polygons#insetSelectedEdges}: only the flagged edges pull
 * inward while a kept edge stays on its line (truncated within the inset where a
 * pulled-in edge crosses it), flagging every output edge by origin; insetting
 * every edge matches the whole-polygon inset; an over-inset empties; a collapse to
 * coincident points empties rather than returning a zero-area sliver; and a mask
 * that is not parallel to the edges is rejected.
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
            assertThat(bottom.startY()).isCloseTo(2.0, within());
            assertThat(bottom.endY()).isCloseTo(2.0, within());
            assertThat(bottom.startX()).isCloseTo(0.0, within());
            assertThat(bottom.endX()).isCloseTo(10.0, within());
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
    class InsetSelectedEdges {
        @Test
        void inset_selected_edges_pulls_in_only_the_flagged_edges() {
            // Square side 10; inset every edge but the right one (index 1). The kept
            // right edge stays at x = 10 while the other three pull in by 2, giving
            // the rectangle (2,2)..(10,8).
            var result = Polygons.insetSelectedEdges(square(),
                    new boolean[] {true, false, true, true}, 2.0);

            assertThat(result.vertices()).hasSize(4);
            assertThat(result.vertices()).anySatisfy(v -> assertThat(v[0]).isCloseTo(10.0, within()));
            assertThat(result.vertices()).allMatch(v -> v[0] >= 2 - 1e-6 && v[0] <= 10 + 1e-6
                    && v[1] >= 2 - 1e-6 && v[1] <= 8 + 1e-6);
        }

        @Test
        void inset_selected_edges_flags_the_kept_edge_and_truncates_it_within_the_inset() {
            var result = Polygons.insetSelectedEdges(square(),
                    new boolean[] {true, false, true, true}, 2.0);

            // Exactly one edge - the un-inset right edge - is flagged not-inset, and
            // it no longer reaches the raw corners (y = 0 and y = 10): both its ends
            // sit within the 2..8 inset band, so the kept edge stays inside the
            // padding rather than poking out to the corner.
            var kept = keptEdgesOf(result);
            assertThat(kept).hasSize(1);
            assertThat(kept.get(0)[0][0]).isCloseTo(10.0, within());
            assertThat(kept.get(0)[1][0]).isCloseTo(10.0, within());
            assertThat(kept.get(0)[0][1]).isBetween(2.0 - 1e-6, 8.0 + 1e-6);
            assertThat(kept.get(0)[1][1]).isBetween(2.0 - 1e-6, 8.0 + 1e-6);
        }

        @Test
        void inset_selected_edges_matches_the_whole_polygon_inset_when_every_edge_is_flagged() {
            var selective = Polygons.insetSelectedEdges(square(),
                    new boolean[] {true, true, true, true}, 2.0);
            var whole = Polygons.insetConvexPolygon(square(), 2.0);

            // With every edge flagged, the result is the plain inset - and every
            // output edge is flagged as an inset edge.
            assertThat(selective.vertices()).hasSameSizeAs(whole);
            assertThat(selective.edgeIsInset()).containsOnly(true);
        }

        @Test
        void inset_selected_edges_empties_when_the_inset_consumes_the_polygon() {
            var thin = Arrays.asList(
                    new double[] {0, 0}, new double[] {10, 0},
                    new double[] {10, 1}, new double[] {0, 1});

            var result = Polygons.insetSelectedEdges(thin,
                    new boolean[] {true, true, true, true}, 2.0);

            assertThat(result.vertices()).isEmpty();
            assertThat(result.edgeIsInset()).isEmpty();
        }

        @Test
        void inset_selected_edges_empties_a_collapse_rather_than_returning_a_sliver() {
            // Insetting every edge of the side-10 square by exactly half its side
            // pulls all four borders through the centre, so the clip collapses to
            // coincident points at (5,5) - a non-zero raw vertex count but no area.
            // It normalises to empty, so a non-empty result is always a real
            // polygon a caller can draw without re-checking its size.
            var result = Polygons.insetSelectedEdges(square(),
                    new boolean[] {true, true, true, true}, 5.0);

            assertThat(result.vertices()).isEmpty();
            assertThat(result.edgeIsInset()).isEmpty();
        }

        @Test
        void inset_selected_edges_rejects_a_mask_not_parallel_to_the_edges() {
            assertThatThrownBy(() ->
                    Polygons.insetSelectedEdges(square(), new boolean[] {true}, 2.0))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        // The edges a selective inset left un-inset (kept seams), each as its two
        // endpoints - what a caller strokes as an interior line rather than a
        // border.
        private static List<double[][]> keptEdgesOf(Polygons.SelectiveInset result) {
            var kept = new java.util.ArrayList<double[][]>();
            var vertices = result.vertices();
            var flags = result.edgeIsInset();
            for (var i = 0; i < vertices.size(); i++) {
                if (!flags[i]) {
                    kept.add(new double[][] {
                            vertices.get(i), vertices.get((i + 1) % vertices.size())});
                }
            }
            return kept;
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
