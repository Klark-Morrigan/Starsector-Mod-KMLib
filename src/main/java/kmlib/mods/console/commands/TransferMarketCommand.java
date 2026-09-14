package kmlib.mods.console.commands;

import kmlib.mods.console.commands.output.CommandOutput;
import kmlib.mods.console.commands.targets.MarketOwnerTarget;
import kmlib.mods.console.commands.targets.MarketOwnerTargetResolver;
import kmlib.mods.console.commands.targets.MarketTargetRequirement;
import kmlib.mods.console.commands.targets.ResolvedTarget;
import kmlib.mods.console.commands.targets.UnresolvedTarget;
import kmlib.starsector.markets.Markets;
import kmlib.starsector.markets.ownership.MarketOwnershipTransfer;

/**
 * Console command (cheat): hands an existing colony to another owner. Takes an optional entity ID
 * and an optional incoming faction ID, so a bare invocation gives the nearest colony to the
 * player.
 *
 * <p>Named, a colony can be handed over from anywhere - another system, or hyperspace - since an
 * entity ID points at one place in the whole sector. Only the bare invocation needs the player to
 * be in a star system, "nearest" having nowhere to measure from otherwise. That is one condition
 * of the search rather than of the command, so it is stated where the search is and this command
 * guards only on being in a campaign.
 *
 * <p>The game itself has no colony hand-over of any kind - a colony may be founded, never taken -
 * so this bypasses no prerequisite in the way {@code kmlib_colonise} does. What it does bypass is
 * the absence of the operation, which is what makes it a cheat: standing, intel and the reasons a
 * place changed hands are all skipped over.
 *
 * <p>A shell over two collaborators, and deliberately holds no mechanics of its own: what the run
 * was aimed at is {@code MarketOwnerTargetResolver}'s, and what handing a colony over consists of
 * is {@link MarketOwnershipTransfer}'s. That split is what lets the same hand-over be reached from
 * a mod's own code rather than only from a player typing at the console.
 *
 * <p>Soft Console Commands dependency: this class touches {@code org.lazywizard.console.*}, but
 * it is loaded only when Console Commands instantiates it from KMLib's commands.csv. A game
 * without Console Commands never loads it, so KMLib runs fine without that mod - no hard
 * dependency is declared.
 */
public final class TransferMarketCommand extends BaseKmlibCommand {

    private static final MarketOwnerSpec SPEC =
        new MarketOwnerSpec("Usage: kmlib_transfer_market [entity-id] [faction-id].");

    public TransferMarketCommand() {
    }

    TransferMarketCommand(CommandOutput output) {
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

        // Both halves are resolved before anything is mutated, so a run naming an unknown faction
        // leaves the colony exactly as it was rather than detached from an owner and given to
        // nobody.
        var sector = readActiveSector();

        var target = MarketOwnerTargetResolver.resolveMarketAndOwner(
            sector,
            parsed.get(SPEC.entityId),
            parsed.get(SPEC.factionId),
            MarketTargetRequirement.EXISTING_COLONY);

        if (target instanceof UnresolvedTarget<MarketOwnerTarget> unresolvedTarget) {

            output.showMessage(unresolvedTarget.failureMessage());
            return CommandResult.ERROR;
        }

        var found = ((ResolvedTarget<MarketOwnerTarget>) target).target();

        // A hand-over to the incumbent is not one, and the transfer refuses it on this same read -
        // so the run is stopped here to say why, rather than reported as a success that did
        // nothing. This is no target requirement: every one of those weighs the market alone,
        // while this is a relation between the two halves and so cannot be asked until both have
        // resolved.
        if (Markets.isOwnedBy(found.market(), found.owner().getId())) {

            output.showMessage(describeUnchangedOwnership(found));
            return CommandResult.ERROR;
        }

        MarketOwnershipTransfer.transferOwnership(sector, found.market(), found.owner().getId());

        output.showMessage(describeTransferredColony(found));
        return CommandResult.SUCCESS;
    }

    // What the player is told about the colony that has changed hands.
    private static String describeTransferredColony(MarketOwnerTarget target) {

        return "Transferred "
            + target.market().getName()
            + " to "
            + target.readOwnerName()
            + '.';
    }

    // Why a run that named the colony's own owner did nothing. Worded as a statement about the
    // colony rather than about the argument, the mistake being a belief about who holds the place.
    private static String describeUnchangedOwnership(MarketOwnerTarget target) {

        return target.market().getName()
            + " is already owned by "
            + target.readOwnerName()
            + '.';
    }
}
