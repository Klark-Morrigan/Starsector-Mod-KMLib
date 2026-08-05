package kmlib.starsector.ui.widgets.tabs;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link TabWash}: a lift is clamped into the unit range whatever composes it, scales to a fraction of
 * its own depth without changing the colour it lifts toward, and moves a colour toward its target by exactly
 * that fraction while leaving the colour's own transparency alone - the rule a tab's fill and its label are
 * both lifted by, so a drift here would part a washed fill from the text standing on it.
 */
final class TabWashTest {
    
    private static final float TOLERANCE = 0.001f;

    // A base with a distinct value in every channel and a part-transparent alpha, so a blend that mixed
    // two channels up, or that recomputed the alpha, shows as a wrong number rather than as a coincidence.
    private static final Color BASE_COLOUR = new Color(0, 100, 200, 128);
    private static final Color TARGET_COLOUR = new Color(100, 200, 0);

    @Nested
    class Strength {

        @Test
        void strengthCarriesAnInRangeValueThrough() {
            assertThat(new TabWash(TARGET_COLOUR, 0.35f).strength())
                .isCloseTo(0.35f, within(TOLERANCE));
        }

        @Test
        void strengthFloorsAtZeroWhenNegative() {
            // A composed lift can subtract its way below nothing; the floor makes that a tab at rest
            // rather than one washing away from its target into a colour neither side named.
            assertThat(new TabWash(TARGET_COLOUR, -0.4f).strength())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void strengthCapsAtOneWhenAboveTheRange() {
            // Overlapping lifts can sum past full; the cap settles the tab at the target instead of
            // blending past it and back out the far side.
            assertThat(new TabWash(TARGET_COLOUR, 1.6f).strength())
                .isCloseTo(1f, within(TOLERANCE));
        }
    }

    @Nested
    class ComputeScaledWash {

        @Test
        void computeScaledWashKeepsTheTargetAndScalesTheDepth() {
            // A decaying lift fades back along the one colour it lifted toward, so only the depth moves.
            var scaled = new TabWash(TARGET_COLOUR, 0.4f).computeScaledWash(0.5f);

            assertThat(scaled.target())
                .isEqualTo(TARGET_COLOUR);
            assertThat(scaled.strength())
                .isCloseTo(0.2f, within(TOLERANCE));
        }

        @Test
        void computeScaledWashReachesTheFullDepthAtTheTopOfTheRange() {
            assertThat(new TabWash(TARGET_COLOUR, 0.4f).computeScaledWash(1f).strength())
                .isCloseTo(0.4f, within(TOLERANCE));
        }

        @Test
        void computeScaledWashLiftsNothingAtTheBottomOfTheRange() {
            // What a tab with no pulse running on it carries, so nothing at rest needs a path of its own.
            assertThat(new TabWash(TARGET_COLOUR, 0.4f).computeScaledWash(0f).strength())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void computeScaledWashCannotBeDrivenPastTheFullDepth() {
            // The strength clamp catches an out-of-range fraction, so an overshooting animator settles the
            // tab at its peak rather than blending past the target.
            assertThat(new TabWash(TARGET_COLOUR, 0.4f).computeScaledWash(4f).strength())
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void computeScaledWashFloorsANegativeFractionAtNoLift() {
            assertThat(new TabWash(TARGET_COLOUR, 0.4f).computeScaledWash(-1f).strength())
                .isCloseTo(0f, within(TOLERANCE));
        }
    }

    @Nested
    class ComputeWashedColour {

        @Test
        void computeWashedColourReturnsTheBaseColourAtZeroStrength() {
            assertThat(new TabWash(TARGET_COLOUR, 0f).computeWashedColour(BASE_COLOUR))
                .isEqualTo(new Color(0, 100, 200, 128));
        }

        @Test
        void computeWashedColourReachesTheTargetAtFullStrength() {
            assertThat(new TabWash(TARGET_COLOUR, 1f).computeWashedColour(BASE_COLOUR))
                .isEqualTo(new Color(100, 200, 0, 128));
        }

        @Test
        void computeWashedColourMovesEachChannelByTheStrength() {
            assertThat(new TabWash(TARGET_COLOUR, 0.5f).computeWashedColour(BASE_COLOUR))
                .isEqualTo(new Color(50, 150, 100, 128));
        }

        @Test
        void computeWashedColourKeepsTheBaseAlphaWhateverTheTargetCarries() {
            // The target's own alpha is not part of the lift: a wash is a brightness shift, so a strip
            // fading as a unit keeps deciding transparency for itself.
            var opaqueTarget = new Color(100, 200, 0, 255);

            assertThat(new TabWash(opaqueTarget, 1f).computeWashedColour(BASE_COLOUR).getAlpha())
                .isEqualTo(128);
        }

        @Test
        void computeWashedColourLeavesTheColourUntouchedForARestingTab() {
            assertThat(TabWash.NONE.computeWashedColour(BASE_COLOUR))
                .isEqualTo(new Color(0, 100, 200, 128));
        }
    }
}
