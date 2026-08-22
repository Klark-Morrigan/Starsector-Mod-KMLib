package kmlib.console.targets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import kmlib.text.KmlibStrings;

/**
 * Finding the one faction a command was told to act for: the one named by id, or - when none was
 * named - the player's own.
 *
 * <p>Shared for the reason the market-target search is: naming an owner is the shape of every
 * command that changes who holds a place, and only what is done with the answer differs between
 * them. Written per command instead, the two would drift on the parts a player notices - whether
 * omitting the id means the player or means nothing at all, and how an id no faction answers to
 * is worded.
 *
 * <p>Defaulting to the player is what makes the bare invocation useful: a dev tool handing out
 * colonies is nearly always handing them to the person running it, and an owner is not something
 * a colony can be left without.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state, and free
 * of {@code Global}, so a run reads only the sector it is given.
 */
public final class FactionTargetResolver {

    // The one refusal that names no faction: without a sector there is nothing to look an id up
    // in, and no default to fall back to either - the player's faction is the sector's to answer
    // for like any other.
    private static final String NO_SECTOR_MESSAGE = "No sector to read factions from.";

    private FactionTargetResolver() {
        // utility class, no instances.
    }

    /**
     * Resolves the faction a command should act for.
     *
     * <p>An id names the faction outright; without one the player's own is taken. Either way the
     * sector has to answer for it, so a command never acts for a faction that does not exist -
     * a mistyped id would otherwise be applied verbatim and leave a colony held by nobody.
     *
     * @param sector    the sector the run is made against; without one there is nothing to look
     *                  an id up in
     * @param factionId the id of the faction to act for; null or blank asks for the player's own
     * @return the faction to act for, or why there is none
     */
    public static TargetResolution<FactionAPI> resolveOwningFaction(
            SectorAPI sector,
            String factionId) {

        if (sector == null) {
            return new UnresolvedTarget<>(NO_SECTOR_MESSAGE);
        }

        var requestedId = KmlibStrings.hasText(factionId)
            ? factionId
            : Factions.PLAYER;

        var faction = sector.getFaction(requestedId);

        if (faction == null) {
            return new UnresolvedTarget<>("No faction with id '" + requestedId + "'.");
        }
        return new ResolvedTarget<>(faction);
    }
}
