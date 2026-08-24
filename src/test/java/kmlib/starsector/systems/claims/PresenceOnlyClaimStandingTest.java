package kmlib.starsector.systems.claims;

import kmlib.starsector.entities.EntityNameplate;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Pins the one thing a presence-only standing must never do: report a number. It exists for a
 * faction the contest weighed nothing for, so anything other than nought would be a score nobody
 * worked out - and, since the mechanic's lead changes only on a score strictly greater than nought,
 * a number here is the one way a widening meant for display could move a claimant.
 */
final class PresenceOnlyClaimStandingTest {

    private static final String CRUSADER_PLAN = "crusader_plan";

    // Where each colony falls in the system's listing. The two are simply told apart; no case here
    // poses a tie, there being no score for one to be settled on.
    private static final int FIRST_LISTED = 1;
    private static final int SECOND_LISTED = 2;

    // Both colonies are ones the player has found, this being nothing a standing itself reads.
    private static final boolean IS_KNOWN_TO_PLAYER = true;

    private static final boolean IS_TERRITORIAL = true;

    // The sibling term never enters a case here: a standing that reports nought reports it whatever
    // the terms beneath it say, which is the whole point of the record.
    private static final int NO_SIBLING_MARKETS = 0;

    @Nested
    class Score {

        @Test
        void readsNoughtHoweverLargeTheColoniesBehindItAre() {

            var standing = new PresenceOnlyClaimStanding(
                CRUSADER_PLAN,
                IS_TERRITORIAL,
                List.of(buildMarket("Cinis Beta", FIRST_LISTED, 9, ContestAdmission.HIDDEN)));

            // A size-9 base out-sizes anything the sector holds and still counts for nothing: the
            // walk never weighed it, so there is no sum to report and printing what it would have
            // been worth would put a faction that took no part above the one that took the system.
            assertThat(standing.score())
                .isZero();
        }

        @Test
        void readsNoughtForEitherKindOfUnweighedColony() {

            var standing = new PresenceOnlyClaimStanding(
                CRUSADER_PLAN,
                IS_TERRITORIAL,
                List.of(
                    buildMarket("Cinis Beta", FIRST_LISTED, 6, ContestAdmission.HIDDEN),
                    buildMarket("Galatia Academy", SECOND_LISTED, 6, ContestAdmission.OFF_ECONOMY)));

            // Concealment and an absence from the economy's listing suppress scoring in different
            // ways, and neither is a discount: both leave the faction at the same nought.
            assertThat(standing.score())
                .isZero();
        }
    }

    @Nested
    class ReadHeldMarkets {

        @Test
        void listsTheColoniesInTheOrderTheListingReachesThem() {

            var standing = new PresenceOnlyClaimStanding(
                CRUSADER_PLAN,
                IS_TERRITORIAL,
                List.of(
                    buildMarket("Cinis Beta", FIRST_LISTED, 6, ContestAdmission.HIDDEN),
                    buildMarket("Galatia Academy", SECOND_LISTED, 4, ContestAdmission.OFF_ECONOMY)));

            assertThat(standing.readHeldMarkets())
                .extracting(market -> market.marketNameplate().displayName())
                .containsExactly("Cinis Beta", "Galatia Academy");
        }

        @Test
        void rejectsAnAttemptToChangeTheColoniesItLists() {

            var standing = new PresenceOnlyClaimStanding(
                CRUSADER_PLAN,
                IS_TERRITORIAL,
                List.of(buildMarket("Cinis Beta", FIRST_LISTED, 6, ContestAdmission.HIDDEN)));

            assertThatThrownBy(() -> standing.readHeldMarkets().clear())
                .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    @Nested
    class Construct {

        @Test
        void keepsTheColoniesItWasBuiltWithWhenTheSourceListChangesLater() {

            var unweighedMarkets = new ArrayList<MarketClaimBreakdown>();
            unweighedMarkets.add(buildMarket("Cinis Beta", FIRST_LISTED, 6, ContestAdmission.HIDDEN));

            var standing =
                new PresenceOnlyClaimStanding(CRUSADER_PLAN, IS_TERRITORIAL, unweighedMarkets);

            unweighedMarkets.clear();

            assertThat(standing.unweighedMarkets())
                .extracting(market -> market.marketNameplate().displayName())
                .containsExactly("Cinis Beta");
        }

        @Test
        void readsAbsentColoniesAsNone() {

            var standing = new PresenceOnlyClaimStanding(CRUSADER_PLAN, IS_TERRITORIAL, null);

            assertThat(standing.unweighedMarkets())
                .isEmpty();
        }
    }

    // One of the faction's colonies, stated by what a case here varies: its name, its place in the
    // listing, a size no case expects to see reported, and which of the two ways it went unweighed.
    // Marked with no glyph, what identifies a colony beyond its name being nothing a standing reads.
    private static MarketClaimBreakdown buildMarket(
            String marketName,
            int listingPosition,
            int marketSize,
            ContestAdmission admission) {

        return new MarketClaimBreakdown(
            EntityNameplate.createUnmarkedNameplate(marketName),
            marketName,
            listingPosition,
            IS_KNOWN_TO_PLAYER,
            admission,
            marketSize,
            NO_SIBLING_MARKETS,
            OptionalInt.empty());
    }
}
