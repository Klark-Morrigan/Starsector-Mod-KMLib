package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static kmlib.math.geometry.GeometryTestSupport.buildAssertionSlack;
import static kmlib.math.geometry.GeometryTestSupport.computeSignedArea;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link PolylineBands#strokeToTriangles}: a straight centreline
 * strokes the rectangle around it, a corner mitres so the band covers both its arms
 * once, a corner past the spike limit bevels the outside of the turn away, a corner
 * whose miter would outrun the segments it joins pinches to the centreline instead of
 * folding the band into a bowtie, a point the centreline runs straight through leaves
 * the band unbroken, repeated points are one point, and a centreline with nothing to
 * stroke - one distinct point, or no width - strokes nothing.
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
        void inner_rail_pinches_to_the_centreline_where_the_miter_outruns_its_segments() {
            // A one-long segment between two right angles: each miter reaches root-two
            // back along it, and the two of them together reach farther than the segment
            // is long, so their rails would cross and the band would fold into a bowtie.
            // Both corners give the miter up and take the centreline point itself, which
            // no rail can cross past.
            var hairpin = List.of(
                new double[] {0, 0},
                new double[] {10, 0},
                new double[] {10, 1},
                new double[] {0, 1});

            var band = PolylineBands.strokeToTriangles(hairpin, WIDTH, MITER_SPIKE_LIMIT);

            assertThat(hasCorner(band, new double[] {10, 0}))
                .isTrue();
            assertThat(hasCorner(band, new double[] {10, 1}))
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

    private static void assertThatPointsAre(List<double[]> points, List<double[]> expected) {

        assertThat(points)
            .hasSameSizeAs(expected);

        for (var i = 0; i < expected.size(); i++) {
            assertThat(points.get(i))
                .containsExactly(expected.get(i), buildAssertionSlack());
        }
    }
}
