package kmlib.starsector.systems.claims;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the two nested questions a reader of a finished contest asks of a market: did the mechanic
 * weigh it, and did the mechanic reach it at all. Two independent facts answer both, so a reader that
 * had to test them itself would be one forgotten clause away from printing a score for a market that
 * took no part - which is the drift this value exists to make impossible.
 *
 * <p>The concealed colony is the case that makes them two questions rather than one: it is never
 * weighed and is counted as a sibling all the same, so a reader asking the narrower question of it and
 * a reader asking the broader one are owed different answers.
 */
final class ContestAdmissionTest {

    private static final boolean IS_HIDDEN = true;
    private static final boolean IS_NOT_HIDDEN = false;
    private static final boolean IS_OFF_ECONOMY = true;
    private static final boolean IS_NOT_OFF_ECONOMY = false;

    @Nested
    class IsCountedTowardSiblings {

        @Test
        void countsAColonyHeldInTheOpenAndListedByTheEconomy() {

            assertThat(new ContestAdmission(IS_NOT_HIDDEN, IS_NOT_OFF_ECONOMY)
                    .isCountedTowardSiblings())
                .isTrue();
        }

        @Test
        void countsAConcealedColonyTheEconomyLists() {
            // Where this parts company with the scoring question, and the whole reason it is asked
            // apart: the count walks the economy's listing without caring what is concealed on it, so
            // a base the mechanic refused to weigh is still paid for as somebody's sibling.
            assertThat(new ContestAdmission(IS_HIDDEN, IS_NOT_OFF_ECONOMY)
                    .isCountedTowardSiblings())
                .isTrue();
        }

        @Test
        void passesOverAColonyTheEconomyDoesNotList() {
            // The count walks the economy and nothing else, so a colony left off that listing is not
            // reached for even this - the one way a market takes no part in the contest at all.
            assertThat(new ContestAdmission(IS_NOT_HIDDEN, IS_OFF_ECONOMY)
                    .isCountedTowardSiblings())
                .isFalse();
        }

        @Test
        void passesOverAColonyThatIsBothAtOnce() {
            // Galatia Academy's own shape. Concealment would have left it counted; the absence from
            // the listing is what keeps it out, so the pair is read on the listing alone.
            assertThat(new ContestAdmission(IS_HIDDEN, IS_OFF_ECONOMY)
                    .isCountedTowardSiblings())
                .isFalse();
        }
    }

    @Nested
    class IsScoredOnItsOwnAccount {

        @Test
        void countsAColonyHeldInTheOpenAndListedByTheEconomy() {

            assertThat(new ContestAdmission(IS_NOT_HIDDEN, IS_NOT_OFF_ECONOMY)
                    .isScoredOnItsOwnAccount())
                .isTrue();
        }

        @Test
        void passesOverAConcealedColony() {
            // The mechanic skips it before scoring, so it reaches the contest through the sibling
            // term alone and competes for nothing.
            assertThat(new ContestAdmission(IS_HIDDEN, IS_NOT_OFF_ECONOMY)
                    .isScoredOnItsOwnAccount())
                .isFalse();
        }

        @Test
        void passesOverAColonyTheEconomyDoesNotList() {
            // The mechanic walks the economy, so a colony left off that listing is never reached -
            // a different reason from concealment, and the same answer.
            assertThat(new ContestAdmission(IS_NOT_HIDDEN, IS_OFF_ECONOMY)
                    .isScoredOnItsOwnAccount())
                .isFalse();
        }

        @Test
        void passesOverAColonyThatIsBothAtOnce() {
            // Galatia Academy's own shape: concealed and unregistered together, which is why the
            // two are carried as separate facts rather than folded into one.
            assertThat(new ContestAdmission(IS_HIDDEN, IS_OFF_ECONOMY).isScoredOnItsOwnAccount())
                .isFalse();
        }
    }

    @Nested
    class Weighed {

        @Test
        void namesTheColonyTheMechanicCompetes() {
            // The shorthand every hand-built ordinary market is posed with, so it must be neither
            // exclusion rather than merely one of them.
            assertThat(ContestAdmission.WEIGHED.isHiddenMarket())
                .isFalse();
            assertThat(ContestAdmission.WEIGHED.isOffEconomyMarket())
                .isFalse();
            assertThat(ContestAdmission.WEIGHED.isScoredOnItsOwnAccount())
                .isTrue();
        }
    }

    @Nested
    class Hidden {

        @Test
        void namesTheConcealedColonyTheEconomyStillLists() {
            // Concealed and nothing else: a shorthand that also read as unregistered would keep such
            // a market out of the sibling term, which is the one part of the contest it does reach.
            assertThat(ContestAdmission.HIDDEN.isHiddenMarket())
                .isTrue();
            assertThat(ContestAdmission.HIDDEN.isOffEconomyMarket())
                .isFalse();
        }
    }

    @Nested
    class OffEconomy {

        @Test
        void namesTheOpenColonyTheEconomyDoesNotList() {
            // Unregistered and nothing else: a shorthand that also read as concealed would say the
            // walk skipped a market it never reached, which are two different findings.
            assertThat(ContestAdmission.OFF_ECONOMY.isHiddenMarket())
                .isFalse();
            assertThat(ContestAdmission.OFF_ECONOMY.isOffEconomyMarket())
                .isTrue();
        }
    }
}
