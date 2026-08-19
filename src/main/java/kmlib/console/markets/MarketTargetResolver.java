package kmlib.console.markets;

import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.StarSystemAPI;

import kmlib.starsector.systems.StarSystems;
import kmlib.text.KmlibStrings;

/**
 * Finding the one market a command was pointed at: the place named by id, or - when none was
 * named - the nearest one that will do.
 *
 * <p>Shared because that pair of ways to say "this place" is the shape of every command acting
 * on a single colony, and only what counts as a candidate differs between them. Written per
 * command instead, the two would drift on the parts a player notices: whether a bare invocation
 * searches at all, whether an id is looked for beyond the system the fleet is in, and how a
 * place that does not qualify is told apart from one that is not there.
 *
 * <p>The search is confined to the given system on purpose. A dev command acts where the player
 * is standing, and an id resolved sector-wide would silently act on a same-named place on the
 * other side of the map.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state, and free
 * of {@code Global} so a run can be posed against a stub sector.
 */
public final class MarketTargetResolver {

    // The one refusal that names no place and no requirement: without a system there is nothing
    // to search and nothing to say about what was wanted.
    private static final String NO_SYSTEM_MESSAGE = "No star system to search.";

    private MarketTargetResolver() {
        // utility class, no instances.
    }

    /**
     * Resolves the market a command should act on within {@code system}.
     *
     * <p>An id names the place outright; without one the qualifying place nearest the player's
     * fleet is taken, which is what makes the bare invocation useful at all. Either way the
     * market has to meet {@code requirement}, so a command never acts on a place of the wrong
     * kind merely because it was named.
     *
     * @param sector      the sector the run is made against; null leaves nothing to measure
     *                    from, so a bare invocation finds nothing
     * @param system      the system to search, the one the player's fleet is in; null yields an
     *                    unresolved target rather than a search of the sector
     * @param entityId    the id of the entity to act on; null or blank asks for the nearest
     *                    qualifying place instead
     * @param requirement what makes a market a candidate, and the phrase a refusal names it by
     * @return the market to act on, or why there is none
     */
    public static MarketTargetResolution resolveTargetMarket(
            SectorAPI sector,
            StarSystemAPI system,
            String entityId,
            MarketTargetRequirement requirement) {

        if (system == null) {
            return new UnresolvedMarketTarget(NO_SYSTEM_MESSAGE);
        }
        return KmlibStrings.hasText(entityId)
            ? resolveNamedMarket(system, entityId, requirement)
            : resolveNearestMarket(sector, system, requirement);
    }

    // The place a player named, refused at whichever of three points it fails: no such entity
    // here, nothing on it to act on, or a market of the wrong kind. Told apart rather than
    // folded into one "no target", because each is a different mistake to correct.
    private static MarketTargetResolution resolveNamedMarket(
            StarSystemAPI system,
            String entityId,
            MarketTargetRequirement requirement) {

        var entity = system.getEntityById(entityId);

        if (entity == null) {
            return new UnresolvedMarketTarget("No entity with id '"
                + entityId
                + "' in "
                + StarSystems.readDisplayName(system)
                + ".");
        }
        var market = entity.getMarket();

        if (market == null) {
            return new UnresolvedMarketTarget("Entity '" + entityId + "' has no market.");
        }
        if (!requirement.isMetBy(market)) {
            return new UnresolvedMarketTarget("The market on '"
                + entityId
                + "' is not "
                + requirement.requirementPhrase()
                + ".");
        }
        return new ResolvedMarketTarget(market);
    }

    // The qualifying place nearest the player's fleet, that being where a player acting without
    // naming anything means. A sector with no fleet to measure from finds nothing, which reads
    // the same as a system holding nothing of the kind - and, for a command, is.
    private static MarketTargetResolution resolveNearestMarket(
            SectorAPI sector,
            StarSystemAPI system,
            MarketTargetRequirement requirement) {

        var playerFleet = sector == null ? null : sector.getPlayerFleet();
        var nearestMarket = StarSystems
            .findNearestMarket(sector, system, playerFleet, requirement::isMetBy);

        if (nearestMarket.isEmpty()) {
            return new UnresolvedMarketTarget("Nothing in "
                + StarSystems.readDisplayName(system)
                + " is "
                + requirement.requirementPhrase()
                + ".");
        }
        return new ResolvedMarketTarget(nearestMarket.get());
    }
}
