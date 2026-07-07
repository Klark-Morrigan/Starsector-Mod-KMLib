package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.econ.MarketConditionAPI;
import com.fs.starfarer.api.impl.campaign.ids.Conditions;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contracts of {@link DecivilisedMarkets#hasRevealedDecivilisedPlanet}
 * and {@link DecivilisedMarkets#findRevealedDecivilisedSystemIds}: a planet
 * counts as a revealed dead colony only when the player has encountered it and
 * the {@code decivilized} condition is visible under vanilla's own survey rule; a
 * never-encountered world, a still-hidden condition, or a plain rock without the
 * condition does not; and the system-level scan collects exactly the systems that
 * hold one. Each method's cases live in a {@link Nested} group so the suite
 * reports as a per-method tree; the shared mock builders stay on the outer class.
 */
final class DecivilisedMarketsTest {

    @Nested
    class HasRevealedDecivilisedPlanet {
        @Test
        void is_true_for_an_encountered_revealed_planet() {
            var system = systemWithPlanets("a",
                    planetWithMarket(decivilisedMarket(MarketAPI.SurveyLevel.FULL, true, true)));

            assertThat(DecivilisedMarkets.hasRevealedDecivilisedPlanet(system)).isTrue();
        }

        @Test
        void is_true_when_the_condition_does_not_require_surveying() {
            // A condition visible on contact reveals as soon as the planet is
            // encountered, without a survey.
            var system = systemWithPlanets("a",
                    planetWithMarket(decivilisedMarket(MarketAPI.SurveyLevel.SEEN, false, false)));

            assertThat(DecivilisedMarkets.hasRevealedDecivilisedPlanet(system)).isTrue();
        }

        @Test
        void is_false_for_a_never_encountered_planet() {
            // Still at SurveyLevel.NONE: the player has never been here, so even a
            // surveyed-flagged condition must not leak onto the map.
            var system = systemWithPlanets("a",
                    planetWithMarket(decivilisedMarket(MarketAPI.SurveyLevel.NONE, false, true)));

            assertThat(DecivilisedMarkets.hasRevealedDecivilisedPlanet(system)).isFalse();
        }

        @Test
        void is_false_when_the_condition_still_needs_surveying() {
            var system = systemWithPlanets("a", planetWithMarket(
                    decivilisedMarket(MarketAPI.SurveyLevel.PRELIMINARY, true, false)));

            assertThat(DecivilisedMarkets.hasRevealedDecivilisedPlanet(system)).isFalse();
        }

        @Test
        void is_false_for_a_planet_without_the_decivilised_condition() {
            // An ordinary surveyed rock - encountered, but carrying no decivilised
            // condition - is not a dead colony.
            var system = systemWithPlanets("a",
                    planetWithMarket(marketWithoutDecivilisedCondition(MarketAPI.SurveyLevel.FULL)));

            assertThat(DecivilisedMarkets.hasRevealedDecivilisedPlanet(system)).isFalse();
        }

        @Test
        void skips_planets_without_a_market() {
            // The central star has no market; the ruin on a later planet still
            // counts.
            var system = systemWithPlanets("a", starWithoutMarket(),
                    planetWithMarket(decivilisedMarket(MarketAPI.SurveyLevel.FULL, false, false)));

            assertThat(DecivilisedMarkets.hasRevealedDecivilisedPlanet(system)).isTrue();
        }

        @Test
        void is_false_for_a_null_system() {
            assertThat(DecivilisedMarkets.hasRevealedDecivilisedPlanet(null)).isFalse();
        }
    }

    @Nested
    class FindRevealedDecivilisedSystemIds {
        @Test
        void collects_only_systems_with_a_revealed_ruin() {
            var withRuin = systemWithPlanets("ruined",
                    planetWithMarket(decivilisedMarket(MarketAPI.SurveyLevel.FULL, false, false)));
            var withoutRuin = systemWithPlanets("clean",
                    planetWithMarket(marketWithoutDecivilisedCondition(MarketAPI.SurveyLevel.FULL)));

            assertThat(DecivilisedMarkets.findRevealedDecivilisedSystemIds(
                    sectorOf(withRuin, withoutRuin))).containsExactly("ruined");
        }

        @Test
        void is_empty_for_a_null_sector() {
            assertThat(DecivilisedMarkets.findRevealedDecivilisedSystemIds(null)).isEmpty();
        }
    }

    private static SectorAPI sectorOf(StarSystemAPI... systems) {
        var sectorMock = mock(SectorAPI.class);
        when(sectorMock.getStarSystems()).thenReturn(List.of(systems));
        return sectorMock;
    }

    private static StarSystemAPI systemWithPlanets(String id, PlanetAPI... planets) {
        var systemMock = mock(StarSystemAPI.class);
        when(systemMock.getId()).thenReturn(id);
        when(systemMock.getPlanets()).thenReturn(List.of(planets));
        return systemMock;
    }

    private static PlanetAPI planetWithMarket(MarketAPI market) {
        var planetMock = mock(PlanetAPI.class);
        when(planetMock.getMarket()).thenReturn(market);
        return planetMock;
    }

    // A planet with no market (e.g. the central star), so getMarket() is null.
    private static PlanetAPI starWithoutMarket() {
        return mock(PlanetAPI.class);
    }

    private static MarketAPI decivilisedMarket(MarketAPI.SurveyLevel surveyLevel,
            boolean doesRequireSurveying, boolean isSurveyed) {
        var conditionMock = mock(MarketConditionAPI.class);
        when(conditionMock.requiresSurveying()).thenReturn(doesRequireSurveying);
        when(conditionMock.isSurveyed()).thenReturn(isSurveyed);
        var marketMock = mock(MarketAPI.class);
        when(marketMock.getSurveyLevel()).thenReturn(surveyLevel);
        when(marketMock.getFirstCondition(Conditions.DECIVILIZED)).thenReturn(conditionMock);
        return marketMock;
    }

    // A market with no decivilised condition (getFirstCondition returns null),
    // standing in for an ordinary uninhabited planet.
    private static MarketAPI marketWithoutDecivilisedCondition(MarketAPI.SurveyLevel surveyLevel) {
        var marketMock = mock(MarketAPI.class);
        when(marketMock.getSurveyLevel()).thenReturn(surveyLevel);
        return marketMock;
    }
}
