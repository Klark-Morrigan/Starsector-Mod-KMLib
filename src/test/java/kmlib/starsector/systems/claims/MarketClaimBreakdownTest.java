package kmlib.starsector.systems.claims;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Pins the sum a market's claim score is: the three terms vanilla adds, and nothing else. The
 * scalar the contest is settled on is read off this, so a term dropped or double-counted here
 * would move a border on the map while every reader of the parts still read consistently.
 */
final class MarketClaimBreakdownTest {

    private static final String MARKET_NAME = "Chicomoztoc";

    // Where the market falls in the system's listing. No case here poses a tie, so every market
    // built below takes the head of the list.
    private static final int FIRST_LISTED = 1;

    // The market is one the player has found, held in the open. Nothing here is about what a box
    // may name or which market competes, and the sum the whole suite is about is the same either
    // way - the arithmetic reads neither flag.
    private static final boolean IS_KNOWN_TO_PLAYER = true;
    private static final boolean IS_NOT_HIDDEN = false;

    // The market is one the economy lists, which the arithmetic reads no more than it reads the
    // other two. Only the competitor test below turns on it, and it states the flag itself.
    private static final boolean IS_NOT_OFF_ECONOMY = false;

    // The plainest colony there is - a size and nothing else - for the cases about how a contest
    // reached a market rather than about what it came to.
    private static final int PLAIN_MARKET_SIZE = 5;
    private static final int NO_SIBLING_MARKETS = 0;

    @Nested
    class ComputeTotalScore {

        @Test
        void scoresAColonyStandingAloneOnItsSizeAlone() {

            var claim = buildClaim(5, 0, OptionalInt.empty());

            assertThat(claim.computeTotalScore())
                .isEqualTo(5);
        }

        @Test
        void addsOnePointForEverySiblingMarket() {

            var claim = buildClaim(5, 2, OptionalInt.empty());

            // A faction's other holdings never join its score directly - they are worth a point
            // apiece to the market that stands for it, which is the whole sibling term.
            assertThat(claim.computeTotalScore())
                .isEqualTo(7);
        }

        @Test
        void addsTheFlatBonusAGarrisonEarns() {

            var claim = buildClaim(5, 0, OptionalInt.of(10));

            assertThat(claim.computeTotalScore())
                .isEqualTo(15);
        }

        @Test
        void addsTheSizeSiblingAndGarrisonTermsTogether() {

            var claim = buildClaim(5, 2, OptionalInt.of(10));

            // The case the three terms can hide each other in: a rule that dropped one would
            // still add up in each of the cases above, where two of them are nought.
            assertThat(claim.computeTotalScore())
                .isEqualTo(17);
        }
    }

    @Nested
    class Construct {

        @Test
        void readsAnAbsentBonusGivenAsNullAsNoBonus() {

            var claim = buildClaim(5, 0, null);

            assertThat(claim.militaryBonus())
                .isEmpty();
            assertThat(claim.computeTotalScore())
                .isEqualTo(5);
        }

        @Test
        void carriesTheColonyItWasBuiltFrom() {

            var claim = buildClaim(5, 2, OptionalInt.of(10));

            // Every term survives the sum, since the box explaining a claim prints them rather
            // than the total the map paints its fill by.
            assertThat(claim.marketName())
                .isEqualTo("Chicomoztoc");
            assertThat(claim.marketSize())
                .isEqualTo(5);
            assertThat(claim.siblingMarketCount())
                .isEqualTo(2);
            assertThat(claim.militaryBonus())
                .hasValue(10);
        }
    }

    @Nested
    class IsScoredOnItsOwnAccount {

        @Test
        void countsAMarketHeldInTheOpenAndListedByTheEconomy() {

            assertThat(buildClaimReachedAs(false, false).isScoredOnItsOwnAccount())
                .isTrue();
        }

        @Test
        void passesOverAHiddenMarket() {
            // The mechanic skips it before scoring, so it reaches the contest through the sibling
            // term alone and competes for nothing.
            assertThat(buildClaimReachedAs(true, false).isScoredOnItsOwnAccount())
                .isFalse();
        }

        @Test
        void passesOverAMarketTheEconomyDoesNotList() {
            // The mechanic walks the economy, so a colony left off that listing is never reached -
            // a different reason from concealment, and the same answer, which is the whole point
            // of asking it here rather than as two tests at every reader.
            assertThat(buildClaimReachedAs(false, true).isScoredOnItsOwnAccount())
                .isFalse();
        }

        @Test
        void passesOverAMarketThatIsBothAtOnce() {
            // Galatia Academy's own shape: concealed and unregistered together, which is why the
            // two are carried as separate facts rather than folded into one.
            assertThat(buildClaimReachedAs(true, true).isScoredOnItsOwnAccount())
                .isFalse();
        }
    }

    // One market's claim arithmetic, stated by the three terms every case here varies and nothing
    // else: which market it is, whether the player has found it and how the contest reached it are
    // the same throughout, and spelled at each call they would bury the terms the suite is about.
    private static MarketClaimBreakdown buildClaim(
            int marketSize,
            int siblingMarketCount,
            OptionalInt militaryBonus) {

        return new MarketClaimBreakdown(
            MARKET_NAME,
            FIRST_LISTED,
            IS_KNOWN_TO_PLAYER,
            IS_NOT_HIDDEN,
            IS_NOT_OFF_ECONOMY,
            marketSize,
            siblingMarketCount,
            militaryBonus);
    }

    // The same market posed by the two ways a contest can carry one without weighing it, which is
    // the whole of what the competitor test reads.
    private static MarketClaimBreakdown buildClaimReachedAs(
            boolean isHiddenMarket,
            boolean isOffEconomyMarket) {

        return new MarketClaimBreakdown(
            MARKET_NAME,
            FIRST_LISTED,
            IS_KNOWN_TO_PLAYER,
            isHiddenMarket,
            isOffEconomyMarket,
            PLAIN_MARKET_SIZE,
            NO_SIBLING_MARKETS,
            OptionalInt.empty());
    }
}
