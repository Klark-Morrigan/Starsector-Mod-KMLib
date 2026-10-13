package kmlib.math.percentages;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the round-to-nearest contract of {@link Percentages#computeWholePercent(float)}. The float cases are the ones
 * a truncating conversion puts one percent low.
 */
class PercentagesTests {

    @Nested
    class ComputeWholePercent {

        @Test
        void returnsWholePercentOfOneSignificantFigureSetting() {

            assertThat(Percentages.computeWholePercent(0.20f))
                .isEqualTo(20);
        }

        // 0.53f is stored as 0.52999997.
        @Test
        void returnsTypedPercentWhenFloatFallsJustUnder() {

            assertThat(Percentages.computeWholePercent(0.53f))
                .isEqualTo(53);
        }

        // A fraction computed as a base minus a discount: 0.06f - 0.01f is 0.049999997.
        @Test
        void returnsWholePercentWhenComputedFractionFallsJustUnder() {

            assertThat(Percentages.computeWholePercent(0.06f - 0.01f))
                .isEqualTo(5);
        }

        @Test
        void roundsUpFromAboveHalf() {

            assertThat(Percentages.computeWholePercent(0.019f))
                .isEqualTo(2);
        }

        @Test
        void roundsDownFromBelowHalf() {

            assertThat(Percentages.computeWholePercent(0.014f))
                .isEqualTo(1);
        }

        @Test
        void returnsNegativeWholePercentForNegativeFraction() {

            assertThat(Percentages.computeWholePercent(-0.05f))
                .isEqualTo(-5);
        }
    }
}
