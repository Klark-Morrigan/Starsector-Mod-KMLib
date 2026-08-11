package kmlib.starsector.ui.widgets.tooltip;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins how a leader rule reads its own two ends: that whether there is anything to rule is answered from
 * the run's length rather than from a flag beside it, and that a run with no length measures nothing.
 *
 * <p>Worth fixing on the value itself rather than only through the layout that measures one, because the
 * reading is what lets an absence be spelled as a value at all. A stated {@link TooltipLeaderLine#NONE}
 * and a pair that closed up or crossed have to answer alike, or a row could hold a rule that says it
 * draws while having nothing to draw across - and a paint asking the wrong one of the two would put a
 * mark on a line the layout had already decided against.
 */
class TooltipLeaderLineTest {

    @Nested
    class IsRuled {

        @Test
        void isRuledIsTrueWhereTheRunHasLength() {

            var leaderLine = new TooltipLeaderLine(120f, 180f);

            assertThat(leaderLine.isRuled())
                .isTrue();
        }

        @Test
        void isRuledIsFalseForTheStatedAbsence() {

            assertThat(TooltipLeaderLine.NONE.isRuled())
                .isFalse();
        }

        @Test
        void isRuledIsFalseWhereTheEndsMeet() {
            // A row whose label stops exactly where its value starts has no run between them, and the
            // reading has to agree with the stated absence rather than treat a zero-length run as a mark.
            var closedUp = new TooltipLeaderLine(120f, 120f);

            assertThat(closedUp.isRuled())
                .isFalse();
        }

        @Test
        void isRuledIsFalseWhereTheEndsCross() {
            // The end sitting before the start is what a value wide enough to reach back past its label
            // produces. Read as a run it would be negative, and a paint taking the difference on trust
            // would draw a quad running backwards out of the box.
            var crossed = new TooltipLeaderLine(180f, 120f);

            assertThat(crossed.isRuled())
                .isFalse();
        }
    }

    @Nested
    class ComputeWidth {

        @Test
        void computeWidthIsTheDistanceBetweenTheEnds() {

            var leaderLine = new TooltipLeaderLine(120f, 180f);

            assertThat(leaderLine.computeWidth())
                .isEqualTo(60f);
        }

        @Test
        void computeWidthIsNothingForTheStatedAbsence() {

            assertThat(TooltipLeaderLine.NONE.computeWidth())
                .isEqualTo(0f);
        }

        @Test
        void computeWidthIsNothingWhereTheEndsCross() {
            // Floored rather than answered as the negative it works out to, so the one value a paint
            // fills across can never be a width that runs the wrong way.
            var crossed = new TooltipLeaderLine(180f, 120f);

            assertThat(crossed.computeWidth())
                .isEqualTo(0f);
        }
    }
}
