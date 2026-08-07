package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.combat.StatBonus;
import com.fs.starfarer.api.fleet.MutableMarketStatsAPI;
import com.fs.starfarer.api.impl.campaign.ids.MemFlags;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.util.DynamicStatsAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link MarketPatrols#fieldsPatrols} and
 * {@link MarketPatrols#readPatrolCounts}. The cases live in a {@link Nested} group per
 * method so the suite reports as a per-method tree; the shared mock builders stay on the
 * outer class.
 */
final class MarketPatrolsTest {

    @Nested
    class ReadPatrolCounts {

        @Test
        void reads_the_three_tier_counts_from_the_dynamic_stats() {

            var market = buildMarketWithPatrolMods(4.0f, 3.0f, 1.0f);

            assertThat(MarketPatrols.readPatrolCounts(market))
                .isEqualTo(new PatrolCounts(4, 3, 1));
        }

        @Test
        void truncates_a_fractional_tier_count_to_int() {
            // Vanilla getMaxPatrols casts the effective mod to int, so a fractional
            // count floors rather than rounds.
            var market = buildMarketWithPatrolMods(2.9f, 1.4f, 0.6f);

            assertThat(MarketPatrols.readPatrolCounts(market))
                .isEqualTo(new PatrolCounts(2, 1, 0));
        }

        @Test
        void floors_a_negative_tier_count_to_zero() {

            var market = buildMarketWithPatrolMods(-1.0f, 2.0f, 0.0f);

            assertThat(MarketPatrols.readPatrolCounts(market))
                .isEqualTo(new PatrolCounts(0, 2, 0));
        }

        @Test
        void treats_a_missing_tier_mod_as_zero() {
            // A market with no military industry has no patrol mods; getMod returns
            // null for every tier.
            var market = buildMarketWithDynamic(mock(DynamicStatsAPI.class));

            assertThat(MarketPatrols.readPatrolCounts(market))
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

            assertThat(MarketPatrols.readPatrolCounts(marketMock))
                .isEqualTo(PatrolCounts.NONE);
        }

        @Test
        void returns_none_for_a_market_with_no_stats() {

            var marketMock = mock(MarketAPI.class);

            when(marketMock.getStats())
                .thenReturn(null);

            assertThat(MarketPatrols.readPatrolCounts(marketMock))
                .isEqualTo(PatrolCounts.NONE);
        }

        @Test
        void returns_none_for_a_null_market() {
            assertThat(MarketPatrols.readPatrolCounts(null))
                .isEqualTo(PatrolCounts.NONE);
        }
    }

    @Nested
    class FieldsPatrols {
        
        @Test
        void returns_true_when_the_patrol_flag_is_set() {

            var market = buildMarketWithPatrolFlag(true);

            assertThat(MarketPatrols.fieldsPatrols(market))
                .isTrue();
        }

        @Test
        void returns_false_when_the_patrol_flag_is_unset() {
            // A hidden raider base writes the patrol-count stats but never sets
            // $patrol, so it reads as fielding no patrols.
            var market = buildMarketWithPatrolFlag(false);

            assertThat(MarketPatrols.fieldsPatrols(market))
                .isFalse();
        }

        @Test
        void returns_false_for_a_market_with_no_memory() {

            var marketMock = mock(MarketAPI.class);

            when(marketMock.getMemoryWithoutUpdate())
                .thenReturn(null);

            assertThat(MarketPatrols.fieldsPatrols(marketMock))
                .isFalse();
        }

        @Test
        void returns_false_for_a_null_market() {
            assertThat(MarketPatrols.fieldsPatrols(null))
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
}
