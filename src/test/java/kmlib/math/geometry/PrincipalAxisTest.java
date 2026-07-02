package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class PrincipalAxisTest {

    @Nested
    class FitTo {
        @Test
        void fitToCentresOnTheMeanOfThePoints() {
            var axis = PrincipalAxis.fitTo(
                    List.of(new double[] {0, 0}, new double[] {4, 0}, new double[] {2, 6}));

            assertThat(axis.centroidX()).isCloseTo(2.0, within(1e-9));
            assertThat(axis.centroidY()).isCloseTo(2.0, within(1e-9));
        }

        @Test
        void fitToPointsAlongTheDirectionOfGreatestSpread() {
            // Points spread far along x and little along y, so the axis is the x
            // direction (unit length). The sign is arbitrary, so compare magnitudes.
            var axis = PrincipalAxis.fitTo(List.of(
                    new double[] {-10, 0}, new double[] {0, 1}, new double[] {10, 0}));

            assertThat(Math.abs(axis.axisX())).isCloseTo(1.0, within(1e-6));
            assertThat(Math.abs(axis.axisY())).isCloseTo(0.0, within(1e-6));
        }

        @Test
        void fitToFindsADiagonalSpread() {
            // A cloud stretched along y = x: the axis is the 45-degree diagonal, so
            // its two components share a magnitude.
            var axis = PrincipalAxis.fitTo(List.of(
                    new double[] {0, 0}, new double[] {1, 1}, new double[] {2, 2},
                    new double[] {3, 3}));

            assertThat(Math.abs(axis.axisX())).isCloseTo(Math.sqrt(0.5), within(1e-6));
            assertThat(Math.abs(axis.axisY())).isCloseTo(Math.sqrt(0.5), within(1e-6));
        }

        @Test
        void fitToMeasuresTheExtentAlongTheAxis() {
            // Span 20 along x (from -10 to 10); the projected extent is that full span.
            var axis = PrincipalAxis.fitTo(List.of(
                    new double[] {-10, 0}, new double[] {0, 1}, new double[] {10, 0}));

            assertThat(axis.length()).isCloseTo(20.0, within(1e-6));
        }

        @Test
        void fitToYieldsAUnitVector() {
            var axis = PrincipalAxis.fitTo(List.of(
                    new double[] {1, 2}, new double[] {5, 9}, new double[] {3, 4}));

            assertThat(Math.hypot(axis.axisX(), axis.axisY())).isCloseTo(1.0, within(1e-9));
        }

        @Test
        void fitToFallsBackToTheXAxisForASinglePoint() {
            // One point has a centroid but no direction or spread; the axis defaults
            // to the x-axis with zero length rather than a NaN from dividing by a
            // zero eigenvector.
            var axis = PrincipalAxis.fitTo(List.of(new double[] {7, 3}));

            assertThat(axis.centroidX()).isEqualTo(7.0);
            assertThat(axis.centroidY()).isEqualTo(3.0);
            assertThat(axis.axisX()).isEqualTo(1.0);
            assertThat(axis.axisY()).isEqualTo(0.0);
            assertThat(axis.length()).isZero();
        }

        @Test
        void fitToFallsBackToTheXAxisForAnIsotropicSpread() {
            // A symmetric square has equal spread in every direction, so no axis
            // dominates; the fallback keeps the result finite.
            var axis = PrincipalAxis.fitTo(List.of(
                    new double[] {-1, -1}, new double[] {1, -1},
                    new double[] {1, 1}, new double[] {-1, 1}));

            assertThat(axis.axisX()).isEqualTo(1.0);
            assertThat(axis.axisY()).isEqualTo(0.0);
        }

        @Test
        void fitToRejectsAnEmptyCloud() {
            assertThatThrownBy(() -> PrincipalAxis.fitTo(List.of()))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
