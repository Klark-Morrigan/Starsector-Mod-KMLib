package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contract of {@link MarketColoniser#isReadyForColonisation}. The cases live in a
 * {@link Nested} group so the suite reports as a per-method tree; the shared mock builder stays
 * on the outer class.
 */
final class MarketColoniserTest {

    @Nested
    class IsReadyForColonisation {

        @Test
        void accepts_survey_data_on_a_body_no_one_has_colonised() {
            // The shape procgen leaves on every uninhabited world: a market carrying the
            // planet's conditions and nothing else, hung on the entity and never registered.
            assertThat(MarketColoniser.isReadyForColonisation(buildMarket(true, false)))
                .isTrue();
        }

        @Test
        void rejects_a_colony_that_already_exists() {

            assertThat(MarketColoniser.isReadyForColonisation(buildMarket(false, true)))
                .isFalse();
        }

        @Test
        void rejects_a_colony_the_economy_does_not_list() {
            // Vanilla builds Galatia Academy this way on purpose. Only the condition-only arm
            // rejects it, which is why registration cannot be the whole of the read.
            assertThat(MarketColoniser.isReadyForColonisation(buildMarket(false, false)))
                .isFalse();
        }

        @Test
        void rejects_survey_data_the_economy_already_lists() {
            // Nothing vanilla builds, and founding on it would register the market a second
            // time - so the registration arm answers a shape the flag alone would admit.
            assertThat(MarketColoniser.isReadyForColonisation(buildMarket(true, true)))
                .isFalse();
        }

        @Test
        void rejects_a_null_market() {
            assertThat(MarketColoniser.isReadyForColonisation(null))
                .isFalse();
        }
    }

    // A market posed by the two states colonisation readiness is decided on: whether it is a
    // planet's condition-only placeholder, and whether the economy has it registered.
    private static MarketAPI buildMarket(boolean isConditionOnly, boolean isInEconomy) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.isPlanetConditionMarketOnly())
            .thenReturn(isConditionOnly);
        when(marketMock.isInEconomy())
            .thenReturn(isInEconomy);

        return marketMock;
    }
}
