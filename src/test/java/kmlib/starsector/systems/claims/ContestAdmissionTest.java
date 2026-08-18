package kmlib.starsector.systems.claims;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the one question every reader of a finished contest asks of a market: did the mechanic weigh
 * it. Two independent facts answer it and they answer it the same way, so a reader that had to test
 * both itself would be one forgotten clause away from printing a score for a market that took no
 * part - which is the drift this value exists to make impossible.
 */
final class ContestAdmissionTest {

    private static final boolean IS_HIDDEN = true;
    private static final boolean IS_NOT_HIDDEN = false;
    private static final boolean IS_OFF_ECONOMY = true;
    private static final boolean IS_NOT_OFF_ECONOMY = false;

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
