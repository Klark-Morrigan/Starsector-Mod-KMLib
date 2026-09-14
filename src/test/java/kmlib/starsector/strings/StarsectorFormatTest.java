package kmlib.starsector.strings;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link StarsectorFormat#formatPercent(float)} against the
 * truncate-not-round contract called out in the helper's KDoc. The
 * tooltip copy across the KM* mod family reads these figures back at
 * the player, so a silent rounding flip would mislead them on a value
 * the modder picked deliberately.
 */
class StarsectorFormatTest {

    @Nested
    class FormatPercent {
        @Test
        void formatPercentRendersWholePercentWithSign() {
            // Default-shape value from a one-significant-figure setting.
            assertThat(StarsectorFormat.formatPercent(0.20f))
                .isEqualTo("20%");
        }

        @Test
        void formatPercentRendersSmallFractionAsOnePercent() {
            // 0.01f is the KMU/KMO baseline magnitude for per-instance
            // bonuses; rendering it as 1% is the everyday case.
            assertThat(StarsectorFormat.formatPercent(0.01f))
                .isEqualTo("1%");
        }

        @Test
        void formatPercentTruncatesRatherThanRounds() {
            // 1.4% would round to 1% under standard rounding too, but the
            // contract is truncation so a value of 1.9% must also collapse
            // to 1% rather than rounding up to 2%. Pinning the upper edge
            // catches a future swap of (int) for Math.round() that would
            // otherwise look harmless on the default-magnitude cases.
            assertThat(StarsectorFormat.formatPercent(0.019f))
                .isEqualTo("1%");
        }

        @Test
        void formatPercentRendersZeroAsZero() {
            assertThat(StarsectorFormat.formatPercent(0f))
                .isEqualTo("0%");
        }
    }
}
