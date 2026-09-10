package kmlib.mods.console.commands.targets;

import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.starsector.factions.StarsectorPlayerFactionResolver;

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
     * What to call the owner in something the player reads.
     *
     * <p>Not {@code getDisplayName()}, because the player's own faction reports a placeholder
     * until it has an identity of its own - "Independent" before the first colony, and the
     * literal "player" on a stock Nexerelin setup - either of which reads in a sentence as
     * somebody else entirely. The id stands in until then, that being what was typed to name the
     * faction in the first place.
     *
     * @return the owner's name for prose
     */
    public String readOwnerName() {
        return StarsectorPlayerFactionResolver.resolveDisplayName(owner, owner.getId());
    }
}
