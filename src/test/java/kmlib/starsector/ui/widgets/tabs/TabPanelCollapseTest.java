package kmlib.starsector.ui.widgets.tabs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link TabPanelCollapse}: a linear progress stepped by elapsed time, eased on read, that both starts
 * and reverses on the handle and settles cleanly at the docked and expanded ends. A fresh holder is fully
 * expanded, so every test drives it from a known start. Unless a case exercises the duration itself, the
 * default pace is supplied to {@link TabPanelCollapse#advanceByElapsedTime} and elapsed times are expressed
 * as fractions of it, so the arithmetic stays independent of the concrete duration.
 */
final class TabPanelCollapseTest {
    private static final float TOLERANCE = 0.0001f;
    private static final float DURATION = TabPanelCollapse.DEFAULT_DURATION_SECONDS;
    private static final float FULL_DURATION = DURATION;
    private static final float HALF_DURATION = DURATION / 2f;
    private static final float QUARTER_DURATION = DURATION / 4f;

    @Nested
    class CreateDocked {

        @Test
        void createDockedStartsFullyCollapsedAtTheDockedRail() {
            var collapse = TabPanelCollapse.createDocked();
            assertThat(collapse.getCollapseFraction()).isCloseTo(1f, within(TOLERANCE));
            assertThat(collapse.isDocked()).isTrue();
        }

        @Test
        void createDockedIsSettledSoAFrameDoesNotDriftItOffTheDockedEnd() {
            var collapse = TabPanelCollapse.createDocked();
            // Seeded at the docked end and aimed there, an unconditional per-frame advance leaves it put
            // rather than pushing progress past one.
            collapse.advanceByElapsedTime(FULL_DURATION, DURATION);
            assertThat(collapse.getCollapseFraction()).isCloseTo(1f, within(TOLERANCE));
            assertThat(collapse.isAnimating()).isFalse();
        }

        @Test
        void createDockedExpandsBackToZeroAfterTheHandleReverses() {
            var collapse = TabPanelCollapse.createDocked();
            // The next toggle sends a docked start toward expanded, so a full duration back reaches the
            // expanded end - a docked start animates open just as an expanded start animates shut.
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(FULL_DURATION, DURATION);
            assertThat(collapse.getCollapseFraction()).isCloseTo(0f, within(TOLERANCE));
            assertThat(collapse.isDocked()).isFalse();
        }
    }

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
            collapse.advanceByElapsedTime(HALF_DURATION, DURATION);
            // Progress 0.5 eases to the smoothstep midpoint, which happens to sit back on the linear line.
            assertThat(collapse.getCollapseFraction()).isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void getCollapseFractionEasesTheQuarterProgressBelowItsLinearValue() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(QUARTER_DURATION, DURATION);
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
            collapse.advanceByElapsedTime(FULL_DURATION, DURATION);
            assertThat(collapse.getCollapseFraction()).isCloseTo(1f, within(TOLERANCE));
            assertThat(collapse.isDocked()).isTrue();
        }

        @Test
        void advanceByElapsedTimeClampsAtTheDockedEndRatherThanOvershooting() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            // Two full durations would step progress to 2; it settles at the docked end instead.
            collapse.advanceByElapsedTime(FULL_DURATION * 2f, DURATION);
            assertThat(collapse.getCollapseFraction()).isCloseTo(1f, within(TOLERANCE));
            assertThat(collapse.isDocked()).isTrue();
        }

        @Test
        void advanceByElapsedTimeLeavesASettledExpandedPanelUnchanged() {
            var collapse = new TabPanelCollapse();
            // Fresh and expanded, with no toggle, a frame's advance cannot push it below zero.
            collapse.advanceByElapsedTime(FULL_DURATION, DURATION);
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
                manySteps.advanceByElapsedTime(DURATION / 10f, DURATION);
            }
            var oneStep = new TabPanelCollapse();
            oneStep.toggleCollapse();
            oneStep.advanceByElapsedTime(HALF_DURATION, DURATION);
            assertThat(manySteps.getCollapseFraction())
                .isCloseTo(oneStep.getCollapseFraction(), within(TOLERANCE));
        }

        @Test
        void advanceByElapsedTimeExpandsBackToZeroAfterReversing() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(FULL_DURATION, DURATION);
            // Reverse from docked and run a full duration back the other way to the expanded end.
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(FULL_DURATION, DURATION);
            assertThat(collapse.getCollapseFraction()).isCloseTo(0f, within(TOLERANCE));
            assertThat(collapse.isDocked()).isFalse();
        }

        @Test
        void advanceByElapsedTimeSnapsToDockedInOneStepWhenDurationIsZero() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            // A zero duration is the "no animation" setting: one advance jumps straight to the docked end
            // rather than dividing by zero. Even a tiny elapsed slice completes it.
            collapse.advanceByElapsedTime(HALF_DURATION, 0f);
            assertThat(collapse.getCollapseFraction()).isCloseTo(1f, within(TOLERANCE));
            assertThat(collapse.isDocked()).isTrue();
        }

        @Test
        void advanceByElapsedTimeSnapsBackToExpandedInOneStepWhenDurationIsZero() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(FULL_DURATION, DURATION);
            // From docked, the zero-duration setting snaps the reverse straight back to expanded too.
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(HALF_DURATION, 0f);
            assertThat(collapse.getCollapseFraction()).isCloseTo(0f, within(TOLERANCE));
            assertThat(collapse.isDocked()).isFalse();
        }

        @Test
        void advanceByElapsedTimeReachesTheDockedEndAfterAFullTwoSecondRamp() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            // The slider's slow end: a two-second duration reaches the docked end only after two seconds.
            collapse.advanceByElapsedTime(1f, 2f);
            assertThat(collapse.getCollapseFraction()).isGreaterThan(0f).isLessThan(1f);
            collapse.advanceByElapsedTime(1f, 2f);
            assertThat(collapse.getCollapseFraction()).isCloseTo(1f, within(TOLERANCE));
            assertThat(collapse.isDocked()).isTrue();
        }

        @Test
        void advanceByElapsedTimePacesAtTheDefaultWhenGivenTheDefaultDuration() {
            var stretched = new TabPanelCollapse();
            stretched.toggleCollapse();
            // The eased fraction depends only on the elapsed-to-duration ratio, so a mid-range duration
            // advanced a quarter of its length sits exactly where the old fixed-constant pace did.
            stretched.advanceByElapsedTime(2f / 4f, 2f);
            var atDefault = new TabPanelCollapse();
            atDefault.toggleCollapse();
            atDefault.advanceByElapsedTime(QUARTER_DURATION, DURATION);
            assertThat(stretched.getCollapseFraction())
                .isCloseTo(atDefault.getCollapseFraction(), within(TOLERANCE));
        }
    }

    @Nested
    class ToggleCollapse {

        @Test
        void toggleCollapseBeginsCollapsingFromTheExpandedStart() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(HALF_DURATION, DURATION);
            // The handle sent an expanded panel toward docked, so the fraction has climbed off zero.
            assertThat(collapse.getCollapseFraction()).isGreaterThan(0f);
        }

        @Test
        void toggleCollapseBeginsExpandingFromTheDockedEnd() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(FULL_DURATION, DURATION);
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(HALF_DURATION, DURATION);
            // From docked, the handle sent it back toward expanded, so the fraction has fallen below one.
            assertThat(collapse.getCollapseFraction()).isLessThan(1f);
        }

        @Test
        void toggleCollapseReversesMidFlightFromTheCurrentFraction() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(HALF_DURATION, DURATION);
            var midFraction = collapse.getCollapseFraction();
            // Reversing part-way keeps the fraction continuous - the next advance eases on from here, down.
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(QUARTER_DURATION, DURATION);
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
            collapse.advanceByElapsedTime(HALF_DURATION, DURATION);
            assertThat(collapse.isDocked()).isFalse();
        }

        @Test
        void isDockedIsTrueOnceFullyCollapsed() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(FULL_DURATION, DURATION);
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
            collapse.advanceByElapsedTime(FULL_DURATION, DURATION);
            collapse.toggleCollapse();
            // Docked and now heading back for expanded, it reads as animating before the first step so the
            // expand pumps frames the same way a collapse does.
            assertThat(collapse.isAnimating()).isTrue();
        }

        @Test
        void isAnimatingIsTrueMidFlight() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(HALF_DURATION, DURATION);
            assertThat(collapse.isAnimating()).isTrue();
        }

        @Test
        void isAnimatingIsFalseOnceSettledAtTheDockedEnd() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(FULL_DURATION, DURATION);
            assertThat(collapse.isAnimating()).isFalse();
        }
    }

    @Nested
    class IsFullyExpanded {

        @Test
        void isFullyExpandedIsTrueForAFreshExpandedHolder() {
            // The expanded default sits idle at zero, so a consumer's expanded-only input is live from open.
            assertThat(new TabPanelCollapse().isFullyExpanded()).isTrue();
        }

        @Test
        void isFullyExpandedIsFalseTheFrameACollapseBegins() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            // Aimed at the dock but not yet stepped, progress is still zero - the direction guard is what
            // reports this docking-from-the-start frame as not expanded.
            assertThat(collapse.isFullyExpanded()).isFalse();
        }

        @Test
        void isFullyExpandedIsFalseMidCollapse() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(HALF_DURATION, DURATION);
            assertThat(collapse.isFullyExpanded()).isFalse();
        }

        @Test
        void isFullyExpandedIsFalseWhenDocked() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(FULL_DURATION, DURATION);
            assertThat(collapse.isFullyExpanded()).isFalse();
        }

        @Test
        void isFullyExpandedIsFalseWhileUndocking() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(FULL_DURATION, DURATION);
            // Reversed off the dock and stepped part-way back, it is heading for expanded but not there yet.
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(HALF_DURATION, DURATION);
            assertThat(collapse.isFullyExpanded()).isFalse();
        }

        @Test
        void isFullyExpandedIsTrueOnceAnUndockCompletes() {
            var collapse = new TabPanelCollapse();
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(FULL_DURATION, DURATION);
            // A full duration back from the dock settles at the expanded end, so expanded-only input resumes.
            collapse.toggleCollapse();
            collapse.advanceByElapsedTime(FULL_DURATION, DURATION);
            assertThat(collapse.isFullyExpanded()).isTrue();
        }
    }
}
