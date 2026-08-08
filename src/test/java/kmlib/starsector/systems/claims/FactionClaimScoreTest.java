package kmlib.starsector.systems.claims;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the two guarantees a standing carries: its score is the market it rests on rather than a
 * second copy of that sum, and the markets it lists cannot change under a reader. A stored score
 * would be free to drift from the terms printed beneath it, which is exactly the disagreement
 * between a claim and its explanation the breakdown exists to rule out.
 */
final class FactionClaimScoreTest {

    private static final String HEGEMONY = "hegemony";

    // Where each market falls in the system's listing. No case here poses a tie, so the two are
    // simply told apart - the standing market ahead of the one beside it.
    private static final int FIRST_LISTED = 1;
    private static final int SECOND_LISTED = 2;

    // Both markets are ones the player has found, held in the open. Nothing here is about what a
    // box may name or which market competes, and a standing carries the same score either way -
    // the score reads neither flag.
    private static final boolean IS_KNOWN_TO_PLAYER = true;
    private static final boolean IS_NOT_HIDDEN = false;

    private static final boolean IS_TERRITORIAL = true;

    @Nested
    class Score {

        @Test
        void readsTheTotalOfTheMarketTheStandingRestsOn() {

            var standing = new FactionClaimScore(
                HEGEMONY,
                IS_TERRITORIAL,
                buildMarket("Chicomoztoc", FIRST_LISTED, 5, 2, OptionalInt.of(10)),
                List.of());

            assertThat(standing.score())
                .isEqualTo(17);
        }

        @Test
        void ignoresTheOtherMarketsTheFactionHolds() {

            var standing = new FactionClaimScore(
                HEGEMONY,
                IS_TERRITORIAL,
                buildMarket("Chicomoztoc", FIRST_LISTED, 5, 1, OptionalInt.empty()),
                List.of(buildMarket("Kazeron", SECOND_LISTED, 3, 1, OptionalInt.empty())));

            // Holdings are never summed: the second colony reaches the contest as the sibling
            // point already inside the standing market's score, not as a score of its own.
            assertThat(standing.score())
                .isEqualTo(6);
        }
    }

    @Nested
    class Construct {

        @Test
        void keepsTheMarketsItWasBuiltWithWhenTheSourceListChangesLater() {

            var otherMarkets = new ArrayList<MarketClaimBreakdown>();
            otherMarkets.add(buildMarket("Kazeron", SECOND_LISTED, 3, 1, OptionalInt.empty()));

            var standing = new FactionClaimScore(
                HEGEMONY,
                IS_TERRITORIAL,
                buildMarket("Chicomoztoc", FIRST_LISTED, 5, 1, OptionalInt.empty()),
                otherMarkets);

            otherMarkets.clear();

            assertThat(standing.otherMarkets())
                .extracting(MarketClaimBreakdown::marketName)
                .containsExactly("Kazeron");
        }

        @Test
        void rejectsAnAttemptToChangeTheMarketsItLists() {

            var standing = new FactionClaimScore(
                HEGEMONY,
                IS_TERRITORIAL,
                buildMarket("Chicomoztoc", FIRST_LISTED, 5, 0, OptionalInt.empty()),
                List.of(buildMarket("Kazeron", SECOND_LISTED, 3, 1, OptionalInt.empty())));

            assertThatThrownBy(() -> standing.otherMarkets().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void readsAbsentOtherMarketsAsNone() {

            var standing = new FactionClaimScore(
                HEGEMONY,
                IS_TERRITORIAL,
                buildMarket("Chicomoztoc", FIRST_LISTED, 5, 0, OptionalInt.empty()),
                null);

            assertThat(standing.otherMarkets())
                .isEmpty();
        }
    }

    // One of the faction's markets, stated by what a case here varies. Whether the player has found
    // it is the same throughout, and spelled at each call it would bury the terms that matter.
    private static MarketClaimBreakdown buildMarket(
            String marketName,
            int listingPosition,
            int marketSize,
            int siblingMarketCount,
            OptionalInt militaryBonus) {

        return new MarketClaimBreakdown(
            marketName,
            listingPosition,
            IS_KNOWN_TO_PLAYER,
            IS_NOT_HIDDEN,
            marketSize,
            siblingMarketCount,
            militaryBonus);
    }
}
