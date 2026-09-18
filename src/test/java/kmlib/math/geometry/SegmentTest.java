package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static kmlib.math.geometry.GeometryTestSupport.assertThatPointsAre;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the box a segment occupies at a given girth: the rectangle its whole length, half the
 * girth either side, wound as a ring - and the two degenerate readings that describe no
 * rectangle at all.
 *
 * <p>Worth pinning in coordinates because more than one thing reads this box off the same
 * segment - what is drawn from it, and what keeps clear of it - so which side of the centreline
 * each corner falls on is a fact both depend on rather than an internal detail.
 */
final class SegmentTest {

    // A segment running east, so the perpendicular the girth is spent along is the y axis and
    // every expected corner below is a literal.
    private static final Segment EASTWARD = new Segment(0, 0, 10, 0);

    private static final double GIRTH = 4;

    @Nested
    class ComputeBandCorners {

        @Test
        void bandCornersRunTheSegmentsLengthHalfTheGirthEitherSide() {
            // The ring starts on the left of the direction of travel, runs to the far end, and
            // comes back down the right - a closed ring with no repeated corner, which is what
            // the polygon operations here take.
            assertThatPointsAre(
                EASTWARD.computeBandCorners(GIRTH),
                List.of(
                    new double[] {0, 2},
                    new double[] {10, 2},
                    new double[] {10, -2},
                    new double[] {0, -2}));
        }

        @Test
        void bandCornersTurnWithTheSegment() {
            // The box is oriented, not axis-aligned: a name laid along a slanted line occupies
            // a slanted box, and a bounding box round it would claim room the name never takes.
            assertThatPointsAre(
                new Segment(0, 0, 0, 10).computeBandCorners(GIRTH),
                List.of(
                    new double[] {-2, 0},
                    new double[] {-2, 10},
                    new double[] {2, 10},
                    new double[] {2, 0}));
        }

        @Test
        void aSegmentWithNoDirectionHasNoBox() {
            // Both ends at one point: there is no direction to spend the girth across, so
            // there is no rectangle rather than a degenerate one for a caller to trip over.
            assertThat(new Segment(3, 3, 3, 3).computeBandCorners(GIRTH))
                .isEmpty();
        }

        @Test
        void aBandWithNoGirthHasNoBox() {
            // A collapsed fit reports zero girth, and a line encloses no area - so it takes up
            // no room and stands in nothing's way.
            assertThat(EASTWARD.computeBandCorners(0))
                .isEmpty();
        }
    }

    @Nested
    class ReadEnd {

        @Test
        void theEndIsTheEndAsAPoint() {

            assertThat(EASTWARD.readEnd())
                .containsExactly(10, 0);
        }
    }

    @Nested
    class ReadStart {

        @Test
        void theStartIsTheStartAsAPoint() {

            assertThat(EASTWARD.readStart())
                .containsExactly(0, 0);
        }

        @Test
        void eachReadIsItsOwnArray() {
            // A welder keeps the array it is handed as the corner itself, so two readers
            // sharing one array would have every corner move with the last edge welded.
            assertThat(EASTWARD.readStart())
                .isNotSameAs(EASTWARD.readStart());
        }
    }
}
