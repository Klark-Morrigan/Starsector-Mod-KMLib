package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.SectorWalkCounters;
import kmlib.starsector.WalkCountCapture;
import kmlib.testfixtures.starsector.colonies.ColonyMarketFixture;
import kmlib.testfixtures.starsector.colonies.ColonyPlacementFixture;

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
 * little else. What counts as a colony in one location belongs to {@link Colonies} and is
 * pinned by its own suite; one case is posed here only to show the selection is inherited from
 * it rather than re-derived.
 *
 * <p>Colonies are built through {@link ColonyMarketFixture}, so a colony posed here is the same
 * shape as one posed against a single location's set. The world is this suite's own: a sector
 * spanning several locations, which the single-system fixture cannot express.
 */
final class SectorColoniesTest {

    @Nested
    class ReadColonies {

        @Test
        void yields_every_star_system_s_colonies_in_the_sector_s_own_order() {

            var sector = new SectorFixture();
            var jangala = ColonyMarketFixture.buildVisibleColony("hegemony");
            var kazeron = ColonyMarketFixture.buildVisibleColony("persean");

            sector.addSystemHolding(jangala);
            sector.addSystemHolding(kazeron);

            assertThat(SectorColonies.readColonies(sector.getSector()))
                .containsExactly(
                    new Colony(jangala, true),
                    new Colony(kazeron, true));
        }

        @Test
        void yields_the_colonies_sitting_in_hyperspace() {
            // Vanilla builds none, but mods put markets out there, and getStarSystems() does not
            // reach them - so a walk that only looped the systems would drop them silently.
            // What hyperspace itself yields is HyperspaceColoniesTest's; this is the composition.
            var sector = new SectorFixture();
            var deepSpaceStation = ColonyMarketFixture.buildVisibleColony("independent");

            sector.setHyperspaceHolding(deepSpaceStation);

            assertThat(SectorColonies.readColonies(sector.getSector()))
                .containsExactly(new Colony(deepSpaceStation, true));
        }

        @Test
        void yields_hyperspace_s_colonies_after_every_system_s() {

            var sector = new SectorFixture();
            var jangala = ColonyMarketFixture.buildVisibleColony("hegemony");
            var deepSpaceStation = ColonyMarketFixture.buildVisibleColony("independent");

            sector.addSystemHolding(jangala);
            sector.setHyperspaceHolding(deepSpaceStation);

            assertThat(SectorColonies.readColonies(sector.getSector()))
                .containsExactly(
                    new Colony(jangala, true),
                    new Colony(deepSpaceStation, true));
        }

        @Test
        void selects_colonies_through_the_colony_set_rather_than_off_the_economy() {
            // The condition-only market a bare planet carries is rejected by the colony set's
            // ownership rule. Its absence here is the tell that the walk goes through the set.
            var sector = new SectorFixture();
            var jangala = ColonyMarketFixture.buildVisibleColony("hegemony");
            var barePlanet = ColonyMarketFixture.buildConditionOnlyMarket();

            sector.addSystemHolding(jangala, barePlanet);

            assertThat(SectorColonies.readColonies(sector.getSector()))
                .containsExactly(new Colony(jangala, true));
        }

        @Test
        void yields_the_systems_colonies_for_a_sector_with_no_hyperspace() {

            var sector = new SectorFixture();
            var jangala = ColonyMarketFixture.buildVisibleColony("hegemony");

            sector.addSystemHolding(jangala);

            assertThat(SectorColonies.readColonies(sector.getSector()))
                .containsExactly(new Colony(jangala, true));
        }

        @Test
        void yields_nothing_for_a_sector_holding_no_colony() {
            assertThat(SectorColonies.readColonies(new SectorFixture().getSector()))
                .isEmpty();
        }

        @Test
        void still_reads_hyperspace_for_a_sector_listing_no_systems() {
            // The two are separate places, so an unreadable system list says nothing about
            // hyperspace - and treating it as "the sector holds nothing" would drop exactly the
            // colonies this read exists to catch.
            var sector = new SectorFixture();
            var deepSpaceStation = ColonyMarketFixture.buildVisibleColony("independent");

            sector.setHyperspaceHolding(deepSpaceStation);
            sector.listNoStarSystems();

            assertThat(SectorColonies.readColonies(sector.getSector()))
                .containsExactly(new Colony(deepSpaceStation, true));
        }

        @Test
        void yields_nothing_for_a_null_sector() {
            assertThat(SectorColonies.readColonies(null))
                .isEmpty();
        }

        @Test
        void counts_one_walk_over_the_systems_and_the_colonies_it_selected() {
            // The sector-wide read walks the system list itself rather than through the system
            // reader, so what it costs is stated here or nowhere. The colonies come from the one
            // selection every set is read through, hyperspace's included.
            var sector = new SectorFixture();

            sector.addSystemHolding(ColonyMarketFixture.buildVisibleColony("hegemony"));
            sector.setHyperspaceHolding(ColonyMarketFixture.buildVisibleColony("independent"));

            var counts = WalkCountCapture.captureCountsOf(
                () -> SectorColonies.readColonies(sector.getSector()));

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(1L);
            assertThat(counts.readCount(SectorWalkCounters.SYSTEMS_VISITED))
                .isEqualTo(1L);
            assertThat(counts.readCount(SectorWalkCounters.COLONIES_READ))
                .isEqualTo(2L);
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

        // A sector that cannot answer for its systems at all - the malformed shape that must not
        // be read as "and therefore holds nothing anywhere".
        private void listNoStarSystems() {

            when(sectorMock.getStarSystems())
                .thenReturn(null);
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
        // the entity each sits on. Both halves, since every case here poses ordinary registered
        // colonies - the listed-versus-unlisted split is the colony set's own suites' business.
        private void placeColoniesIn(LocationAPI location, MarketAPI[] locationColonies) {

            ColonyPlacementFixture.placeColonies(location, locationColonies);
            ColonyPlacementFixture.listColonies(economyMock, location, locationColonies);
        }
    }
}
