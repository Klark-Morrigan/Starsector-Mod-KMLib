package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.EconomyAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contract of {@link LocationMarkets#findNearestMarket}, the search itself, at the
 * layer that owns it. The cases live in a {@link Nested} group so the suite reports as a
 * per-method tree; the shared wiring stays on the outer class.
 *
 * <p>Posed against a plain {@link LocationAPI} rather than a star system, which is the point of
 * the search sitting here: nothing about ranking markets by distance is system-specific, and a
 * caller searching hyperspace gets the same answers. What the star-system surface adds - that a
 * system is searched by a read accepting nothing else - is
 * {@code StarSystemsTest.FindNearestMarket}'s.
 *
 * <p>The markets themselves come from {@link MarketPlacementFixture}, so a market posed here is
 * the same shape as one posed against a system.
 */
final class LocationMarketsTest {

    @Nested
    class FindNearestMarket {

        @Test
        void returns_the_admitted_market_whose_body_sits_closest() {

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
        void passes_over_a_closer_market_the_eligibility_rejects() {
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
        void reaches_a_market_the_economy_does_not_list() {
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
        void ranks_an_unlisted_market_against_a_listed_one_by_distance_alone() {
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
        void settles_an_equal_distance_on_the_lowest_body_id() {
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
        void ignores_a_market_standing_for_no_body() {
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
        void ignores_a_market_whose_body_sits_nowhere() {
            // A body sitting nowhere is the same case as no body at all: there is no distance to
            // take, and reading one off it would be reading off a null.
            var locationMock = mock(LocationAPI.class);
            var sector = buildSectorListing(
                locationMock,
                MarketPlacementFixture.buildMarketOnPlacelessBody("adrift"));

            assertThat(LocationMarkets.findNearestMarket(
                    sector,
                    locationMock,
                    MarketPlacementFixture.buildFleetAt(0, 0),
                    market -> true))
                .isEmpty();
        }

        @Test
        void returns_empty_when_nothing_present_is_admitted() {

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
        void returns_empty_for_an_empty_location() {

            var locationMock = mock(LocationAPI.class);

            assertThat(LocationMarkets.findNearestMarket(
                    buildSectorListing(locationMock),
                    locationMock,
                    MarketPlacementFixture.buildFleetAt(0, 0),
                    market -> true))
                .isEmpty();
        }

        @Test
        void returns_empty_without_anything_to_measure_from() {

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
        void returns_empty_when_what_it_measures_from_sits_nowhere() {
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
        void returns_empty_without_an_eligibility_rule() {
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
        void returns_empty_for_a_null_sector() {
            assertThat(LocationMarkets.findNearestMarket(
                    null,
                    mock(LocationAPI.class),
                    MarketPlacementFixture.buildFleetAt(0, 0),
                    market -> true))
                .isEmpty();
        }

        @Test
        void returns_empty_for_a_null_location() {
            assertThat(LocationMarkets.findNearestMarket(
                    mock(SectorAPI.class),
                    null,
                    MarketPlacementFixture.buildFleetAt(0, 0),
                    market -> true))
                .isEmpty();
        }

        @Test
        void returns_empty_when_the_sector_has_no_economy() {

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
