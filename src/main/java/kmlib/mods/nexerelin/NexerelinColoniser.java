package kmlib.mods.nexerelin;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.PlanetAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import kmlib.extensions.DeclinedWork;
import kmlib.extensions.ExecutedWork;
import kmlib.extensions.WorkOutcome;

import exerelin.campaign.intel.colony.ColonyExpeditionIntel;

/**
 * Founding a colony the way Nexerelin founds one, on an install running it.
 *
 * <p>Nexerelin does far more than settle a body. A colony it founds carries the record that it was
 * founded rather than generated with the sector, the faction that founded it, that faction's own
 * tariffs and free-port stance, trading counters chosen by that mod's rules rather than the game's,
 * an administrator, and the condition its diplomacy reads a colony through. None of that can be
 * added to a colony afterwards by anything that does not already know the mod, so on such an
 * install its routine takes the whole founding and the sequence this library composes is skipped
 * entirely rather than run underneath it.
 *
 * <p>Whose colony it is decided by the faction id alone, matching how ownership is stated
 * everywhere else here. The mod's routine draws the same distinction the game's two colonisation
 * routines do - the player's colony is flagged as theirs, gets a spaceport underway and a storage
 * hold already paid for, while a faction's arrives with industries and immigration incentives.
 *
 * <p>Nothing extra is announced. The mod's routine tells its own colony listener and stops, raising
 * neither the game's player-colonisation report nor the mod's own transfer fan-out - so a colony
 * founded through here is announced exactly as one founded by that mod's colony fleet, which is
 * what a caller on that install is entitled to expect.
 *
 * <p>The mod is optional, so the sole reference to a type of its own lives in the nested
 * {@link NexerelinTypes} holder, which the classloader does not resolve until the presence gate has
 * passed. That keeps an install without the mod from ever seeking a class it does not have.
 *
 * <p>Nothing outside this package is named here. The routine is wrapped as the mod states it, and
 * which of the two colonisation paths an install takes is decided where that choice belongs -
 * beside the composed sequence that is the other half of it - so the two sides depend on this one
 * rather than on each other.
 */
public final class NexerelinColoniser {

    // Whether the colony is a ruined world being resettled, which is a founding this routine is
    // never asked for: that branch skips the administrator and forces a spaceport up rather than
    // queueing one, being about a place that was lived in before.
    private static final boolean FOUNDING_RATHER_THAN_RESETTLING = false;

    private NexerelinColoniser() {
        // utility class, no instances.
    }

    /**
     * Founds the colony through Nexerelin's own routine where this install can take that path.
     *
     * <p>Declining is a normal answer rather than a failure: the mod may not be installed, and its
     * routine needs a planet and a faction it can resolve. Every decline leaves the market
     * untouched, so the caller is free to found the colony itself.
     *
     * @param sector    the sector holding the faction the colony is founded under
     * @param market    the survey data to found on; the mod's routine renames a body still carrying
     *                  the name its star system gave it, so the colony may not keep its old name
     * @param factionId  the owner the colony is founded under, {@link Factions#PLAYER} for the
     *                   player
     * @param colonySize the size the colony is founded at, which the caller states rather than
     *                   this wrapper: a colony has one size however it was founded, so the number
     *                   is the caller's to keep in step with whatever it founds at otherwise. The
     *                   mod's own callers all pass three
     * @return the founding performed by Nexerelin, or a decline naming what about this call it
     *         could not found - the market left exactly as it was either way
     */
    public static WorkOutcome establishColony(
            SectorAPI sector,
            MarketAPI market,
            String factionId,
            int colonySize) {

        // The presence gate is asked first and alone, so an install without the mod reads nothing
        // else and never reaches the holder below.
        if (!NexerelinPresence.isModEnabled()) {
            return new DeclinedWork("Nexerelin is not enabled on this install");
        }

        if (sector == null || market == null || factionId == null) {
            return new DeclinedWork("the founding was stated without a sector, a market or an "
                + "owner");
        }

        // The body has to be a planet: the routine dereferences it to rename a world still carrying
        // its star system's name, and to read the system that name came from. A colonisable market
        // whose body is something else - which a modded body can be - is a decline rather than a
        // crash, and the caller founds the colony itself.
        if (!(market.getPrimaryEntity() instanceof PlanetAPI planet)) {
            return new DeclinedWork("the body under '" + market.getName()
                + "' is not a planet, and Nexerelin's own founding renames one and reads its "
                + "star system");
        }

        // The faction is looked up rather than passed on as an id: the routine reads that mod's
        // own configuration and tariffs off the faction object, so an id the sector does not know
        // is a decline rather than a founding that dies partway through.
        var faction = sector.getFaction(factionId);

        if (faction == null) {
            return new DeclinedWork("the sector answers for no faction with id '" + factionId
                + "', and Nexerelin's own founding reads its configuration and tariffs off one");
        }

        NexerelinTypes.createColony(
            market,
            planet,
            faction,
            Factions.PLAYER.equals(factionId),
            colonySize);

        return new ExecutedWork();
    }

    // Isolates the only reference to a Nexerelin type. The classloader resolves this holder on
    // first call, which the presence gate defers until the mod is known to be present.
    private static final class NexerelinTypes {

        private static void createColony(
                MarketAPI market,
                PlanetAPI planet,
                FactionAPI faction,
                boolean isPlayerColony,
                int colonySize) {

            ColonyExpeditionIntel.createColonyStatic(
                market,
                planet,
                faction,
                FOUNDING_RATHER_THAN_RESETTLING,
                isPlayerColony,
                colonySize);
        }
    }
}
