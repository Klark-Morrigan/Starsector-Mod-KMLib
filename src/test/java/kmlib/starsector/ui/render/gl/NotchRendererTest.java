package kmlib.starsector.ui.render.gl;

import kmlib.math.geometry.Rectangle;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link NotchRenderer}'s pure computations - the ones that carry the handle's semantics rather than
 * just emit GL. {@link NotchRenderer#computeChevronArms} owns the rotating-glyph contract: the chevron
 * points the way that cues the action at each end of the collapse and straightens to a vertical line
 * between them. {@link NotchRenderer#computeNotchBorder} owns the one-pixel-thinner floor, and {@link
 * NotchRenderer#computeHoverWashAlpha} how far the hover has lit the handle's face.
 */
final class NotchRendererTest {

    private static final float TOLERANCE = 0.001f;

    // A notch off the origin so a centred apex is a non-trivial coordinate, not zero by construction.
    private static final Rectangle NOTCH = new Rectangle(100f, 200f, 16f, 44f);

    // A fully opaque panel, so a wash alpha reads as the hover's own contribution alone.
    private static final float FULL_OPACITY = 1f;

    @Nested
    class ComputeChevronArms {

        @Test
        void computeChevronArmsPointsTheApexLeftOfTheEndsWhenFullyExpanded() {

            var arms = NotchRenderer.computeChevronArms(NOTCH, 0f);
            // Expanded, the glyph is the collapse cue "<": apex to the left, the arms opening to the right.
            assertThat(arms.apexX())
                .isLessThan(arms.endsX());
        }

        @Test
        void computeChevronArmsStraightensToAVerticalLineAtTheMidpoint() {

            var arms = NotchRenderer.computeChevronArms(NOTCH, 0.5f);

            // Halfway, the apex sits on the ends' own x, so the two arms fall on one vertical line.
            assertThat(arms.apexX())
                .isCloseTo(arms.endsX(), within(TOLERANCE));
            assertThat(arms.apexX())
                .isCloseTo(NOTCH.computeCenterX(), within(TOLERANCE));
        }

        @Test
        void computeChevronArmsPointsTheApexRightOfTheEndsWhenFullyDocked() {

            var arms = NotchRenderer.computeChevronArms(NOTCH, 1f);

            // Docked, the glyph is the expand cue ">": apex to the right, the arms opening to the left.
            assertThat(arms.apexX())
                .isGreaterThan(arms.endsX());
        }

        @Test
        void computeChevronArmsSwingsSymmetricallyAboutTheCentreAcrossTheCollapse() {

            var expanded = NotchRenderer.computeChevronArms(NOTCH, 0f);
            var docked = NotchRenderer.computeChevronArms(NOTCH, 1f);

            // The end and apex simply swap sides between the ends, so the docked glyph mirrors the expanded
            // one about the centre - one chevron rotating, not two differently sized shapes.
            assertThat(docked.apexX())
                .isCloseTo(expanded.endsX(), within(TOLERANCE));
            assertThat(docked.endsX())
                .isCloseTo(expanded.apexX(), within(TOLERANCE));
        }

        @Test
        void computeChevronArmsClampsAnOvershootingFractionToTheDockedGlyph() {

            var docked = NotchRenderer.computeChevronArms(NOTCH, 1f);
            var overshoot = NotchRenderer.computeChevronArms(NOTCH, 2f);

            // A fraction past the end holds the docked glyph rather than swinging the apex further out, so
            // an overshooting animation value never inverts or distorts the chevron.
            assertThat(overshoot.apexX())
                .isCloseTo(docked.apexX(), within(TOLERANCE));
            assertThat(overshoot.endsX())
                .isCloseTo(docked.endsX(), within(TOLERANCE));
        }

        @Test
        void computeChevronArmsRunsTheApexMidwayBetweenTheArmEnds() {

            var arms = NotchRenderer.computeChevronArms(NOTCH, 0f);

            // The apex's y sits between the two arm ends' ys, so the glyph is a chevron, not a skewed line.
            assertThat(arms.midY())
                .isBetween(arms.bottomY(), arms.topY());
            assertThat(arms.midY())
                .isCloseTo(NOTCH.computeCenterY(), within(TOLERANCE));
        }
    }

    @Nested
    class ComputeNotchBorder {

        @Test
        void computeNotchBorderStrokesOnePixelThinnerThanTheFrame() {
            assertThat(NotchRenderer.computeNotchBorder(3f))
                .isCloseTo(2f, within(TOLERANCE));
        }

        @Test
        void computeNotchBorderFloorsAtOnePixelForAHairlineFrame() {
            // A one-pixel frame would leave a zero-width notch edge; the floor keeps an edge to trace.
            assertThat(NotchRenderer.computeNotchBorder(1f))
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void computeNotchBorderFloorsAtOnePixelForABorderlessFrame() {
            assertThat(NotchRenderer.computeNotchBorder(0f))
                .isCloseTo(1f, within(TOLERANCE));
        }
    }

    @Nested
    class ComputeHoverWashAlpha {

        @Test
        void computeHoverWashAlphaWashesNothingWhileTheHandleIsUnlit() {
            // A resting handle must contribute no wash at all, so the draw can be skipped rather than
            // blending a fully transparent quad over the notch's own face every frame.
            assertThat(NotchRenderer.computeHoverWashAlpha(FULL_OPACITY, 0f))
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void computeHoverWashAlphaReachesTheWashPeakOnceTheHandleIsFullyLit() {
            assertThat(NotchRenderer.computeHoverWashAlpha(FULL_OPACITY, 1f))
                .isCloseTo(0.35f, within(TOLERANCE));
        }

        @Test
        void computeHoverWashAlphaScalesWithTheHoverPartWayThroughTheTravel() {
            // The whole point of taking a fraction: the face lights gradually rather than switching on.
            assertThat(NotchRenderer.computeHoverWashAlpha(FULL_OPACITY, 0.5f))
                .isCloseTo(0.175f, within(TOLERANCE));
        }

        @Test
        void computeHoverWashAlphaFadesWithThePanelsOwnOpacity() {
            // A lit handle on a half-faded panel washes at half its peak, so the handle keeps fading with
            // the panel rather than lighting to full strength over a translucent frame.
            assertThat(NotchRenderer.computeHoverWashAlpha(0.5f, 1f))
                .isCloseTo(0.175f, within(TOLERANCE));
        }

        @Test
        void computeHoverWashAlphaClampsAnOvershootingFractionToTheWashPeak() {
            assertThat(NotchRenderer.computeHoverWashAlpha(FULL_OPACITY, 2f))
                .isCloseTo(0.35f, within(TOLERANCE));
        }
    }
}
