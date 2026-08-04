package kmlib.starsector.ui.input;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins a keyed set of hover fades: one pass steps the hovered key up and every other one down, so a row
 * reads as one hover travelling across it rather than as several fades a caller has to wind back itself. The
 * pruning is checked through the fraction rather than through a size, since a dropped fade and a rested one
 * must be indistinguishable to a consumer - that indistinguishability is what licenses the pruning.
 */
final class HoverFadesTest {

    private static final float TOLERANCE = 0.0001f;

    // A whole duration in one step, so an end state is reached without walking frames.
    private static final float FULL_STEP_SECONDS = 1f;
    private static final float DURATION_SECONDS = 1f;

    private static final float HALF_STEP_SECONDS = 0.5f;

    private static final Integer FIRST_KEY = 0;
    private static final Integer SECOND_KEY = 1;
    private static final Integer UNTOUCHED_KEY = 2;
    private static final Integer NO_KEY = null;

    @Nested
    class ResolveHoverFractionAt {

        @Test
        void resolveHoverFractionAtAnswersFullyOffForAKeyNothingHasHovered() {

            var fades = new HoverFades<Integer>();
            fades.advanceTowardHoveredKey(FIRST_KEY, FULL_STEP_SECONDS, DURATION_SECONDS);

            assertThat(fades.resolveHoverFractionAt(UNTOUCHED_KEY))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class AdvanceTowardHoveredKey {

        @Test
        void advanceTowardHoveredKeyMovesTheHoveredKeyOntoItsHoveredLook() {

            var fades = new HoverFades<Integer>();
            fades.advanceTowardHoveredKey(FIRST_KEY, FULL_STEP_SECONDS, DURATION_SECONDS);

            assertThat(fades.resolveHoverFractionAt(FIRST_KEY))
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void advanceTowardHoveredKeyStartsTheNewlyHoveredKeyMovingInTheSamePass() {
            // A key minted this frame is stepped this frame; standing still for one frame at zero would be
            // a visible stutter at the moment the pointer lands.
            var fades = new HoverFades<Integer>();
            fades.advanceTowardHoveredKey(FIRST_KEY, HALF_STEP_SECONDS, DURATION_SECONDS);

            assertThat(fades.resolveHoverFractionAt(FIRST_KEY))
                .isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void advanceTowardHoveredKeyWindsEveryOtherKeyBackDown() {
            // The whole point of a keyed set: the pointer moving from one element to the next winds the
            // first one down without the caller naming it.
            var fades = new HoverFades<Integer>();
            fades.advanceTowardHoveredKey(FIRST_KEY, FULL_STEP_SECONDS, DURATION_SECONDS);
            fades.advanceTowardHoveredKey(SECOND_KEY, FULL_STEP_SECONDS, DURATION_SECONDS);

            assertThat(fades.resolveHoverFractionAt(FIRST_KEY))
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(fades.resolveHoverFractionAt(SECOND_KEY))
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void advanceTowardHoveredKeyWindsEveryKeyDownWhenThePointerIsOnNone() {

            var fades = new HoverFades<Integer>();
            fades.advanceTowardHoveredKey(FIRST_KEY, FULL_STEP_SECONDS, DURATION_SECONDS);
            fades.advanceTowardHoveredKey(NO_KEY, FULL_STEP_SECONDS, DURATION_SECONDS);

            assertThat(fades.resolveHoverFractionAt(FIRST_KEY))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceTowardHoveredKeyLeavesADepartedKeyPartWayDownRatherThanSnappingIt() {
            // A key the pointer has left is still on screen while it falls, so it must read as part-way
            // rather than as either end.
            var fades = new HoverFades<Integer>();
            fades.advanceTowardHoveredKey(FIRST_KEY, FULL_STEP_SECONDS, DURATION_SECONDS);
            fades.advanceTowardHoveredKey(SECOND_KEY, HALF_STEP_SECONDS, DURATION_SECONDS);

            assertThat(fades.resolveHoverFractionAt(FIRST_KEY))
                .isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void advanceTowardHoveredKeyKeepsARestedKeyReadingAsFullyOff() {
            // A settled fade is dropped from the set; asking for it must still answer, since a consumer
            // walking a row cannot know which of its keys are still held.
            var fades = new HoverFades<Integer>();
            fades.advanceTowardHoveredKey(FIRST_KEY, FULL_STEP_SECONDS, DURATION_SECONDS);
            fades.advanceTowardHoveredKey(NO_KEY, FULL_STEP_SECONDS, DURATION_SECONDS);
            fades.advanceTowardHoveredKey(NO_KEY, FULL_STEP_SECONDS, DURATION_SECONDS);

            assertThat(fades.resolveHoverFractionAt(FIRST_KEY))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceTowardHoveredKeyRaisesAKeyHoveredAgainAfterItSettled() {
            // The pruned key must be able to come back: a pointer returning to an element it left is the
            // ordinary case, not an edge one.
            var fades = new HoverFades<Integer>();
            fades.advanceTowardHoveredKey(FIRST_KEY, FULL_STEP_SECONDS, DURATION_SECONDS);
            fades.advanceTowardHoveredKey(NO_KEY, FULL_STEP_SECONDS, DURATION_SECONDS);
            fades.advanceTowardHoveredKey(FIRST_KEY, FULL_STEP_SECONDS, DURATION_SECONDS);

            assertThat(fades.resolveHoverFractionAt(FIRST_KEY))
                .isCloseTo(1f, within(TOLERANCE));
        }
    }

    @Nested
    class ResetFades {

        @Test
        void resetFadesDropsAFadeLeftPartWayUp() {

            var fades = new HoverFades<Integer>();
            fades.advanceTowardHoveredKey(FIRST_KEY, HALF_STEP_SECONDS, DURATION_SECONDS);
            fades.resetFades();

            assertThat(fades.resolveHoverFractionAt(FIRST_KEY))
                .isCloseTo(0f, within(TOLERANCE));
        }
    }
}
