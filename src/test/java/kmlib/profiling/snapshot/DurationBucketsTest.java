package kmlib.profiling.snapshot;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins where a duration lands: everything under a microsecond in the first band, a band per doubling
 * after it with each boundary belonging to the band it opens, and everything past the last floor in
 * the last band rather than off the end. Plus that a set of bands cannot be changed through the
 * array it was built from.
 */
final class DurationBucketsTest {

    private static final long HALF_MICROSECOND_IN_NANOS = 500L;
    private static final long ONE_MICROSECOND_IN_NANOS = 1_000L;
    private static final long JUST_UNDER_TWO_MICROSECONDS_IN_NANOS = 1_999L;
    private static final long TWO_MICROSECONDS_IN_NANOS = 2_000L;
    private static final long FOUR_MICROSECONDS_IN_NANOS = 4_000L;
    private static final long TEN_SECONDS_IN_NANOS = 10_000_000_000L;

    private static final int SUB_MICROSECOND_BAND = 0;
    private static final int FIRST_MICROSECOND_BAND = 1;
    private static final int SECOND_MICROSECOND_BAND = 2;
    private static final int THIRD_MICROSECOND_BAND = 3;

    private static final long ONE_CALL = 1L;

    @Nested
    class ResolveBucketIndex {

        @Test
        void placesEveryCallTooShortToMatterInOneBand() {
            // A call under a microsecond is free at frame scale, so how free it was is not worth
            // bands of its own.
            assertThat(DurationBuckets.resolveBucketIndex(HALF_MICROSECOND_IN_NANOS))
                .isEqualTo(SUB_MICROSECOND_BAND);
        }

        @Test
        void opensABandAtItsOwnFloorAndClosesItBelowTheNext() {
            // The boundary belongs to the band it opens, so a duration is in exactly one band and
            // two captures of the same call cannot fall either side of a line.
            assertThat(DurationBuckets.resolveBucketIndex(ONE_MICROSECOND_IN_NANOS))
                .isEqualTo(FIRST_MICROSECOND_BAND);
            assertThat(DurationBuckets.resolveBucketIndex(JUST_UNDER_TWO_MICROSECONDS_IN_NANOS))
                .isEqualTo(FIRST_MICROSECOND_BAND);
            assertThat(DurationBuckets.resolveBucketIndex(TWO_MICROSECONDS_IN_NANOS))
                .isEqualTo(SECOND_MICROSECOND_BAND);
            assertThat(DurationBuckets.resolveBucketIndex(FOUR_MICROSECONDS_IN_NANOS))
                .isEqualTo(THIRD_MICROSECOND_BAND);
        }

        @Test
        void placesADurationPastTheLastFloorInTheLastBand() {
            // A ten-second call is a stall to report, not a duration the scheme has no room for.
            assertThat(DurationBuckets.resolveBucketIndex(TEN_SECONDS_IN_NANOS))
                .isEqualTo(DurationBuckets.countBuckets() - 1);
        }
    }

    @Nested
    class GetCallsInBucket {

        @Test
        void keepsWhatItWasBuiltWithWhenTheArrayBehindItMovesOn() {
            // The array is a profiler's accumulator and goes on being added to after a snapshot is
            // taken; a reader holding a set of bands must not see it change under them.
            var callsPerBucket = new long[DurationBuckets.countBuckets()];

            callsPerBucket[FIRST_MICROSECOND_BAND] = ONE_CALL;

            var buckets = new DurationBuckets(callsPerBucket);

            callsPerBucket[FIRST_MICROSECOND_BAND] = 0;

            assertThat(buckets.getCallsInBucket(FIRST_MICROSECOND_BAND))
                .isEqualTo(ONE_CALL);
        }
    }

    @Nested
    class HasAnyCalls {

        @Test
        void readsASetOfBandsNoCallLandedInAsHavingNothingToShow() {

            assertThat(DurationBuckets.NO_CALLS.hasAnyCalls())
                .isFalse();
        }

        @Test
        void readsOneCallAnywhereAsAShapeWorthShowing() {

            var callsPerBucket = new long[DurationBuckets.countBuckets()];

            callsPerBucket[SUB_MICROSECOND_BAND] = ONE_CALL;

            assertThat(new DurationBuckets(callsPerBucket).hasAnyCalls())
                .isTrue();
        }
    }
}
