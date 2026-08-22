package kmlib.mods.nexerelin;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.extensions.DeclinedWork;
import kmlib.extensions.ExecutedWork;
import kmlib.extensions.WorkOutcome;

import java.util.List;

import exerelin.campaign.SectorManager;

/**
 * Handing a colony to another owner the way Nexerelin hands one over, on an install running it.
 *
 * <p>That mod's hand-over does far more than rewrite a flag. It re-flags the station, the relay and
 * the fleets based at each of them, re-posts the people holding the offices a capture changes,
 * settles the account at the local resources counter, decides the trading counters by its own rules,
 * re-reads the free-port setting and the immigration incentives, re-applies its tariffs, files the
 * hand-over as intel its players can read, tells its diplomacy so standing and alliances move,
 * re-checks whether the outgoing owner has been eliminated or the incoming one has just respawned,
 * and tells the colony listeners it keeps of its own. None of that can be arranged after the fact by
 * something that only knows the market, so on such an install its routine takes the whole hand-over
 * and the sequence this library composes is skipped entirely rather than run underneath it.
 *
 * <p>What the hand-over is <em>not</em> is a capture. That mod draws the distinction itself, and
 * everything downstream of it - a rebellion raised among the population, the timeout before the
 * place can be stabilised, the cancelled construction that stops an invasion being farmed, the
 * credits taken off a player who lost the colony - belongs to a place taken by force. A colony
 * handed over administratively is none of those things, so the flags that mark a capture are all
 * declined and nobody's standing moves for it.
 *
 * <p>It is filed rather than performed quietly: the mod's own intel entry stands, which is what
 * tells whoever asked for the hand-over that it registered.
 *
 * <p>The mod is optional, so the sole reference to a type of its own lives in the nested
 * {@link NexerelinTypes} holder, which the classloader does not resolve until the presence gate has
 * passed. That keeps an install without the mod from ever seeking a class it does not have.
 *
 * <p>Nothing outside this package is named here. The routine is wrapped as the mod states it, and
 * which of the two hand-over paths an install takes is decided where that choice belongs - beside
 * the composed sequence that is the other half of it - so the two sides depend on this one rather
 * than on each other.
 */
public final class NexerelinMarketTransfer {

    // Whether the player is a party to the hand-over. They are not: this stands for an owner being
    // named from outside the game's own politics, so the mod is not told to move anyone's standing
    // towards the player, credit them with the capture, or hold the colony against them.
    private static final boolean WITHOUT_THE_PLAYER_TAKING_PART = false;

    // Whether the colony was taken by force. It was not, and the difference is most of what the
    // mod's routine branches on - a capture raises a rebellion, holds the place under a
    // stabilisation timeout, cancels the construction an invasion could otherwise farm, and bills
    // a player who lost the colony for part of their cash.
    private static final boolean HANDED_OVER_RATHER_THAN_CAPTURED = false;

    // Which factions hear about the hand-over as something done to them. None, for the same reason
    // it is not a capture: an owner named from outside the game's politics is not an act any
    // faction can hold against another. The mod's routine reads an unnamed list as nobody and
    // builds an empty one of its own, which is why this is the absence rather than an empty list
    // it might be free to add to.
    private static final List<String> NO_FACTIONS_TO_NOTIFY = null;

    // How far standing moves between the factions involved. Nowhere: see above, and a hand-over
    // arranged from outside the sector's politics that swung diplomacy would be changing far more
    // than who holds one colony.
    private static final float NO_REPUTATION_CHANGE = 0f;

    private NexerelinMarketTransfer() {
        // utility class, no instances.
    }

    /**
     * Hands the colony over through Nexerelin's own routine where this install can take that path.
     *
     * <p>Declining is a normal answer rather than a failure: the mod may not be installed, and its
     * routine needs both owners as factions it can resolve. Every decline leaves the colony
     * untouched, so the caller is free to hand it over itself.
     *
     * <p>The outgoing owner is read off the colony rather than asked for. A hand-over is stated as
     * the colony and whoever is taking it, and the colony still flies the previous owner's flag at
     * the moment it is offered here - so reading it is exact where asking for it would let a caller
     * name an owner the colony never had.
     *
     * @param sector    the sector holding the faction the colony is being handed to; null is
     *                  declined
     * @param market    the colony changing hands; null is declined, as is one flying no flag at all,
     *                  the mod's routine reading the outgoing owner's id off it
     * @param factionId the incoming owner's faction id; null is declined, and so is an id no
     *                  faction answers to - the routine reads that mod's own configuration and
     *                  tariffs off the faction rather than off an id
     * @return the hand-over performed by Nexerelin, or a decline naming what about this call it
     *         could not move - the colony left exactly as it was either way
     */
    public static WorkOutcome transferOwnership(
            SectorAPI sector,
            MarketAPI market,
            String factionId) {

        // The presence gate is asked first and alone, so an install without the mod reads nothing
        // else and never reaches the holder below.
        if (!NexerelinPresence.isModEnabled()) {
            return new DeclinedWork("Nexerelin is not enabled on this install");
        }

        if (sector == null || market == null || factionId == null) {
            return new DeclinedWork("the hand-over was stated without a sector, a colony or an "
                + "incoming owner");
        }

        var incomingOwner = sector.getFaction(factionId);

        if (incomingOwner == null) {
            return new DeclinedWork("the sector answers for no faction with id '" + factionId
                + "', and Nexerelin's own hand-over reads its configuration and standing off one");
        }

        // Both owners reach the routine as factions rather than ids: it reads each one's
        // configuration, colours and remaining holdings, so an owner it cannot resolve is a decline
        // rather than a hand-over that dies partway through.
        var outgoingOwner = market.getFaction();

        if (outgoingOwner == null) {
            return new DeclinedWork("'" + market.getName()
                + "' flies no flag to hand over from, and Nexerelin's own hand-over is stated "
                + "between two owners");
        }

        NexerelinTypes.transferMarket(market, incomingOwner, outgoingOwner);

        return new ExecutedWork();
    }

    // Isolates the only reference to a Nexerelin type. The classloader resolves this holder on
    // first call, which the presence gate defers until the mod is known to be present.
    private static final class NexerelinTypes {

        private static void transferMarket(
                MarketAPI market,
                FactionAPI incomingOwner,
                FactionAPI outgoingOwner) {

            SectorManager.transferMarket(
                market,
                incomingOwner,
                outgoingOwner,
                WITHOUT_THE_PLAYER_TAKING_PART,
                HANDED_OVER_RATHER_THAN_CAPTURED,
                NO_FACTIONS_TO_NOTIFY,
                NO_REPUTATION_CHANGE);
        }
    }
}
