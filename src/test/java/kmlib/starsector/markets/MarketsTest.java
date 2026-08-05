package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.MutableMarketStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.DynamicStatsAPI;

import kmlib.starsector.testing.StarsectorSettingsFake;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link Markets#isOwnedColony},
 * {@link Markets#hasAttachedStation}, {@link Markets#getStabilityFraction},
 * {@link Markets#isKnownToPlayer}, {@link Markets#isCountedAsColony},
 * {@link Markets#isDiscoveredByPlayer}, {@link Markets#isFoundColony},
 * {@link Markets#fieldsPatrols}, {@link Markets#readPatrolCounts} and
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
    class HasAttachedStation {
        @Test
        void returns_true_for_a_market_that_owns_a_station() {

            var market = buildMarketConnectedTo(buildStationEntity());

            assertThat(Markets.hasAttachedStation(market))
                .isTrue();
        }

        @Test
        void returns_false_for_a_market_with_no_connected_entities() {

            var market = buildMarketConnectedTo();

            assertThat(Markets.hasAttachedStation(market))
                .isFalse();
        }

        @Test
        void returns_false_when_no_connected_entity_is_a_station() {

            var market = buildMarketConnectedTo(buildNonStationEntity());

            assertThat(Markets.hasAttachedStation(market))
                .isFalse();
        }

        @Test
        void ignores_a_station_tagged_no_orbital_station() {

            var market = buildMarketConnectedTo(buildOptedOutStationEntity());

            assertThat(Markets.hasAttachedStation(market))
                .isFalse();
        }

        @Test
        void returns_false_for_a_null_market() {
            assertThat(Markets.hasAttachedStation(null))
                .isFalse();
        }

        @Test
        void returns_false_for_null_connected_entities() {

            var marketMock = mock(MarketAPI.class);

            when(marketMock.getConnectedEntities())
                .thenReturn(null);

            assertThat(Markets.hasAttachedStation(marketMock))
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
    class ReadPatrolCounts {
        @Test
        void reads_the_three_tier_counts_from_the_dynamic_stats() {

            var market = buildMarketWithPatrolMods(4.0f, 3.0f, 1.0f);

            assertThat(Markets.readPatrolCounts(market))
                .isEqualTo(new PatrolCounts(4, 3, 1));
        }

        @Test
        void truncates_a_fractional_tier_count_to_int() {
            // Vanilla getMaxPatrols casts the effective mod to int, so a fractional
            // count floors rather than rounds.
            var market = buildMarketWithPatrolMods(2.9f, 1.4f, 0.6f);

            assertThat(Markets.readPatrolCounts(market))
                .isEqualTo(new PatrolCounts(2, 1, 0));
        }

        @Test
        void floors_a_negative_tier_count_to_zero() {

            var market = buildMarketWithPatrolMods(-1.0f, 2.0f, 0.0f);

            assertThat(Markets.readPatrolCounts(market))
                .isEqualTo(new PatrolCounts(0, 2, 0));
        }

        @Test
        void treats_a_missing_tier_mod_as_zero() {
            // A market with no military industry has no patrol mods; getMod returns
            // null for every tier.
            var market = buildMarketWithDynamic(mock(DynamicStatsAPI.class));

            assertThat(Markets.readPatrolCounts(market))
                .isEqualTo(PatrolCounts.NONE);
        }

        @Test
        void returns_none_for_a_market_with_no_dynamic_stats() {

            var statsMock = mock(MutableMarketStatsAPI.class);

            when(statsMock.getDynamic())
                .thenReturn(null);

            var marketMock = mock(MarketAPI.class);

            when(marketMock.getStats())
                .thenReturn(statsMock);

            assertThat(Markets.readPatrolCounts(marketMock))
                .isEqualTo(PatrolCounts.NONE);
        }

        @Test
        void returns_none_for_a_market_with_no_stats() {

            var marketMock = mock(MarketAPI.class);

            when(marketMock.getStats())
                .thenReturn(null);

            assertThat(Markets.readPatrolCounts(marketMock))
                .isEqualTo(PatrolCounts.NONE);
        }

        @Test
        void returns_none_for_a_null_market() {
            assertThat(Markets.readPatrolCounts(null))
                .isEqualTo(PatrolCounts.NONE);
        }
    }

    @Nested
    class FieldsPatrols {
        @Test
        void returns_true_when_the_patrol_flag_is_set() {

            var market = buildMarketWithPatrolFlag(true);

            assertThat(Markets.fieldsPatrols(market))
                .isTrue();
        }

        @Test
        void returns_false_when_the_patrol_flag_is_unset() {
            // A hidden raider base writes the patrol-count stats but never sets
            // $patrol, so it reads as fielding no patrols.
            var market = buildMarketWithPatrolFlag(false);

            assertThat(Markets.fieldsPatrols(market))
                .isFalse();
        }

        @Test
        void returns_false_for_a_market_with_no_memory() {

            var marketMock = mock(MarketAPI.class);
            
            when(marketMock.getMemoryWithoutUpdate())
                .thenReturn(null);

            assertThat(Markets.fieldsPatrols(marketMock))
                .isFalse();
        }

        @Test
        void returns_false_for_a_null_market() {
            assertThat(Markets.fieldsPatrols(null))
                .isFalse();
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

    // A market whose memory carries the $patrol flag at the given value - the signal a
    // functional patrol HQ sets, vanilla's own "fields patrols" gate.
    private static MarketAPI buildMarketWithPatrolFlag(boolean fieldsPatrols) {

        var memoryMock = mock(MemoryAPI.class);

        when(memoryMock.getBoolean(MemFlags.MARKET_PATROL))
            .thenReturn(fieldsPatrols);

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getMemoryWithoutUpdate())
            .thenReturn(memoryMock);

        return marketMock;
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

    // A market whose dynamic stats carry the three patrol-tier mods at the given
    // effective values (light, medium, heavy).
    private static MarketAPI buildMarketWithPatrolMods(float light, float medium, float heavy) {

        // Build each tier's mock before the getMod stubbing: buildPatrolMod() stubs a mock
        // of its own, and Mockito rejects a nested when(...) inside a thenReturn(...).
        var lightMod = buildPatrolMod(light);
        var mediumMod = buildPatrolMod(medium);
        var heavyMod = buildPatrolMod(heavy);
        
        var dynamicMock = mock(DynamicStatsAPI.class);

        when(dynamicMock.getMod(Stats.PATROL_NUM_LIGHT_MOD))
            .thenReturn(lightMod);
        when(dynamicMock.getMod(Stats.PATROL_NUM_MEDIUM_MOD))
            .thenReturn(mediumMod);
        when(dynamicMock.getMod(Stats.PATROL_NUM_HEAVY_MOD))
            .thenReturn(heavyMod);

        return buildMarketWithDynamic(dynamicMock);
    }

    // Wires a market whose stats expose the given dynamic stats, the seam the patrol
    // read walks.
    private static MarketAPI buildMarketWithDynamic(DynamicStatsAPI dynamic) {

        var statsMock = mock(MutableMarketStatsAPI.class);

        when(statsMock.getDynamic())
            .thenReturn(dynamic);

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getStats())
            .thenReturn(statsMock);

        return marketMock;
    }

    // A patrol-count mod whose effective value at base 0 is the given count.
    private static StatBonus buildPatrolMod(float effective) {

        var modMock = mock(StatBonus.class);

        when(modMock.computeEffective(0.0f))
            .thenReturn(effective);

        return modMock;
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

    // A station entity: carries the "station" tag and no opt-out.
    private static SectorEntityToken buildStationEntity() {

        var entityMock = mock(SectorEntityToken.class);

        when(entityMock.hasTag(Tags.STATION))
            .thenReturn(true);

        return entityMock;
    }

    // A "station"-tagged entity flagged NO_ORBITAL_STATION, vanilla's own opt-out.
    private static SectorEntityToken buildOptedOutStationEntity() {

        var entityMock = buildStationEntity();

        when(entityMock.hasTag("NO_ORBITAL_STATION"))
            .thenReturn(true);

        return entityMock;
    }

    // A connected entity that is not a station (e.g. the market's planet).
    private static SectorEntityToken buildNonStationEntity() {
        return mock(SectorEntityToken.class);
    }
}
