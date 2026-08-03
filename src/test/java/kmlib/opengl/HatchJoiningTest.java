package kmlib.opengl;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins which joinings merge. A caller branches on this to decide whether a join tolerance applies
 * and whether a {@link HatchJoinTally} holds measurements at all, so the flag is part of the
 * contract rather than an internal note - and a joining whose flag disagreed with its sink would
 * have its readings reported under the wrong reading of them.
 */
final class HatchJoiningTest {

    @Nested
    class IsMerging {

        @Test
        void is_merging_is_false_for_the_joining_that_emits_one_segment_per_triangle() {
            assertThat(HatchJoining.PER_TRIANGLE.isMerging()).isFalse();
        }

        @Test
        void is_merging_is_true_for_the_joining_that_coalesces_a_lines_crossings() {
            assertThat(HatchJoining.COALESCED.isMerging()).isTrue();
        }

        @Test
        void is_merging_agrees_with_whether_the_joining_ever_reports_a_join() {
            // The flag and the sink behind it are two statements of the same fact, so they are
            // held against each other on a soup that does give a merging joining something to
            // close: a flag flipped without its sink, or a sink swapped without its flag, shows
            // here rather than as a log line quietly reporting the wrong shape.
            var splitSquare = new float[] {
                0f, 0f, 4f, 0f, 4f, 4f,
                0f, 0f, 4f, 4f, 0f, 4f};

            for (var joining : HatchJoining.values()) {

                var joins = Hatching.computeHatchRun(splitSquare, 0, 1, joining, 0).joins();

                assertThat(joins.equals(HatchJoinTally.NO_JOINS))
                    .as("%s reports no join at all", joining)
                    .isNotEqualTo(joining.isMerging());
            }
        }
    }
}
