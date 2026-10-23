package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link RingStretch#computeLength}: how far along a path a stretch reaches, including
 * the stretch that closes past the perimeter.
 *
 * <p>The second case is the one worth stating. A stretch crossing the path's start is carried
 * on into the next lap rather than being stated as a start after its end, so its length is
 * plain subtraction like any other - and a caller comparing stretches gets the run's real
 * length rather than one wrapped back within a lap.
 *
 * <p>And {@link RingStretch#selectLongest}, which reads that length: the longest stretch wins
 * wherever it sits, a tie keeps the first, and no stretches choose nothing.
 */
final class RingStretchTests {

    @Nested
    class ComputeLength {

        @Test
        void lengthIsTheDistanceBetweenTheTwoEnds() {

            assertThat(new RingStretch(4, 10).computeLength())
                .isEqualTo(6.0);
        }

        @Test
        void stretchClosingPastThePerimeterMeasuresItsWholeRun() {
            // A stretch fused across a 24-long path's start: from 20 round through the origin to
            // 8 on the next lap, which is 12 of path rather than the 12 back to the start or a
            // negative reading of the two ends.
            assertThat(new RingStretch(20, 32).computeLength())
                .isEqualTo(12.0);
        }
    }

    @Nested
    class SelectLongest {

        // A stretch closing past the perimeter is measured on its whole run, so a run fused
        // across the start beats a shorter one that sits within a lap.
        @Test
        void theLongestStretchIsChosenWhereverItSits() {

            // ARRANGE
            var stretches = List.of(
                new RingStretch(2, 6),
                new RingStretch(8, 12),
                new RingStretch(20, 32));

            var expectedLongest = new RingStretch(20, 32);

            // ACT
            var actualLongest = RingStretch.selectLongest(stretches);

            // ASSERT
            assertThat(actualLongest)
                .isEqualTo(expectedLongest);
        }

        // Ties keep the earlier stretch, so the choice does not hang on the last of the rounding.
        @Test
        void aTieKeepsTheFirstStretch() {

            // ARRANGE
            var stretches = List.of(
                new RingStretch(0, 4),
                new RingStretch(10, 14));

            var expectedLongest = new RingStretch(0, 4);

            // ACT
            var actualLongest = RingStretch.selectLongest(stretches);

            // ASSERT
            assertThat(actualLongest)
                .isEqualTo(expectedLongest);
        }

        @Test
        void noStretchesLeaveNothingToChoose() {

            // ARRANGE
            List<RingStretch> stretches = List.of();

            // ACT
            var actualLongest = RingStretch.selectLongest(stretches);

            // ASSERT
            assertThat(actualLongest)
                .isNull();
        }
    }
}
