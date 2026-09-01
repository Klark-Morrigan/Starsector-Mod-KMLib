package kmlib.starsector.ui.input;

import kmlib.animation.TraverseDurations;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins what a tab row holds across frames: the two motions that compose into one shade, the held lift a press
 * leaves on a tab, the blink that answers to its own clock, and the arrival latch behind their sound. The
 * fades, pulses and latches themselves are pinned where they live; what is read here is the row's own rules
 * about them.
 */
final class TabHeaderMotionsTest {

    // A pace slow enough that nothing charged at it moves perceptibly in the slices below, so a motion found
    // at the top of its travel got there on a clock of its own.
    private static final TraverseDurations SLOW_DURATIONS = TraverseDurations.createSymmetric(100f);

    private static final int FIRST_TAB = 0;
    private static final int SECOND_TAB = 1;

    // A frame's worth of time for the snapping cases, where the pace decides the answer and not this.
    private static final float ONE_FRAME_SECONDS = 0.016f;

    private final TabHeaderMotions headerMotions = new TabHeaderMotions();

    @Nested
    class ResolveHoverFractionAt {

        @Test
        void resolveHoverFractionAtIsRestingBeforeAnythingReachesTheTab() {

            assertThat(headerMotions.resolveHoverFractionAt(FIRST_TAB))
                .isZero();
        }

        @Test
        void resolveHoverFractionAtFollowsThePointerHoldingTheTab() {
            headerMotions.advanceTabMotionsForFrame(
                FIRST_TAB,
                ONE_FRAME_SECONDS,
                TraverseDurations.SNAP);

            assertThat(headerMotions.resolveHoverFractionAt(FIRST_TAB))
                .isEqualTo(1f);

            // Only the tab the pointer is on: a row does not light along its length.
            assertThat(headerMotions.resolveHoverFractionAt(SECOND_TAB))
                .isZero();
        }

        @Test
        void resolveHoverFractionAtFollowsABlinkOnATabThePointerIsNotOn() {
            // The blink reaches the same shade the pointer would carry a tab onto, which is what lets a
            // keypress answer on a row nobody is pointing at.
            headerMotions.startHotkeyBlinkAt(FIRST_TAB);
            headerMotions.advanceTabMotionsForFrame(null, 0.05f, SLOW_DURATIONS);

            assertThat(headerMotions.resolveHoverFractionAt(FIRST_TAB))
                .isGreaterThan(0.5f);
        }

        @Test
        void resolveHoverFractionAtTakesTheGreaterOfTheTwoRatherThanTheirSum() {
            // Both motions aim at the one shade, so a blink struck on a tab the pointer already holds fully
            // carries it nowhere. Summed, this would read past a shade neither names.
            headerMotions.startHotkeyBlinkAt(FIRST_TAB);
            headerMotions.advanceTabMotionsForFrame(FIRST_TAB, 0.05f, TraverseDurations.SNAP);

            assertThat(headerMotions.resolveHoverFractionAt(FIRST_TAB))
                .isEqualTo(1f);
        }
    }

    @Nested
    class AdvanceTabMotionsForFrame {

        @Test
        void advanceTabMotionsForFramePacesTheBlinkByItsOwnClock() {
            // The one motion here that does not run at the pace the host sets: a strike has to be over about
            // as fast as the eye can catch it however leisurely the rest of the panel moves. Charged at the
            // host's pace instead, a blink given the full strike time would barely have left rest.
            headerMotions.startHotkeyBlinkAt(FIRST_TAB);
            headerMotions.advanceTabMotionsForFrame(null, 0.05f, SLOW_DURATIONS);

            assertThat(headerMotions.resolveHoverFractionAt(FIRST_TAB))
                .isGreaterThan(0.5f);
        }

        @Test
        void advanceTabMotionsForFramePacesTheClickLiftByTheHostsClock() {
            // The lift is a travel like the fades, so it moves at whatever pace the panel was given - here
            // one slow enough that a frame leaves it near rest.
            headerMotions.startHeldClickPulseAt(FIRST_TAB);
            headerMotions.advanceTabMotionsForFrame(null, ONE_FRAME_SECONDS, SLOW_DURATIONS);

            assertThat(headerMotions.resolveTabInteractionSources()
                    .pulseSource()
                    .resolvePulseFractionAt(FIRST_TAB))
                .isLessThan(0.5f);
        }
    }

    @Nested
    class ReleaseHeldClickPulses {

        @Test
        void releaseHeldClickPulsesReportsThatAHoldEnded() {
            headerMotions.startHeldClickPulseAt(FIRST_TAB);

            assertThat(headerMotions.releaseHeldClickPulses())
                .isTrue();
        }

        @Test
        void releaseHeldClickPulsesReportsNothingWhenNoTabIsHeld() {
            // Every release on the screen reaches the row, and only the ones that let go of a tab were owed
            // anything - which is what a caller sounds a press on.
            assertThat(headerMotions.releaseHeldClickPulses())
                .isFalse();
        }

        @Test
        void releaseHeldClickPulsesReportsNothingOnASecondRelease() {

            headerMotions.startHeldClickPulseAt(FIRST_TAB);
            headerMotions.releaseHeldClickPulses();

            assertThat(headerMotions.releaseHeldClickPulses())
                .isFalse();
        }
    }

    @Nested
    class DetectTabArrivalAt {

        @Test
        void detectTabArrivalAtFiresOnceForOneArrival() {

            assertThat(headerMotions.detectTabArrivalAt(FIRST_TAB))
                .isTrue();
            assertThat(headerMotions.detectTabArrivalAt(FIRST_TAB))
                .isFalse();
        }

        @Test
        void detectTabArrivalAtFiresAgainAfterThePointerLeaves() {
            headerMotions.detectTabArrivalAt(FIRST_TAB);
            headerMotions.detectTabArrivalAt(null);

            assertThat(headerMotions.detectTabArrivalAt(FIRST_TAB))
                .isTrue();
        }

        @Test
        void detectTabArrivalAtFiresOnCrossingToAnotherTab() {
            // One latch for the row rather than one per tab, so crossing from a tab to its neighbour is an
            // arrival on the neighbour and not a pointer that never left.
            headerMotions.detectTabArrivalAt(FIRST_TAB);

            assertThat(headerMotions.detectTabArrivalAt(SECOND_TAB))
                .isTrue();
        }
    }

    @Nested
    class ResetTabMotions {

        @Test
        void resetTabMotionsDropsEveryMotionTheRowHolds() {
            // A fade left part-way up, or a lift left part-way through its cycle, would otherwise be the
            // first thing the next session paints and then wind down.
            headerMotions.startHeldClickPulseAt(FIRST_TAB);
            headerMotions.startHotkeyBlinkAt(SECOND_TAB);
            headerMotions.advanceTabMotionsForFrame(
                FIRST_TAB,
                ONE_FRAME_SECONDS,
                TraverseDurations.SNAP);

            headerMotions.resetTabMotions();

            assertThat(headerMotions.resolveHoverFractionAt(FIRST_TAB))
                .isZero();
            assertThat(headerMotions.resolveHoverFractionAt(SECOND_TAB))
                .isZero();
            assertThat(headerMotions.resolveTabInteractionSources()
                    .pulseSource()
                    .resolvePulseFractionAt(FIRST_TAB))
                .isZero();
        }

        @Test
        void resetTabMotionsForgetsWhatWasAnnounced() {
            // So a panel re-opening under a still pointer sounds that tab's arrival afresh: the row was not
            // there a moment ago, even though the pointer never moved.
            headerMotions.detectTabArrivalAt(FIRST_TAB);

            headerMotions.resetTabMotions();

            assertThat(headerMotions.detectTabArrivalAt(FIRST_TAB))
                .isTrue();
        }
    }
}
