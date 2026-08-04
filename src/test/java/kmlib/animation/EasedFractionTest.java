package kmlib.animation;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * Pins {@link EasedFraction}: a linear position stepped by elapsed time toward a target, stopped exactly on
 * it, and eased only on read - so a retarget mid-flight carries on from where the fraction stands. A fresh
 * fraction sits at the empty end, so every case drives it from a known start. Elapsed times are expressed as
 * fractions of one duration, keeping the arithmetic independent of the concrete pace, and the eased
 * expectations are the smoothstep values of the linear positions those steps land on.
 */
final class EasedFractionTest {

    private static final float TOLERANCE = 0.0001f;
    private static final float DURATION = 0.25f;
    private static final float FULL_DURATION = DURATION;
    private static final float HALF_DURATION = DURATION / 2f;
    private static final float QUARTER_DURATION = DURATION / 4f;
    private static final float EMPTY_TARGET = 0f;
    private static final float FULL_TARGET = 1f;

    @Nested
    class CreateAtValue {

        @Test
        void createAtValueSeedsTheFractionSettledAtTheFullEnd() {

            var fraction = EasedFraction.createAtValue(1f);

            assertThat(fraction.getEasedValue())
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(fraction.hasReachedTarget(FULL_TARGET))
                .isTrue();
        }

        @Test
        void createAtValueSeedsTheFractionPartWayForAMidRangeValue() {
            // A quarter of the way along, so the eased read is the smoothstep value of 0.25 rather than an
            // end - a seeded fraction is a position like any other, not only an end state.
            var fraction = EasedFraction.createAtValue(0.25f);

            assertThat(fraction.getEasedValue())
                .isCloseTo(0.15625f, within(TOLERANCE));
        }

        @Test
        void createAtValueClampsASeedAboveTheRangeToTheFullEnd() {

            var fraction = EasedFraction.createAtValue(2f);

            assertThat(fraction.getEasedValue())
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(fraction.hasReachedTarget(FULL_TARGET))
                .isTrue();
        }

        @Test
        void createAtValueClampsASeedBelowTheRangeToTheEmptyEnd() {

            var fraction = EasedFraction.createAtValue(-1f);

            assertThat(fraction.getEasedValue())
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(fraction.hasReachedTarget(EMPTY_TARGET))
                .isTrue();
        }
    }

    @Nested
    class AdvanceTowardTarget {

        @Test
        void advanceTowardTargetReachesTheFullEndAfterAFullDuration() {

            var fraction = new EasedFraction();
            fraction.advanceTowardTarget(FULL_TARGET, FULL_DURATION, DURATION);

            assertThat(fraction.getEasedValue())
                .isCloseTo(1f, within(TOLERANCE));
        }

        @Test
        void advanceTowardTargetStopsOnTheTargetRatherThanOvershooting() {

            var fraction = new EasedFraction();

            // Two full durations would step the position to 2; it settles on the target instead.
            fraction.advanceTowardTarget(FULL_TARGET, FULL_DURATION * 2f, DURATION);

            assertThat(fraction.getEasedValue())
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(fraction.hasReachedTarget(FULL_TARGET))
                .isTrue();
        }

        @Test
        void advanceTowardTargetStopsOnAMidRangeTarget() {

            var fraction = new EasedFraction();

            // A whole duration's step aimed a quarter of the way along stops there, so a target need not be
            // an end of the range for the advance to settle exactly on it.
            fraction.advanceTowardTarget(0.25f, FULL_DURATION, DURATION);

            assertThat(fraction.getEasedValue())
                .isCloseTo(0.15625f, within(TOLERANCE));
            assertThat(fraction.hasReachedTarget(0.25f))
                .isTrue();
        }

        @Test
        void advanceTowardTargetLeavesAFractionAlreadyAtItsTargetUnchanged() {

            var fraction = new EasedFraction();

            // Fresh at the empty end and aimed there, an unconditional per-frame advance cannot push the
            // position below the range.
            fraction.advanceTowardTarget(EMPTY_TARGET, FULL_DURATION, DURATION);

            assertThat(fraction.getEasedValue())
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(fraction.hasReachedTarget(EMPTY_TARGET))
                .isTrue();
        }

        @Test
        void advanceTowardTargetTravelsBackToTheEmptyEndFromTheFullOne() {

            var fraction = EasedFraction.createAtValue(1f);
            fraction.advanceTowardTarget(EMPTY_TARGET, FULL_DURATION, DURATION);

            assertThat(fraction.getEasedValue())
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(fraction.hasReachedTarget(EMPTY_TARGET))
                .isTrue();
        }

        @Test
        void advanceTowardTargetCarriesOnFromTheCurrentPositionWhenRetargetedMidFlight() {

            var fraction = new EasedFraction();
            fraction.advanceTowardTarget(FULL_TARGET, HALF_DURATION, DURATION);

            // Aimed back at the empty end from halfway and stepped a quarter, the position falls to 0.25 -
            // the retarget moved it on from where it stood rather than replaying a curve from either end.
            fraction.advanceTowardTarget(EMPTY_TARGET, QUARTER_DURATION, DURATION);

            assertThat(fraction.getEasedValue())
                .isCloseTo(0.15625f, within(TOLERANCE));
        }

        @Test
        void advanceTowardTargetAccumulatesManySmallStepsLikeOneBigStep() {

            var fraction = new EasedFraction();

            // Five per-frame slices of a tenth of the duration land on the same halfway position one
            // half-duration step reaches, so the pace does not depend on how the frames split the time.
            for (var frame = 0; frame < 5; frame++) {
                fraction.advanceTowardTarget(FULL_TARGET, DURATION / 10f, DURATION);
            }
            assertThat(fraction.getEasedValue())
                .isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void advanceTowardTargetPacesByTheRatioOfElapsedTimeToDuration() {

            var fraction = new EasedFraction();

            // Half a second of a two-second pace is a quarter of the way along, the same position a quarter
            // of any other duration reaches - only the ratio matters.
            fraction.advanceTowardTarget(FULL_TARGET, 0.5f, 2f);

            assertThat(fraction.getEasedValue())
                .isCloseTo(0.15625f, within(TOLERANCE));
        }

        @Test
        void advanceTowardTargetSnapsToTheTargetInOneStepWhenDurationIsZero() {

            var fraction = new EasedFraction();

            // A zero duration is the "no animation" setting: one advance covers the whole way rather than
            // dividing by zero. Even a tiny elapsed slice completes it.
            fraction.advanceTowardTarget(FULL_TARGET, HALF_DURATION, 0f);

            assertThat(fraction.getEasedValue())
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(fraction.hasReachedTarget(FULL_TARGET))
                .isTrue();
        }

        @Test
        void advanceTowardTargetClampsATargetAboveTheRangeToTheFullEnd() {
            
            var fraction = new EasedFraction();
            fraction.advanceTowardTarget(2f, FULL_DURATION, DURATION);

            assertThat(fraction.getEasedValue())
                .isCloseTo(1f, within(TOLERANCE));
            assertThat(fraction.hasReachedTarget(FULL_TARGET))
                .isTrue();
        }

        @Test
        void advanceTowardTargetClampsATargetBelowTheRangeToTheEmptyEnd() {

            var fraction = EasedFraction.createAtValue(1f);
            fraction.advanceTowardTarget(-1f, FULL_DURATION, DURATION);

            assertThat(fraction.getEasedValue())
                .isCloseTo(0f, within(TOLERANCE));
            assertThat(fraction.hasReachedTarget(EMPTY_TARGET))
                .isTrue();
        }
    }

    @Nested
    class GetEasedValue {

        @Test
        void getEasedValueStartsAtTheEmptyEndForAFreshFraction() {
            assertThat(new EasedFraction().getEasedValue())
                .isCloseTo(0f, within(TOLERANCE));
        }

        @Test
        void getEasedValueEasesTheHalfwayPositionToTheCurveMidpoint() {

            var fraction = new EasedFraction();
            fraction.advanceTowardTarget(FULL_TARGET, HALF_DURATION, DURATION);

            // The halfway position eases to the smoothstep midpoint, which happens to sit back on the
            // linear line.
            assertThat(fraction.getEasedValue())
                .isCloseTo(0.5f, within(TOLERANCE));
        }

        @Test
        void getEasedValueTrailsTheLinearPositionAQuarterOfTheWayIn() {

            var fraction = new EasedFraction();
            fraction.advanceTowardTarget(FULL_TARGET, QUARTER_DURATION, DURATION);

            // A quarter along, the eased value trails the linear 0.25 - the slow, accelerating start.
            assertThat(fraction.getEasedValue())
                .isCloseTo(0.15625f, within(TOLERANCE));
        }

        @Test
        void getEasedValueLeadsTheLinearPositionThreeQuartersOfTheWayIn() {

            var fraction = new EasedFraction();
            fraction.advanceTowardTarget(FULL_TARGET, DURATION * 3f / 4f, DURATION);

            // Three quarters along, the eased value leads the linear 0.75 - the fast middle before it
            // settles into the end.
            assertThat(fraction.getEasedValue())
                .isCloseTo(0.84375f, within(TOLERANCE));
        }
    }

    @Nested
    class HasReachedTarget {

        @Test
        void hasReachedTargetIsTrueForAFreshFractionAtTheEmptyEnd() {
            assertThat(new EasedFraction().hasReachedTarget(EMPTY_TARGET))
                .isTrue();
        }

        @Test
        void hasReachedTargetIsFalseAtEitherEndWhileMidFlight() {

            var fraction = new EasedFraction();
            fraction.advanceTowardTarget(FULL_TARGET, HALF_DURATION, DURATION);

            assertThat(fraction.hasReachedTarget(FULL_TARGET))
                .isFalse();
            assertThat(fraction.hasReachedTarget(EMPTY_TARGET))
                .isFalse();
        }

        @Test
        void hasReachedTargetIsTrueOnceTheAdvanceSettlesOnIt() {
            
            var fraction = new EasedFraction();
            fraction.advanceTowardTarget(FULL_TARGET, FULL_DURATION, DURATION);
            
            assertThat(fraction.hasReachedTarget(FULL_TARGET))
                .isTrue();
        }

        @Test
        void hasReachedTargetClampsATargetAboveTheRange() {
            // The advance confines an out-of-range target to the range, so the arrival test must confine it
            // the same way or a fraction settled at the full end would read as still travelling.
            assertThat(EasedFraction.createAtValue(1f).hasReachedTarget(2f))
                .isTrue();
        }

        @Test
        void hasReachedTargetClampsATargetBelowTheRange() {
            assertThat(new EasedFraction().hasReachedTarget(-1f))
                .isTrue();
        }
    }
}
