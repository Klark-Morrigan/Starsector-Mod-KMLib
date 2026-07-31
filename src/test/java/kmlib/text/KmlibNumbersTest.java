package kmlib.text;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KmlibNumbersTest {

    @Nested
    class FormatDelta {
        @Test
        void formatDeltaIntPositive() {
            assertThat(KmlibNumbers.formatDelta(3))
                .isEqualTo("+3");
        }

        @Test
        void formatDeltaIntZero() {
            assertThat(KmlibNumbers.formatDelta(0))
                .isEqualTo("+0");
        }

        @Test
        void formatDeltaIntNegative() {
            assertThat(KmlibNumbers.formatDelta(-5))
                .isEqualTo("-5");
        }

        @Test
        void formatDeltaFloatTruncatesTowardZero() {
            assertThat(KmlibNumbers.formatDelta(1.999f))
                .isEqualTo("+1");
            assertThat(KmlibNumbers.formatDelta(-1.999f))
                .isEqualTo("-1");
        }

        @Test
        void formatDeltaFloatZero() {
            assertThat(KmlibNumbers.formatDelta(0.0f))
                .isEqualTo("+0");
        }
    }

    @Nested
    class FormatGroupedInteger {
        @Test
        void formatGroupedIntegerZero() {
            assertThat(KmlibNumbers.formatGroupedInteger(0))
                .isEqualTo("0");
        }

        @Test
        void formatGroupedIntegerSubThousandRendersUngrouped() {
            // Below a thousand no separator is warranted.
            assertThat(KmlibNumbers.formatGroupedInteger(999))
                .isEqualTo("999");
        }

        @Test
        void formatGroupedIntegerGroupsAtFirstThousand() {
            // A comma regardless of the JVM's default locale, per the
            // fixed root locale in the contract.
            assertThat(KmlibNumbers.formatGroupedInteger(1000))
                .isEqualTo("1,000");
        }

        @Test
        void formatGroupedIntegerGroupsEveryThreeDigits() {
            assertThat(KmlibNumbers.formatGroupedInteger(1234567))
                .isEqualTo("1,234,567");
        }

        @Test
        void formatGroupedIntegerNegativeKeepsSignOutsideGrouping() {
            // The minus sits before the first digit, never adjacent to
            // a separator, and grouping counts digits only.
            assertThat(KmlibNumbers.formatGroupedInteger(-1234567))
                .isEqualTo("-1,234,567");
        }
    }

    @Nested
    class FormatScientific {
        @Test
        void formatScientificStripsExponentSignAndPadding() {
            assertThat(KmlibNumbers.formatScientific(1523.4))
                .isEqualTo("1.5e3");
        }

        @Test
        void formatScientificKeepsNegativeExponent() {
            assertThat(KmlibNumbers.formatScientific(0.05))
                .isEqualTo("5.0e-2");
        }

        @Test
        void formatScientificZero() {
            assertThat(KmlibNumbers.formatScientific(0.0))
                .isEqualTo("0.0e0");
        }
    }
}
