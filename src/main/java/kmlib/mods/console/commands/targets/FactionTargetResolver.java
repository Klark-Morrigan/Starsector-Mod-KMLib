package kmlib.mods.console.commands.targets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.impl.campaign.ids.Factions;

import kmlib.mods.console.ConsoleStrings;
import kmlib.text.KmlibStrings;

import static kmlib.starsector.strings.KmlibStringKeys.CATEGORY;
import static kmlib.starsector.strings.KmlibStringKeys.CONSOLE_TARGET_FACTION_NOT_FOUND;
import static kmlib.starsector.strings.KmlibStringKeys.CONSOLE_TARGET_NO_SECTOR_FOR_FACTIONS;

/**
 * Finding the one faction a command was told to act for: the one named by ID, or - when none was
 * named - the player's own.
 *
 * <p>Shared for the reason the market-target search is: naming an owner is the shape of every
 * command that changes who holds a place, and only what is done with the answer differs between
 * them. Written per command instead, the two would drift on the parts a player notices - whether
 * omitting the ID means the player or means nothing at all, and how an ID no faction answers to
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

    private FactionTargetResolver() {
        // utility class, no instances.
    }

    /**
     * Resolves the faction a command should act for.
     *
     * <p>An ID names the faction outright; without one the player's own is taken. Either way the
     * sector has to answer for it, so a command never acts for a faction that does not exist -
     * a mistyped ID would otherwise be applied verbatim and leave a colony held by nobody.
     *
     * @param sector    the sector the run is made against; without one there is nothing to look
     *                  an ID up in
     * @param factionId the ID of the faction to act for; null or blank asks for the player's own
     * @return the faction to act for, or why there is none
     */
    public static TargetResolution<FactionAPI> resolveOwningFaction(
            SectorAPI sector,
            String factionId) {

        // The one refusal that names no faction: without a sector there is nothing to look an ID up
        // in, and no default to fall back to either - the player's faction is the sector's to answer
        // for like any other.
        if (sector == null) {
            return new UnresolvedTarget<>(ConsoleStrings.get(CATEGORY, CONSOLE_TARGET_NO_SECTOR_FOR_FACTIONS));
        }

        var requestedId = KmlibStrings.hasText(factionId)
            ? factionId
            : Factions.PLAYER;

        var faction = sector.getFaction(requestedId);

        if (faction == null) {
            return new UnresolvedTarget<>(
                ConsoleStrings.format(CATEGORY, CONSOLE_TARGET_FACTION_NOT_FOUND, requestedId));
        }
        return new ResolvedTarget<>(faction);
    }
}
