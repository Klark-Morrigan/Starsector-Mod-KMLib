package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link Entities#readFactionId} and {@link Entities#isDiscoveredByPlayer} -
 * the two facts every kind of thing standing on an entity reads through rather than restating.
 * Each method's cases live in a {@link Nested} group so the suite reports as a per-method tree.
 */
final class EntitiesTest {

    @Nested
    class ReadFactionId {

        @Test
        void reportsTheIdOfTheFactionTheEntityNames() {

            var factionMock = mock(FactionAPI.class);

            when(factionMock.getId())
                .thenReturn("hegemony");

            var entityMock = mock(SectorEntityToken.class);

            when(entityMock.getFaction())
                .thenReturn(factionMock);

            assertThat(Entities.readFactionId(entityMock))
                .isEqualTo("hegemony");
        }

        @Test
        void reportsNoFactionWhereTheEntityNamesNone() {

            assertThat(Entities.readFactionId(mock(SectorEntityToken.class)))
                .isNull();
        }

        @Test
        void reportsNoFactionForAnAbsentEntity() {

            assertThat(Entities.readFactionId(null))
                .isNull();
        }
    }

    @Nested
    class IsDiscoveredByPlayer {

        @Test
        void reportsAFoundEntityAsDiscovered() {
            // An entity stops being discoverable once found, so the answer is the negation of
            // that flag - which is the inversion this exists to state once.
            assertThat(Entities.isDiscoveredByPlayer(mock(SectorEntityToken.class)))
                .isTrue();
        }

        @Test
        void reportsAStillDiscoverableEntityAsUndiscovered() {

            var entityMock = mock(SectorEntityToken.class);

            when(entityMock.isDiscoverable())
                .thenReturn(true);

            assertThat(Entities.isDiscoveredByPlayer(entityMock))
                .isFalse();
        }

        @Test
        void reportsAnAbsentEntityAsDiscovered() {
            // Nothing left to find is nothing to withhold, and a caller holding no entity would
            // otherwise have to decide that for itself.
            assertThat(Entities.isDiscoveredByPlayer(null))
                .isTrue();
        }
    }
}
