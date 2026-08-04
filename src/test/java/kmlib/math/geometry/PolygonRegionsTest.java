package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static kmlib.math.geometry.GeometryTestSupport.bigSquare;
import static kmlib.math.geometry.GeometryTestSupport.signedArea;
import static kmlib.math.geometry.GeometryTestSupport.within;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link PolygonRegions#computeSignedArea}: a counter-clockwise
 * ring reports a positive area equal to the region it encloses, reversing the
 * winding negates it, and a ring that encloses nothing (fewer than three vertices,
 * or collinear vertices) is zero - the sign and magnitude a consumer's fold-guard
 * relies on.
 *
 * <p>And of {@link PolygonRegions#isPointInsideRing}: a point within a convex ring is
 * inside and one beyond it outside regardless of winding, a concave ring's notch reads
 * as outside while its arms read as inside, and a ring too short to enclose area holds
 * no point.
 *
 * <p>And of {@link PolygonRegions#groupRingsIntoRegions}: an outer ring alone is a region
 * with nothing cut out of it, two disjoint bodies each keep their own hole, a hole goes to
 * the ring containing it rather than the one listed first, an island in a lake takes its own
 * hole back off the landmass around it, and a hole no ring contains - or a ring too short to
 * enclose area - bounds nothing and is dropped.
 *
 * <p>And of {@link PolygonRegions#findLineInteriorSpans}: a convex ring yields one
 * span, a concave ring the line exits and re-enters yields separate spans (no chord
 * bridges the notch), a hole splits a span around it, and a miss, a corner graze, or
 * a degenerate direction yields nothing.
 *
 * <p>And of {@link PolygonRegions#findBandInteriorSpans}: a band clear of every
 * border keeps its whole span, a band splits around a parallel border its centreline
 * clears (its edge crosses the intrusion), a band wider than the region empties, a
 * zero-thickness band reduces to the line test, and a degenerate direction yields
 * nothing.
 */
final class PolygonRegionsTest {

    @Nested
    class ComputeSignedArea {
        @Test
        void signed_area_is_positive_and_the_enclosed_area_for_a_counter_clockwise_ring() {
            // The side-10 CCW square encloses 100; a positive sign reports the CCW
            // winding a consumer's fold-guard checks against.
            assertThat(signedArea(bigSquare(10)))
                .isCloseTo(100.0, within());
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

            assertThat(signedArea(clockwise))
                .isCloseTo(-100.0, within());
        }

        @Test
        void signed_area_is_zero_for_fewer_than_three_vertices() {
            // No ring can enclose area with under three corners, so both a lone point
            // and a two-vertex degenerate return zero rather than a stray sum.
            assertThat(signedArea(List.of(new double[] {1, 1})))
                .isCloseTo(0.0, within());
            assertThat(signedArea(Arrays.asList(new double[] {0, 0}, new double[] {10, 0})))
                .isCloseTo(0.0, within());
        }

        @Test
        void signed_area_is_zero_for_collinear_vertices() {
            // Three collinear points enclose no area; the shoelace sum must cancel to
            // zero rather than report a sliver.
            var collinear = Arrays.asList(
                new double[] {0, 0},
                new double[] {5, 0},
                new double[] {10, 0});

            assertThat(signedArea(collinear))
            .isCloseTo(0.0, within());
        }
    }

    @Nested
    class IsPointInsideRing {
        // A U shape: the side-10 square with a notch cut down from the top between
        // x=4 and x=6, so a point in the notch sits within the ring's bounds but
        // outside the ring itself.
        private List<double[]> buildNotchedSquare() {
            return Arrays.asList(
                new double[] {0, 0},
                new double[] {10, 0},
                new double[] {10, 10},
                new double[] {6, 10},
                new double[] {6, 4},
                new double[] {4, 4},
                new double[] {4, 10},
                new double[] {0, 10});
        }

        @Test
        void a_point_within_a_convex_ring_is_inside() {
            assertThat(PolygonRegions.isPointInsideRing(bigSquare(10), 5, 5))
                .isTrue();
        }

        @Test
        void a_point_beyond_a_convex_ring_is_outside() {
            assertThat(PolygonRegions.isPointInsideRing(bigSquare(10), 15, 5))
                .isFalse();
        }

        @Test
        void the_verdict_is_blind_to_the_rings_winding() {
            // Same square traced clockwise: the even-odd rule counts crossings, so
            // reversing the winding cannot change what the ring encloses.
            var clockwise = new ArrayList<>(bigSquare(10));
            Collections.reverse(clockwise);

            assertThat(PolygonRegions.isPointInsideRing(clockwise, 5, 5))
                .isTrue();
        }

        @Test
        void a_point_in_a_concave_rings_notch_is_outside() {
            // (5, 7) is inside the bounding box and between the U's two arms, but the
            // ray crosses two edges on its way out - even, so outside.
            assertThat(PolygonRegions.isPointInsideRing(buildNotchedSquare(), 5, 7))
                .isFalse();
        }

        @Test
        void a_point_in_a_concave_rings_arm_is_inside() {
            // (2, 7) sits in the U's left arm: one crossing, so inside.
            assertThat(PolygonRegions.isPointInsideRing(buildNotchedSquare(), 2, 7))
                .isTrue();
        }

        @Test
        void a_ring_too_short_to_enclose_area_holds_no_point() {
            // Two vertices bound nothing, so the point they straddle is still outside.
            var segment = Arrays.asList(
                new double[] {0, 0},
                new double[] {10, 0});

            assertThat(PolygonRegions.isPointInsideRing(segment, 5, 0))
                .isFalse();
        }
    }

    @Nested
    class GroupRingsIntoRegions {

        @Test
        void a_lone_outer_ring_becomes_one_region_with_nothing_cut_out_of_it() {
            // The common case, and the one that must pay nothing for the machinery: a
            // single body with no hole comes back as itself. Held by identity, since the
            // grouping sorts the rings it was handed rather than rebuilding them.
            var body = bigSquare(10);

            var regions = PolygonRegions.groupRingsIntoRegions(List.of(body));

            assertThat(regions)
                .hasSize(1);
            assertThat(regions.get(0).outerRing())
                .isSameAs(body);
            assertThat(regions.get(0).holeRings())
                .isEmpty();
        }

        @Test
        void two_disjoint_bodies_keep_the_hole_each_was_given() {
            // The failure this rules out is silent: swap the two holes and every ring
            // still draws, in the right place, with only the filled area wrong.
            var leftBody = buildSquareAt(0, 0, 10);
            var rightBody = buildSquareAt(100, 0, 10);
            var leftHole = buildClockwiseSquareAt(3, 3, 4);
            var rightHole = buildClockwiseSquareAt(103, 3, 4);

            var regions = PolygonRegions.groupRingsIntoRegions(
                List.of(leftBody, rightBody, leftHole, rightHole));

            assertThat(regions)
                .hasSize(2);
            assertThat(regions.get(0).holeRings())
                .containsExactly(leftHole);
            assertThat(regions.get(1).holeRings())
                .containsExactly(rightHole);
        }

        @Test
        void a_hole_lands_in_its_container_and_not_in_whichever_outer_ring_came_first() {
            // The second body owns the hole, so a rule that took the first outer ring it
            // walked - or simply the first in the list - would put it in the first.
            var firstBody = buildSquareAt(0, 0, 10);
            var containingBody = buildSquareAt(100, 0, 10);
            var hole = buildClockwiseSquareAt(103, 3, 4);

            var regions = PolygonRegions.groupRingsIntoRegions(
                List.of(firstBody, containingBody, hole));

            assertThat(regions.get(0).holeRings()).isEmpty();
            assertThat(regions.get(1).holeRings()).containsExactly(hole);
        }

        @Test
        void an_island_in_a_lake_takes_its_own_hole_from_the_landmass_around_it() {
            // Three nested bodies: a landmass, a lake cut out of it, an island in the lake,
            // and a pond on the island. Both the landmass and the island contain the pond,
            // and only the island is the body it is actually cut from - which is what
            // smallest-container decides and first-container would get backwards.
            var landmass = buildSquareAt(0, 0, 100);
            var lake = buildClockwiseSquareAt(10, 10, 80);
            var island = buildSquareAt(20, 20, 60);
            var pond = buildClockwiseSquareAt(30, 30, 40);

            var regions = PolygonRegions.groupRingsIntoRegions(
                List.of(landmass, island, lake, pond));

            assertThat(regions.get(0).outerRing()).isSameAs(landmass);
            assertThat(regions.get(0).holeRings()).containsExactly(lake);

            assertThat(regions.get(1).outerRing()).isSameAs(island);
            assertThat(regions.get(1).holeRings()).containsExactly(pond);
        }

        @Test
        void a_hole_no_outer_ring_contains_is_dropped_rather_than_carried() {
            // It cuts nothing out of anything. Kept, it would be stroked as a stray loop
            // over an area that is not the region's.
            var body = buildSquareAt(0, 0, 10);
            var strayHole = buildClockwiseSquareAt(100, 100, 4);
            var regions = PolygonRegions.groupRingsIntoRegions(List.of(body, strayHole));

            assertThat(regions)
                .hasSize(1);
            assertThat(regions.get(0).holeRings())
                .isEmpty();
        }

        @Test
        void rings_too_short_to_enclose_area_bound_no_region_either_way() {
            // A two-vertex ring has no winding to sort it by and no area to fill, so it is
            // neither an outer ring nor a hole.
            var segment = List.of(
                new double[] {0, 0},
                new double[] {10, 0});

            assertThat(PolygonRegions.groupRingsIntoRegions(List.of(segment)))
                .isEmpty();
        }

        // A CCW square of the given side, anchored where asked - the outer-ring shape, placed
        // so containment rather than winding is what a case turns on.
        private List<double[]> buildSquareAt(double x, double y, double side) {
            return List.of(
                new double[] {x, y},
                new double[] {x + side, y},
                new double[] {x + side, y + side},
                new double[] {x, y + side});
        }

        // The same square wound clockwise, which is what marks a ring as a hole.
        private List<double[]> buildClockwiseSquareAt(double x, double y, double side) {
            var ring = new ArrayList<>(buildSquareAt(x, y, side));
            Collections.reverse(ring);
            return ring;
        }
    }

    @Nested
    class FindLineInteriorSpans {
        @Test
        void interior_spans_yield_one_span_across_a_convex_ring() {
            // A horizontal line through the centre of the side-10 square enters at
            // x=0 and leaves at x=10: one span, parameters measured from the
            // through-point at x=5.
            var spans = PolygonRegions.findLineInteriorSpans(List.of(bigSquare(10)),
                new DirectedLine(5, 5, 1, 0));

            assertThat(spans)
                .hasSize(1);
            assertThat(spans.get(0)[0])
                .isCloseTo(-5.0, within());
            assertThat(spans.get(0)[1])
                .isCloseTo(5.0, within());
        }

        @Test
        void interior_spans_split_where_the_line_exits_and_reenters_a_concave_ring() {
            // A U shape (side-10 square with a notch cut down from the top between
            // x=4 and x=6): a horizontal line at y=7 crosses both arms but the
            // stretch between them lies in the notch, outside the region - so two
            // spans come back, never one chord bridging the gap.
            var uShape = Arrays.asList(
                new double[] {0, 0},
                new double[] {10, 0},
                new double[] {10, 10},
                new double[] {6, 10},
                new double[] {6, 4},
                new double[] {4, 4},
                new double[] {4, 10},
                new double[] {0, 10});

            var spans = PolygonRegions.findLineInteriorSpans(List.of(uShape),
                new DirectedLine(5, 7, 1, 0));

            assertThat(spans).hasSize(2);

            assertThat(spans.get(0)[0]).isCloseTo(-5.0, within());
            assertThat(spans.get(0)[1]).isCloseTo(-1.0, within());
            assertThat(spans.get(1)[0]).isCloseTo(1.0, within());
            assertThat(spans.get(1)[1]).isCloseTo(5.0, within());
        }

        @Test
        void interior_spans_split_around_a_hole_ring() {
            // A side-20 square with a hole from x=8..12: the line through the middle
            // is interior only outside the hole, so the hole splits the single span
            // in two.
            var hole = Arrays.asList(
                new double[] {8, 8},
                new double[] {12, 8},
                new double[] {12, 12},
                new double[] {8, 12});

            var spans = PolygonRegions.findLineInteriorSpans(
                List.of(
                    bigSquare(20), hole),
                    new DirectedLine(10, 10, 1, 0));

            assertThat(spans).hasSize(2);

            assertThat(spans.get(0)[0]).isCloseTo(-10.0, within());
            assertThat(spans.get(0)[1]).isCloseTo(-2.0, within());
            assertThat(spans.get(1)[0]).isCloseTo(2.0, within());
            assertThat(spans.get(1)[1]).isCloseTo(10.0, within());
        }

        @Test
        void interior_spans_are_empty_when_the_line_misses_the_ring() {
            var spans = PolygonRegions.findLineInteriorSpans(List.of(bigSquare(10)),
                new DirectedLine(5, 50, 1, 0));

            assertThat(spans).isEmpty();
        }

        @Test
        void interior_spans_are_empty_when_the_line_only_grazes_a_corner() {
            // A diagonal through the square's corner touches at a single point: the
            // two crossings coincide, so the zero-length interval between them is no
            // span.
            var spans = PolygonRegions.findLineInteriorSpans(
                List.of(bigSquare(10)),
                new DirectedLine(0, 0, 1, -1));

            assertThat(spans).isEmpty();
        }

        @Test
        void interior_spans_are_empty_for_a_degenerate_direction() {
            // A zero direction defines no line to cross, so there is nothing to span.
            var spans = PolygonRegions.findLineInteriorSpans(
                List.of(bigSquare(10)),
                new DirectedLine(5, 5, 0, 0));

            assertThat(spans).isEmpty();
        }
    }

    @Nested
    class FindBandInteriorSpans {
        @Test
        void band_inside_a_convex_ring_keeps_its_whole_span() {
            // A horizontal band through the side-20 square, its rails from y=7 to
            // y=13, stays clear of the vertical walls, so the band span equals the
            // line span: x=0..20, parameters from the through-point at x=10.
            var spans = PolygonRegions.findBandInteriorSpans(
                List.of(bigSquare(20)),
                new DirectedLine(10, 10, 1, 0), 3);

            assertThat(spans).hasSize(1);

            assertThat(spans.get(0)[0]).isCloseTo(-10.0, within());
            assertThat(spans.get(0)[1]).isCloseTo(10.0, within());
        }

        @Test
        void band_splits_around_a_parallel_border_the_centreline_clears() {
            // A 20-wide, 10-tall room with a rectangular notch bitten down from the
            // top to y=6 over x=8..12. A horizontal centreline at y=5 clears the notch
            // whole, but a band of half-thickness 2.5 lifts its upper rails to y=6.25
            // and y=7.5 - inside the bite over x=8..12 - so the band span splits into
            // the two clear stretches [-10, -2] and [2, 10] the centreline alone missed.
            var notchedRoom = Arrays.asList(
                new double[] {0, 0},
                new double[] {20, 0},
                new double[] {20, 10},
                new double[] {12, 10},
                new double[] {12, 6},
                new double[] {8, 6},
                new double[] {8, 10},
                new double[] {0, 10});

            var spans = PolygonRegions.findBandInteriorSpans(
                List.of(notchedRoom),
                new DirectedLine(10, 5, 1, 0), 2.5);

            assertThat(spans).hasSize(2);

            assertThat(spans.get(0)[0]).isCloseTo(-10.0, within());
            assertThat(spans.get(0)[1]).isCloseTo(-2.0, within());
            assertThat(spans.get(1)[0]).isCloseTo(2.0, within());
            assertThat(spans.get(1)[1]).isCloseTo(10.0, within());
        }

        @Test
        void band_is_empty_when_wider_than_the_region() {
            // Half-thickness 6 in a side-10 square lifts the outer rails to y=-1 and
            // y=11, both outside; a rail with no interior empties the whole band.
            var spans = PolygonRegions.findBandInteriorSpans(List.of(
                bigSquare(10)),
                new DirectedLine(5, 5, 1, 0), 6);

            assertThat(spans).isEmpty();
        }

        @Test
        void band_with_zero_half_thickness_matches_the_line_test() {
            // Every rail collapses onto the centreline, so the band test reduces
            // exactly to the line test on the same concave U shape.
            var uShape = Arrays.asList(
                new double[] {0, 0},
                new double[] {10, 0},
                new double[] {10, 10},
                new double[] {6, 10},
                new double[] {6, 4},
                new double[] {4, 4},
                new double[] {4, 10},
                new double[] {0, 10});

            var bandSpans = PolygonRegions.findBandInteriorSpans(
                List.of(uShape),
                new DirectedLine(5, 7, 1, 0), 0);
            var lineSpans = PolygonRegions.findLineInteriorSpans(
                List.of(uShape),
                new DirectedLine(5, 7, 1, 0));

            assertThat(bandSpans).hasSize(lineSpans.size());

            for (var i = 0; i < lineSpans.size(); i++) {

                assertThat(bandSpans.get(i)[0])
                    .isCloseTo(lineSpans.get(i)[0], within());

                assertThat(bandSpans.get(i)[1])
                    .isCloseTo(lineSpans.get(i)[1], within());
            }
        }

        @Test
        void band_is_empty_for_a_degenerate_direction() {
            var spans = PolygonRegions.findBandInteriorSpans(
                List.of(bigSquare(10)),
                new DirectedLine(5, 5, 0, 0), 2);

            assertThat(spans).isEmpty();
        }
    }
}
