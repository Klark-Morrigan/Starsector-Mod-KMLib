package kmlib.starsector.strings;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StarsectorFormatTests {

    @Nested
    class FormatPercent {

        @Test
        void rendersWholePercentWithSign() {

            assertThat(StarsectorFormat.formatPercent(0.20f))
                .isEqualTo("20%");
        }

        @Test
        void rendersSmallFractionAsOnePercent() {

            assertThat(StarsectorFormat.formatPercent(0.01f))
                .isEqualTo("1%");
        }

        @Test
        void rendersZeroAsZero() {

            assertThat(StarsectorFormat.formatPercent(0f))
                .isEqualTo("0%");
        }
    }

    @Nested
    class FormatTrimmedNumber {

        @Test
        void dropsTheDecimalOfAWholeFigure() {

            assertThat(StarsectorFormat.formatTrimmedNumber(2f))
                .isEqualTo("2");
        }

        @Test
        void keepsTheDecimalOfAFraction() {

            assertThat(StarsectorFormat.formatTrimmedNumber(1.5f))
                .isEqualTo("1.5");
        }

        // Float.toString switches to scientific notation from ten million up.
        @Test
        void rendersALargeFigureInFull() {

            assertThat(StarsectorFormat.formatTrimmedNumber(20_000_000f))
                .isEqualTo("20000000");
        }

        @Test
        void rendersZeroAsZero() {

            assertThat(StarsectorFormat.formatTrimmedNumber(0f))
                .isEqualTo("0");
        }
    }
}
