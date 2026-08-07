package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Tags;

import kmlib.starsector.testing.StarsectorSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link Markets#isOwnedColony},
 * {@link Markets#findAttachedStation}, {@link Markets#hasAttachedStation},
 * {@link Markets#getStabilityFraction},
 * {@link Markets#isKnownToPlayer}, {@link Markets#isCountedAsColony},
 * {@link Markets#isDiscoveredByPlayer}, {@link Markets#isFoundColony},
 * {@link Markets#readLargestMarketsPerFaction} and
 * {@link Markets#isMilitary}. The cases live in a {@link Nested} group per method so
 * the suite reports as a per-method tree; the shared mock builders stay on the outer
 * class.
 */
final class MarketsTest {

    @Nested
    class IsOwnedColony {
        @Test
        void returns_true_for_a_faction_owned_non_condition_market() {

            var market = buildOwnedColony(buildFaction("hegemony"), false);

            assertThat(Markets.isOwnedColony(market))
                .isTrue();
        }

        @Test
        void returns_false_for_a_condition_only_market() {

            var market = buildOwnedColony(buildFaction("hegemony"), true);

            assertThat(Markets.isOwnedColony(market))
                .isFalse();
        }

        @Test
        void returns_false_when_no_faction_owns_the_market() {
            
            var market = buildOwnedColony(null, false);

            assertThat(Markets.isOwnedColony(market))
                .isFalse();
        }

        @Test
        void returns_false_for_a_null_market() {
            assertThat(Markets.isOwnedColony(null))
                .isFalse();
        }
    }

    @Nested
    class FindAttachedStation {
        
        @Test
        void yields_the_station_a_market_owns() {

            var station = buildStationEntity();
            var market = buildMarketConnectedTo(station);

            assertThat(Markets.findAttachedStation(market))
                .contains(station);
        }

        @Test
        void picks_the_station_out_of_the_market_s_other_connected_entities() {

            var station = buildStationEntity();
            var market = buildMarketConnectedTo(buildNonStationEntity(), station);

            assertThat(Markets.findAttachedStation(market))
                .contains(station);
        }

        @Test
        void yields_empty_for_a_market_with_no_connected_entities() {

            var market = buildMarketConnectedTo();

            assertThat(Markets.findAttachedStation(market))
                .isEmpty();
        }

        @Test
        void yields_empty_when_no_connected_entity_is_a_station() {

            var market = buildMarketConnectedTo(buildNonStationEntity());

            assertThat(Markets.findAttachedStation(market))
                .isEmpty();
        }

        @Test
        void yields_empty_for_a_station_tagged_entity_raising_no_station_fleet() {
            // The tag alone says "this is a station", not "this defends the market". The
            // fleet an orbital-station industry raises is what says the second.
            var market = buildMarketConnectedTo(buildFleetlessStationEntity());

            assertThat(Markets.findAttachedStation(market))
                .isEmpty();
        }

        @Test
        void yields_empty_for_a_market_sited_on_a_station_of_its_own() {
            // A market built on a station is connected to its own primary entity, which
            // carries the tag for what the place is. It must not come back as the place's
            // own defensive station.
            var market = buildStationSitedMarket();

            assertThat(Markets.findAttachedStation(market))
                .isEmpty();
        }

        @Test
        void ignores_a_station_tagged_no_orbital_station() {

            var market = buildMarketConnectedTo(buildOptedOutStationEntity());

            assertThat(Markets.findAttachedStation(market))
                .isEmpty();
        }

        @Test
        void yields_empty_for_a_null_market() {
            assertThat(Markets.findAttachedStation(null))
                .isEmpty();
        }

        @Test
        void yields_empty_for_null_connected_entities() {

            var marketMock = mock(MarketAPI.class);

            when(marketMock.getConnectedEntities())
                .thenReturn(null);

            assertThat(Markets.findAttachedStation(marketMock))
                .isEmpty();
        }
    }

    // The presence verdict is the entity read taken as a boolean, so this group pins that
    // pairing - a found station reads true, an unfound one false - plus the null contract
    // its own Javadoc states. The scan's edge cases (the NO_ORBITAL_STATION opt-out, absent
    // or null connected entities) belong to the read that runs the scan, above.
    @Nested
    class HasAttachedStation {
        @Test
        void returns_true_for_a_market_that_owns_a_station() {

            var market = buildMarketConnectedTo(buildStationEntity());

            assertThat(Markets.hasAttachedStation(market))
                .isTrue();
        }

        @Test
        void returns_false_when_no_connected_entity_is_a_station() {

            var market = buildMarketConnectedTo(buildNonStationEntity());

            assertThat(Markets.hasAttachedStation(market))
                .isFalse();
        }

        @Test
        void returns_false_for_a_null_market() {
            assertThat(Markets.hasAttachedStation(null))
                .isFalse();
        }
    }

    @Nested
    class GetStabilityFraction {
        @Test
        void full_stability_is_one() {
            assertThat(Markets.getStabilityFraction(buildMarketAtStability(10.0f)))
                .isEqualTo(1.0);
        }

        @Test
        void half_stability_is_one_half() {
            assertThat(Markets.getStabilityFraction(buildMarketAtStability(5.0f)))
                .isEqualTo(0.5);
        }

        @Test
        void zero_stability_is_zero() {
            assertThat(Markets.getStabilityFraction(buildMarketAtStability(0.0f)))
                .isEqualTo(0.0);
        }

        @Test
        void above_band_clamps_to_one() {
            assertThat(Markets.getStabilityFraction(buildMarketAtStability(12.0f)))
                .isEqualTo(1.0);
        }

        @Test
        void below_band_clamps_to_zero() {
            assertThat(Markets.getStabilityFraction(buildMarketAtStability(-3.0f)))
                .isEqualTo(0.0);
        }

        @Test
        void null_market_is_zero() {
            assertThat(Markets.getStabilityFraction(null))
                .isEqualTo(0.0);
        }
    }

    @Nested
    class IsKnownToPlayer {
        @Test
        void returns_true_when_the_entity_is_discovered() {

            var market = buildMarket(buildDiscoveredEntity(), true);

            assertThat(Markets.isKnownToPlayer(market))
                .isTrue();
        }

        @Test
        void returns_true_when_the_market_is_un_hidden_but_the_entity_is_still_discoverable() {

            var market = buildMarket(buildDiscoverableEntity(), false);

            assertThat(Markets.isKnownToPlayer(market))
                .isTrue();
        }

        @Test
        void returns_true_when_the_primary_entity_is_null() {

            var market = buildMarket(null, true);

            assertThat(Markets.isKnownToPlayer(market))
                .isTrue();
        }

        @Test
        void returns_false_when_the_market_is_hidden_on_a_discoverable_entity() {

            var market = buildMarket(buildDiscoverableEntity(), true);

            assertThat(Markets.isKnownToPlayer(market))
                .isFalse();
        }

        @Test
        void returns_false_for_a_null_market() {
            assertThat(Markets.isKnownToPlayer(null))
                .isFalse();
        }
    }

    @Nested
    class IsCountedAsColony {
        @Test
        void returns_true_for_a_known_owned_colony() {

            var market = buildVisibleColony();

            assertThat(Markets.isCountedAsColony(market, false))
                .isTrue();
        }

        @Test
        void returns_false_for_a_condition_only_market() {

            var market = buildConditionOnlyColony();

            assertThat(Markets.isCountedAsColony(market, false))
                .isFalse();
        }

        @Test
        void returns_false_for_an_undiscovered_concealed_colony() {

            var market = buildConcealedStation();

            assertThat(Markets.isCountedAsColony(market, false))
                .isFalse();
        }

        @Test
        void returns_true_for_an_undiscovered_concealed_colony_when_including_undiscovered() {

            var market = buildConcealedStation();

            assertThat(Markets.isCountedAsColony(market, true))
                .isTrue();
        }

        @Test
        void returns_false_for_a_null_market() {
            assertThat(Markets.isCountedAsColony(null, true))
                .isFalse();
        }
    }

    @Nested
    class IsDiscoveredByPlayer {
        @Test
        void returns_true_when_the_entity_is_discovered() {

            var market = buildMarket(buildDiscoveredEntity(), true);

            assertThat(Markets.isDiscoveredByPlayer(market))
                .isTrue();
        }

        @Test
        void returns_true_when_the_primary_entity_is_null() {

            var market = buildMarket(null, true);

            assertThat(Markets.isDiscoveredByPlayer(market))
                .isTrue();
        }

        @Test
        void returns_false_when_the_entity_is_undiscovered_though_the_market_is_un_hidden() {

            var market = buildMarket(buildDiscoverableEntity(), false);

            // The arm that carries this market past isKnownToPlayer is exactly the one this
            // read must not have: being publicly listed is not having been there.
            assertThat(Markets.isDiscoveredByPlayer(market))
                .isFalse();
        }

        @Test
        void returns_false_for_a_null_market() {
            assertThat(Markets.isDiscoveredByPlayer(null))
                .isFalse();
        }
    }

    @Nested
    class IsFoundColony {
        @Test
        void returns_true_for_a_found_colony_that_stays_concealed() {

            var market = buildFoundConcealedStation();

            // A raided base is hidden forever and its system plainly holds people.
            assertThat(Markets.isFoundColony(market, false))
                .isTrue();
        }

        @Test
        void returns_false_for_an_un_hidden_colony_whose_entity_is_undiscovered() {

            var market = buildUnfoundListedColony();

            // The one case parting this filter from isCountedAsColony, which admits the market
            // on its un-hidden arm.
            assertThat(Markets.isFoundColony(market, false))
                .isFalse();
            assertThat(Markets.isCountedAsColony(market, false))
                .isTrue();
        }

        @Test
        void returns_false_for_an_undiscovered_concealed_colony() {

            var market = buildConcealedStation();

            assertThat(Markets.isFoundColony(market, false))
                .isFalse();
        }

        @Test
        void returns_true_for_an_undiscovered_colony_when_including_undiscovered() {

            var market = buildConcealedStation();

            assertThat(Markets.isFoundColony(market, true))
                .isTrue();
        }

        @Test
        void returns_false_for_a_condition_only_market() {

            var market = buildConditionOnlyColony();

            assertThat(Markets.isFoundColony(market, false))
                .isFalse();
        }

        @Test
        void returns_false_for_a_null_market() {
            assertThat(Markets.isFoundColony(null, true))
                .isFalse();
        }
    }

    @Nested
    class ReadLargestMarketsPerFaction {

        @Test
        void keeps_only_the_larger_of_two_markets_sharing_an_entity_and_owner() {

            var station = buildDiscoveredEntity();
            var independent = buildFaction("independent");
            var vanillaAcademy = buildMarketAtPlace(station, independent, 3);
            var moddedAcademy = buildMarketAtPlace(station, independent, 5);

            assertThat(Markets.readLargestMarketsPerFaction(
                    List.of(vanillaAcademy, moddedAcademy)))
                .containsExactly(moddedAcademy);
        }

        @Test
        void keeps_a_market_of_each_owner_when_one_entity_carries_two() {

            var station = buildDiscoveredEntity();
            var hegemony = buildMarketAtPlace(station, buildFaction("hegemony"), 3);
            var pirates = buildMarketAtPlace(station, buildFaction("pirates"), 5);

            assertThat(Markets.readLargestMarketsPerFaction(List.of(hegemony, pirates)))
                .containsExactly(hegemony, pirates);
        }

        @Test
        void keeps_both_markets_of_one_owner_on_separate_entities() {

            var independent = buildFaction("independent");
            var academy = buildMarketAtPlace(buildDiscoveredEntity(), independent, 3);
            var ancyra = buildMarketAtPlace(buildDiscoveredEntity(), independent, 5);

            assertThat(Markets.readLargestMarketsPerFaction(List.of(academy, ancyra)))
                .containsExactly(academy, ancyra);
        }

        @Test
        void keeps_the_first_of_two_equal_sized_markets_sharing_a_place() {

            var station = buildDiscoveredEntity();
            var independent = buildFaction("independent");
            var first = buildMarketAtPlace(station, independent, 3);
            var second = buildMarketAtPlace(station, independent, 3);

            assertThat(Markets.readLargestMarketsPerFaction(List.of(first, second)))
                .containsExactly(first);
        }

        @Test
        void yields_a_place_where_it_was_first_named_though_its_winner_arrives_later() {

            var station = buildDiscoveredEntity();
            var independent = buildFaction("independent");
            var small = buildMarketAtPlace(station, independent, 3);
            var elsewhere = buildMarketAtPlace(buildDiscoveredEntity(), independent, 4);
            var large = buildMarketAtPlace(station, independent, 6);

            assertThat(Markets.readLargestMarketsPerFaction(List.of(small, elsewhere, large)))
                .containsExactly(large, elsewhere);
        }

        @Test
        void passes_through_markets_of_one_owner_that_have_no_entity() {

            var independent = buildFaction("independent");
            var first = buildMarketAtPlace(null, independent, 3);
            var second = buildMarketAtPlace(null, independent, 5);

            assertThat(Markets.readLargestMarketsPerFaction(List.of(first, second)))
                .containsExactly(first, second);
        }

        @Test
        void passes_through_markets_on_one_entity_that_have_no_owner() {

            var station = buildDiscoveredEntity();
            var first = buildMarketAtPlace(station, null, 3);
            var second = buildMarketAtPlace(station, null, 5);

            assertThat(Markets.readLargestMarketsPerFaction(List.of(first, second)))
                .containsExactly(first, second);
        }

        @Test
        void passes_through_markets_on_one_entity_whose_owner_has_no_id() {
            // An owner with no id cannot be told apart from any other, so keying on it
            // would merge places that share nothing but an unreadable faction.
            var station = buildDiscoveredEntity();
            var unnamedOwner = buildFaction(null);
            var first = buildMarketAtPlace(station, unnamedOwner, 3);
            var second = buildMarketAtPlace(station, unnamedOwner, 5);

            assertThat(Markets.readLargestMarketsPerFaction(List.of(first, second)))
                .containsExactly(first, second);
        }

        @Test
        void drops_null_markets() {

            var market = buildMarketAtPlace(
                buildDiscoveredEntity(), buildFaction("hegemony"), 4);

            assertThat(Markets.readLargestMarketsPerFaction(
                    Arrays.asList(null, market, null)))
                .containsExactly(market);
        }

        @Test
        void yields_an_empty_list_for_no_markets() {
            assertThat(Markets.readLargestMarketsPerFaction(List.of()))
                .isEmpty();
        }

        @Test
        void yields_an_empty_list_for_a_null_collection() {
            assertThat(Markets.readLargestMarketsPerFaction(null))
                .isEmpty();
        }
    }

    @Nested
    class IsMilitary {

        @BeforeEach
        void setUp() {
            // Misc's static initialiser reads Global.getSettings(), so the no-op proxy
            // must be installed before the delegating read loads the class.
            StarsectorSettingsFake.installSettings();
        }

        @AfterEach
        void tearDown() {
            StarsectorSettingsFake.clearSettings();
        }

        @Test
        void returns_true_when_the_military_flag_is_set() {

            var market = buildMarketWithMilitaryFlag(true);

            assertThat(Markets.isMilitary(market))
                .isTrue();
        }

        @Test
        void returns_false_when_the_military_flag_is_unset() {

            var market = buildMarketWithMilitaryFlag(false);

            assertThat(Markets.isMilitary(market))
                .isFalse();
        }

        @Test
        void returns_false_for_a_market_with_no_memory() {
            // Vanilla's own read would throw here; the neighbouring flag reads absorb it, so
            // this one does too rather than being the single read a caller must defend.
            var marketMock = mock(MarketAPI.class);

            when(marketMock.getMemoryWithoutUpdate())
                .thenReturn(null);

            assertThat(Markets.isMilitary(marketMock))
                .isFalse();
        }

        @Test
        void returns_false_for_a_null_market() {
            assertThat(Markets.isMilitary(null))
                .isFalse();
        }
    }

    // A market whose memory carries the $military flag at the given value - the signal a
    // military industry raises, and the one vanilla's own classification reads.
    private static MarketAPI buildMarketWithMilitaryFlag(boolean isMilitary) {

        var memoryMock = mock(MemoryAPI.class);

        when(memoryMock.getBoolean(MemFlags.MARKET_MILITARY))
            .thenReturn(isMilitary);

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getMemoryWithoutUpdate())
            .thenReturn(memoryMock);

        return marketMock;
    }

    // An ordinary colony: publicly listed on an entity the player has found. The plain case both
    // colony filters admit.
    private static MarketAPI buildVisibleColony() {
        return buildColonyMarket(false, false, false);
    }

    // A bare planet's condition-only placeholder: owned, but not a colony, so it fails the
    // ownership arm before either visibility rule is consulted.
    private static MarketAPI buildConditionOnlyColony() {
        return buildColonyMarket(true, false, false);
    }

    // A base still to be found: hidden and on a discoverable entity, so it fails both rules.
    private static MarketAPI buildConcealedStation() {
        return buildColonyMarket(false, true, true);
    }

    // The same base once raided: the entity is discovered, the market stays hidden for good.
    private static MarketAPI buildFoundConcealedStation() {
        return buildColonyMarket(false, true, false);
    }

    // A colony surfaced into the open ahead of being reached: publicly listed, entity still
    // undiscovered. Paired with the raided base above, this is where the two rules disagree.
    private static MarketAPI buildUnfoundListedColony() {
        return buildColonyMarket(false, false, true);
    }

    // An owned colony wired for both filter arms: ownership (a faction owns it, not
    // condition-only) and visibility (its entity's discoverability and the hidden flag). Reached
    // through the named builders above - three positional booleans say nothing at a call site.
    private static MarketAPI buildColonyMarket(
            boolean isConditionOnly,
            boolean isHidden,
            boolean isEntityDiscoverable) {

        // Build the entity and faction (each stubs its own mock) before opening the market's
        // stubbing, so the two do not nest into an unfinished-stubbing error.
        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.isDiscoverable())
            .thenReturn(isEntityDiscoverable);

        var factionMock = buildFaction("hegemony");
        var marketMock = mock(MarketAPI.class);

        when(marketMock.getFaction())
            .thenReturn(factionMock);
        when(marketMock.isPlanetConditionMarketOnly())
            .thenReturn(isConditionOnly);
        when(marketMock.isHidden())
            .thenReturn(isHidden);
        when(marketMock.getPrimaryEntity())
            .thenReturn(entityMock);

        return marketMock;
    }

    private static MarketAPI buildOwnedColony(FactionAPI faction, boolean isConditionOnly) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getFaction())
            .thenReturn(faction);
        when(marketMock.isPlanetConditionMarketOnly())
            .thenReturn(isConditionOnly);

        return marketMock;
    }

    // A market sited on an entity, owned by a faction, at a size - the three reads the
    // per-place resolution groups and picks winners by. Either half may be null to build
    // a market the resolution cannot key.
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

    private static FactionAPI buildFaction(String id) {

        var factionMock = mock(FactionAPI.class);

        when(factionMock.getId())
            .thenReturn(id);

        return factionMock;
    }

    private static MarketAPI buildMarket(SectorEntityToken primaryEntity, boolean isHidden) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getPrimaryEntity())
            .thenReturn(primaryEntity);
        when(marketMock.isHidden())
            .thenReturn(isHidden);

        return marketMock;
    }

    // An entity the player has already found: no longer flagged discoverable.
    private static SectorEntityToken buildDiscoveredEntity() {

        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.isDiscoverable())
            .thenReturn(false);

        return entityMock;
    }

    // An entity still awaiting physical discovery: flagged discoverable.
    private static SectorEntityToken buildDiscoverableEntity() {

        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.isDiscoverable())
            .thenReturn(true);

        return entityMock;
    }

    private static MarketAPI buildMarketAtStability(float stability) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getStabilityValue())
            .thenReturn(stability);

        return marketMock;
    }

    private static MarketAPI buildMarketConnectedTo(SectorEntityToken... entities) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getConnectedEntities())
            .thenReturn(Set.of(entities));

        return marketMock;
    }

    // A market that is itself a station: its own primary entity sits among its connected
    // entities and carries the "station" tag for what the place is, while raising no
    // station fleet of its own.
    private static MarketAPI buildStationSitedMarket() {

        var entityMock = buildFleetlessStationEntity();
        var marketMock = buildMarketConnectedTo(entityMock);

        when(marketMock.getPrimaryEntity())
            .thenReturn(entityMock);

        return marketMock;
    }

    // A station entity: carries the "station" tag, no opt-out, and the station fleet an
    // orbital-station industry raises - the shape the scan admits.
    private static SectorEntityToken buildStationEntity() {
        return buildStationTaggedEntity(mock(CampaignFleetAPI.class));
    }

    // A "station"-tagged entity with no station fleet in memory: a place built on a station
    // rather than a colony defended by one. Its own primary entity looks like this.
    private static SectorEntityToken buildFleetlessStationEntity() {
        return buildStationTaggedEntity(null);
    }

    // A "station"-tagged entity flagged NO_ORBITAL_STATION, vanilla's own opt-out.
    private static SectorEntityToken buildOptedOutStationEntity() {

        var entityMock = buildStationEntity();

        when(entityMock.hasTag("NO_ORBITAL_STATION"))
            .thenReturn(true);

        return entityMock;
    }

    // The shared wiring behind the two station shapes: the tag, plus whatever the entity's
    // memory answers for the station-fleet key. Vanilla's fleet read dereferences that memory
    // unconditionally, so it is always stubbed even where the fleet itself is absent.
    private static SectorEntityToken buildStationTaggedEntity(CampaignFleetAPI stationFleet) {

        var memoryMock = mock(MemoryAPI.class);

        when(memoryMock.get(MemFlags.STATION_FLEET))
            .thenReturn(stationFleet);

        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.hasTag(Tags.STATION))
            .thenReturn(true);
        when(entityMock.getMemoryWithoutUpdate())
            .thenReturn(memoryMock);

        return entityMock;
    }

    // A connected entity that is not a station (e.g. the market's planet).
    private static SectorEntityToken buildNonStationEntity() {
        return mock(SectorEntityToken.class);
    }
}
