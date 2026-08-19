package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
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
 * Pins the contract of {@link HyperspaceColonies#readColonies}. The cases live in a
 * {@link Nested} group so the suite reports as a per-method tree.
 *
 * <p>This is where the shared selection is pinned as reaching somewhere other than a star system.
 * The rule itself is {@link LocationColonies}' and is exercised exhaustively through
 * {@link Colonies}; what has to be shown here is that hyperspace is read on the same terms
 * - the off-economy colony in particular, that being the likeliest shape for a mod to have built
 * out here.
 *
 * <p>Colonies come from {@link ColonyMarketFixture}, so one posed here is the same shape as one
 * posed in a system.
 */
final class HyperspaceColoniesTest {

    @Nested
    class ReadColonies {

        @Test
        void yields_the_colonies_the_economy_places_in_hyperspace() {

            var deepSpaceStation = ColonyMarketFixture.buildVisibleColony("independent");
            var sector = buildSectorWhoseHyperspaceHolds(List.of(deepSpaceStation), List.of());

            assertThat(HyperspaceColonies.readColonies(sector).colonies())
                .containsExactly(new Colony(deepSpaceStation, ColonyKind.COLONY, true));
        }

        @Test
        void yields_a_hyperspace_colony_the_economy_does_not_list() {
            // The off-economy shape reaches hyperspace too, and is the likelier one out here: a
            // mod hanging a station in deep space has no reason to register it with the economy.
            var deepSpaceStation = ColonyMarketFixture.buildVisibleColony("independent");
            var sector = buildSectorWhoseHyperspaceHolds(List.of(), List.of(deepSpaceStation));

            assertThat(HyperspaceColonies.readColonies(sector).colonies())
                .containsExactly(new Colony(deepSpaceStation, ColonyKind.COLONY, false));
        }

        @Test
        void drops_a_condition_only_market_sitting_in_hyperspace() {
            // The ownership rule is the shared one, so it rejects here exactly as in a system.
            var barePlanet = ColonyMarketFixture.buildConditionOnlyMarket();
            var sector = buildSectorWhoseHyperspaceHolds(List.of(barePlanet), List.of());

            assertThat(HyperspaceColonies.readColonies(sector).colonies())
                .isEmpty();
        }

        @Test
        void yields_nothing_for_a_hyperspace_holding_no_colony() {

            var sector = buildSectorWhoseHyperspaceHolds(List.of(), List.of());

            assertThat(HyperspaceColonies.readColonies(sector))
                .isEqualTo(Colonies.NONE);
        }

        @Test
        void yields_nothing_for_a_sector_with_no_hyperspace() {
            // What a sector reads as before it is built. Nothing to read is not a fault.
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getHyperspace())
                .thenReturn(null);

            assertThat(HyperspaceColonies.readColonies(sectorMock).colonies())
                .isEmpty();
        }

        @Test
        void yields_nothing_for_a_null_sector() {
            assertThat(HyperspaceColonies.readColonies(null))
                .isEqualTo(Colonies.NONE);
        }
    }

    // A sector whose hyperspace holds these colonies: the listed ones registered with the
    // economy, the unlisted ones only hung on entities out there - the split the colony set
    // reports back through Colony.isListedByEconomy.
    private static SectorAPI buildSectorWhoseHyperspaceHolds(
            List<MarketAPI> listedColonies,
            List<MarketAPI> unlistedColonies) {

        var presentColonies = new ArrayList<MarketAPI>(listedColonies);
        presentColonies.addAll(unlistedColonies);

        var hyperspaceMock = mock(LocationAPI.class);
        var economyMock = mock(EconomyAPI.class);

        ColonyPlacementFixture.placeColonies(
            hyperspaceMock, presentColonies.toArray(new MarketAPI[0]));
        ColonyPlacementFixture.listColonies(
            economyMock, hyperspaceMock, listedColonies.toArray(new MarketAPI[0]));

        var sectorMock = mock(SectorAPI.class);

        when(sectorMock.getEconomy())
            .thenReturn(economyMock);
        when(sectorMock.getHyperspace())
            .thenReturn(hyperspaceMock);

        return sectorMock;
    }
}
