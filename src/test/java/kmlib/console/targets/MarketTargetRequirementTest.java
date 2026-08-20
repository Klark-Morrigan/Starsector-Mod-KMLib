package kmlib.console.targets;

import kmlib.starsector.markets.MarketStateFixture;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the two requirements the console offers - what each admits, and the phrase a refusal
 * names it by, that phrase being what every message about a rejected target is worded from. The
 * cases live in {@link Nested} groups so the suite reports as a per-method tree; the markets
 * they are posed against are {@link MarketStateFixture}'s.
 */
final class MarketTargetRequirementTest {

    @Nested
    class IsMetBy {

        @Test
        void admits_a_body_carrying_only_survey_data_as_colonisable() {

            assertThat(MarketTargetRequirement.COLONISABLE_BODY.isMetBy(
                    MarketStateFixture.buildColonisableBody()))
                .isTrue();
        }

        @Test
        void refuses_an_existing_colony_as_colonisable() {

            assertThat(MarketTargetRequirement.COLONISABLE_BODY.isMetBy(
                    MarketStateFixture.buildColony("hegemony")))
                .isFalse();
        }

        @Test
        void admits_a_colony_a_faction_holds_as_transferable() {

            assertThat(MarketTargetRequirement.EXISTING_COLONY.isMetBy(
                    MarketStateFixture.buildColony("hegemony")))
                .isTrue();
        }

        @Test
        void admits_a_colony_the_economy_does_not_list_as_transferable() {
            // Galatia Academy is a real colony under a real faction that vanilla never
            // registers, and moving it to another owner is as much a transfer as any other -
            // which is why registration is no part of the rule.
            assertThat(MarketTargetRequirement.EXISTING_COLONY.isMetBy(
                    MarketStateFixture.buildColonyUnlistedByEconomy("independent")))
                .isTrue();
        }

        @Test
        void refuses_a_market_no_faction_holds_as_transferable() {

            assertThat(MarketTargetRequirement.EXISTING_COLONY.isMetBy(
                    MarketStateFixture.buildUnownedMarket()))
                .isFalse();
        }

        @Test
        void refuses_a_body_carrying_only_survey_data_as_transferable() {
            // The two requirements are the two sides of one axis - whether the place is settled -
            // which is what lets one resolver serve both commands.
            assertThat(MarketTargetRequirement.EXISTING_COLONY.isMetBy(
                    MarketStateFixture.buildColonisableBody()))
                .isFalse();
        }

        @Test
        void refuses_a_derelict_station_flying_the_neutral_flag_as_transferable() {
            // A hulk carries a faction like any other market, and it is the neutral one - so a
            // requirement asking only whether some faction held the place would offer a derelict
            // as a colony to hand out.
            assertThat(MarketTargetRequirement.EXISTING_COLONY.isMetBy(
                    MarketStateFixture.buildAbandonedStation()))
                .isFalse();
        }

        @Test
        void refuses_a_decivilised_world_as_transferable() {

            assertThat(MarketTargetRequirement.EXISTING_COLONY.isMetBy(
                    MarketStateFixture.buildDecivilisedWorld()))
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
}
