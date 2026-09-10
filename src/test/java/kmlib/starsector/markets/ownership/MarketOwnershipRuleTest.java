package kmlib.starsector.markets.ownership;

import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import kmlib.extensions.DeclinedWork;
import kmlib.extensions.ExecutedWork;
import kmlib.starsector.markets.MarketOwnershipFixture;
import kmlib.testfixtures.starsector.settings.ModStateScopes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static kmlib.testfixtures.starsector.settings.StubbedModIds.NEXERELIN;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;

/**
 * Pins the contract of {@link MarketOwnershipRule#applyOwnership}. The cases live in a {@link Nested}
 * group so the suite reports as a per-method tree; the colonies they are posed against are
 * {@link MarketOwnershipFixture}'s, which answer from what has been done to them.
 *
 * <p>The round trip - player, faction, player again - is the case the rest exist around. An
 * ownership rule written as a sequence of additions passes every one-way case and still leaves a
 * colony carrying the last owner's counters, so a direction is only proven by being reversed.
 *
 * <p>The counters an installed mod may decide instead are posed through the seam rather than by
 * installing one, so both branches run on a machine holding whichever mods it happens to hold. What
 * the mod's own rule then does to a colony is verified in play, that rule being reached only once
 * the presence gate has passed.
 */
final class MarketOwnershipRuleTest {

    // Vanilla's own id for the tariff modifier, spelt here rather than read off the class under
    // test: what is being pinned is that the rule writes the key vanilla's colonisation writes, and
    // a case reading the key from the code it checks would pass under any key at all.
    private static final String DEFAULT_TARIFF_MODIFIER_ID = "default_tariff";

    @Nested
    class ApplyOwnership {

        @Test
        void opensTheCountersAPlayerColonyTradesThrough() {
            // Local resources is the player's own production made buyable, and it replaces the
            // open and black markets an NPC colony trades over. Storage arrives with it because a
            // colony the player holds is one they can store in.
            var market = MarketOwnershipFixture.buildColonyTradingThrough(
                MarketOwnershipFixture.FACTION_OWNER_ID,
                "open_market",
                "black_market");

            MarketOwnershipRule.applyOwnership(market, Factions.PLAYER);

            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .containsExactlyInAnyOrder("local_resources", "storage");
        }

        @Test
        void opensTheCountersAFactionColonyTradesThrough() {

            var market = MarketOwnershipFixture.buildColonyTradingThrough(
                Factions.PLAYER,
                "local_resources");

            MarketOwnershipRule.applyOwnership(market, MarketOwnershipFixture.FACTION_OWNER_ID);

            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .containsExactlyInAnyOrder("open_market", "black_market");
        }

        @Test
        void leavesNoCounterOfThePreviousOwnerSBehindAcrossARoundTrip() {
            // The whole reason the rule is stated per submarket rather than as a sequence: each
            // leg has to undo the one before it, or a colony that changes hands twice ends up
            // trading over both owners' counters at once.
            var market = MarketOwnershipFixture.buildColonyHeldBy(
                MarketOwnershipFixture.FACTION_OWNER_ID);

            MarketOwnershipRule.applyOwnership(market, Factions.PLAYER);

            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .containsExactlyInAnyOrder("local_resources", "storage");

            MarketOwnershipRule.applyOwnership(market, MarketOwnershipFixture.FACTION_OWNER_ID);

            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .containsExactlyInAnyOrder("open_market", "black_market", "storage");

            MarketOwnershipRule.applyOwnership(market, Factions.PLAYER);

            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .containsExactlyInAnyOrder("local_resources", "storage");
        }

        @Test
        void keepsACounterTheIncomingOwnerAlreadyTradesOver() {
            // Opening a counter builds a new one, and whatever was stocked in the old one goes
            // with it. So a submarket already in the state the owner calls for is left where it
            // is rather than closed and opened again - which is why the rule asks each submarket
            // for a verdict and acts only on the ones whose answer it does not already match.
            var market = MarketOwnershipFixture.buildColonyTradingThrough(
                MarketOwnershipFixture.FACTION_OWNER_ID,
                "open_market",
                "black_market");

            var openMarketBefore = market.getSubmarket("open_market");

            MarketOwnershipRule.applyOwnership(market, MarketOwnershipFixture.FACTION_OWNER_ID);

            assertThat(market.getSubmarket("open_market"))
                .isSameAs(openMarketBefore);
        }

        @Test
        void chargesTheIncomingOwnerSTariffOnEveryChange() {
            // Written to the one key on every leg rather than added to, so the rate on the market
            // is the current owner's rather than the sum of everyone who has held the place.
            var market = MarketOwnershipFixture.buildColonyHeldBy(
                MarketOwnershipFixture.FACTION_OWNER_ID);

            MarketOwnershipRule.applyOwnership(market, Factions.PLAYER);

            assertThat(market.getTariff().getFlatStatMod(DEFAULT_TARIFF_MODIFIER_ID).value)
                .isEqualTo(0.3f);

            MarketOwnershipRule.applyOwnership(market, MarketOwnershipFixture.FACTION_OWNER_ID);

            assertThat(market.getTariff().getFlatStatMod(DEFAULT_TARIFF_MODIFIER_ID).value)
                .isEqualTo(0.2f);
        }

        @Test
        void marksTheMarketAsThePlayerSOwnAndUnmarksItAgain() {
            // The flag every player-only rule downstream keys on, and the one this method both
            // sets and has to be able to clear - a rule reading it back to decide who the owner is
            // would never clear it once set.
            var market = MarketOwnershipFixture.buildColonyHeldBy(
                MarketOwnershipFixture.FACTION_OWNER_ID);

            MarketOwnershipRule.applyOwnership(market, Factions.PLAYER);

            assertThat(market.isPlayerOwned())
                .isTrue();

            MarketOwnershipRule.applyOwnership(market, MarketOwnershipFixture.FACTION_OWNER_ID);

            assertThat(market.isPlayerOwned())
                .isFalse();
            assertThat(market.getFactionId())
                .isEqualTo("hegemony");
        }

        @Test
        void fliesTheNewOwnerSFlagOverTheColonyAndEverythingConnectedToIt() {
            // A holding is the body, its station, its relay and its sensor array. An entity left
            // on the old flag keeps reading as the previous owner's property to anything that
            // asks the entity rather than the market.
            var market = MarketOwnershipFixture.buildColonyHeldBy(Factions.PLAYER);

            MarketOwnershipRule.applyOwnership(market, MarketOwnershipFixture.FACTION_OWNER_ID);

            assertThat(MarketOwnershipFixture.readFactionId(market.getPrimaryEntity()))
                .isEqualTo("hegemony");
            assertThat(market.getConnectedEntities())
                .allSatisfy(entity -> assertThat(MarketOwnershipFixture.readFactionId(entity))
                    .isEqualTo("hegemony"));
        }

        @Test
        void fliesTheNewOwnerSFlagOverABodyTheMarketDoesNotListAsConnected() {
            // A market is not obliged to name its own body among its connected entities, so the
            // body is re-flagged in its own right rather than by being swept up with the rest.
            var market = MarketOwnershipFixture.buildColonyNotListingItsOwnBody(Factions.PLAYER);

            MarketOwnershipRule.applyOwnership(market, MarketOwnershipFixture.FACTION_OWNER_ID);

            assertThat(MarketOwnershipFixture.readFactionId(market.getPrimaryEntity()))
                .isEqualTo("hegemony");
        }

        @Test
        void opensAMarketCounterOnAPlayerColonyRunningCommerce() {
            // The one aspect that is not the inverse of the faction side: a player colony trading
            // commercially has an open market too, which a rule derived by negating the faction
            // set would get wrong.
            var market = MarketOwnershipFixture.buildColonyRunning(
                MarketOwnershipFixture.FACTION_OWNER_ID,
                "commerce");

            MarketOwnershipRule.applyOwnership(market, Factions.PLAYER);

            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .containsExactlyInAnyOrder("local_resources", "open_market", "storage");
        }

        @Test
        void opensTheMilitaryCounterOnAFactionGarrison() {

            var market = MarketOwnershipFixture.buildColonyRunning(
                Factions.PLAYER,
                "militarybase");

            MarketOwnershipRule.applyOwnership(market, MarketOwnershipFixture.FACTION_OWNER_ID);

            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .contains("generic_military");
        }

        @Test
        void withholdsTheMilitaryCounterFromAPlayerColonyRunningAMilitaryBase() {
            // The player's own garrison equips itself rather than trading with itself, so the
            // military counter closes when the place changes hands to the player however it is
            // armed.
            var market = MarketOwnershipFixture.buildColonyRunning(
                MarketOwnershipFixture.FACTION_OWNER_ID,
                "militarybase");

            MarketOwnershipRule.applyOwnership(market, MarketOwnershipFixture.FACTION_OWNER_ID);
            MarketOwnershipRule.applyOwnership(market, Factions.PLAYER);

            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .doesNotContain("generic_military");
        }

        @Test
        void keepsStorageWhenAColonyLeavesThePlayer() {
            // Storage holds the player's own cargo and hulls. Closing it on a hand-over would
            // destroy property rather than transfer it, which is why no owner's rule names it
            // among the counters it opens or closes.
            var market = MarketOwnershipFixture.buildColonyTradingThrough(
                Factions.PLAYER,
                "local_resources",
                "storage");

            MarketOwnershipRule.applyOwnership(market, MarketOwnershipFixture.FACTION_OWNER_ID);

            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .contains("storage");
        }

        @Test
        void opensStorageAlreadyPaidForWhenThePlayerTakesAColonyWithoutOne() {
            // Vanilla charges a fee at a storage counter the player did not build. A colony the
            // player now holds is theirs, so the fee is marked settled rather than billed to reach
            // their own hold.
            var market = MarketOwnershipFixture.buildColonyHeldBy(
                MarketOwnershipFixture.FACTION_OWNER_ID);

            MarketOwnershipRule.applyOwnership(market, Factions.PLAYER);

            verify(MarketOwnershipFixture.readStoragePlugin(market))
                .setPlayerPaidToUnlock(true);
        }

        @Test
        void changesTheOwnerOfAColonyWithNoBodyToReFlag() {
            // A market can stand for a place the game gave no entity, and the flag half of the
            // change simply has nothing to reach - the counters and the tariff still land.
            var market = MarketOwnershipFixture.buildColonyWithNothingAttached(
                MarketOwnershipFixture.FACTION_OWNER_ID);

            MarketOwnershipRule.applyOwnership(market, Factions.PLAYER);

            assertThat(market.getFactionId())
                .isEqualTo("player");
            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .containsExactlyInAnyOrder("local_resources", "storage");
        }

        @Test
        void handsTheCountersToASubmarketRuleTheInstallSupplies() {
            // A mod running its own diplomacy knows which modded markets trade without a black
            // market and which faction ships a military counter of its own, so where such a rule is
            // present it decides the counters whole and none of this library's verdicts apply.
            var market = MarketOwnershipFixture.buildColonyTradingThrough(
                Factions.PLAYER,
                "local_resources");

            var offeredMarket = new AtomicReference<MarketAPI>();
            var offeredOldOwnerId = new AtomicReference<String>();
            var offeredNewOwnerId = new AtomicReference<String>();

            MarketOwnershipRule.applyOwnership(
                market,
                MarketOwnershipFixture.FACTION_OWNER_ID,
                (ruleMarket, ruleOldOwnerId, ruleNewOwnerId) -> {
                    offeredMarket.set(ruleMarket);
                    offeredOldOwnerId.set(ruleOldOwnerId);
                    offeredNewOwnerId.set(ruleNewOwnerId);
                    return new ExecutedWork();
                });

            assertThat(offeredMarket.get())
                .isSameAs(market);
            assertThat(offeredNewOwnerId.get())
                .isEqualTo("hegemony");
            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .containsExactly("local_resources");
        }

        @Test
        void namesTheOutgoingOwnerToTheInstalledRuleAsWellAsTheIncomingOne() {
            // A rule may restock the counters only where the colony has actually changed hands, and
            // the outgoing owner is the only thing that says whether it has - so it is read before
            // the incoming id lands rather than after, when it is gone.
            var market = MarketOwnershipFixture.buildColonyHeldBy(
                MarketOwnershipFixture.FACTION_OWNER_ID);

            var offeredOldOwnerId = new AtomicReference<String>();

            MarketOwnershipRule.applyOwnership(
                market,
                Factions.PLAYER,
                (ruleMarket, ruleOldOwnerId, ruleNewOwnerId) -> {
                    offeredOldOwnerId.set(ruleOldOwnerId);
                    return new ExecutedWork();
                });

            assertThat(offeredOldOwnerId.get())
                .isEqualTo("hegemony");
        }

        @Test
        void opensStorageForThePlayerEvenWhereTheInstalledRuleTakesTheCounters() {
            // Storage is the one counter no owner's rule names, this library's included, so it is
            // applied whichever rule decided the rest - a colony handed to the player through a
            // mod's rule would otherwise be left with no hold they can reach.
            var market = MarketOwnershipFixture.buildColonyHeldBy(
                MarketOwnershipFixture.FACTION_OWNER_ID);

            MarketOwnershipRule.applyOwnership(
                market,
                Factions.PLAYER,
                (ruleMarket, ruleOldOwnerId, ruleNewOwnerId) -> new ExecutedWork());

            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .containsExactly("storage");
            verify(MarketOwnershipFixture.readStoragePlugin(market))
                .setPlayerPaidToUnlock(true);
        }

        @Test
        void changesEveryOtherAspectItselfWhereTheInstalledRuleTakesTheCounters() {
            // The rule stands in for one aspect of the change rather than for the change. Nothing
            // an install supplies decides whose flag the colony flies, whether it is the player's,
            // what its outlying entities fly or what its goods are taxed at - so all four land
            // whichever rule decided the counters.
            var market = MarketOwnershipFixture.buildColonyHeldBy(
                MarketOwnershipFixture.FACTION_OWNER_ID);

            MarketOwnershipRule.applyOwnership(
                market,
                Factions.PLAYER,
                (ruleMarket, ruleOldOwnerId, ruleNewOwnerId) -> new ExecutedWork());

            assertThat(market.getFactionId())
                .isEqualTo("player");
            assertThat(market.isPlayerOwned())
                .isTrue();
            assertThat(MarketOwnershipFixture.readFactionId(market.getPrimaryEntity()))
                .isEqualTo("player");
            assertThat(market.getConnectedEntities())
                .allSatisfy(entity -> assertThat(MarketOwnershipFixture.readFactionId(entity))
                    .isEqualTo("player"));
            assertThat(market.getTariff().getFlatStatMod(DEFAULT_TARIFF_MODIFIER_ID).value)
                .isEqualTo(0.3f);
        }

        @Test
        void appliesItsOwnVerdictsWhenTheInstalledRuleDeclinesTheCounters() {
            // The answer on every install without such a mod, so the colony still trades over the
            // counters its new owner should have.
            var market = MarketOwnershipFixture.buildColonyTradingThrough(
                Factions.PLAYER,
                "local_resources");

            MarketOwnershipRule.applyOwnership(
                market,
                MarketOwnershipFixture.FACTION_OWNER_ID,
                (ruleMarket, ruleOldOwnerId, ruleNewOwnerId) ->
                    new DeclinedWork("this stub decides nothing"));

            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .containsExactlyInAnyOrder("open_market", "black_market");
        }

        @Test
        void appliesItsOwnVerdictsThroughTheLiveBindingWhenNoSuchModIsInstalled() {
            // The public entry point rather than the seam beneath it: nothing else here exercises
            // the rule the library actually binds, and an install without that mod is what the
            // fallback exists for.
            var market = MarketOwnershipFixture.buildColonyTradingThrough(
                Factions.PLAYER,
                "local_resources");

            ModStateScopes.runWithModEnabled(NEXERELIN, false, () ->
                MarketOwnershipRule.applyOwnership(
                    market,
                    MarketOwnershipFixture.FACTION_OWNER_ID));

            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .containsExactlyInAnyOrder("open_market", "black_market");
        }

        @Test
        void leavesANullMarketAlone() {
            assertThatCode(() -> MarketOwnershipRule.applyOwnership(null, Factions.PLAYER))
                .doesNotThrowAnyException();
        }

        @Test
        void leavesAColonyAloneWhenNoOwnerIsNamed() {
            // Naming nobody is not the same as unowning the place, so the colony is left exactly
            // as it was rather than stripped of the owner it has.
            var market = MarketOwnershipFixture.buildColonyTradingThrough(
                MarketOwnershipFixture.FACTION_OWNER_ID,
                "open_market");

            MarketOwnershipRule.applyOwnership(market, null);

            assertThat(market.getFactionId())
                .isEqualTo("hegemony");
            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .containsExactly("open_market");
        }
    }
}
