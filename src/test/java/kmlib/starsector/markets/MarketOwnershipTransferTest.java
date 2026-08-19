package kmlib.starsector.markets;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link MarketOwnershipTransfer#isReadyForTransfer}. The cases live in a
 * {@link Nested} group so the suite reports as a per-method tree; the markets they are posed
 * against are {@link MarketStateFixture}'s, named for the shape each one is.
 */
final class MarketOwnershipTransferTest {

    @Nested
    class IsReadyForTransfer {

        @Test
        void accepts_a_colony_a_faction_holds() {

            assertThat(MarketOwnershipTransfer.isReadyForTransfer(
                    MarketStateFixture.buildColony("hegemony")))
                .isTrue();
        }

        @Test
        void accepts_a_colony_the_economy_does_not_list() {
            // Galatia Academy is a real colony under a real faction that vanilla never
            // registers, and moving it to another owner is as much a transfer as any other -
            // which is why registration is no part of this read.
            assertThat(MarketOwnershipTransfer.isReadyForTransfer(
                    MarketStateFixture.buildColonyUnlistedByEconomy("independent")))
                .isTrue();
        }

        @Test
        void rejects_a_body_carrying_only_survey_data() {
            // The complement of colonisation readiness: there is no owner here to move.
            assertThat(MarketOwnershipTransfer.isReadyForTransfer(
                    MarketStateFixture.buildColonisableBody()))
                .isFalse();
        }

        @Test
        void rejects_a_market_no_faction_holds() {

            assertThat(MarketOwnershipTransfer.isReadyForTransfer(
                    MarketStateFixture.buildUnownedMarket()))
                .isFalse();
        }

        @Test
        void rejects_a_null_market() {
            assertThat(MarketOwnershipTransfer.isReadyForTransfer(null))
                .isFalse();
        }
    }
}
