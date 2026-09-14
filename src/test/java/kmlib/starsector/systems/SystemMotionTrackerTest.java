package kmlib.starsector.systems;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link SystemMotionTracker}: a system that moves across hyperspace between polls is
 * reported under its key, two systems sharing an ID are told apart, an empty poll stops every
 * mover, and a cleared tracker judges the next poll afresh. The detection state machine itself is
 * pinned by {@link kmlib.math.motion.MotionTracker}'s tests; this covers the binding to
 * {@link SystemKey} the tracker layers on top.
 */
final class SystemMotionTrackerTest {

    // The keys the posed systems carry, written out rather than read off a system: an expectation
    // taken from the code under test is no expectation at all.
    private static final SystemKey MOVER = new SystemKey("mover", "mover_star", "893");

    // Vanilla's own deep space pair: one ID, told apart by their anchors alone.
    private static final SystemKey FIRST_DEEP_SPACE = new SystemKey("deep space", "", "8b3");
    private static final SystemKey SECOND_DEEP_SPACE = new SystemKey("deep space", "", "38d53");

    @Nested
    class UpdateMovingSystems {

        @Test
        void reportsASystemThatMovedBetweenPolls() {

            var tracker = new SystemMotionTracker();

            tracker.updateMovingSystems(Map.of(MOVER, buildPoint(0, 0)));

            var hasChanged = tracker.updateMovingSystems(Map.of(MOVER, buildPoint(500, 0)));

            assertThat(hasChanged)
                .isTrue();
            assertThat(tracker.getMovingSystemKeys())
                .containsExactly(MOVER);
        }

        @Test
        void keepsTwoSystemsSharingAnIdApart() {
            // The pair a live sector holds co-located under one id. Observed by ID they would be
            // one entry, and the move below would read as a move by whichever the sector lists
            // last; observed by key the mover is reported and the other is not.
            var tracker = new SystemMotionTracker();

            tracker.updateMovingSystems(Map.of(
                FIRST_DEEP_SPACE, buildPoint(0, 0),
                SECOND_DEEP_SPACE, buildPoint(0, 0)));

            var hasChanged = tracker.updateMovingSystems(Map.of(
                FIRST_DEEP_SPACE, buildPoint(500, 0),
                SECOND_DEEP_SPACE, buildPoint(0, 0)));

            assertThat(hasChanged)
                .isTrue();
            assertThat(tracker.getMovingSystemKeys())
                .containsExactly(FIRST_DEEP_SPACE);
        }

        @Test
        void anEmptyPollReportsEveryMoverAsStopped() {
            // An empty map is an empty sector observed, not a poll that saw nothing: every system
            // that had been moving is no longer observed, so the moving set empties and the change
            // is reported.
            var tracker = new SystemMotionTracker();

            tracker.updateMovingSystems(Map.of(MOVER, buildPoint(0, 0)));
            tracker.updateMovingSystems(Map.of(MOVER, buildPoint(500, 0)));

            assertThat(tracker.updateMovingSystems(Map.of()))
                .isTrue();
            assertThat(tracker.getMovingSystemKeys())
                .isEmpty();
        }
    }

    @Nested
    class ClearObservations {

        @Test
        void forgetsAMoverSoTheNextPollJudgesItAfresh() {
            // What a save load calls: the sector is replaced, and a system key carried over from
            // the previous one must not be measured against where it sat in that sector. Cleared,
            // the next poll is a first sighting, which cannot report motion however far the system
            // moved in between.
            var tracker = new SystemMotionTracker();

            tracker.updateMovingSystems(Map.of(MOVER, buildPoint(0, 0)));
            tracker.updateMovingSystems(Map.of(MOVER, buildPoint(500, 0)));
            tracker.clearObservations();

            assertThat(tracker.getMovingSystemKeys())
                .isEmpty();

            assertThat(tracker.updateMovingSystems(Map.of(MOVER, buildPoint(1000, 0))))
                .isFalse();
            assertThat(tracker.getMovingSystemKeys())
                .isEmpty();
        }
    }

    private static double[] buildPoint(double x, double y) {
        return new double[] {x, y};
    }
}
