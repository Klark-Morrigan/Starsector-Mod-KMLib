package kmlib.time;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link Timings}: the conversions between nanoseconds and the coarser units either
 * way, and the fixed three-decimal "{@code 1.234ms}" format used by timing output.
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
    class ConvertMillisToNanos {

        @Test
        void convertMillisToNanosMultipliesByAMillion() {

            assertThat(Timings.convertMillisToNanos(2.5))
                .isEqualTo(2_500_000L);
        }

        @Test
        void convertMillisToNanosKeepsSubMillisecondPrecision() {
            // A frame budget is stated in whole milliseconds, but the knob behind one moves in
            // fractions of one, so the fraction is the case that matters.
            assertThat(Timings.convertMillisToNanos(0.25))
                .isEqualTo(250_000L);
        }

        @Test
        void convertMillisToNanosIsZeroForZero() {

            assertThat(Timings.convertMillisToNanos(0))
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
            // Three microseconds is "0.003ms" in milliseconds - a leading zero and two of them
            // spent before a digit says anything, which is why a per-item cost gets this format
            // rather than the millisecond one.
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
        void formatMillisShowsThreeDecimalsAndTheUnit() {

            assertThat(Timings.formatMillis(1_234_567L))
                .isEqualTo("1.235ms");
        }

        @Test
        void formatMillisRoundsToThreeDecimals() {
            // 1.2348ms rounds up at the third decimal (fourth digit 8).
            assertThat(Timings.formatMillis(1_234_800L))
                .isEqualTo("1.235ms");
        }

        @Test
        void formatMillisKeepsASpanTwoDecimalsWouldRoundAway() {
            // Eight microseconds is "0.01ms" at two decimals and "0.00ms" at anything coarser -
            // three is what keeps a sub-ten-microsecond row from reading as nothing.
            assertThat(Timings.formatMillis(8_000L))
                .isEqualTo("0.008ms");
        }

        @Test
        void formatMillisIsZeroForZero() {

            assertThat(Timings.formatMillis(0L))
                .isEqualTo("0.000ms");
        }
    }
}
