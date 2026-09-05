package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.fs.starfarer.api.campaign.econ.MarketAPI.SurveyLevel.FULL;
import static com.fs.starfarer.api.campaign.econ.MarketAPI.SurveyLevel.NONE;
import static com.fs.starfarer.api.campaign.econ.MarketAPI.SurveyLevel.PRELIMINARY;
import static com.fs.starfarer.api.campaign.econ.MarketAPI.SurveyLevel.SEEN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins what a decivilised world is and how far the player has to have surveyed one before being
 * told its colony collapsed.
 *
 * <p>The reads are deliberately separate, and the cases are grouped that way: what the market is
 * turns on the condition it carries and on its being the condition-only shell a collapse leaves
 * behind, the reveal turns on how much of the world has been surveyed and on vanilla's own rule
 * for reading the condition, and the sighting bar turns on nothing but the level asked for.
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

            // A world resettled over its own ruins is the governed colony it is now, not the
            // collapse it came out of - which is what being condition-only is asked to tell apart.
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
    class IsMetBySighting {

        @Test
        void meetsABarOfNothingAtAll() {

            assertThat(DecivilisedMarkets.isMetBySighting(NONE))
                .isTrue();
        }

        @Test
        void meetsABarOfHavingBeenSeen() {

            // The whole of what a sighting says: somebody laid eyes on the place.
            assertThat(DecivilisedMarkets.isMetBySighting(SEEN))
                .isTrue();
        }

        @Test
        void refusesABarOfPreliminarySurveyData() {

            // Above SEEN the caller is asking for readings taken off the world, which nobody's
            // presence beside it produces - so a sighting reaches these neither in part nor by
            // degree.
            assertThat(DecivilisedMarkets.isMetBySighting(PRELIMINARY))
                .isFalse();
        }

        @Test
        void refusesABarOfAFullSurvey() {

            assertThat(DecivilisedMarkets.isMetBySighting(FULL))
                .isFalse();
        }

        @Test
        void readsAnAbsentLevelAsTheBarVanillaAsksFor() {

            // The same rule the market-taking reads follow, and here it lands on SEEN - which a
            // sighting does meet.
            assertThat(DecivilisedMarkets.isMetBySighting(null))
                .isTrue();
        }
    }

    @Nested
    class IsRevealedDecivilised {

        @Test
        void admitsASurveyedWorldWhoseConditionNeededSurveying() {

            var market = buildSurveyedMarket(MarketAPI.SurveyLevel.FULL, true, true);

            assertThat(DecivilisedMarkets.isRevealedDecivilised(market, SEEN))
                .isTrue();
        }

        @Test
        void admitsAnEncounteredWorldWhoseConditionNeedsNoSurveying() {

            // A condition visible on contact reveals as soon as the planet is encountered.
            var market = buildSurveyedMarket(MarketAPI.SurveyLevel.SEEN, false, false);

            assertThat(DecivilisedMarkets.isRevealedDecivilised(market, SEEN))
                .isTrue();
        }

        @Test
        void refusesANeverEncounteredWorld() {

            // Still at SurveyLevel.NONE: the player has never been here, so even a surveyed-flagged
            // condition must not leak onto the map.
            var market = buildSurveyedMarket(MarketAPI.SurveyLevel.NONE, false, true);

            assertThat(DecivilisedMarkets.isRevealedDecivilised(market, SEEN))
                .isFalse();
        }

        @Test
        void refusesAWorldWhoseConditionStillNeedsSurveying() {

            var market = buildSurveyedMarket(MarketAPI.SurveyLevel.PRELIMINARY, true, false);

            assertThat(DecivilisedMarkets.isRevealedDecivilised(market, SEEN))
                .isFalse();
        }

        @Test
        void refusesASeenWorldWhereAFullSurveyIsAskedFor() {
            // The bar moves both ways, which is what makes it a level rather than a reveal: a
            // world vanilla would show is withheld where the caller asks for more than vanilla
            // asks.
            var market = buildSurveyedMarket(MarketAPI.SurveyLevel.SEEN, false, false);

            assertThat(DecivilisedMarkets.isRevealedDecivilised(market, FULL))
                .isFalse();
        }

        @Test
        void admitsANeverEncounteredWorldWhereNoSurveyIsAskedFor() {
            // The other end of the same ladder, and the only setting that names a world nobody has
            // looked at.
            var market = buildSurveyedMarket(MarketAPI.SurveyLevel.NONE, false, false);

            assertThat(DecivilisedMarkets.isRevealedDecivilised(market, NONE))
                .isTrue();
        }

        @Test
        void readsAnAbsentLevelAsTheBarVanillaAsksFor() {
            // A missing argument may not be read as no bar at all: the direction it must never
            // take is the widening one.
            var market = buildSurveyedMarket(MarketAPI.SurveyLevel.NONE, false, false);

            assertThat(DecivilisedMarkets.isRevealedDecivilised(market, null))
                .isFalse();
        }

        @Test
        void refusesAWorldCarryingNoDecivilisedCondition() {

            var marketMock = mock(MarketAPI.class);

            when(marketMock.getSurveyLevel())
                .thenReturn(MarketAPI.SurveyLevel.FULL);

            assertThat(DecivilisedMarkets.isRevealedDecivilised(marketMock, SEEN))
                .isFalse();
        }

        @Test
        void refusesANullMarket() {

            assertThat(DecivilisedMarkets.isRevealedDecivilised(null, SEEN))
                .isFalse();
        }
    }

    // A market on the condition-only shell every uninhabited world carries, which the decivilised
    // condition is then what parts a collapsed colony from.
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

    // A decivilised world at a stated survey level, whose condition carries its own survey bar.
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
