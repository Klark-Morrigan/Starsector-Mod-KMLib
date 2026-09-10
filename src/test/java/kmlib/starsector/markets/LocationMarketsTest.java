package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.SectorWalkCounters;
import kmlib.starsector.WalkCountCapture;
import kmlib.testfixtures.starsector.markets.MarketPlacementFixture;
import kmlib.testfixtures.starsector.markets.MarketStateFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link LocationMarkets#readMarkets},
 * {@link LocationMarkets#readMarketsUnlistedByEconomy} and
 * {@link LocationMarkets#findNearestMarket} - the searches themselves, at the layer that owns
 * them. Each method's cases live in a {@link Nested} group so the suite reports as a per-method
 * tree; the shared wiring stays on the outer class.
 *
 * <p>Posed against a plain {@link LocationAPI} rather than a star system, which is the point of
 * the searches sitting here: nothing about reading or ranking a place's markets is
 * system-specific, and a caller asking about hyperspace gets the same answers. What the
 * star-system readers add - that a system is asked by reads accepting nothing else - is
 * {@code StarSystemsTest}'s, in groups holding one case each.
 *
 * <p>The markets themselves come from {@link MarketPlacementFixture} and
 * {@link MarketStateFixture}, so a market posed here is the same shape as one posed anywhere
 * else.
 */
final class LocationMarketsTest {

    @Nested
    class ReadMarkets {

        @Test
        void returnsTheLocationsMarketsInEconomyOrder() {
            // Order is the economy's, and the listing unfiltered: a caller mirroring vanilla's
            // tie rule resolves on which market comes first, so the traversal must not reorder,
            // and a bare planet's placeholder comes back beside a colony rather than being
            // weeded out by a read that was only asked what is present.
            var colony = MarketStateFixture.buildColony("hegemony");
            var placeholder = MarketStateFixture.buildColonisableBody();
            var locationMock = mock(LocationAPI.class);

            assertThat(LocationMarkets.readMarkets(
                    buildSectorListing(locationMock, colony, placeholder),
                    locationMock))
                .containsExactly(colony, placeholder);
        }

        @Test
        void returnsEmptyForALocationWithNoMarkets() {

            var locationMock = mock(LocationAPI.class);

            assertThat(LocationMarkets.readMarkets(buildSectorListing(locationMock), locationMock))
                .isEmpty();
        }

        @Test
        void returnsEmptyForANullSector() {
            assertThat(LocationMarkets.readMarkets(null, mock(LocationAPI.class)))
                .isEmpty();
        }

        @Test
        void returnsEmptyForANullLocation() {
            assertThat(LocationMarkets.readMarkets(mock(SectorAPI.class), null))
                .isEmpty();
        }

        @Test
        void returnsEmptyWhenTheSectorHasNoEconomy() {

            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getEconomy())
                .thenReturn(null);

            assertThat(LocationMarkets.readMarkets(sectorMock, mock(LocationAPI.class)))
                .isEmpty();
        }

        @Test
        void returnsEmptyWhenTheEconomyReportsNoMarketList() {

            var economyMock = mock(EconomyAPI.class);
            var locationMock = mock(LocationAPI.class);

            when(economyMock.getMarkets(locationMock))
                .thenReturn(null);

            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getEconomy())
                .thenReturn(economyMock);

            assertThat(LocationMarkets.readMarkets(sectorMock, locationMock))
                .isEmpty();
        }

        @Test
        void countsTheListingOnEveryReadOfIt() {
            // Two reads of one place are two reads, whoever made them: the row that asked states
            // both, which is what makes a place read twice for one answer visible at all.
            var colony = MarketStateFixture.buildColony("hegemony");
            var locationMock = mock(LocationAPI.class);
            var sector = buildSectorListing(locationMock, colony);

            var counts = WalkCountCapture.captureCountsOf(() -> {
                LocationMarkets.readMarkets(sector, locationMock);
                LocationMarkets.readMarkets(sector, locationMock);
            });

            assertThat(counts.readCount(SectorWalkCounters.MARKETS_READ))
                .isEqualTo(2L);
        }
    }

    @Nested
    class ReadMarketsUnlistedByEconomy {

        @Test
        void yieldsTheMarketTheEconomyDoesNotListAndNotTheOnesItDoes() {
            // Vanilla builds Galatia Academy as a real market on a real station and deliberately
            // never registers it, so a read of the economy alone reports the station as nobody's.
            // Only that market comes back: a caller wanting the listed ones has already read
            // them, and handing them over again would leave it comparing the two lists to tell
            // them apart.
            var listed = MarketPlacementFixture.buildMarketOnBody("ancyra");
            var academy = MarketPlacementFixture.buildMarketOnBody("academy_station");
            var locationMock = mock(LocationAPI.class);
            var sector = buildSectorListing(locationMock, listed);

            MarketPlacementFixture.placeMarketsIn(locationMock, listed, academy);

            assertThat(LocationMarkets.readMarketsUnlistedByEconomy(sector, locationMock))
                .containsExactly(academy);
        }

        @Test
        void yieldsNothingForALocationTheEconomyListsWhole() {
            // The ordinary system: every market on a body is one the economy already hands over,
            // so the read that exists to find what it left out finds nothing to add.
            var first = MarketPlacementFixture.buildMarketOnBody("ancyra");
            var second = MarketPlacementFixture.buildMarketOnBody("tibicena");
            var locationMock = mock(LocationAPI.class);
            var sector = buildSectorListing(locationMock, first, second);

            MarketPlacementFixture.placeMarketsIn(locationMock, first, second);

            assertThat(LocationMarkets.readMarketsUnlistedByEconomy(sector, locationMock))
                .isEmpty();
        }

        @Test
        void yieldsUnlistedMarketsInEntityOrder() {

            var first = MarketPlacementFixture.buildMarketOnBody("first");
            var second = MarketPlacementFixture.buildMarketOnBody("second");
            var locationMock = mock(LocationAPI.class);
            var sector = buildSectorListing(locationMock);

            MarketPlacementFixture.placeMarketsIn(locationMock, first, second);

            assertThat(LocationMarkets.readMarketsUnlistedByEconomy(sector, locationMock))
                .containsExactly(first, second);
        }

        @Test
        void yieldsOneUnlistedMarketOnceThoughTwoBodiesCarryIt() {
            // Vanilla hangs a station's market on the station and on what it orbits alike, so
            // one colony can be reached twice down the entity walk.
            var academy = MarketPlacementFixture.buildMarketOnBody("academy_station");
            var locationMock = mock(LocationAPI.class);
            var sector = buildSectorListing(locationMock);

            MarketPlacementFixture.placeMarketsIn(locationMock, academy, academy);

            assertThat(LocationMarkets.readMarketsUnlistedByEconomy(sector, locationMock))
                .containsExactly(academy);
        }

        @Test
        void resolvesTwoMarketsOnOneBodyUnderOneOwnerToOne() {
            // A mod supersedes a market by adding rather than replacing, so the station ends up
            // carrying two market objects for the one place - counted twice, it would read as
            // two holdings where the player sees one.
            var listed = MarketPlacementFixture.buildOwnedMarketOnBody("station", "independent");

            MarketPlacementFixture.buildSupplementaryMarketOn(listed);

            var locationMock = mock(LocationAPI.class);
            var sector = buildSectorListing(locationMock, listed);

            MarketPlacementFixture.placeMarketsIn(locationMock, listed);

            assertThat(LocationMarkets.readMarketsUnlistedByEconomy(sector, locationMock))
                .isEmpty();
        }

        @Test
        void ignoresABodyCarryingNoMarket() {

            var locationMock = mock(LocationAPI.class);
            var sector = buildSectorListing(
                locationMock,
                MarketPlacementFixture.buildMarketOnBody("ancyra"));

            // The bare body finishes its own stubbing before the location's opens, so the two
            // do not nest into an unfinished-stubbing error.
            var gate = MarketPlacementFixture.buildBodyAt("corvus_gate", 0, 0);

            when(locationMock.getAllEntities())
                .thenReturn(List.of(gate));

            assertThat(LocationMarkets.readMarketsUnlistedByEconomy(sector, locationMock))
                .isEmpty();
        }

        @Test
        void returnsEmptyForANullSector() {
            assertThat(LocationMarkets.readMarketsUnlistedByEconomy(null, mock(LocationAPI.class)))
                .isEmpty();
        }

        @Test
        void returnsEmptyForANullLocation() {
            assertThat(LocationMarkets.readMarketsUnlistedByEconomy(mock(SectorAPI.class), null))
                .isEmpty();
        }

        @Test
        void returnsEmptyWhenTheSectorHasNoEconomy() {
            // With no listing to compare against there is no telling a listed market from an
            // unlisted one, so the read reports nothing rather than every market it can reach.
            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getEconomy())
                .thenReturn(null);

            assertThat(LocationMarkets.readMarketsUnlistedByEconomy(
                    sectorMock,
                    mock(LocationAPI.class)))
                .isEmpty();
        }

        @Test
        void countsTheEntitiesItScannedBesideTheMarketsItProduced() {
            // The entities are what this read costs and the markets are what it yielded, and
            // neither answers the other: a place with one unlisted colony among many bodies is a
            // cheap answer to state and an expensive one to find.
            var listed = MarketPlacementFixture.buildMarketOnBody("ancyra");
            var academy = MarketPlacementFixture.buildMarketOnBody("academy_station");
            var locationMock = mock(LocationAPI.class);
            var sector = buildSectorListing(locationMock, listed);

            MarketPlacementFixture.placeMarketsIn(locationMock, listed, academy);

            var counts = WalkCountCapture.captureCountsOf(
                () -> LocationMarkets.readMarketsUnlistedByEconomy(sector, locationMock));

            assertThat(counts.readCount(SectorWalkCounters.ENTITIES_VISITED))
                .isEqualTo(2L);
            // The listing this read makes of its own, plus the one unlisted market it found.
            assertThat(counts.readCount(SectorWalkCounters.MARKETS_READ))
                .isEqualTo(2L);
        }
    }

    @Nested
    class FindNearestMarket {

        @Test
        void returnsTheAdmittedMarketWhoseBodySitsClosest() {

            var near = MarketPlacementFixture.buildMarketOnBodyAt("near", 100, 0);
            var far = MarketPlacementFixture.buildMarketOnBodyAt("far", 5000, 0);
            var locationMock = mock(LocationAPI.class);

            // Listed far-first, so the pick is shown to come from distance and not from the
            // order the economy happened to list them in.
            var sector = buildSectorListing(locationMock, far, near);

            assertThat(LocationMarkets.findNearestMarket(
                    sector,
                    locationMock,
                    MarketPlacementFixture.buildFleetAt(0, 0),
                    market -> true))
                .contains(near);
        }

        @Test
        void passesOverACloserMarketTheEligibilityRejects() {
            // What a caller is looking for decides, not proximity alone: the nearest place is
            // routinely the wrong kind of place, which is the whole reason for the predicate.
            var wanted = MarketPlacementFixture.buildMarketOnBodyAt("wanted", 500, 0);
            var unwanted = MarketPlacementFixture.buildMarketOnBodyAt("unwanted", 100, 0);
            var locationMock = mock(LocationAPI.class);
            var sector = buildSectorListing(locationMock, unwanted, wanted);

            assertThat(LocationMarkets.findNearestMarket(
                    sector,
                    locationMock,
                    MarketPlacementFixture.buildFleetAt(0, 0),
                    market -> market == wanted))
                .contains(wanted);
        }

        @Test
        void reachesAMarketTheEconomyDoesNotList() {
            // A body still carrying only survey data is never registered, so a search of the
            // economy's own listing could not find one to colonise at all.
            var surveyData = MarketPlacementFixture.buildMarketOnBodyAt("planet", 100, 0);
            var locationMock = mock(LocationAPI.class);
            var sector = buildSectorListing(locationMock);

            MarketPlacementFixture.placeMarketsIn(locationMock, surveyData);

            assertThat(LocationMarkets.findNearestMarket(
                    sector,
                    locationMock,
                    MarketPlacementFixture.buildFleetAt(0, 0),
                    market -> true))
                .contains(surveyData);
        }

        @Test
        void ranksAnUnlistedMarketAgainstAListedOneByDistanceAlone() {
            // Which listing a market came from is not part of the answer: a colony the economy
            // never registered is as much the nearest place as one it did.
            var listed = MarketPlacementFixture.buildMarketOnBodyAt("listed", 5000, 0);
            var unlisted = MarketPlacementFixture.buildMarketOnBodyAt("unlisted", 100, 0);
            var locationMock = mock(LocationAPI.class);
            var sector = buildSectorListing(locationMock, listed);

            MarketPlacementFixture.placeMarketsIn(locationMock, unlisted);

            assertThat(LocationMarkets.findNearestMarket(
                    sector,
                    locationMock,
                    MarketPlacementFixture.buildFleetAt(0, 0),
                    market -> true))
                .contains(unlisted);
        }

        @Test
        void settlesAnEqualDistanceOnTheLowestBodyId() {
            // Listed high-id first, so the lower id is shown to be the deterministic pick rather
            // than whichever the traversal happened to meet first.
            var beta = MarketPlacementFixture.buildMarketOnBodyAt("beta", 0, 100);
            var alpha = MarketPlacementFixture.buildMarketOnBodyAt("alpha", 0, -100);
            var locationMock = mock(LocationAPI.class);
            var sector = buildSectorListing(locationMock, beta, alpha);

            assertThat(LocationMarkets.findNearestMarket(
                    sector,
                    locationMock,
                    MarketPlacementFixture.buildFleetAt(0, 0),
                    market -> true))
                .contains(alpha);
        }

        @Test
        void ignoresAMarketStandingForNoBody() {
            // Nothing to measure to, so it cannot be nearest - only nearest by default, which
            // would have a caller act on a place nobody pointed at.
            var locationMock = mock(LocationAPI.class);
            var sector = buildSectorListing(locationMock, mock(MarketAPI.class));

            assertThat(LocationMarkets.findNearestMarket(
                    sector,
                    locationMock,
                    MarketPlacementFixture.buildFleetAt(0, 0),
                    market -> true))
                .isEmpty();
        }

        @Test
        void ignoresAMarketWhoseBodySitsNowhere() {
            // A body sitting nowhere is the same case as no body at all: there is no distance to
            // take, and reading one off it would be reading off a null.
            var locationMock = mock(LocationAPI.class);
            var sector = buildSectorListing(
                locationMock,
                MarketPlacementFixture.buildMarketOnBody("adrift"));

            assertThat(LocationMarkets.findNearestMarket(
                    sector,
                    locationMock,
                    MarketPlacementFixture.buildFleetAt(0, 0),
                    market -> true))
                .isEmpty();
        }

        @Test
        void returnsEmptyWhenNothingPresentIsAdmitted() {

            var locationMock = mock(LocationAPI.class);
            var sector = buildSectorListing(
                locationMock,
                MarketPlacementFixture.buildMarketOnBodyAt("planet", 100, 0));

            assertThat(LocationMarkets.findNearestMarket(
                    sector,
                    locationMock,
                    MarketPlacementFixture.buildFleetAt(0, 0),
                    market -> false))
                .isEmpty();
        }

        @Test
        void returnsEmptyForAnEmptyLocation() {

            var locationMock = mock(LocationAPI.class);

            assertThat(LocationMarkets.findNearestMarket(
                    buildSectorListing(locationMock),
                    locationMock,
                    MarketPlacementFixture.buildFleetAt(0, 0),
                    market -> true))
                .isEmpty();
        }

        @Test
        void returnsEmptyWithoutAnythingToMeasureFrom() {

            var locationMock = mock(LocationAPI.class);
            var sector = buildSectorListing(
                locationMock,
                MarketPlacementFixture.buildMarketOnBodyAt("planet", 100, 0));

            assertThat(LocationMarkets.findNearestMarket(
                    sector,
                    locationMock,
                    null,
                    market -> true))
                .isEmpty();
        }

        @Test
        void returnsEmptyWhenWhatItMeasuresFromSitsNowhere() {
            // An entity sitting nowhere is as unmeasurable as none at all, and taking a
            // distance to it would be reading off a null rather than answering "nearest".
            var locationMock = mock(LocationAPI.class);
            var sector = buildSectorListing(
                locationMock,
                MarketPlacementFixture.buildMarketOnBodyAt("planet", 100, 0));

            var placelessFleetMock = mock(SectorEntityToken.class);

            assertThat(LocationMarkets.findNearestMarket(
                    sector,
                    locationMock,
                    placelessFleetMock,
                    market -> true))
                .isEmpty();
        }

        @Test
        void returnsEmptyWithoutAnEligibilityRule() {
            // No rule is not "every market": a caller that failed to say what it wants must not
            // be handed the nearest place of any kind to act on.
            var locationMock = mock(LocationAPI.class);
            var sector = buildSectorListing(
                locationMock,
                MarketPlacementFixture.buildMarketOnBodyAt("planet", 100, 0));

            assertThat(LocationMarkets.findNearestMarket(
                    sector,
                    locationMock,
                    MarketPlacementFixture.buildFleetAt(0, 0),
                    null))
                .isEmpty();
        }

        @Test
        void returnsEmptyForANullSector() {
            assertThat(LocationMarkets.findNearestMarket(
                    null,
                    mock(LocationAPI.class),
                    MarketPlacementFixture.buildFleetAt(0, 0),
                    market -> true))
                .isEmpty();
        }

        @Test
        void returnsEmptyForANullLocation() {
            assertThat(LocationMarkets.findNearestMarket(
                    mock(SectorAPI.class),
                    null,
                    MarketPlacementFixture.buildFleetAt(0, 0),
                    market -> true))
                .isEmpty();
        }

        @Test
        void returnsEmptyWhenTheSectorHasNoEconomy() {

            var sectorMock = mock(SectorAPI.class);

            when(sectorMock.getEconomy())
                .thenReturn(null);

            assertThat(LocationMarkets.findNearestMarket(
                    sectorMock,
                    mock(LocationAPI.class),
                    MarketPlacementFixture.buildFleetAt(0, 0),
                    market -> true))
                .isEmpty();
        }
    }

    // A sector whose economy lists the given markets in that location, the half of what is
    // present that a search reaches through the economy.
    private static SectorAPI buildSectorListing(LocationAPI locationMock, MarketAPI... markets) {

        // The economy finishes its own stubbing before the sector's opens, so the two do not
        // nest into an unfinished-stubbing error.
        var economyMock = mock(EconomyAPI.class);

        MarketPlacementFixture.listMarketsIn(economyMock, locationMock, markets);

        var sectorMock = mock(SectorAPI.class);

        when(sectorMock.getEconomy())
            .thenReturn(economyMock);

        return sectorMock;
    }
}
