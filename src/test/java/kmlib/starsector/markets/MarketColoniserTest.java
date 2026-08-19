package kmlib.starsector.markets;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the contract of {@link MarketColoniser#isReadyForColonisation}. The cases live in a
 * {@link Nested} group so the suite reports as a per-method tree; the markets they are posed
 * against are {@link MarketStateFixture}'s, named for the shape each one is.
 */
final class MarketColoniserTest {

    @Nested
    class IsReadyForColonisation {

        @Test
        void accepts_a_body_carrying_only_survey_data() {
            // What procgen leaves on every uninhabited world: a market holding the planet's
            // conditions and nothing else, hung on the entity and never registered.
            assertThat(MarketColoniser.isReadyForColonisation(
                    MarketStateFixture.buildColonisableBody()))
                .isTrue();
        }

        @Test
        void rejects_a_colony_that_already_exists() {

            assertThat(MarketColoniser.isReadyForColonisation(
                    MarketStateFixture.buildColony("hegemony")))
                .isFalse();
        }

        @Test
        void rejects_a_colony_the_economy_does_not_list() {
            // Vanilla builds Galatia Academy this way on purpose. Only the condition-only arm
            // rejects it, which is why registration cannot be the whole of the read.
            assertThat(MarketColoniser.isReadyForColonisation(
                    MarketStateFixture.buildColonyUnlistedByEconomy("independent")))
                .isFalse();
        }

        @Test
        void rejects_survey_data_the_economy_already_lists() {
            // Nothing vanilla builds, and founding on it would register the market a second
            // time - so the registration arm answers a shape the flag alone would admit.
            assertThat(MarketColoniser.isReadyForColonisation(
                    MarketStateFixture.buildRegisteredSurveyData()))
                .isFalse();
        }

        @Test
        void rejects_a_null_market() {
            assertThat(MarketColoniser.isReadyForColonisation(null))
                .isFalse();
        }
    }
}
