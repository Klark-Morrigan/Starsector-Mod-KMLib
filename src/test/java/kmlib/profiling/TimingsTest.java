package kmlib.profiling;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link Timings}: the nanosecond-to-millisecond conversion and the fixed
 * two-decimal "{@code 1.23ms}" format used by timing output.
 */
class TimingsTest {

    @Nested
    class NanosToMillis {
        @Test
        void nanosToMillisDividesByAMillion() {
            assertThat(Timings.nanosToMillis(2_500_000L)).isEqualTo(2.5);
        }

        @Test
        void nanosToMillisIsZeroForZero() {
            assertThat(Timings.nanosToMillis(0L)).isZero();
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
