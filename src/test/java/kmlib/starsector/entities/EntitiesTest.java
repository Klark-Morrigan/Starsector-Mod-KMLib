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
        void reports_the_id_of_the_faction_the_entity_names() {

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
        void reports_no_faction_where_the_entity_names_none() {

            assertThat(Entities.readFactionId(mock(SectorEntityToken.class)))
                .isNull();
        }

        @Test
        void reports_no_faction_for_an_absent_entity() {

            assertThat(Entities.readFactionId(null))
                .isNull();
        }
    }

    @Nested
    class IsDiscoveredByPlayer {

        @Test
        void reports_a_found_entity_as_discovered() {
            // An entity stops being discoverable once found, so the answer is the negation of
            // that flag - which is the inversion this exists to state once.
            assertThat(Entities.isDiscoveredByPlayer(mock(SectorEntityToken.class)))
                .isTrue();
        }

        @Test
        void reports_a_still_discoverable_entity_as_undiscovered() {

            var entityMock = mock(SectorEntityToken.class);

            when(entityMock.isDiscoverable())
                .thenReturn(true);

            assertThat(Entities.isDiscoveredByPlayer(entityMock))
                .isFalse();
        }

        @Test
        void reports_an_absent_entity_as_discovered() {
            // Nothing left to find is nothing to withhold, and a caller holding no entity would
            // otherwise have to decide that for itself.
            assertThat(Entities.isDiscoveredByPlayer(null))
                .isTrue();
        }
    }
}
