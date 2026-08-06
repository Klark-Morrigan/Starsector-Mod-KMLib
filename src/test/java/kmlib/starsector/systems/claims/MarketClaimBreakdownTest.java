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

    @Nested
    class ComputeTotalScore {

        @Test
        void scoresAColonyStandingAloneOnItsSizeAlone() {

            var claim = new MarketClaimBreakdown(MARKET_NAME, 5, 0, OptionalInt.empty());

            assertThat(claim.computeTotalScore())
                .isEqualTo(5);
        }

        @Test
        void addsOnePointForEverySiblingMarket() {

            var claim = new MarketClaimBreakdown(MARKET_NAME, 5, 2, OptionalInt.empty());

            // A faction's other holdings never join its score directly - they are worth a point
            // apiece to the market that stands for it, which is the whole sibling term.
            assertThat(claim.computeTotalScore())
                .isEqualTo(7);
        }

        @Test
        void addsTheFlatBonusAGarrisonEarns() {

            var claim = new MarketClaimBreakdown(MARKET_NAME, 5, 0, OptionalInt.of(10));

            assertThat(claim.computeTotalScore())
                .isEqualTo(15);
        }

        @Test
        void addsTheSizeSiblingAndGarrisonTermsTogether() {

            var claim = new MarketClaimBreakdown(MARKET_NAME, 5, 2, OptionalInt.of(10));

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

            var claim = new MarketClaimBreakdown(MARKET_NAME, 5, 0, null);

            assertThat(claim.militaryBonus())
                .isEmpty();
            assertThat(claim.computeTotalScore())
                .isEqualTo(5);
        }

        @Test
        void carriesTheColonyItWasBuiltFrom() {

            var claim = new MarketClaimBreakdown(MARKET_NAME, 5, 2, OptionalInt.of(10));

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
}
