package kmlib.text;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KmlibNumbersTest {

    @Nested
    class FormatDelta {
        @Test
        void formatDeltaIntPositive() {
            assertThat(KmlibNumbers.formatDelta(3)).isEqualTo("+3");
        }

        @Test
        void formatDeltaIntZero() {
            assertThat(KmlibNumbers.formatDelta(0)).isEqualTo("+0");
        }

        @Test
        void formatDeltaIntNegative() {
            assertThat(KmlibNumbers.formatDelta(-5)).isEqualTo("-5");
        }

        @Test
        void formatDeltaFloatTruncatesTowardZero() {
            assertThat(KmlibNumbers.formatDelta(1.999f)).isEqualTo("+1");
            assertThat(KmlibNumbers.formatDelta(-1.999f)).isEqualTo("-1");
        }

        @Test
        void formatDeltaFloatZero() {
            assertThat(KmlibNumbers.formatDelta(0.0f)).isEqualTo("+0");
        }
    }

    @Nested
    class FormatScientific {
        @Test
        void formatScientificStripsExponentSignAndPadding() {
            assertThat(KmlibNumbers.formatScientific(1523.4)).isEqualTo("1.5e3");
        }

        @Test
        void formatScientificKeepsNegativeExponent() {
            assertThat(KmlibNumbers.formatScientific(0.05)).isEqualTo("5.0e-2");
        }

        @Test
        void formatScientificZero() {
            assertThat(KmlibNumbers.formatScientific(0.0)).isEqualTo("0.0e0");
        }
    }
}
