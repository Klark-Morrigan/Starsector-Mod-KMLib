package kmlib.starsector.markets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Pins the contract of {@link MarketOwnershipTransfer#isReadyForTransfer}. The cases live in a
 * {@link Nested} group so the suite reports as a per-method tree; the shared mock builder stays
 * on the outer class.
 */
final class MarketOwnershipTransferTest {

    @Nested
    class IsReadyForTransfer {

        @Test
        void accepts_a_colony_a_faction_holds() {
            // The market says nothing about being registered with the economy, and is accepted
            // anyway: Galatia Academy is a real colony under a real faction that vanilla never
            // registers, and moving it to another owner is as much a transfer as any other.
            assertThat(MarketOwnershipTransfer.isReadyForTransfer(
                    buildMarket(mock(FactionAPI.class), false)))
                .isTrue();
        }

        @Test
        void rejects_a_body_carrying_only_survey_data() {
            // The complement of colonisation readiness: there is no owner here to move.
            assertThat(MarketOwnershipTransfer.isReadyForTransfer(
                    buildMarket(mock(FactionAPI.class), true)))
                .isFalse();
        }

        @Test
        void rejects_a_market_no_faction_holds() {

            assertThat(MarketOwnershipTransfer.isReadyForTransfer(buildMarket(null, false)))
                .isFalse();
        }

        @Test
        void rejects_a_null_market() {
            assertThat(MarketOwnershipTransfer.isReadyForTransfer(null))
                .isFalse();
        }
    }

    // A market posed by the two states transfer readiness is decided on: who holds it, and
    // whether it is a planet's condition-only placeholder rather than a colony.
    private static MarketAPI buildMarket(FactionAPI faction, boolean isConditionOnly) {

        var marketMock = mock(MarketAPI.class);

        when(marketMock.getFaction())
            .thenReturn(faction);
        when(marketMock.isPlanetConditionMarketOnly())
            .thenReturn(isConditionOnly);

        return marketMock;
    }
}
