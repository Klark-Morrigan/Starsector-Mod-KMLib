package kmlib.starsector.markets.ownership;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.SettingsAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.campaign.listeners.EconomyTickListener;
import com.fs.starfarer.api.impl.campaign.econ.RecentUnrest;
import com.fs.starfarer.api.impl.campaign.ids.Submarkets;

import kmlib.starsector.markets.Markets;

/**
 * Handing an existing colony to another owner.
 *
 * <p>Two halves, and only one of them is about whoever holds the place next. A colony first has to
 * be detached from the owner it is leaving: the administrator they appointed, the free-port
 * arrangement they made, the stockpiling they turned on, the unrest their rule accrued, and the
 * running account at the counter their own production was sold over. None of that is a property of
 * the incoming owner, which is why it is stated here rather than in
 * {@link MarketOwnershipRule#applyOwnership} - a colony founded a moment ago has no previous owner, and
 * a founding that undid arrangements nobody had made would be describing a takeover instead.
 *
 * <p>What the colony becomes under its new owner is that rule's answer whole, applied rather than
 * restated. A colony that changed hands and one founded under the same owner therefore read
 * identically afterwards, instead of a takeover being a second and quieter definition of what an
 * owner's colony looks like.
 *
 * <p>Because the leaving half is destructive, a hand-over that is not one is refused rather than
 * performed: naming the owner a colony already has would cost it the arrangements that owner made
 * and settle its account, all for a call that changes nothing about who holds the place. A caller
 * that cannot be certain the owner it names is a new one can therefore make the call anyway.
 *
 * <p>The order of the two halves carries a charge with it. The player takes goods from the local
 * resources counter on credit, and the bill for what was taken is raised at the month's end only
 * while the colony is still theirs - so the account is settled before the flag changes. Reversed,
 * the same transfer would silently forgive whatever was outstanding, which on an operation whose
 * whole purpose is handing colonies around is free goods for the asking.
 *
 * <p>All of which is what handing a colony over means where nothing else has an opinion about it.
 * An install running a mod with a hand-over of its own gives that mod the whole of it, since a
 * colony changing hands there moves standing, files intel and re-posts offices that nothing can
 * arrange after the fact - see {@link OwnershipTransferRoutine}.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state, and
 * null-defensive like the rest of the library.
 */
public final class MarketOwnershipTransfer {

    // The engine's own setting for how many times a month the economy is stepped. The last of those
    // steps is the one a counter's outstanding account is billed on, so the number is what says
    // which step to ask for - no ids class names the key, so this is the named home for it.
    private static final String ECONOMY_ITERATIONS_PER_MONTH_SETTING = "economyIterPerMonth";

    // What the outgoing owner's unrest is settled to. A colony's unrest is a record of how its
    // previous owner was resented, and the incoming owner inherits the place rather than the
    // resentment.
    private static final int NO_UNREST = 0;

    // Whether to hang an unrest condition on a colony that carries none, which is asked only to
    // decline it: the point of the read is to clear unrest that exists, and a condition added here
    // would mark a peacefully handed-over colony as recently troubled until the next economy step
    // swept it away again.
    private static final boolean WITHOUT_ADDING_THE_CONDITION = false;

    // Whatever hand-over this install supplies, offered every transfer before the sequence below is
    // composed. Asked of this package's own register rather than by naming a mod here: which mods
    // are present is a fact about the install and is settled where the install is composed, not by
    // the operation that happens to have a colony to hand over.
    //
    // A hand-over is stated as a colony and whoever is taking it, which is the whole of what one
    // is. A routine needing more than that - the faction behind the id, the sector it lives in - is
    // describing itself rather than the operation, so it reads what it needs where it is registered
    // and this parameter list stays what a hand-over actually takes.
    private static final OwnershipTransferRoutine INSTALLED_OWNERSHIP_TRANSFER_ROUTINES =
        OwnershipTransferRoutines::offerTransfer;

    private MarketOwnershipTransfer() {
        // utility class, no instances.
    }

    /**
     * Hands an existing colony to another owner: detached from the one it is leaving, then held by
     * the one it is given to.
     *
     * <p>Only the leaving half is this method's own. Everything the colony reads as afterwards -
     * its flag, the flags of its outlying entities, its trading counters and its tariff - is
     * {@link MarketOwnershipRule#applyOwnership}'s single statement of what holding a colony means,
     * so a colony taken by a faction is the same shape as one that faction founded.
     *
     * <p>Naming the owner the colony already has is refused rather than performed. Only the
     * ownership half of such a call would settle where it already was - the detaching half has no
     * idea it is being asked for nothing, so the colony would lose the administrator, the free
     * port and the stockpiling its own owner set up, and be billed there and then, for a call that
     * reads as changing nothing. A hand-over that is not one is a mistake wherever it is made, so
     * the refusal is here rather than left to each caller to remember.
     *
     * <p>Whether the market is a colony at all is still the caller's question: a market that is
     * not one has nothing to detach and nothing an owner would hold, so it is left alone by every
     * step below rather than tested for here.
     *
     * <p>Where the install runs a mod with a hand-over of its own, that mod moves the colony and
     * none of the above happens: a colony changing hands on such an install has to move the way the
     * rest of that mod expects, which nothing can arrange after the fact. What a caller is promised
     * either way is a colony held by the owner it named, not the particular sequence that got it
     * there.
     *
     * @param market    the colony changing hands; null is left alone
     * @param factionId the incoming owner's faction id; null leaves the colony alone rather than
     *                  detaching it from an owner and giving it to nobody, as does the id of the
     *                  faction already holding it
     */
    public static void transferOwnership(MarketAPI market, String factionId) {
        transferOwnership(market, factionId, INSTALLED_OWNERSHIP_TRANSFER_ROUTINES);
    }

    // The same hand-over against a stated routine rather than the installed one, which is what lets
    // both branches be posed on a machine that has whichever mods it happens to have.
    static void transferOwnership(
            MarketAPI market,
            String factionId,
            OwnershipTransferRoutine ownershipTransferRoutine) {

        // Asked before the routine below is offered the hand-over, so a call this library refuses
        // is not one a mod is handed either - the colony that would lose its administrator, its
        // free port and its stockpiling for nothing loses them to whichever sequence ran.
        if (market == null || factionId == null || Markets.isOwnedBy(market, factionId)) {
            return;
        }

        // A mod that moves its own colonies takes the whole hand-over rather than having this
        // sequence run under it, so a colony on that install changes hands the way the rest of that
        // mod expects. A routine that declines leaves the colony untouched for the sequence below -
        // which is the answer on every install without such a mod.
        var outcome = ownershipTransferRoutine.transferOwnership(market, factionId);

        if (outcome != null && outcome.wasExecuted()) {
            return;
        }

        detachFromOutgoingOwner(market);

        MarketOwnershipRule.applyOwnership(market, factionId);
    }

    // Everything the outgoing owner leaves behind, undone before the colony is anyone else's. The
    // account is settled first of all, being the one part of this that is only chargeable while the
    // colony still reads as the player's - the rest is undone in any order.
    private static void detachFromOutgoingOwner(MarketAPI market) {

        settleLocalResourcesAccount(market);

        // An administrator is a person the previous owner posted here, a free port is an
        // arrangement they made, and stockpiling for shortages is an instruction they gave. The
        // incoming owner gets a colony run to none of those rather than to their predecessor's.
        market.setAdmin(null);
        market.setFreePort(false);
        market.setUseStockpilesForShortages(false);

        clearRecentUnrest(market);
    }

    // Raises the outstanding bill at the local resources counter, where the colony has one. The
    // goods themselves are the colony's own production restocked from its industries, so losing
    // them costs the player nothing - what would be lost is the record of what was taken on credit
    // and never paid for, which the counter only ever bills while the colony is the player's.
    //
    // Asked of the counter through the listener interface that declares the billing step rather
    // than of the engine's own plugin class, so a colony whose counter a mod has replaced is billed
    // by whatever is actually keeping its account.
    private static void settleLocalResourcesAccount(MarketAPI market) {

        var settings = Global.getSettings();

        // Outside a running game there is no month to bill against, and nothing has been taken on
        // credit either - a caller reaching this from outside one is posing a market rather than
        // playing a save.
        if (settings == null) {
            return;
        }

        var account = Markets.readSubmarketPlugin(
            market,
            Submarkets.LOCAL_RESOURCES,
            EconomyTickListener.class);

        if (account != null) {
            account.reportEconomyTick(readMonthEndEconomyIteration(settings));
        }
    }

    // Which economy step of the month is its last, that being the only one an outstanding account
    // is billed on. Counted from the number of steps a month has, so the answer follows whatever
    // the install's own settings say rather than a number assumed here.
    private static int readMonthEndEconomyIteration(SettingsAPI settings) {
        return (int) settings.getFloat(ECONOMY_ITERATIONS_PER_MONTH_SETTING) - 1;
    }

    // Wipes the unrest the outgoing owner accrued, where the colony carries any. Read without
    // adding the condition, so a colony that was never troubled stays that way - the game's own
    // takeover reaches for it unconditionally because a takeover it performs is the end of a
    // blockade that caused the unrest in the first place.
    private static void clearRecentUnrest(MarketAPI market) {

        var recentUnrest = RecentUnrest.get(market, WITHOUT_ADDING_THE_CONDITION);

        if (recentUnrest != null) {
            recentUnrest.setPenalty(NO_UNREST);
        }
    }
}
