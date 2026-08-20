package kmlib.console;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.FactionAPI;
import com.fs.starfarer.api.campaign.econ.MarketAPI;

import kmlib.console.output.CommandOutput;
import kmlib.console.parsing.Parameter;
import kmlib.console.parsing.ParameterSpec;
import kmlib.console.targets.FactionTargetResolver;
import kmlib.console.targets.MarketTargetRequirement;
import kmlib.console.targets.MarketTargetResolver;
import kmlib.console.targets.ResolvedTarget;
import kmlib.console.targets.UnresolvedTarget;
import kmlib.starsector.factions.StarsectorPlayerFactionResolver;
import kmlib.starsector.markets.Markets;
import kmlib.starsector.markets.ownership.MarketOwnershipTransfer;

import static kmlib.console.parsing.ParameterValues.text;

/**
 * Console command (cheat): hands an existing colony to another owner. Takes an optional entity id
 * and an optional incoming faction id, so a bare invocation gives the nearest colony to the
 * player.
 *
 * <p>Named, a colony can be handed over from anywhere - another system, or hyperspace - since an
 * entity id points at one place in the whole sector. Only the bare invocation needs the player to
 * be in a star system, "nearest" having nowhere to measure from otherwise. That is one condition
 * of the search rather than of the command, so it is stated where the search is and this command
 * guards only on being in a campaign.
 *
 * <p>The game itself has no colony hand-over of any kind - a colony may be founded, never taken -
 * so this bypasses no prerequisite in the way {@code kmlib_colonise} does. What it does bypass is
 * the absence of the operation, which is what makes it a cheat: standing, intel and the reasons a
 * place changed hands are all skipped over.
 *
 * <p>A shell over three collaborators, and deliberately holds no mechanics of its own: which
 * place was meant is {@code MarketTargetResolver}'s, which faction was meant is
 * {@code FactionTargetResolver}'s, and what handing a colony over consists of is
 * {@link MarketOwnershipTransfer}'s. That split is what lets the same hand-over be reached from a
 * mod's own code rather than only from a player typing at the console.
 *
 * <p>Every refusal is settled before anything is mutated, so a run naming an unknown faction, or
 * the faction already holding the place, leaves the colony exactly as it was. The target is
 * resolved first only because a run with two mistakes in it has to report one of them, and the
 * place is the argument a player is likelier to have got wrong.
 *
 * <p>Soft Console Commands dependency: this class touches {@code org.lazywizard.console.*}, but
 * it is loaded only when Console Commands instantiates it from KMLib's commands.csv. A game
 * without Console Commands never loads it, so KMLib runs fine without that mod - no hard
 * dependency is declared.
 */
public final class TransferMarketCommand extends KmlibBaseConsoleCommand {

    private static final TransferMarketSpec SPEC = new TransferMarketSpec();

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

        var sector = Global.getSector();

        var target = MarketTargetResolver.resolveTargetMarket(
            sector,
            parsed.get(SPEC.entityId),
            MarketTargetRequirement.EXISTING_COLONY);

        if (target instanceof UnresolvedTarget<MarketAPI> unresolvedTarget) {

            output.showMessage(unresolvedTarget.failureMessage());
            return CommandResult.ERROR;
        }

        var owner = FactionTargetResolver.resolveOwningFaction(sector, parsed.get(SPEC.factionId));

        if (owner instanceof UnresolvedTarget<FactionAPI> unresolvedOwner) {

            output.showMessage(unresolvedOwner.failureMessage());
            return CommandResult.ERROR;
        }

        var market = ((ResolvedTarget<MarketAPI>) target).target();
        var faction = ((ResolvedTarget<FactionAPI>) owner).target();

        // A hand-over to the incumbent is not one, and the transfer refuses it on this same read -
        // so the run is stopped here to say why, rather than reported as a success that did
        // nothing. This is no target requirement: every one of those weighs the market alone,
        // while this is a relation between the market and the faction argument and so cannot be
        // asked until both have resolved.
        if (Markets.isOwnedBy(market, faction.getId())) {

            output.showMessage(describeUnchangedOwnership(market, faction));
            return CommandResult.ERROR;
        }

        MarketOwnershipTransfer.transferOwnership(market, faction.getId());

        output.showMessage(describeTransferredColony(market, faction));
        return CommandResult.SUCCESS;
    }

    // What the player is told about the colony that has changed hands.
    private static String describeTransferredColony(MarketAPI market, FactionAPI faction) {

        return "Transferred "
            + market.getName()
            + " to "
            + readOwnerName(faction)
            + '.';
    }

    // Why a run that named the colony's own owner did nothing. Worded as a statement about the
    // colony rather than about the argument, the mistake being a belief about who holds the place.
    private static String describeUnchangedOwnership(MarketAPI market, FactionAPI faction) {

        return market.getName()
            + " is already owned by "
            + readOwnerName(faction)
            + '.';
    }

    // Through the resolver rather than getDisplayName(), because the player faction reports a
    // placeholder until it has an identity of its own - "Independent" before the first colony, and
    // the literal "player" on a stock Nexerelin setup. Falls back to the id, which is what the
    // player typed to name the faction in the first place.
    private static String readOwnerName(FactionAPI faction) {
        return StarsectorPlayerFactionResolver.resolveDisplayName(faction, faction.getId());
    }

    /**
     * What {@code kmlib_transfer_market} accepts: the colony to hand over and the faction to hand
     * it to, both optional and in that order.
     *
     * <p>Neither carries a default of its own. What an omitted argument means is a question about
     * the sector rather than about the command line - the nearest colony, the player's own faction
     * - so each is left unsupplied here and answered by the resolver that knows how to look it up.
     */
    private static final class TransferMarketSpec extends ParameterSpec {

        private final Parameter<String> entityId =
            acceptsPositional("entity_id", "<entity-id>", text());
        private final Parameter<String> factionId =
            acceptsPositional("faction_id", "<faction-id>", text());

        private TransferMarketSpec() {
            super("Usage: kmlib_transfer_market [entity-id] [faction-id].");
        }
    }
}
