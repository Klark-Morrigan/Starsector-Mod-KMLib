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
 *
 * <p>Also the reads beside it: the normal every offset across the segment steps along, the lane
 * dividers within the same band, and the segment hung centred on a point.
 */
final class SegmentTests {

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
    class ComputeLaneDividers {

        // Three lanes across a girth of 6 are two apart: the dividers stand 1 either side of the
        // centreline, the right-hand one first, each running the whole length.
        @Test
        void dividersSplitTheGirthIntoEqualLanes() {

            // ARRANGE
            var expectedDividers = List.of(
                new Segment(0, -1, 10, -1),
                new Segment(0, 1, 10, 1));

            // ACT
            var actualDividers = EASTWARD.computeLaneDividers(6, 3);

            // ASSERT
            assertThat(actualDividers)
                .isEqualTo(expectedDividers);
        }

        // One lane has no boundary inside it.
        @Test
        void aSingleLaneHasNoDivider() {

            // ACT
            var actualDividers = EASTWARD.computeLaneDividers(GIRTH, 1);

            // ASSERT
            assertThat(actualDividers)
                .isEmpty();
        }

        // Both ends at one point: there is no direction to step across.
        @Test
        void aSegmentWithNoDirectionHasNoDivider() {

            // ARRANGE
            var pointSegment = new Segment(3, 3, 3, 3);

            // ACT
            var actualDividers = pointSegment.computeLaneDividers(GIRTH, 2);

            // ASSERT
            assertThat(actualDividers)
                .isEmpty();
        }
    }

    @Nested
    class ComputeLeftUnitNormal {

        // The left of travel: east turns to north, north turns to west.
        @Test
        void theNormalIsTheDirectionTurnedAQuarterTurnAnticlockwise() {

            // ARRANGE
            var northward = new Segment(0, 0, 0, 5);

            // ACT
            var actualEastwardNormal = EASTWARD.computeLeftUnitNormal();
            var actualNorthwardNormal = northward.computeLeftUnitNormal();

            // ASSERT
            assertThatPointsAre(
                List.of(actualEastwardNormal, actualNorthwardNormal),
                List.of(new double[] {0, 1}, new double[] {-1, 0}));
        }

        @Test
        void aSegmentWithNoDirectionHasNoNormal() {

            // ARRANGE
            var pointSegment = new Segment(3, 3, 3, 3);

            // ACT
            var actualNormal = pointSegment.computeLeftUnitNormal();

            // ASSERT
            assertThat(actualNormal)
                .isNull();
        }
    }

    @Nested
    class ComputeOffsetSegment {

        // A positive distance steps along the left normal, which for a segment running east is north.
        @Test
        void aPositiveDistanceMovesTheSegmentToItsLeft() {

            // ARRANGE
            var expectedSegment = new Segment(0, 3, 10, 3);

            // ACT
            var actualSegment = EASTWARD.computeOffsetSegment(3);

            // ASSERT
            assertThat(actualSegment)
                .isEqualTo(expectedSegment);
        }

        @Test
        void aNegativeDistanceMovesTheSegmentToItsRight() {

            // ARRANGE
            var expectedSegment = new Segment(0, -3, 10, -3);

            // ACT
            var actualSegment = EASTWARD.computeOffsetSegment(-3);

            // ASSERT
            assertThat(actualSegment)
                .isEqualTo(expectedSegment);
        }

        // Both ends at one point: there is no side to move it to.
        @Test
        void aSegmentWithNoDirectionHasNoOffset() {

            // ARRANGE
            var pointSegment = new Segment(3, 3, 3, 3);

            // ACT
            var actualSegment = pointSegment.computeOffsetSegment(3);

            // ASSERT
            assertThat(actualSegment)
                .isNull();
        }
    }

    @Nested
    class CreateCentredOn {

        @Test
        void theSegmentRunsHalfItsLengthEitherSideOfTheCentre() {

            // ARRANGE
            var expectedSegment = new Segment(0, 0, 10, 0);

            // ACT
            var actualSegment = Segment.createCentredOn(5, 0, 0, 10);

            // ASSERT
            assertThat(actualSegment)
                .isEqualTo(expectedSegment);
        }

        // A quarter turn runs the segment from below the centre to above it.
        @Test
        void theSegmentRunsAlongTheDirectionGiven() {

            // ACT
            var actualSegment = Segment.createCentredOn(5, 5, Angles.QUARTER_TURN, 4);

            // ASSERT
            assertThatPointsAre(
                List.of(actualSegment.readStart(), actualSegment.readEnd()),
                List.of(new double[] {5, 3}, new double[] {5, 7}));
        }
    }

    @Nested
    class JoinPoints {

        @Test
        void theSegmentRunsFromTheFirstPointToTheSecond() {

            assertThat(Segment.joinPoints(new double[] {0, 0}, new double[] {10, 0}))
                .isEqualTo(EASTWARD);
        }

        @Test
        void laterChangesToThePointsDoNotMoveTheSegment() {
            // A record of four doubles holds values, so a caller reusing its arrays as scratch
            // space after the call leaves the segment where it was laid.
            var start = new double[] {0, 0};
            var segment = Segment.joinPoints(start, new double[] {10, 0});

            start[0] = 5;

            assertThat(segment)
                .isEqualTo(EASTWARD);
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
