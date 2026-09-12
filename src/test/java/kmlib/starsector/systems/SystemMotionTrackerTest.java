package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
            var systemMock = buildSystemAt("mover", 0, 0);
            var sectorMock = buildSectorOf(systemMock);

            tracker.updateMovingSystems(sectorMock, acceptAll());

            when(systemMock.getLocation())
                .thenReturn(new Vector2f(500, 0));

            var hasChanged = tracker.updateMovingSystems(sectorMock, acceptAll());

            assertThat(hasChanged)
                .isTrue();
            assertThat(tracker.getMovingSystemIds())
                .containsExactly("mover");
        }

        @Test
        void neverTracksASystemThePredicateRejects() {

            var tracker = new SystemMotionTracker();
            var systemMock = buildSystemAt("excluded", 0, 0);
            var sectorMock = buildSectorOf(systemMock);

            tracker.updateMovingSystems(sectorMock, acceptNone());

            when(systemMock.getLocation())
                .thenReturn(new Vector2f(500, 0));

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

    private static SectorAPI buildSectorOf(StarSystemAPI system) {

        var sectorMock = mock(SectorAPI.class);

        when(sectorMock.getStarSystems())
            .thenReturn(List.of(system));

        return sectorMock;
    }

    private static StarSystemAPI buildSystemAt(String id, float x, float y) {

        var systemMock = mock(StarSystemAPI.class);

        when(systemMock.getId())
            .thenReturn(id);
        when(systemMock.getLocation())
            .thenReturn(new Vector2f(x, y));
            
        return systemMock;
    }

    private static Predicate<StarSystemAPI> acceptAll() {
        return system -> true;
    }

    private static Predicate<StarSystemAPI> acceptNone() {
        return system -> false;
    }
}
