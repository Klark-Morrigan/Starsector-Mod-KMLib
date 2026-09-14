package kmlib.mods.console.commands;

import kmlib.mods.console.commands.output.CommandOutput;
import kmlib.mods.console.commands.targets.MarketOwnerTarget;
import kmlib.mods.console.commands.targets.MarketOwnerTargetResolver;
import kmlib.mods.console.commands.targets.MarketTargetRequirement;
import kmlib.mods.console.commands.targets.ResolvedTarget;
import kmlib.mods.console.commands.targets.UnresolvedTarget;
import kmlib.starsector.markets.colonisation.MarketColoniser;

/**
 * Console command (cheat): founds a colony on a body that so far carries only survey data. Takes
 * an optional entity ID and an optional owning faction ID, so a bare invocation colonises the
 * nearest such body for the player.
 *
 * <p>Named, a body can be colonised from anywhere - another system, or hyperspace - since an
 * entity ID points at one place in the whole sector. Only the bare invocation needs the player to
 * be in a star system, "nearest" having nowhere to measure from otherwise. That is one condition
 * of the search rather than of the command, so it is stated where the search is and this command
 * guards only on being in a campaign.
 *
 * <p>Bypasses every prerequisite the survey panel's own button enforces - no survey, no outpost
 * cost, no need to be in orbit - which is what makes it a cheat rather than a shortcut. What it
 * produces is otherwise the game's own baseline colony, and on an install running a mod with a
 * colonisation of its own, that mod's.
 *
 * <p>A shell over two collaborators, and deliberately holds no mechanics of its own: what the run
 * was aimed at is {@code MarketOwnerTargetResolver}'s, and what founding a colony consists of is
 * {@link MarketColoniser}'s. That split is what lets the same founding be reached from a mod's own
 * code rather than only from a player typing at the console.
 *
 * <p>Soft Console Commands dependency: this class touches {@code org.lazywizard.console.*}, but
 * it is loaded only when Console Commands instantiates it from KMLib's commands.csv. A game
 * without Console Commands never loads it, so KMLib runs fine without that mod - no hard
 * dependency is declared.
 */
public final class ColoniseCommand extends BaseKmlibCommand {

    private static final MarketOwnerSpec SPEC =
        new MarketOwnerSpec("Usage: kmlib_colonise [entity-id] [faction-id].");

    public ColoniseCommand() {
    }

    ColoniseCommand(CommandOutput output) {
        super(output);
    }

    @Override
    public CommandResult runCommand(String args, CommandContext context) {

        // A campaign is the only context guard: whether being in a star system is needed depends
        // on which argument shape was typed, which is not known until the parse, and is the
        // search's own condition rather than this command's.
        var parsed = readInput(context, args)
            .requireCampaign()
            .parseArguments(SPEC);

        if (!parsed.isValid()) {
            return parsed.getResult();
        }

        var sector = readActiveSector();

        // Both halves are resolved before anything is mutated, so a run naming an unknown faction
        // leaves the body untouched rather than half-colonised under nobody.
        var target = MarketOwnerTargetResolver.resolveMarketAndOwner(
            sector,
            parsed.get(SPEC.entityId),
            parsed.get(SPEC.factionId),
            MarketTargetRequirement.COLONISABLE_BODY);

        if (target instanceof UnresolvedTarget<MarketOwnerTarget> unresolvedTarget) {

            output.showMessage(unresolvedTarget.failureMessage());
            return CommandResult.ERROR;
        }

        var found = ((ResolvedTarget<MarketOwnerTarget>) target).target();

        MarketColoniser.establishColony(sector, found.market(), found.owner().getId());

        output.showMessage(describeFoundedColony(found));
        return CommandResult.SUCCESS;
    }

    // What the player is told about the colony that now exists. The name is read from the market
    // only after the founding, never before it: survey data goes by whatever name its placeholder
    // happened to hold, and the colony takes the body's - or, where a mod founded it, whatever
    // name that mod gave the place.
    private static String describeFoundedColony(MarketOwnerTarget target) {

        return "Founded a colony on "
            + target.market().getName()
            + " for "
            + target.readOwnerName()
            + '.';
    }
}
