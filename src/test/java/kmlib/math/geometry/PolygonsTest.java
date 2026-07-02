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
 * <p>And of {@link Polygons#insetPolygonByMiter}: a convex square insets to the
 * same concentric square the half-plane inset gives, a concave L keeps its reflex
 * corner (which a half-plane clip would shear off) and moves every corner into the
 * solid, and fewer than three distinct vertices yields nothing.
 *
 * <p>And of {@link Polygons#roundCorners}: a corner becomes an arc of
 * {@code segmentsPerCorner + 1} points while straight edges are preserved, the
 * radius is clamped so it never spikes, a zero radius leaves the polygon
 * untouched, and a corner sharper than the threshold is chamfered flat instead
 * of rounded.
 *
 * <p>And of {@link Polygons#removeSpikes}: a sharp thin protrusion and an equally
 * sharp inward cusp are both spliced out, a sharp but tall peninsula is kept
 * (clears the height bar), a gently curved run is kept (clears the angle bar), a
 * non-positive threshold disables the pass, and a ring that is all sliver never
 * drops below three vertices.
 *
 * <p>And of {@link Polygons#computeSignedArea}: a counter-clockwise ring reports a
 * positive area equal to the region it encloses, reversing the winding negates it,
 * and a ring that encloses nothing (fewer than three vertices, or collinear
 * vertices) is zero - the sign and magnitude a consumer's fold-guard relies on.
 */
final class PolygonsTest {

    // A generous miter spike limit for the inset tests: high enough that the
    // ordinary right-angle corners keep their crisp miter, so only a deliberately
    // sharp reflex corner bevels.
    private static final double MITER_SPIKE_LIMIT = 4.0;

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

    // CCW square of the given side, anchored at the origin.
    private static List<double[]> bigSquare(double side) {
        return Arrays.asList(
                new double[] {0, 0},
                new double[] {side, 0},
                new double[] {side, side},
                new double[] {0, side});
    }

    // The signed area of a closed ring; positive is counter-clockwise. Used to check
    // an inset kept its winding rather than folding. Delegates to the production
    // shoelace so the test does not restate it.
    private static double signedArea(List<double[]> ring) {
        return Polygons.computeSignedArea(ring);
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
    class InsetPolygonByMiter {
        @Test
        void miter_inset_matches_the_convex_inset_on_a_square() {
            // On a convex shape the miter join and the half-plane clip agree: the
            // side-10 square insets by 2 to the concentric (2,2)..(8,8) square.
            var inset = Polygons.insetPolygonByMiter(square(), 2.0, MITER_SPIKE_LIMIT);

            assertThat(inset).hasSize(4);
            assertThat(inset.get(0)).containsExactly(new double[] {2, 2}, within());
            assertThat(inset.get(1)).containsExactly(new double[] {8, 2}, within());
            assertThat(inset.get(2)).containsExactly(new double[] {8, 8}, within());
            assertThat(inset.get(3)).containsExactly(new double[] {2, 8}, within());
        }

        @Test
        void miter_inset_keeps_a_concave_corner_bevelling_the_reflex_turn() {
            // CCW L-shape with a reflex corner at (10,10). A half-plane clip would
            // shear the concavity off; this keeps it, mitring the five convex corners
            // (the origin to (2,2)) and bevelling the reflex one into two points -
            // where its inbound edge offsets to (10,8) and its outbound edge to
            // (8,10) - rather than spiking their crossing at (8,8). So the notch
            // survives as a small chamfer: seven output vertices, not six.
            var lShape = Arrays.asList(
                    new double[] {0, 0}, new double[] {30, 0}, new double[] {30, 10},
                    new double[] {10, 10}, new double[] {10, 30}, new double[] {0, 30});

            var inset = Polygons.insetPolygonByMiter(lShape, 2.0, MITER_SPIKE_LIMIT);

            assertThat(inset).hasSize(7);
            assertThat(inset.get(0)).containsExactly(new double[] {2, 2}, within());
            assertThat(inset.get(3)).containsExactly(new double[] {10, 8}, within());
            assertThat(inset.get(4)).containsExactly(new double[] {8, 10}, within());
            // The reflex corner never spikes to its miter crossing at (8,8).
            assertThat(inset).noneSatisfy(v -> assertThat(v).containsExactly(new double[] {8, 8}, within()));
        }

        @Test
        void miter_inset_bevels_a_reflex_spike_instead_of_spiking_inward() {
            // A side-200 CCW square with a narrow spike jutting into its interior
            // from the bottom edge - a sharp reflex corner at (100,15). A plain miter
            // would shoot that corner's join far up into the interior (to ~y=78), a
            // stray inward spike. Bevelling it keeps the offset near the bottom band,
            // so no vertex lands in the wide empty mid-height of the square.
            var squareWithSpike = Arrays.asList(
                    new double[] {0, 0}, new double[] {95, 0}, new double[] {100, 15},
                    new double[] {105, 0}, new double[] {200, 0},
                    new double[] {200, 200}, new double[] {0, 200});

            var inset = Polygons.insetPolygonByMiter(squareWithSpike, 20.0, MITER_SPIKE_LIMIT);

            assertThat(inset).isNotEmpty();
            assertThat(signedArea(inset)).isPositive();
            // The only corners are the inset bottom band (y ~ 20) and the top (y ~
            // 180); a spike would place a vertex in the empty middle.
            assertThat(inset).noneMatch(v -> v[1] > 30 && v[1] < 150);
        }

        @Test
        void miter_inset_returns_empty_for_fewer_than_three_distinct_vertices() {
            assertThat(Polygons.insetPolygonByMiter(
                    Arrays.asList(new double[] {0, 0}, new double[] {10, 0}), 1.0, MITER_SPIKE_LIMIT))
                    .isEmpty();
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

        @Test
        void round_corners_arcs_a_reflex_corner_into_the_concavity() {
            // CCW L-shape with a reflex corner at (10,10). The arc there must bulge
            // toward the notch (the corner apex), not fly outside the shape: with
            // radius 3 it steps back to (13,10) and (10,13) and curves through about
            // (10.9,10.9). Six corners arced at 3 + 1 points each = 24.
            var lShape = Arrays.asList(
                    new double[] {0, 0}, new double[] {30, 0}, new double[] {30, 10},
                    new double[] {10, 10}, new double[] {10, 30}, new double[] {0, 30});

            var rounded = Polygons.roundCorners(lShape, 3.0, 3, 0.0);

            assertThat(rounded).hasSize(24);
            assertThat(rounded).allMatch(vertex ->
                    vertex[0] >= -1e-6 && vertex[0] <= 30 + 1e-6
                            && vertex[1] >= -1e-6 && vertex[1] <= 30 + 1e-6);
            // The reflex arc bulges into the notch rather than cutting across it.
            assertThat(rounded).anyMatch(vertex ->
                    vertex[0] > 10 && vertex[0] < 12 && vertex[1] > 10 && vertex[1] < 12);
        }
    }

    @Nested
    class RemoveSpikes {
        // Sharp enough that only a genuine needle/cusp qualifies; a square's right
        // angles (90 deg) stay well clear.
        private static final double MAX_CORNER_ANGLE = Math.toRadians(45);

        @Test
        void remove_spikes_splices_out_an_outward_needle() {
            // A side-100 square whose top edge is interrupted by a thin needle poking
            // up to (50,108): a sharp corner (well under 45 deg) rising only 8 above
            // the y = 100 chord. With the height bar at 20 it is spliced out, leaving
            // the four square corners.
            var squareWithNeedle = Arrays.asList(
                    new double[] {0, 0}, new double[] {100, 0}, new double[] {100, 100},
                    new double[] {51, 100}, new double[] {50, 108}, new double[] {49, 100},
                    new double[] {0, 100});

            var cleaned = Polygons.removeSpikes(squareWithNeedle, 20.0, MAX_CORNER_ANGLE);

            // The needle apex is gone: nothing rises above the top edge. Splicing the
            // apex leaves its two shoulders collinear on y = 100 (this pass sands
            // spikes, not redundant-collinear points), so the count drops but not to 4.
            assertThat(cleaned).noneMatch(vertex -> vertex[1] > 100 + 1e-6);
            assertThat(cleaned).hasSizeLessThan(7);
        }

        @Test
        void remove_spikes_splices_out_an_inward_cusp() {
            // The same square with a sharp nick biting DOWN to (50,92) from the top
            // edge - an inward cusp of the same 8-unit depth. Because the interior
            // angle is unsigned, it is treated like the outward needle and removed.
            var squareWithCusp = Arrays.asList(
                    new double[] {0, 0}, new double[] {100, 0}, new double[] {100, 100},
                    new double[] {51, 100}, new double[] {50, 92}, new double[] {49, 100},
                    new double[] {0, 100});

            var cleaned = Polygons.removeSpikes(squareWithCusp, 20.0, MAX_CORNER_ANGLE);

            // The inward nick apex at (50,92) is gone. As with the needle the spliced
            // shoulders stay collinear on y = 100, so the count drops short of 4.
            assertThat(cleaned).noneMatch(vertex ->
                    Math.abs(vertex[0] - 50) < 1e-6 && Math.abs(vertex[1] - 92) < 1e-6);
            assertThat(cleaned).hasSizeLessThan(7);
        }

        @Test
        void remove_spikes_keeps_a_sharp_but_tall_peninsula() {
            // A sharp corner that juts far is real shape, not a sliver: the apex at
            // (50,160) rises 60 above the y = 100 chord, past the 20 height bar, so
            // even though it is sharp it survives.
            var squareWithPeninsula = Arrays.asList(
                    new double[] {0, 0}, new double[] {100, 0}, new double[] {100, 100},
                    new double[] {60, 100}, new double[] {50, 160}, new double[] {40, 100},
                    new double[] {0, 100});

            var cleaned = Polygons.removeSpikes(squareWithPeninsula, 20.0, MAX_CORNER_ANGLE);

            assertThat(cleaned).anyMatch(vertex -> vertex[1] > 150);
        }

        @Test
        void remove_spikes_keeps_a_gently_curved_run() {
            // A shallow bump only 8 above the chord but spread wide, so its corner is
            // near-straight (well over 45 deg). It clears the angle bar and stays even
            // though it is under the height bar - the pass sands slivers, not curves.
            var squareWithBump = Arrays.asList(
                    new double[] {0, 0}, new double[] {100, 0}, new double[] {100, 100},
                    new double[] {70, 100}, new double[] {50, 108}, new double[] {30, 100},
                    new double[] {0, 100});

            var cleaned = Polygons.removeSpikes(squareWithBump, 20.0, MAX_CORNER_ANGLE);

            assertThat(cleaned).anyMatch(vertex -> vertex[1] > 100 + 1e-6);
        }

        @Test
        void remove_spikes_leaves_the_polygon_unchanged_for_a_non_positive_threshold() {
            var squareWithNeedle = Arrays.asList(
                    new double[] {0, 0}, new double[] {100, 0}, new double[] {100, 100},
                    new double[] {51, 100}, new double[] {50, 108}, new double[] {49, 100},
                    new double[] {0, 100});

            assertThat(Polygons.removeSpikes(squareWithNeedle, 0.0, MAX_CORNER_ANGLE)).hasSize(7);
            assertThat(Polygons.removeSpikes(squareWithNeedle, 20.0, 0.0)).hasSize(7);
        }

        @Test
        void remove_spikes_never_drops_below_three_vertices() {
            // A degenerate sliver triangle (all corners sharp and thin): the pass must
            // not eat it down to a line - it stops at three so the caller's own area
            // check discards it.
            var sliver = Arrays.asList(
                    new double[] {0, 0}, new double[] {100, 1}, new double[] {50, 2});

            assertThat(Polygons.removeSpikes(sliver, 20.0, MAX_CORNER_ANGLE)).hasSize(3);
        }
    }

    @Nested
    class ComputeSignedArea {
        @Test
        void signed_area_is_positive_and_the_enclosed_area_for_a_counter_clockwise_ring() {
            // The side-10 CCW square encloses 100; a positive sign reports the CCW
            // winding a consumer's fold-guard checks against.
            assertThat(signedArea(bigSquare(10))).isCloseTo(100.0, within());
        }

        @Test
        void signed_area_is_negated_when_the_winding_flips() {
            // Same square traced clockwise: same magnitude, opposite sign - so a sign
            // change between two rings is the fold a caller detects.
            var clockwise = Arrays.asList(
                    new double[] {0, 0},
                    new double[] {0, 10},
                    new double[] {10, 10},
                    new double[] {10, 0});

            assertThat(signedArea(clockwise)).isCloseTo(-100.0, within());
        }

        @Test
        void signed_area_is_zero_for_fewer_than_three_vertices() {
            // No ring can enclose area with under three corners, so both a lone point
            // and a two-vertex degenerate return zero rather than a stray sum.
            assertThat(signedArea(List.of(new double[] {1, 1}))).isCloseTo(0.0, within());
            assertThat(signedArea(Arrays.asList(new double[] {0, 0}, new double[] {10, 0})))
                    .isCloseTo(0.0, within());
        }

        @Test
        void signed_area_is_zero_for_collinear_vertices() {
            // Three collinear points enclose no area; the shoelace sum must cancel to
            // zero rather than report a sliver.
            var collinear = Arrays.asList(
                    new double[] {0, 0}, new double[] {5, 0}, new double[] {10, 0});

            assertThat(signedArea(collinear)).isCloseTo(0.0, within());
        }
    }
}
