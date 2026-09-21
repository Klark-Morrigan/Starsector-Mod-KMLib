package kmlib.mods.console.commands.targets;

import kmlib.testfixtures.starsector.markets.MarketStateFixture;

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
        void admitsABodyCarryingOnlySurveyDataAsColonisable() {

            assertThat(MarketTargetRequirement.COLONISABLE_BODY.isMetBy(
                    MarketStateFixture.buildColonisableBody()))
                .isTrue();
        }

        @Test
        void refusesAnExistingColonyAsColonisable() {

            assertThat(MarketTargetRequirement.COLONISABLE_BODY.isMetBy(
                    MarketStateFixture.buildColony("hegemony")))
                .isFalse();
        }

        @Test
        void admitsAColonyAFactionHoldsAsTransferable() {

            assertThat(MarketTargetRequirement.EXISTING_COLONY.isMetBy(
                    MarketStateFixture.buildColony("hegemony")))
                .isTrue();
        }

        @Test
        void admitsAColonyTheEconomyDoesNotListAsTransferable() {
            // Galatia Academy is a real colony under a real faction that vanilla never
            // registers, and moving it to another owner is as much a transfer as any other -
            // which is why registration is no part of the rule.
            assertThat(MarketTargetRequirement.EXISTING_COLONY.isMetBy(
                    MarketStateFixture.buildColonyUnlistedByEconomy("independent")))
                .isTrue();
        }

        @Test
        void refusesAMarketNoFactionHoldsAsTransferable() {

            assertThat(MarketTargetRequirement.EXISTING_COLONY.isMetBy(
                    MarketStateFixture.buildUnownedMarket()))
                .isFalse();
        }

        @Test
        void refusesABodyCarryingOnlySurveyDataAsTransferable() {
            // The two requirements are the two sides of one axis - whether the place is settled -
            // which is what lets one resolver serve both commands.
            assertThat(MarketTargetRequirement.EXISTING_COLONY.isMetBy(
                    MarketStateFixture.buildColonisableBody()))
                .isFalse();
        }

        @Test
        void refusesADerelictStationFlyingTheNeutralFlagAsTransferable() {
            // A derelict carries a faction like any other market, and it is the neutral one - so a
            // requirement asking only whether some faction held the place would offer a derelict
            // as a colony to hand out.
            assertThat(MarketTargetRequirement.EXISTING_COLONY.isMetBy(
                    MarketStateFixture.buildAbandonedStation()))
                .isFalse();
        }

        @Test
        void refusesADecivilisedWorldAsTransferable() {

            assertThat(MarketTargetRequirement.EXISTING_COLONY.isMetBy(
                    MarketStateFixture.buildDecivilisedWorld()))
                .isFalse();
        }
    }

    @Nested
    class RequirementPhrase {

        @Test
        void wordsTheColonisableBodyRequirementWithItsOwnArticle() {

            assertThat(MarketTargetRequirement.COLONISABLE_BODY.requirementPhrase())
                .isEqualTo("a body ready for colonisation");
        }

        @Test
        void wordsTheExistingColonyRequirementWithItsOwnArticle() {
            // "an", not "a": the article belongs to the phrase, so a message reads as a
            // sentence without the caller having to know which one this requirement takes.
            assertThat(MarketTargetRequirement.EXISTING_COLONY.requirementPhrase())
                .isEqualTo("an existing colony");
        }
    }
}
