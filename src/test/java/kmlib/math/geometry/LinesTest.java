package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LinesTest {
    }

    @Nested
    class ComputeSignedOffsetFromLine {
        @Test
        void computeSignedOffsetFromLineIsPositiveOnTheNormalSide() {
            // Line x = 5 with the normal pointing +x: a point at x = 8 is three
            // units into the kept side.
            assertThat(Lines.computeSignedOffsetFromLine(new double[] {8, 0}, 5, 0, 1, 0))
                    .isEqualTo(3.0);
        }

        @Test
        void computeSignedOffsetFromLineIsNegativeOnTheFarSide() {
            assertThat(Lines.computeSignedOffsetFromLine(new double[] {2, 0}, 5, 0, 1, 0))
                    .isEqualTo(-3.0);
        }

        @Test
        void computeSignedOffsetFromLineIsZeroOnTheLine() {
            assertThat(Lines.computeSignedOffsetFromLine(new double[] {5, 100}, 5, 0, 1, 0))
                    .isZero();
        }

        @Test
        void computeSignedOffsetFromLineScalesWithANonUnitNormal() {
            // Only the sign is reliable when the normal is not unit length: a
            // normal of length two doubles the magnitude but keeps the side.
            assertThat(Lines.computeSignedOffsetFromLine(new double[] {8, 0}, 5, 0, 2, 0))
                    .isEqualTo(6.0);
        }
    }
}
