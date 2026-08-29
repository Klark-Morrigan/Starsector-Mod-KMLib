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

    private static final float TURN_START = 0f;
    private static final float TURN_END = 1f;
    private static final float ELEMENT_START = 0f;

    // Inside the last element of a turn whatever the rhythm, so the case closing every pattern on a silence
    // can ask each of them at one phase rather than at a position of its own.
    private static final float JUST_INSIDE_THE_TURNS_END = 0.999f;

    // A pace slow enough that a period reads as a different number from the unit count it scales.
    private static final float UNIT_SECONDS = 0.5f;

    @Nested
    class ResolveBeatAt {

        @Test
        void resolveBeatAtSoundsThroughTheFirstHalfOfASteadyBeat() {

            var beat = resolveSteadyBeatAtUnit(0.5f);

            assertThat(beat.isSounding())
                .isTrue();
            assertThat(beat.beatPhase())
                .isCloseTo(HALF_WAY_THROUGH, within(TOLERANCE));
        }

        @Test
        void resolveBeatAtRestsThroughTheSecondHalfOfASteadyBeat() {

            var beat = resolveSteadyBeatAtUnit(1.5f);

            assertThat(beat.isSounding())
                .isFalse();
            assertThat(beat.beatPhase())
                .isCloseTo(HALF_WAY_THROUGH, within(TOLERANCE));
        }

        @Test
        void resolveBeatAtOpensTheDistressSilhouetteWithThreeShortSounds() {
            // Each short sound is parted from the next by a silence as long as itself, which is what makes
            // three of them read as three rather than as one long one.
            assertThat(resolveSosBeatAtUnit(0.5f).isSounding())
                .isTrue();
            assertThat(resolveSosBeatAtUnit(1.5f).isSounding())
                .isFalse();
            assertThat(resolveSosBeatAtUnit(2.5f).isSounding())
                .isTrue();
            assertThat(resolveSosBeatAtUnit(3.5f).isSounding())
                .isFalse();
            assertThat(resolveSosBeatAtUnit(4.5f).isSounding())
                .isTrue();
            assertThat(resolveSosBeatAtUnit(5.5f).isSounding())
                .isFalse();
        }

        @Test
        void resolveBeatAtHoldsTheMiddleSoundsOfTheDistressSilhouetteThreeTimesAsLong() {
            // The middle group is what tells the silhouette from a plain string of flashes, so the sound has
            // to be pinned by both of its ends: it is still running a unit after a short one would have
            // stopped, and it stops before the next gap. Its halfway reading alone would not say that - a
            // short sound sitting a unit later would answer the same.
            assertThat(resolveSosBeatAtUnit(5.5f).isSounding())
                .isFalse();
            assertThat(resolveSosBeatAtUnit(6.5f).isSounding())
                .isTrue();
            assertThat(resolveSosBeatAtUnit(8.5f).isSounding())
                .isTrue();
            assertThat(resolveSosBeatAtUnit(9.5f).isSounding())
                .isFalse();

            assertThat(resolveSosBeatAtUnit(7.5f).beatPhase())
                .isCloseTo(HALF_WAY_THROUGH, within(TOLERANCE));
        }

        @Test
        void resolveBeatAtClosesTheDistressSilhouetteWithThreeShortSounds() {

            assertThat(resolveSosBeatAtUnit(18.5f).isSounding())
                .isTrue();
            assertThat(resolveSosBeatAtUnit(20.5f).isSounding())
                .isTrue();
            assertThat(resolveSosBeatAtUnit(22.5f).isSounding())
                .isTrue();
        }

        @Test
        void resolveBeatAtRestsAfterTheDistressSilhouetteForLongerThanItPartsItsOwnSounds() {
            // The trailing rest is the whole reason the group repeats as a message: a silence no longer than
            // the internal gaps would run the last sound of one turn into the first of the next.
            assertThat(resolveSosBeatAtUnit(23.5f).isSounding())
                .isFalse();
            assertThat(resolveSosBeatAtUnit(29.5f).isSounding())
                .isFalse();
        }

        @Test
        void resolveBeatAtOpensEveryRhythmOnASoundAndClosesEachOfThemOnASilence() {
            // The invariant the seam reading rests on: walking off the end of a turn lands on the opening
            // state only because every rhythm alternates from a sound and ends on a silence. Asked of the
            // whole set rather than of the two by name, so a rhythm added later that broke it is caught here
            // instead of by a repeat that has quietly run its last sound into its first.
            for (var pattern : PhasePattern.values()) {

                assertThat(pattern.resolveBeatAt(TURN_START).isSounding())
                    .isTrue();
                assertThat(pattern.resolveBeatAt(JUST_INSIDE_THE_TURNS_END).isSounding())
                    .isFalse();
            }
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

    // What a rhythm answers at a position named in its own beat units, converted to the phase a consumer
    // would be holding when the cycle stands there. The conversion is the input to a case rather than its
    // expectation, so no assertion is derived from the arithmetic under test.
    private static PatternBeat resolveSteadyBeatAtUnit(float unitPosition) {
        return PhasePattern.STEADY_BEAT.resolveBeatAt(unitPosition / STEADY_BEAT_CYCLE_UNITS);
    }

    private static PatternBeat resolveSosBeatAtUnit(float unitPosition) {
        return PhasePattern.SOS.resolveBeatAt(unitPosition / SOS_CYCLE_UNITS);
    }
}
