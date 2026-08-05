package kmlib.starsector.ui.input;

import kmlib.animation.TraverseDurations;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins one element's hover fade: it starts off the hovered look, travels onto it while the pointer is there
 * and back off it when the pointer leaves, and - the whole reason it is a fade rather than a flag - carries
 * on from where it stands when the pointer turns around mid-travel.
 */
final class HoverFadeTest {

    private static final float TOLERANCE = 0.0001f;

    // A whole duration in one step, so a test that only cares about an end state reaches it without walking
    // frames.
    private static final float FULL_STEP_SECONDS = 1f;
    private static final float DURATION_SECONDS = 1f;

    // Half a duration, the step the eased curve's midpoint is pinned at.
    private static final float HALF_STEP_SECONDS = 0.5f;
    private static final float QUARTER_STEP_SECONDS = 0.25f;

    // The same pace each way, so every case below reads one duration off the step it charges rather than
    // having to say which direction it is travelling. The asymmetric case names its own pair.
    private static final TraverseDurations DURATIONS =
        TraverseDurations.createSymmetric(DURATION_SECONDS);

    private static final boolean HOVERED = true;
    private static final boolean UNHOVERED = false;

    @Nested
    class DefaultDurations {

        @Test
        void defaultDurationsTravelOntoALookTwiceAsQuicklyAsOffIt() {
            // The tuned pace every motion on a panel takes when its consumer offers no control. Pinned as
            // the pair rather than as a ratio, since it is the two values a player actually sees - and the
            // asymmetry is the point: equal halves make the whole motion read as the slower one.
            assertThat(HoverFade.DEFAULT_DURATIONS)
                .isEqualTo(new TraverseDurations(0.15f, 0.3f));
        }
    }

    @Nested
    class GetHoverFraction {

        @Test
        void getHoverFractionStartsFullyOffTheHoveredLook() {
            assertThat(new HoverFade().getHoverFraction())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void getHoverFractionEasesTheHalfwayProgressToTheCurveMidpoint() {
            // The stored progress is linear and the curve is applied on read, so half a duration reads as
            // the curve's own midpoint rather than as the raw half.
            var fade = new HoverFade();
            fade.advanceTowardHover(HOVERED, HALF_STEP_SECONDS, DURATIONS);

            assertThat(fade.getHoverFraction())
                .isCloseTo(0.5f, within(TOLERANCE));
        }
    }

    @Nested
    class AdvanceTowardHover {

        @Test
        void advanceTowardHoverReachesTheHoveredLookWhilePointedAt() {

            var fade = new HoverFade();
            fade.advanceTowardHover(HOVERED, FULL_STEP_SECONDS, DURATIONS);

            assertThat(fade.getHoverFraction())
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void advanceTowardHoverComesBackOffTheHoveredLookOncePointedAway() {

            var fade = new HoverFade();
            fade.advanceTowardHover(HOVERED, FULL_STEP_SECONDS, DURATIONS);
            fade.advanceTowardHover(UNHOVERED, FULL_STEP_SECONDS, DURATIONS);

            assertThat(fade.getHoverFraction())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void advanceTowardHoverFallsFromWhereItStandsWhenThePointerLeavesMidRise() {
            // The point of a retargeted fade: an element abandoned half-way up falls from half-way rather
            // than snapping to either end or replaying a curve from the top.
            var fade = new HoverFade();
            fade.advanceTowardHover(HOVERED, HALF_STEP_SECONDS, DURATIONS);
            fade.advanceTowardHover(UNHOVERED, QUARTER_STEP_SECONDS, DURATIONS);

            // Half a duration up then a quarter back down leaves the linear progress at a quarter, which
            // the curve reads out below its linear value.
            assertThat(fade.getHoverFraction())
                .isCloseTo(0.15625f, within(TOLERANCE));
        }

        @Test
        void advanceTowardHoverSnapsAllTheWayForANonPositiveDuration() {
            // A zero pace means "no animation", so the element lands on its hovered look in one step
            // rather than dividing by a zero duration.
            var fade = new HoverFade();
            fade.advanceTowardHover(HOVERED, FULL_STEP_SECONDS, TraverseDurations.SNAP);

            assertThat(fade.getHoverFraction())
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void advanceTowardHoverTravelsOnAtTheRisePaceAndOffAtTheFallPace() {
            // The reason the two are a pair rather than one value: a fade taking the same time either way
            // reads as the slower half. The rise here is charged its whole duration and arrives; the same
            // step then spends only half of the longer fall, so the element is still up at the midpoint
            // rather than back at rest - which is what a single duration would have given.
            var fade = new HoverFade();
            var durations = new TraverseDurations(HALF_STEP_SECONDS, DURATION_SECONDS);

            fade.advanceTowardHover(HOVERED, HALF_STEP_SECONDS, durations);

            assertThat(fade.getHoverFraction())
                .isCloseTo(1f, within(TOLERANCE));

            fade.advanceTowardHover(UNHOVERED, HALF_STEP_SECONDS, durations);

            assertThat(fade.getHoverFraction())
                .isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void advanceTowardHoverLeavesASettledFadeWhereItIs() {
            // A render loop calls this unconditionally every frame, so a fade already at its end must rest
            // there rather than creeping past it.
            var fade = new HoverFade();
            fade.advanceTowardHover(HOVERED, FULL_STEP_SECONDS, DURATIONS);
            fade.advanceTowardHover(HOVERED, FULL_STEP_SECONDS, DURATIONS);

            assertThat(fade.getHoverFraction())
                .isCloseTo(1f, within(TOLERANCE));
        }
    }

    @Nested
    class HasSettledOffHover {

        @Test
        void hasSettledOffHoverIsTrueForAFreshFade() {
            assertThat(new HoverFade().hasSettledOffHover())
                .isTrue();
        }

        @Test
        void hasSettledOffHoverIsFalseWhileStillPartWayOntoTheHoveredLook() {

            var fade = new HoverFade();
            fade.advanceTowardHover(HOVERED, HALF_STEP_SECONDS, DURATIONS);

            assertThat(fade.hasSettledOffHover())
                .isFalse();
        }

        @Test
        void hasSettledOffHoverIsTrueOnceTheFadeHasComeAllTheWayBack() {

            var fade = new HoverFade();
            fade.advanceTowardHover(HOVERED, FULL_STEP_SECONDS, DURATIONS);
            fade.advanceTowardHover(UNHOVERED, FULL_STEP_SECONDS, DURATIONS);

            assertThat(fade.hasSettledOffHover())
                .isTrue();
        }
    }

    @Nested
    class ResetFade {

        @Test
        void resetFadeDropsAFadeLeftPartWayUpWithoutWindingItDown() {
            // The element has stopped showing, so there is no travel left to see: the fade has to be off
            // the hovered look outright rather than falling from where it stood over the next frames.
            var fade = new HoverFade();
            fade.advanceTowardHover(HOVERED, HALF_STEP_SECONDS, DURATIONS);
            fade.resetFade();

            assertThat(fade.getHoverFraction())
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(fade.hasSettledOffHover())
                .isTrue();
        }

        @Test
        void resetFadeRisesAgainFromRestWhenTheElementComesBack() {
            // A reset must leave a reusable fade, not a spent one - the next session's hover has to travel
            // from the bottom exactly as a fresh element's does.
            var fade = new HoverFade();
            fade.advanceTowardHover(HOVERED, FULL_STEP_SECONDS, DURATIONS);
            fade.resetFade();
            fade.advanceTowardHover(HOVERED, HALF_STEP_SECONDS, DURATIONS);

            assertThat(fade.getHoverFraction())
                .isCloseTo(0.5f, within(TOLERANCE));
        }
    }
}
