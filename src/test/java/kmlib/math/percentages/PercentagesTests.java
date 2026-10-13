package kmlib.math.percentages;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the conversions between fractions and percents. The whole-percent float cases are the ones a truncating
 * conversion puts one percent low.
 */
class PercentagesTests {

    @Nested
    class ConvertFractionToPercent {

        @Test
        void keepsTheDigitsPastAWholePercent() {

            assertThat(Percentages.convertFractionToPercent(0.125))
                .isEqualTo(12.5);
        }

        @Test
        void returnsZeroForZero() {

            assertThat(Percentages.convertFractionToPercent(0.0))
                .isEqualTo(0.0);
        }
    }

    @Nested
    class ConvertFractionToWholePercentFromDouble {

        @Test
        void returnsWholePercentOfOneSignificantFigureFraction() {

            assertThat(Percentages.convertFractionToWholePercent(0.20))
                .isEqualTo(20);
        }

        @Test
        void roundsUpFromAboveHalf() {

            assertThat(Percentages.convertFractionToWholePercent(0.019))
                .isEqualTo(2);
        }

        @Test
        void roundsDownFromBelowHalf() {

            assertThat(Percentages.convertFractionToWholePercent(0.014))
                .isEqualTo(1);
        }
    }

    @Nested
    class ConvertFractionToWholePercentFromFloat {

        @Test
        void returnsWholePercentOfOneSignificantFigureSetting() {

            assertThat(Percentages.convertFractionToWholePercent(0.20f))
                .isEqualTo(20);
        }

        // 0.53f is stored as 0.52999997.
        @Test
        void returnsTypedPercentWhenFloatFallsJustUnder() {

            assertThat(Percentages.convertFractionToWholePercent(0.53f))
                .isEqualTo(53);
        }

        // A fraction computed as a base minus a discount: 0.06f - 0.01f is 0.049999997.
        @Test
        void returnsWholePercentWhenComputedFractionFallsJustUnder() {

            assertThat(Percentages.convertFractionToWholePercent(0.06f - 0.01f))
                .isEqualTo(5);
        }

        // Widened to double, 0.015f lands just under 1.5 and would round down.
        @Test
        void roundsUpAtAHalfTheFloatProductHolds() {

            assertThat(Percentages.convertFractionToWholePercent(0.015f))
                .isEqualTo(2);
        }

        @Test
        void roundsDownFromBelowHalf() {

            assertThat(Percentages.convertFractionToWholePercent(0.014f))
                .isEqualTo(1);
        }

        @Test
        void returnsNegativeWholePercentForNegativeFraction() {

            assertThat(Percentages.convertFractionToWholePercent(-0.05f))
                .isEqualTo(-5);
        }
    }

    @Nested
    class ConvertPercentToFraction {

        @Test
        void returnsTheFractionOfAPercent() {

            assertThat(Percentages.convertPercentToFraction(12.5))
                .isEqualTo(0.125);
        }

        @Test
        void returnsOneForAHundred() {

            assertThat(Percentages.convertPercentToFraction(100.0))
                .isEqualTo(1.0);
        }
    }
}
