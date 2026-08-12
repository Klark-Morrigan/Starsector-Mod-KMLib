package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static kmlib.math.geometry.GeometryTestSupport.buildReferenceSquare;
import static kmlib.math.geometry.GeometryTestSupport.computeSignedArea;
import static kmlib.math.geometry.GeometryTestSupport.within;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the contract of {@link PolygonOffsets#offsetEdgesInward}:
 *  - one segment per edge,
 *  - edges move toward the interior by the offset,
 *  - a thin polygon never collapses (a whole-polygon inset would),
 *  - fewer than two vertices yields nothing.
 *
 * <p>And of {@link PolygonOffsets#insetConvexPolygon}: a convex polygon shrinks to
 * a concentric closed polygon, duplicate vertices are dropped first, an over-inset
 * cell empties rather than inverting, and fewer than three distinct vertices
 * yields nothing.
 *
 * <p>And of {@link PolygonOffsets#insetSelectedEdges}: only the flagged edges pull
 * inward while a kept edge stays on its line (truncated within the inset where a
 * pulled-in edge crosses it), flagging every output edge by origin; insetting
 * every edge matches the whole-polygon inset; an over-inset empties; a collapse to
 * coincident points empties rather than returning a zero-area sliver; and a mask
 * that is not parallel to the edges is rejected. Its per-edge variant reproduces
 * the boolean form for a uniform array, pulls each edge by its own distance, reads
 * a zero entry as "leave on the line", and rejects a non-parallel array.
 *
 * <p>And of {@link PolygonOffsets#insetPolygonByMiter}: a convex square insets to
 * the same concentric square the half-plane inset gives, a concave L keeps its
 * reflex corner (which a half-plane clip would shear off) and moves every corner
 * into the solid, and fewer than three distinct vertices yields nothing. Its
 * per-edge signed variant reduces to the scalar inset for a uniform array, bulges a
 * single negative-distance edge outward, bevels a sharp convex corner where two
 * outward edges would spike, drops a collapsed ring, and rejects a non-parallel
 * array.
 */
final class PolygonOffsetsTest {

    // A generous miter spike limit for the inset tests: high enough that the
    // ordinary right-angle corners keep their crisp miter, so only a deliberately
    // sharp reflex corner bevels.
    private static final double MITER_SPIKE_LIMIT = 4.0;

    @Nested
    class OffsetEdgesInward {

        @Test
        void offset_returns_one_segment_per_edge() {
            assertThat(PolygonOffsets.offsetEdgesInward(buildReferenceSquare(), 2.0))
                .hasSize(4);
        }

        @Test
        void offset_moves_edges_toward_the_interior() {
            // The first edge (0,0)->(10,0) is the bottom edge; offsetting inward by
            // 2 lifts it from y = 0 to y = 2, spanning the same x extent.
            var segments = PolygonOffsets.offsetEdgesInward(buildReferenceSquare(), 2.0);
            var bottom = segments.get(0);

            assertThat(bottom.startY())
                .isCloseTo(2.0, within());
            assertThat(bottom.endY())
                .isCloseTo(2.0, within());
            assertThat(bottom.startX())
                .isCloseTo(0.0, within());
            assertThat(bottom.endX())
                .isCloseTo(10.0, within());
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

            assertThat(PolygonOffsets.offsetEdgesInward(thin, 2.0))
                .hasSize(4);
        }

        @Test
        void offset_returns_empty_for_fewer_than_two_vertices() {
            assertThat(PolygonOffsets.offsetEdgesInward(
                    List.of(new double[] {0, 0}),
                    1.0))
                .isEmpty();
        }
    }

    @Nested
    class InsetConvexPolygon {

        @Test
        void inset_shrinks_a_square_to_a_concentric_square() {
            // The side-10 square inset by 2 is the side-6 square (2,2)..(8,8): each
            // corner is where two adjacent offset edges meet, so the shape stays
            // closed with clean corners rather than overshooting segments.
            var inset = PolygonOffsets.insetConvexPolygon(buildReferenceSquare(), 2.0);

            assertThat(inset)
                .hasSize(4);

            assertThat(inset.get(0))
                .containsExactly(new double[] {2, 2}, within());
            assertThat(inset.get(1))
                .containsExactly(new double[] {8, 2}, within());
            assertThat(inset.get(2))
                .containsExactly(new double[] {8, 8}, within());
            assertThat(inset.get(3))
                .containsExactly(new double[] {2, 8}, within());
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

            assertThat(PolygonOffsets.insetConvexPolygon(withDuplicate, 2.0))
                .hasSize(4);
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

            assertThat(PolygonOffsets.insetConvexPolygon(thin, 2.0))
                .isEmpty();
        }

        @Test
        void inset_returns_empty_for_fewer_than_three_distinct_vertices() {
            assertThat(PolygonOffsets.insetConvexPolygon(
                    Arrays.asList(new double[] {0, 0}, new double[] {10, 0}),
                    1.0))
                .isEmpty();
        }
    }

    @Nested
    class InsetSelectedEdges {
        @Test
        void inset_selected_edges_pulls_in_only_the_flagged_edges() {
            // Square side 10; inset every edge but the right one (index 1). The kept
            // right edge stays at x = 10 while the other three pull in by 2, giving
            // the rectangle (2,2)..(10,8).
            var result = PolygonOffsets.insetSelectedEdges(
                buildReferenceSquare(),
                new boolean[] {true, false, true, true},
                2.0);

            assertThat(result.vertices())
                .hasSize(4);
            assertThat(result.vertices())
                .anySatisfy(v -> assertThat(v[0])
                    .isCloseTo(10.0, within()));
            assertThat(result.vertices())
                .allMatch(v -> v[0] >= 2 - 1e-6
                    && v[0] <= 10 + 1e-6
                    && v[1] >= 2 - 1e-6
                    && v[1] <= 8 + 1e-6);
        }

        @Test
        void inset_selected_edges_flags_the_kept_edge_and_truncates_it_within_the_inset() {

            var result = PolygonOffsets.insetSelectedEdges(
                buildReferenceSquare(),
                new boolean[] {true, false, true, true},
                2.0);

            // Exactly one edge - the un-inset right edge - is flagged not-inset, and
            // it no longer reaches the raw corners (y = 0 and y = 10): both its ends
            // sit within the 2..8 inset band, so the kept edge stays inside the
            // padding rather than poking out to the corner.
            var kept = listKeptEdgesOf(result);

            assertThat(kept)
                .hasSize(1);

            assertThat(kept.get(0)[0][0])
                .isCloseTo(10.0, within());
            assertThat(kept.get(0)[1][0])
                .isCloseTo(10.0, within());
            assertThat(kept.get(0)[0][1])
                .isBetween(2.0 - 1e-6, 8.0 + 1e-6);
            assertThat(kept.get(0)[1][1])
                .isBetween(2.0 - 1e-6, 8.0 + 1e-6);
        }

        @Test
        void inset_selected_edges_matches_the_whole_polygon_inset_when_every_edge_is_flagged() {

            var selective = PolygonOffsets.insetSelectedEdges(
                buildReferenceSquare(),
                new boolean[] {true, true, true, true},
                2.0);

            var whole = PolygonOffsets.insetConvexPolygon(buildReferenceSquare(), 2.0);

            // With every edge flagged, the result is the plain inset - and every
            // output edge is flagged as an inset edge.
            assertThat(selective.vertices())
                .hasSameSizeAs(whole);
            assertThat(selective.edgeIsInset())
                .containsOnly(true);
        }

        @Test
        void inset_selected_edges_empties_when_the_inset_consumes_the_polygon() {

            var thin = Arrays.asList(
                new double[] {0, 0},
                new double[] {10, 0},
                new double[] {10, 1},
                new double[] {0, 1});

            var result = PolygonOffsets.insetSelectedEdges(
                thin,
                new boolean[] {true, true, true, true},
                2.0);

            assertThat(result.vertices())
                .isEmpty();
            assertThat(result.edgeIsInset())
                .isEmpty();
        }

        @Test
        void inset_selected_edges_empties_a_collapse_rather_than_returning_a_sliver() {
            // Insetting every edge of the side-10 square by exactly half its side
            // pulls all four borders through the centre, so the clip collapses to
            // coincident points at (5,5) - a non-zero raw vertex count but no area.
            // It normalises to empty, so a non-empty result is always a real
            // polygon a caller can draw without re-checking its size.
            var result = PolygonOffsets.insetSelectedEdges(
                buildReferenceSquare(),
                new boolean[] {true, true, true, true},
                5.0);

            assertThat(result.vertices())
                .isEmpty();
            assertThat(result.edgeIsInset())
                .isEmpty();
        }

        @Test
        void inset_selected_edges_rejects_a_mask_not_parallel_to_the_edges() {
            assertThatThrownBy(() -> PolygonOffsets.insetSelectedEdges(buildReferenceSquare(), new boolean[] {true}, 2.0))
                .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        void per_edge_uniform_distances_reproduce_the_boolean_scalar_result() {
            // A per-edge array with every entry equal to one distance is the boolean
            // form flagging every edge with that scalar: same inset square, same
            // all-inset flags.
            var perEdge = PolygonOffsets.insetSelectedEdges(
                buildReferenceSquare(),
                new double[] {2.0, 2.0, 2.0, 2.0});

            var booleanScalar = PolygonOffsets.insetSelectedEdges(
                buildReferenceSquare(),
                new boolean[] {true, true, true, true},
                2.0);

            assertVerticesClose(perEdge.vertices(), booleanScalar.vertices());

            assertThat(perEdge.edgeIsInset())
                .containsExactly(booleanScalar.edgeIsInset());
            assertThat(perEdge.edgeIsInset())
                .containsOnly(true);
        }

        @Test
        void per_edge_mixed_distances_pull_each_edge_independently() {
            // Square side 10; the right edge (index 1) stays on its line at distance 0
            // while the other three pull in by their own amounts: bottom by 2 (y ->
            // 2), top by 3 (y -> 7), left by 1 (x -> 1). The result is the rectangle
            // (1,2)..(10,7), each border at its own inset - not one shared channel.
            var result = PolygonOffsets.insetSelectedEdges(
                buildReferenceSquare(),
                new double[] {2.0, 0.0, 3.0, 1.0});

            assertThat(result.vertices())
                .hasSize(4);

            assertThat(result.vertices())
                .allMatch(v -> v[0] >= 1 - 1e-6
                    && v[0] <= 10 + 1e-6
                    && v[1] >= 2 - 1e-6
                    && v[1] <= 7 + 1e-6);

            // The kept right edge is the only non-inset edge and sits at x = 10.
            var kept = listKeptEdgesOf(result);

            assertThat(kept)
                .hasSize(1);

            assertThat(kept.get(0)[0][0])
                .isCloseTo(10.0, within());
            assertThat(kept.get(0)[1][0])
                .isCloseTo(10.0, within());
        }

        @Test
        void per_edge_zero_distance_leaves_that_edge_on_its_line() {
            // A zero entry is exactly the boolean form's false: the edge is kept on
            // its line and flagged not-inset. {2,0,2,2} matches {true,false,true,true}
            // at distance 2 vertex-for-vertex and flag-for-flag.
            var perEdge = PolygonOffsets.insetSelectedEdges(
                buildReferenceSquare(),
                new double[] {2.0, 0.0, 2.0, 2.0});

            var booleanScalar = PolygonOffsets.insetSelectedEdges(
                buildReferenceSquare(),
                new boolean[] {true, false, true, true},
                2.0);

            assertVerticesClose(perEdge.vertices(), booleanScalar.vertices());

            assertThat(perEdge.edgeIsInset())
                .containsExactly(booleanScalar.edgeIsInset());
            assertThat(perEdge.edgeIsInset())
                .contains(false);
        }

        @Test
        void per_edge_rejects_distances_not_parallel_to_the_edges() {
            assertThatThrownBy(() -> PolygonOffsets.insetSelectedEdges(buildReferenceSquare(), new double[] {2.0}))
                .isInstanceOf(IllegalArgumentException.class);
        }

        // Asserts two vertex rings match in size and position, so a per-edge result
        // can be pinned against the boolean form it must reproduce.
        private static void assertVerticesClose(List<double[]> actual, List<double[]> expected) {

            assertThat(actual)
                .hasSameSizeAs(expected);

            for (var i = 0; i < expected.size(); i++) {

                assertThat(actual.get(i))
                    .containsExactly(expected.get(i), within());
            }
        }

        // The edges a selective inset left un-inset (kept seams), each as its two
        // endpoints - what a caller strokes as an interior line rather than a
        // border.
        private static List<double[][]> listKeptEdgesOf(PolygonOffsets.SelectiveInset result) {

            var kept = new java.util.ArrayList<double[][]>();
            var vertices = result.vertices();
            var flags = result.edgeIsInset();

            for (var i = 0; i < vertices.size(); i++) {

                if (!flags[i]) {

                    kept.add(new double[][] {
                        vertices.get(i),
                        vertices.get((i + 1) % vertices.size())});
                }
            }
            return kept;
        }
    }

    @Nested
    class RemoveReversedLoops {
        // A bowtie: the square's top two corners are swapped, so the ring crosses itself
        // in the middle and its upper half comes back wound against the lower one.
        private List<double[]> buildBowtieRing() {
            return Arrays.asList(
                new double[] {0, 0},
                new double[] {10, 0},
                new double[] {0, 10},
                new double[] {10, 10});
        }

        @Test
        void removal_splices_out_a_loop_wound_against_the_ring() {

            var cleaned = PolygonOffsets.removeReversedLoops(buildBowtieRing(), 0);

            // The reversed half collapses to the crossing at (5,5), leaving the triangle
            // that was wound with the ring.
            assertThat(cleaned)
                .hasSize(3);

            assertThat(cleaned.get(0))
                .containsExactly(new double[] {0, 0}, within());
            assertThat(cleaned.get(1))
                .containsExactly(new double[] {10, 0}, within());
            assertThat(cleaned.get(2))
                .containsExactly(new double[] {5, 5}, within());
        }

        @Test
        void removal_leaves_a_ring_that_does_not_fold_untouched() {
            assertThat(PolygonOffsets.removeReversedLoops(buildReferenceSquare(), 0))
                .containsExactlyElementsOf(buildReferenceSquare());
        }

        @Test
        void removal_leaves_a_fold_outside_the_window_alone() {
            // Two edges must be at least two apart along the ring to be able to cross -
            // adjacent ones share a vertex - so a window of one compares no pair at all and
            // every fold survives. Pinned because the window is the cheap scan's cost, and
            // a caller who narrows it past this stops cleaning anything rather than
            // cleaning less.
            assertThat(PolygonOffsets.removeReversedLoops(buildBowtieRing(), 1))
                .containsExactlyElementsOf(buildBowtieRing());
        }

        @Test
        void removal_leaves_nothing_for_a_second_pass_to_find() {
            // The method splices one fold per scan and repeats, so "no fold is left" is the
            // termination condition rather than something the caller checks. Idempotence
            // pins it without having to hand-build a shape that folds a chosen number of
            // times - a construction easy to get wrong and hard to read.
            var once = PolygonOffsets.removeReversedLoops(buildBowtieRing(), 0);

            assertThat(PolygonOffsets.removeReversedLoops(once, 0))
                .containsExactlyElementsOf(once);
        }

        @Test
        void removal_leaves_a_clockwise_ring_clockwise() {
            // Winding is read from the ring itself rather than assumed counter-clockwise,
            // so a clockwise input keeps its own sense and only folds against IT are cut.
            var clockwise = Arrays.asList(
                new double[] {0, 10},
                new double[] {10, 10},
                new double[] {10, 0},
                new double[] {0, 0});

            var cleaned = PolygonOffsets.removeReversedLoops(clockwise, 0);

            assertThat(computeSignedArea(cleaned))
                .isLessThan(0.0);
            assertThat(cleaned)
                .containsExactlyElementsOf(clockwise);
        }

        @Test
        void removal_returns_the_input_below_three_vertices() {

            var line = Arrays.asList(new double[] {0, 0}, new double[] {10, 0});

            assertThat(PolygonOffsets.removeReversedLoops(line, 0))
                .isSameAs(line);
        }
    }

    @Nested
    class InsetPolygonByMiter {

        @Test
        void miter_inset_matches_the_convex_inset_on_a_square() {
            // On a convex shape the miter join and the half-plane clip agree: the
            // side-10 square insets by 2 to the concentric (2,2)..(8,8) square.
            var inset = PolygonOffsets.insetPolygonByMiter(buildReferenceSquare(), 2.0, MITER_SPIKE_LIMIT);

            assertThat(inset)
                .hasSize(4);

            assertThat(inset.get(0))
                .containsExactly(new double[] {2, 2}, within());
            assertThat(inset.get(1))
                .containsExactly(new double[] {8, 2}, within());
            assertThat(inset.get(2))
                .containsExactly(new double[] {8, 8}, within());
            assertThat(inset.get(3))
                .containsExactly(new double[] {2, 8}, within());
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
                new double[] {0, 0},
                new double[] {30, 0},
                new double[] {30, 10},
                new double[] {10, 10},
                new double[] {10, 30},
                new double[] {0, 30});

            var inset = PolygonOffsets.insetPolygonByMiter(lShape, 2.0, MITER_SPIKE_LIMIT);

            assertThat(inset)
                .hasSize(7);

            assertThat(inset.get(0))
                .containsExactly(new double[] {2, 2}, within());
            assertThat(inset.get(3))
                .containsExactly(new double[] {10, 8}, within());
            assertThat(inset.get(4))
                .containsExactly(new double[] {8, 10}, within());

            // The reflex corner never spikes to its miter crossing at (8,8).
            assertThat(inset)
                .noneSatisfy(v -> assertThat(v)
                    .containsExactly(new double[] {8, 8}, within()));
        }

        @Test
        void miter_inset_bevels_a_reflex_spike_instead_of_spiking_inward() {
            // A side-200 CCW square with a narrow spike jutting into its interior
            // from the bottom edge - a sharp reflex corner at (100,15). A plain miter
            // would shoot that corner's join far up into the interior (to ~y=78), a
            // stray inward spike. Bevelling it keeps the offset near the bottom band,
            // so no vertex lands in the wide empty mid-height of the square.
            var squareWithSpike = Arrays.asList(
                new double[] {0, 0},
                new double[] {95, 0},
                new double[] {100, 15},
                new double[] {105, 0},
                new double[] {200, 0},
                new double[] {200, 200},
                new double[] {0, 200});

            var inset = PolygonOffsets.insetPolygonByMiter(squareWithSpike, 20.0, MITER_SPIKE_LIMIT);

            assertThat(inset)
                .isNotEmpty();
            assertThat(computeSignedArea(inset))
                .isPositive();

            // The only corners are the inset bottom band (y ~ 20) and the top (y ~
            // 180); a spike would place a vertex in the empty middle.
            assertThat(inset)
                .noneMatch(v -> v[1] > 30
                    && v[1] < 150);
        }

        @Test
        void miter_inset_returns_empty_for_fewer_than_three_distinct_vertices() {
            assertThat(PolygonOffsets.insetPolygonByMiter(
                    Arrays.asList(new double[] {0, 0}, new double[] {10, 0}),
                    1.0,
                    MITER_SPIKE_LIMIT))
                .isEmpty();
        }

        @Test
        void per_edge_uniform_distances_reproduce_the_scalar_inset() {
            // Every edge shifted the same distance is the scalar inset: the side-10
            // square insets by 2 to the concentric (2,2)..(8,8) square.
            var uniform = new double[] {2.0, 2.0, 2.0, 2.0};
            var inset = PolygonOffsets.insetPolygonByMiter(buildReferenceSquare(), uniform, MITER_SPIKE_LIMIT);

            assertThat(inset)
                .hasSize(4);

            assertThat(inset.get(0))
                .containsExactly(new double[] {2, 2}, within());
            assertThat(inset.get(1))
                .containsExactly(new double[] {8, 2}, within());
            assertThat(inset.get(2))
                .containsExactly(new double[] {8, 8}, within());
            assertThat(inset.get(3))
                .containsExactly(new double[] {2, 8}, within());
        }

        @Test
        void per_edge_negative_distance_bulges_that_edge_outward() {
            // Only the bottom edge (edge 0) is pushed outward by 2 while the other
            // three inset inward by 2: the bottom corners drop below the original
            // y=0 line to y=-2, the outward bulge, and the top stays inset.
            var distances = new double[] {-2.0, 2.0, 2.0, 2.0};
            var inset = PolygonOffsets.insetPolygonByMiter(buildReferenceSquare(), distances, MITER_SPIKE_LIMIT);

            assertThat(inset)
                .hasSize(4);

            assertThat(inset.get(0))
                .containsExactly(new double[] {2, -2}, within());
            assertThat(inset.get(1))
                .containsExactly(new double[] {8, -2}, within());
            assertThat(inset.get(2))
                .containsExactly(new double[] {8, 8}, within());
            assertThat(inset.get(3))
                .containsExactly(new double[] {2, 8}, within());
        }

        @Test
        void per_edge_two_outward_edges_bevel_instead_of_spiking_at_a_sharp_corner() {
            // A sharp convex tip at the origin, its two edges (0 and 2) both pushed
            // outward by 5. Their offset lines would cross ~100 units out along -x -
            // a self-intersecting spike; the spike guard bevels the corner instead,
            // so no output vertex lands out at that spike.
            var sharpTriangle = Arrays.asList(
                new double[] {0, 0},
                new double[] {200, -10},
                new double[] {200, 10});

            var distances = new double[] {-5.0, 2.0, -5.0};
            var inset = PolygonOffsets.insetPolygonByMiter(sharpTriangle, distances, MITER_SPIKE_LIMIT);

            assertThat(inset)
                .isNotEmpty();
            assertThat(inset)
                .noneMatch(vertex -> vertex[0] < -20);
        }

        @Test
        void per_edge_returns_empty_for_fewer_than_three_distinct_vertices() {
            // A duplicate vertex collapses the "triangle" to two distinct points; the
            // distance-preserving dedup keeps the array parallel and still drops it.
            var collapsed = Arrays.asList(
                new double[] {0, 0},
                new double[] {0, 0},
                new double[] {10, 0});

            assertThat(PolygonOffsets.insetPolygonByMiter(
                    collapsed,
                    new double[] {1.0, 1.0, 1.0},
                    MITER_SPIKE_LIMIT))
                .isEmpty();
        }

        @Test
        void per_edge_rejects_distances_not_parallel_to_the_edges() {
            assertThatThrownBy(() -> PolygonOffsets.insetPolygonByMiter(
                    buildReferenceSquare(),
                    new double[] {1.0, 1.0},
                    MITER_SPIKE_LIMIT))
                .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
