package kmlib.starsector.systems;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contract of {@link SectorColonies#readColonies}. The cases live in a {@link Nested}
 * group so the suite reports as a per-method tree.
 *
 * <p>What is under test here is the walk - which locations it reaches and in what order - and
 * little else. What counts as a colony in one location belongs to {@link SystemColonies} and is
 * pinned by its own suite; one case is posed here only to show the selection is inherited from
 * it rather than re-derived.
 *
 * <p>Colonies are built through {@link SystemColonyFixture}, so a colony posed here is the same
 * shape as one posed against the colony set. The sector is not: this suite needs one spanning
 * several locations, which the single-system fixture cannot express.
 */
final class SectorColoniesTest {

    // Used for its colony builders alone. The system and sector it opens go unread - this suite
    // poses its own sector, and a colony's shape is all that is wanted from here.
    private final SystemColonyFixture colonies = new SystemColonyFixture("unused");

    @Nested
    class ReadColonies {

        @Test
        void yields_every_star_system_s_colonies_in_the_sector_s_own_order() {

            var sector = new SectorFixture();
            var jangala = colonies.buildVisibleColony("hegemony");
            var kazeron = colonies.buildVisibleColony("persean");

            sector.addSystemHolding(jangala);
            sector.addSystemHolding(kazeron);

            assertThat(SectorColonies.readColonies(sector.getSector()))
                .containsExactly(
                    new SystemColony(jangala, true),
                    new SystemColony(kazeron, true));
        }

        @Test
        void yields_the_colonies_sitting_in_hyperspace() {
            // Vanilla builds none, but mods put markets out there, and getStarSystems() does not
            // reach them - so a walk that only looped the systems would drop them silently.
            var sector = new SectorFixture();
            var deepSpaceStation = colonies.buildVisibleColony("independent");

            sector.setHyperspaceHolding(deepSpaceStation);

            assertThat(SectorColonies.readColonies(sector.getSector()))
                .containsExactly(new SystemColony(deepSpaceStation, true));
        }

        @Test
        void yields_hyperspace_s_colonies_after_every_system_s() {

            var sector = new SectorFixture();
            var jangala = colonies.buildVisibleColony("hegemony");
            var deepSpaceStation = colonies.buildVisibleColony("independent");

            sector.addSystemHolding(jangala);
            sector.setHyperspaceHolding(deepSpaceStation);

            assertThat(SectorColonies.readColonies(sector.getSector()))
                .containsExactly(
                    new SystemColony(jangala, true),
                    new SystemColony(deepSpaceStation, true));
        }

        @Test
        void selects_colonies_through_the_colony_set_rather_than_off_the_economy() {
            // The condition-only market a bare planet carries is rejected by the colony set's
            // ownership rule. Its absence here is the tell that the walk goes through the set.
            var sector = new SectorFixture();
            var jangala = colonies.buildVisibleColony("hegemony");
            var barePlanet = colonies.buildConditionOnlyMarket();

            sector.addSystemHolding(jangala, barePlanet);

            assertThat(SectorColonies.readColonies(sector.getSector()))
                .containsExactly(new SystemColony(jangala, true));
        }

        @Test
        void yields_the_systems_colonies_for_a_sector_with_no_hyperspace() {

            var sector = new SectorFixture();
            var jangala = colonies.buildVisibleColony("hegemony");

            sector.addSystemHolding(jangala);

            assertThat(SectorColonies.readColonies(sector.getSector()))
                .containsExactly(new SystemColony(jangala, true));
        }

        @Test
        void yields_nothing_for_a_sector_holding_no_colony() {
            assertThat(SectorColonies.readColonies(new SectorFixture().getSector()))
                .isEmpty();
        }

        @Test
        void yields_nothing_for_a_sector_with_no_star_systems() {

            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getStarSystems())
                .thenReturn(null);

            assertThat(SectorColonies.readColonies(sectorMock))
                .isEmpty();
        }

        @Test
        void yields_nothing_for_a_null_sector() {
            assertThat(SectorColonies.readColonies(null))
                .isEmpty();
        }
    }

    /**
     * A sector spanning as many locations as a case needs: star systems added in the order the
     * sector will list them, and at most one hyperspace.
     *
     * <p>Locations are wired the way the game wires one - the economy lists the colonies, and the
     * location carries the entities they sit on - so both halves of the colony set's walk find
     * them. A location a case never adds is simply absent, which is how the no-hyperspace and
     * empty-sector cases are posed.
     */
    private static final class SectorFixture {

        private final EconomyAPI economyMock = mock(EconomyAPI.class);
        private final SectorAPI sectorMock = mock(SectorAPI.class);

        // Handed to the sector mock once and added to afterwards. Mockito answers the same list
        // instance every call, so a system added later is still listed - which is what lets a
        // case read as "open a sector, then put systems in it".
        private final List<StarSystemAPI> systems = new ArrayList<>();

        private SectorFixture() {

            when(sectorMock.getEconomy())
                .thenReturn(economyMock);
            when(sectorMock.getStarSystems())
                .thenReturn(systems);
        }

        private SectorAPI getSector() {
            return sectorMock;
        }

        private void addSystemHolding(MarketAPI... locationColonies) {

            var systemMock = mock(StarSystemAPI.class);

            placeColoniesIn(systemMock, locationColonies);
            systems.add(systemMock);
        }

        private void setHyperspaceHolding(MarketAPI... locationColonies) {

            var hyperspaceMock = mock(LocationAPI.class);

            placeColoniesIn(hyperspaceMock, locationColonies);

            when(sectorMock.getHyperspace())
                .thenReturn(hyperspaceMock);
        }

        // Sites the colonies in one location: the economy lists them, and the location carries
        // the entity each sits on.
        private void placeColoniesIn(LocationAPI location, MarketAPI[] locationColonies) {

            // The entities are read off the colonies before either stubbing opens, so calling a
            // mock does not land inside a stubbing in progress.
            var entities = new ArrayList<SectorEntityToken>();

            for (var colony : locationColonies) {
                entities.add(colony.getPrimaryEntity());
            }
            when(location.getAllEntities())
                .thenReturn(entities);
            when(economyMock.getMarkets(location))
                .thenReturn(List.of(locationColonies));
        }
    }
}
