package kmlib.profiling;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link Timings}: the nanosecond-to-millisecond and nanosecond-to-second
 * conversions, and the fixed two-decimal "{@code 1.23ms}" format used by timing output.
 */
class TimingsTest {

    @Nested
    class ConvertNanosToMillis {
        @Test
        void convertNanosToMillisDividesByAMillion() {
            assertThat(Timings.convertNanosToMillis(2_500_000L)).isEqualTo(2.5);
        }

        @Test
        void convertNanosToMillisIsZeroForZero() {
            assertThat(Timings.convertNanosToMillis(0L)).isZero();
        }
    }

    @Nested
    class ConvertNanosToSeconds {
        @Test
        void convertNanosToSecondsDividesByABillion() {
            assertThat(Timings.convertNanosToSeconds(2_500_000_000L)).isEqualTo(2.5);
        }

        @Test
        void convertNanosToSecondsKeepsSubSecondPrecision() {
            // A phase read off the clock lands mid-second far more often than on one, so the
            // fraction is the case that matters rather than a whole-second divide.
            assertThat(Timings.convertNanosToSeconds(1_500_000L)).isEqualTo(0.0015);
        }

        @Test
        void convertNanosToSecondsIsZeroForZero() {
            assertThat(Timings.convertNanosToSeconds(0L)).isZero();
        }
    }

    @Nested
    class FormatMillis {
        @Test
        void formatMillisShowsTwoDecimalsAndTheUnit() {
            assertThat(Timings.formatMillis(1_234_567L)).isEqualTo("1.23ms");
        }

        @Test
        void formatMillisRoundsToTwoDecimals() {
            // 1.238ms rounds up at the second decimal (third digit 8).
            assertThat(Timings.formatMillis(1_238_000L)).isEqualTo("1.24ms");
        }

        @Test
        void formatMillisIsZeroForZero() {
            assertThat(Timings.formatMillis(0L)).isEqualTo("0.00ms");
        }
    }
}
