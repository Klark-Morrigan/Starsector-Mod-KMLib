package kmlib.starsector.ui.coreui;

import kmlib.animation.TraverseDurations;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins that raised and painted part company, which is the whole of what this clock is for.
 *
 * <p>Input reads the first and must move on the press: a panel that stayed raised for its fall would eat
 * the click that follows the press, and one that came up only once its rise had run would let a click
 * through to the screen on the frame it opened. The paint reads the second and must not move on the
 * press: a box that vanished on the frame it was dismissed is the cut the fade exists to remove.
 *
 * <p>And that the pace is the one it was built with, each direction at its own duration.
 */
final class ModalPanelFadeTests {

    private static final float TOLERANCE = 0.0001f;

    // The way in and the shorter way out.
    private static final float RISE_SECONDS = 0.3f;
    private static final float FALL_SECONDS = 0.2f;

    private static final TraverseDurations DURATIONS = new TraverseDurations(RISE_SECONDS, FALL_SECONDS);

    // Part of a fall, so a reopen mid-way has somewhere to come up from.
    private static final float HALF_A_FALL_SECONDS = 0.1f;

    // One frame's worth of time, short enough that a rise begun on it has not finished.
    private static final float A_FRAME_SECONDS = 0.02f;

    @Nested
    class IsPanelRaised {

        @Test
        void isFalseAtRest() {

            assertThat(new ModalPanelFade(DURATIONS).isPanelRaised())
                .isFalse();
        }

        @Test
        void isTrueOnTheFrameThePanelIsRaisedBeforeItsRiseHasBegun() {
            // Input goes the instant the panel appears, while the paint is still at nothing.
            var fade = new ModalPanelFade(DURATIONS);

            fade.raisePanel();

            assertThat(fade.isPanelRaised())
                .isTrue();
        }

        @Test
        void isFalseOnThePressWhileThePaintStillStands() {
            // The split itself: dismissed for input at once, with the box still fully painted.
            var fade = createFullyRaisedFade();

            fade.dismissPanel();

            assertThat(fade.isPanelRaised())
                .isFalse();
            assertThat(fade.resolveFadeFraction())
                .isCloseTo(1f, within(TOLERANCE));
        }
    }

    @Nested
    class AdvanceFade {

        @Test
        void reachesFullyUpAfterTheRise() {

            var fade = new ModalPanelFade(DURATIONS);

            fade.raisePanel();
            fade.advanceFade(RISE_SECONDS);

            assertThat(fade.resolveFadeFraction())
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void isStillRisingAFrameIn() {
            // The rise is a travel rather than a switch: one frame in, the box is part way and not whole.
            var fade = new ModalPanelFade(DURATIONS);

            fade.raisePanel();
            fade.advanceFade(A_FRAME_SECONDS);

            assertThat(fade.resolveFadeFraction())
                .isGreaterThan(0f)
                .isLessThan(1f);
        }

        @Test
        void reachesDownAfterTheFall() {

            var fade = createFullyRaisedFade();

            fade.dismissPanel();
            fade.advanceFade(FALL_SECONDS);

            assertThat(fade.resolveFadeFraction())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void comesUpFromWhereTheFallStoodOnAReopen() {
            // Retargeted rather than replayed: a panel reopened mid-fall neither drops to nothing and
            // starts over nor jumps to whole, it turns round where it stands.
            var fade = createFullyRaisedFade();

            fade.dismissPanel();
            fade.advanceFade(HALF_A_FALL_SECONDS);

            var whereTheFallStood = fade.resolveFadeFraction();

            fade.raisePanel();
            fade.advanceFade(A_FRAME_SECONDS);

            assertThat(fade.resolveFadeFraction())
                .isGreaterThan(whereTheFallStood)
                .isLessThan(1f);
        }
    }

    @Nested
    class ResolveFadeFraction {

        @Test
        void isNothingAtRest() {

            assertThat(new ModalPanelFade(DURATIONS).resolveFadeFraction())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void isNothingOnTheFrameThePanelIsRaised() {
            // A first open comes up from nothing: raising moves input, not the paint.
            var fade = new ModalPanelFade(DURATIONS);

            fade.raisePanel();

            assertThat(fade.resolveFadeFraction())
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class IsSettledDown {

        @Test
        void isTrueAtRest() {

            assertThat(new ModalPanelFade(DURATIONS).isSettledDown())
                .isTrue();
        }

        @Test
        void isFalseForARaisedPanelWhosePaintHasNotBegun() {
            // Down and at nothing is not settled while the panel is up: the panel a caller would take
            // off is the one about to rise.
            var fade = new ModalPanelFade(DURATIONS);

            fade.raisePanel();

            assertThat(fade.isSettledDown())
                .isFalse();
        }

        @Test
        void isFalseWhileTheFallIsStillRunning() {

            var fade = createFullyRaisedFade();

            fade.dismissPanel();
            fade.advanceFade(HALF_A_FALL_SECONDS);

            assertThat(fade.isSettledDown())
                .isFalse();
        }

        @Test
        void isTrueOnceTheFallHasRunOut() {
            // Which is when the panel comes off the screen: at the end of the fall, not at the press.
            var fade = createFullyRaisedFade();

            fade.dismissPanel();
            fade.advanceFade(FALL_SECONDS);

            assertThat(fade.isSettledDown())
                .isTrue();
        }
    }

    @Nested
    class DropFade {

        @Test
        void takesARaisedPanelDownAndItsPaintWithItInOneStep() {

            var fade = createFullyRaisedFade();

            fade.dropFade();

            assertThat(fade.isPanelRaised())
                .isFalse();
            assertThat(fade.resolveFadeFraction())
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(fade.isSettledDown())
                .isTrue();
        }
    }

    // A panel up and fully painted, which is where every dismissal starts from.
    private static ModalPanelFade createFullyRaisedFade() {

        var fade = new ModalPanelFade(DURATIONS);

        fade.raisePanel();
        fade.advanceFade(RISE_SECONDS);

        return fade;
    }
}
