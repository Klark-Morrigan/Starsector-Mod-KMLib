package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link MarketColocation#isSamePlaceAndOwner} and
 * {@link MarketColocation#readLargestMarketsPerFaction}. Each method's cases live in a
 * {@link Nested} group so the suite reports as a per-method tree; the shared mock builders
 * stay on the outer class.
 *
 * <p>Every case here poses a market by the two facts the identity is built from - the entity
 * it sits on and the faction holding it - so the builder takes those and a size, and nothing
 * else about a market is stubbed. A case that had to say whether its market was hidden would
 * be claiming the identity read consults something it does not.
 */
final class MarketColocationTest {

    @Nested
    class IsSamePlaceAndOwner {

        @Test
        void reports_two_markets_on_one_entity_under_one_owner_as_one_place() {
            // The shape a merged listing has to collapse: IndEvo hangs its own Galatia Academy
            // market on the station that already carries vanilla's, both independent-owned.
            var station = buildPlaceEntity();
            var independent = buildFaction("independent");

            assertThat(MarketColocation.isSamePlaceAndOwner(
                    buildMarketAtPlace(station, independent, 3),
                    buildMarketAtPlace(station, independent, 5)))
                .isTrue();
        }

        @Test
        void reports_a_market_as_its_own_place() {

            var market = buildMarketAtPlace(
                buildPlaceEntity(), buildFaction("hegemony"), 4);

            assertThat(MarketColocation.isSamePlaceAndOwner(market, market))
                .isTrue();
        }

        @Test
        void reports_a_market_with_no_entity_as_its_own_place() {
            // Identity settles it before the key is ever built, so a market that answers for
            // nothing but itself still answers for itself.
            var market = buildMarketAtPlace(null, buildFaction("hegemony"), 4);

            assertThat(MarketColocation.isSamePlaceAndOwner(market, market))
                .isTrue();
        }

        @Test
        void reports_two_markets_on_one_entity_under_different_owners_as_two_places() {

            var station = buildPlaceEntity();

            assertThat(MarketColocation.isSamePlaceAndOwner(
                    buildMarketAtPlace(station, buildFaction("hegemony"), 3),
                    buildMarketAtPlace(station, buildFaction("pirates"), 5)))
                .isFalse();
        }

        @Test
        void reports_two_markets_of_one_owner_on_separate_entities_as_two_places() {

            var independent = buildFaction("independent");

            assertThat(MarketColocation.isSamePlaceAndOwner(
                    buildMarketAtPlace(buildPlaceEntity(), independent, 3),
                    buildMarketAtPlace(buildPlaceEntity(), independent, 5)))
                .isFalse();
        }

        @Test
        void reports_two_distinct_markets_with_no_entity_as_two_places() {
            // A missing key is not a key two markets can share, so each answers for itself alone.
            var independent = buildFaction("independent");

            assertThat(MarketColocation.isSamePlaceAndOwner(
                    buildMarketAtPlace(null, independent, 3),
                    buildMarketAtPlace(null, independent, 5)))
                .isFalse();
        }

        @Test
        void reports_two_distinct_markets_with_no_owner_as_two_places() {

            var station = buildPlaceEntity();

            assertThat(MarketColocation.isSamePlaceAndOwner(
                    buildMarketAtPlace(station, null, 3),
                    buildMarketAtPlace(station, null, 5)))
                .isFalse();
        }

        @Test
        void reports_a_null_market_as_the_same_place_as_nothing() {

            var market = buildMarketAtPlace(
                buildPlaceEntity(), buildFaction("hegemony"), 4);

            assertThat(MarketColocation.isSamePlaceAndOwner(null, market))
                .isFalse();
            assertThat(MarketColocation.isSamePlaceAndOwner(market, null))
                .isFalse();
            assertThat(MarketColocation.isSamePlaceAndOwner(null, null))
                .isFalse();
        }
    }

    @Nested
    class ReadLargestMarketsPerFaction {

        @Test
        void keeps_only_the_larger_of_two_markets_sharing_an_entity_and_owner() {

            var station = buildPlaceEntity();
            var independent = buildFaction("independent");
            var vanillaAcademy = buildMarketAtPlace(station, independent, 3);
            var moddedAcademy = buildMarketAtPlace(station, independent, 5);

            assertThat(MarketColocation.readLargestMarketsPerFaction(
                    List.of(vanillaAcademy, moddedAcademy)))
                .containsExactly(moddedAcademy);
        }

        @Test
        void keeps_a_market_of_each_owner_when_one_entity_carries_two() {

            var station = buildPlaceEntity();
            var hegemony = buildMarketAtPlace(station, buildFaction("hegemony"), 3);
            var pirates = buildMarketAtPlace(station, buildFaction("pirates"), 5);

            assertThat(MarketColocation.readLargestMarketsPerFaction(List.of(hegemony, pirates)))
                .containsExactly(hegemony, pirates);
        }

        @Test
        void keeps_both_markets_of_one_owner_on_separate_entities() {

            var independent = buildFaction("independent");
            var academy = buildMarketAtPlace(buildPlaceEntity(), independent, 3);
            var ancyra = buildMarketAtPlace(buildPlaceEntity(), independent, 5);

            assertThat(MarketColocation.readLargestMarketsPerFaction(List.of(academy, ancyra)))
                .containsExactly(academy, ancyra);
        }

        @Test
        void keeps_the_first_of_two_equal_sized_markets_sharing_a_place() {

            var station = buildPlaceEntity();
            var independent = buildFaction("independent");
            var first = buildMarketAtPlace(station, independent, 3);
            var second = buildMarketAtPlace(station, independent, 3);

            assertThat(MarketColocation.readLargestMarketsPerFaction(List.of(first, second)))
                .containsExactly(first);
        }

        @Test
        void yields_a_place_where_it_was_first_named_though_its_winner_arrives_later() {

            var station = buildPlaceEntity();
            var independent = buildFaction("independent");
            var small = buildMarketAtPlace(station, independent, 3);
            var elsewhere = buildMarketAtPlace(buildPlaceEntity(), independent, 4);
            var large = buildMarketAtPlace(station, independent, 6);

            assertThat(MarketColocation.readLargestMarketsPerFaction(
                    List.of(small, elsewhere, large)))
                .containsExactly(large, elsewhere);
        }

        @Test
        void passes_through_markets_of_one_owner_that_have_no_entity() {

            var independent = buildFaction("independent");
            var first = buildMarketAtPlace(null, independent, 3);
            var second = buildMarketAtPlace(null, independent, 5);

            assertThat(MarketColocation.readLargestMarketsPerFaction(List.of(first, second)))
                .containsExactly(first, second);
        }

        @Test
        void passes_through_markets_on_one_entity_that_have_no_owner() {

            var station = buildPlaceEntity();
            var first = buildMarketAtPlace(station, null, 3);
            var second = buildMarketAtPlace(station, null, 5);

            assertThat(MarketColocation.readLargestMarketsPerFaction(List.of(first, second)))
                .containsExactly(first, second);
        }

        @Test
        void passes_through_markets_on_one_entity_whose_owner_has_no_id() {
            // An owner with no id cannot be told apart from any other, so keying on it
            // would merge places that share nothing but an unreadable faction.
            var station = buildPlaceEntity();
            var unnamedOwner = buildFaction(null);
            var first = buildMarketAtPlace(station, unnamedOwner, 3);
            var second = buildMarketAtPlace(station, unnamedOwner, 5);

            assertThat(MarketColocation.readLargestMarketsPerFaction(List.of(first, second)))
                .containsExactly(first, second);
        }

        @Test
        void drops_null_markets() {

            var market = buildMarketAtPlace(
                buildPlaceEntity(), buildFaction("hegemony"), 4);

            assertThat(MarketColocation.readLargestMarketsPerFaction(
                    Arrays.asList(null, market, null)))
                .containsExactly(market);
        }

        @Test
        void yields_an_empty_list_for_no_markets() {
            assertThat(MarketColocation.readLargestMarketsPerFaction(List.of()))
                .isEmpty();
        }

        @Test
        void yields_an_empty_list_for_a_null_collection() {
            assertThat(MarketColocation.readLargestMarketsPerFaction(null))
                .isEmpty();
        }
    }

    // A market sited on a place under an owner, at a size. The three facts the identity is built
    // from and the tie is broken on, and the only ones any case here has business posing.
    private static MarketAPI buildMarketAtPlace(
            SectorEntityToken entity,
            FactionAPI faction,
            int size) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getPrimaryEntity())
            .thenReturn(entity);
        when(marketMock.getFaction())
            .thenReturn(faction);
        when(marketMock.getSize())
            .thenReturn(size);

        return marketMock;
    }

    // A bare entity standing for one place. Nothing is stubbed on it: the identity compares
    // entities by their own equality, which for a mock is identity, so two of these are two
    // places for exactly the reason two vanilla stations are.
    private static SectorEntityToken buildPlaceEntity() {
        return mock(SectorEntityToken.class);
    }

    private static FactionAPI buildFaction(String id) {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getId())
            .thenReturn(id);

        return factionMock;
    }
}
