package kmlib.profiling;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link Timings}: the conversions between nanoseconds and the coarser units either
 * way, and the fixed two-decimal "{@code 1.23ms}" format used by timing output.
 */
class TimingsTest {

    @Nested
    class ConvertNanosToMillis {

        @Test
        void convertNanosToMillisDividesByAMillion() {

            assertThat(Timings.convertNanosToMillis(2_500_000L))
                .isEqualTo(2.5);
        }

        @Test
        void convertNanosToMillisIsZeroForZero() {

            assertThat(Timings.convertNanosToMillis(0L))
                .isZero();
        }
    }

    @Nested
    class ConvertNanosToSeconds {

        @Test
        void convertNanosToSecondsDividesByABillion() {

            assertThat(Timings.convertNanosToSeconds(2_500_000_000L))
                .isEqualTo(2.5);
        }

        @Test
        void convertNanosToSecondsKeepsSubSecondPrecision() {
            // A phase read off the clock lands mid-second far more often than on one, so the
            // fraction is the case that matters rather than a whole-second divide.
            assertThat(Timings.convertNanosToSeconds(1_500_000L))
                .isEqualTo(0.0015);
        }

        @Test
        void convertNanosToSecondsIsZeroForZero() {

            assertThat(Timings.convertNanosToSeconds(0L))
                .isZero();
        }
    }

    @Nested
    class ConvertSecondsToNanos {

        @Test
        void convertSecondsToNanosMultipliesByABillion() {

            assertThat(Timings.convertSecondsToNanos(2.5))
                .isEqualTo(2_500_000_000L);
        }

        @Test
        void convertSecondsToNanosKeepsSubSecondPrecision() {
            // A duration stated in seconds is nearly always a fraction of one - an animation pace,
            // a frame span - so the fraction is the case that matters rather than a whole second.
            assertThat(Timings.convertSecondsToNanos(0.0015))
                .isEqualTo(1_500_000L);
        }

        @Test
        void convertSecondsToNanosIsZeroForZero() {

            assertThat(Timings.convertSecondsToNanos(0))
                .isZero();
        }
    }

    @Nested
    class ConvertNanosToMicros {

        @Test
        void convertNanosToMicrosDividesByAThousand() {

            assertThat(Timings.convertNanosToMicros(2_500L))
                .isEqualTo(2.5);
        }

        @Test
        void convertNanosToMicrosIsZeroForZero() {

            assertThat(Timings.convertNanosToMicros(0L))
                .isZero();
        }
    }

    @Nested
    class FormatMicros {

        @Test
        void formatMicrosShowsOneDecimalAndTheUnit() {

            assertThat(Timings.formatMicros(123_400L))
                .isEqualTo("123.4us");
        }

        @Test
        void formatMicrosRoundsToOneDecimal() {
            // 123.45us rounds up at the first decimal (second digit 5).
            assertThat(Timings.formatMicros(123_450L))
                .isEqualTo("123.5us");
        }

        @Test
        void formatMicrosKeepsASpanMillisecondsWouldRoundAway() {
            // Three microseconds is "0.00ms" at two decimals, which is the whole reason this
            // format exists rather than the millisecond one.
            assertThat(Timings.formatMicros(3_000L))
                .isEqualTo("3.0us");
        }

        @Test
        void formatMicrosIsZeroForZero() {

            assertThat(Timings.formatMicros(0L))
                .isEqualTo("0.0us");
        }
    }

    @Nested
    class FormatMillis {

        @Test
        void formatMillisShowsTwoDecimalsAndTheUnit() {

            assertThat(Timings.formatMillis(1_234_567L))
                .isEqualTo("1.23ms");
        }

        @Test
        void formatMillisRoundsToTwoDecimals() {
            // 1.238ms rounds up at the second decimal (third digit 8).
            assertThat(Timings.formatMillis(1_238_000L))
                .isEqualTo("1.24ms");
        }

        @Test
        void formatMillisIsZeroForZero() {

            assertThat(Timings.formatMillis(0L))
                .isEqualTo("0.00ms");
        }
    }
}
