package kmlib.starsector.entities;

import com.fs.starfarer.api.campaign.CustomCampaignEntityAPI;
import com.fs.starfarer.api.campaign.LocationAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contract of {@link LocationMarketlessEntities#readMarketlessEntitiesIn} - the search
 * itself, at the layer that owns it. The one method's cases live in a {@link Nested} group so the
 * suite reports as a per-method tree; the shared builders stay on the outer class.
 *
 * <p>Posed against a plain {@link LocationAPI} rather than a star system, which is the point of
 * the search sitting here: nothing about finding what stands in a place is system-specific, and a
 * caller asking about hyperspace gets the same answers.
 */
final class LocationMarketlessEntitiesTest {

    @Nested
    class ReadMarketlessEntitiesIn {

        @Test
        void yields_every_market_less_entity_and_leaves_the_colonys_out() {
            // The market is the whole of the exclusion, and it is the only one: the relay and the
            // cargo pod come back beside the derelict station, because which of them counts as a
            // place worth listing is the caller's judgement rather than this search's.
            var locationMock = mock(LocationAPI.class);
            var colonyStationMock = buildCustomEntity("station_side03");

            when(colonyStationMock.getMarket())
                .thenReturn(mock(MarketAPI.class));

            var stationMock = buildCustomEntity("station_research");
            var relayMock = buildCustomEntity("comm_relay");
            var cargoPodMock = buildCustomEntity("cargo_pods");

            when(locationMock.getCustomEntities())
                .thenReturn(List.of(colonyStationMock, stationMock, relayMock, cargoPodMock));

            assertThat(readTypesOf(
                    LocationMarketlessEntities.readMarketlessEntitiesIn(locationMock)))
                .containsExactly("station_research", "comm_relay", "cargo_pods");
        }

        @Test
        void yields_an_undiscovered_entity_like_any_other() {
            // Nothing is filtered here: discovery is a fact a caller reads off the record and
            // applies to its own purpose, and a search that withheld one would leave a caller
            // recording observations unable to see what it was meant to be recording.
            var locationMock = mock(LocationAPI.class);
            var entityMock = buildCustomEntity("station_research");

            when(entityMock.isDiscoverable())
                .thenReturn(true);

            when(locationMock.getCustomEntities())
                .thenReturn(List.of(entityMock));

            assertThat(LocationMarketlessEntities.readMarketlessEntitiesIn(locationMock))
                .containsExactly(new MarketlessEntity(entityMock));
        }

        @Test
        void skips_an_entity_the_location_lists_as_nothing() {
            // Nothing to read facts off, and the record refuses one - so the listing is filtered
            // here rather than letting one bad entry take out the whole read.
            var locationMock = mock(LocationAPI.class);
            var entityMock = buildCustomEntity("station_research");

            when(locationMock.getCustomEntities())
                .thenReturn(Arrays.asList(entityMock, null));

            assertThat(LocationMarketlessEntities.readMarketlessEntitiesIn(locationMock))
                .containsExactly(new MarketlessEntity(entityMock));
        }

        @Test
        void returns_empty_for_a_location_holding_no_custom_entities() {
            // Empty rather than null: a place with nothing standing in it is an ordinary answer,
            // and every caller walking one would otherwise guard against it separately.
            var locationMock = mock(LocationAPI.class);

            when(locationMock.getCustomEntities())
                .thenReturn(List.of());

            assertThat(LocationMarketlessEntities.readMarketlessEntitiesIn(locationMock))
                .isEmpty();
        }

        @Test
        void returns_empty_for_a_null_location() {
            assertThat(LocationMarketlessEntities.readMarketlessEntitiesIn(null))
                .isEmpty();
        }

        @Test
        void returns_empty_when_the_location_reports_no_custom_entity_list() {

            var locationMock = mock(LocationAPI.class);

            when(locationMock.getCustomEntities())
                .thenReturn(null);

            assertThat(LocationMarketlessEntities.readMarketlessEntitiesIn(locationMock))
                .isEmpty();
        }
    }

    // What each found entity says it is, which is what an assertion about the selection reads: the
    // entity type is what identifies one kind from another.
    private static List<String> readTypesOf(List<MarketlessEntity> marketlessEntities) {

        var types = new ArrayList<String>();

        for (var marketlessEntity : marketlessEntities) {
            types.add(marketlessEntity.readTypeId());
        }
        return types;
    }

    // An entity answering to the type that names its kind, carrying no market. Nothing else is
    // wired: what the search decides is which entities come back, and every fact about one is the
    // record's own.
    private static CustomCampaignEntityAPI buildCustomEntity(String entityType) {

        var entityMock = mock(CustomCampaignEntityAPI.class);

        when(entityMock.getCustomEntityType())
            .thenReturn(entityType);

        return entityMock;
    }
}
