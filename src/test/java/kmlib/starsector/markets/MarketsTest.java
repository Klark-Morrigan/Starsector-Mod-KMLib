package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contract of {@link Markets#hasAttachedStation}. The cases live in a
 * {@link Nested} group so the suite reports as a per-method tree; the shared mock
 * builders stay on the outer class.
 */
final class MarketsTest {

    @Nested
    class HasAttachedStation {
        @Test
        void returns_true_for_a_market_that_owns_a_station() {
            var market = buildMarketConnectedTo(stationEntity());

            assertThat(Markets.hasAttachedStation(market)).isTrue();
        }

        @Test
        void returns_false_for_a_market_with_no_connected_entities() {
            var market = buildMarketConnectedTo();

            assertThat(Markets.hasAttachedStation(market)).isFalse();
        }

        @Test
        void returns_false_when_no_connected_entity_is_a_station() {
            var market = buildMarketConnectedTo(nonStationEntity());

            assertThat(Markets.hasAttachedStation(market)).isFalse();
        }

        @Test
        void ignores_a_station_tagged_no_orbital_station() {
            var market = buildMarketConnectedTo(optedOutStationEntity());

            assertThat(Markets.hasAttachedStation(market)).isFalse();
        }

        @Test
        void returns_false_for_a_null_market() {
            assertThat(Markets.hasAttachedStation(null)).isFalse();
        }

        @Test
        void returns_false_for_null_connected_entities() {
            var marketMock = mock(MarketAPI.class);
            when(marketMock.getConnectedEntities()).thenReturn(null);

            assertThat(Markets.hasAttachedStation(marketMock)).isFalse();
        }
    }

    private static MarketAPI buildMarketConnectedTo(SectorEntityToken... entities) {
        var marketMock = mock(MarketAPI.class);
        when(marketMock.getConnectedEntities()).thenReturn(Set.of(entities));
        return marketMock;
    }

    // A station entity: carries the "station" tag and no opt-out.
    private static SectorEntityToken stationEntity() {
        var entityMock = mock(SectorEntityToken.class);
        when(entityMock.hasTag(Tags.STATION)).thenReturn(true);
        return entityMock;
    }

    // A "station"-tagged entity flagged NO_ORBITAL_STATION, vanilla's own opt-out.
    private static SectorEntityToken optedOutStationEntity() {
        var entityMock = stationEntity();
        when(entityMock.hasTag("NO_ORBITAL_STATION")).thenReturn(true);
        return entityMock;
    }

    // A connected entity that is not a station (e.g. the market's planet).
    private static SectorEntityToken nonStationEntity() {
        return mock(SectorEntityToken.class);
    }
}
