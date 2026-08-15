package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static kmlib.math.geometry.GeometryTestSupport.assertThatPointsAre;
import static kmlib.math.geometry.GeometryTestSupport.buildAssertionSlack;
import static kmlib.math.geometry.GeometryTestSupport.computeSignedArea;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link PolylineBands#strokeToTriangles}, and of
 * {@link PolylineBands#strokeSpansToTriangles} splitting one such band between the spans it
 * was asked for.
 *
 * <p>The single-stroke cases: a straight centreline
 * strokes the rectangle around it, a corner mitres so the band covers both its arms
 * once, a corner past the spike limit bevels the outside of the turn away, a corner
 * whose miter would outrun the segments it joins keeps the band's full width across the
 * turn rather than either folding into a bowtie or necking to the centreline, a point
 * the centreline runs straight through leaves the band unbroken, repeated points are one
 * point, and a centreline with nothing to stroke - one distinct point, or no width -
 * strokes nothing.
 *
 * <p>The span cases are about one thing the single-stroke ones cannot state: a boundary
 * between two spans is a join rather than two square ends butted together, which is the
 * whole reason for stroking a multi-coloured band once instead of a piece at a time. The
 * rest of them are the questions two spans raise that one band never does - which of them a
 * bevelled corner's wedge falls to, and whether their shared point is stated once or twice.
 *
 * <p>Area carries most of the assertions: the corners a join produces are stated
 * exactly where a case is about one join, but whether the band covers what it should
 * is a question about the whole of it, and the order triangles come out in is the
 * stroker's own business.
 */
final class PolylineBandsTest {

    // Band width 2 puts each rail a whole unit off the centreline, so every corner the
    // cases below assert lands on whole coordinates.
    private static final double WIDTH = 2.0;

    // A generous miter spike limit, as the inset suites use: high enough that a right
    // angle keeps its crisp mitred corner, so the bevel cases below are asserting a
    // deliberately tighter limit rather than the default behaviour of the fixture.
    private static final double MITER_SPIKE_LIMIT = 4.0;

    // The slack a corner is matched at, the geometry suites' own, taken from it rather
    // than restated so the two cannot drift apart.
    private static final double CORNER_SLACK = buildAssertionSlack().value;

    // A right angle: 10 east from the origin, then 10 north. Both arms are far longer
    // than the band is wide, so the corner has all the room a miter could want and the
    // spike limit is the only thing that decides it.
    private static final List<double[]> RIGHT_ANGLE = List.of(
        new double[] {0, 0},
        new double[] {10, 0},
        new double[] {10, 10});

    @Nested
    class StrokeToTriangles {

        @Test
        void straight_centreline_strokes_the_rectangle_around_it() {
            // Half the width either side of the line from (0,0) to (10,0), as the two
            // triangles of one quad.
            var band = PolylineBands.strokeToTriangles(
                List.of(new double[] {0, 0}, new double[] {10, 0}),
                WIDTH,
                MITER_SPIKE_LIMIT);

            assertThatPointsAre(band, List.of(
                new double[] {0, 1},
                new double[] {0, -1},
                new double[] {10, -1},
                new double[] {0, 1},
                new double[] {10, -1},
                new double[] {10, 1}));
        }

        @Test
        void mitred_corner_covers_both_arms_of_the_turn_once() {
            // Each arm is a 10-by-2 rectangle, so the band is 40 - the square the two
            // arms would otherwise share on the inside of the turn is exactly the square
            // the miter adds on the outside. A band that covered less would have a notch
            // at the corner; one that covered more would be stacking its two arms there.
            var band = PolylineBands.strokeToTriangles(
                RIGHT_ANGLE,
                WIDTH,
                MITER_SPIKE_LIMIT);

            assertThat(computeTotalTriangleArea(band))
                .isCloseTo(40.0, buildAssertionSlack());
        }

        @Test
        void mitred_corner_turns_on_its_two_miter_points() {
            // The rails either side of the corner at (10,0) cross at (9,1) inside the
            // turn and (11,-1) outside it, and the band turns on those rather than on
            // the square ends the two arms would otherwise stop at.
            var band = PolylineBands.strokeToTriangles(
                RIGHT_ANGLE,
                WIDTH,
                MITER_SPIKE_LIMIT);

            assertThat(hasCorner(band, new double[] {9, 1}))
                .isTrue();
            assertThat(hasCorner(band, new double[] {11, -1}))
                .isTrue();
        }

        @Test
        void corner_past_the_spike_limit_bevels_the_outside_of_the_turn() {
            // The outer miter at (11,-1) stands root-two from the corner, past a limit of
            // one half-width, so the outer rail cuts straight across from (10,-1) to
            // (11,0) instead. That takes the half-unit triangle behind the cut off the
            // mitred band's 40, and the point of the miter is gone from the band.
            var band = PolylineBands.strokeToTriangles(RIGHT_ANGLE, WIDTH, 1.0);

            assertThat(computeTotalTriangleArea(band))
                .isCloseTo(39.5, buildAssertionSlack());
            assertThat(hasCorner(band, new double[] {10, -1}))
                .isTrue();
            assertThat(hasCorner(band, new double[] {11, 0}))
                .isTrue();
            assertThat(hasCorner(band, new double[] {11, -1}))
                .isFalse();
        }

        @Test
        void a_turn_the_other_way_bevels_the_outside_of_its_own_turn() {
            // The mirror of the case above: 10 east then 10 south, so the outside of the
            // turn is now the left of the band rather than its right. The band has to
            // read the sides off the turn - a stroker with the sides fixed passes both
            // the area and the corner assertions above and puts the bevel on the inside
            // here, cutting the corner out of the band instead of off it.
            var band = PolylineBands.strokeToTriangles(
                List.of(
                    new double[] {0, 0},
                    new double[] {10, 0},
                    new double[] {10, -10}),
                WIDTH,
                1.0);

            assertThat(computeTotalTriangleArea(band))
                .isCloseTo(39.5, buildAssertionSlack());
            assertThat(hasCorner(band, new double[] {9, -1}))
                .isTrue();
            assertThat(hasCorner(band, new double[] {10, 1}))
                .isTrue();
            assertThat(hasCorner(band, new double[] {11, 0}))
                .isTrue();
            assertThat(hasCorner(band, new double[] {11, 1}))
                .isFalse();
        }

        @Test
        void corner_whose_miter_outruns_its_segments_keeps_the_bands_full_width() {
            // A one-long segment between two right angles: each miter reaches root-two
            // back along it, and the two of them together reach farther than the segment
            // is long, so their rails would cross and the band would fold into a bowtie.
            // Both corners give the miter up - and give it up to the plain offset each
            // segment has of its own, not to the centreline. So the short segment carries
            // the full-width rectangle from (9,0) to (11,1), where a rail brought in to
            // the centreline would have left it a unit wide and tapered the two long arms
            // into it.
            //
            // The area is the three rectangles - 20, 2 and 20 - plus a half-unit wedge
            // holding each bevelled corner open. It counts the two arms' overlap across
            // the inside of each turn twice, which is what a band with nowhere to put its
            // width but over itself is: brighter there when translucent, and whole.
            var hairpin = List.of(
                new double[] {0, 0},
                new double[] {10, 0},
                new double[] {10, 1},
                new double[] {0, 1});

            var band = PolylineBands.strokeToTriangles(hairpin, WIDTH, MITER_SPIKE_LIMIT);

            assertThat(computeTotalTriangleArea(band))
                .isCloseTo(43.0, buildAssertionSlack());
            assertThat(hasCorner(band, new double[] {9, 0}))
                .isTrue();
            assertThat(hasCorner(band, new double[] {9, 1}))
                .isTrue();
        }

        @Test
        void point_the_centreline_runs_straight_through_leaves_the_band_unbroken() {
            // A point with no turn at it has no join to make: the two quads either side
            // meet exactly, so the band is the 10-by-2 rectangle and carries no bevel.
            var band = PolylineBands.strokeToTriangles(
                List.of(new double[] {0, 0}, new double[] {5, 0}, new double[] {10, 0}),
                WIDTH,
                MITER_SPIKE_LIMIT);

            assertThat(computeTotalTriangleArea(band))
                .isCloseTo(20.0, buildAssertionSlack());
            assertThat(band)
                .hasSize(12);
        }

        @Test
        void a_point_repeated_on_the_centreline_is_one_point() {
            // The step between the two is no step at all - it names no direction to
            // offset the rails along - so the band is the one the centreline without it
            // strokes, rather than a degenerate piece plus that band.
            var repeated = PolylineBands.strokeToTriangles(
                List.of(
                    new double[] {0, 0},
                    new double[] {5, 0},
                    new double[] {5, 0},
                    new double[] {10, 0}),
                WIDTH,
                MITER_SPIKE_LIMIT);

            var once = PolylineBands.strokeToTriangles(
                List.of(new double[] {0, 0}, new double[] {5, 0}, new double[] {10, 0}),
                WIDTH,
                MITER_SPIKE_LIMIT);

            assertThatPointsAre(repeated, once);
        }

        @Test
        void a_centreline_of_one_distinct_point_strokes_nothing() {
            // No direction, so no sides: there is nothing to put a width either side of.
            var band = PolylineBands.strokeToTriangles(
                List.of(new double[] {3, 3}, new double[] {3, 3}),
                WIDTH,
                MITER_SPIKE_LIMIT);

            assertThat(band)
                .isEmpty();
        }

        @Test
        void a_band_of_no_width_strokes_nothing() {
            // Both rails would sit on the centreline, enclosing no area.
            var band = PolylineBands.strokeToTriangles(
                List.of(new double[] {0, 0}, new double[] {10, 0}),
                0.0,
                MITER_SPIKE_LIMIT);

            assertThat(band)
                .isEmpty();
        }
    }

    @Nested
    class StrokeSpansToTriangles {

        @Test
        void spans_of_a_centreline_stroke_the_band_the_whole_of_it_strokes() {
            // The spans are stretches of one centreline, so their pieces put back together
            // are that centreline's own band - nothing is added at a boundary and nothing is
            // lost there. Which makes every case pinned above hold of a split band too.
            var spans = PolylineBands.strokeSpansToTriangles(
                List.of(
                    List.of(new double[] {0, 0}, new double[] {10, 0}),
                    List.of(new double[] {10, 0}, new double[] {10, 10})),
                WIDTH,
                MITER_SPIKE_LIMIT);

            assertThatPointsAre(
                concatenate(spans),
                PolylineBands.strokeToTriangles(RIGHT_ANGLE, WIDTH, MITER_SPIKE_LIMIT));
        }

        @Test
        void a_boundary_landing_on_a_corner_turns_with_the_corner() {
            // The boundary between the two spans sits exactly on the centreline's corner,
            // which is where stroking each span on its own would leave the band's worst
            // artefact: the first span ending square at (10,1) and (10,-1), the second
            // starting square at (9,0) and (11,0), and an open wedge between the two. Stroked
            // as one band the boundary takes the corner's own miter, so both spans end on the
            // miter points and the two meet along the line between them.
            var spans = PolylineBands.strokeSpansToTriangles(
                List.of(
                    List.of(new double[] {0, 0}, new double[] {10, 0}),
                    List.of(new double[] {10, 0}, new double[] {10, 10})),
                WIDTH,
                MITER_SPIKE_LIMIT);

            assertThat(hasCorner(spans.get(0), new double[] {9, 1}))
                .isTrue();
            assertThat(hasCorner(spans.get(0), new double[] {11, -1}))
                .isTrue();
            assertThat(hasCorner(spans.get(0), new double[] {10, 1}))
                .isFalse();
            assertThat(hasCorner(spans.get(0), new double[] {10, -1}))
                .isFalse();
            assertThat(hasCorner(spans.get(1), new double[] {9, 1}))
                .isTrue();
        }

        @Test
        void each_span_takes_the_triangles_of_its_own_stretch() {
            // A straight centreline cut in half: each span is the rectangle around its own
            // half and nothing of the other's, so a caller colouring the spans separately
            // colours exactly the stretch it asked about.
            var spans = PolylineBands.strokeSpansToTriangles(
                List.of(
                    List.of(new double[] {0, 0}, new double[] {5, 0}),
                    List.of(new double[] {5, 0}, new double[] {10, 0})),
                WIDTH,
                MITER_SPIKE_LIMIT);

            assertThatPointsAre(spans.get(0), List.of(
                new double[] {0, 1},
                new double[] {0, -1},
                new double[] {5, -1},
                new double[] {0, 1},
                new double[] {5, -1},
                new double[] {5, 1}));

            assertThatPointsAre(spans.get(1), List.of(
                new double[] {5, 1},
                new double[] {5, -1},
                new double[] {10, -1},
                new double[] {5, 1},
                new double[] {10, -1},
                new double[] {10, 1}));
        }

        @Test
        void a_boundary_landing_on_a_bevelled_corner_gives_the_wedge_to_the_span_arriving() {
            // The same corner at a limit tight enough to bevel it. The wedge the bevel holds
            // open belongs to the corner rather than to either span, so the two have to
            // differ over it: it goes to the span arriving, whose 19 units of quad it takes
            // to 20.5, leaving the span leaving the corner its 19 alone.
            var spans = PolylineBands.strokeSpansToTriangles(
                List.of(
                    List.of(new double[] {0, 0}, new double[] {10, 0}),
                    List.of(new double[] {10, 0}, new double[] {10, 10})),
                WIDTH,
                1.0);

            assertThat(computeTotalTriangleArea(spans.get(0)))
                .isCloseTo(20.5, buildAssertionSlack());
            assertThat(computeTotalTriangleArea(spans.get(1)))
                .isCloseTo(19.0, buildAssertionSlack());
            assertThat(hasCorner(spans.get(0), new double[] {10, -1}))
                .isTrue();
            assertThat(hasCorner(spans.get(1), new double[] {10, -1}))
                .isFalse();
        }

        @Test
        void a_span_may_open_on_the_point_after_the_one_its_predecessor_ended_on() {
            // The boundary point is where one span ended, so stating it again at the head of
            // the next is a courtesy rather than a requirement - a caller cutting a path into
            // stretches has it either way round, and the band is the same band.
            var boundaryGivenOnce = PolylineBands.strokeSpansToTriangles(
                List.of(
                    List.of(new double[] {0, 0}, new double[] {10, 0}),
                    List.of(new double[] {10, 10})),
                WIDTH,
                MITER_SPIKE_LIMIT);

            var boundaryGivenTwice = PolylineBands.strokeSpansToTriangles(
                List.of(
                    List.of(new double[] {0, 0}, new double[] {10, 0}),
                    List.of(new double[] {10, 0}, new double[] {10, 10})),
                WIDTH,
                MITER_SPIKE_LIMIT);

            assertThatPointsAre(
                boundaryGivenOnce.get(0),
                boundaryGivenTwice.get(0));

            assertThatPointsAre(
                boundaryGivenOnce.get(1),
                boundaryGivenTwice.get(1));
        }

        @Test
        void a_span_that_covers_no_distance_strokes_nothing_and_keeps_its_place() {
            // A span standing where its predecessor ended covers no stretch of the band, so
            // it has nothing to stroke. It comes back empty rather than being dropped - the
            // spans answer by position, and a caller reading a colour off each would
            // otherwise start colouring the wrong stretches from there on.
            var spans = PolylineBands.strokeSpansToTriangles(
                List.of(
                    List.of(new double[] {0, 0}, new double[] {5, 0}),
                    List.of(new double[] {5, 0}, new double[] {5, 0}),
                    List.of(new double[] {5, 0}, new double[] {10, 0})),
                WIDTH,
                MITER_SPIKE_LIMIT);

            assertThat(spans)
                .hasSize(3);
            assertThat(spans.get(1))
                .isEmpty();
            assertThat(spans.get(2))
                .isNotEmpty();
        }

        @Test
        void a_band_of_no_width_strokes_nothing_for_any_span() {
            // The degenerate band still answers per span, so a caller need not tell the two
            // reasons a span is empty apart.
            var spans = PolylineBands.strokeSpansToTriangles(
                List.of(
                    List.of(new double[] {0, 0}, new double[] {5, 0}),
                    List.of(new double[] {5, 0}, new double[] {10, 0})),
                0.0,
                MITER_SPIKE_LIMIT);

            assertThat(spans)
                .hasSize(2)
                .allSatisfy(span -> assertThat(span).isEmpty());
        }
    }

    // The spans' triangles back in one list, in span order - the band they were cut from.
    private static List<double[]> concatenate(List<List<double[]>> spans) {

        var triangles = new ArrayList<double[]>();

        for (var span : spans) {
            triangles.addAll(span);
        }
        return triangles;
    }

    // The area the band covers, as the sum of its triangles' own areas. The band is built
    // gap-free and, on these fixtures, without stacking its pieces, so the sum is the area
    // covered rather than an over-count of it.
    private static double computeTotalTriangleArea(List<double[]> triangles) {

        var total = 0.0;

        for (var corner = 0; corner + 2 < triangles.size(); corner += 3) {
            total += Math.abs(computeSignedArea(triangles.subList(corner, corner + 3)));
        }
        return total;
    }

    // Whether the band turns on the given point - whether any of its triangles has a
    // corner there. The band's shape is asserted through the corners it turns at, since
    // which triangle carries one is the stroker's own business.
    private static boolean hasCorner(List<double[]> triangles, double[] point) {

        return triangles.stream()
            .anyMatch(corner -> Points.computeDistance(corner, point) < CORNER_SLACK);
    }

}
