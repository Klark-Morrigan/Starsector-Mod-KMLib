package kmlib.mods.console.commands.targets;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.systems.SectorStarSystems;
import kmlib.starsector.systems.StarSystems;
import kmlib.text.KmlibStrings;

/**
 * Finding the one market a command was pointed at: the place named by ID, or - when none was
 * named - the nearest one that will do.
 *
 * <p>Shared because that pair of ways to say "this place" is the shape of every command acting
 * on a single colony, and only what counts as a candidate differs between them. Written per
 * command instead, the two would drift on the parts a player notices: whether a bare invocation
 * searches at all, how far an ID is looked for, and how a place that does not qualify is told
 * apart from one that is not there.
 *
 * <p>The two ways reach different distances, and that is the point rather than an inconsistency.
 * An ID names one place outright, and IDs are unique across the sector, so it is looked for
 * across the sector - hyperspace included. "Nearest" is only meaningful from somewhere, so it is
 * measured from the player's fleet and confined to the star system that fleet is in.
 *
 * <p>Which system that is, is settled here rather than asked of the caller: a command has no
 * second opinion about where its player is standing, and passing it in would have the lookup run
 * twice per invocation with the second one invisible at the call site. It is also why the
 * requirement to be in one belongs here and not in each command's context guards - it is a
 * condition of searching, not of running.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state, and free
 * of {@code Global}, so a run reads only the sector it is given.
 */
public final class MarketTargetResolver {

    // Nothing to look an ID up in and nowhere to search from. The one refusal that names neither
    // a place nor a requirement, there being no sector to phrase either against.
    private static final String NO_SECTOR_MESSAGE = "No sector to search.";

    // What a bare invocation is refused with from hyperspace: nowhere to measure "nearest" from,
    // and the way out said in the same breath, since naming a place is not a narrowing of the
    // search but the alternative to it.
    private static final String NO_SYSTEM_MESSAGE =
        "Not in a star system - name an entity id to point at a place directly.";

    private MarketTargetResolver() {
        // utility class, no instances.
    }

    /**
     * Resolves the market a command should act on.
     *
     * <p>An ID names the place outright, anywhere in the sector; without one the qualifying place
     * nearest the player's fleet is taken, which is what makes the bare invocation useful at all.
     * Either way the market has to meet {@code requirement}, so a command never acts on a place of
     * the wrong kind merely because it was named.
     *
     * @param sector      the sector the run is made against; without one there is nothing to look
     *                    an ID up in and nowhere to search
     * @param entityId    the ID of the entity to act on; null or blank asks for the nearest
     *                    qualifying place instead
     * @param requirement what makes a market a candidate, and the phrase a refusal names it by
     * @return the market to act on, or why there is none
     */
    public static TargetResolution<MarketAPI> resolveTargetMarket(
            SectorAPI sector,
            String entityId,
            MarketTargetRequirement requirement) {

        if (sector == null) {
            return new UnresolvedTarget<>(NO_SECTOR_MESSAGE);
        }
        return KmlibStrings.hasText(entityId)
            ? resolveNamedMarket(sector, entityId, requirement)
            : resolveNearestMarket(sector, requirement);
    }

    // The place a player named, refused at whichever of three points it fails: no such entity in
    // the sector, nothing on it to act on, or a market of the wrong kind. Told apart rather than
    // folded into one "no target", because each is a different mistake to correct.
    //
    // Looked up through the sector rather than a system, so a place can be acted on from
    // anywhere - the other side of the map, or hyperspace, where a command has no system to be
    // scoped to at all. Safe because an entity ID is unique sector-wide, unlike a name.
    private static TargetResolution<MarketAPI> resolveNamedMarket(
            SectorAPI sector,
            String entityId,
            MarketTargetRequirement requirement) {

        var entity = sector.getEntityById(entityId);

        if (entity == null) {
            return new UnresolvedTarget<>("No entity with id '" + entityId + "' in the sector.");
        }
        var market = entity.getMarket();

        if (market == null) {
            return new UnresolvedTarget<>("Entity '" + entityId + "' has no market.");
        }
        if (!requirement.isMetBy(market)) {
            return new UnresolvedTarget<>("The market on '"
                + entityId
                + "' is not "
                + requirement.requirementPhrase()
                + ".");
        }
        return new ResolvedTarget<>(market);
    }

    // The qualifying place nearest the player's fleet, that being where a player acting without
    // naming anything means. Confined to the system the fleet is in: a sector-wide "nearest"
    // would answer with a place several jumps away that the player never had in mind, and a
    // fleet in hyperspace is in no system at all, so there is nothing to search. The fleet is
    // there to measure from by construction, a system having been found at all only by reading
    // which one the fleet is in.
    private static TargetResolution<MarketAPI> resolveNearestMarket(
            SectorAPI sector,
            MarketTargetRequirement requirement) {

        var system = SectorStarSystems.getPlayerStarSystem(sector);

        if (system == null) {
            return new UnresolvedTarget<>(NO_SYSTEM_MESSAGE);
        }
        var nearestMarket = StarSystems
            .findNearestMarket(sector, system, sector.getPlayerFleet(), requirement::isMetBy);

        if (nearestMarket.isEmpty()) {
            return new UnresolvedTarget<>("Nothing in "
                + StarSystems.readDisplayName(system)
                + " is "
                + requirement.requirementPhrase()
                + ".");
        }
        return new ResolvedTarget<>(nearestMarket.get());
    }
}
