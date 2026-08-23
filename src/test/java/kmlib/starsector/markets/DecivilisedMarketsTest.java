package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins what a dead world is and when the player may be told it is one.
 *
 * <p>The two are deliberately separate reads, and the cases are grouped that way: what the market
 * is turns on the condition it carries and on its being the condition-only shell a colony leaves
 * behind, while the reveal turns on how much of it has been surveyed.
 */
final class DecivilisedMarketsTest {

    @Nested
    class IsDecivilisedWorld {

        @Test
        void admitsAConditionOnlyShellCarryingTheDecivilisedCondition() {

            var market = buildConditionOnlyMarket(true);

            assertThat(DecivilisedMarkets.isDecivilisedWorld(market))
                .isTrue();
        }

        @Test
        void refusesABarePlanetsPlaceholder() {

            // Every uninhabited world in the sector carries one of these, and admitting them would
            // report somebody present in every system anybody ever surveyed.
            var market = buildConditionOnlyMarket(false);

            assertThat(DecivilisedMarkets.isDecivilisedWorld(market))
                .isFalse();
        }

        @Test
        void refusesAHeldColonyCarryingTheCondition() {

            // A world resettled over its own ruins is the living colony it is now, not the ruin it
            // was - which is what being condition-only is asked to tell apart.
            var market = buildMarket(true, false);

            assertThat(DecivilisedMarkets.isDecivilisedWorld(market))
                .isFalse();
        }

        @Test
        void refusesAMarketWithNoOwner() {

            var marketMock = mock(MarketAPI.class);

            when(marketMock.isPlanetConditionMarketOnly())
                .thenReturn(true);
            when(marketMock.hasCondition(Conditions.DECIVILIZED))
                .thenReturn(true);

            assertThat(DecivilisedMarkets.isDecivilisedWorld(marketMock))
                .isFalse();
        }

        @Test
        void refusesANullMarket() {

            assertThat(DecivilisedMarkets.isDecivilisedWorld(null))
                .isFalse();
        }
    }

    @Nested
    class IsRevealedDecivilised {

        @Test
        void admitsASurveyedWorldWhoseConditionNeededSurveying() {

            var market = buildSurveyedMarket(MarketAPI.SurveyLevel.FULL, true, true);

            assertThat(DecivilisedMarkets.isRevealedDecivilised(market))
                .isTrue();
        }

        @Test
        void admitsAnEncounteredWorldWhoseConditionNeedsNoSurveying() {

            // A condition visible on contact reveals as soon as the planet is encountered.
            var market = buildSurveyedMarket(MarketAPI.SurveyLevel.SEEN, false, false);

            assertThat(DecivilisedMarkets.isRevealedDecivilised(market))
                .isTrue();
        }

        @Test
        void refusesANeverEncounteredWorld() {

            // Still at SurveyLevel.NONE: the player has never been here, so even a surveyed-flagged
            // condition must not leak onto the map.
            var market = buildSurveyedMarket(MarketAPI.SurveyLevel.NONE, false, true);

            assertThat(DecivilisedMarkets.isRevealedDecivilised(market))
                .isFalse();
        }

        @Test
        void refusesAWorldWhoseConditionStillNeedsSurveying() {

            var market = buildSurveyedMarket(MarketAPI.SurveyLevel.PRELIMINARY, true, false);

            assertThat(DecivilisedMarkets.isRevealedDecivilised(market))
                .isFalse();
        }

        @Test
        void refusesAWorldCarryingNoDecivilisedCondition() {

            var marketMock = mock(MarketAPI.class);

            when(marketMock.getSurveyLevel())
                .thenReturn(MarketAPI.SurveyLevel.FULL);

            assertThat(DecivilisedMarkets.isRevealedDecivilised(marketMock))
                .isFalse();
        }

        @Test
        void refusesANullMarket() {

            assertThat(DecivilisedMarkets.isRevealedDecivilised(null))
                .isFalse();
        }
    }

    // A market on the condition-only shell every uninhabited world carries, which the decivilised
    // condition is then what parts a ruin from.
    private static MarketAPI buildConditionOnlyMarket(boolean isDecivilised) {
        return buildMarket(isDecivilised, true);
    }

    private static MarketAPI buildMarket(boolean isDecivilised, boolean isConditionOnly) {

        // The faction finishes its own stubbing before the market's opens, so the two do not nest
        // into an unfinished-stubbing error.
        var factionMock = mock(FactionAPI.class);
        var marketMock = mock(MarketAPI.class);

        when(marketMock.getFaction())
            .thenReturn(factionMock);
        when(marketMock.isPlanetConditionMarketOnly())
            .thenReturn(isConditionOnly);
        when(marketMock.hasCondition(Conditions.DECIVILIZED))
            .thenReturn(isDecivilised);

        return marketMock;
    }

    // A ruin at a stated survey level, whose condition carries its own survey bar.
    private static MarketAPI buildSurveyedMarket(
            MarketAPI.SurveyLevel surveyLevel,
            boolean doesRequireSurveying,
            boolean isSurveyed) {

        var conditionMock = mock(MarketConditionAPI.class);

        when(conditionMock.requiresSurveying())
            .thenReturn(doesRequireSurveying);
        when(conditionMock.isSurveyed())
            .thenReturn(isSurveyed);

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getSurveyLevel())
            .thenReturn(surveyLevel);
        when(marketMock.getFirstCondition(Conditions.DECIVILIZED))
            .thenReturn(conditionMock);

        return marketMock;
    }
}
