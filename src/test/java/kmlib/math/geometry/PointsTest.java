package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.assertj.core.api.Assertions.withinPercentage;

class PointsTest {

    @Nested
    class ComputeDistance {
        @Test
        void computeDistanceIsEuclidean() {
            assertThat(Points.computeDistance(0, 0, 3, 4)).isEqualTo(5.0);
        }

        @Test
        void computeDistanceIsZeroForCoincidentPoints() {
            assertThat(Points.computeDistance(2, 7, 2, 7)).isZero();
        }

        @Test
        void computeDistanceArrayOverloadMatchesCoordinateForm() {
            assertThat(Points.computeDistance(new double[] {1, 1}, new double[] {4, 5}))
                    .isEqualTo(5.0);
        }

        @Test
        void computeDistanceVectorOverloadMatchesCoordinateForm() {
            assertThat(Points.computeDistance(new Vector2f(1, 1), new Vector2f(4, 5)))
                    .isEqualTo(5.0);
        }
    }

    @Nested
    class ComputeVectorLength {
        @Test
        void computeVectorLengthIsTheEuclideanMagnitude() {
            assertThat(Points.computeVectorLength(3, 4)).isEqualTo(5.0);
        }

        @Test
        void computeVectorLengthMatchesFurtherPythagoreanTriples() {
            // Independent integer right triangles pin the magnitude at more than one
            // point, so a passing test is not a single-triple coincidence.
            assertThat(Points.computeVectorLength(5, 12)).isEqualTo(13.0);
            assertThat(Points.computeVectorLength(8, 15)).isEqualTo(17.0);
            assertThat(Points.computeVectorLength(7, 24)).isEqualTo(25.0);
        }

        @Test
        void computeVectorLengthIsZeroForTheZeroVector() {
            assertThat(Points.computeVectorLength(0, 0)).isZero();
        }

        @Test
        void computeVectorLengthEqualsTheLoneComponentWhenAxisAligned() {
            // With one component zero the magnitude collapses to the other's absolute
            // value - the vector lies flat along an axis.
            assertThat(Points.computeVectorLength(5, 0)).isEqualTo(5.0);
            assertThat(Points.computeVectorLength(0, 5)).isEqualTo(5.0);
            assertThat(Points.computeVectorLength(-5, 0)).isEqualTo(5.0);
            assertThat(Points.computeVectorLength(0, -5)).isEqualTo(5.0);
        }

        @Test
        void computeVectorLengthIgnoresComponentSign() {
            // Magnitude squares each component, so every sign pairing of the same
            // two components shares one length.
            assertThat(Points.computeVectorLength(-3, -4)).isEqualTo(5.0);
            assertThat(Points.computeVectorLength(-3, 4)).isEqualTo(5.0);
            assertThat(Points.computeVectorLength(3, -4)).isEqualTo(5.0);
        }

        @Test
        void computeVectorLengthIsSymmetricInItsComponents() {
            // Swapping x and y cannot change a sum of squares, so the length is the
            // same either way round.
            assertThat(Points.computeVectorLength(3, 4))
                    .isEqualTo(Points.computeVectorLength(4, 3));
        }

        @Test
        void computeVectorLengthHandlesEqualComponents() {
            // A 45-degree vector: both legs equal, so the magnitude is that leg times
            // root two.
            assertThat(Points.computeVectorLength(1, 1)).isCloseTo(Math.sqrt(2), within(1e-12));
        }

        @Test
        void computeVectorLengthHandlesFractionalComponents() {
            assertThat(Points.computeVectorLength(0.3, 0.4)).isCloseTo(0.5, within(1e-12));
        }

        @Test
        void computeVectorLengthScalesLinearlyWithItsComponents() {
            // Scaling both components by k scales the magnitude by |k| - the positive
            // homogeneity any norm must hold.
            var base = Points.computeVectorLength(3, 4);
            assertThat(Points.computeVectorLength(30, 40)).isCloseTo(base * 10, within(1e-9));
        }

        @Test
        void computeVectorLengthStaysFiniteWhenSquaringWouldOverflow() {
            // Each squared component (1e200^2 = 1e400) overflows a double to infinity,
            // so a naive sqrt(x*x + y*y) reports infinity here; the true magnitude is
            // 1e200 * sqrt(2), well within range. An overflow-safe evaluation keeps
            // the answer finite and correct.
            assertThat(Points.computeVectorLength(1e200, 1e200))
                    .isCloseTo(1e200 * Math.sqrt(2), withinPercentage(1e-6));
        }

        @Test
        void computeVectorLengthStaysAccurateWhenSquaringWouldUnderflow() {
            // The mirror case: 1e-200^2 = 1e-400 underflows to zero, so a naive sqrt
            // reports zero for a non-zero vector. An overflow-safe evaluation keeps
            // the small magnitude 1e-200 * sqrt(2).
            assertThat(Points.computeVectorLength(1e-200, 1e-200))
                    .isCloseTo(1e-200 * Math.sqrt(2), withinPercentage(1e-6));
        }
    }

    @Nested
    class ComputeUnitVector {
        @Test
        void computeUnitVectorReturnsTheDirectionScaledToUnitLength() {
            var unit = Points.computeUnitVector(3, 4, 1e-9);

            assertThat(unit).isNotNull();
            assertThat(Math.hypot(unit[0], unit[1])).isCloseTo(1.0, within(1e-12));
            assertThat(unit[0]).isCloseTo(0.6, within(1e-12));
            assertThat(unit[1]).isCloseTo(0.8, within(1e-12));
        }

        @Test
        void computeUnitVectorKeepsTheComponentSigns() {
            var unit = Points.computeUnitVector(-3, -4, 1e-9);

            assertThat(unit[0]).isCloseTo(-0.6, within(1e-12));
            assertThat(unit[1]).isCloseTo(-0.8, within(1e-12));
        }

        @Test
        void computeUnitVectorReturnsTheAxisForAnAxisAlignedInput() {
            assertThat(Points.computeUnitVector(5, 0, 1e-9)).containsExactly(1.0, 0.0);
            assertThat(Points.computeUnitVector(0, 5, 1e-9)).containsExactly(0.0, 1.0);
            assertThat(Points.computeUnitVector(-5, 0, 1e-9)).containsExactly(-1.0, 0.0);
        }

        @Test
        void computeUnitVectorIsNullForTheZeroVector() {
            assertThat(Points.computeUnitVector(0, 0, 1e-9)).isNull();
        }

        @Test
        void computeUnitVectorIsNullBelowTheGivenFloor() {
            // Length 1 sits under a floor of 2, so the direction counts as undefined.
            assertThat(Points.computeUnitVector(1, 0, 2.0)).isNull();
        }

        @Test
        void computeUnitVectorAcceptsALengthExactlyAtTheFloor() {
            // The floor is a strict lower bound: a vector whose length equals it keeps
            // its direction and normalises rather than returning null.
            assertThat(Points.computeUnitVector(2, 0, 2.0)).containsExactly(1.0, 0.0);
        }

        @Test
        void computeUnitVectorScalesTheFloorToTheCallersOwnMagnitude() {
            // The same tiny vector is a valid direction under a tiny floor and a
            // degenerate one under a coarse floor - the caller chooses the scale.
            assertThat(Points.computeUnitVector(1e-4, 0, 1e-9)).containsExactly(1.0, 0.0);
            assertThat(Points.computeUnitVector(1e-4, 0, 1e-3)).isNull();
        }

        @Test
        void computeUnitVectorStaysUnitLengthWhenSquaringWouldOverflow() {
            // The shared magnitude step is overflow-safe, so even a huge input yields a
            // genuine unit vector rather than NaN from an infinity divided by infinity.
            var unit = Points.computeUnitVector(1e200, 1e200, 1e-9);

            assertThat(unit).isNotNull();
            assertThat(Math.hypot(unit[0], unit[1])).isCloseTo(1.0, within(1e-12));
            assertThat(unit[0]).isCloseTo(Math.sqrt(0.5), within(1e-12));
            assertThat(unit[1]).isCloseTo(Math.sqrt(0.5), within(1e-12));
        }
    }

    @Nested
    class ComputeAngleDegrees {
        @Test
        void computeAngleDegreesVectorOverloadMatchesCoordinateForm() {
            assertThat(Points.computeAngleDegrees(new Vector2f(0, 0), new Vector2f(1, 1)))
                    .isEqualTo(45.0);
        }

        @Test
        void computeAngleDegreesIsCounterClockwiseFromPositiveX() {
            assertThat(Points.computeAngleDegrees(0, 0, 1, 1)).isEqualTo(45.0);
            assertThat(Points.computeAngleDegrees(0, 0, 0, 1)).isEqualTo(90.0);
            assertThat(Points.computeAngleDegrees(0, 0, -1, 0)).isEqualTo(180.0);
        }

        @Test
        void computeAngleDegreesIsRelativeToTheFirstPoint() {
            assertThat(Points.computeAngleDegrees(2, 2, 5, 6)).isCloseTo(53.13,
                    within(0.01));
        }
    }
}
