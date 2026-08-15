package kmlib.math.geometry;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link RingStretch#computeLength}: how far along a path a stretch reaches, including
 * the stretch that closes past the perimeter.
 *
 * <p>The second case is the one worth stating. A stretch crossing the path's start is carried
 * on into the next lap rather than being stated as a start after its end, so its length is
 * plain subtraction like any other - and a caller comparing stretches gets the run's real
 * length rather than one wrapped back within a lap.
 */
final class RingStretchTest {

    @Nested
    class ComputeLength {

        @Test
        void length_is_the_distance_between_the_two_ends() {
            assertThat(new RingStretch(4, 10).computeLength())
                .isEqualTo(6.0);
        }

        @Test
        void stretch_closing_past_the_perimeter_measures_its_whole_run() {
            // A stretch fused across a 24-long path's start: from 20 round through the origin to
            // 8 on the next lap, which is 12 of path rather than the 12 back to the start or a
            // negative reading of the two ends.
            assertThat(new RingStretch(20, 32).computeLength())
                .isEqualTo(12.0);
        }
    }
}
