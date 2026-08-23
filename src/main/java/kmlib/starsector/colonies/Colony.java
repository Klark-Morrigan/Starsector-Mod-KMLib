package kmlib.starsector.colonies;

import com.fs.starfarer.api.campaign.StarSystemAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.markets.MarketVisibility;

import java.util.Objects;

/**
 * One colony: its market, what kind of place that market stands for, and whether the sector's
 * economy lists it.
 *
 * <p>Carries no location. Where a colony was found is the caller's own knowledge, because the
 * caller is what asked - it named a star system, or hyperspace, or the whole sector, and got
 * back what was there. Storing the place here would be a second copy of that, free to disagree
 * with it, and would make a colony unusable in any listing that spans more than one place. A
 * caller that does need the location asks the market for it.
 *
 * <p>A colony can sit on a real entity under a real faction and never be registered with the
 * economy - vanilla builds Galatia Academy that way. Which listing a market was found in is
 * therefore real information, and it is destroyed the moment the two listings are
 * concatenated. A reader that weighs a colony needs it, since weight is computed from
 * industries, conditions and stability the economy is what maintains; a reader that merely
 * counts or names one does not. Carrying it lets both read one set rather than each walking
 * the listing that suits it and then disagreeing about what is present.
 *
 * <p>Concealment, discovery and where the colony stands are all read back off the market rather
 * than stored beside it, so a colony can never report a state its own market contradicts.
 * Listing membership has no such source to defer to - it is a property of where the market was
 * found, not of the market - which is exactly why it is stored here.
 *
 * <p>Those reads are why the market is required rather than absorbed when absent. A colony with
 * no market has no concealment, no discovery and nowhere to stand, so every one of them would
 * have to invent an answer; refusing the colony at construction fails where the mistake is
 * instead of three reads downstream, each free to invent a different one.
 *
 * <p>Kind is stored for the other reason: it is recoverable from the market, but every reader
 * routes on it, and one resolving it for itself is a second statement of what an abandoned
 * station is, free to disagree with this one. Resolving it where the colony is selected also
 * settles it on the market that won its place, rather than on whichever market a later reader
 * happens to hold.
 *
 * @param market            the colony's market; mandatory, a colony with no market being no
 *                          colony at all - a null one is refused rather than absorbed
 * @param kind              what kind of place the market stands for - somewhere people live, or
 *                          a derelict nobody ever lived on
 * @param isListedByEconomy whether the sector's economy lists this market, as opposed to it
 *                          hanging on one of its location's entities unregistered
 */
public record Colony(
    MarketAPI market,
    ColonyKind kind,
    boolean isListedByEconomy) {

    /** Refuses a colony with no market, there being nothing for its own reads to answer off. */
    public Colony {
        Objects.requireNonNull(market, "A colony needs a market: it has no facts without one.");
    }

    /**
     * Whether the colony is concealed - present and owned, but not publicly listed.
     *
     * <p>Independent of whether the player has found it: a raided pirate base stays
     * permanently hidden while being perfectly well known. Callers asking what the player may
     * be told want {@link Colonies#readKnownColonies}, and callers asking whether anybody lives
     * somewhere want {@link Colonies#readInhabitingColonies}.
     *
     * <p>Concealment is also one of the two things that put a colony behind a revelation gate,
     * the other being its kind - a place hiding itself is one the fog alone would show the
     * moment its entity turned out never to have been discoverable.
     *
     * @return true when the market is hidden
     */
    public boolean isHidden() {
        return market.isHidden();
    }

    /**
     * Whether the player has found this colony's market - the entity's own fact, and the base
     * every rule about showing a colony is built on.
     *
     * <p>Not the whole of what the player may be told. An abandoned station or a concealed
     * colony has to have been revealed as well, and that question needs the place the colony
     * stands in - so it is answered over a colony set rather than here.
     *
     * @return true when the player has found the market's entity
     */
    public boolean isDiscoveredByPlayer() {
        return MarketVisibility.isDiscoveredByPlayer(market);
    }

    /**
     * Whether this colony has been observed where it now stands - the recorded half of what makes
     * a gated colony known.
     *
     * <p>Says nothing about who did the observing, and deliberately so. The player standing in
     * the place and the place's own inhabitants are both observations, and both are written to
     * one register, so a colony known through its neighbours stays known once those neighbours
     * are gone. A route tested live rather than recorded would take the colony off the map the
     * day its witnesses died, for a player who has known about it for years.
     *
     * <p>The observation has to name the place the colony is in today, not merely some place it
     * was once seen in. A colony that has moved since is unseen again until somebody meets it
     * where it has gone, and one founded after the last observation was never seen at all - both
     * of which a bare "has this system been entered" would answer wrongly, and in opposite
     * directions.
     *
     * <p>A colony in no star system - hyperspace, where mods put a few - reads sighted. There is
     * no system to have been in and none to be settled, so a gate answering otherwise would hold
     * such a colony back for good rather than until somebody saw it.
     *
     * @param sightings what has been observed and where; an unstated register reads as nothing
     *                  seen, which withholds rather than leaks
     * @return true when the colony was seen in the system it stands in, or it stands in no system
     *         at all
     */
    public boolean isSighted(ColonySightings sightings) {

        if (!(market.getContainingLocation() instanceof StarSystemAPI system)) {
            return true;
        }
        var observation = sightings == null
            ? null
            : sightings.readObservation(market.getId());

        return observation != null && observation.locationId().equals(system.getId());
    }

    /**
     * Who holds this colony, as the faction id an ownership change writes.
     *
     * <p>Read off the market's own id rather than its faction object, that being what a transfer
     * writes and therefore what answers for the owner a moment after one.
     *
     * <p>Asked wherever one colony's owner has to be told from another's - the place a colony
     * stands in vouches for it only through somebody else's colony, a faction being the last
     * witness to credit with announcing its own concealed holdings.
     *
     * @return the owning faction's id, or null where the market names none
     */
    public String readOwnerId() {
        return market.getFactionId();
    }
}
