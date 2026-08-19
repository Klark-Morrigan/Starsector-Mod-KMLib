package kmlib.console.markets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the two requirements the console offers - what each admits, and the phrase a refusal
 * names it by, that phrase being what every message about a rejected target is worded from. The
 * cases live in {@link Nested} groups so the suite reports as a per-method tree; the shared mock
 * builder stays on the outer class.
 */
final class MarketTargetRequirementTest {

    @Nested
    class IsMetBy {

        @Test
        void admits_a_body_still_carrying_only_survey_data_as_colonisable() {

            assertThat(MarketTargetRequirement.COLONISABLE_BODY.isMetBy(
                    buildMarket(mock(FactionAPI.class), true, false)))
                .isTrue();
        }

        @Test
        void refuses_an_existing_colony_as_colonisable() {

            assertThat(MarketTargetRequirement.COLONISABLE_BODY.isMetBy(
                    buildMarket(mock(FactionAPI.class), false, true)))
                .isFalse();
        }

        @Test
        void admits_a_colony_a_faction_holds_as_transferable() {

            assertThat(MarketTargetRequirement.EXISTING_COLONY.isMetBy(
                    buildMarket(mock(FactionAPI.class), false, true)))
                .isTrue();
        }

        @Test
        void refuses_a_body_still_carrying_only_survey_data_as_transferable() {
            // The two requirements are complements over the same market, which is what lets one
            // resolver serve both commands: whatever one admits, the other refuses.
            assertThat(MarketTargetRequirement.EXISTING_COLONY.isMetBy(
                    buildMarket(mock(FactionAPI.class), true, false)))
                .isFalse();
        }
    }

    @Nested
    class RequirementPhrase {

        @Test
        void words_the_colonisable_body_requirement_with_its_own_article() {

            assertThat(MarketTargetRequirement.COLONISABLE_BODY.requirementPhrase())
                .isEqualTo("a body ready for colonisation");
        }

        @Test
        void words_the_existing_colony_requirement_with_its_own_article() {
            // "an", not "a": the article belongs to the phrase, so a message reads as a
            // sentence without the caller having to know which one this requirement takes.
            assertThat(MarketTargetRequirement.EXISTING_COLONY.requirementPhrase())
                .isEqualTo("an existing colony");
        }
    }

    // A market posed by the three states the two requirements read: who holds it, whether it is
    // a planet's condition-only placeholder, and whether the economy has it registered.
    private static MarketAPI buildMarket(
            FactionAPI faction,
            boolean isConditionOnly,
            boolean isInEconomy) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getFaction())
            .thenReturn(faction);
        when(marketMock.isPlanetConditionMarketOnly())
            .thenReturn(isConditionOnly);
        when(marketMock.isInEconomy())
            .thenReturn(isInEconomy);

        return marketMock;
    }
}
