package kmlib.starsector.systems.claims;

import kmlib.starsector.colonies.ColonyKind;
import kmlib.starsector.entities.EntityNameplate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the three guarantees a weighed standing carries: its score is the market it rests on rather
 * than a second copy of that sum, its holdings read back in the order the system's listing reaches
 * them however the two components split them, and the markets it lists cannot change under a
 * reader. A stored score would be free to drift from the terms printed beneath it, which is exactly
 * the disagreement between a claim and its explanation the breakdown exists to rule out.
 */
final class WeighedClaimStandingTest {

    private static final String HEGEMONY = "hegemony";

    // Where each market falls in the system's listing. No case here poses a tie, so the three are
    // simply told apart - and the standing market is deliberately not always the first of them,
    // since which market carried the score and where the economy listed it are separate facts.
    private static final int FIRST_LISTED = 1;
    private static final int SECOND_LISTED = 2;
    private static final int THIRD_LISTED = 3;

    // Both markets are ones the player has found, and both are ones the mechanic weighed. Nothing
    // here is about what a box may name or which market competes, and a standing carries the same
    // score either way - the score reads neither.
    private static final boolean IS_KNOWN_TO_PLAYER = true;

    private static final boolean IS_TERRITORIAL = true;

    @Nested
    class Score {

        @Test
        void readsTheTotalOfTheMarketTheStandingRestsOn() {

            var standing = new WeighedClaimStanding(
                HEGEMONY,
                IS_TERRITORIAL,
                buildMarket("Chicomoztoc", FIRST_LISTED, 5, 2, OptionalInt.of(10)),
                List.of());

            assertThat(standing.score())
                .isEqualTo(17);
        }

        @Test
        void ignoresTheOtherMarketsTheFactionHolds() {

            var standing = new WeighedClaimStanding(
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
    class ReadHeldMarkets {

        @Test
        void listsEveryMarketInTheOrderTheEconomyListsThem() {
            // The standing market is the second of the three, so leading the read with it would
            // report an order the system's listing never had - and a reader counting or naming a
            // faction's colonies is reading the listing, not the ranking that picked a winner.
            var standing = new WeighedClaimStanding(
                HEGEMONY,
                IS_TERRITORIAL,
                buildMarket("Chicomoztoc", SECOND_LISTED, 5, 2, OptionalInt.empty()),
                List.of(
                    buildMarket("Sindria", FIRST_LISTED, 1, 2, OptionalInt.empty()),
                    buildMarket("Kazeron", THIRD_LISTED, 3, 2, OptionalInt.empty())));

            assertThat(standing.readHeldMarkets())
                .extracting(market -> market.marketNameplate().displayName())
                .containsExactly("Sindria", "Chicomoztoc", "Kazeron");
        }

        @Test
        void listsTheStandingMarketAloneForAFactionHoldingNothingElse() {

            var standing = new WeighedClaimStanding(
                HEGEMONY,
                IS_TERRITORIAL,
                buildMarket("Chicomoztoc", FIRST_LISTED, 5, 0, OptionalInt.empty()),
                List.of());

            assertThat(standing.readHeldMarkets())
                .extracting(market -> market.marketNameplate().displayName())
                .containsExactly("Chicomoztoc");
        }

        @Test
        void rejectsAnAttemptToChangeTheMarketsItLists() {

            var standing = new WeighedClaimStanding(
                HEGEMONY,
                IS_TERRITORIAL,
                buildMarket("Chicomoztoc", FIRST_LISTED, 5, 1, OptionalInt.empty()),
                List.of(buildMarket("Kazeron", SECOND_LISTED, 3, 1, OptionalInt.empty())));

            assertThatThrownBy(() -> standing.readHeldMarkets().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class Construct {

        @Test
        void keepsTheMarketsItWasBuiltWithWhenTheSourceListChangesLater() {

            var otherMarkets = new ArrayList<MarketClaimBreakdown>();
            otherMarkets.add(buildMarket("Kazeron", SECOND_LISTED, 3, 1, OptionalInt.empty()));

            var standing = new WeighedClaimStanding(
                HEGEMONY,
                IS_TERRITORIAL,
                buildMarket("Chicomoztoc", FIRST_LISTED, 5, 1, OptionalInt.empty()),
                otherMarkets);

            otherMarkets.clear();

            assertThat(standing.otherMarkets())
                .extracting(market -> market.marketNameplate().displayName())
                .containsExactly("Kazeron");
        }

        @Test
        void rejectsAnAttemptToChangeTheMarketsItLists() {

            var standing = new WeighedClaimStanding(
                HEGEMONY,
                IS_TERRITORIAL,
                buildMarket("Chicomoztoc", FIRST_LISTED, 5, 0, OptionalInt.empty()),
                List.of(buildMarket("Kazeron", SECOND_LISTED, 3, 1, OptionalInt.empty())));

            assertThatThrownBy(() -> standing.otherMarkets().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void readsAbsentOtherMarketsAsNone() {

            var standing = new WeighedClaimStanding(
                HEGEMONY,
                IS_TERRITORIAL,
                buildMarket("Chicomoztoc", FIRST_LISTED, 5, 0, OptionalInt.empty()),
                null);

            assertThat(standing.otherMarkets())
                .isEmpty();
        }
    }

    // One of the faction's markets, stated by what a case here varies. Whether the player has found
    // it is the same throughout, and spelled at each call it would bury the terms that matter. The
    // colony is marked with no glyph: what identifies a market beyond its name is nothing a standing
    // reads, so stating one would say a case turns on it.
    private static MarketClaimBreakdown buildMarket(
            String marketName,
            int listingPosition,
            int marketSize,
            int siblingMarketCount,
            OptionalInt militaryBonus) {

        return new MarketClaimBreakdown(
            EntityNameplate.createUnmarkedNameplate(marketName),
            ColonyKind.COLONY,
            listingPosition,
            IS_KNOWN_TO_PLAYER,
            ContestAdmission.WEIGHED,
            marketSize,
            siblingMarketCount,
            militaryBonus);
    }
}
