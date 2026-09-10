package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link Angles#normalise}: a direction already in the first turn is
 * unchanged, one a turn either side lands on the same place, the open end wraps to zero
 * rather than to a full turn, and a direction many turns out still lands in range.
 *
 * <p>And of {@link Angles#measureGap}: two directions a hair apart are a hair apart whichever
 * order they are given in, a pair either side of zero measure across it rather than the long
 * way round, and nothing exceeds a half turn.
 *
 * <p>And of {@link Angles#placeAfter}: a direction at the origin stays there, one just before
 * it moves a whole turn on rather than staying behind, and the result is always within one
 * turn of the origin - which is what makes it comparable to an interval that runs past a turn.
 *
 * <p>And of {@link Angles#measureSignedTurn}: a small turn keeps its sign either way, the
 * long way round becomes the short way with the sign flipped, and a half turn resolves
 * positive so that the range is closed at one end and open at the other.
 *
 * <p>And of {@link Angles#foldToHalfTurn}: a line already within a quarter turn of level is
 * untouched at both ends of that range, one past it folds by a half turn rather than a whole
 * one, and a direction and its opposite fold to the same line.
 *
 * <p>And of {@link Angles#measureUndirectedGap}: two lines a hair either side of vertical are
 * a hair apart rather than nearly a half turn, a line is no distance from itself or from its
 * own opposite, and nothing exceeds a quarter turn.
 *
 * <p>And of {@link Angles#intersectSpans}: two overlapping spans leave what they share, spans
 * built a turn apart still meet, one long span can meet another at both its ends, and spans
 * that only touch or that miss leave nothing.
 *
 * <p>And of {@link Angles#mergeSpans}: overlapping spans become one run, a span wholly inside
 * another leaves the outer one untouched, spans that only touch still join, disjoint ones
 * stay apart, and a run gathered around the far side of the window is not split across its
 * edge.
 */
final class AnglesTest {

    // Half a degree, comfortably wider than any rounding these do and far narrower than any
    // distinction they are asked to draw.
    private static final double SLACK = 0.0087;

    @Nested
    class Normalise {

        @Test
        void aDirectionInTheFirstTurnIsLeftWhereItIs() {
            assertThat(Angles.normalise(1.0))
                .isCloseTo(1.0, within());
        }

        @Test
        void aDirectionATurnPastTheFirstLandsBackInIt() {
            // 1 + 2pi is the same direction as 1.
            assertThat(Angles.normalise(1.0 + 2 * Math.PI))
                .isCloseTo(1.0, within());
        }

        @Test
        void aNegativeDirectionComesBackRoundTheTop() {
            // A quarter turn short of zero is three quarters of a turn past it.
            assertThat(Angles.normalise(-Math.PI / 2))
                .isCloseTo(4.712388, within());
        }

        @Test
        void aWholeTurnIsZeroRatherThanATurn() {
            // The range is closed at zero and open at a turn, so a caller comparing against
            // its own zero never sees two spellings of the same direction.
            assertThat(Angles.normalise(2 * Math.PI))
                .isCloseTo(0.0, within());
        }

        @Test
        void aDirectionManyTurnsOutStillLandsInRange() {
            assertThat(Angles.normalise(1.0 - 10 * Math.PI))
                .isCloseTo(1.0, within());
        }
    }

    @Nested
    class MeasureGap {

        @Test
        void twoDirectionsAHairApartAreAHairApart() {
            assertThat(Angles.measureGap(1.0, 1.25))
                .isCloseTo(0.25, within());
        }

        @Test
        void theGapReadsTheSameWhicheverOrderTheTwoAreGivenIn() {
            assertThat(Angles.measureGap(1.25, 1.0))
                .isCloseTo(0.25, within());
        }

        @Test
        void aPairEitherSideOfZeroMeasureAcrossItRatherThanRound() {
            // A tenth before a turn and a tenth after zero are a fifth apart, not a turn less.
            assertThat(Angles.measureGap(2 * Math.PI - 0.1, 0.1))
                .isCloseTo(0.2, within());
        }

        @Test
        void oppositeDirectionsAreAHalfTurnApartWhichIsTheMostThereIs() {
            assertThat(Angles.measureGap(0.0, Math.PI))
                .isCloseTo(3.141593, within());
        }

        @Test
        void moreThanAHalfTurnRoundIsReportedAsTheShorterWayBack() {
            // Three quarters of a turn forward is a quarter of a turn back.
            assertThat(Angles.measureGap(0.0, 3 * Math.PI / 2))
                .isCloseTo(1.570796, within());
        }
    }

    @Nested
    class PlaceAfter {

        @Test
        void aDirectionAtTheOriginStaysAtTheOrigin() {
            assertThat(Angles.placeAfter(1.0, 1.0))
                .isCloseTo(1.0, within());
        }

        @Test
        void aDirectionJustPastTheOriginStaysJustPastIt() {
            assertThat(Angles.placeAfter(1.5, 1.0))
                .isCloseTo(1.5, within());
        }

        @Test
        void aDirectionJustBeforeTheOriginMovesAWholeTurnOn() {
            // 0.5 sits before an origin of 1, so it is placed at 0.5 + 2pi - which is what
            // lets an interval running from 1 to 1.2 past a turn be tested against it.
            assertThat(Angles.placeAfter(0.5, 1.0))
                .isCloseTo(6.783185, within());
        }

        @Test
        void aDirectionATurnOutIsPlacedAsThoughItNeverWas() {
            assertThat(Angles.placeAfter(1.5 + 2 * Math.PI, 1.0))
                .isCloseTo(1.5, within());
        }

        @Test
        void anOriginPastATurnKeepsTheResultBesideItRatherThanInTheFirstTurn() {
            // The point of the operation: the answer is in the origin's turn, not in turn one.
            assertThat(Angles.placeAfter(0.5, 7.0))
                .isCloseTo(13.066371, within());
        }
    }

    @Nested
    class MeasureSignedTurn {

        @Test
        void aSmallTurnForwardKeepsItsSign() {
            assertThat(Angles.measureSignedTurn(0.5))
                .isCloseTo(0.5, within());
        }

        @Test
        void aSmallTurnBackKeepsItsSign() {
            assertThat(Angles.measureSignedTurn(-0.5))
                .isCloseTo(-0.5, within());
        }

        @Test
        void theLongWayForwardBecomesTheShortWayBack() {
            // Three quarters of a turn anticlockwise is a quarter turn clockwise.
            assertThat(Angles.measureSignedTurn(3 * Math.PI / 2))
                .isCloseTo(-1.570796, within());
        }

        @Test
        void aHalfTurnResolvesForwardSoTheRangeIsClosedAtOneEnd() {
            assertThat(Angles.measureSignedTurn(Math.PI))
                .isCloseTo(3.141593, within());
        }

        @Test
        void aHalfTurnBackResolvesForwardOntoTheSameAnswer() {
            assertThat(Angles.measureSignedTurn(-Math.PI))
                .isCloseTo(3.141593, within());
        }
    }

    @Nested
    class FoldToHalfTurn {

        @Test
        void aLineNearLevelIsLeftWhereItIs() {
            assertThat(Angles.foldToHalfTurn(0.3))
                .isCloseTo(0.3, within());
        }

        @Test
        void aLineAtAQuarterTurnIsLeftThereRatherThanFoldedToItsNegative() {
            // Both ends of the range are directions in their own right, so a caller that
            // asks with one does not get the other back.
            assertThat(Angles.foldToHalfTurn(Math.PI / 2))
                .isCloseTo(1.570796, within());
        }

        @Test
        void aLineAtMinusAQuarterTurnIsLeftThereToo() {
            assertThat(Angles.foldToHalfTurn(-Math.PI / 2))
                .isCloseTo(-1.570796, within());
        }

        @Test
        void aLinePastTheRangeFoldsByAHalfTurnRatherThanAWholeOne() {
            // Two radians is past a quarter turn, so it folds to 2 - pi.
            assertThat(Angles.foldToHalfTurn(2.0))
                .isCloseTo(-1.141593, within());
        }

        @Test
        void aDirectionAndItsOppositeFoldToTheSameLine() {
            assertThat(Angles.foldToHalfTurn(0.3 + Math.PI))
                .isCloseTo(0.3, within());
        }

        @Test
        void aLineSeveralHalfTurnsOutStillFoldsIntoRange() {
            assertThat(Angles.foldToHalfTurn(0.3 - 3 * Math.PI))
                .isCloseTo(0.3, within());
        }
    }

    @Nested
    class MeasureUndirectedGap {

        @Test
        void aLineIsNoDistanceFromItself() {
            assertThat(Angles.measureUndirectedGap(0.4, 0.4))
                .isCloseTo(0.0, within());
        }

        @Test
        void aLineIsNoDistanceFromItsOwnOpposite() {
            assertThat(Angles.measureUndirectedGap(0.4, 0.4 + Math.PI))
                .isCloseTo(0.0, within());
        }

        @Test
        void twoLinesAHairApartAreAHairApart() {
            assertThat(Angles.measureUndirectedGap(0.4, 0.6))
                .isCloseTo(0.2, within());
        }

        @Test
        void twoLinesEitherSideOfVerticalAreNearParallelRatherThanOpposite() {
            // A tenth short of a quarter turn and a tenth past it are a fifth apart as
            // lines, though as directions they are nearly a half turn apart.
            assertThat(Angles.measureUndirectedGap(Math.PI / 2 - 0.1, Math.PI / 2 + 0.1))
                .isCloseTo(0.2, within());
        }

        @Test
        void perpendicularLinesAreAQuarterTurnApartWhichIsTheMostThereIs() {
            assertThat(Angles.measureUndirectedGap(0.0, Math.PI / 2))
                .isCloseTo(1.570796, within());
        }
    }

    @Nested
    class IntersectSpans {

        @Test
        void twoOverlappingSpansLeaveTheStretchTheyShare() {
            // [1, 3] against [2, 5] shares [2, 3].
            var shared = Angles.intersectSpans(
                List.of(new double[] {1.0, 2.0}),
                List.of(new double[] {2.0, 3.0}));

            assertThat(shared)
                .hasSize(1);

            assertThat(shared.get(0)[0])
                .isCloseTo(2.0, within());
            assertThat(shared.get(0)[1])
                .isCloseTo(1.0, within());
        }

        @Test
        void spansBuiltATurnApartStillMeet() {
            // The same two spans as above, the second built a turn further round. Compared
            // as raw numbers they miss entirely.
            var shared = Angles.intersectSpans(
                List.of(new double[] {1.0, 2.0}),
                List.of(new double[] {2.0 + 2 * Math.PI, 3.0}));

            assertThat(shared)
                .hasSize(1);

            assertThat(shared.get(0)[0])
                .isCloseTo(2.0, within());
            assertThat(shared.get(0)[1])
                .isCloseTo(1.0, within());
        }

        @Test
        void theAnswerIsGivenInTheTurnTheFirstSetWasBuiltIn() {

            var shared = Angles.intersectSpans(
                List.of(new double[] {1.0 + 2 * Math.PI, 2.0}),
                List.of(new double[] {2.0, 3.0}));

            assertThat(shared)
                .hasSize(1);
            assertThat(shared.get(0)[0])
                .isCloseTo(8.283185, within());
        }

        @Test
        void aSpanMostOfATurnLongCanMeetAnotherAtBothOfItsEnds() {
            // A span of 6 radians starting at 0 wraps nearly all the way round, so a short
            // span at 5.8 meets it both before it wraps and after.
            var shared = Angles.intersectSpans(
                List.of(new double[] {0.0, 6.0}),
                List.of(new double[] {5.8, 0.6}));

            assertThat(shared)
                .hasSize(2);
        }

        @Test
        void spansThatOnlyTouchShareNothing() {

            assertThat(Angles.intersectSpans(
                    List.of(new double[] {1.0, 1.0}),
                    List.of(new double[] {2.0, 1.0})))
                .isEmpty();
        }

        @Test
        void spansThatMissShareNothing() {

            assertThat(Angles.intersectSpans(
                    List.of(new double[] {1.0, 0.5}),
                    List.of(new double[] {3.0, 0.5})))
                .isEmpty();
        }

        @Test
        void nothingSharesNothing() {
            assertThat(Angles.intersectSpans(List.of(), List.of(new double[] {1.0, 1.0})))
                .isEmpty();
        }
    }

    @Nested
    class MergeSpans {

        @Test
        void twoOverlappingSpansBecomeOneRun() {
            // [1, 3] and [2, 5] make [1, 5].
            var merged = Angles.mergeSpans(
                List.of(new double[] {1.0, 2.0}, new double[] {2.0, 3.0}),
                2.0);

            assertThat(merged)
                .hasSize(1);
            assertThat(merged.get(0)[1])
                .isCloseTo(4.0, within());
        }

        @Test
        void aSpanWhollyInsideAnotherLeavesTheOuterOneAsItWas() {

            var merged = Angles.mergeSpans(
                List.of(new double[] {1.0, 4.0}, new double[] {2.0, 1.0}),
                2.0);

            assertThat(merged)
                .hasSize(1);
            assertThat(merged.get(0)[1])
                .isCloseTo(4.0, within());
        }

        @Test
        void spansThatOnlyTouchStillJoin() {
            // Ends meeting exactly is one run, not two - a wall handing on to the next
            // leaves no circle between them.
            var merged = Angles.mergeSpans(
                List.of(new double[] {1.0, 1.0}, new double[] {2.0, 1.0}),
                2.0);

            assertThat(merged)
                .hasSize(1);
            assertThat(merged.get(0)[1])
                .isCloseTo(2.0, within());
        }

        @Test
        void disjointSpansStayApart() {

            var merged = Angles.mergeSpans(
                List.of(new double[] {1.0, 0.5}, new double[] {3.0, 0.5}),
                2.0);

            assertThat(merged)
                .hasSize(2);
        }

        @Test
        void theRunsComeBackInAscendingOrderWhateverOrderTheyWentIn() {

            var merged = Angles.mergeSpans(
                List.of(new double[] {3.0, 0.5}, new double[] {1.0, 0.5}),
                2.0);

            assertThat(merged)
                .hasSize(2);

            assertThat(merged.get(0)[0])
                .isCloseTo(1.0, within());
            assertThat(merged.get(1)[0])
                .isCloseTo(3.0, within());
        }

        @Test
        void spansBuiltATurnApartAreGatheredIntoOneRun() {
            // The second is the first's neighbour, written a turn further round. Sorted as
            // raw numbers they are a turn apart and would never be joined.
            var merged = Angles.mergeSpans(
                List.of(new double[] {1.0, 1.0}, new double[] {2.0 + 2 * Math.PI, 1.0}),
                2.0);

            assertThat(merged)
                .hasSize(1);
            assertThat(merged.get(0)[1])
                .isCloseTo(2.0, within());
        }

        @Test
        void aRunGatheredAroundTheWindowEdgeIsNotSplitAcrossIt() {
            // Two spans either side of zero, gathered about zero: read in the first turn
            // they sit at opposite ends and come back as two, but the window is centred on
            // what they gather around, so they are one.
            var merged = Angles.mergeSpans(
                List.of(new double[] {2 * Math.PI - 0.5, 0.5}, new double[] {0.0, 0.5}),
                0.0);

            assertThat(merged)
                .hasSize(1);
            assertThat(merged.get(0)[1])
                .isCloseTo(1.0, within());
        }

        @Test
        void nothingMergesToNothing() {
            assertThat(Angles.mergeSpans(List.of(), 0.0))
                .isEmpty();
        }
    }

    private static org.assertj.core.data.Offset<Double> within() {
        return org.assertj.core.data.Offset.offset(SLACK);
    }
}
