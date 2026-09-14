package kmlib.starsector.markets.ownership;

import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.extensions.DeclinedWork;
import kmlib.extensions.ExecutedWork;
import kmlib.extensions.FallbackToDefaults;
import kmlib.extensions.WorkOutcome;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * Pins what a colony's counters are offered to: that an install with nothing installed decides
 * nothing, that the installed rule's verdict is the answer, that both owners reach it, that a
 * second registration replaces the first outright, and that a rule which had to decide them and
 * did not fails the run.
 *
 * <p>The point is one per running game, so each case empties it before and after itself - a rule
 * left behind would be offered ownership changes in whatever suite ran next.
 *
 * <p>Rules here record that they were offered and answer a stated verdict, rather than opening or
 * closing anything: what counters an owner's colony trades over is
 * {@code MarketOwnershipRuleTest}'s, and what this pins is only who gets asked and what comes of
 * it.
 */
final class OwnerSubmarketRulesTest {

    private static final String INCOMING_OWNER_ID = "hegemony";
    private static final String OUTGOING_OWNER_ID = "persean_league";

    private List<String> offeredTo;
    private MarketAPI marketMock;

    @BeforeEach
    void setUp() {

        OwnerSubmarketRules.clearRule();

        offeredTo = new ArrayList<>();
        marketMock = mock(MarketAPI.class);
    }

    @AfterEach
    void tearDown() {
        OwnerSubmarketRules.clearRule();
    }

    @Nested
    class OfferSubmarkets {

        @Test
        void decidesNothingWhereNothingIsInstalled() {
            // The answer on every install running no mod with a submarket rule of its own, and the
            // one that leaves this library's own table to say what an owner's colony trades over.
            assertThat(offerSubmarkets().wasExecuted())
                .isFalse();
        }

        @Test
        void answersThatTheCountersWereDecidedWhereTheInstalledRuleDecidesThem() {

            installPermittingFallback("Some Mod", buildRuleNamed("installed", true));

            assertThat(offerSubmarkets().wasExecuted())
                .isTrue();
            assertThat(offeredTo)
                .containsExactly("installed");
        }

        @Test
        void leavesTheCountersWhereTheInstalledRuleDeclines() {

            installPermittingFallback("Some Mod", buildRuleNamed("installed", false));

            assertThat(offerSubmarkets().wasExecuted())
                .isFalse();
            assertThat(offeredTo)
                .containsExactly("installed");
        }

        @Test
        void failsTheChangeWhereARuleThatHadToDecideThemDeclines() {
            // A mod whose colonies trade over counters this library's table does not know about
            // has no correct outcome from that table, so the run stops here rather than opening
            // counters the mod would never have opened.
            installForbiddingFallback("Total Conversion", buildRuleNamed("installed", false));

            assertThatThrownBy(OwnerSubmarketRulesTest.this::offerSubmarkets)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Total Conversion");
        }

        @Test
        void handsTheInstalledRuleBothOwners() {
            // A rule may restock only where the colony has actually changed hands, which the
            // incoming ID alone cannot say - so both IDs have to reach it.
            var seenOwners = new ArrayList<String>();

            installPermittingFallback("Some Mod", (market, oldOwnerId, newOwnerId) -> {
                seenOwners.add(oldOwnerId);
                seenOwners.add(newOwnerId);
                return new ExecutedWork();
            });

            offerSubmarkets();

            assertThat(seenOwners)
                .containsExactly(OUTGOING_OWNER_ID, INCOMING_OWNER_ID);
        }
    }

    @Nested
    class RegisterRule {

        @Test
        void offersTheCountersToTheLastRuleRegistered() {
            // One rule decides a colony's counters on an install: two of them opening and closing
            // the same counters would leave whichever ran last in charge of a colony neither
            // describes, so the last registered decides them outright.
            installPermittingFallback("First Mod", buildRuleNamed("first", true));
            installPermittingFallback("Second Mod", buildRuleNamed("second", true));

            offerSubmarkets();

            assertThat(offeredTo)
                .containsExactly("second");
        }

        @Test
        void answersWithTheNameTheInstalledRuleWasRegisteredUnder() {

            installPermittingFallback("Some Mod", buildRuleNamed("installed", true));

            assertThat(OwnerSubmarketRules.readRuleName())
                .isEqualTo("Some Mod");
        }
    }

    @Nested
    class ReadRule {

        @Test
        void answersWithWhatThisInstallRegistered() {

            var ownerSubmarketRule = buildRuleNamed("installed", false);

            installPermittingFallback("Some Mod", ownerSubmarketRule);

            assertThat(OwnerSubmarketRules.readRule())
                .isSameAs(ownerSubmarketRule);
        }

        @Test
        void isAbsentWhereNothingRegisteredOne() {

            assertThat(OwnerSubmarketRules.readRule())
                .isNull();
            assertThat(OwnerSubmarketRules.readRuleName())
                .isNull();
        }
    }

    private WorkOutcome offerSubmarkets() {
        return OwnerSubmarketRules.offerSubmarkets(
            marketMock,
            OUTGOING_OWNER_ID,
            INCOMING_OWNER_ID);
    }

    private static void installForbiddingFallback(
            String integrationName,
            OwnerSubmarketRule ownerSubmarketRule) {

        OwnerSubmarketRules.registerRule(
            integrationName,
            ownerSubmarketRule,
            FallbackToDefaults.FORBIDDEN);
    }

    private static void installPermittingFallback(
            String integrationName,
            OwnerSubmarketRule ownerSubmarketRule) {

        OwnerSubmarketRules.registerRule(
            integrationName,
            ownerSubmarketRule,
            FallbackToDefaults.PERMITTED);
    }

    // A rule that records having been offered a colony's counters and answers the stated verdict,
    // so a case can assert both who was asked and what came of it.
    private OwnerSubmarketRule buildRuleNamed(String name, boolean decidesTheCounters) {

        return (market, oldOwnerId, newOwnerId) -> {
            offeredTo.add(name);
            return decidesTheCounters ? new ExecutedWork() : new DeclinedWork("this stub declines");
        };
    }
}
