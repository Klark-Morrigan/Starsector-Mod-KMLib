package kmlib.animation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link PhaseEnvelope}: a climb over the leading share of the turn, a fall over the rest of it, rest at
 * both ends so a wrapping phase meets no seam, and the two shares that leave one half nothing at all. The
 * expectations are the smoothstep values of the linear positions each phase lands on - the same curve every
 * other animation in the package settles along.
 */
final class PhaseEnvelopeTest {

    private static final float TOLERANCE = 0.0001f;

    // A rise over the first quarter of the turn, so the two halves are told apart by more than symmetry: a
    // phase that reads the same amplitude on the way up and on the way down does so at different phases.
    private static final float QUARTER_RISE = 0.25f;

    // Where that envelope stands half way up its climb and half way down its fall. Both linear positions are
    // 0.5, whose smoothstep value is 0.5 - the one point the curve leaves where it found it.
    private static final float PHASE_HALF_WAY_UP = 0.125f;
    private static final float PHASE_HALF_WAY_DOWN = 0.625f;
    private static final float HALF_AMPLITUDE = 0.5f;

    private static final float TURN_START = 0f;
    private static final float TURN_END = 1f;

    private static final float REST_AMPLITUDE = 0f;
    private static final float PEAK_AMPLITUDE = 1f;

    private final PhaseEnvelope envelope = new PhaseEnvelope(QUARTER_RISE);

    @Nested
    class ResolveAmplitude {

        @Test
        void resolveAmplitudeOpensTheTurnAtRest() {

            assertThat(envelope.resolveAmplitude(TURN_START))
                .isCloseTo(REST_AMPLITUDE, within(TOLERANCE));
        }

        @Test
        void resolveAmplitudeClimbsToThePeakOverTheLeadingShare() {

            assertThat(envelope.resolveAmplitude(PHASE_HALF_WAY_UP))
                .isCloseTo(HALF_AMPLITUDE, within(TOLERANCE));

            assertThat(envelope.resolveAmplitude(QUARTER_RISE))
                .isCloseTo(PEAK_AMPLITUDE, within(TOLERANCE));
        }

        @Test
        void resolveAmplitudeFallsBackOverTheRemainderOfTheTurn() {

            assertThat(envelope.resolveAmplitude(PHASE_HALF_WAY_DOWN))
                .isCloseTo(HALF_AMPLITUDE, within(TOLERANCE));
        }

        @Test
        void resolveAmplitudeClosesTheTurnAtRest() {
            // What lets a phase wrap without a jump: the amplitude a turn ends on is the one the next turn
            // opens on, so nothing about the seam is visible in what is drawn.
            assertThat(envelope.resolveAmplitude(TURN_END))
                .isCloseTo(REST_AMPLITUDE, within(TOLERANCE));
        }

        @Test
        void resolveAmplitudeStandsAtThePeakFromTheOutsetWhenNothingIsSpentClimbing() {
            // A share of nothing means a strike: the value is already at its peak on the turn's first instant
            // and spends the whole turn decaying, rather than dividing a climb by a span of nothing.
            var strikeEnvelope = new PhaseEnvelope(0f);

            assertThat(strikeEnvelope.resolveAmplitude(TURN_START))
                .isCloseTo(PEAK_AMPLITUDE, within(TOLERANCE));
            assertThat(strikeEnvelope.resolveAmplitude(TURN_END))
                .isCloseTo(REST_AMPLITUDE, within(TOLERANCE));
        }

        @Test
        void resolveAmplitudeClimbsForTheWholeTurnWhenNothingIsLeftToFallOver() {
            // The mirror case, and the one that would divide by nothing if the fall were taken unguarded: a
            // turn spent entirely climbing ends at the peak.
            var swellEnvelope = new PhaseEnvelope(1f);

            assertThat(swellEnvelope.resolveAmplitude(TURN_START))
                .isCloseTo(REST_AMPLITUDE, within(TOLERANCE));
            assertThat(swellEnvelope.resolveAmplitude(TURN_END))
                .isCloseTo(PEAK_AMPLITUDE, within(TOLERANCE));
        }

        @Test
        void resolveAmplitudeConfinesAPhaseFromOutsideTheTurn() {
            // A caller reading a phase of its own need not guard it, so a reading that has drifted past
            // either end is answered with the end rather than with a curve run past where it folds back.
            var strikeEnvelope = new PhaseEnvelope(0f);

            assertThat(strikeEnvelope.resolveAmplitude(-0.5f))
                .isCloseTo(PEAK_AMPLITUDE, within(TOLERANCE));
            assertThat(strikeEnvelope.resolveAmplitude(1.5f))
                .isCloseTo(REST_AMPLITUDE, within(TOLERANCE));
        }
    }

    @Nested
    class RiseFraction {

        @Test
        void riseFractionConfinesAShareFromOutsideTheTurn() {
            // Confined once, at the point the envelope is made, so no reading has to guard against a share
            // that would leave one of the two halves a negative span.
            assertThat(new PhaseEnvelope(-1f).riseFraction())
                .isEqualTo(0f);
            assertThat(new PhaseEnvelope(2f).riseFraction())
                .isEqualTo(1f);
        }
    }
}
