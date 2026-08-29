package kmlib.animation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link PhasePattern}: which elements of a turn sound and which stay silent, how far through the
 * current element a phase stands, the seam at the turn's end reading as the next turn's opening, and the pace
 * a whole turn takes at a given beat length. Phases are named as positions in beat units and converted at the
 * point of asking, so each case reads as the moment of the rhythm it is about rather than as a fraction.
 */
final class PhasePatternTest {

    private static final float TOLERANCE = 0.0001f;

    // How long each rhythm's turn is in beat units, which is what turns a position in the rhythm into the
    // phase a consumer would be holding at that moment.
    private static final float STEADY_BEAT_CYCLE_UNITS = 2f;
    private static final float SOS_CYCLE_UNITS = 30f;

    // Half way into the element being asked about, so a case names one moment and pins both what the pattern
    // is doing there and how far through it is.
    private static final float HALF_WAY_THROUGH = 0.5f;

    private static final float TURN_END = 1f;
    private static final float ELEMENT_START = 0f;

    // A pace slow enough that a period reads as a different number from the unit count it scales.
    private static final float UNIT_SECONDS = 0.5f;

    @Nested
    class ResolveBeatAt {

        @Test
        void resolveBeatAtSoundsThroughTheFirstHalfOfASteadyBeat() {

            var beat = PhasePattern.STEADY_BEAT.resolveBeatAt(resolveSteadyBeatPhaseAtUnit(0.5f));

            assertThat(beat.isSounding())
                .isTrue();
            assertThat(beat.beatPhase())
                .isCloseTo(HALF_WAY_THROUGH, within(TOLERANCE));
        }

        @Test
        void resolveBeatAtRestsThroughTheSecondHalfOfASteadyBeat() {

            var beat = PhasePattern.STEADY_BEAT.resolveBeatAt(resolveSteadyBeatPhaseAtUnit(1.5f));

            assertThat(beat.isSounding())
                .isFalse();
            assertThat(beat.beatPhase())
                .isCloseTo(HALF_WAY_THROUGH, within(TOLERANCE));
        }

        @Test
        void resolveBeatAtOpensTheDistressSilhouetteWithThreeShortSounds() {
            // Each short sound is parted from the next by a silence as long as itself, which is what makes
            // three of them read as three rather than as one long one.
            assertThat(PhasePattern.SOS.resolveBeatAt(resolveSosPhaseAtUnit(0.5f)).isSounding())
                .isTrue();
            assertThat(PhasePattern.SOS.resolveBeatAt(resolveSosPhaseAtUnit(1.5f)).isSounding())
                .isFalse();
            assertThat(PhasePattern.SOS.resolveBeatAt(resolveSosPhaseAtUnit(2.5f)).isSounding())
                .isTrue();
            assertThat(PhasePattern.SOS.resolveBeatAt(resolveSosPhaseAtUnit(3.5f)).isSounding())
                .isFalse();
            assertThat(PhasePattern.SOS.resolveBeatAt(resolveSosPhaseAtUnit(4.5f)).isSounding())
                .isTrue();
            assertThat(PhasePattern.SOS.resolveBeatAt(resolveSosPhaseAtUnit(5.5f)).isSounding())
                .isFalse();
        }

        @Test
        void resolveBeatAtHoldsTheMiddleSoundsOfTheDistressSilhouetteThreeTimesAsLong() {
            // The middle group is what tells the silhouette from a plain string of flashes, so its length is
            // pinned by where the halfway point of a single sound falls rather than by its state alone.
            var beat = PhasePattern.SOS.resolveBeatAt(resolveSosPhaseAtUnit(7.5f));

            assertThat(beat.isSounding())
                .isTrue();
            assertThat(beat.beatPhase())
                .isCloseTo(HALF_WAY_THROUGH, within(TOLERANCE));

            assertThat(PhasePattern.SOS.resolveBeatAt(resolveSosPhaseAtUnit(9.5f)).isSounding())
                .isFalse();
        }

        @Test
        void resolveBeatAtClosesTheDistressSilhouetteWithThreeShortSounds() {

            assertThat(PhasePattern.SOS.resolveBeatAt(resolveSosPhaseAtUnit(18.5f)).isSounding())
                .isTrue();
            assertThat(PhasePattern.SOS.resolveBeatAt(resolveSosPhaseAtUnit(20.5f)).isSounding())
                .isTrue();
            assertThat(PhasePattern.SOS.resolveBeatAt(resolveSosPhaseAtUnit(22.5f)).isSounding())
                .isTrue();
        }

        @Test
        void resolveBeatAtRestsAfterTheDistressSilhouetteForLongerThanItPartsItsOwnSounds() {
            // The trailing rest is the whole reason the group repeats as a message: a silence no longer than
            // the internal gaps would run the last sound of one turn into the first of the next.
            assertThat(PhasePattern.SOS.resolveBeatAt(resolveSosPhaseAtUnit(23.5f)).isSounding())
                .isFalse();
            assertThat(PhasePattern.SOS.resolveBeatAt(resolveSosPhaseAtUnit(29.5f)).isSounding())
                .isFalse();
        }

        @Test
        void resolveBeatAtReadsTheEndOfATurnAsTheOpeningOfTheNext() {
            // The seam a wrapping phase crosses: the last instant of a turn and the first of the next are the
            // same moment of the rhythm, so neither an element nor a repeat goes missing there.
            var beat = PhasePattern.SOS.resolveBeatAt(TURN_END);

            assertThat(beat.isSounding())
                .isTrue();
            assertThat(beat.beatPhase())
                .isCloseTo(ELEMENT_START, within(TOLERANCE));
        }

        @Test
        void resolveBeatAtConfinesAPhaseFromOutsideTheTurn() {
            // A caller reading a phase of its own need not guard it: a reading past either end is answered
            // with that end rather than with an element the sequence does not have.
            assertThat(PhasePattern.STEADY_BEAT.resolveBeatAt(-0.5f).isSounding())
                .isTrue();
            assertThat(PhasePattern.STEADY_BEAT.resolveBeatAt(1.5f).isSounding())
                .isTrue();
        }
    }

    @Nested
    class ResolvePeriodSeconds {

        @Test
        void resolvePeriodSecondsScalesASteadyBeatsSoundAndSilenceByThePace() {

            assertThat(PhasePattern.STEADY_BEAT.resolvePeriodSeconds(UNIT_SECONDS))
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void resolvePeriodSecondsScalesTheWholeDistressSilhouetteByThePace() {
            // The reading exists so a consumer drives its clock with a period the rhythm agrees with; getting
            // it wrong would cut the silhouette off part way or leave it padded with a silence it never had.
            assertThat(PhasePattern.SOS.resolvePeriodSeconds(UNIT_SECONDS))
                .isCloseTo(15f, within(TOLERANCE));
        }
    }

    // A position in the rhythm, expressed as the phase a consumer would be holding when the cycle stands
    // there. The input to a case rather than its expectation, so no assertion is derived from the arithmetic
    // under test.
    private static float resolveSteadyBeatPhaseAtUnit(float unitPosition) {
        return unitPosition / STEADY_BEAT_CYCLE_UNITS;
    }

    private static float resolveSosPhaseAtUnit(float unitPosition) {
        return unitPosition / SOS_CYCLE_UNITS;
    }
}
