package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.testfixtures.starsector.systems.StarSystemFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins {@link SystemMotionTracker}: a selected system that moves across hyperspace
 * between polls is reported, a system the predicate rejects is never tracked, and a
 * null sector observes nothing. The detection state machine itself is pinned by
 * {@link kmlib.math.motion.MotionTracker}'s tests; this covers the sector walk and the
 * predicate scoping the tracker layers on top.
 */
final class SystemMotionTrackerTest {

    @Nested
    class UpdateMovingSystems {

        @Test
        void reportsASelectedSystemThatMovedBetweenPolls() {

            var tracker = new SystemMotionTracker();
            var systemMock = StarSystemFixture.buildSystemAt("mover", 0, 0);
            var sectorMock = StarSystemFixture.buildSectorOf(systemMock);

            tracker.updateMovingSystems(sectorMock, acceptAll());
            StarSystemFixture.placeSystemAt(systemMock, 500, 0);

            var hasChanged = tracker.updateMovingSystems(sectorMock, acceptAll());

            assertThat(hasChanged)
                .isTrue();
            assertThat(tracker.getMovingSystemIds())
                .containsExactly("mover");
        }

        @Test
        void neverTracksASystemThePredicateRejects() {

            var tracker = new SystemMotionTracker();
            var systemMock = StarSystemFixture.buildSystemAt("excluded", 0, 0);
            var sectorMock = StarSystemFixture.buildSectorOf(systemMock);

            tracker.updateMovingSystems(sectorMock, acceptNone());
            StarSystemFixture.placeSystemAt(systemMock, 500, 0);

            var hasChanged = tracker.updateMovingSystems(sectorMock, acceptNone());

            assertThat(hasChanged)
                .isFalse();
            assertThat(tracker.getMovingSystemIds())
                .isEmpty();
        }

        @Test
        void aNullSectorReportsNoChange() {

            var tracker = new SystemMotionTracker();

            assertThat(tracker.updateMovingSystems(null, acceptAll()))
                .isFalse();
        }
    }

    @Nested
    class ClearObservations {

        @Test
        void forgetsAMoverSoTheNextPollJudgesItAfresh() {
            // What a save load calls: the sector is replaced, and a system id carried over from the
            // previous one must not be measured against where it sat in that sector. Cleared, the
            // next poll is a first sighting, which cannot report motion however far the system
            // moved in between.
            var tracker = new SystemMotionTracker();
            var systemMock = StarSystemFixture.buildSystemAt("mover", 0, 0);
            var sectorMock = StarSystemFixture.buildSectorOf(systemMock);

            tracker.updateMovingSystems(sectorMock, acceptAll());
            StarSystemFixture.placeSystemAt(systemMock, 500, 0);
            tracker.updateMovingSystems(sectorMock, acceptAll());
            tracker.clearObservations();

            assertThat(tracker.getMovingSystemIds())
                .isEmpty();

            StarSystemFixture.placeSystemAt(systemMock, 1000, 0);

            assertThat(tracker.updateMovingSystems(sectorMock, acceptAll()))
                .isFalse();
            assertThat(tracker.getMovingSystemIds())
                .isEmpty();
        }
    }

    private static Predicate<StarSystemAPI> acceptAll() {
        return system -> true;
    }

    private static Predicate<StarSystemAPI> acceptNone() {
        return system -> false;
    }
}
