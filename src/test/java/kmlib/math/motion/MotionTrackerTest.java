package kmlib.math.motion;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link MotionTracker}'s detection: a first sighting cannot be judged moving; a
 * key that holds still is not moving; one whose position shifted past the floor is,
 * while a shift under the floor reads as noise; a key that keeps moving stays in the
 * set without re-reporting a change; one that stops leaves it; and a key that drops
 * out and returns is judged afresh rather than against its stale position. Drives the
 * detection core directly so the state machine is pinned without a sector.
 */
final class MotionTrackerTest {

    @Nested
    class Observe {

        @Test
        void a_first_sighting_is_not_yet_moving() {
            var tracker = new MotionTracker(1.0);

            // No baseline to compare against, so nothing is moving and the set (still
            // empty) has not changed.
            var hasChanged = tracker.observe(Map.of("a", buildPoint(0, 0)));

            assertThat(hasChanged).isFalse();
            assertThat(tracker.getMovingKeys()).isEmpty();
        }

        @Test
        void a_stationary_key_is_not_moving() {
            var tracker = new MotionTracker(1.0);
            tracker.observe(Map.of("a", buildPoint(0, 0)));

            var hasChanged = tracker.observe(Map.of("a", buildPoint(0, 0)));

            assertThat(hasChanged).isFalse();
            assertThat(tracker.getMovingKeys()).isEmpty();
        }

        @Test
        void a_key_that_shifted_past_the_floor_is_moving() {
            var tracker = new MotionTracker(1.0);
            tracker.observe(Map.of("a", buildPoint(0, 0)));

            var hasChanged = tracker.observe(Map.of("a", buildPoint(500, 0)));

            assertThat(hasChanged).isTrue();
            assertThat(tracker.getMovingKeys()).containsExactly("a");
        }

        @Test
        void a_shift_under_the_floor_reads_as_noise() {
            var tracker = new MotionTracker(1.0);
            tracker.observe(Map.of("a", buildPoint(0, 0)));

            // Half a unit, under the one-unit floor: jitter, not motion.
            var hasChanged = tracker.observe(Map.of("a", buildPoint(0.5, 0)));

            assertThat(hasChanged).isFalse();
            assertThat(tracker.getMovingKeys()).isEmpty();
        }

        @Test
        void a_key_that_keeps_moving_stays_in_the_set_without_reporting_a_change() {
            var tracker = new MotionTracker(1.0);
            tracker.observe(Map.of("a", buildPoint(0, 0)));
            tracker.observe(Map.of("a", buildPoint(500, 0)));

            // Still moving, to a fresh point: already in the set, so it is unchanged
            // and no transition is reported.
            var hasChanged = tracker.observe(Map.of("a", buildPoint(1200, 0)));

            assertThat(hasChanged).isFalse();
            assertThat(tracker.getMovingKeys()).containsExactly("a");
        }

        @Test
        void a_key_that_stops_leaves_the_moving_set() {
            var tracker = new MotionTracker(1.0);
            tracker.observe(Map.of("a", buildPoint(0, 0)));
            tracker.observe(Map.of("a", buildPoint(500, 0)));

            // Holds at its last point: no longer moving, so the set changes back to
            // empty.
            var hasChanged = tracker.observe(Map.of("a", buildPoint(500, 0)));

            assertThat(hasChanged).isTrue();
            assertThat(tracker.getMovingKeys()).isEmpty();
        }

        @Test
        void a_key_that_returns_is_judged_afresh_rather_than_against_a_stale_position() {
            var tracker = new MotionTracker(1.0);
            tracker.observe(Map.of("a", buildPoint(0, 0), "b", buildPoint(0, 0)));
            // "b" drops out of the observation, so its baseline is pruned.
            tracker.observe(Map.of("a", buildPoint(0, 0)));

            // "b" returns far from where it last was; without pruning that gap would
            // read as motion, but a returning key is a first sighting again, so it is
            // not moving.
            var hasChanged = tracker.observe(Map.of("a", buildPoint(0, 0), "b", buildPoint(5000, 0)));

            assertThat(hasChanged).isFalse();
            assertThat(tracker.getMovingKeys()).isEmpty();
        }

        @Test
        void clearing_observations_makes_the_next_sighting_a_first_sighting() {
            var tracker = new MotionTracker(1.0);
            tracker.observe(Map.of("a", buildPoint(0, 0)));

            tracker.clearObservations();
            // With the baseline dropped, the same key at a far point is a first
            // sighting again, so it is not moving.
            var hasChanged = tracker.observe(Map.of("a", buildPoint(5000, 0)));

            assertThat(hasChanged).isFalse();
            assertThat(tracker.getMovingKeys()).isEmpty();
        }
    }

    private static double[] buildPoint(double x, double y) {
        return new double[] {x, y};
    }
}
