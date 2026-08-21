package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;

import kmlib.starsector.markets.MarketPlacementFixture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pins the sighting register kept in sector memory: what an unwritten register answers, what a
 * visit records, which places are worth recording at all, and what a load sheds.
 *
 * <p>The stored map is a real one behind a mocked memory, rather than a stub per key, because
 * every case here is about how the register accumulates across calls - a stubbed read could not
 * show a second visit overwriting the first, nor a reconciliation removing an entry.
 */
final class SectorColonySightingsTest {

    private static final String OTHER_SYSTEM_ID = "corvus";
    private static final String SIGHTINGS_KEY = "$kmlib_colony_sightings";
    private static final String SYSTEM_ID = "kumari_kandam";

    private EconomyAPI economyMock;
    private MemoryAPI memoryMock;
    private SectorAPI sectorMock;
    private StarSystemAPI systemMock;

    @BeforeEach
    void setUp() {

        economyMock = mock(EconomyAPI.class);
        memoryMock = mock(MemoryAPI.class);
        sectorMock = mock(SectorAPI.class);
        systemMock = mock(StarSystemAPI.class);

        when(sectorMock.getEconomy())
            .thenReturn(economyMock);
        when(sectorMock.getMemoryWithoutUpdate())
            .thenReturn(memoryMock);
        when(sectorMock.getStarSystems())
            .thenReturn(List.of(systemMock));
        when(systemMock.getId())
            .thenReturn(SYSTEM_ID);
    }

    @Nested
    class ReadSightings {

        @Test
        void reports_nothing_seen_where_the_register_has_never_been_written() {

            assertThat(SectorColonySightings.readSightings(sectorMock).readSightedLocationId("any"))
                .isNull();
        }

        @Test
        void reports_nothing_seen_where_there_is_no_sector_to_read() {

            assertThat(SectorColonySightings.readSightings(null))
                .isSameAs(ColonySightings.NONE);
        }

        @Test
        void reports_nothing_seen_where_the_key_holds_something_that_is_not_a_register() {
            // Another party writing over the key must cost the sightings and nothing else: a read
            // that threw here would take down every colony set in the sector with it.
            storeSightings("not a register");

            assertThat(SectorColonySightings.readSightings(sectorMock).readSightedLocationId("any"))
                .isNull();
        }

        @Test
        void reports_where_a_recorded_colony_was_seen() {

            var stored = new HashMap<String, String>();

            stored.put("sentinel_gantries", SYSTEM_ID);
            storeSightings(stored);

            assertThat(SectorColonySightings.readSightings(sectorMock)
                    .readSightedLocationId("sentinel_gantries"))
                .isEqualTo(SYSTEM_ID);
        }
    }

    @Nested
    class RecordSightingsIn {

        @Test
        void records_every_colony_standing_in_the_system_visited() {
            // Both listings, since a gated colony is most often the unregistered shape: hung on
            // one of the system's own entities and never entered in the economy.
            var listedColony = buildColony("jangala");
            var unlistedColony = buildColony("galatia_academy");

            listColoniesInSystem(listedColony);
            placeColoniesOnSystemEntities(unlistedColony);
            openStoredSightings();

            SectorColonySightings.recordSightingsIn(sectorMock, systemMock);

            assertThat(readStoredSightings())
                .containsEntry("jangala", SYSTEM_ID)
                .containsEntry("galatia_academy", SYSTEM_ID);
        }

        @Test
        void moves_a_colonys_sighting_to_wherever_it_was_last_seen() {
            // The mover's own case, from the register's side: meeting a colony again names the
            // new place rather than adding to a list of places it has ever been.
            var mover = buildColony("rat_exoship");
            var stored = openStoredSightings();

            stored.put("rat_exoship", OTHER_SYSTEM_ID);
            listColoniesInSystem(mover);

            SectorColonySightings.recordSightingsIn(sectorMock, systemMock);

            assertThat(readStoredSightings())
                .containsEntry("rat_exoship", SYSTEM_ID);
        }

        @Test
        void records_nothing_for_a_place_that_is_not_a_star_system() {
            // Hyperspace, which is where the sector's largest entity list lives and where a colony
            // reads sighted whatever the register says. Walking it would buy nothing at all.
            SectorColonySightings.recordSightingsIn(sectorMock, mock(LocationAPI.class));

            verify(memoryMock, never())
                .set(anyString(), any());
        }

        @Test
        void records_nothing_where_there_is_no_memory_to_write_into() {

            when(sectorMock.getMemoryWithoutUpdate())
                .thenReturn(null);

            SectorColonySightings.recordSightingsIn(sectorMock, systemMock);

            verifyNoInteractions(memoryMock);
        }
    }

    @Nested
    class DropSightingsOfAbsentColonies {

        @Test
        void drops_a_sighting_of_a_colony_no_longer_anywhere_in_the_sector() {
            // A sighting outliving what it was about would go on answering for whatever next took
            // the id, which is a sighting the player never made.
            var survivor = buildColony("jangala");
            var stored = openStoredSightings();

            stored.put("jangala", SYSTEM_ID);
            stored.put("razed_base", SYSTEM_ID);
            listColoniesInSystem(survivor);

            SectorColonySightings.dropSightingsOfAbsentColonies(sectorMock);

            assertThat(readStoredSightings())
                .containsOnlyKeys("jangala");
        }

        @Test
        void keeps_a_sighting_of_a_colony_that_has_moved_to_another_system() {
            // Present but elsewhere is not absent. The sighting stays and simply stops matching
            // where the colony stands, which is the rule's own way of saying it is unseen again.
            var mover = buildColony("rat_exoship");
            var stored = openStoredSightings();

            stored.put("rat_exoship", OTHER_SYSTEM_ID);
            listColoniesInSystem(mover);

            SectorColonySightings.dropSightingsOfAbsentColonies(sectorMock);

            assertThat(readStoredSightings())
                .containsEntry("rat_exoship", OTHER_SYSTEM_ID);
        }

        @Test
        void leaves_an_unwritten_register_alone() {

            SectorColonySightings.dropSightingsOfAbsentColonies(sectorMock);

            verify(memoryMock, never())
                .set(anyString(), any());
        }
    }

    @Nested
    class ReconcileWithLoadedSave {

        @Test
        void records_the_colonies_where_the_save_was_left_and_sheds_the_ones_that_have_gone() {
            // What a load owes the register. No location change fires until the player leaves, so
            // without this the place they are looking at is the one place nothing is known about -
            // and on a save written before any sighting was made, that is all it could learn.
            var survivor = buildColony("jangala");
            var stored = openStoredSightings();

            stored.put("razed_base", SYSTEM_ID);
            listColoniesInSystem(survivor);

            when(sectorMock.getCurrentLocation())
                .thenReturn(systemMock);

            SectorColonySightings.reconcileWithLoadedSave(sectorMock);

            assertThat(readStoredSightings())
                .containsOnlyKeys("jangala")
                .containsEntry("jangala", SYSTEM_ID);
        }

        @Test
        void records_nothing_where_the_save_was_left_outside_a_star_system() {

            var colony = buildColony("jangala");

            openStoredSightings();
            listColoniesInSystem(colony);

            when(sectorMock.getCurrentLocation())
                .thenReturn(mock(LocationAPI.class));

            SectorColonySightings.reconcileWithLoadedSave(sectorMock);

            assertThat(readStoredSightings())
                .isEmpty();
        }
    }

    // A market on a body of its own, named by the id a sighting is kept against.
    private static MarketAPI buildColony(String colonyId) {

        var marketMock = MarketPlacementFixture.buildMarketOnBody(colonyId + "_body");

        when(marketMock.getId())
            .thenReturn(colonyId);

        return marketMock;
    }

    private void listColoniesInSystem(MarketAPI... colonies) {
        MarketPlacementFixture.listMarketsIn(economyMock, systemMock, colonies);
    }

    private void placeColoniesOnSystemEntities(MarketAPI... colonies) {
        MarketPlacementFixture.placeMarketsIn(systemMock, colonies);
    }

    // Opens the register the way a first sighting would, so a case can seed it and then assert
    // against the very map the code under test writes into.
    private Map<String, String> openStoredSightings() {

        var stored = new HashMap<String, String>();

        storeSightings(stored);

        return stored;
    }

    private void storeSightings(Object storedValue) {

        when(memoryMock.contains(SIGHTINGS_KEY))
            .thenReturn(true);
        when(memoryMock.get(SIGHTINGS_KEY))
            .thenReturn(storedValue);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> readStoredSightings() {
        return (Map<String, String>) memoryMock.get(SIGHTINGS_KEY);
    }
}
