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

    @Nested
    class Score {

        @Test
        void readsTheTotalOfTheMarketTheStandingRestsOn() {

            var standing = new FactionClaimScore(
                HEGEMONY,
                true,
                new MarketClaimBreakdown("Chicomoztoc", 5, 2, OptionalInt.of(10)),
                List.of());

            assertThat(standing.score())
                .isEqualTo(17);
        }

        @Test
        void ignoresTheOtherMarketsTheFactionHolds() {

            var standing = new FactionClaimScore(
                HEGEMONY,
                true,
                new MarketClaimBreakdown("Chicomoztoc", 5, 1, OptionalInt.empty()),
                List.of(new MarketClaimBreakdown("Kazeron", 3, 1, OptionalInt.empty())));

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
            otherMarkets.add(new MarketClaimBreakdown("Kazeron", 3, 1, OptionalInt.empty()));

            var standing = new FactionClaimScore(
                HEGEMONY,
                true,
                new MarketClaimBreakdown("Chicomoztoc", 5, 1, OptionalInt.empty()),
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
                true,
                new MarketClaimBreakdown("Chicomoztoc", 5, 0, OptionalInt.empty()),
                List.of(new MarketClaimBreakdown("Kazeron", 3, 1, OptionalInt.empty())));

            assertThatThrownBy(() -> standing.otherMarkets().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void readsAbsentOtherMarketsAsNone() {

            var standing = new FactionClaimScore(
                HEGEMONY,
                true,
                new MarketClaimBreakdown("Chicomoztoc", 5, 0, OptionalInt.empty()),
                null);

            assertThat(standing.otherMarkets())
                .isEmpty();
        }
    }
}
