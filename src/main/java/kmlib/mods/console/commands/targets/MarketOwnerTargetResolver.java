package kmlib.mods.console.commands.targets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

/**
 * Finding both halves of what a command was aimed at - the place and the faction it acts for -
 * and refusing the run whole where either is missing.
 *
 * <p>Stated once because the pair is the argument shape of every command that changes who holds
 * a place, and because the order the two are asked in is a decision rather than an accident.
 * Written out per command, that decision would be a comment each of them could drift from, and
 * the second command to be written would settle it again from scratch.
 *
 * <p>Nothing is looked up after a refusal. The place is asked for first, so a run with two
 * mistakes in it reports the place: an ID that names nothing is the likelier of the two to have
 * been mistyped, and it is the argument a player can check without knowing what the sector calls
 * its factions.
 *
 * <p>Final class with a private constructor: pure-function utility, no instance state, and free
 * of {@code Global}, so a run reads only the sector it is given.
 */
public final class MarketOwnerTargetResolver {

    private MarketOwnerTargetResolver() {
        // utility class, no instances.
    }

    /**
     * Resolves the place a command acts on together with the faction it acts for.
     *
     * <p>Both searches are the ones a command would otherwise run itself, in the order stated
     * above, and a refusal from either is passed on as this pair's own - the copy is already
     * finished, and rewording it here would put a second voice on the same failure.
     *
     * @param sector      the sector the run is made against; without one there is nothing to look
     *                    either half up in
     * @param entityId    the ID of the entity to act on; null or blank asks for the nearest
     *                    qualifying place instead
     * @param factionId   the ID of the faction to act for; null or blank asks for the player's own
     * @param requirement what makes a market a candidate, and the phrase a refusal names it by
     * @return the place and the owner, or why the run cannot go ahead
     */
    public static TargetResolution<MarketOwnerTarget> resolveMarketAndOwner(
            SectorAPI sector,
            String entityId,
            String factionId,
            MarketTargetRequirement requirement) {

        var marketTarget = MarketTargetResolver.resolveTargetMarket(sector, entityId, requirement);

        if (marketTarget instanceof UnresolvedTarget<MarketAPI> unresolvedMarket) {
            return new UnresolvedTarget<>(unresolvedMarket.failureMessage());
        }

        var ownerTarget = FactionTargetResolver.resolveOwningFaction(sector, factionId);

        if (ownerTarget instanceof UnresolvedTarget<FactionAPI> unresolvedOwner) {
            return new UnresolvedTarget<>(unresolvedOwner.failureMessage());
        }

        return new ResolvedTarget<>(new MarketOwnerTarget(
            ((ResolvedTarget<MarketAPI>) marketTarget).target(),
            ((ResolvedTarget<FactionAPI>) ownerTarget).target()));
    }
}
