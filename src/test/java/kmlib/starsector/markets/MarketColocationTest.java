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
        void reportsTwoMarketsOnOneEntityUnderOneOwnerAsOnePlace() {
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
        void reportsAMarketAsItsOwnPlace() {

            var market = buildMarketAtPlace(
                buildPlaceEntity(), buildFaction("hegemony"), 4);

            assertThat(MarketColocation.isSamePlaceAndOwner(market, market))
                .isTrue();
        }

        @Test
        void reportsAMarketWithNoEntityAsItsOwnPlace() {
            // Identity settles it before the key is ever built, so a market that answers for
            // nothing but itself still answers for itself.
            var market = buildMarketAtPlace(null, buildFaction("hegemony"), 4);

            assertThat(MarketColocation.isSamePlaceAndOwner(market, market))
                .isTrue();
        }

        @Test
        void reportsTwoMarketsOnOneEntityUnderDifferentOwnersAsTwoPlaces() {

            var station = buildPlaceEntity();

            assertThat(MarketColocation.isSamePlaceAndOwner(
                    buildMarketAtPlace(station, buildFaction("hegemony"), 3),
                    buildMarketAtPlace(station, buildFaction("pirates"), 5)))
                .isFalse();
        }

        @Test
        void reportsTwoMarketsOfOneOwnerOnSeparateEntitiesAsTwoPlaces() {

            var independent = buildFaction("independent");

            assertThat(MarketColocation.isSamePlaceAndOwner(
                    buildMarketAtPlace(buildPlaceEntity(), independent, 3),
                    buildMarketAtPlace(buildPlaceEntity(), independent, 5)))
                .isFalse();
        }

        @Test
        void reportsTwoDistinctMarketsWithNoEntityAsTwoPlaces() {
            // A missing key is not a key two markets can share, so each answers for itself alone.
            var independent = buildFaction("independent");

            assertThat(MarketColocation.isSamePlaceAndOwner(
                    buildMarketAtPlace(null, independent, 3),
                    buildMarketAtPlace(null, independent, 5)))
                .isFalse();
        }

        @Test
        void reportsTwoDistinctMarketsWithNoOwnerAsTwoPlaces() {

            var station = buildPlaceEntity();

            assertThat(MarketColocation.isSamePlaceAndOwner(
                    buildMarketAtPlace(station, null, 3),
                    buildMarketAtPlace(station, null, 5)))
                .isFalse();
        }

        @Test
        void reportsANullMarketAsTheSamePlaceAsNothing() {

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
        void keepsOnlyTheLargerOfTwoMarketsSharingAnEntityAndOwner() {

            var station = buildPlaceEntity();
            var independent = buildFaction("independent");
            var vanillaAcademy = buildMarketAtPlace(station, independent, 3);
            var moddedAcademy = buildMarketAtPlace(station, independent, 5);

            assertThat(MarketColocation.readLargestMarketsPerFaction(
                    List.of(vanillaAcademy, moddedAcademy)))
                .containsExactly(moddedAcademy);
        }

        @Test
        void keepsAMarketOfEachOwnerWhenOneEntityCarriesTwo() {

            var station = buildPlaceEntity();
            var hegemony = buildMarketAtPlace(station, buildFaction("hegemony"), 3);
            var pirates = buildMarketAtPlace(station, buildFaction("pirates"), 5);

            assertThat(MarketColocation.readLargestMarketsPerFaction(List.of(hegemony, pirates)))
                .containsExactly(hegemony, pirates);
        }

        @Test
        void keepsBothMarketsOfOneOwnerOnSeparateEntities() {

            var independent = buildFaction("independent");
            var academy = buildMarketAtPlace(buildPlaceEntity(), independent, 3);
            var ancyra = buildMarketAtPlace(buildPlaceEntity(), independent, 5);

            assertThat(MarketColocation.readLargestMarketsPerFaction(List.of(academy, ancyra)))
                .containsExactly(academy, ancyra);
        }

        @Test
        void keepsTheFirstOfTwoEqualSizedMarketsSharingAPlace() {

            var station = buildPlaceEntity();
            var independent = buildFaction("independent");
            var first = buildMarketAtPlace(station, independent, 3);
            var second = buildMarketAtPlace(station, independent, 3);

            assertThat(MarketColocation.readLargestMarketsPerFaction(List.of(first, second)))
                .containsExactly(first);
        }

        @Test
        void yieldsAPlaceWhereItWasFirstNamedThoughItsWinnerArrivesLater() {

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
        void passesThroughMarketsOfOneOwnerThatHaveNoEntity() {

            var independent = buildFaction("independent");
            var first = buildMarketAtPlace(null, independent, 3);
            var second = buildMarketAtPlace(null, independent, 5);

            assertThat(MarketColocation.readLargestMarketsPerFaction(List.of(first, second)))
                .containsExactly(first, second);
        }

        @Test
        void passesThroughMarketsOnOneEntityThatHaveNoOwner() {

            var station = buildPlaceEntity();
            var first = buildMarketAtPlace(station, null, 3);
            var second = buildMarketAtPlace(station, null, 5);

            assertThat(MarketColocation.readLargestMarketsPerFaction(List.of(first, second)))
                .containsExactly(first, second);
        }

        @Test
        void passesThroughMarketsOnOneEntityWhoseOwnerHasNoId() {
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
        void dropsNullMarkets() {

            var market = buildMarketAtPlace(
                buildPlaceEntity(), buildFaction("hegemony"), 4);

            assertThat(MarketColocation.readLargestMarketsPerFaction(
                    Arrays.asList(null, market, null)))
                .containsExactly(market);
        }

        @Test
        void yieldsAnEmptyListForNoMarkets() {
            assertThat(MarketColocation.readLargestMarketsPerFaction(List.of()))
                .isEmpty();
        }

        @Test
        void yieldsAnEmptyListForANullCollection() {
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
