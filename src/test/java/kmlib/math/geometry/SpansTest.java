package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static kmlib.math.geometry.GeometryTestSupport.buildAssertionSlack;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link Spans#computeLineBlockers}: an obstacle within clearance
 * carves the chord its keep-out circle cuts, one beyond it carves nothing, a
 * non-positive clearance blocks nothing at all, the intervals come back sorted whatever
 * order the obstacles arrived in, parameters read as world distances even for a
 * non-unit direction, and a degenerate direction yields null rather than an empty set.
 *
 * <p>And of {@link Spans#findLongestClearSubsegment}: an obstacle on
 * the line splits a span and the longer side wins, an obstacle off the line but
 * within clearance blocks the chord its keep-out circle cuts (half-width
 * {@code sqrt(clearance^2 - perp^2)}), an obstacle beyond clearance blocks nothing,
 * multiple obstacles each carve their own interval, the longest survivor is picked
 * across all input spans, and empty, fully blocked, or degenerate-direction input
 * yields null. Both the obstacle-taking form and the one taking blockers projected in
 * advance are pinned, since they must reach the same answer.
 *
 * <p>And of {@link Spans#intersectSpans}: overlapping spans yield their shared
 * interval, a span meeting several yields one overlap per meeting, spans that only
 * touch or miss yield nothing, and an empty list on either side yields nothing.
 */
final class SpansTest {

    @Nested
    class ComputeLineBlockers {

        @Test
        void lineBlockersCarveTheChordEachObstacleWithinClearanceCuts() {
            // Perpendicular distance 3 with clearance 5 cuts a chord of half-width
            // sqrt(25 - 9) = 4 about the projection at t=0.
            var blockers = Spans.computeLineBlockers(
                new DirectedLine(0, 0, 1, 0),
                List.of(new double[] {0, 3}),
                5);

            assertThat(blockers.intervals())
                .hasSize(1);

            assertThat(blockers.intervals().get(0)[0])
                .isCloseTo(-4.0, buildAssertionSlack());
            assertThat(blockers.intervals().get(0)[1])
                .isCloseTo(4.0, buildAssertionSlack());
        }

        @Test
        void lineBlockersArriveSortedByStartWhateverOrderTheObstaclesCameIn() {
            // Sorted intervals are what lets a span be walked with one advancing
            // cursor, so the order the obstacles were handed over must not survive:
            // the far obstacle listed first still yields the near interval first.
            var blockers = Spans.computeLineBlockers(
                new DirectedLine(0, 0, 1, 0),
                List.of(new double[] {5, 0}, new double[] {-5, 0}),
                2);

            assertThat(blockers.intervals())
                .hasSize(2);

            assertThat(blockers.intervals().get(0)[0])
                .isCloseTo(-7.0, buildAssertionSlack());
            assertThat(blockers.intervals().get(0)[1])
                .isCloseTo(-3.0, buildAssertionSlack());
            assertThat(blockers.intervals().get(1)[0])
                .isCloseTo(3.0, buildAssertionSlack());
            assertThat(blockers.intervals().get(1)[1])
                .isCloseTo(7.0, buildAssertionSlack());
        }

        @Test
        void lineBlockersMeasureParametersAsWorldDistancesForANonUnitDirection() {
            // The direction is normalised here, so the obstacle 4 units along a
            // doubled direction blocks about t=4, not about t=2.
            var blockers = Spans.computeLineBlockers(
                new DirectedLine(0, 0, 2, 0),
                List.of(new double[] {4, 0}),
                1);

            assertThat(blockers.intervals())
                .hasSize(1);

            assertThat(blockers.intervals().get(0)[0])
                .isCloseTo(3.0, buildAssertionSlack());
            assertThat(blockers.intervals().get(0)[1])
                .isCloseTo(5.0, buildAssertionSlack());
        }

        @Test
        void lineBlockersSkipAnObstacleFartherThanTheClearanceFromTheLine() {
            // Perpendicular distance 5 with clearance 3: the keep-out circle never
            // reaches the line, so it carves nothing from it.
            var blockers = Spans.computeLineBlockers(
                new DirectedLine(0, 0, 1, 0),
                List.of(new double[] {0, 5}),
                3);

            assertThat(blockers.intervals())
                .isEmpty();
        }

        @Test
        void lineBlockersAreEmptyForANonPositiveClearance() {
            // No clearance means no keep-out, so even an obstacle dead on the line
            // blocks nothing.
            var blockers = Spans.computeLineBlockers(
                new DirectedLine(0, 0, 1, 0),
                List.of(new double[] {0, 0}),
                0);

            assertThat(blockers.intervals())
                .isEmpty();
        }

        @Test
        void lineBlockersAreNullForADegenerateDirection() {
            // A zero direction defines no line, so there is no frame to measure
            // blocked parameters in - distinct from a line nothing blocks.
            assertThat(Spans.computeLineBlockers(
                    new DirectedLine(0, 0, 0, 0),
                    List.of(new double[] {0, 0}),
                    3))
                .isNull();
        }
    }

    @Nested
    class FindLongestClearSubsegment {

        @Test
        void clearSubsegmentTakesTheLongerSideWhenAnObstacleOnTheLineSplitsTheSpan() {
            // The obstacle at t=2 with clearance 3 blocks [-1, 5]: the left remainder
            // [-10, -1] (length 9) beats the right [5, 10] (length 5).
            var clear = Spans.findLongestClearSubsegment(
                List.of(new double[] {-10, 10}),
                new DirectedLine(0, 0, 1, 0),
                List.of(new double[] {2, 0}),
                3);

            assertThat(clear[0])
                .isCloseTo(-10.0, buildAssertionSlack());
            assertThat(clear[1])
                .isCloseTo(-1.0, buildAssertionSlack());
        }

        @Test
        void clearSubsegmentIgnoresAnObstacleFartherThanTheClearanceFromTheLine() {
            // Perpendicular distance 5 with clearance 3: the keep-out circle never
            // touches the line, so the whole span survives.
            var clear = Spans.findLongestClearSubsegment(
                List.of(new double[] {-10, 10}),
                new DirectedLine(0, 0, 1, 0),
                List.of(new double[] {0, 5}),
                3);

            assertThat(clear[0])
                .isCloseTo(-10.0, buildAssertionSlack());
            assertThat(clear[1])
                .isCloseTo(10.0, buildAssertionSlack());
        }

        @Test
        void clearSubsegmentBlocksTheChordOfAnObstacleOffTheLineButWithinClearance() {
            // Perpendicular distance 3 with clearance 5 cuts a chord of half-width
            // sqrt(25 - 9) = 4 about the projection at t=0, blocking [-4, 4]: the
            // left remainder [-10, -4] beats the right [4, 8].
            var clear = Spans.findLongestClearSubsegment(
                List.of(new double[] {-10, 8}),
                new DirectedLine(0, 0, 1, 0),
                List.of(new double[] {0, 3}),
                5);

            assertThat(clear[0])
                .isCloseTo(-10.0, buildAssertionSlack());
            assertThat(clear[1])
                .isCloseTo(-4.0, buildAssertionSlack());
        }

        @Test
        void clearSubsegmentSurvivesBetweenTwoObstacles() {
            // Obstacles at t=-5 and t=5 with clearance 2 block [-7, -3] and [3, 7]:
            // the middle gap [-3, 3] (length 6) beats both end remainders (length 3).
            var clear = Spans.findLongestClearSubsegment(
                List.of(new double[] {-10, 10}),
                new DirectedLine(0, 0, 1, 0),
                List.of(new double[] {-5, 0}, new double[] {5, 0}),
                2);

            assertThat(clear[0])
                .isCloseTo(-3.0, buildAssertionSlack());
            assertThat(clear[1])
                .isCloseTo(3.0, buildAssertionSlack());
        }

        @Test
        void clearSubsegmentPicksTheLongestAcrossMultipleSpans() {
            // With nothing blocked the contest is between the spans themselves: the
            // length-6 span beats the length-2 one.
            var clear = Spans.findLongestClearSubsegment(
                List.of(new double[] {-10, -4}, new double[] {0, 2}),
                new DirectedLine(0, 0, 1, 0),
                List.of(),
                3);

            assertThat(clear[0])
                .isCloseTo(-10.0, buildAssertionSlack());
            assertThat(clear[1])
                .isCloseTo(-4.0, buildAssertionSlack());
        }

        @Test
        void clearSubsegmentBlocksNothingForANonPositiveClearance() {
            // Zero clearance means no keep-out at all, so even an obstacle dead on
            // the line leaves the span whole.
            var clear = Spans.findLongestClearSubsegment(
                List.of(new double[] {-10, 10}),
                new DirectedLine(0, 0, 1, 0),
                List.of(new double[] {0, 0}),
                0);

            assertThat(clear[0])
                .isCloseTo(-10.0, buildAssertionSlack());
            assertThat(clear[1])
                .isCloseTo(10.0, buildAssertionSlack());
        }

        @Test
        void clearSubsegmentIsNullForEmptySpans() {
            assertThat(Spans.findLongestClearSubsegment(
                    List.of(),
                    new DirectedLine(0, 0, 1, 0),
                    List.of(),
                    3))
                .isNull();
        }

        @Test
        void clearSubsegmentIsNullWhenAnObstacleBlocksTheWholeSpan() {
            // Clearance 5 around an obstacle at the origin swallows the entire
            // [-1, 1] span; nothing clear remains.
            assertThat(Spans.findLongestClearSubsegment(
                    List.of(new double[] {-1, 1}),
                    new DirectedLine(0, 0, 1, 0),
                    List.of(new double[] {0, 0}),
                    5))
                .isNull();
        }

        @Test
        void clearSubsegmentIsNullForADegenerateDirection() {
            // A zero direction defines no line to project onto, so there is no
            // interval to pick.
            assertThat(Spans.findLongestClearSubsegment(
                    List.of(new double[] {-10, 10}),
                    new DirectedLine(0, 0, 0, 0),
                    List.of(),
                    3))
                .isNull();
        }

        @Test
        void clearSubsegmentTrimsTheSpanByAlreadyProjectedBlockers() {
            // The same obstacle-on-the-line case as above, reached through blockers
            // projected in advance: t=2 with clearance 3 blocks [-1, 5], so the left
            // remainder [-10, -1] wins - a precomputed subtraction is the same
            // subtraction.
            var blockers = Spans.computeLineBlockers(
                new DirectedLine(0, 0, 1, 0),
                List.of(new double[] {2, 0}),
                3);

            var clear = Spans.findLongestClearSubsegment(
                List.of(new double[] {-10, 10}),
                blockers);

            assertThat(clear[0])
                .isCloseTo(-10.0, buildAssertionSlack());
            assertThat(clear[1])
                .isCloseTo(-1.0, buildAssertionSlack());
        }

        @Test
        void clearSubsegmentAnswersSeveralSpanListsFromOneBlockerSet() {
            // Blockers outlive any one query: the obstacle at t=0 with clearance 2
            // blocks [-2, 2] for both spans asked about, trimming the first to its
            // left remainder [-10, -2] and leaving the second, which starts past the
            // blocker, whole.
            var blockers = Spans.computeLineBlockers(
                new DirectedLine(0, 0, 1, 0),
                List.of(new double[] {0, 0}),
                2);

            var first = Spans.findLongestClearSubsegment(
                List.of(new double[] {-10, 1}),
                blockers);

            var second = Spans.findLongestClearSubsegment(
                List.of(new double[] {4, 9}),
                blockers);

            assertThat(first[0])
                .isCloseTo(-10.0, buildAssertionSlack());
            assertThat(first[1])
                .isCloseTo(-2.0, buildAssertionSlack());

            assertThat(second[0])
                .isCloseTo(4.0, buildAssertionSlack());
            assertThat(second[1])
                .isCloseTo(9.0, buildAssertionSlack());
        }

        @Test
        void clearSubsegmentIsNullWhenPrecomputedBlockersSwallowTheSpan() {

            var blockers = Spans.computeLineBlockers(
                new DirectedLine(0, 0, 1, 0),
                List.of(new double[] {0, 0}),
                5);

            assertThat(Spans.findLongestClearSubsegment(
                    List.of(new double[] {-1, 1}),
                    blockers))
                .isNull();
        }
    }

    @Nested
    class FindLongestSpan {

        @Test
        void longestSpanReturnsTheWidestOfSeveral() {
            // Lengths 6, 2, 4: the first span wins.
            var longest = Spans.findLongestSpan(List.of(
                new double[] {-10, -4},
                new double[] {0, 2},
                new double[] {5, 9}));

            assertThat(longest[0])
                .isCloseTo(-10.0, buildAssertionSlack());
            assertThat(longest[1])
                .isCloseTo(-4.0, buildAssertionSlack());
        }

        @Test
        void longestSpanReturnsTheOnlySpanWhenTheListIsASingleton() {

            var longest = Spans.findLongestSpan(List.of(new double[] {3, 7}));

            assertThat(longest[0])
                .isCloseTo(3.0, buildAssertionSlack());
            assertThat(longest[1])
                .isCloseTo(7.0, buildAssertionSlack());
        }

        @Test
        void longestSpanIsNullForAnEmptyList() {

            assertThat(Spans.findLongestSpan(List.of()))
                .isNull();
        }

        @Test
        void longestSpanIsNullWhenEverySpanIsDegenerate() {
            // Zero-length spans have no room, so none wins.
            assertThat(Spans.findLongestSpan(List.of(
                    new double[] {2, 2},
                    new double[] {5, 5})))
                .isNull();
        }
    }

    @Nested
    class IntersectSpans {

        @Test
        void intersectSpansYieldsTheSharedIntervalOfTwoOverlappingSpans() {
            // [0, 6] and [4, 10] overlap on [4, 6].
            var overlap = Spans.intersectSpans(
                List.of(new double[] {0, 6}),
                List.of(new double[] {4, 10}));

            assertThat(overlap)
                .hasSize(1);

            assertThat(overlap.get(0)[0])
                .isCloseTo(4.0, buildAssertionSlack());
            assertThat(overlap.get(0)[1])
                .isCloseTo(6.0, buildAssertionSlack());
        }

        @Test
        void intersectSpansReportsOneOverlapPerSpanAWideSpanMeets() {
            // A single [0, 20] span meets two on the other side, [2, 6] and [10, 14],
            // so two overlaps come back - the shape of a band rail split by a notch
            // meeting a whole centreline span.
            var overlaps = Spans.intersectSpans(
                List.of(new double[] {0, 20}),
                List.of(new double[] {2, 6}, new double[] {10, 14}));

            assertThat(overlaps)
                .hasSize(2);

            assertThat(overlaps.get(0)[0])
                .isCloseTo(2.0, buildAssertionSlack());
            assertThat(overlaps.get(0)[1])
                .isCloseTo(6.0, buildAssertionSlack());
            assertThat(overlaps.get(1)[0])
                .isCloseTo(10.0, buildAssertionSlack());
            assertThat(overlaps.get(1)[1])
                .isCloseTo(14.0, buildAssertionSlack());
        }

        @Test
        void intersectSpansYieldsNothingWhenSpansOnlyTouch() {
            // [0, 5] and [5, 10] share only the endpoint t=5: a zero-length touch is
            // no usable interval.
            assertThat(Spans.intersectSpans(
                    List.of(new double[] {0, 5}),
                    List.of(new double[] {5, 10})))
                .isEmpty();
        }

        @Test
        void intersectSpansYieldsNothingWhenSpansMiss() {

            assertThat(Spans.intersectSpans(
                    List.of(new double[] {0, 4}),
                    List.of(new double[] {6, 10})))
                .isEmpty();
        }

        @Test
        void intersectSpansYieldsNothingWhenEitherSideIsEmpty() {

            assertThat(Spans.intersectSpans(List.of(), List.of(new double[] {0, 5})))
                .isEmpty();
            assertThat(Spans.intersectSpans(List.of(new double[] {0, 5}), List.of()))
                .isEmpty();
        }
    }
}
