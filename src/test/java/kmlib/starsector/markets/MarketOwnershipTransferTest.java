package kmlib.starsector.markets;

import com.fs.starfarer.api.impl.campaign.ids.Factions;

import kmlib.testfixtures.starsector.settings.ModStateScopes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Pins the contract of {@link MarketOwnershipTransfer#transferOwnership}. The cases live in a
 * {@link Nested} group so the suite reports as a per-method tree; the colonies they are posed
 * against are {@link MarketTransferFixture}'s, which carry what the owner they are leaving left
 * behind and answer from what has been done to them since.
 *
 * <p>Both directions are posed throughout. A takeover written against the one direction the game
 * itself performs - a colony passing between factions - detaches everything a faction leaves and
 * still hands the player a colony they cannot store in, and the reverse leaves a faction running a
 * colony under the player's own arrangements.
 *
 * <p>What the colony becomes under its new owner is checked only as far as the ownership rule
 * having been applied whole. That rule has its own suite, and restating its verdicts here would
 * make a change to what an owner's colony looks like fail twice for one reason.
 *
 * <p>The cases run inside a game whose mod set is readable and holds no colonisation mod, that
 * being the install the composed sequence exists for. Which the game's own settings are readable in
 * matters beyond the mod gate here: the account a colony's outgoing owner is billed for is settled
 * against the month's last economy step, and the number of steps a month has is one of them.
 */
final class MarketOwnershipTransferTest {

    // Which economy step of a month the account is settled on, spelt here rather than read off the
    // class under test. The fixture's game steps its economy once a month, so the month's last step
    // is its first - what a case reading the number from the code it checks would pin is nothing.
    private static final int MONTH_END_ECONOMY_ITERATION = 0;

    @Nested
    class TransferOwnership {

        @Test
        void hands_a_faction_s_colony_to_the_player() {
            // The one direction the game has no routine of its own for, vanilla having no market
            // capture at all - so what the colony becomes is the ownership rule's answer applied
            // rather than a sequence transcribed from anywhere.
            var market = MarketTransferFixture.buildFactionColonyAsItsOwnerLeftIt();

            ModStateScopes.runWithoutModManager(() ->
                MarketOwnershipTransfer.transferOwnership(market, Factions.PLAYER));

            assertThat(market.getFactionId())
                .isEqualTo("player");
            assertThat(market.isPlayerOwned())
                .isTrue();
            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .containsExactlyInAnyOrder("local_resources", "storage");
        }

        @Test
        void hands_the_player_s_colony_to_a_faction() {

            var market = MarketTransferFixture.buildPlayerColonyAsItsOwnerLeftIt();

            ModStateScopes.runWithoutModManager(() ->
                MarketOwnershipTransfer.transferOwnership(
                    market,
                    MarketTransferFixture.FACTION_OWNER_ID));

            assertThat(market.getFactionId())
                .isEqualTo("hegemony");
            assertThat(market.isPlayerOwned())
                .isFalse();
            assertThat(MarketOwnershipFixture.readSubmarketIds(market))
                .containsExactlyInAnyOrder("open_market", "black_market", "storage");
        }

        @Test
        void removes_the_administrator_the_outgoing_owner_posted() {
            // An administrator is a person the previous owner sent here, not a post the colony
            // carries - leaving them in place would have a faction's own officer running a colony
            // for whoever took it.
            var market = MarketTransferFixture.buildFactionColonyAsItsOwnerLeftIt();

            ModStateScopes.runWithoutModManager(() ->
                MarketOwnershipTransfer.transferOwnership(market, Factions.PLAYER));

            assertThat(market.getAdmin())
                .isNull();
        }

        @Test
        void closes_the_free_port_the_outgoing_owner_opened() {
            // A free port is an arrangement its owner made about their own colony's trade, so the
            // incoming owner gets a colony trading normally rather than under terms they never set.
            var market = MarketTransferFixture.buildPlayerColonyAsItsOwnerLeftIt();

            ModStateScopes.runWithoutModManager(() ->
                MarketOwnershipTransfer.transferOwnership(
                    market,
                    MarketTransferFixture.FACTION_OWNER_ID));

            assertThat(market.isFreePort())
                .isFalse();
        }

        @Test
        void stops_covering_shortages_from_the_outgoing_owner_s_stockpiles() {

            var market = MarketTransferFixture.buildPlayerColonyAsItsOwnerLeftIt();

            ModStateScopes.runWithoutModManager(() ->
                MarketOwnershipTransfer.transferOwnership(
                    market,
                    MarketTransferFixture.FACTION_OWNER_ID));

            assertThat(market.isUseStockpilesForShortages())
                .isFalse();
        }

        @Test
        void wipes_the_unrest_the_outgoing_owner_accrued() {
            // Unrest is a record of how the previous owner was resented. The incoming owner
            // inherits the colony rather than the resentment, and a stability penalty carried over
            // would read as their own rule going badly from the day they took the place.
            var market = MarketTransferFixture.buildFactionColonyAsItsOwnerLeftIt();
            var recentUnrest = MarketTransferFixture.readRecentUnrest(market);

            ModStateScopes.runWithoutModManager(() ->
                MarketOwnershipTransfer.transferOwnership(market, Factions.PLAYER));

            verify(recentUnrest)
                .setPenalty(0);
        }

        @Test
        void leaves_a_colony_nobody_resented_carrying_no_unrest_at_all() {
            // The engine's own read hangs an unrest condition on a colony that has none before
            // answering, which on a peaceful hand-over would mark the colony as recently troubled
            // for as long as it took the next economy step to sweep it off again.
            var market = MarketTransferFixture.buildContentedFactionColony();

            ModStateScopes.runWithoutModManager(() ->
                MarketOwnershipTransfer.transferOwnership(market, Factions.PLAYER));

            verify(market, never())
                .addCondition(anyString());
        }

        @Test
        void settles_the_local_resources_account_while_the_colony_is_still_the_player_s() {
            // The whole reason the account is settled at all: what the player took on credit is
            // billed only while the colony reads as theirs, so a transfer that changed the flag
            // first would forgive the outstanding charge on the way past.
            var market = MarketTransferFixture.buildPlayerColonyAsItsOwnerLeftIt();
            var account = MarketTransferFixture.readLocalResourcesAccount(market);
            var wasStillPlayerHeldWhenBilled = new AtomicBoolean();

            doAnswer(invocation -> {
                    wasStillPlayerHeldWhenBilled.set(market.isPlayerOwned());
                    return null;
                })
                .when(account)
                .reportEconomyTick(anyInt());

            ModStateScopes.runWithoutModManager(() ->
                MarketOwnershipTransfer.transferOwnership(
                    market,
                    MarketTransferFixture.FACTION_OWNER_ID));

            assertThat(wasStillPlayerHeldWhenBilled)
                .isTrue();
        }

        @Test
        void settles_the_account_against_the_month_s_last_economy_step() {
            // The step the counter itself bills on, and the only one it acts upon - asked for by
            // the number of steps the install's own settings give a month rather than by a number
            // assumed here.
            var market = MarketTransferFixture.buildPlayerColonyAsItsOwnerLeftIt();
            var account = MarketTransferFixture.readLocalResourcesAccount(market);

            ModStateScopes.runWithoutModManager(() ->
                MarketOwnershipTransfer.transferOwnership(
                    market,
                    MarketTransferFixture.FACTION_OWNER_ID));

            verify(account)
                .reportEconomyTick(MONTH_END_ECONOMY_ITERATION);
        }

        @Test
        void hands_over_a_colony_that_keeps_no_such_account() {
            // A colony taken from a faction has no local resources counter, that being the
            // player's own - so there is nothing to settle and the hand-over is the whole of it.
            // The counter the colony ends up with is one the change opened for its new owner, and
            // an account opened a moment ago has nothing outstanding to bill for.
            var market = MarketTransferFixture.buildFactionColonyAsItsOwnerLeftIt();

            ModStateScopes.runWithoutModManager(() ->
                MarketOwnershipTransfer.transferOwnership(market, Factions.PLAYER));

            verify(MarketTransferFixture.readLocalResourcesAccount(market), never())
                .reportEconomyTick(anyInt());
            assertThat(market.getFactionId())
                .isEqualTo("player");
        }

        @Test
        void hands_over_a_colony_whose_counter_keeps_no_account() {
            // The counter is asked for its billing step through the listener that step is declared
            // on, so a plugin some other mod put there is simply not asked - rather than the
            // hand-over failing on a colony whose counter is not the one this library expects.
            var market = MarketTransferFixture.buildPlayerColonyWhoseCounterKeepsNoAccount();

            ModStateScopes.runWithoutModManager(() ->
                MarketOwnershipTransfer.transferOwnership(
                    market,
                    MarketTransferFixture.FACTION_OWNER_ID));

            assertThat(market.getFactionId())
                .isEqualTo("hegemony");
            assertThat(market.isFreePort())
                .isFalse();
        }

        @Test
        void hands_over_a_colony_before_the_game_settings_are_up() {
            // A read taken outside a running game cannot say how many steps a month has, and there
            // is no month being played through to have taken anything on credit in - so the
            // account is left unsettled and the colony still changes hands.
            var market = MarketTransferFixture.buildPlayerColonyAsItsOwnerLeftIt();
            var account = MarketTransferFixture.readLocalResourcesAccount(market);

            ModStateScopes.runWithoutGameSettings(() ->
                MarketOwnershipTransfer.transferOwnership(
                    market,
                    MarketTransferFixture.FACTION_OWNER_ID));

            verify(account, never())
                .reportEconomyTick(anyInt());
            assertThat(market.getFactionId())
                .isEqualTo("hegemony");
        }

        @Test
        void leaves_a_null_market_alone() {
            assertThatCode(() -> MarketOwnershipTransfer.transferOwnership(null, Factions.PLAYER))
                .doesNotThrowAnyException();
        }

        @Test
        void leaves_a_colony_alone_when_no_owner_is_named() {
            // Naming nobody would leave a colony detached from the owner it had and given to none,
            // which is worse than the transfer not happening - so nothing is detached either.
            var market = MarketTransferFixture.buildFactionColonyAsItsOwnerLeftIt();

            ModStateScopes.runWithoutModManager(() ->
                MarketOwnershipTransfer.transferOwnership(market, null));

            assertThat(market.getFactionId())
                .isEqualTo("hegemony");
            assertThat(market.getAdmin())
                .isNotNull();
            assertThat(market.isFreePort())
                .isTrue();
        }
    }
}
