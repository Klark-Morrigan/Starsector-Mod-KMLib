package kmlib.starsector.systems.claims;

import kmlib.starsector.entities.EntityMapIcon;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.awt.Color;
import java.util.Optional;
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

    // The market is one the player has found. Nothing here is about what a box may name, and the
    // sum the whole suite is about is the same either way - the arithmetic reads neither this nor
    // how the contest met the market.
    private static final boolean IS_KNOWN_TO_PLAYER = true;

    // The glyph the sector map marks the colony's entity with. Carried through the record untouched,
    // no case here being about what a surface goes on to draw with it.
    private static final Optional<EntityMapIcon> MARKET_ICON = Optional.of(
        new EntityMapIcon("graphics/warroom/icon_planet.png", new Color(120, 200, 90)));

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
        void readsAnAbsentIconGivenAsNullAsNoIcon() {
            // A hand-built market states its terms and rarely its glyph, so an unstated icon has to
            // mean an unmarked colony rather than fail late where a line is being composed.
            var claim = new MarketClaimBreakdown(
                MARKET_NAME,
                null,
                FIRST_LISTED,
                IS_KNOWN_TO_PLAYER,
                ContestAdmission.WEIGHED,
                PLAIN_MARKET_SIZE,
                NO_SIBLING_MARKETS,
                OptionalInt.empty());

            assertThat(claim.marketIcon())
                .isEmpty();
        }

        @Test
        void readsAnAbsentAdmissionGivenAsNullAsTheWeighedOne() {
            // The ordinary market is the one a hand-built case leaves unstated, so an unstated
            // admission has to mean the competitor rather than fail late on a null.
            var claim = buildClaimAdmittedAs(null);

            assertThat(claim.isScoredOnItsOwnAccount())
                .isTrue();
            assertThat(claim.isHiddenMarket())
                .isFalse();
            assertThat(claim.isOffEconomyMarket())
                .isFalse();
        }

        @Test
        void carriesTheColonyItWasBuiltFrom() {

            var claim = buildClaim(5, 2, OptionalInt.of(10));

            // Every term survives the sum, since the box explaining a claim prints them rather
            // than the total the map paints its fill by - and so does what identifies the colony
            // they belong to, name and map glyph alike, a term printed against no colony being
            // no account at all.
            assertThat(claim.marketName())
                .isEqualTo("Chicomoztoc");
            assertThat(claim.marketIcon())
                .contains(new EntityMapIcon(
                    "graphics/warroom/icon_planet.png",
                    new Color(120, 200, 90)));
            assertThat(claim.marketSize())
                .isEqualTo(5);
            assertThat(claim.siblingMarketCount())
                .isEqualTo(2);
            assertThat(claim.militaryBonus())
                .hasValue(10);
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
            MARKET_ICON,
            FIRST_LISTED,
            IS_KNOWN_TO_PLAYER,
            ContestAdmission.WEIGHED,
            marketSize,
            siblingMarketCount,
            militaryBonus);
    }

    // The same market posed under a stated admission, for the case about what an unstated one is
    // read as. Its terms are the plainest there are, no case about admission being about the sum.
    private static MarketClaimBreakdown buildClaimAdmittedAs(ContestAdmission admission) {
        return new MarketClaimBreakdown(
            MARKET_NAME,
            MARKET_ICON,
            FIRST_LISTED,
            IS_KNOWN_TO_PLAYER,
            admission,
            PLAIN_MARKET_SIZE,
            NO_SIBLING_MARKETS,
            OptionalInt.empty());
    }
}
