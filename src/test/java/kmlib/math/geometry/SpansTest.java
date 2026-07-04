package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link Spans#findLongestClearSubsegment}: an obstacle on
 * the line splits a span and the longer side wins, an obstacle off the line but
 * within clearance blocks the chord its keep-out circle cuts (half-width
 * {@code sqrt(clearance^2 - perp^2)}), an obstacle beyond clearance blocks nothing,
 * multiple obstacles each carve their own interval, the longest survivor is picked
 * across all input spans, and empty, fully blocked, or degenerate-direction input
 * yields null.
 *
 * <p>And of {@link Spans#intersectSpans}: overlapping spans yield their shared
 * interval, a span meeting several yields one overlap per meeting, spans that only
 * touch or miss yield nothing, and an empty list on either side yields nothing.
 */
final class SpansTest {

    private static org.assertj.core.data.Offset<Double> within() {
        return org.assertj.core.data.Offset.offset(1e-6);
    }

    @Nested
    class FindLongestClearSubsegment {
        @Test
        void clear_subsegment_takes_the_longer_side_when_an_obstacle_on_the_line_splits_the_span() {
            // The obstacle at t=2 with clearance 3 blocks [-1, 5]: the left remainder
            // [-10, -1] (length 9) beats the right [5, 10] (length 5).
            var clear = Spans.findLongestClearSubsegment(
                    List.of(new double[] {-10, 10}), 0, 0, 1, 0,
                    List.of(new double[] {2, 0}), 3);

            assertThat(clear[0]).isCloseTo(-10.0, within());
            assertThat(clear[1]).isCloseTo(-1.0, within());
        }

        @Test
        void clear_subsegment_ignores_an_obstacle_farther_than_the_clearance_from_the_line() {
            // Perpendicular distance 5 with clearance 3: the keep-out circle never
            // touches the line, so the whole span survives.
            var clear = Spans.findLongestClearSubsegment(
                    List.of(new double[] {-10, 10}), 0, 0, 1, 0,
                    List.of(new double[] {0, 5}), 3);

            assertThat(clear[0]).isCloseTo(-10.0, within());
            assertThat(clear[1]).isCloseTo(10.0, within());
        }

        @Test
        void clear_subsegment_blocks_the_chord_of_an_obstacle_off_the_line_but_within_clearance() {
            // Perpendicular distance 3 with clearance 5 cuts a chord of half-width
            // sqrt(25 - 9) = 4 about the projection at t=0, blocking [-4, 4]: the
            // left remainder [-10, -4] beats the right [4, 8].
            var clear = Spans.findLongestClearSubsegment(
                    List.of(new double[] {-10, 8}), 0, 0, 1, 0,
                    List.of(new double[] {0, 3}), 5);

            assertThat(clear[0]).isCloseTo(-10.0, within());
            assertThat(clear[1]).isCloseTo(-4.0, within());
        }

        @Test
        void clear_subsegment_survives_between_two_obstacles() {
            // Obstacles at t=-5 and t=5 with clearance 2 block [-7, -3] and [3, 7]:
            // the middle gap [-3, 3] (length 6) beats both end remainders (length 3).
            var clear = Spans.findLongestClearSubsegment(
                    List.of(new double[] {-10, 10}), 0, 0, 1, 0,
                    List.of(new double[] {-5, 0}, new double[] {5, 0}), 2);

            assertThat(clear[0]).isCloseTo(-3.0, within());
            assertThat(clear[1]).isCloseTo(3.0, within());
        }

        @Test
        void clear_subsegment_picks_the_longest_across_multiple_spans() {
            // With nothing blocked the contest is between the spans themselves: the
            // length-6 span beats the length-2 one.
            var clear = Spans.findLongestClearSubsegment(
                    List.of(new double[] {-10, -4}, new double[] {0, 2}), 0, 0, 1, 0,
                    List.of(), 3);

            assertThat(clear[0]).isCloseTo(-10.0, within());
            assertThat(clear[1]).isCloseTo(-4.0, within());
        }

        @Test
        void clear_subsegment_blocks_nothing_for_a_non_positive_clearance() {
            // Zero clearance means no keep-out at all, so even an obstacle dead on
            // the line leaves the span whole.
            var clear = Spans.findLongestClearSubsegment(
                    List.of(new double[] {-10, 10}), 0, 0, 1, 0,
                    List.of(new double[] {0, 0}), 0);

            assertThat(clear[0]).isCloseTo(-10.0, within());
            assertThat(clear[1]).isCloseTo(10.0, within());
        }

        @Test
        void clear_subsegment_is_null_for_empty_spans() {
            assertThat(Spans.findLongestClearSubsegment(
                    List.of(), 0, 0, 1, 0, List.of(), 3)).isNull();
        }

        @Test
        void clear_subsegment_is_null_when_an_obstacle_blocks_the_whole_span() {
            // Clearance 5 around an obstacle at the origin swallows the entire
            // [-1, 1] span; nothing clear remains.
            assertThat(Spans.findLongestClearSubsegment(
                    List.of(new double[] {-1, 1}), 0, 0, 1, 0,
                    List.of(new double[] {0, 0}), 5)).isNull();
        }

        @Test
        void clear_subsegment_is_null_for_a_degenerate_direction() {
            // A zero direction defines no line to project onto, so there is no
            // interval to pick.
            assertThat(Spans.findLongestClearSubsegment(
                    List.of(new double[] {-10, 10}), 0, 0, 0, 0, List.of(), 3)).isNull();
        }
    }

    @Nested
    class FindLongestSpan {
        @Test
        void longest_span_returns_the_widest_of_several() {
            // Lengths 6, 2, 4: the first span wins.
            var longest = Spans.findLongestSpan(List.of(
                    new double[] {-10, -4}, new double[] {0, 2}, new double[] {5, 9}));

            assertThat(longest[0]).isCloseTo(-10.0, within());
            assertThat(longest[1]).isCloseTo(-4.0, within());
        }

        @Test
        void longest_span_returns_the_only_span_when_the_list_is_a_singleton() {
            var longest = Spans.findLongestSpan(List.of(new double[] {3, 7}));

            assertThat(longest[0]).isCloseTo(3.0, within());
            assertThat(longest[1]).isCloseTo(7.0, within());
        }

        @Test
        void longest_span_is_null_for_an_empty_list() {
            assertThat(Spans.findLongestSpan(List.of())).isNull();
        }

        @Test
        void longest_span_is_null_when_every_span_is_degenerate() {
            // Zero-length spans have no room, so none wins.
            assertThat(Spans.findLongestSpan(List.of(
                    new double[] {2, 2}, new double[] {5, 5}))).isNull();
        }
    }

    @Nested
    class IntersectSpans {
        @Test
        void intersect_spans_yields_the_shared_interval_of_two_overlapping_spans() {
            // [0, 6] and [4, 10] overlap on [4, 6].
            var overlap = Spans.intersectSpans(
                    List.of(new double[] {0, 6}), List.of(new double[] {4, 10}));

            assertThat(overlap).hasSize(1);
            assertThat(overlap.get(0)[0]).isCloseTo(4.0, within());
            assertThat(overlap.get(0)[1]).isCloseTo(6.0, within());
        }

        @Test
        void intersect_spans_reports_one_overlap_per_span_a_wide_span_meets() {
            // A single [0, 20] span meets two on the other side, [2, 6] and [10, 14],
            // so two overlaps come back - the shape of a band rail split by a notch
            // meeting a whole centreline span.
            var overlaps = Spans.intersectSpans(
                    List.of(new double[] {0, 20}),
                    List.of(new double[] {2, 6}, new double[] {10, 14}));

            assertThat(overlaps).hasSize(2);
            assertThat(overlaps.get(0)[0]).isCloseTo(2.0, within());
            assertThat(overlaps.get(0)[1]).isCloseTo(6.0, within());
            assertThat(overlaps.get(1)[0]).isCloseTo(10.0, within());
            assertThat(overlaps.get(1)[1]).isCloseTo(14.0, within());
        }

        @Test
        void intersect_spans_yields_nothing_when_spans_only_touch() {
            // [0, 5] and [5, 10] share only the endpoint t=5: a zero-length touch is
            // no usable interval.
            assertThat(Spans.intersectSpans(
                    List.of(new double[] {0, 5}), List.of(new double[] {5, 10}))).isEmpty();
        }

        @Test
        void intersect_spans_yields_nothing_when_spans_miss() {
            assertThat(Spans.intersectSpans(
                    List.of(new double[] {0, 4}), List.of(new double[] {6, 10}))).isEmpty();
        }

        @Test
        void intersect_spans_yields_nothing_when_either_side_is_empty() {
            assertThat(Spans.intersectSpans(List.of(), List.of(new double[] {0, 5}))).isEmpty();
            assertThat(Spans.intersectSpans(List.of(new double[] {0, 5}), List.of())).isEmpty();
        }
    }
}
