package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.SectorWalkCounters;
import kmlib.starsector.WalkCountCapture;
import kmlib.testfixtures.starsector.markets.colonies.ColonyMarketFixture;
import kmlib.testfixtures.starsector.markets.colonies.ColonyPlacementFixture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

final class SectorMarketsTests {

    private static MarketAPI createNamedMarket(String name) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getName())
            .thenReturn(name);

        return marketMock;
    }

    private static SectorAPI createSectorListing(List<MarketAPI> markets) {

        var sectorMock = mock(SectorAPI.class);
        var economyMock = mock(EconomyAPI.class);

        when(sectorMock.getEconomy())
            .thenReturn(economyMock);
        when(economyMock.getMarketsCopy())
            .thenReturn(markets);

        return sectorMock;
    }

    @Nested
    class ListMarketNames {

        @Test
        void readsEveryNamedMarketLeavingBlanksOut() {

            var markets = List.of(createNamedMarket("Jangala"), createNamedMarket(""), createNamedMarket("Sindria"));

            assertThat(SectorMarkets.listMarketNames(createSectorListing(markets)))
                .containsExactly("Jangala", "Sindria");
        }

        @Test
        void answersNothingForASectorWithNoEconomyYet() {
            assertThat(SectorMarkets.listMarketNames(mock(SectorAPI.class)))
                .isEmpty();
        }

        @Test
        void answersNothingForANullSector() {
            assertThat(SectorMarkets.listMarketNames(null))
                .isEmpty();
        }

        @Test
        void countsEveryMarketItReadOnTheOpenSection() {

            var markets = List.of(createNamedMarket("Jangala"), createNamedMarket("Sindria"));
            var sectorMock = createSectorListing(markets);

            var counts = WalkCountCapture.captureCountsOf(() -> SectorMarkets.listMarketNames(sectorMock));

            assertThat(counts.readCount(SectorWalkCounters.MARKETS_READ))
                .isEqualTo(2L);
        }
    }

    /**
     * Every case poses a market one of the ways the sector can hold one - listed by the economy,
     * hung on an entity it never registered, standing in hyperspace - because what the walk is for
     * is that no listing is forgotten. A suite posing only economy-listed markets would read the
     * same against a walk that found half of them.
     *
     * <p>What a colony <em>is</em> does not matter here, so the shapes are whatever
     * {@link ColonyMarketFixture} builds most plainly: the walk asks what the sector holds rather
     * than what may be shown of it.
     */
    @Nested
    class ReadMarketIds {

        private static final String HYPERSPACE_MARKET_ID = "abyssal_station";
        private static final String LISTED_MARKET_ID = "jangala";
        private static final String UNLISTED_MARKET_ID = "sentinel_gantries";

        private EconomyAPI economyMock;
        private LocationAPI hyperspaceMock;
        private SectorAPI sectorMock;
        private StarSystemAPI systemMock;

        @BeforeEach
        void setUp() {

            economyMock = mock(EconomyAPI.class);
            hyperspaceMock = mock(LocationAPI.class);
            sectorMock = mock(SectorAPI.class);
            systemMock = mock(StarSystemAPI.class);

            when(sectorMock.getEconomy())
                .thenReturn(economyMock);
            when(sectorMock.getHyperspace())
                .thenReturn(hyperspaceMock);
            when(sectorMock.getStarSystems())
                .thenReturn(List.of(systemMock));
        }

        @Test
        void readsAMarketTheEconomyLists() {

            ColonyPlacementFixture.listColonies(
                economyMock,
                systemMock,
                nameMarket(ColonyMarketFixture.buildVisibleColony("hegemony"), LISTED_MARKET_ID));

            assertThat(SectorMarkets.readMarketIds(sectorMock))
                .containsExactly("jangala");
        }

        @Test
        void readsAMarketHungOnAnEntityTheEconomyNeverRegistered() {
            // A derelict station the economy has never heard of, which a walk over the economy
            // alone would answer as gone.
            ColonyPlacementFixture.hangColoniesOnEntitiesIn(
                systemMock,
                nameMarket(ColonyMarketFixture.buildDerelictStation(), UNLISTED_MARKET_ID));

            assertThat(SectorMarkets.readMarketIds(sectorMock))
                .containsExactly("sentinel_gantries");
        }

        @Test
        void readsAMarketStandingInHyperspace() {

            ColonyPlacementFixture.hangColoniesOnEntitiesIn(
                hyperspaceMock,
                nameMarket(ColonyMarketFixture.buildDerelictStation(), HYPERSPACE_MARKET_ID));

            assertThat(SectorMarkets.readMarketIds(sectorMock))
                .containsExactly("abyssal_station");
        }

        @Test
        void readsEveryMarketOnceWhereBothListingsHoldIt() {
            // The economy and the entity walk overlap on a registered market, and an ID counted
            // twice would say nothing different - so the answer is a set rather than a tally.
            var market = nameMarket(
                ColonyMarketFixture.buildVisibleColony("hegemony"),
                LISTED_MARKET_ID);

            ColonyPlacementFixture.listColonies(economyMock, systemMock, market);
            ColonyPlacementFixture.hangColoniesOnEntitiesIn(systemMock, market);

            assertThat(SectorMarkets.readMarketIds(sectorMock))
                .containsExactly("jangala");
        }

        @Test
        void readsNothingOfAMarketTheGameNamesWithNothing() {
            // A record is filed under the very ID this walk is compared against, so a market with no
            // ID can hold none - and putting a null into the answer would only match by accident.
            ColonyPlacementFixture.hangColoniesOnEntitiesIn(
                systemMock,
                nameMarket(ColonyMarketFixture.buildDerelictStation(), null));

            assertThat(SectorMarkets.readMarketIds(sectorMock))
                .isEmpty();
        }

        @Test
        void readsHyperspaceWhereTheSectorListsNoStarSystemAtAll() {
            // A sector mid-teardown answers nothing for its systems. Faulting there would take down
            // a load, and skipping hyperspace with it would answer every market there as gone.
            when(sectorMock.getStarSystems())
                .thenReturn(null);

            ColonyPlacementFixture.hangColoniesOnEntitiesIn(
                hyperspaceMock,
                nameMarket(ColonyMarketFixture.buildDerelictStation(), HYPERSPACE_MARKET_ID));

            assertThat(SectorMarkets.readMarketIds(sectorMock))
                .containsExactly("abyssal_station");
        }

        @Test
        void readsNothingWhereThereIsNoSectorToWalk() {
            assertThat(SectorMarkets.readMarketIds(null))
                .isEmpty();
        }

        @Test
        void countsOneWalkOverTheSystemList() {

            var counts = WalkCountCapture.captureCountsOf(() -> SectorMarkets.readMarketIds(sectorMock));

            assertThat(counts.readCount(SectorWalkCounters.SECTOR_WALKS))
                .isEqualTo(1L);
        }

        @Test
        void readsEachPlacesEconomyListingOnce() {
            // Both listings come off one paired read per place, so a system holding one listed and
            // one unlisted market costs two markets read, not the listed one twice over.
            var listed = nameMarket(ColonyMarketFixture.buildVisibleColony("hegemony"), LISTED_MARKET_ID);
            var unlisted = nameMarket(ColonyMarketFixture.buildDerelictStation(), UNLISTED_MARKET_ID);

            ColonyPlacementFixture.listColonies(economyMock, systemMock, listed);
            ColonyPlacementFixture.hangColoniesOnEntitiesIn(systemMock, listed, unlisted);

            var counts = WalkCountCapture.captureCountsOf(() -> SectorMarkets.readMarketIds(sectorMock));

            assertThat(counts.readCount(SectorWalkCounters.MARKETS_READ))
                .isEqualTo(2L);
        }
    }

    // The ID a record is kept against. Given here rather than by the colony builders, none of which
    // needs one.
    private static MarketAPI nameMarket(MarketAPI market, String marketId) {

        when(market.getId())
            .thenReturn(marketId);

        return market;
    }
}
