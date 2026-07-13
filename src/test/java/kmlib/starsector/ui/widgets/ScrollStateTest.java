package kmlib.starsector.ui.widgets;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link ScrollState}: {@link ScrollState#scrollBy} accumulates a raw request and {@link
 * ScrollState#clampTo} settles it into the region's real range each frame, so a wheel past the bottom or a
 * region that shrank cannot leave the stored offset drifting outside what can be scrolled. Each test uses
 * a fresh instance, so the cases do not leak into one another.
 */
final class ScrollStateTest {
    private static final float TOLERANCE = 0.01f;

    @Nested
    class ScrollBy {

        @Test
        void scrollByAddsToTheOffset() {
            var state = new ScrollState();
            state.scrollBy(30f);
            assertThat(state.getOffset()).isCloseTo(30f, within(TOLERANCE));
        }

        @Test
        void scrollByAccumulatesAcrossCalls() {
            // A run of notches sums, so repeated scrolling walks the content rather than jumping to a
            // single position.
            var state = new ScrollState();
            state.scrollBy(30f);
            state.scrollBy(-10f);
            assertThat(state.getOffset()).isCloseTo(20f, within(TOLERANCE));
        }
    }

    @Nested
    class SetOffset {

        @Test
        void setOffsetJumpsStraightToTheGivenPosition() {
            // A scrollbar drag maps the pointer to an absolute position, so setOffset replaces the offset
            // rather than accumulating like scrollBy does.
            var state = new ScrollState();
            state.scrollBy(30f);
            state.setOffset(80f);
            assertThat(state.getOffset()).isCloseTo(80f, within(TOLERANCE));
        }
    }

    @Nested
    class ClampTo {

        @Test
        void clampToHoldsTheOffsetWithinTheOverflow() {
            // A request past the content's bottom settles at the overflow, so the stored value tracks what
            // can be scrolled rather than drifting far below the last row.
            var state = new ScrollState();
            state.scrollBy(500f);
            state.clampTo(100f);
            assertThat(state.getOffset()).isCloseTo(100f, within(TOLERANCE));
        }

        @Test
        void clampToFloorsANegativeOffsetAtTheTop() {
            var state = new ScrollState();
            state.scrollBy(-50f);
            state.clampTo(100f);
            assertThat(state.getOffset()).isZero();
        }

        @Test
        void clampToCollapsesToTheTopWhenNothingOverflows() {
            // A region that now fits (overflow 0) pulls the stored offset back to the top, so a shrunk
            // region does not stay scrolled into blank space.
            var state = new ScrollState();
            state.scrollBy(40f);
            state.clampTo(0f);
            assertThat(state.getOffset()).isZero();
        }
    }
}
