package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.MutableMarketStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.util.DynamicStatsAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link Markets#isOwnedColony},
 * {@link Markets#hasAttachedStation}, {@link Markets#getStabilityFraction} and
 * {@link Markets#isKnownToPlayer}. The cases live in a {@link Nested} group per
 * method so the suite reports as a per-method tree; the shared mock builders stay
 * on the outer class.
 */
final class MarketsTest {

    @Nested
    class IsOwnedColony {
        @Test
        void returns_true_for_a_faction_owned_non_condition_market() {
            var market = buildOwnedColony(faction("hegemony"), false);

            assertThat(Markets.isOwnedColony(market)).isTrue();
        }

        @Test
        void returns_false_for_a_condition_only_market() {
            var market = buildOwnedColony(faction("hegemony"), true);

            assertThat(Markets.isOwnedColony(market)).isFalse();
        }

        @Test
        void returns_false_when_no_faction_owns_the_market() {
            var market = buildOwnedColony(null, false);

            assertThat(Markets.isOwnedColony(market)).isFalse();
        }

        @Test
        void returns_false_for_a_null_market() {
            assertThat(Markets.isOwnedColony(null)).isFalse();
        }
    }

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

    @Nested
    class GetStabilityFraction {
        @Test
        void full_stability_is_one() {
            assertThat(Markets.getStabilityFraction(marketAtStability(10.0f))).isEqualTo(1.0);
        }

        @Test
        void half_stability_is_one_half() {
            assertThat(Markets.getStabilityFraction(marketAtStability(5.0f))).isEqualTo(0.5);
        }

        @Test
        void zero_stability_is_zero() {
            assertThat(Markets.getStabilityFraction(marketAtStability(0.0f))).isEqualTo(0.0);
        }

        @Test
        void above_band_clamps_to_one() {
            assertThat(Markets.getStabilityFraction(marketAtStability(12.0f))).isEqualTo(1.0);
        }

        @Test
        void below_band_clamps_to_zero() {
            assertThat(Markets.getStabilityFraction(marketAtStability(-3.0f))).isEqualTo(0.0);
        }

        @Test
        void null_market_is_zero() {
            assertThat(Markets.getStabilityFraction(null)).isEqualTo(0.0);
        }
    }

    @Nested
    class IsKnownToPlayer {
        @Test
        void returns_true_when_the_entity_is_discovered() {
            var market = buildMarket(discoveredEntity(), true);

            assertThat(Markets.isKnownToPlayer(market)).isTrue();
        }

        @Test
        void returns_true_when_the_market_is_un_hidden_but_the_entity_is_still_discoverable() {
            var market = buildMarket(discoverableEntity(), false);

            assertThat(Markets.isKnownToPlayer(market)).isTrue();
        }

        @Test
        void returns_true_when_the_primary_entity_is_null() {
            var market = buildMarket(null, true);

            assertThat(Markets.isKnownToPlayer(market)).isTrue();
        }

        @Test
        void returns_false_when_the_market_is_hidden_on_a_discoverable_entity() {
            var market = buildMarket(discoverableEntity(), true);

            assertThat(Markets.isKnownToPlayer(market)).isFalse();
        }

        @Test
        void returns_false_for_a_null_market() {
            assertThat(Markets.isKnownToPlayer(null)).isFalse();
        }
    }

    @Nested
    class ReadPatrolCounts {
        @Test
        void reads_the_three_tier_counts_from_the_dynamic_stats() {
            var market = marketWithPatrolMods(4.0f, 3.0f, 1.0f);

            assertThat(Markets.readPatrolCounts(market)).isEqualTo(new PatrolCounts(4, 3, 1));
        }

        @Test
        void truncates_a_fractional_tier_count_to_int() {
            // Vanilla getMaxPatrols casts the effective mod to int, so a fractional
            // count floors rather than rounds.
            var market = marketWithPatrolMods(2.9f, 1.4f, 0.6f);

            assertThat(Markets.readPatrolCounts(market)).isEqualTo(new PatrolCounts(2, 1, 0));
        }

        @Test
        void floors_a_negative_tier_count_to_zero() {
            var market = marketWithPatrolMods(-1.0f, 2.0f, 0.0f);

            assertThat(Markets.readPatrolCounts(market)).isEqualTo(new PatrolCounts(0, 2, 0));
        }

        @Test
        void treats_a_missing_tier_mod_as_zero() {
            // A market with no military industry has no patrol mods; getMod returns
            // null for every tier.
            var market = marketWithDynamic(mock(DynamicStatsAPI.class));

            assertThat(Markets.readPatrolCounts(market)).isEqualTo(PatrolCounts.NONE);
        }

        @Test
        void returns_none_for_a_market_with_no_dynamic_stats() {
            var statsMock = mock(MutableMarketStatsAPI.class);
            when(statsMock.getDynamic()).thenReturn(null);
            var marketMock = mock(MarketAPI.class);
            when(marketMock.getStats()).thenReturn(statsMock);

            assertThat(Markets.readPatrolCounts(marketMock)).isEqualTo(PatrolCounts.NONE);
        }

        @Test
        void returns_none_for_a_market_with_no_stats() {
            var marketMock = mock(MarketAPI.class);
            when(marketMock.getStats()).thenReturn(null);

            assertThat(Markets.readPatrolCounts(marketMock)).isEqualTo(PatrolCounts.NONE);
        }

        @Test
        void returns_none_for_a_null_market() {
            assertThat(Markets.readPatrolCounts(null)).isEqualTo(PatrolCounts.NONE);
        }
    }

    // A market whose dynamic stats carry the three patrol-tier mods at the given
    // effective values (light, medium, heavy).
    private static MarketAPI marketWithPatrolMods(float light, float medium, float heavy) {
        // Build each tier's mock before the getMod stubbing: patrolMod() stubs a mock
        // of its own, and Mockito rejects a nested when(...) inside a thenReturn(...).
        var lightMod = patrolMod(light);
        var mediumMod = patrolMod(medium);
        var heavyMod = patrolMod(heavy);
        var dynamicMock = mock(DynamicStatsAPI.class);
        when(dynamicMock.getMod(Stats.PATROL_NUM_LIGHT_MOD)).thenReturn(lightMod);
        when(dynamicMock.getMod(Stats.PATROL_NUM_MEDIUM_MOD)).thenReturn(mediumMod);
        when(dynamicMock.getMod(Stats.PATROL_NUM_HEAVY_MOD)).thenReturn(heavyMod);
        return marketWithDynamic(dynamicMock);
    }

    // Wires a market whose stats expose the given dynamic stats, the seam the patrol
    // read walks.
    private static MarketAPI marketWithDynamic(DynamicStatsAPI dynamic) {
        var statsMock = mock(MutableMarketStatsAPI.class);
        when(statsMock.getDynamic()).thenReturn(dynamic);
        var marketMock = mock(MarketAPI.class);
        when(marketMock.getStats()).thenReturn(statsMock);
        return marketMock;
    }

    // A patrol-count mod whose effective value at base 0 is the given count.
    private static StatBonus patrolMod(float effective) {
        var modMock = mock(StatBonus.class);
        when(modMock.computeEffective(0.0f)).thenReturn(effective);
        return modMock;
    }

    private static MarketAPI buildOwnedColony(FactionAPI faction, boolean isConditionOnly) {
        var marketMock = mock(MarketAPI.class);
        when(marketMock.getFaction()).thenReturn(faction);
        when(marketMock.isPlanetConditionMarketOnly()).thenReturn(isConditionOnly);
        return marketMock;
    }

    private static FactionAPI faction(String id) {
        var factionMock = mock(FactionAPI.class);
        when(factionMock.getId()).thenReturn(id);
        return factionMock;
    }

    private static MarketAPI buildMarket(SectorEntityToken primaryEntity, boolean isHidden) {
        var marketMock = mock(MarketAPI.class);
        when(marketMock.getPrimaryEntity()).thenReturn(primaryEntity);
        when(marketMock.isHidden()).thenReturn(isHidden);
        return marketMock;
    }

    // An entity the player has already found: no longer flagged discoverable.
    private static SectorEntityToken discoveredEntity() {
        var entityMock = mock(SectorEntityToken.class);
        when(entityMock.isDiscoverable()).thenReturn(false);
        return entityMock;
    }

    // An entity still awaiting physical discovery: flagged discoverable.
    private static SectorEntityToken discoverableEntity() {
        var entityMock = mock(SectorEntityToken.class);
        when(entityMock.isDiscoverable()).thenReturn(true);
        return entityMock;
    }

    private static MarketAPI marketAtStability(float stability) {
        var marketMock = mock(MarketAPI.class);
        when(marketMock.getStabilityValue()).thenReturn(stability);
        return marketMock;
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
