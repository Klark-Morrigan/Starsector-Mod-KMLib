package kmlib.mods.console.commands.targets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.SectorAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.factions.FactionNames;

/**
 * The pair every command that changes who holds a place is aimed at: the place, and the faction
 * the command acts for.
 *
 * <p>One value rather than two, because neither half is worth having alone. A command holding a
 * market and no owner has nothing to do with it, and one holding an owner and no market has
 * nowhere to do it - so the two arrive together or the run is already over.
 *
 * @param market the place the command acts on
 * @param owner  the faction it acts for
 */
public record MarketOwnerTarget(
    MarketAPI market,
    FactionAPI owner) {

    /**
     * What to call the owner in something the player reads: {@link FactionNames#resolveDisplayNameOrId}.
     *
     * @param sector the sector the command acts in
     * @return the owner's name for prose
     */
    public String readOwnerName(SectorAPI sector) {
        return FactionNames.resolveDisplayNameOrId(sector, owner);
    }
}
