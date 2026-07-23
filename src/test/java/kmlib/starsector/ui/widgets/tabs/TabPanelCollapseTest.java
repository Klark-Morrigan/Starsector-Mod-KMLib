package kmlib.starsector.ui.widgets.tabs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link TabPanelCollapse}: a linear progress stepped by elapsed time, eased on read, that both starts
 * and reverses on the handle and settles cleanly at the docked and expanded ends. A fresh holder is fully
 * expanded, so every test drives it from a known start. Elapsed times are expressed as fractions of {@link
 * TabPanelCollapse#DURATION_SECONDS}, so the arithmetic stays independent of the concrete duration.
 */
final class TabPanelCollapseTest {
    private static final float TOLERANCE = 0.0001f;
    private static final float FULL_DURATION = TabPanelCollapse.DURATION_SECONDS;
    private static final float HALF_DURATION = TabPanelCollapse.DURATION_SECONDS / 2f;
    private static final float QUARTER_DURATION = TabPanelCollapse.DURATION_SECONDS / 4f;

    @Nested
    class GetCollapseFraction {

        @Test
        void getCollapseFractionStartsFullyExpandedAtZero() {
            assertThat(new TabPanelCollapse().getCollapseFraction()).isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void getCollapseFractionEasesTheHalfwayProgressToTheCurveMidpoint() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(HALF_DURATION);
            // Progress 0.5 eases to the smoothstep midpoint, which happens to sit back on the linear line.
            assertThat(collapse.getCollapseFraction()).isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void getCollapseFractionEasesTheQuarterProgressBelowItsLinearValue() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(QUARTER_DURATION);
            // A quarter through, the eased fraction trails the linear 0.25 - the slow, accelerating start.
            assertThat(collapse.getCollapseFraction()).isCloseTo(0.15625f, within(TOLERANCE));
        }
    }

    @Nested
    class AdvanceByElapsedTime {

        @Test
        void advanceByElapsedTimeReachesTheDockedEndAfterAFullDuration() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(FULL_DURATION);
            assertThat(collapse.getCollapseFraction()).isCloseTo(1f, within(TOLERANCE));
            assertThat(collapse.isDocked()).isTrue();
        }

        @Test
        void advanceByElapsedTimeClampsAtTheDockedEndRatherThanOvershooting() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            // Two full durations would step progress to 2; it settles at the docked end instead.
            collapse.advanceByElapsedTime(FULL_DURATION * 2f);
            assertThat(collapse.getCollapseFraction()).isCloseTo(1f, within(TOLERANCE));
            assertThat(collapse.isDocked()).isTrue();
        }

        @Test
        void advanceByElapsedTimeLeavesASettledExpandedPanelUnchanged() {
            var collapse = new TabPanelCollapse();
            // Fresh and expanded, with no toggle, a frame's advance cannot push it below zero.
            collapse.advanceByElapsedTime(FULL_DURATION);
            assertThat(collapse.getCollapseFraction()).isCloseTo(0f, within(TOLERANCE));
            assertThat(collapse.isAnimating()).isFalse();
        }

        @Test
        void advanceByElapsedTimeAccumulatesManySmallStepsLikeOneBigStep() {
            var manySteps = new TabPanelCollapse();
            manySteps.toggleCollapse();
            // Five per-frame slices of a tenth of the duration reach the same progress as one half-duration
            // step, so the collapse runs at the same pace whatever the frame rate splits the time into.
            for (var frame = 0; frame < 5; frame++) {
                manySteps.advanceByElapsedTime(TabPanelCollapse.DURATION_SECONDS / 10f);
            }
            var oneStep = new TabPanelCollapse();
            oneStep.toggleCollapse();
            oneStep.advanceByElapsedTime(HALF_DURATION);
            assertThat(manySteps.getCollapseFraction())
                    .isCloseTo(oneStep.getCollapseFraction(), within(TOLERANCE));
        }

        @Test
        void advanceByElapsedTimeExpandsBackToZeroAfterReversing() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(FULL_DURATION);
            // Reverse from docked and run a full duration back the other way to the expanded end.
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(FULL_DURATION);
            assertThat(collapse.getCollapseFraction()).isCloseTo(0f, within(TOLERANCE));
            assertThat(collapse.isDocked()).isFalse();
        }
    }

    @Nested
    class ToggleCollapse {

        @Test
        void toggleCollapseBeginsCollapsingFromTheExpandedStart() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(HALF_DURATION);
            // The handle sent an expanded panel toward docked, so the fraction has climbed off zero.
            assertThat(collapse.getCollapseFraction()).isGreaterThan(0f);
        }

        @Test
        void toggleCollapseBeginsExpandingFromTheDockedEnd() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(FULL_DURATION);
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(HALF_DURATION);
            // From docked, the handle sent it back toward expanded, so the fraction has fallen below one.
            assertThat(collapse.getCollapseFraction()).isLessThan(1f);
        }

        @Test
        void toggleCollapseReversesMidFlightFromTheCurrentFraction() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(HALF_DURATION);
            var midFraction = collapse.getCollapseFraction();
            // Reversing part-way keeps the fraction continuous - the next advance eases on from here, down.
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(QUARTER_DURATION);
            assertThat(collapse.getCollapseFraction()).isLessThan(midFraction);
            assertThat(collapse.getCollapseFraction()).isGreaterThan(0f);
        }
    }

    @Nested
    class IsDocked {

        @Test
        void isDockedIsFalseWhileTheBodyIsExpanded() {
            assertThat(new TabPanelCollapse().isDocked()).isFalse();
        }

        @Test
        void isDockedIsFalseMidCollapse() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(HALF_DURATION);
            assertThat(collapse.isDocked()).isFalse();
        }

        @Test
        void isDockedIsTrueOnceFullyCollapsed() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(FULL_DURATION);
            assertThat(collapse.isDocked()).isTrue();
        }
    }

    @Nested
    class IsAnimating {

        @Test
        void isAnimatingIsFalseWhenSettledExpanded() {
            assertThat(new TabPanelCollapse().isAnimating()).isFalse();
        }

        @Test
        void isAnimatingIsTrueImmediatelyAfterTheHandleStartsACollapse() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            // Heading for docked but not yet stepped, it already reads as animating so frames keep pumping.
            assertThat(collapse.isAnimating()).isTrue();
        }

        @Test
        void isAnimatingIsTrueImmediatelyAfterTheHandleStartsAnExpand() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(FULL_DURATION);
            collapse.toggleCollapse();
            // Docked and now heading back for expanded, it reads as animating before the first step so the
            // expand pumps frames the same way a collapse does.
            assertThat(collapse.isAnimating()).isTrue();
        }

        @Test
        void isAnimatingIsTrueMidFlight() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(HALF_DURATION);
            assertThat(collapse.isAnimating()).isTrue();
        }

        @Test
        void isAnimatingIsFalseOnceSettledAtTheDockedEnd() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(FULL_DURATION);
            assertThat(collapse.isAnimating()).isFalse();
        }
    }
}
